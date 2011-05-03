package se.c0la.fatcat.context;

public class ImmutableAttributeParameter extends AttributeParameter
{
	public ImmutableAttributeParameter(AttributeParameter param)
	{
		super(param);
	}
	
	public void setData(String v)
	{
		throw new UnsupportedOperationException();
	}
}
