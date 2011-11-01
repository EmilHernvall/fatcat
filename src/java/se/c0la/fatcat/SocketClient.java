package se.c0la.fatcat;

import se.c0la.fatcat.async.*;

public class SocketClient implements Client
{
    private AsyncServer server;
    private AsyncConnection conn;

    public SocketClient(AsyncServer server, AsyncConnection conn)
    {
        this.server = server;
        this.conn = conn;
    }

    public String getHost()
    {
        return conn.getHost();
    }
    
    public void sendMessage(String message)
    {
        server.sendMessage(conn, message);
    }
    
    public void closeConnection()
    {
        server.closeConnection(conn);
    }
    
    @Override
    public boolean equals(Object obj)
    {
        if (!(obj instanceof SocketClient)) {
            return false;
        }
        
        SocketClient b = (SocketClient)obj;
        
        return conn.equals(b.conn);
    }
    
    @Override
    public int hashCode()
    {
        int code = 17;
        code = 31 * code + conn.getSeq();
        
        return code;
    }
}
