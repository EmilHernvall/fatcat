package se.c0la.fatcat;

import se.c0la.fatcat.async.*;

public class SocketClient implements Client
{
    private AsyncServer server;
    private AsyncClient client;

    public SocketClient(AsyncServer server, AsyncClient client)
    {
        this.server = server;
        this.client = client;
    }

    public String getHost()
    {
        return client.getHost();
    }
    
    public void sendMessage(String message)
    {
        server.sendMessage(client, message);
    }
    
    public void closeConnection()
    {
        server.closeConnection(client);
    }
    
    @Override
    public boolean equals(Object obj)
    {
        if (!(obj instanceof SocketClient)) {
            return false;
        }
        
        SocketClient b = (SocketClient)obj;
        
        return client.equals(b.client);
    }
    
    @Override
    public int hashCode()
    {
        int code = 17;
        code = 31 * code + client.getSeq();
        
        return code;
    }
}
