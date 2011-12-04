package se.c0la.fatcat.context;

import java.util.*;

import se.c0la.fatcat.*;
import se.c0la.fatcat.async.*;

public class User
{
	private volatile Client client;
	private volatile Protocol protocol;
	
	private volatile boolean hasRegistered = false;

	private volatile String nick;
	private volatile String user;
	private volatile String host;
	private volatile String realName;
    private volatile String password;
	
	private volatile Set<Channel> channels;
	private volatile EnumSet<UserAttribute> attributes;
	
	private volatile String quitMessage = null;
	private volatile String awayMessage = null;
	private volatile long idleSince;
	
	public User(User old)
	{
		this.client = old.getClient();
		this.protocol = old.getProtocol();
		
		this.hasRegistered = old.hasRegistered();
		
		this.nick = old.getNick();
		this.user = old.getUser();
		this.host = client.getHost();
		this.realName = old.getRealName();
		this.quitMessage = old.getQuitMessage();
		this.awayMessage = old.getAwayMessage();
		
		this.channels = new HashSet<Channel>(old.channels);
		this.attributes = old.attributes.clone();
		this.idleSince = old.idleSince;
	}

	public User(Client client)
	{
		this.client = client;
		this.host = client.getHost();
		
		channels = new HashSet<Channel>();
		attributes = EnumSet.noneOf(UserAttribute.class);
	}
	
	public Client getClient() { return client; }
	
	public Protocol getProtocol()  { return protocol; }
	public void setProtocol(Protocol v)  { this.protocol = v; }
	
	public ReceiverProtocol getReceiverProtocol() 
	{
		return protocol.getReceiverProtocol(); 
	}
	
	public PropagationProtocol getPropagationProtocol() 
	{ 
		return protocol.getPropagationProtocol(); 
	}

	public boolean hasRegistered() {  return hasRegistered; }
	public void setRegistered() { this.hasRegistered = true; }
	
	public String getNick() { return nick; }
	public void setNick(String v) { this.nick = v; }
	
	public String getUser() { return user; }
	public void setUser(String v) { this.user = v; }
	
	public String getRealName() { return realName; }
	public void setRealName(String v) { this.realName = v; }

    public String getPassword() { return password; }
    public void setPassword(String v) { this.password = v; }
	
	public String getHost() { return host; }
	
	public String getQuitMessage() { return quitMessage; }
	public void setQuitMessage(String v) { this.quitMessage = v; }
	
	public String getAwayMessage() { return awayMessage; }
	public void setAwayMessage(String v) { this.awayMessage = v; }
	
	public long getIdleSince() { return idleSince; }
	public void setIdleSince(long v) { this.idleSince = v; }
	
	public Set<Channel> getChannels()
	{
		return Collections.unmodifiableSet(channels);
	}
	
	public void addChannel(Channel channel)
	{
		channels.add(channel);
	}
	
	public void removeChannel(Channel channel)
	{
		channels.remove(channel);
	}
	
	public void setAttribute(UserAttribute attr)
	{
		attributes.add(attr);
	}
	
	public boolean getAttribute(UserAttribute attr)
	{
		return attributes.contains(attr);
	}
	
	public void removeAttribute(UserAttribute attr)
	{
		attributes.remove(attr);
	}
	
	public Set<UserAttribute> getAttributes()
	{
		return Collections.unmodifiableSet(attributes);
	}
	
	public long getIdleTime() {
		return (System.currentTimeMillis() - idleSince) / 1000;
	}
	
	@Override
	public int hashCode()
	{
		return nick.hashCode();
	}
	
	@Override
	public boolean equals(Object obj)
	{
		if (!(obj instanceof User)) {
			return false;
		}
		
		User b = (User)obj;
		if (nick == null) {
			return false;
		}
		
		return nick.equals(b.getNick());
	}
	
	@Override
	public String toString()
	{
		if (user == null) {
			return String.format("%s@%s", nick, host);
		}
	
		return String.format("%s!%s@%s", nick, user, host);
	}
}
