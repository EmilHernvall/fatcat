package se.c0la.fatcat.irc;

import java.util.*;

import se.c0la.fatcat.*;
import se.c0la.fatcat.context.*;
import se.c0la.fatcat.async.*;

public class IRCPropagationProtocol implements PropagationProtocol
{
	private IRCProtocol protocol;

	private ServerContext ctx;
	private AsyncServer server;

	public IRCPropagationProtocol(IRCProtocol protocol)
	{
		this.protocol = protocol;
		
		ctx = protocol.getServerContext();
		server = protocol.getServer();
	}
	
	@Override
	public void welcomeSequence(User targetUser)
	{
		Client client = targetUser.getClient();
		
		// 001
		NumericResponse welcomeCode = NumericResponse.RPL_WELCOME;
		String welcomeText = welcomeCode.getText()
			.replace("<nick>!<user>@<host>", targetUser.toString());
			
		String welcomeData = String.format(":%s %03d %s %s", ctx.getServerName(), 
			welcomeCode.getNum(), targetUser.getNick(), welcomeText);
            
		client.sendMessage(welcomeData);
		
		// 002
		NumericResponse hostCode = NumericResponse.RPL_YOURHOST;
		String hostText = hostCode.getText()
			.replace("<servername>", ctx.getServerName())
			.replace("<ver>", ctx.getServerVersion());
			
		String hostData = String.format(":%s %03d %s %s", ctx.getServerName(), 
			hostCode.getNum(), targetUser.getNick(), hostText);
            
		client.sendMessage(hostData);
		
		// 003
		NumericResponse createdCode = NumericResponse.RPL_CREATED;
		String createdText = createdCode.getText()
			.replace("<date>", ctx.getStartDate().toString());
			
		String createdData = String.format(":%s %03d %s %s", ctx.getServerName(), 
			createdCode.getNum(), targetUser.getNick(), createdText);
            
		client.sendMessage(createdData);
		
		// 004
		NumericResponse infoCode = NumericResponse.RPL_MYINFO;
		Modes<UserAttribute> userModes = protocol.getUserModes();
		Modes<ChannelAttribute> channelModes = protocol.getChannelModes();
		String infoText = infoCode.getText()
			.replace("<servername>", ctx.getServerName())
			.replace("<version>", ctx.getServerVersion())
			.replace("<available user modes>", "+" + userModes.getModeList())
			.replace("<available channel modes>", "+" + channelModes.getModeList());
			
		String infoData = String.format(":%s %03d %s %s", ctx.getServerName(), 
			infoCode.getNum(), targetUser.getNick(), infoText);
            
		client.sendMessage(infoData);
		
		// Send /lusers and /motd when connecting
		IRCReceiverProtocol recvProp = protocol.getReceiverProtocol();
		try {
			recvProp.motdMessage(targetUser, new String[] { "MOTD" });
			recvProp.lusersMessage(targetUser, new String[] { "LUSERS" });
		}
		catch (ErrorConditionException e) {
			recvProp.errorMessage(targetUser, e.getCode());
		}
		catch (NumericErrorException e) {
			recvProp.errorMessage(targetUser, e.getCode(), null);
		}
	}
	
	@Override
	public void sendHeartBeat(User targetUser)
	{
		String data = String.format(":%s PING %s", ctx.getServerName(), targetUser.getNick());
        
        Client client = targetUser.getClient();
		client.sendMessage(data);
	}
	
	@Override
	public void message(User targetUser, User source, String target, String message)
	{
		String data = String.format(":%s PRIVMSG %s :%s", source, target, message);
        
        Client client = targetUser.getClient();
		client.sendMessage(data);
	}

	@Override
	public void notice(User targetUser, User source, String target,
			String message) {
		String data = String.format(":%s NOTICE %s :%s", source, target, message);
        
        Client client = targetUser.getClient();
		client.sendMessage(data);
	}
	
	@Override
	public void nickChange(User targetUser, User source, String newNick)
	{
		String data = String.format(":%s NICK :%s", source, newNick);
        
        Client client = targetUser.getClient();
		client.sendMessage(data);
	}
	
	@Override
	public void joinedChannel(User targetUser, User source, Channel channel)
	{
		String data = String.format(":%s JOIN %s", source, channel.getName());
        
        Client client = targetUser.getClient();
		client.sendMessage(data);
	}
	
	@Override
	public void partedChannel(User targetUser, User source, Channel channel, String message)
	{
		String data = String.format(":%s PART %s :%s", source, channel.getName(), message);
        
        Client client = targetUser.getClient();
		client.sendMessage(data);
	}
	
	@Override
	public void inviteToChannel(User targetUser, User sourceUser, Channel channel)
	{
		String data = String.format(":%s INVITE %s :%s", sourceUser, targetUser.getNick(), channel.getName());
        
        Client client = targetUser.getClient();
		client.sendMessage(data);
	}
	
	@Override
	public void kickedFromChannel(User targetUser, User source, Channel channel, 
		User kickedUser, String message)
	{
		String data = String.format(":%s KICK %s %s :%s", source, channel.getName(), 
			kickedUser.getNick(), message);
            
        Client client = targetUser.getClient();
		client.sendMessage(data);
	}
	
	@Override
	public void topicChanged(User targetUser, User sourceUser, Channel channel, String message)
	{
		String data = String.format(":%s TOPIC %s :%s", sourceUser.getNick(), 
			channel.getName(), message);
            
        Client client = targetUser.getClient();
		client.sendMessage(data);
	}
	
	@Override
	public void quit(User targetUser, User source, String message)
	{
		String data = String.format(":%s QUIT :%s", source, message);
        
        Client client = targetUser.getClient();
		client.sendMessage(data);
	}
	
	@Override
	public void attributeChange(User targetUser, User sourceUser, Channel channel, 
		List<AttributeChange> channelAttrs)
	{
		Modes<ChannelAttribute> channelModes = protocol.getChannelModes();
		String modeString = channelModes.serialize(channelAttrs);
		
		String message = String.format(":%s MODE %s %s", sourceUser.toString(), 
			channel.getName(), modeString.toString());
            
        Client client = targetUser.getClient();
		client.sendMessage(message);
	}
}
