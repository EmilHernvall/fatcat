package se.c0la.fatcat.irc;

public enum NumericResponse
{
	RPL_WELCOME (1, ":Welcome to the Internet Relay Network <nick>!<user>@<host>"),
	RPL_YOURHOST (2, ":Your host is <servername>, running version <ver>"),
	RPL_CREATED (3, ":This server was created <date>"),
	RPL_MYINFO (4, "<servername> <version> <available user modes> <available channel modes>"),
	
	RPL_UMODEIS (221, "<umode>"),
	
	RPL_LUSERCLIENT (251, ":There are <users> users and <services> services on <servers> servers"),
	RPL_LUSEROP (252, "<operators> :operator(s) online"),
	RPL_LUSERUNKNOWN (253, "<unknowns> :unknown connection(s)"),
	RPL_LUSERCHANNELS (254, "<channels> :channels formed"),
	RPL_LUSERME (255, ":I have <clients> clients and <servers> servers"),
	
	// Whois responses
	RPL_WHOISUSER (311, "<nick> <user> <host> * :<real name>"),
	RPL_WHOISSERVER (312, "<nick> <server> :<server info>"),
	RPL_WHOISOPERATOR (313, "<nick> :is an IRC operator"),
	RPL_ENDOFWHO (315, "<name> :End of WHO list"),
	RPL_WHOISIDLE (317, "<nick> <idle> :seconds idle"),
	RPL_ENDOFWHOIS (318, "<nick> :End of WHOIS list"),
	RPL_WHOISCHANNELS (319, ""),

	RPL_LIST (322, "<channel> <# visible> :<topic>"),
	RPL_LISTEND (323, ":End of LIST"),
	RPL_CHANNELMODEIS (324, "<channel> <mode> <mode params>"),
	RPL_UNIQOPIS (325, "<channel> <nickname>"),

	RPL_NOTOPIC (331, "<channel> :No topic is set"),
	RPL_TOPIC (332, "<channel> :<topic>"),
	RPL_TOPICINFO (333, "<channel> <nick> <timestamp>"),

	RPL_INVITELIST (346, "<channel> <invitemask>"),
	RPL_ENDOFINVITELIST (347, "<channel> :End of channel invite list"),
	RPL_EXCEPTLIST (348 ,"<channel> <exceptionmask>"),
	RPL_ENDOFEXCEPTLIST (349, "<channel> :End of channel exception list"),

	RPL_VERSION (351, "<version>.<debuglevel> <server> :<comments>"),
	RPL_WHOREPLY (352, "<channel> <user> <host> <server> <nick> <status> :<hopcount> <real name>"),
	RPL_NAMREPLY (353, ""),
	
	RPL_LINKS (364, "<mask> <server> :<hopcount> <server info>"),
	RPL_ENDOFLINKS (365, "<mask> :End of LINKS list"),
	RPL_ENDOFNAMES (366, "<channel> :End of NAMES list"),
	RPL_BANLIST (367, "<channel> <banmask>"),
	RPL_ENDOFBANLIST (368,  "<channel> :End of channel ban list"),
	
	RPL_INFO (371, ":<string>"),
	RPL_ENDOFINFO (374, ":End of INFO list"),
	RPL_MOTDSTART (375, ":- <server> Message of the day - "),
	RPL_MOTD (372, ":- <text>"),
	RPL_ENDOFMOTD (376, ":End of MOTD command"),

	RPL_YOUREOPER (381, ":You are now an IRC operator"),

	RPL_TIME (391, "<server> :<time>"),

	// Away replies
	RPL_AWAY (301, "<nick> :<away message>"),
	RPL_UNAWAY (305, ":You are no longer marked as being away"),
	RPL_NOWAWAY (306, ":You have been marked as being away"),
	
	// Error replies
	ERR_NOSUCHNICK (401, "<nickname> :No such nick/channel"),
	ERR_NOSUCHSERVER (402, "<servername> :No such server"),
	ERR_NOSUCHCHANNEL (403, "<channel> :No such channel"),
	ERR_CANNOTSENDTOCHAN (404, "<channel> :Cannot send to channel"),
	ERR_TOOMANYCHANNELS (405, "<channel> :You have joined too many channels"),
	ERR_WASNOSUCHNICK (406, "<nickname> :There was no such nickname"),
	ERR_TOOMANYTARGETS (407, "<target> :<errorcode> recipients. <abortmessage>"),
	ERR_NOSUCHSERVICE (408, "<servicename> :No such service"),
	ERR_NOORIGIN (409, ":No origin specified"),

	ERR_NORECIPIENT (411, ":No recipient given (<command>)"),
	ERR_NOTEXTTOSEND (412, ":No text to send"),
	ERR_NOTOPLEVEL (413, "<mask> :No toplevel domain specified"),
	ERR_WILDTOPLEVEL (414, "<mask> :Wildcard in toplevel domain"),
	ERR_BADMASK (415, "<mask> :Bad Server/host mask"),

	ERR_UNKNOWNCOMMAND (421, "<command> :Unknown command"),
	ERR_NOMOTD (422, ":MOTD File is missing"),
	ERR_NOADMININFO (423, "<server> :No administrative info available"),
	ERR_FILEERROR (424, ":File error doing <fileoperation> on <file>"),

	ERR_NONICKNAMEGIVEN (431, ":No nickname given"),
	ERR_ERRONEUSNICKNAME (432, "<nick> :Erroneous nickname"),
	ERR_NICKNAMEINUSE (433, "<nick> :Nickname is already in use"),
	ERR_NICKCOLLISION (436, "<nick> :Nickname collision KILL from <user>@<host>"),
	ERR_UNAVAILRESOURCE (437, "<nick>/<channel> :Nick/channel is temporarily unavailable"),

	ERR_USERNOTINCHANNEL (441, "<nick> <channel> :They aren't on that channel"),
	ERR_NOTONCHANNEL (442, "<channel> :You're not on that channel"),
	ERR_USERONCHANNEL (443, "<user> <channel> :is already on channel"),
	ERR_NOLOGIN (444, "<user> :User not logged in"),
	ERR_SUMMONDISABLED (445, ":SUMMON has been disabled"),
	ERR_USERSDISABLED (446, ":USERS has been disabled"),

	ERR_NOTREGISTERED (451, ":You have not registered"),

	ERR_NEEDMOREPARAMS (461, "<command> :Not enough parameters"),
	ERR_ALREADYREGISTRED (462, ":Unauthorized command (already registered)"),
	ERR_NOPERMFORHOST (463, ":Your host isn't among the privileged"),
	ERR_PASSWDMISMATCH (464, ":Password incorrect"),
	ERR_YOUREBANNEDCREEP (465, ":You are banned from this server"),
	ERR_YOUWILLBEBANNED (466, ""),
	ERR_KEYSET (467, "<channel> :Channel key already set"),

	ERR_CHANNELISFULL (471, "<channel> :Cannot join channel (+l)"),
	ERR_UNKNOWNMODE (472, "<char> :is unknown mode char to me for <channel>"),
	ERR_INVITEONLYCHAN (473, "<channel> :Cannot join channel (+i)"),
	ERR_BANNEDFROMCHAN (474, "<channel> :Cannot join channel (+b)"),
	ERR_BADCHANNELKEY (475, "<channel> :Cannot join channel (+k)"),
	ERR_BADCHANMASK (476, "<channel> :Bad Channel Mask"),
	ERR_NOCHANMODES (477, "<channel> :Channel doesn't support modes"),
	ERR_BANLISTFULL (478, "<channel> <char> :Channel list is full"),

	ERR_NOPRIVILEGES (481, ":Permission Denied- You're not an IRC operator"),
	ERR_CHANOPRIVSNEEDED (482, "<channel> :You're not channel operator"),
	ERR_CANTKILLSERVER (483, ":You can't kill a server!"),
	ERR_RESTRICTED (484, ":Your connection is restricted!"),
	ERR_UNIQOPPRIVSNEEDED (485, ":You're not the original channel operator"),

	ERR_NOOPERHOST (491, ":No O-lines for your host");
	
	private int num;
	private String text;
	
	private NumericResponse(int num, String text)
	{
		this.num = num;
		this.text = text;
	}
	
	public int getNum()
	{
		return num;
	}
	
	public String getText()
	{
		return text;
	}
}
