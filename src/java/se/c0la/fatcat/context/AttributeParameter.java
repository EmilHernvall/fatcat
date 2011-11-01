package se.c0la.fatcat.context;

public class AttributeParameter
{
	private volatile String data = null;

	public AttributeParameter(AttributeParameter param)
	{
		data = param.getData();
	}
	
	public AttributeParameter()
	{
	}
	
	public void setData(String v)
	{
		this.data = v;
	}
	
	public String getData()
	{
		return data;
	}
}
