package se.c0la.fatcat.context;

import java.util.*;

public class ChannelMember
{
	private EnumSet<ChannelAttribute> attributes;
	
	public ChannelMember(ChannelMember member)
	{
		attributes = member.attributes.clone();
	}

	public ChannelMember()
	{
		attributes = EnumSet.noneOf(ChannelAttribute.class);
	}
	
	public Set<ChannelAttribute> getAttributes() 
	{ 
		return Collections.unmodifiableSet(attributes);
	}
	
	public void setAttribute(ChannelAttribute attr)
	{
		if (!attr.isMemberAttribute()) {
			throw new IllegalArgumentException("Cannot use channel attributes on channel members.");
		}
	
		attributes.add(attr);
	}
	
	public void removeAttribute(ChannelAttribute attr)
	{
		if (!attr.isMemberAttribute()) {
			throw new IllegalArgumentException("Cannot use channel attributes on channel members.");
		}
	
		attributes.remove(attr);
	}
	
	public boolean getAttribute(ChannelAttribute attr)
	{
		if (!attr.isMemberAttribute()) {
			throw new IllegalArgumentException("Cannot use channel attributes on channel members.");
		}
	
		return attributes.contains(attr);
	}
}
