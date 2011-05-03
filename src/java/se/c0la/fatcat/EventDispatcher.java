package se.c0la.fatcat;

import java.util.*;
import java.io.*;
import java.nio.channels.*;
import java.net.*;

import se.c0la.fatcat.irc.*;
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
