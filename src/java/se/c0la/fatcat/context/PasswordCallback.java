package se.c0la.fatcat.context;

public interface PasswordCallback
{
    public boolean verify(String nick, String user, String password);
}

