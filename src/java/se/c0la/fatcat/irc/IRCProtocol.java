package se.c0la.fatcat.irc;

import java.util.*;
import java.util.regex.*;

import se.c0la.fatcat.*;
import se.c0la.fatcat.context.*;
import se.c0la.fatcat.async.*;

public class IRCProtocol implements Protocol
{
	private ServerContext ctx;
	
	private IRCReceiverProtocol receiverProtocol;
	private IRCPropagationProtocol propagationProtocol;
	
	private Modes<UserAttribute> userModes;
	private Modes<ChannelAttribute> channelModes;

	public IRCProtocol(ServerContext ctx)
	{
		this.ctx = ctx;
		
		this.receiverProtocol = new IRCReceiverProtocol(this);
		this.propagationProtocol = new IRCPropagationProtocol(this);
		
		this.userModes = new Modes<UserAttribute>();
		
		userModes.addMode(new Modes.ModeInfo<UserAttribute>('i', false, UserAttribute.INVISIBLE));
		userModes.addMode(new Modes.ModeInfo<UserAttribute>('O', false, UserAttribute.OPERATOR));		
		userModes.addMode(new Modes.ModeInfo<UserAttribute>('M', false, UserAttribute.MINECRAFT));		
		
		this.channelModes = new Modes<ChannelAttribute>();
		
		// Channel modes
		channelModes.addMode(new Modes.ModeInfo<ChannelAttribute>('n', false, ChannelAttribute.NO_EXTERNAL_MESSAGES));
		channelModes.addMode(new Modes.ModeInfo<ChannelAttribute>('t', false, ChannelAttribute.TOPIC_RESTRICTED));
		channelModes.addMode(new Modes.ModeInfo<ChannelAttribute>('m', false, ChannelAttribute.MODERATED));
		channelModes.addMode(new Modes.ModeInfo<ChannelAttribute>('i', false, ChannelAttribute.INVITE_ONLY));
		
		channelModes.addMode(new Modes.ModeInfo<ChannelAttribute>('k', true, ChannelAttribute.KEY));
		channelModes.addMode(new Modes.ModeInfo<ChannelAttribute>('b', true, ChannelAttribute.BAN));
		
		// Channel member modes
		channelModes.addMode(new Modes.ModeInfo<ChannelAttribute>('o', true, ChannelAttribute.OP, '@'));
		channelModes.addMode(new Modes.ModeInfo<ChannelAttribute>('h', true, ChannelAttribute.HALFOP, '%'));
		channelModes.addMode(new Modes.ModeInfo<ChannelAttribute>('v', true, ChannelAttribute.VOICE, '+'));
	}
	
	public String getNickPattern()
	{
		return "[A-Za-z0-9\\[\\]\\\\`_^{|}]{2,15}";
	}
	
	public String getChannelPattern()
	{
		return "#[A-Za-z0-9\\[\\]\\\\`_^{|}]{2,50}";
	}
	
	public Modes<UserAttribute> getUserModes()
	{
		return userModes;
	}
	
	public Modes<ChannelAttribute> getChannelModes()
	{
		return channelModes;
	}
	
	public ServerContext getServerContext()
	{
		return ctx;
	}
	
	public AsyncServer getServer()
	{
		return ctx.getServer();
	}
	
	@Override
	public IRCReceiverProtocol getReceiverProtocol()
	{
		return receiverProtocol;
	}
	
	@Override
	public IRCPropagationProtocol getPropagationProtocol()
	{
		return propagationProtocol;
	}
	
	public Channel.Ban parseBan(String banString)
	{
		Pattern splitPattern = Pattern.compile("([^!]+)!?([^@]*)@(.+)");
		Matcher matcher = splitPattern.matcher(banString);
		if (!matcher.matches()) {
			return null;
		}
		
		String nickMask = matcher.group(1);
		String userMask = matcher.group(2);
		String hostMask = matcher.group(3);
		
		if ("".equals(userMask)) {
			userMask = "*";
		}
		
		long timestamp = System.currentTimeMillis();
		return new Channel.Ban(nickMask, userMask, hostMask, timestamp);
	}
}
