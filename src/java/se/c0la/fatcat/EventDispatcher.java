package se.c0la.fatcat;

import se.c0la.fatcat.context.*;
import se.c0la.fatcat.async.*;

public class EventDispatcher implements AsyncClientListener
{
	private AsyncServer server;
	private ServerContext ctx;
    private Protocol defaultProtocol;

	public EventDispatcher(AsyncServer server, ServerContext ctx)
	{
		this.server = server;
		this.ctx = ctx;
	}
    
    public void setDefaultProtocol(Protocol protocol)
    {
        this.defaultProtocol = protocol;
    }

	@Override
	public void connected(AsyncClient asyncClient)
	{
        SocketClient client = new SocketClient(server, asyncClient);
        asyncClient.setUserObject(client);
        
		ctx.userConnectedEvent(client, defaultProtocol);
	}

	@Override
	public void messageReceived(AsyncClient asyncClient, String message)
	{
        SocketClient client = (SocketClient)asyncClient.getUserObject();
		User user = ctx.getUser(client);

		/* Silently drop empty messages */
		if(message.length() == 0)
			return;

		if (user == null) {
			return;
		}

		ReceiverProtocol protocol = user.getReceiverProtocol();
		protocol.translateMessage(user, message);
	}

	@Override
	public void disconnected(AsyncClient asyncClient)
	{
        SocketClient client = (SocketClient)asyncClient.getUserObject();
		ctx.userDisconnectedEvent(client);
	}
}
