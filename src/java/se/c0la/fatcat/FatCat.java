package se.c0la.fatcat;

import java.util.concurrent.*;
import java.io.*;
import java.net.*;

import se.c0la.fatcat.irc.*;
import se.c0la.fatcat.context.*;
import se.c0la.fatcat.async.*;

public class FatCat
{
	public static void main(String[] args)
	{
		try {
			AsyncServer server = new AsyncServer();
			
			ServerContext ctx = new ServerContext(server);
			EventDispatcher dispatcher = new EventDispatcher(server, ctx);
			
			ExecutorService monitoringService = Executors.newSingleThreadExecutor();
			monitoringService.submit(new ConnectionMonitor(ctx));
			
			server.addClientListener(dispatcher);
			server.listen(new int[] { 6667, 6668 });
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
}
