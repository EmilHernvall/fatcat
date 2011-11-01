package se.c0la.fatcat.async;

import java.util.*;
import java.util.concurrent.*;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.channels.spi.*;
import java.net.*;

public class AsyncServer
{
	private final static int BUFFER_SIZE = 8092;
	
	private List<AsyncClientListener> listeners;

	private Selector socketSelector = null;
    //private ServerSocketChannel serverChannel = null;
    private List<ServerSocket> sockets = null;
	private ByteBuffer readBuffer = ByteBuffer.allocate(BUFFER_SIZE);
	
	private Set<AsyncClient> clients;
	private Queue<AsyncClient> writeQueue;
	private Queue<AsyncClient> disconnectQueue;
	
	private int clientSequence;
    
    private volatile boolean running = false;

	public AsyncServer()
	{
        sockets = new ArrayList<ServerSocket>();
		listeners = new ArrayList<AsyncClientListener>();
		clients = new ConcurrentSkipListSet<AsyncClient>();
		writeQueue = new ConcurrentLinkedQueue<AsyncClient>();
		disconnectQueue = new ConcurrentLinkedQueue<AsyncClient>();
		
		clientSequence = 0;
	}
	
	public void addClientListener(AsyncClientListener listener)
	{
		listeners.add(listener);
	}
	
	public void removeClientListener(AsyncClientListener listener)
	{
		listeners.remove(listener);
	}
	
	public Set<AsyncClient> getClients()
	{
		return Collections.unmodifiableSet(clients);
	}
	
	public void sendMessage(AsyncClient client, String message)
	{
		client.addMessage(message);
		writeQueue.offer(client);
		socketSelector.wakeup();
	}
	
	public void closeConnection(AsyncClient client)
	{
		disconnectQueue.add(client);
		socketSelector.wakeup();
	}
    
    public void shutdown()
    {
        running = false;
        disconnectQueue.addAll(clients);
        socketSelector.wakeup();
    }
	
	public void listen(int port)
	throws IOException
	{
		listen(new int[] { port });
	}
	
	public void listen(int[] ports)
	throws IOException
	{
		if (socketSelector != null) {
			throw new IllegalStateException("Server has already been initialized.");
		}
	
		// Retrieve a selector
		SelectorProvider provider = SelectorProvider.provider();
		this.socketSelector = provider.openSelector();
		
		// Initialize all channels
		for (int port : ports) {
			ServerSocketChannel serverChannel = ServerSocketChannel.open();
			serverChannel.configureBlocking(false);
			
			ServerSocket socket = serverChannel.socket();
            socket.setReuseAddress(true);
			socket.bind(new InetSocketAddress(port));
            sockets.add(socket);
			
			serverChannel.register(socketSelector, SelectionKey.OP_ACCEPT);
		}
        
		// Main loop
        running = true;
		while (running) {
		
			// Check the list of clients with pending writes
			// and set them to write mode
			AsyncClient client;
			while ((client = writeQueue.poll()) != null) {
				try {
					SelectionKey key = client.getSelectionKey();
					key.interestOps(SelectionKey.OP_WRITE);
				}
				catch (CancelledKeyException e) {
					// Suppressed
					continue;
				}
			}
			
			// Block until we receive an event
			socketSelector.select();
			
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
				// Handle incoming data
				else if (key.isReadable()) {
					read(key);
				}
				// Write outgoing data
				else if (key.isWritable()) {
					write(key);
				}
			}

			// Handle any clients in the disconnect queue
			while ((client = disconnectQueue.poll()) != null) {
				disconnect(client);
			}
		}
        
        for (ServerSocket socket : sockets) {
            socket.close();
        }
        //serverChannel.close();
        socketSelector.close();
	}
	
	private void accept(SelectionKey key)
	throws IOException
	{
		ServerSocketChannel serverChannel = (ServerSocketChannel)key.channel();
	
		SocketChannel clientChannel = serverChannel.accept();
		clientChannel.configureBlocking(false);
	
		// We're only interested in read events
		clientChannel.register(socketSelector, SelectionKey.OP_READ);
		
		// Retrieve the SectionKey and associate it with 
		// a new Client object
		SelectionKey newKey = clientChannel.keyFor(socketSelector);
		AsyncClient client = new AsyncClient(clientSequence++, newKey);
		clients.add(client);
		newKey.attach(client);
        
		// Notify listeners
		for (AsyncClientListener listener : listeners) {
			listener.connected(client);
		}
		
		client.setLastActivity(System.currentTimeMillis());
		client.setLastHeartBeat(System.currentTimeMillis());
	}
	
	private void read(SelectionKey key)
	throws IOException
	{
		AsyncClient client = (AsyncClient)key.attachment();
		SocketChannel clientChannel = client.getChannel();
		
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
			disconnect(client);
			return;
		}
		
		// The buffer is used to store data we have already
		// received that didn't contain any line break
		StringBuffer buffer = client.getReadBuffer();
		
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
			for (AsyncClientListener listener : listeners) {
				listener.messageReceived(client, line);
			}
		}
		
		// All remaining data is put in the buffer
		if (data.length() > 0) {
			buffer.append(data);
		}
		
		client.setLastActivity(System.currentTimeMillis());
		client.setLastHeartBeat(System.currentTimeMillis());
	}
	
	private void write(SelectionKey key)
	throws IOException
	{
		AsyncClient client = (AsyncClient)key.attachment();
		SocketChannel clientChannel = client.getChannel();
		
		List<String> messages = client.getMessages();
		for (String message : messages) {
			message = message + "\r\n";
			ByteBuffer writeBuffer = ByteBuffer.wrap(message.getBytes());
			while (writeBuffer.remaining() > 0) {
				clientChannel.write(writeBuffer);
			}
		}
		
		// Restore the channel to read mode
		key.interestOps(SelectionKey.OP_READ);
	}
	
	private void disconnect(AsyncClient client)
	{
		clients.remove(client);
	
		try {
			SelectionKey key = client.getSelectionKey();
			key.cancel();
			
			SocketChannel channel = client.getChannel();
			channel.close();
		}
		catch (IOException e) {
			// If this fails, it's probably because we're
			// already disconnected. Lets assume that everything
			// is fine.
		}

		// Notify listeners
		for (AsyncClientListener listener : listeners) {
			listener.disconnected(client);
		}
	}
}
