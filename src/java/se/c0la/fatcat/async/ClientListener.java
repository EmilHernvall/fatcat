package se.c0la.fatcat.async;

import java.io.*;
import java.net.*;

public interface ClientListener
{
	public void connected(Client client);
	public void messageReceived(Client client, String message);
	public void disconnected(Client client);
}
