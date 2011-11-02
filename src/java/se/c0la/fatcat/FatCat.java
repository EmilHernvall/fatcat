package se.c0la.fatcat;

import java.util.concurrent.*;
import se.c0la.fatcat.context.*;
import se.c0la.fatcat.async.*;
import se.c0la.fatcat.irc.*;

public class FatCat
{
	public static void main(String[] args)
	{
		try {
            if (args.length > 0) {
                if ("genpw".equals(args[0]) && args.length > 1) {
                    String pw = args[1];
                    System.out.println(Operator.getHash(pw));
                }
                
                return;
            }
        
			AsyncSocketServer server = new AsyncSocketServer();
			ServerContext ctx = new ServerContext(server);
			
			ConfigReader config = new ConfigReader();
			config.setServerContext(ctx);
			config.setAsyncServer(server);
			config.parse("config.txt");
			config.validate();
			
			EventDispatcher dispatcher = new EventDispatcher(server, ctx);
            dispatcher.setDefaultProtocol(new IRCProtocol(ctx));
			server.addConnectionListener(dispatcher);
			
			server.listen();
            
			ExecutorService monitoringService = Executors.newSingleThreadExecutor();
			monitoringService.submit(new ConnectionMonitor(ctx));
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
}
