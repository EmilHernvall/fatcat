package se.c0la.fatcat;

public interface Client
{
    public String getHost();
    public void sendMessage(String message);
    public void closeConnection();
}
