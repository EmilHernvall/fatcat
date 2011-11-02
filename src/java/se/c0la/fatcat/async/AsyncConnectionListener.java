package se.c0la.fatcat.async;

import java.net.InetSocketAddress;

public interface AsyncConnectionListener
{
    public void connectionFailed(InetSocketAddress host);
	public void connected(AsyncConnection conn);
	public void messageReceived(AsyncConnection conn, String message);
	public void disconnected(AsyncConnection conn);
}
