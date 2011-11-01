package se.c0la.fatcat;

public class ConfigException extends Exception 
{
	private static final long serialVersionUID = -4020104523466904110L;

	public ConfigException(String message) 
    {
		super(message);
	}
    
	public ConfigException(String message, Exception e) 
    {
		super(message, e);
	}
    
	public ConfigException(Exception e) 
    {
		super(e);
	}
}
