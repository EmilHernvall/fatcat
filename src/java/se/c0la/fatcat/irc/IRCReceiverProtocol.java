package se.c0la.fatcat.irc;

import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.util.*;

import se.c0la.fatcat.*;
import se.c0la.fatcat.async.*;
import se.c0la.fatcat.context.*;

public class IRCReceiverProtocol implements ReceiverProtocol
{
	private IRCProtocol protocol;

	private ServerContext ctx;
	private AsyncServer server;

	public IRCReceiverProtocol(IRCProtocol protocol)
	{
		this.protocol = protocol;

		ctx = protocol.getServerContext();
		server = protocol.getServer();
	}

	@Override
	public void translateMessage(User user, String message)
	{
		boolean isIdle = true;
		try {
			String[] messageParts = MessageTokenizer.tokenize(message);
			String command = messageParts[0].toUpperCase();

			if ("USER".equals(command)) {
				userMessage(user, messageParts);
			}
			else if ("NICK".equals(command)) {
				nickMessage(user, messageParts);
				isIdle = false;
			}
			else if ("PRIVMSG".equals(command)) {
				privmsgMessage(user, messageParts);
				isIdle = false;
			}
			else if("NOTICE".equals(command)) {
				noticeMessage(user, messageParts);
				isIdle = false;
			}
			else if ("JOIN".equals(command)) {
				joinMessage(user, messageParts);
				isIdle = false;
			}
			else if ("INVITE".equals(command)) {
				inviteMessage(user, messageParts);
				isIdle = false;
			}
			else if ("PART".equals(command)) {
				partMessage(user, messageParts);
				isIdle = false;
			}
			else if ("KICK".equals(command)) {
				kickMessage(user, messageParts);
				isIdle = false;
			}
			else if ("QUIT".equals(command)) {
				quitMessage(user, messageParts);
				isIdle = false; // :P
			}
			else if ("NAMES".equals(command)) {
				namesMessage(user, messageParts);
			}
			else if ("TOPIC".equals(command)) {
				topicMessage(user, messageParts);
				isIdle = false;
			}
			else if ("LUSERS".equals(command)) {
				lusersMessage(user, messageParts);
			}
			else if ("MOTD".equals(command)) {
				motdMessage(user, messageParts);
			}
			else if ("VERSION".equals(command)) {
				versionMessage(user, messageParts);
			}
			else if ("PING".equals(command)) {
				pingMessage(user, messageParts);
			}
			else if ("MODE".equals(command)) {
				modeMessage(user, messageParts);
				isIdle = false;
			}
			else if ("WHOIS".equals(command)) {
				whoisMessage(user, messageParts);
			}
			else if ("WHO".equals(command)) {
				whoMessage(user, messageParts);
			}
			else if ("PONG".equals(command)) {
				// accept quietly.
			}
			else if ("LIST".equals(command)) {
				listMessage(user, messageParts);
			}
			else if ("OPER".equals(command)) {
				operMessage(user, messageParts);
				isIdle = false;
			}
			else if ("KILL".equals(command)) {
				killMessage(user, messageParts);
				isIdle = false;
			}
			else if("AWAY".equals(command)) {
				awayMessage(user, messageParts);
			}
			else if("TIME".equals(command)) {
				timeMessage(user, messageParts);
			}
			else {
				String responseText = NumericResponse.ERR_UNKNOWNCOMMAND.getText()
					.replace("<command>", messageParts[0]);
				throw new NumericErrorException(NumericResponse.ERR_UNKNOWNCOMMAND, responseText);
			}
		}
		catch (ErrorConditionException e) {
			errorMessage(user, e.getCode());
		}
		catch (NumericErrorException e) {
			errorMessage(user, e.getCode(), e.getText());
		}

		/*
		 * Update user idle time
		 */
		if(!isIdle) {
			ctx.idleEvent(user);
		}
	}

	public void userMessage(User user, String[] messageParts)
	throws ErrorConditionException, NumericErrorException
	{
		if (messageParts.length < 4) {
			NumericResponse num = NumericResponse.ERR_NEEDMOREPARAMS;
			String text = num.getText().replace("<command>", messageParts[0]);
			throw new NumericErrorException(num, text);
		}

		if (user.hasRegistered()) {
			throw new NumericErrorException(NumericResponse.ERR_ALREADYREGISTRED);
		}

		ctx.userIdentificationEvent(user, messageParts[1], messageParts[2], messageParts[4]);
	}

	public void nickMessage(User user, String[] messageParts)
	throws ErrorConditionException, NumericErrorException
	{

		if (messageParts.length < 2) {
			throw new NumericErrorException(NumericResponse.ERR_NONICKNAMEGIVEN);
		}

		String nick = messageParts[1];

		String valid = protocol.getNickPattern();
		if (!nick.matches(valid)) {
			NumericResponse num = NumericResponse.ERR_ERRONEUSNICKNAME;
			String text = num.getText().replace("<nick>", nick);
			throw new NumericErrorException(num, text);
		}
        
        if (nick.matches("^\\{[^}]+\\}$")) {
			NumericResponse num = NumericResponse.ERR_ERRONEUSNICKNAME;
			String text = num.getText().replace("<nick>", nick);
			throw new NumericErrorException(num, text);
        }

		User possibleDupe = ctx.getUser(nick);
		if (possibleDupe != null) {
			NumericResponse num = NumericResponse.ERR_NICKNAMEINUSE;
			String text = num.getText().replace("<nick>", nick);
			throw new NumericErrorException(num, text);
		}

		// These are valid errors that can be returned as a response to this message,
		// but are not yet implemented:
		// ERR_NICKCOLLISION
		// ERR_UNAVAILRESOURCE
		// ERR_RESTRICTED

		ctx.nickEvent(user, messageParts[1]);
	}

	public void privmsgMessage(User user, String[] messageParts)
	throws ErrorConditionException, NumericErrorException
	{
		if (!user.hasRegistered()) {
			throw new NumericErrorException(NumericResponse.ERR_NOTREGISTERED);
		}

		if (messageParts.length < 3) {
			NumericResponse num = NumericResponse.ERR_NEEDMOREPARAMS;
			String text = num.getText().replace("<command>", messageParts[0]);
			throw new NumericErrorException(num, text);
		}

		String target = messageParts[1];

		// Messages can't be sent to empty channels.
		Channel channel = ctx.getChannel(target);

		// Message is being sent to a channel
		if (channel != null) {
			// If channel mode +n is set, we do not allow messages to be sent
			// to the channel if the user isn't actually in it.
			AttributeParameter noExternalMessages =
				channel.getAttribute(ChannelAttribute.NO_EXTERNAL_MESSAGES);
			ChannelMember member = channel.getUser(user);
			if (noExternalMessages != null && member == null) {
				NumericResponse num = NumericResponse.ERR_NOTONCHANNEL;
				String text = num.getText().replace("<channel>", target);
				throw new NumericErrorException(num, text);
			}

			// If the channel is +m, make sure the user has mode +v
			AttributeParameter moderated =
					channel.getAttribute(ChannelAttribute.MODERATED);
			if (moderated != null && (!member.getAttribute(ChannelAttribute.OP)
				&& !member.getAttribute(ChannelAttribute.HALFOP)
				&& !member.getAttribute(ChannelAttribute.VOICE))) {

				NumericResponse num = NumericResponse.ERR_CANNOTSENDTOCHAN;
				String text = num.getText().replace("<channel>", target);
				throw new NumericErrorException(num, text);
			}
		}
		// Message is being sent to a user
		else if (ctx.getUser(target) != null) {
			// No validation to perform
		}
		// Target unknown
		else {
			NumericResponse num = NumericResponse.ERR_NOSUCHCHANNEL;
			String text = num.getText().replace("<channel>", target);
			throw new NumericErrorException(num, text);
		}

		ctx.messageEvent(user, messageParts[1], messageParts[2]);
	}

	/**
	 * This method must not return errors to sender
	 * @param user
	 * @param messageParts
	 * @throws ErrorConditionException
	 */
	public void noticeMessage(User user, String[] messageParts)
	{
		if (!user.hasRegistered()) {
			return;
		}

		if (messageParts.length < 3) {
			return;
		}

		String target = messageParts[1];

		// Messages can't be sent to empty channels.
		Channel channel = ctx.getChannel(target);

		// Message is being sent to a channel
		if (channel != null) {
			// If channel mode +n is set, we do not allow messages to be sent
			// to the channel if the user isn't actually in it.
			AttributeParameter noExternalMessages =
				channel.getAttribute(ChannelAttribute.NO_EXTERNAL_MESSAGES);
			ChannelMember member = channel.getUser(user);
			if (noExternalMessages != null && member == null) {
				return;
			}

			// If the channel is +m, make sure the user has mode +v
			AttributeParameter moderated =
					channel.getAttribute(ChannelAttribute.MODERATED);
			if (moderated != null && (!member.getAttribute(ChannelAttribute.OP)
				&& !member.getAttribute(ChannelAttribute.HALFOP)
				&& !member.getAttribute(ChannelAttribute.VOICE))) {

				return;
			}
		}
		// Message is being sent to a user
		else if (ctx.getUser(target) != null) {
			// No validation to perform
		}
		// Target unknown
		else {
			return;
		}

		ctx.noticeEvent(user, messageParts[1], messageParts[2]);
	}

	public void joinMessage(User user, String[] messageParts)
	throws ErrorConditionException, NumericErrorException
	{
		if (!user.hasRegistered()) {
			throw new NumericErrorException(NumericResponse.ERR_NOTREGISTERED);
		}

		if (messageParts.length < 2) {
			NumericResponse num = NumericResponse.ERR_NEEDMOREPARAMS;
			String text = num.getText().replace("<command>", messageParts[0]);
			throw new NumericErrorException(num, text);
		}

		String[] channelList = messageParts[1].split(",");
        
        // Prevalidate all channel names
        for (String channelName : channelList) {
            if (!channelName.matches(protocol.getChannelPattern())) {
                NumericResponse num = NumericResponse.ERR_NOSUCHCHANNEL;
                String text = num.getText().replace("<channel>", channelName);
                throw new NumericErrorException(num, text);
            }
        }
        
        // Join all channels sent
        for (String channelName : channelList) {
            Channel channel = ctx.getChannel(channelName);
            if (channel != null && channel.getUser(user) != null) {
                return;
            }

            if (channel != null) {
                List<Channel.Ban> bans = channel.getBans();
                for (Channel.Ban ban : bans) {
                    if (ban.matches(user)) {
                        NumericResponse num = NumericResponse.ERR_BANNEDFROMCHAN;
                        String text = num.getText().replace("<channel>", channelName);
                        throw new NumericErrorException(num, text);
                    }
                }

                // Don't let the user join if the channel is invite only
                if (channel.getAttribute(ChannelAttribute.INVITE_ONLY) != null && !channel.getInvite(user.getNick())) {
                        NumericResponse num = NumericResponse.ERR_INVITEONLYCHAN;
                        String text = num.getText().replace("<channel>", channelName);
                        throw new NumericErrorException(num, text);
                }

                // Don't let the user join if the channel has a key and the user hasn't provided it
                if (channel.getAttribute(ChannelAttribute.KEY) != null && messageParts.length < 3) {
                        NumericResponse num = NumericResponse.ERR_BADCHANNELKEY;
                        String text = num.getText().replace("<channel>", channelName);
                        throw new NumericErrorException(num, text);
                }
                else if (channel.getAttribute(ChannelAttribute.KEY) != null && !messageParts[2].equals(channel.getAttribute(ChannelAttribute.KEY).getData())) {
                        NumericResponse num = NumericResponse.ERR_BADCHANNELKEY;
                        String text = num.getText().replace("<channel>", channelName);
                        throw new NumericErrorException(num, text);
                }
            }

            ctx.joinEvent(user, channelName);

            modeMessage(user, new String[] { "MODE", channelName });
            topicMessage(user, new String[] { "TOPIC", channelName });
            namesMessage(user, new String[] { "NAMES", channelName });
        }
	}


	public void inviteMessage(User user, String[] messageParts)
	throws ErrorConditionException, NumericErrorException
	{
		if (!user.hasRegistered()) {
			throw new NumericErrorException(NumericResponse.ERR_NOTREGISTERED);
		}

		if (messageParts.length < 2) {
			NumericResponse num = NumericResponse.ERR_NEEDMOREPARAMS;
			String text = num.getText().replace("<command>", messageParts[0]);
			throw new NumericErrorException(num, text);
		}

		String channelName = messageParts[2];
		Channel channel = ctx.getChannel(channelName);
		if (channel == null) {
			return;
		}
		else if (channel.getUser(user) == null) {
			NumericResponse num = NumericResponse.ERR_NOTONCHANNEL;
			String text = num.getText().replace("<channel>", channelName);
			throw new NumericErrorException(num, text);
		}

		User targetUser = ctx.getUser(messageParts[1]);
		ChannelMember member = channel.getUser(user);

		if (channel.getAttribute(ChannelAttribute.INVITE_ONLY) != null && !member.getAttribute(ChannelAttribute.OP)) {
			NumericResponse num = NumericResponse.ERR_CHANOPRIVSNEEDED;
			String text = num.getText().replace("<channel>", channelName);
			throw new NumericErrorException(num, text);
		}
		else if (targetUser == null) {
			return;
		}

		ctx.inviteEvent(user, channel, targetUser);
	}

	public void partMessage(User user, String[] messageParts)
	throws ErrorConditionException, NumericErrorException
	{
		if (!user.hasRegistered()) {
			throw new NumericErrorException(NumericResponse.ERR_NOTREGISTERED);
		}

		if (messageParts.length < 2) {
			NumericResponse num = NumericResponse.ERR_NEEDMOREPARAMS;
			String text = num.getText().replace("<command>", messageParts[0]);
			throw new NumericErrorException(num, text);
		}

		String channelName = messageParts[1];
		Channel channel = ctx.getChannel(channelName);
		if (channel == null) {
			NumericResponse num = NumericResponse.ERR_NOSUCHCHANNEL;
			String text = num.getText().replace("<channel>", channelName);
			throw new NumericErrorException(num, text);
		}

		ChannelMember member = channel.getUser(user);
		if (member == null) {
			NumericResponse num = NumericResponse.ERR_NOTONCHANNEL;
			String text = num.getText().replace("<channel>", channelName);
			throw new NumericErrorException(num, text);
		}

		String partMessage = (messageParts.length > 2 ? messageParts[2] : user.getNick());
		ctx.partEvent(user, channelName, partMessage);
	}

	public void kickMessage(User user, String[] messageParts)
	throws ErrorConditionException, NumericErrorException
	{
		if (!user.hasRegistered()) {
			throw new NumericErrorException(NumericResponse.ERR_NOTREGISTERED);
		}

		// KICK #channel target :message
		if (messageParts.length < 3) {
			NumericResponse num = NumericResponse.ERR_NEEDMOREPARAMS;
			String text = num.getText().replace("<command>", messageParts[0]);
			throw new NumericErrorException(num, text);
		}

		String channelName = messageParts[1];
		Channel channel = ctx.getChannel(channelName);
		if (channel == null) {
			NumericResponse num = NumericResponse.ERR_NOSUCHCHANNEL;
			String text = num.getText().replace("<channel>", channelName);
			throw new NumericErrorException(num, text);
		}

		ChannelMember member = channel.getUser(user);
		if (member == null) {
			NumericResponse num = NumericResponse.ERR_NOTONCHANNEL;
			String text = num.getText().replace("<channel>", channelName);
			throw new NumericErrorException(num, text);
		}

		if (!member.getAttribute(ChannelAttribute.OP) && !member.getAttribute(ChannelAttribute.HALFOP) && !user.getAttribute(UserAttribute.OPERATOR)) {
			NumericResponse num = NumericResponse.ERR_CHANOPRIVSNEEDED;
			String text = num.getText().replace("<channel>", channelName);
			throw new NumericErrorException(num, text);
		}

		String kickNick = messageParts[2];
		User kickUser = ctx.getUser(kickNick);
		ChannelMember kickMember = channel.getUser(kickUser);
		if (kickMember == null) {
			NumericResponse num = NumericResponse.ERR_USERNOTINCHANNEL;
			String text = num.getText()
				.replace("<channel>", channelName)
				.replace("<nick>", kickNick);
			throw new NumericErrorException(num, text);
		}

		String kickMessage = (messageParts.length > 3 ? messageParts[3] : user.getNick());
		ctx.kickEvent(user, channelName, kickUser, kickMessage);
	}

	public void modeMessage(User user, String[] messageParts)
	throws ErrorConditionException, NumericErrorException
	{
        Client client = user.getClient();
    
		if (!user.hasRegistered()) {
			throw new NumericErrorException(NumericResponse.ERR_NOTREGISTERED);
		}

		if (messageParts.length < 2) {
			NumericResponse num = NumericResponse.ERR_NEEDMOREPARAMS;
			String text = num.getText().replace("<command>", messageParts[0]);
			throw new NumericErrorException(num, text);
		}

		// Mode operations on a user.
		if (messageParts[1].compareToIgnoreCase(user.getNick()) == 0) {

			Modes<UserAttribute> userModes = protocol.getUserModes();
			if (messageParts.length == 2) {
				Set<UserAttribute> attrs = user.getAttributes();
				String modeString = userModes.serialize(attrs);

				NumericResponse modeIs = NumericResponse.RPL_UMODEIS;

				String text = modeIs.getText()
					.replace("<umode>", modeString);

				String message = String.format(":%s %d %s %s", ctx.getServerName(),
					modeIs.getNum(), user.getNick(), text);
				client.sendMessage(message);

				return;
			}

			String mode = messageParts[2];

			List<AttributeChange> userAttrs =
				userModes.parseAttributes(mode, new String[0]);

			// Remove attributes that users aren't allowed to set
			Iterator<AttributeChange> it = userAttrs.iterator();
			for ( ; it.hasNext(); ) {
				AttributeChange change = it.next();
				UserAttribute attr = (UserAttribute)change.getAttribute();
				if (attr.isServerOnly()) {
					it.remove();
				}
			}

			ctx.userAttributeEvent(user, userAttrs);

			String modeString = userModes.serialize(userAttrs);

			String message = String.format(":%s MODE %s %s", user.toString(),
				user.getNick(), modeString.toString());
			client.sendMessage(message);
		}
		// Mode operations on a channel.
		else {
			// Non-existent channels doesn't have modes.
			String channelName = messageParts[1];
			Channel channel = ctx.getChannel(channelName);
			if (channel == null) {
				NumericResponse num = NumericResponse.ERR_NOSUCHCHANNEL;
				String text = num.getText().replace("<channel>", channelName);
				throw new NumericErrorException(num, text);
			}

			// User requests the current mode
			if (messageParts.length == 2) {
				Map<ChannelAttribute, AttributeParameter> attrs =
					channel.getAttributes();

				Modes<ChannelAttribute> channelModes = protocol.getChannelModes();
				String mode = channelModes.serialize(attrs);

				NumericResponse modeIs = NumericResponse.RPL_CHANNELMODEIS;

				String text = modeIs.getText()
					.replace("<channel>", channel.getName())
					.replace("<mode> <mode params>", mode);

				String message = String.format(":%s %d %s %s", ctx.getServerName(),
					modeIs.getNum(), user.getNick(), text);
				client.sendMessage(message);

				return;
			}

			// Users that wanted to check the current mode have been taken care
			// of. From here on out, we deal with modification requests.

			// Only users that actually have joined the channel
			// can modify the modes.
			ChannelMember member = channel.getUser(user);
			if (member == null) {
				NumericResponse num = NumericResponse.ERR_NOTONCHANNEL;
				String text = num.getText().replace("<channel>", channelName);
				throw new NumericErrorException(num, text);
			}

			String mode = messageParts[2];

			// Request for bans list
			if ("b".equals(mode)) {
				List<Channel.Ban> bans = channel.getBans();
				for (Channel.Ban ban : bans) {
					NumericResponse banListCode = NumericResponse.RPL_BANLIST;
					String banListText = banListCode.getText()
						.replace("<channel>", channel.getName())
						.replace("<banmask>", ban.toString());

					String banListData = String.format(":%s %03d %s %s", ctx.getServerName(),
						banListCode.getNum(), user.getNick(), banListText);
					client.sendMessage(banListData);
				}

				NumericResponse banListCode = NumericResponse.RPL_ENDOFBANLIST;
				String banListText = banListCode.getText()
					.replace("<channel>", channel.getName());

				String banListData = String.format(":%s %d %s %s", ctx.getServerName(),
					banListCode.getNum(), user.getNick(), banListText);
				client.sendMessage(banListData);

				return;
			}

			// Only ops can change channel modes
			if (!member.getAttribute(ChannelAttribute.OP) && !user.getAttribute(UserAttribute.OPERATOR)) {
				NumericResponse num = NumericResponse.ERR_CHANOPRIVSNEEDED;
				String text = num.getText().replace("<channel>", channelName);
				throw new NumericErrorException(num, text);
			}

			// Everything after the mode code is considered to be
			// parameters.
			// TODO: This violates the specification in a way that
			// might be incompatible. Section 3.2.3 specifies that
			// the following is a legal command:
			// MODE &oulu +b *!*@*.edu +e *!*@*.bu.edu
			int paramCount = messageParts.length - 3;
			String[] params = new String[paramCount];
			if (paramCount > 0) {
				System.arraycopy(messageParts, 3, params, 0, paramCount);
			}

			Modes<ChannelAttribute> channelModes = protocol.getChannelModes();
			List<AttributeChange> channelAttrs =
				channelModes.parseAttributes(mode, params);

			for (Iterator<AttributeChange> it = channelAttrs.iterator(); it.hasNext();) {
				AttributeChange change = it.next();
				ChannelAttribute attr = (ChannelAttribute)change.getAttribute();
				if (attr.equals(ChannelAttribute.BAN)) {
					Channel.Ban ban = protocol.parseBan((String)change.getParameter());
					if (ban == null) {
						it.remove();
						continue;
					}

					change.setParameter((Object)ban);
				}
			}

			ctx.channelAttributeEvent(user, channel, channelAttrs);
		}
	}

	public void topicMessage(User user, String[] messageParts)
	throws ErrorConditionException, NumericErrorException
	{
        Client client = user.getClient();
    
		if (!user.hasRegistered()) {
			throw new NumericErrorException(NumericResponse.ERR_NOTREGISTERED);
		}

		if (messageParts.length < 2) {
			NumericResponse num = NumericResponse.ERR_NEEDMOREPARAMS;
			String text = num.getText().replace("<command>", messageParts[0]);
			throw new NumericErrorException(num, text);
		}

		// You can't retrieve the topic for a channel that doesn't exist.
		String channelName = messageParts[1];
		Channel channel = ctx.getChannel(channelName);
		if (channel == null) {
			NumericResponse num = NumericResponse.ERR_NOSUCHCHANNEL;
			String text = num.getText().replace("<channel>", channelName);
			throw new NumericErrorException(num, text);
		}

		// User wants to change topic
		if (messageParts.length > 2) {

			// Users that isn't on the channel are never allowed to change
			// the topic.
			ChannelMember member = channel.getUser(user);
			if (member == null) {
				NumericResponse num = NumericResponse.ERR_NOTONCHANNEL;
				String text = num.getText().replace("<channel>", channelName);
				throw new NumericErrorException(num, text);
			}

			// If the TOPIC_RESTRICTED attribute (+t) is set, we only allow ops
			// to change topic.
			AttributeParameter topicRestricted =
				channel.getAttribute(ChannelAttribute.TOPIC_RESTRICTED);
			if (topicRestricted != null && !member.getAttribute(ChannelAttribute.OP) && !user.getAttribute(UserAttribute.OPERATOR)) {
				NumericResponse num = NumericResponse.ERR_CHANOPRIVSNEEDED;
				String text = num.getText().replace("<channel>", channelName);
				throw new NumericErrorException(num, text);
			}

			ctx.topicEvent(channel, user, messageParts[2]);
			return;
		}

		// Request for current topic
		NumericResponse infoCode;
		String infoText;
		String infoData;

		// No topic is set. Send a message to indicate this.
		if ("".equals(channel.getTopic())) {
			// RPL_NOTOPIC
			infoCode = NumericResponse.RPL_NOTOPIC;
			infoText = infoCode.getText()
				.replace("<channel>", channel.getName());

			infoData = String.format(":%s %03d %s %s", ctx.getServerName(),
				infoCode.getNum(), user.getNick(), infoText);
			client.sendMessage(infoData);
		}

		// The topic is set. Send the topic to the user.
		else {
			// RPL_TOPIC
			infoCode = NumericResponse.RPL_TOPIC;
			infoText = infoCode.getText()
				.replace("<channel>", channel.getName())
				.replace("<topic>", channel.getTopic());

			infoData = String.format(":%s %03d %s %s", ctx.getServerName(),
				infoCode.getNum(), user.getNick(), infoText);
			client.sendMessage(infoData);

			// RPL_TOPICINFO
			infoCode = NumericResponse.RPL_TOPICINFO;
			infoText = infoCode.getText()
				.replace("<channel>", channel.getName())
				.replace("<nick>", channel.getTopicChanger())
				.replace("<timestamp>", Integer.toString(channel.getTopicTime()));

			infoData = String.format(":%s %03d %s %s", ctx.getServerName(),
				infoCode.getNum(), user.getNick(), infoText);
			client.sendMessage(infoData);
		}

	}

	@SuppressWarnings({ "unchecked" })
	public void namesMessage(User user, String[] messageParts)
	throws ErrorConditionException, NumericErrorException
	{
        Client client = user.getClient();
    
		if (!user.hasRegistered()) {
			throw new NumericErrorException(NumericResponse.ERR_NOTREGISTERED);
		}

		if (messageParts.length < 2) {
			NumericResponse num = NumericResponse.ERR_NEEDMOREPARAMS;
			String text = num.getText().replace("<command>", messageParts[0]);
			throw new NumericErrorException(num, text);
		}

		// Retrieve the channel. The /names message doesn't return an error
		// when a request is made for a non-existant channel. Instead
		// it returns an empty list of nicks, by sending just the
		// end of names reply.
		String channelName = messageParts[1];
		Channel channel = ctx.getChannel(channelName);
		if (channel != null) {

			// For large channel we will be sending out several messages. However,
			// the beginning of the reply is always the same, so we prepare it
			// separately.
			NumericResponse namReply = NumericResponse.RPL_NAMREPLY;
			String prepend = String.format(":%s %03d %s = %s :",
				ctx.getServerName(), namReply.getNum(), user.getNick(), channelName);

			Set<User> channelUsers = channel.getUsers();

			// Use StringBuffers for speed. For large channels with hundreds of users,
			// this might make a real difference.
			StringBuffer message = new StringBuffer();
			message.append(prepend);

			// Return an arbitrary number of users per row.
			int i = 0, perRow = 10;
			Modes channelModes = protocol.getChannelModes();
			for (User channelUser : channelUsers) {

				// Send a reply every perRow users, and allocate a new buffer.
				if (i % perRow == 0 && i != 0) {
					client.sendMessage(message.toString());

					message = new StringBuffer();
					message.append(prepend);
				}

				ChannelMember member = channel.getUser(channelUser);

				ChannelAttribute[] attrs = new ChannelAttribute[] {
						ChannelAttribute.OP,
						ChannelAttribute.HALFOP,
						ChannelAttribute.VOICE
					};

				String prefix = "";
				for (ChannelAttribute attr : attrs) {
					if (member.getAttribute(attr)) {
						prefix = channelModes.getNickPrefix(attr);
						break;
					}
				}

				message.append(prefix + channelUser.getNick() + " ");
				i++;
			}

			// If userCount % perRow != 0, there will be a number of users
			// remaining in the buffer. Send the last few remaining.
			client.sendMessage(message.toString());
		}

		// Send end of names message.
		NumericResponse endOfNamesReply = NumericResponse.RPL_ENDOFNAMES;
		String endOfNamesData = String.format(":%s %03d %s %s",
			ctx.getServerName(), endOfNamesReply.getNum(), user.getNick(),
			endOfNamesReply.getText().replace("<channel>", channelName));

		client.sendMessage(endOfNamesData);
	}

	public void listMessage(User user, String[] messageParts)
	throws ErrorConditionException, NumericErrorException
	{
        Client client = user.getClient();
    
		if (!user.hasRegistered()) {
			throw new NumericErrorException(NumericResponse.ERR_NOTREGISTERED);
		}

		NumericResponse infoCode;
		String infoText;
		String infoData;

		Collection<Channel> channels = ctx.getChannels();
		for (Channel channel : channels)
		{
			infoCode = NumericResponse.RPL_LIST;
			infoText = infoCode.getText()
				.replace("<channel>", channel.getName())
				.replace("<# visible>", Integer.toString(channel.getUserCount()))
				.replace("<topic>", channel.getTopic());

			infoData = String.format(":%s %03d %s %s", ctx.getServerName(),
				infoCode.getNum(), user.getNick(), infoText);
			client.sendMessage(infoData);
		}

		infoCode = NumericResponse.RPL_LISTEND;
		infoData = String.format(":%s %03d %s %s",
			ctx.getServerName(), infoCode.getNum(), user.getNick(), infoCode.getText());

		client.sendMessage(infoData);
	}

	public void operMessage(User user, String[] messageParts)
	throws ErrorConditionException, NumericErrorException
	{
		try {
            Client client = user.getClient();
        
			if (!user.hasRegistered()) {
				throw new NumericErrorException(NumericResponse.ERR_NOTREGISTERED);
			}

			if (messageParts.length < 2) {
				NumericResponse num = NumericResponse.ERR_NEEDMOREPARAMS;
				String text = num.getText().replace("<command>", messageParts[0]);
				throw new NumericErrorException(num, text);
			}

			Operator operator = ctx.getOperator(messageParts[1]);
			if (operator == null) {
				NumericResponse num = NumericResponse.ERR_NOOPERHOST;
				throw new NumericErrorException(num, num.getText());
			} else
					if (!operator.tryPassword(messageParts[2])) {
						NumericResponse num = NumericResponse.ERR_PASSWDMISMATCH;
						throw new NumericErrorException(num, num.getText());
					}

			ctx.operEvent(user);

			NumericResponse infoCode = NumericResponse.RPL_YOUREOPER;
			String infoData = String.format(":%s %03d %s %s",
				ctx.getServerName(), infoCode.getNum(), user.getNick(), infoCode.getText());

			client.sendMessage(infoData);
		} catch (NoSuchAlgorithmException e) {
			System.err.println(e.getMessage());
			NumericResponse num = NumericResponse.ERR_PASSWDMISMATCH;
			throw new NumericErrorException(num, num.getText());
		}
	}

	public void killMessage(User user, String[] messageParts)
	throws ErrorConditionException, NumericErrorException
	{
		if (!user.hasRegistered()) {
			throw new NumericErrorException(NumericResponse.ERR_NOTREGISTERED);
		}

		if (messageParts.length < 2) {
			NumericResponse num = NumericResponse.ERR_NEEDMOREPARAMS;
			String text = num.getText().replace("<command>", messageParts[0]);
			throw new NumericErrorException(num, text);
		}

		if (!user.getAttribute(UserAttribute.OPERATOR)) {
			NumericResponse num = NumericResponse.ERR_NOPRIVILEGES;
			throw new NumericErrorException(num, num.getText());
		}

		User userToKill = ctx.getUser(messageParts[1]);
		if (userToKill == null) {
			NumericResponse num = NumericResponse.ERR_NOSUCHNICK;
			throw new NumericErrorException(num, num.getText());
		}

		ctx.killEvent(userToKill, user, "Killed by "+user.getNick()+": "+messageParts[2]);
	}

	public void awayMessage(User user, String[] messageParts)
	throws ErrorConditionException, NumericErrorException
	{
        Client client = user.getClient();
    
		if(!user.hasRegistered()) {
			throw new NumericErrorException(NumericResponse.ERR_NOTREGISTERED);
		}

		if(messageParts.length == 1 || messageParts[1].length() == 0) {
			ctx.notAwayEvent(user);
			NumericResponse infoCode = NumericResponse.RPL_UNAWAY;
			String text = String.format(":%s %03d %s %s", ctx.getServerName(), infoCode.getNum(), user.getNick(), infoCode.getText());
			client.sendMessage(text);
		}
		else
		{
			ctx.awayEvent(user, messageParts[1]);
			NumericResponse infoCode = NumericResponse.RPL_NOWAWAY;
			String text = String.format(":%s %03d %s %s", ctx.getServerName(), infoCode.getNum(), user.getNick(), infoCode.getText());
			client.sendMessage(text);
		}
	}

	public void quitMessage(User user, String[] messageParts)
	throws ErrorConditionException, NumericErrorException
	{
		String quitMessage = (messageParts.length > 1 ? messageParts[1] : "Leaving");
		ctx.quitEvent(user, quitMessage);
	}

	public void lusersMessage(User user, String[] messageParts)
	throws ErrorConditionException, NumericErrorException
	{
        Client client = user.getClient();
    
		if (!user.hasRegistered()) {
			throw new NumericErrorException(NumericResponse.ERR_NOTREGISTERED);
		}

		// RPL_LUSERCLIENT
		NumericResponse infoCode = NumericResponse.RPL_LUSERCLIENT;
		String infoText = infoCode.getText()
			.replace("<users>", Integer.toString(ctx.getUserCount()))
			.replace("<services>", Integer.toString(ctx.getServiceCount()))
			.replace("<servers>", Integer.toString(ctx.getServerCount()));
		String infoData = String.format(":%s %03d %s %s", ctx.getServerName(),
			infoCode.getNum(), user.getNick(), infoText);
		client.sendMessage(infoData);

		// RPL_LUSEROP
		infoCode = NumericResponse.RPL_LUSEROP;
		infoText = infoCode.getText()
			.replace("<operators>", Integer.toString(ctx.getOperatorCount()));
		infoData = String.format(":%s %03d %s %s", ctx.getServerName(),
			infoCode.getNum(), user.getNick(), infoText);
		client.sendMessage(infoData);

		// RPL_LUSERUNKNOWN
		infoCode = NumericResponse.RPL_LUSERUNKNOWN;
		infoText = infoCode.getText()
			.replace("<unknowns>", Integer.toString(ctx.getUnknownsCount()));
		infoData = String.format(":%s %03d %s %s", ctx.getServerName(),
			infoCode.getNum(), user.getNick(), infoText);
		client.sendMessage(infoData);

		// RPL_LUSERCHANNELS
		infoCode = NumericResponse.RPL_LUSERCHANNELS;
		infoText = infoCode.getText()
			.replace("<channels>", Integer.toString(ctx.getChannelCount()));
		infoData = String.format(":%s %03d %s %s", ctx.getServerName(),
			infoCode.getNum(), user.getNick(), infoText);
		client.sendMessage(infoData);

		// LUSERME
		infoCode = NumericResponse.RPL_LUSERME;
		infoText = infoCode.getText()
			.replace("<clients>", Integer.toString(ctx.getUserCount()))
			.replace("<servers>", Integer.toString(ctx.getServerCount()));
		infoData = String.format(":%s %03d %s %s", ctx.getServerName(),
			infoCode.getNum(), user.getNick(), infoText);
		client.sendMessage(infoData);
	}

	public void motdMessage(User user, String[] messageParts)
	throws ErrorConditionException, NumericErrorException
	{
        Client client = user.getClient();
    
		if (!user.hasRegistered()) {
			throw new NumericErrorException(NumericResponse.ERR_NOTREGISTERED);
		}

		// RPL_MOTDSTART
		NumericResponse infoCode = NumericResponse.RPL_MOTDSTART;
		String infoText = infoCode.getText()
			.replace("<server>", ctx.getServerName());
		String infoData = String.format(":%s %03d %s %s", ctx.getServerName(),
			infoCode.getNum(), user.getNick(), infoText);
		client.sendMessage(infoData);

		// MOTD
		infoCode = NumericResponse.RPL_MOTD;
		infoText = infoCode.getText()
			.replace("<text>", "Det finns inget MOTD! :D :D :D");
		infoData = String.format(":%s %03d %s %s", ctx.getServerName(),
			infoCode.getNum(), user.getNick(), infoText);
		client.sendMessage(infoData);

		// ENDOFMOTD
		infoCode = NumericResponse.RPL_ENDOFMOTD;
		infoData = String.format(":%s %03d %s %s", ctx.getServerName(),
			infoCode.getNum(), user.getNick(), infoCode.getText());
		client.sendMessage(infoData);
	}

	public void versionMessage(User user, String[] messageParts)
	throws ErrorConditionException, NumericErrorException
	{
		NumericResponse infoCode = NumericResponse.RPL_VERSION;
		String infoText = infoCode.getText()
			.replace("<version>", ctx.getServerVersion())
			.replace("<debuglevel>", Integer.toString(Integer.MAX_VALUE))
			.replace("<server>", ctx.getServerName())
			.replace("<comments>", "Life is a cheesecake!");
		String infoData = String.format(":%s %03d %s %s", ctx.getServerName(),
			infoCode.getNum(), user.getNick(), infoText);

        Client client = user.getClient();
		client.sendMessage(infoData);
	}

	public void whoisMessage(User user, String[] messageParts)
	throws ErrorConditionException, NumericErrorException
	{
        Client client = user.getClient();
    
		if (!user.hasRegistered()) {
			throw new NumericErrorException(NumericResponse.ERR_NOTREGISTERED);
		}

		if (messageParts.length < 2) {
			throw new NumericErrorException(NumericResponse.ERR_NONICKNAMEGIVEN);
		}

		String nick = messageParts[1];
		User queryUser = ctx.getUser(nick);
		if (queryUser == null) {
			NumericResponse num = NumericResponse.ERR_NOSUCHNICK;
			String text = num.getText().replace("<nickname>", nick);
			throw new NumericErrorException(num, text);
		}

		// RPL_WHOISUSER
		NumericResponse whoisUser = NumericResponse.RPL_WHOISUSER;
		String whoisUserText = whoisUser.getText()
			.replace("<nick>", queryUser.getNick())
			.replace("<user>", queryUser.getUser())
			.replace("<host>", queryUser.getHost())
			.replace("<real name>", queryUser.getRealName());
		String whoisUserData = String.format(":%s %03d %s %s", ctx.getServerName(),
			whoisUser.getNum(), user.getNick(), whoisUserText);

		client.sendMessage(whoisUserData);

		// RPL_WHOISSERVER
		NumericResponse whoisServer = NumericResponse.RPL_WHOISSERVER;
		String whoisServerText = whoisServer.getText()
			.replace("<nick>", queryUser.getNick())
			.replace("<server>", ctx.getServerName())
			.replace("<server info>", ctx.getServerInfo());
		String whoisServerData = String.format(":%s %03d %s %s", ctx.getServerName(),
			whoisServer.getNum(), user.getNick(), whoisServerText);

		client.sendMessage(whoisServerData);

		// RPL_WHOISOPERATOR
		if (queryUser.getAttribute(UserAttribute.OPERATOR)) {
			NumericResponse whoisOperator = NumericResponse.RPL_WHOISOPERATOR;
			String whoisOperatorText = whoisOperator.getText()
				.replace("<nick>", queryUser.getNick());
			String whoisOperatorData = String.format(":%s %03d %s %s", ctx.getServerName(),
				whoisOperator.getNum(), user.getNick(), whoisOperatorText);

			client.sendMessage(whoisOperatorData);
		}

		// RPL_WHOISIDLE
		NumericResponse whoisIdle = NumericResponse.RPL_WHOISIDLE;
		String whoisIdleText = whoisIdle.getText()
			.replace("<nick>", queryUser.getNick())
			.replace("<idle>", "" + queryUser.getIdleTime());
		String whoisIdleData = String.format(":%s %03d %s %s", ctx.getServerName(),
			whoisIdle.getNum(), user.getNick(), whoisIdleText);

		client.sendMessage(whoisIdleData);

		// RPL_WHOISCHANNELS
		Set<Channel> channels = queryUser.getChannels();
		if (channels.size() > 0) {
			NumericResponse whoisChannels = NumericResponse.RPL_WHOISCHANNELS;

			StringBuffer channelsBuffer = new StringBuffer();
			channelsBuffer.append(queryUser.getNick());
			channelsBuffer.append(" :");

			for (Channel channel : channels) {
				channelsBuffer.append(channel.getName());
				channelsBuffer.append(" ");
			}

			String whoisChannelsData = String.format(":%s %03d %s %s", ctx.getServerName(),
				whoisChannels.getNum(), user.getNick(), channelsBuffer.toString());

			client.sendMessage(whoisChannelsData);
		}
        
        // RPL_WHOISSPECIAL
		if (queryUser.getAttribute(UserAttribute.MINECRAFT)) {
			NumericResponse special = NumericResponse.RPL_WHOISSPECIAL;
			String specialText = special.getText()
				.replace("<nick>", queryUser.getNick())
				.replace("<message>", "is a Minecraft player.");
			String specialData = String.format(":%s %03d %s %s", ctx.getServerName(),
				special.getNum(), user.getNick(), specialText);

			client.sendMessage(specialData);
		}

		// RPL_ENDOFWHOIS
		NumericResponse endOfWhois = NumericResponse.RPL_ENDOFWHOIS;
		String endOfWhoisText = endOfWhois.getText()
			.replace("<nick>", queryUser.getNick());
		String endOfWhoisData = String.format(":%s %03d %s %s", ctx.getServerName(),
			endOfWhois.getNum(), user.getNick(), endOfWhoisText);

		client.sendMessage(endOfWhoisData);
	}

	public void whoMessage(User user, String[] messageParts)
	throws ErrorConditionException, NumericErrorException
	{
        Client client = user.getClient();
    
		if (!user.hasRegistered()) {
			throw new NumericErrorException(NumericResponse.ERR_NOTREGISTERED);
		}

		if (messageParts.length < 1) {
			throw new NumericErrorException(NumericResponse.ERR_NEEDMOREPARAMS);
		}

		String name = messageParts[1];

		Channel channel = ctx.getChannel(name);

		if (channel != null)
		{
			Set<User> channelUsers = channel.getUsers();
			String channelName = channel.getName();

			for (User channelUser : channelUsers) {
				// RPL_WHOREPLY
				NumericResponse num = NumericResponse.RPL_WHOREPLY;

				ChannelMember channelMember = channel.getUser(channelUser);
				StringBuffer status = new StringBuffer();
				status.append(user.getAttribute(UserAttribute.AWAY) ? "H" : "G");
				status.append(user.getAttribute(UserAttribute.OPERATOR) ? "*" : "");
				status.append(channelMember.getAttribute(ChannelAttribute.OP) ? "@" : "");
				status.append(channelMember.getAttribute(ChannelAttribute.VOICE) ? "+" : "");

				String whoText = num.getText()
					.replace("<channel>", channelName)
					.replace("<user>", channelUser.getUser())
					.replace("<host>", channelUser.getHost())
					.replace("<server>", ctx.getServerName())
					.replace("<nick>", channelUser.getNick())
					.replace("<status>", status)
					.replace("<hopcount>", "0")
					.replace("<real name>", channelUser.getRealName());
				String whoData = String.format(":%s %03d %s %s", ctx.getServerName(),
					num.getNum(), user.getNick(), whoText);

                client.sendMessage(whoData);
			}
		}

		// RPL_ENDOFWHO
		NumericResponse endOfWhois = NumericResponse.RPL_ENDOFWHO;
		String endOfWhoisText = endOfWhois.getText()
			.replace("<name>", name);
		String endOfWhoisData = String.format(":%s %03d %s %s", ctx.getServerName(),
			endOfWhois.getNum(), user.getNick(), endOfWhoisText);

		client.sendMessage(endOfWhoisData);
	}

	public void pingMessage(User user, String[] messageParts)
	throws ErrorConditionException, NumericErrorException
	{
		if (messageParts.length < 2) {
			NumericResponse num = NumericResponse.ERR_NEEDMOREPARAMS;
			String text = num.getText().replace("<command>", messageParts[0]);
			throw new NumericErrorException(num, text);
		}

		String reply = String.format(":%s PONG :%s",
			ctx.getServerName(), messageParts[1]);

        Client client = user.getClient();
		client.sendMessage(reply);
	}

	public void timeMessage(User user, String[] messageParts)
	throws ErrorConditionException, NumericErrorException
	{
		DateFormat dateFormatter;

		dateFormatter = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG);
		Date today = new Date();
		NumericResponse modeIs = NumericResponse.RPL_TIME;

		String text = modeIs.getText()
			.replace("<server>", ctx.getServerName())
			.replace("<time>", dateFormatter.format(today));

        Client client = user.getClient();
		client.sendMessage(text);
	}

	/**
	 * ErrorConditions are protocol independent and are thrown from
	 * the ServerContext. This method translates them into the appropriate
	 * irc specific numeric error codes.
	 */
	public void errorMessage(User user, ErrorCondition code)
	{
		NumericResponse response = null;
		switch (code) {
			case E_NOT_REGISTERED:
				response = NumericResponse.ERR_NOTREGISTERED;
				break;
		}

		errorMessage(user, response, null);
	}

	/**
	 * This method handles NumericErrorExceptions, thrown by other methods
	 * in this class.
	 */
	public void errorMessage(User user, NumericResponse code, String text)
	{
		String actualText = text;
		if (actualText == null) {
			actualText = code.getText();
		}

		String message = String.format(":%s %d %s %s", ctx.getServerName(),
			code.getNum(), (user.getNick() != null ? user.getNick() : ""), actualText);

		Client client = user.getClient();
		client.sendMessage(message);
	}
}
