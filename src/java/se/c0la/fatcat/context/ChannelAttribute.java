package se.c0la.fatcat.context;

public enum ChannelAttribute
{
	NO_EXTERNAL_MESSAGES (false),
	TOPIC_RESTRICTED (false),
	MODERATED (false),
	KEY (false), 
	INVITE_ONLY (false),
	
	BAN (true),

	FOUNDER (true),
	OP (true),
	HALFOP (true),
	VOICE (true),
	PROTECTED (true);
	
	private boolean memberAttr;
	
	private ChannelAttribute(boolean memberAttr)
	{
		this.memberAttr = memberAttr;
	}
	
	public boolean isMemberAttribute()
	{
		return memberAttr;
	}
}
