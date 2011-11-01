package se.c0la.fatcat;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;


import se.c0la.fatcat.async.AsyncServer;
import se.c0la.fatcat.context.Operator;
import se.c0la.fatcat.context.ServerContext;
import se.c0la.fatcat.irc.MessageTokenizer;

public class ConfigReader {

	private ServerContext ctx;
	private AsyncServer server;
	private List<String[]> foundSettings;

	public ConfigReader() {
		foundSettings = new ArrayList<String[]>();
	}

	public ServerContext getServerContext() { return ctx; }
	public void setServerContext(ServerContext ctx) {
		this.ctx = ctx;
	}

	public AsyncServer getAsyncServer() { return server; }
	public void setAsyncServer(AsyncServer server) {
		this.server = server;
	}

	public void parse(String configFile)
	throws ConfigException {
		File file = new File(configFile);
		String buf;
		String token[];

		if(!file.exists())
			throw new ConfigException(configFile + " does not exist.");

		if(!file.isFile())
			throw new ConfigException(configFile + " is not a file.");

		if(!file.canRead())
			throw new ConfigException(configFile + " is not readable by me.");

		try {
			BufferedReader bis = new BufferedReader(new FileReader(file));
			while(bis.ready()) {
				buf = bis.readLine();

				if(buf.startsWith("#"))
					continue;

				token = MessageTokenizer.tokenize(buf);
				foundSettings.add(token);
			}
			
		} catch(FileNotFoundException e) {
			System.err.println(e.getMessage());
			// Already handled
		} catch (IOException e) {
			e.printStackTrace();
			throw new ConfigException("IO error: " + e.getMessage());
		}
	}
	
	public void validate()
	throws ConfigException 
    {
		for(String[] entry : foundSettings) {
			String command = entry[0].toUpperCase();
			String[] params = new String[entry.length-1];
			System.arraycopy(entry, 1, params, 0, params.length);

			if ("LISTEN".equals(command)) {
				listenMessage(params);
			}
			else if("OPER".equals(command)) {
				operMessage(params);
			}
			else if("SERVERNAME".equals(command)) {
				serverNameMessage(params);
			}
			else if("SERVERINFO".equals(command)) {
				serverInfoMessage(params);
			}
		}
	}
	
	private void listenMessage(String[] params)
	throws ConfigException {
		for(String p : params) {
			try {
				server.addListenPort(Integer.parseInt(p));
			} catch(NumberFormatException e) {
				throw new ConfigException("Invalid port: " + p);
			}
		}
	}
	
	private void operMessage(String[] params)
	throws ConfigException {
		Operator oper;
		try {
			if(params.length != 2)
				throw new ConfigException("Oper <name> :<password>");
			
			oper = new Operator(params[0], params[1]);
			ctx.addOperator(params[0], oper);
		} catch (NoSuchAlgorithmException e) {
			throw new ConfigException(e.getMessage());
		}
	}
	
	private void serverNameMessage(String[] params)
	throws ConfigException {
		if(params.length != 1)
			throw new ConfigException("ServerName <name>");
		ctx.setServerName(params[0]);
	}
	
	private void serverInfoMessage(String[] params)
	throws ConfigException {
		if(params.length != 1)
			throw new ConfigException("ServerInfo :<info>");
		ctx.setServerInfo(params[0]);
	}
}
