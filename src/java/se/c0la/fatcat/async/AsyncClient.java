package se.c0la.fatcat.async;

import java.util.*;
import java.net.*;
import java.io.*;
import java.nio.channels.*;

public class AsyncClient implements Comparable<AsyncClient>
{
	private int seq;
	private SelectionKey key;
	
	private long lastActivity = 0;
	private long lastHeartBeat = 0;

	private StringBuffer buffer;
	private List<String> outgoing;
    
    private Object userObject;

	public AsyncClient(int seq, SelectionKey key)
	{
		this.seq = seq;
		this.key = key;
	
		buffer = new StringBuffer();
		outgoing = new ArrayList<String>();
		
		lastActivity = System.currentTimeMillis();
	}
	
	//
	// Public methods
	//
	
	public int getSeq() { return seq; }
	
	public long getLastActivity() { return lastActivity; }
	public void setLastActivity(long v) { this.lastActivity = v; }
	
	public long getLastHeartBeat() { return lastHeartBeat; }
	public void setLastHeartBeat(long v) { this.lastHeartBeat = v; }
    
    public void setUserObject(Object v) { this.userObject = v; }
    public Object getUserObject() { return userObject; }
	
	public String getHost()
	{
		SocketChannel channel = (SocketChannel)key.channel();
		Socket socket = channel.socket();
		InetAddress addr = socket.getInetAddress();
		
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
	public int compareTo(AsyncClient client)
	{
		return seq - client.getSeq();
	}
	
	@Override
	public boolean equals(Object obj)
	{
		if (!(obj instanceof AsyncClient)) {
			return false;
		}
		
		AsyncClient b = (AsyncClient)obj;
	
		return seq == b.getSeq();
	}
}
