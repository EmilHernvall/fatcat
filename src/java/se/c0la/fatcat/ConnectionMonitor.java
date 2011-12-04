package se.c0la.fatcat;

import java.util.*;
import java.io.*;
import java.net.*;

import se.c0la.fatcat.irc.*;
import se.c0la.fatcat.context.*;
import se.c0la.fatcat.async.*;

/**
 * Monitor all active connections to the server. Send heart beats to
 * idle clients, and disconnect the ones that doesn't respond.
 */
public class ConnectionMonitor implements Runnable
{
	private static final long PING_INTERVAL = 120000L;
	private static final long TIMEOUT_INTERVAL = 120000L;

	private ServerContext ctx;
	private AsyncServer server;

	public ConnectionMonitor(ServerContext ctx)
	{
		this.ctx = ctx;
		this.server = ctx.getServer();
	}
	
	public void run()
	{
		while (true) {
			try {
				Set<AsyncClient> clients = server.getClients();
				
				long diff = 0;
				for (AsyncClient client : clients) {
				
					diff = System.currentTimeMillis() - client.getLastActivity();
					if (diff > PING_INTERVAL + TIMEOUT_INTERVAL) {
						pingTimeout(client);
						continue;
					}
					
					diff = System.currentTimeMillis() - client.getLastHeartBeat();
					if (diff > PING_INTERVAL) {
						sendPing(client);
						continue;
					}
				}

				Thread.sleep(1000);
			} 
			catch (InterruptedException e) { 
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	private void sendPing(AsyncClient asyncClient)
	{	
        Client client = (Client)asyncClient.getUserObject();
		User user = ctx.getUser(client);
		if (user == null) {
			return;
		}
		
		asyncClient.setLastHeartBeat(System.currentTimeMillis());
		
		PropagationProtocol propProtocol = user.getPropagationProtocol();
		propProtocol.sendHeartBeat(user);
	}
	
	private void pingTimeout(AsyncClient asyncClient)
	{
        Client client = (Client)asyncClient.getUserObject();
		ctx.setQuitMessage(client, "Ping timeout");
		client.closeConnection();
	}
}
