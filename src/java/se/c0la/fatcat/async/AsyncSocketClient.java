package se.c0la.fatcat.async;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;

public class AsyncSocketClient extends AsyncBase
{
	private List<InetSocketAddress> hosts;

	public AsyncSocketClient()
	{
		super();
		
		hosts = new ArrayList<InetSocketAddress>();
	}
	
	public void addHost(String host, int port)
    throws IOException
	{
        addHost(new InetSocketAddress(host, port));
	}
	
    public void addHost(InetSocketAddress host)
    throws IOException
    {
		hosts.add(host);
        
        // handle late connections that are added after we've entered main
        // event loop.
        if (running) {
            registerConnection(host);
        }
    }
    
	@Override
	protected void initialize()
	throws IOException
	{
		for (InetSocketAddress host : hosts) {
			System.out.println("Connecting to " + host);
			registerConnection(host);
		}
	}
    
    protected void registerConnection(InetSocketAddress host)
    throws IOException
    {
        SocketChannel socketChannel = SocketChannel.open();
        socketChannel.configureBlocking(false);
        socketChannel.connect(host);
        
        SelectionKey key = socketChannel.register(socketSelector, SelectionKey.OP_CONNECT);
        
		AsyncConnection conn = new AsyncConnection(connSequence++, key, host);
		key.attach(conn);
    }
	
	@Override
	protected void connect(SelectionKey key)
	{
        AsyncConnection conn = (AsyncConnection)key.attachment();
    
		SocketChannel channel = (SocketChannel)key.channel();
		try {
			channel.finishConnect();
		}
		catch (IOException e) {
			// Notify listeners about disconnection.
			for (AsyncConnectionListener listener : listeners) {
				listener.connectionFailed(conn.getInetSocketAddress());
			}
			key.cancel();
			return;
		}
		
		key.interestOps(SelectionKey.OP_READ);
        
		connections.add(conn);
		
		// Notify listeners
		for (AsyncConnectionListener listener : listeners) {
			listener.connected(conn);
		}
	}
}
