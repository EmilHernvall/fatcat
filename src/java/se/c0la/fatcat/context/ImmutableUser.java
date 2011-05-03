package se.c0la.fatcat.context;

import java.util.*;

import se.c0la.fatcat.*;
import se.c0la.fatcat.async.*;

public class ImmutableUser extends User
{
	public ImmutableUser(User old)
	{
		super(old);
	}

	@Override
	public void setProtocol(Protocol v) 
	{ 
		throw new UnsupportedOperationException();
	}

	@Override
	public void setRegistered()
	{
		throw new UnsupportedOperationException();
	}
	
	@Override
	public void setNick(String v) 
	{ 
		throw new UnsupportedOperationException();
	}
	
	@Override
	public void setUser(String v)
	{
		throw new UnsupportedOperationException();
	}
	
	@Override
	public void setRealName(String v)
	{
		throw new UnsupportedOperationException();
	}
	
	@Override
	public void setQuitMessage(String v)
	{
		throw new UnsupportedOperationException();
	}
	
	@Override
	public void addChannel(Channel channel)
	{
		throw new UnsupportedOperationException();
	}
	
	@Override
	public void removeChannel(Channel channel)
	{
		throw new UnsupportedOperationException();
	}
}
