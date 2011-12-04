package se.c0la.fatcat;

import java.util.*;

import se.c0la.fatcat.context.*;

/**
 * This class is called by the ServerContext whenever an event
 * needs to be propagated to other Clients.
 */
public interface PropagationProtocol
{
	public void welcomeSequence(User targetUser);
    public void invalidPassword(User targetUser);
	public void sendHeartBeat(User targetUser);
	public void message(User targetUser, User source, String target, String message);
	public void notice(User targetUser, User source, String target, String message);
	public void nickChange(User targetUser, User source, String newNick);
	public void joinedChannel(User targetUser, User source, Channel channel);
	public void partedChannel(User targetUser, User source, Channel channel, 
		String message);
	public void inviteToChannel(User targetUser, User sourceUser, Channel channel);
	public void kickedFromChannel(User targetUser, User source, Channel channel, 
		User kickedUser, String message);
	public void topicChanged(User targetUser, User sourceUser, Channel channel, 
		String message);
	public void quit(User targetUser, User source, String message);
	public void attributeChange(User targetUser, User sourceUser, Channel channel, 
		List<AttributeChange> channelAttrs);
}
