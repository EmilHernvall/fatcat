package se.c0la.fatcat;

import java.net.InetSocketAddress;

import se.c0la.fatcat.context.*;
import se.c0la.fatcat.async.*;

public class EventDispatcher implements AsyncConnectionListener
{
	private AsyncSocketServer server;
	private ServerContext ctx;
    private Protocol defaultProtocol;

	public EventDispatcher(AsyncSocketServer server, ServerContext ctx)
	{
		this.server = server;
		this.ctx = ctx;
	}
    
    public void setDefaultProtocol(Protocol protocol)
    {
        this.defaultProtocol = protocol;
    }
    
    @Override
    public void connectionFailed(InetSocketAddress host)
    {
    }

	@Override
	public void connected(AsyncConnection conn)
	{
        Client client = new SocketClient(server, conn);
        conn.setUserObject(client);
        
		ctx.userConnectedEvent(client, defaultProtocol);
	}

	@Override
	public void messageReceived(AsyncConnection conn, String message)
	{
		// Silently drop empty messages
		if (message.length() == 0) {
			return;
        }
    
        Client client = (Client)conn.getUserObject();
		User user = ctx.getUser(client);
		if (user == null) {
			return;
		}

		ReceiverProtocol protocol = user.getReceiverProtocol();

        // Top level exception handler for network events
        try {
    		protocol.translateMessage(user, message);
        } catch (Exception e) {
            e.printStackTrace();
        }
	}

	@Override
	public void disconnected(AsyncConnection conn)
	{
        Client client = (Client)conn.getUserObject();
		ctx.userDisconnectedEvent(client);
	}
}
