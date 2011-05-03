package se.c0la.fatcat.context;

import java.util.*;

public class ImmutableChannelMember extends ChannelMember
{
	public ImmutableChannelMember(ChannelMember member)
	{
		super(member);
	}

	public void setAttribute(ChannelAttribute attr)
	{
		throw new UnsupportedOperationException();
	}
	
	public void removeAttribute(ChannelAttribute attr)
	{
		throw new UnsupportedOperationException();
	}
}
