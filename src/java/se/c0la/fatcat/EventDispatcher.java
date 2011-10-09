package se.c0la.fatcat;

import se.c0la.fatcat.context.*;
import se.c0la.fatcat.async.*;

public class EventDispatcher implements ClientListener
{
	private AsyncServer server;
	private ServerContext ctx;

	public EventDispatcher(AsyncServer server, ServerContext ctx)
	{
		this.server = server;
		this.ctx = ctx;
	}

	@Override
	public void connected(Client client)
	{
		ctx.userConnectedEvent(client);
	}

	@Override
	public void messageReceived(Client client, String message)
	{
		User user = ctx.getUser(client);

		/* Silently drop empty messages */
		if(message.length() == 0)
			return;

		System.out.println("raw: " + message);
		if (user == null) {
			return;
		}

		ReceiverProtocol protocol = user.getReceiverProtocol();
		protocol.translateMessage(user, message);
	}

	@Override
	public void disconnected(Client client)
	{
		ctx.userDisconnectedEvent(client);
	}
}
