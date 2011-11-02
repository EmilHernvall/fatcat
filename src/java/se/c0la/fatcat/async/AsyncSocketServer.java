package se.c0la.fatcat.async;

import java.util.List;
import java.util.ArrayList;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

public class AsyncSocketServer extends AsyncBase
{
	private List<Integer> ports;
    private List<ServerSocket> sockets = null;

	public AsyncSocketServer()
	{
		super();
        
        this.ports = new ArrayList<Integer>();
        this.sockets = new ArrayList<ServerSocket>();
	}
	
	public void addListenPort(int port)
	{
		ports.add(port);
	}
	
	@Override
	protected void initialize()
	throws IOException
	{
		for (int port : ports) {
			ServerSocketChannel serverChannel = ServerSocketChannel.open();
			serverChannel.configureBlocking(false);
			
			ServerSocket socket = serverChannel.socket();
            socket.setReuseAddress(true);
			socket.bind(new InetSocketAddress(port));
            sockets.add(socket);
			
			serverChannel.register(socketSelector, SelectionKey.OP_ACCEPT);
		}
	}
    
    @Override
    protected void deinitialize()
    throws IOException
    {
        for (ServerSocket socket : sockets) {
            socket.close();
        }
        super.deinitialize();
    }
	
	@Override
	protected void accept(SelectionKey key)
	throws IOException
	{
		ServerSocketChannel serverChannel = (ServerSocketChannel)key.channel();
	
		SocketChannel clientChannel = serverChannel.accept();
		clientChannel.configureBlocking(false);
	
		// We're only interested in read events
		clientChannel.register(socketSelector, SelectionKey.OP_READ);
		
		// Retrieve the SelectionKey and associate it with 
		// a new AsyncConnection object
		SelectionKey newKey = clientChannel.keyFor(socketSelector);

		Socket socket = clientChannel.socket();
		InetSocketAddress host = new InetSocketAddress(socket.getInetAddress(), socket.getPort());
        
		AsyncConnection conn = new AsyncConnection(connSequence++, newKey, host);
		connections.add(conn);
		newKey.attach(conn);
		
		// Notify listeners
		for (AsyncConnectionListener listener : listeners) {
			listener.connected(conn);
		}
	}
}
