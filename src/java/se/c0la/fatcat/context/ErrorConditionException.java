package se.c0la.fatcat.context;

public class ErrorConditionException extends Exception
{
	private ErrorCondition code;
	
	public ErrorConditionException(ErrorCondition code)
	{
		this.code = code;
	}
	
	public ErrorCondition getCode()
	{
		return code;
	}
}
