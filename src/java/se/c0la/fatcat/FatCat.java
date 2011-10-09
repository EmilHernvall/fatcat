package se.c0la.fatcat;

import java.util.concurrent.*;
import se.c0la.fatcat.context.*;
import se.c0la.fatcat.async.*;

public class FatCat
{
	public static void main(String[] args)
	{
		try {
			AsyncServer server = new AsyncServer();
			
			ServerContext ctx = new ServerContext(server);
			
			ConfigReader config = new ConfigReader();
			config.setServerContext(ctx);
			config.setAsyncServer(server);
			config.parse("config.txt");
			config.validate();
			
			EventDispatcher dispatcher = new EventDispatcher(server, ctx);
			
			ExecutorService monitoringService = Executors.newSingleThreadExecutor();
			monitoringService.submit(new ConnectionMonitor(ctx));
			
			server.addClientListener(dispatcher);
			server.listen();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
}
