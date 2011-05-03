package se.c0la.fatcat.irc;

public class NumericErrorException extends Exception
{
	private NumericResponse code;
	private String text = null;
	
	public NumericErrorException(NumericResponse code)
	{
		this.code = code;
	}
	
	public NumericErrorException(NumericResponse code, String text)
	{
		this.code = code;
		this.text = text;
	}
	
	public NumericResponse getCode()
	{
		return code;
	}
	
	public void setText(String text)
	{
		this.text = text;
	}
	
	public String getText()
	{
		return text;
	}
}
