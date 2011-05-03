package se.c0la.fatcat.context;

import java.util.*;

import se.c0la.fatcat.*;
import se.c0la.fatcat.async.*;

public class ImmutableChannel extends Channel
{
	public ImmutableChannel(Channel channel)
	{
		super(channel);
	}

	@Override
	public void addUser(User user)
	{
		throw new UnsupportedOperationException();
	}
	
	@Override
	public void removeUser(User user)
	{
		throw new UnsupportedOperationException();
	}
	
	@Override
	public ChannelMember getUser(User user)
	{
		ChannelMember member = users.get(user);
		if (member == null) {
			return null;
		}

		return new ImmutableChannelMember(member);
	}
	
	@Override
	public void setTopic(String topic, String nick)
	{
		throw new UnsupportedOperationException();
	}
	
	@Override
	public AttributeParameter getAttribute(ChannelAttribute attr)
	{
		if (attr.isMemberAttribute()) {
			throw new IllegalArgumentException("Cannot use channel member attributes on channel.");
		}
	
		AttributeParameter param = attributes.get(attr);
		if (param == null) {
			return null;
		}

		return new ImmutableAttributeParameter(param);
	}
	
	@Override
	public void setAttribute(ChannelAttribute attr, AttributeParameter param)
	{
		throw new UnsupportedOperationException();
	}
	
	@Override
	public void removeAttribute(ChannelAttribute attr)
	{
		throw new UnsupportedOperationException();
	}
}
