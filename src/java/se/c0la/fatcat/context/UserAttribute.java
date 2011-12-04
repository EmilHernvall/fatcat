package se.c0la.fatcat.context;

public enum UserAttribute
{
	INVISIBLE (false),
	OPERATOR (true),
	AWAY (true),
    MINECRAFT (true);
	
	private boolean serverOnly;
	
	private UserAttribute(boolean serverOnly)
	{
		this.serverOnly = serverOnly;
	}
	
	public boolean isServerOnly()
	{
		return serverOnly;
	}
}
