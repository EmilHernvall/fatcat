package se.c0la.fatcat.context;

import java.security.NoSuchAlgorithmException;
import java.util.*;

import se.c0la.fatcat.*;
import se.c0la.fatcat.async.*;
import se.c0la.fatcat.irc.*;

/**
 * ServerContext methods are responsible for propagating
 * events to all other clients, but any reply messages
 * to the sender should be handled by the Protocol
 * implementation of the source user.
 */
public class ServerContext
{
	Map<String, Operator> operators = new HashMap<String, Operator>();

	private static class CaseInsensitiveStringComparator implements Comparator<String>
	{
		public int compare(String a, String b)
		{
			return a.compareToIgnoreCase(b);
		}
	}

	private AsyncServer server;

	private Date startDate;

	private IRCProtocol ircProtocol;

	// Invariant: users.values() == nicks.values()
	private Map<Client, User> users;
	private Map<String, User> nicks;

	private Map<String, Channel> channels;
	
	private int maxUserCount;
	private int maxChannelCount;

	private long maxUserCountHappened;
	private long maxChannelCountHappened;

	public ServerContext(AsyncServer server)
	throws NoSuchAlgorithmException
	{
		this.server = server;

		this.startDate = new Date();

		users = new HashMap<Client, User>();
		nicks = new TreeMap<String, User>(new CaseInsensitiveStringComparator());

		channels = new TreeMap<String, Channel>(new CaseInsensitiveStringComparator());

		ircProtocol = new IRCProtocol(this);

		operators.put("erik", new Operator("erik", "25cfe5b055cf6b1fd5205f36a43c9a0eb12d3b67a6064973d69368e186d19b62"));
		operators.put("emil", new Operator("emil", "1639622dfac80e688b73aa31848e297de8d82bac7d2b724a09ec6554165cf182"));
	}

	// Active helper objects
	public AsyncServer getServer() { return server; }

	// Server info
	public String getServerName() { return "fatcat.c0la.se"; }
	public String getServerVersion() { return "1.3.3.7"; }
	public String getServerInfo() { return "Life is a cheesecake"; }
	public Date getStartDate() { return startDate; }

	// Server statistics
	public int getUserCount() { return users.size(); }
	public int getServiceCount() { return 0; }
	public int getOperatorCount() { return 0; }
	public int getServerCount() { return 0; }
	public int getUnknownsCount() { return 0; }
	public int getChannelCount() { return channels.size(); }

	public int getMaxUserCount() { return maxUserCount; }
	public int getMaxChannelCount() { return maxChannelCount; }
	public long getMaxUserCountHappened() { return maxUserCountHappened; }
	public long getMaxChannelCountHappened() { return maxChannelCountHappened; }
	
	// State getters
	public User getUser(Client client)
	{
		User user = users.get(client);
		if (user == null) {
			return null;
		}

		return new ImmutableUser(user);
	}
	
	public User getUser(String nick)
	{
		User user = nicks.get(nick);
		if (user == null) {
			return null;
		}

		return new ImmutableUser(user);
	}
	
	public Channel getChannel(String name)
	{
		Channel channel = channels.get(name);
		if (channel == null) {
			return null;
		}

		return new ImmutableChannel(channel);
	}

	public Operator getOperator(String name)
	{
		return operators.get(name);
	}

	public Collection<Channel> getChannels()
	{
		return Collections.unmodifiableCollection(channels.values());
	}
	
	// State setters
	public void setQuitMessage(Client client, String message)
	{
		User user = users.get(client);
		user.setQuitMessage(message);
	}
	
	// Events
	public void userConnectedEvent(Client client)
	{
		User user = new User(client);
		user.setProtocol(ircProtocol);

		users.put(client, user);
	}

	public void userDisconnectedEvent(Client client)
	{
		User user = users.get(client);
		
		if (user == null) { return; }
		
		String message = "Connection lost";
		if (user.getQuitMessage() != null) {
			message = user.getQuitMessage();
		}

		users.remove(client);
		if (user.getNick() != null) {
			nicks.remove(user.getNick());
		} else {
			// not registered
			return;
		}

		Set<Channel> channels = user.getChannels();
		Set<User> sentTo = new HashSet<User>();
		for (Channel channel : channels) {
			if (channel.getUserCount() == 0) {
				channels.remove(channel.getName());
				continue;
			}
			
			channel.removeUser(user);
			
			Set<User> users = channel.getUsers();
			for (User targetUser : users) {
				if (sentTo.contains(targetUser)) {
					continue;
				}
			
				PropagationProtocol propProtocol = targetUser.getPropagationProtocol();
				propProtocol.quit(targetUser, user, message);
				
				sentTo.add(targetUser);
			}
		}
	}

	public void userIdentificationEvent(User user, String userName, String mode, String realName)
	throws ErrorConditionException
	{
		// User objects are read-only when we receive them from
		// an outside source. Lets retrieve a writable instance.
		user = users.get(user.getClient());

		user.setUser(userName);
		user.setRealName(realName);

		if (user.getNick() != null && !user.hasRegistered()) {
			user.setRegistered();

			PropagationProtocol propProt = user.getPropagationProtocol();
			propProt.welcomeSequence(user);

			if(users.size() > maxUserCount) {
				maxUserCount = users.size();
				maxUserCountHappened = System.currentTimeMillis();
			}
		}
	}
	
	public void nickEvent(User user, String newNick)
	throws ErrorConditionException
	{
		if (user.getNick() != null) {
			nicks.remove(user.getNick());
		}

		// User objects are read-only when we receive them from
		// an outside source. Lets retrieve a writable instance.
		user = users.get(user.getClient());

		// propagate to users on the same channels as
		// the source user
		Set<Channel> channels = user.getChannels();
		Set<User> sentTo = new HashSet<User>();
		for (Channel channel : channels) {
			Set<User> users = channel.getUsers();
			
			for (User targetUser : users) {
				if (sentTo.contains(targetUser)) {
					continue;
				}
				
				PropagationProtocol propProtocol = targetUser.getPropagationProtocol();
				propProtocol.nickChange(targetUser, user, newNick);
				
				sentTo.add(targetUser);
			}
		}

		nicks.put(newNick, user);
		user.setNick(newNick);

		if (user.getUser() != null && !user.hasRegistered()) {
			user.setRegistered();

			PropagationProtocol propProt = user.getPropagationProtocol();
			propProt.welcomeSequence(user);

			if(users.size() > maxUserCount) {
				maxUserCount = users.size();
				maxUserCountHappened = System.currentTimeMillis();
			}
		}
	}
	
	public void messageEvent(User sourceUser, String targetName, String message)
	throws ErrorConditionException
	{
		// If a user exist with the value of targetName, send a 
		// private message to her instead
		User targetUser = this.getUser(targetName);
		if (targetUser != null) {
			PropagationProtocol propProtocol = targetUser.getPropagationProtocol();
			propProtocol.message(targetUser, sourceUser, targetName, message);

			return;
		}

		Channel channel = channels.get(targetName);
		if (channel == null) {
			return;
		}

		Set<User> users = channel.getUsers();
		for (User recvUser : users) {
			if (recvUser.equals(sourceUser)) {
				continue;
			}

			PropagationProtocol propProtocol = recvUser.getPropagationProtocol();
			propProtocol.message(recvUser, sourceUser, targetName, message);
		}
	}
	
	public void noticeEvent(User sourceUser, String targetName, String message)
	{
		// If a user exist with the value of targetName, send a 
		// private message to her instead
		User targetUser = this.getUser(targetName);
		if (targetUser != null) {
			PropagationProtocol propProtocol = targetUser.getPropagationProtocol();
			propProtocol.notice(targetUser, sourceUser, targetName, message);

			return;
		}

		Channel channel = channels.get(targetName);
		if (channel == null) {
			return;
		}

		Set<User> users = channel.getUsers();
		for (User recvUser : users) {
			if (recvUser.equals(sourceUser)) {
				continue;
			}

			PropagationProtocol propProtocol = recvUser.getPropagationProtocol();
			propProtocol.notice(recvUser, sourceUser, targetName, message);
		}
	}
	
	public void joinEvent(User user, String name)
	throws ErrorConditionException
	{
		// User objects are read-only when we receive them from
		// an outside source. Lets retrieve a writable instance.
		user = users.get(user.getClient());

		Channel channel = channels.get(name);
		boolean newChannel = false;
		if (channel == null) {
			channel = new Channel(name);
			channels.put(name, channel);
			
			newChannel = true;
			
			if(channels.size() > maxChannelCount) {
				maxChannelCount = channels.size();
				maxChannelCountHappened = System.currentTimeMillis();
			}
		}

		channel.addUser(user);
		user.addChannel(channel);
		
		if (newChannel) {
			ChannelMember member = channel.getUser(user);
			member.setAttribute(ChannelAttribute.OP);
		}
		else {
			channel.removeInvite(user.getNick());
		}

		// propagate to all other users on the channel
		Set<User> users = channel.getUsers();
		for (User targetUser : users) {
			PropagationProtocol propProtocol = targetUser.getPropagationProtocol();
			propProtocol.joinedChannel(targetUser, user, channel);
		}
	}
	
	public void inviteEvent(User sourceUser, Channel channel, User targetUser)
	throws ErrorConditionException
	{
		channel.addInvite(targetUser.getNick());
		
		PropagationProtocol propProtocol = targetUser.getPropagationProtocol();
		propProtocol.inviteToChannel(targetUser, sourceUser, channel);
	}
	
	public void topicEvent(Channel channel, User user, String newTopic)
	throws ErrorConditionException
	{
		channel = channels.get(channel.getName());
		channel.setTopic(newTopic, user.getNick());
		
		Set<User> users = channel.getUsers();
		for (User targetUser : users) {
			PropagationProtocol propProtocol = targetUser.getPropagationProtocol();
			propProtocol.topicChanged(targetUser, user, channel, newTopic);
		}
	}

	public void partEvent(User user, String name, String message)
	throws ErrorConditionException
	{
		Channel channel = channels.get(name);
		if (channel == null) {
			return;
		}

		// User objects are read-only when we receive them from
		// an outside source. Lets retrieve a writable instance.
		user = users.get(user.getClient());

		// propagate to all other users on the channel
		Set<User> users = channel.getUsers();
		for (User targetUser : users) {
			PropagationProtocol propProtocol = targetUser.getPropagationProtocol();
			propProtocol.partedChannel(targetUser, user, channel, message);
		}

		channel.removeUser(user);
		user.removeChannel(channel);
		
		if (channel.getUserCount() == 0) {
			channels.remove(channel.getName());
		}
	}
	
	public void kickEvent(User user, String channelName, User kickUser, String message)
	throws ErrorConditionException
	{
		Channel channel = channels.get(channelName);
		if (channel == null) {
			return;
		}
		
		kickUser = users.get(kickUser.getClient());
		
		Set<User> users = channel.getUsers();
		for (User targetUser : users) {
			PropagationProtocol propProtocol = user.getPropagationProtocol();
			propProtocol.kickedFromChannel(targetUser, user, channel, kickUser,
				message);
		}
		
		channel.removeUser(kickUser);
		kickUser.removeChannel(channel);
	}

	public void operEvent(User user)
	throws ErrorConditionException
	{
		user = users.get(user.getClient());

		user.setAttribute(UserAttribute.OPERATOR);
	}

	public void killEvent(User targetUser, User sourceUser, String quitmessage)
	{
		targetUser = users.get(targetUser.getClient());
		PropagationProtocol propProtocol = targetUser.getPropagationProtocol();

		propProtocol.quit(targetUser, sourceUser, quitmessage);

		targetUser.setQuitMessage(quitmessage);
		server.closeConnection(targetUser.getClient());
	}

	public void userAttributeEvent(User user, List<AttributeChange> userAttrs)
	throws ErrorConditionException
	{
		user = users.get(user.getClient());
		
		for (AttributeChange attrChange : userAttrs) {
			UserAttribute attr = (UserAttribute)attrChange.getAttribute();
			
			if (attrChange.isSet()) {
				user.setAttribute(attr);
			} else {
				user.removeAttribute(attr);
			}
		}
	}
	
	public void channelAttributeEvent(User user, Channel channel, 
		List<AttributeChange> channelAttrs)
	throws ErrorConditionException
	{
		channel = channels.get(channel.getName());
		
		for (AttributeChange attrChange : channelAttrs) {
			ChannelAttribute attr = (ChannelAttribute)attrChange.getAttribute();
			
			switch (attr) {
				case BAN:
					Channel.Ban ban = (Channel.Ban)attrChange.getParameter();
				
					if (attrChange.isSet()) {
						channel.addBan(ban);
					} else {
						channel.removeBan(ban);
					}
					continue;
			}
			
			if (attr.isMemberAttribute()) {
				String nick = (String)attrChange.getParameter();
				
				User changeUser = nicks.get(nick);
				if (changeUser == null) {
					continue;
				}
				
				ChannelMember member = channel.getUser(changeUser);
				if (attrChange.isSet()) {
					member.setAttribute(attr);
				} else {
					member.removeAttribute(attr);
				}
			} 
			else {
				AttributeParameter param = new AttributeParameter();
				param.setData((String)attrChange.getParameter());
				
				if (attrChange.isSet()) {
					channel.setAttribute(attr, param);
				} else {
					channel.removeAttribute(attr);
				}
			}
		}
		
		Set<User> users = channel.getUsers();
		for (User targetUser : users) {
			PropagationProtocol propProtocol = user.getPropagationProtocol();
			propProtocol.attributeChange(targetUser, user, channel, channelAttrs);
		}
	}
	
	public void quitEvent(User user, String message)
	{
		Client client = user.getClient();
		
		// User objects are read-only when we receive them from
		// an outside source. Lets retrieve a writable instance.
		user = users.get(user.getClient());
		user.setQuitMessage(message);
		
		server.closeConnection(client);
	}
	
	public void awayEvent(User user, String message)
	{
		// User objects are read-only when we receive them from
		// an outside source. Lets retrieve a writable instance.
		user = users.get(user.getClient());
		user.setAwayMessage(message);
		user.setAttribute(UserAttribute.AWAY);
	}
	
	public void notAwayEvent(User user)
	{
		// User objects are read-only when we receive them from
		// an outside source. Lets retrieve a writable instance.
		user = users.get(user.getClient());
		user.setAwayMessage(null);
		user.removeAttribute(UserAttribute.AWAY);
	}
	
	public void idleEvent(User user) {
		// User objects are read-only when we receive them from
		// an outside source. Lets retrieve a writable instance.
		user = users.get(user.getClient());
		user.setIdleSince(System.currentTimeMillis());
	}
}
