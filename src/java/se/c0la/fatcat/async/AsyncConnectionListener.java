package se.c0la.fatcat.async;

import java.io.*;
import java.net.*;

public interface AsyncConnectionListener
{
	public void connected(AsyncConnection client);
	public void messageReceived(AsyncConnection client, String message);
	public void disconnected(AsyncConnection client);
}
