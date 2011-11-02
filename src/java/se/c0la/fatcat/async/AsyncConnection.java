package se.c0la.fatcat.async;

import java.net.InetSocketAddress;
import java.net.InetAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AsyncConnection implements Comparable<AsyncConnection>
{
    private InetSocketAddress host = null;

	private int seq;
	private SelectionKey key;
	
	private volatile boolean connected;

	private StringBuffer buffer;
	private List<String> outgoing;
    
	private long lastActivity = 0;
	private long lastHeartBeat = 0;
    
    private Object userObject;

	public AsyncConnection(int seq, SelectionKey key, InetSocketAddress host)
    {
        this.host = host;
		this.seq = seq;
		this.key = key;
		this.connected = true;
        this.lastActivity = System.currentTimeMillis();
	
		buffer = new StringBuffer();
		outgoing = new ArrayList<String>();
    }
    
	//
	// Public methods
	//
	
	public int getSeq() { return seq; }
	
	protected synchronized void setConnected(boolean v) { this.connected = v; }
	public synchronized boolean isConnected() { return connected; }
    
	public long getLastActivity() { return lastActivity; }
	public void setLastActivity(long v) { this.lastActivity = v; }
	
	public long getLastHeartBeat() { return lastHeartBeat; }
	public void setLastHeartBeat(long v) { this.lastHeartBeat = v; }
    
    public void setUserObject(Object v) { this.userObject = v; }
    public Object getUserObject() { return userObject; }
	
	public InetAddress getInetAddress()
	{
        return host.getAddress();
	}
    
    public InetSocketAddress getInetSocketAddress()
    {
        return host;
    }
	
	public String getHost()
	{
		InetAddress addr = getInetAddress();
		
		String host = addr.getHostName();
		if (host == null) {
			host = addr.getHostAddress();
		}
		
		return host;
	}
    
	//
	// Protected methods
	// - These methods should not be used outside of the async package,
	//   since it would break the abstraction and could be hazardous
	//   for the thread safety of the application.
	//
	
	protected SelectionKey getSelectionKey()
	{
		return key;
	}
	
	protected SocketChannel getChannel()
	{
		return (SocketChannel)key.channel();
	}
	
	protected StringBuffer getReadBuffer()
	{
		return buffer;
	}
	
	protected synchronized void addMessage(String message)
	{
		outgoing.add(message);
	}
	
	protected synchronized List<String> getMessages()
	{
		List<String> messages = outgoing;
		outgoing = new ArrayList<String>();
		
		return Collections.unmodifiableList(messages);
	}
	
	//
	// Standard methods
	//
	
	@Override
	public int compareTo(AsyncConnection conn)
	{
		return seq - conn.getSeq();
	}
	
	@Override
	public boolean equals(Object obj)
	{
		if (!(obj instanceof AsyncConnection)) {
			return false;
		}
		
		AsyncConnection b = (AsyncConnection)obj;
	
		return seq == b.getSeq();
	}
}
