package se.c0la.fatcat.context;

import java.util.List;

import se.c0la.fatcat.Client;
import se.c0la.fatcat.Protocol;

public interface EventListener
{	
    public void userConnectedEvent(Client client, Protocol protocol);
	public void userDisconnectedEvent(Client client);
	public void userIdentificationEvent(User user, String userName, String mode, String realName);
	public void nickEvent(User user, String newNick);
	public void messageEvent(User sourceUser, String targetName, String message);
	public void noticeEvent(User sourceUser, String targetName, String message);
	public void joinEvent(User user, String name);
	public void inviteEvent(User sourceUser, Channel channel, User targetUser);
	public void topicEvent(Channel channel, User user, String newTopic);
	public void partEvent(User user, String name, String message);
	public void kickEvent(User user, String channelName, User kickUser, String message);
	public void operEvent(User user);
	public void killEvent(User targetUser, User sourceUser, String quitmessage);
	public void userAttributeEvent(User user, List<AttributeChange> userAttrs);
	public void channelAttributeEvent(User user, Channel channel, List<AttributeChange> channelAttrs);
	public void quitEvent(User user, String message);
	public void awayEvent(User user, String message);
	public void notAwayEvent(User user);
	public void idleEvent(User user);
}
