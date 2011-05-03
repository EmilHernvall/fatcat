package se.c0la.fatcat.context;

import java.util.*;

import se.c0la.fatcat.*;
import se.c0la.fatcat.async.*;

public class Channel
{
	public static class Ban
	{
		private String nickMask;
		private String userMask;
		private String hostMask;
		
		private long timestamp;
	
		public Ban(String nickMask, String userMask, String hostMask, long timestamp)
		{
			this.nickMask = nickMask;
			this.userMask = userMask;
			this.hostMask = hostMask;
			
			this.timestamp = timestamp;
		}
		
		public String getNickMask() { return nickMask; }
		public String getUserMask() { return userMask; }
		public String getHostMask() { return hostMask; }
		
		public long getTimestamp() { return timestamp; }
		
		public boolean matches(User user)
		{
			String nick = user.getNick();
			if (!nick.matches(nickMask.replace("*", "(.*)"))) {
				return false;
			}
		
			String userName = user.getUser();
			if (!userName.matches(userMask.replace("*", "(.*)"))) {
				return false;
			}
			
			String host = user.getHost();
			if (!host.matches(hostMask.replace("*", "(.*)"))) {
				return false;
			}
			
			return true;
		}
		
		@Override
		public String toString()
		{
			return String.format("%s!%s@%s", nickMask, userMask, hostMask);
		}
		
		@Override
		public int hashCode()
		{
			int code = 17;
			code = 31 * code + nickMask.hashCode();
			code = 31 * code + userMask.hashCode();
			code = 31 * code + hostMask.hashCode();
			
			return code;
		}
		
		@Override
		public boolean equals(Object obj)
		{
			if (!(obj instanceof Ban)) {
				return false;
			}
			
			Ban b = (Ban)obj;
			return nickMask.equals(b.getNickMask())
				&& userMask.equals(b.getUserMask())
				&& hostMask.equals(b.getHostMask());
		}
	}

	private String name;
	private String topic = "";
	private String topicChanger = "";
	private int topicTime = 0;

	protected Map<ChannelAttribute, AttributeParameter> attributes;
	protected Map<User, ChannelMember> users;
	protected List<Ban> bans;
	protected Set<String> currentInvites;

	public Channel(Channel old)
	{
		this.name = old.getName();
		this.topic = old.getTopic();
		this.topicChanger = old.getTopicChanger();
		this.topicTime = old.getTopicTime();
		
		this.currentInvites = old.getInvites();
		
		this.attributes = new EnumMap<ChannelAttribute, AttributeParameter>(old.attributes);
		this.users = new HashMap<User, ChannelMember>(old.users);
		this.bans = new ArrayList<Ban>(old.bans);
	}

	public Channel(String name)
	{
		this.name = name;
		
		attributes = new EnumMap<ChannelAttribute, AttributeParameter>(ChannelAttribute.class);
		users = new HashMap<User, ChannelMember>();
		bans = new ArrayList<Ban>();
		currentInvites = new HashSet<String>();
	}

	public String getName() { return name; }
	
	public String getTopic() { return this.topic; }
	public String getTopicChanger() { return this.topicChanger; }
	public int getTopicTime() { return this.topicTime; }

	public Set<User> getUsers()
	{
		return Collections.unmodifiableSet(users.keySet());
	}
	
	public int getUserCount()
	{
		return users.size();
	}

	public void addUser(User user)
	{
		ChannelMember member = new ChannelMember();
		users.put(user, member);
	}

	public void removeUser(User user)
	{
		users.remove(user);
	}
	
	public ChannelMember getUser(User user)
	{
		return users.get(user);
	}

	public void setTopic(String topic, String nick)
	{
		this.topic = topic;
		this.topicChanger = nick;
		this.topicTime = (int)(System.currentTimeMillis()/1000);
	}
	
	public void addInvite(String nick)
	{
		currentInvites.add(nick);
		
		System.out.println(nick+" "+currentInvites.contains(nick));
	}
	
	public boolean getInvite(String nick)
	{
		return currentInvites.contains(nick);
	}
	
	public Set<String> getInvites()
	{
		return currentInvites;
	}
	
	public void removeInvite(String nick)
	{
		currentInvites.remove(nick);
	}

	public void setAttribute(ChannelAttribute attr, AttributeParameter param)
	{
		if (attr.isMemberAttribute()) {
			throw new IllegalArgumentException("Cannot use channel member attributes on channel.");
		}
	
		attributes.put(attr, param);
	}
	
	public AttributeParameter getAttribute(ChannelAttribute attr)
	{
		if (attr.isMemberAttribute()) {
			throw new IllegalArgumentException("Cannot use channel member attributes on channel.");
		}
	
		return attributes.get(attr);
	}
	
	public void removeAttribute(ChannelAttribute attr)
	{
		if (attr.isMemberAttribute()) {
			throw new IllegalArgumentException("Cannot use channel member attributes on channel.");
		}
	
		attributes.remove(attr);
	}
	
	public Map<ChannelAttribute, AttributeParameter> getAttributes()
	{
		return Collections.unmodifiableMap(attributes);
	}
	
	public void addBan(Ban mask)
	{
		bans.add(mask);
	}
	
	public void removeBan(Ban mask)
	{
		bans.remove(mask);
	}
	
	public List<Ban> getBans()
	{
		return new ArrayList<Ban>(bans);
	}
}
