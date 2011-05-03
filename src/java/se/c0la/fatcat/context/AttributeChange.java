package se.c0la.fatcat.context;

public class AttributeChange
{
	private Object attr;
	private boolean set;
	private Object param = null;

	public AttributeChange(Object attr, boolean set, Object param)
	{
		this.attr = attr;
		this.set = set;
		this.param = param;
	}
	
	public Object getAttribute()
	{
		return attr;
	}
	
	public boolean isSet()
	{
		return set;
	}
	
	public void setParameter(Object v)
	{
		this.param = v;
	}
	
	public Object getParameter()
	{
		return param;
	}
}
