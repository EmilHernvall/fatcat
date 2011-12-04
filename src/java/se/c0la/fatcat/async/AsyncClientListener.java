package se.c0la.fatcat.async;

import java.io.*;
import java.net.*;

public interface AsyncClientListener
{
	public void connected(AsyncClient client);
	public void messageReceived(AsyncClient client, String message);
	public void disconnected(AsyncClient client);
}
