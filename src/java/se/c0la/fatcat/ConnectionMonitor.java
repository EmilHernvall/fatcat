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
				Set<AsyncConnection> clients = server.getClients();
				
				long diff = 0;
				for (AsyncConnection conn : clients) {
				
					diff = System.currentTimeMillis() - conn.getLastActivity();
					if (diff > PING_INTERVAL + TIMEOUT_INTERVAL) {
						pingTimeout(conn);
						continue;
					}
					
					diff = System.currentTimeMillis() - conn.getLastHeartBeat();
					if (diff > PING_INTERVAL) {
						sendPing(conn);
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
	
	private void sendPing(AsyncConnection conn)
	{	
        Client client = (Client)conn.getUserObject();
		User user = ctx.getUser(client);
		if (user == null) {
			return;
		}
		
		conn.setLastHeartBeat(System.currentTimeMillis());
		
		PropagationProtocol propProtocol = user.getPropagationProtocol();
		propProtocol.sendHeartBeat(user);
	}
	
	private void pingTimeout(AsyncConnection conn)
	{
        Client client = (Client)conn.getUserObject();
		ctx.setQuitMessage(client, "Ping timeout");
		client.closeConnection();
	}
}
