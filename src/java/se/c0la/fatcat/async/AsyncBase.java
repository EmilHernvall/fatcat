package se.c0la.fatcat.async;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListSet;

public abstract class AsyncBase
{
	private final static int BUFFER_SIZE = 8092;
	
    private int timeout;
    
	protected List<AsyncConnectionListener> listeners;

	protected Selector socketSelector = null;
	private ByteBuffer readBuffer = ByteBuffer.allocate(BUFFER_SIZE);
	
	protected Set<AsyncConnection> connections;
	private Queue<AsyncConnection> writeQueue;
	private Queue<AsyncConnection> disconnectQueue;
	
	protected int connSequence;
	protected volatile boolean running;

	public AsyncBase()
	{
		listeners = new ArrayList<AsyncConnectionListener>();
		connections = new ConcurrentSkipListSet<AsyncConnection>();
		writeQueue = new ConcurrentLinkedQueue<AsyncConnection>();
		disconnectQueue = new ConcurrentLinkedQueue<AsyncConnection>();
		
        timeout = 0;
		connSequence = 0;
		running = false;
	}
    
    public void setTimeout(int milliSeconds)
    {
        this.timeout = milliSeconds;
    }
	
	public void addConnectionListener(AsyncConnectionListener listener)
	{
		listeners.add(listener);
	}
	
	public void removeConnectionListener(AsyncConnectionListener listener)
	{
		listeners.remove(listener);
	}
	
	public Set<AsyncConnection> getConnections()
	{
		return Collections.unmodifiableSet(connections);
	}
	
	public void sendMessage(AsyncConnection conn, String message)
	{
		conn.addMessage(message);
		writeQueue.offer(conn);
		socketSelector.wakeup();
	}
	
	public void closeConnection(AsyncConnection conn)
	{
		disconnectQueue.add(conn);
		socketSelector.wakeup();
	}
	
	public void shutdown()
	{
		this.running = false;
		socketSelector.wakeup();
	}
	
	protected abstract void initialize()
	throws IOException;
    
    protected void deinitialize()
    throws IOException
    {
        socketSelector.close();
    }
	
	public void listen()
	throws IOException
	{
		if (socketSelector != null) {
			throw new IllegalStateException("Server has already been initialized.");
		}
	
		// Retrieve a selector
		SelectorProvider provider = SelectorProvider.provider();
		this.socketSelector = provider.openSelector();
		
		// Initialize all channels
		initialize();
		
		// Main loop
        this.running = true;
		while (this.running) {
			// Check the list of connections with pending writes
			// and set them to write mode
			AsyncConnection conn;
			while ((conn = writeQueue.poll()) != null) {
				try {
					SelectionKey key = conn.getSelectionKey();
					key.interestOps(SelectionKey.OP_WRITE);
				}
				catch (CancelledKeyException e) {
					// Suppressed
					continue;
				}
			}
			
            if (timeout != 0) {
                // If timeout is enabled we have to wake up every now
                // and then to check for them.
                socketSelector.select(timeout/10);
            } else {
                // Otherwise we block continously until we receive an event
                socketSelector.select(timeout);
            }
			
			// Iterate through all events. Keys have to be
			// removed explicitly, so we use an iterator.
			Set<SelectionKey> keys = socketSelector.selectedKeys();
			Iterator<SelectionKey> iterator = keys.iterator();
			for ( ; iterator.hasNext(); ) {
				SelectionKey key = iterator.next();
				if (!key.isValid()) {
					continue;
				}
				
				iterator.remove();
				
				// Handle new connections
				if (key.isAcceptable()) {
					accept(key);
				}
				else if (key.isConnectable()) {
					connect(key);
				}
				// Handle incoming data
				else if (key.isReadable()) {
					read(key);
				}
				// Write outgoing data
				else if (key.isWritable()) {
					write(key);
				}
			}

            // Handle timeouts
            if (timeout != 0) {
                for (AsyncConnection cur : connections) {
                    long diff = System.currentTimeMillis() - cur.getLastActivity();
                    if (diff < timeout) {
                        continue;
                    }
                    
                    disconnect(cur);
                }
            }
            
			// Handle any connections in the disconnect queue
			while ((conn = disconnectQueue.poll()) != null) {
				disconnect(conn);
			}
		}
        
        deinitialize();
	}
	
	protected void connect(SelectionKey key)
	throws IOException
	{
	}
	
	protected void accept(SelectionKey key)
	throws IOException
	{
	}
	
	private void read(SelectionKey key)
	throws IOException
	{
		AsyncConnection conn = (AsyncConnection)key.attachment();
        
		SocketChannel clientChannel = conn.getChannel();
		
		readBuffer.clear();
		
		int numRead;
		try {
			numRead = clientChannel.read(readBuffer);
		}
		catch (IOException e) {
			key.cancel();
			clientChannel.close();
			return;
		}
		
		// Connection closed
		if (numRead == -1) {
			disconnect(conn);
			return;
		}
		
		// The buffer is used to store data we have already
		// received that didn't contain any line break
		StringBuffer buffer = conn.getReadBuffer();
		
		// Merge stored data with the current and clear the
		// buffer so we can reuse it
		buffer.append(new String(readBuffer.array(), 0, numRead, "ISO-8859-1"));
		String data = buffer.toString();
		buffer.delete(0, buffer.length());
		
		// Process one line at a time
		int idx;
		while ((idx = data.indexOf('\n')) != -1) {
			String line = data.substring(0, idx).trim();
			data = data.substring(idx + 1);
			
			// Notify listeners
			for (AsyncConnectionListener listener : listeners) {
				listener.messageReceived(conn, line);
			}
		}
		
		// All remaining data is put in the buffer
		if (data.length() > 0) {
			buffer.append(data);
		}
        
		conn.setLastActivity(System.currentTimeMillis());
		conn.setLastHeartBeat(System.currentTimeMillis());
	}
	
	private void write(SelectionKey key)
	throws IOException
	{
		AsyncConnection conn = (AsyncConnection)key.attachment();
       
		SocketChannel clientChannel = conn.getChannel();
		
		try {
			List<String> messages = conn.getMessages();
			for (String message : messages) {
				message = message + "\n";
				ByteBuffer writeBuffer = ByteBuffer.wrap(message.getBytes());
				while (writeBuffer.remaining() > 0) {
					clientChannel.write(writeBuffer);
				}
			}
			
			// Restore the channel to read mode
			key.interestOps(SelectionKey.OP_READ);
		}
		catch (IOException e) {
			disconnect(conn);
			return;
		}
	}
	
	private void disconnect(AsyncConnection conn)
	{
		connections.remove(conn);
		conn.setConnected(false);
	
		try {
			SelectionKey key = conn.getSelectionKey();
			key.cancel();
			
			SocketChannel channel = conn.getChannel();
			channel.close();
		}
		catch (IOException e) {
			// If this fails, it's probably because we're
			// already disconnected. Lets assume that everything
			// is fine.
		}

		// Notify listeners
		for (AsyncConnectionListener listener : listeners) {
			listener.disconnected(conn);
		}
	}
}
