package se.c0la.fatcat.irc;

import java.util.*;

import se.c0la.fatcat.*;
import se.c0la.fatcat.context.*;
import se.c0la.fatcat.async.*;

/**
 * Utility class that handles operation related to IRC modes.
 */
public class Modes<E>
{
	/**
	 * Hidden class used to associate channel attributes with IRC channel modes.
	 */
	public static class ModeInfo<F>
	{
		public final char modeLetter;
		public final boolean hasParam;
		public final F attr;
		public final Character nickPrefix;
		
		public ModeInfo(char modeLetter, boolean hasParam, F attr)
		{
			this.modeLetter = modeLetter;
			this.hasParam = hasParam;
			this.attr = attr;
			this.nickPrefix = null;
		}
	
		public ModeInfo(char modeLetter, boolean hasParam, F attr, Character nickPrefix)
		{
			this.modeLetter = modeLetter;
			this.hasParam = hasParam;
			this.attr = attr;
			this.nickPrefix = nickPrefix;
		}
		
		public String getModeLetter()
		{
			return new String(new char[] { modeLetter });
		}
		
		public String getNickPrefix()
		{
			return new String(new char[] { nickPrefix });
		}
	}
	
	// A list of all modes
	private List<ModeInfo> modes;
	
	// Hidden translation tables
	private Map<Character, ModeInfo> lettersMap;
	private Map<E, ModeInfo> attributesMap;
	
	public Modes()
	{
		modes = new ArrayList<ModeInfo>();
		lettersMap = new HashMap<Character, ModeInfo>();
		attributesMap = new HashMap<E, ModeInfo>();
	}
	
	public void addMode(ModeInfo<E> mode)
	{
		modes.add(mode);
		lettersMap.put(mode.modeLetter, mode);
		attributesMap.put(mode.attr, mode);	
	}
	
	/**
	 * Return a summary of all channel modes supported by the server.
	 */
	public String getModeList()
	{
		StringBuffer buffer = new StringBuffer();
		for (ModeInfo mode : modes) {
			buffer.append(mode.modeLetter);
		}
		
		return buffer.toString();
	}
	
	/**
	 * Translate a channel attribute into an IRC nick prefix.
	 */
	public String getNickPrefix(ChannelAttribute attr)
	{
		ModeInfo mode = attributesMap.get(attr);
		if (mode == null) {
			return null;
		}
		
		return mode.getNickPrefix();
	}

	/**
	 * Parse an IRC mode string and return a list of AttributeChange's.
	 */
	public List<AttributeChange> 
	parseAttributes(String mode, String[] params)
	{
		List<AttributeChange> channelAttrs = 
			new ArrayList<AttributeChange>();
			
		try {
			char prefix = '+';
			int paramPos = 0;
			for (int i = 0; i < mode.length(); i++) {
				char cur = mode.charAt(i);
				if (cur == '-' || cur == '+') {
					prefix = cur;
					continue;
				}
				
				Object attr = null;
				String parameter = null;
				
				ModeInfo modeInfo = lettersMap.get(cur);
				if (modeInfo == null) {
					continue;
				}
				
				attr = modeInfo.attr;
				if (modeInfo.hasParam) {
					parameter = params[paramPos++];
				}
				
				boolean set = true;
				if (prefix == '-') {
					set = false;
				}
				
				channelAttrs.add(new AttributeChange(attr, set, parameter));
			}
		}
		catch (IndexOutOfBoundsException e) {
			e.printStackTrace();
			// ignore
		}
		
		return channelAttrs;
	}
	
	/**
	 * Convert a map of ChannelAttribute's to an IRC mode string.
	 */
	public String serialize(Map<E, AttributeParameter> attrs)
	{
		List<AttributeChange> tmp = new ArrayList<AttributeChange>();
		for (Map.Entry<E, AttributeParameter> entry : attrs.entrySet()) {
			E attr = entry.getKey();
			AttributeParameter param = entry.getValue();
			
			AttributeChange change = new AttributeChange(attr, true, param.getData());
			tmp.add(change);
		}
		
		return serialize(tmp);
	}
	
	public String serialize(Set<E> attrs)
	{
		List<AttributeChange> tmp = new ArrayList<AttributeChange>();
		for (E attr : attrs) {
			AttributeChange change = new AttributeChange(attr, true, null);
			tmp.add(change);
		}
		
		return serialize(tmp);
	}

	/**
	 * Convert a list of AttributeChange's to an IRC mode string.
	 */
	public String serialize(List<AttributeChange> attrs)
	{
		StringBuffer negative = new StringBuffer();
		StringBuffer positive = new StringBuffer();
		StringBuffer negativeParams = new StringBuffer();
		StringBuffer positiveParams = new StringBuffer();
		
		for (AttributeChange attrChange : attrs) {
			Object attr = attrChange.getAttribute();
			Object param = attrChange.getParameter();
			
			ModeInfo info = attributesMap.get(attr);
			if (info == null) {
				continue;
			}
			
			String mode = info.getModeLetter();
			
			if (attrChange.isSet()) {
				positive.append(mode);
				if (param != null) {
					positiveParams.append(param);
					positiveParams.append(" ");
				}
			}
			else {
				negative.append(mode);
				if (param != null) {
					negativeParams.append(param);
					negativeParams.append(" ");
				}
			}
		}
		
		StringBuffer modeString = new StringBuffer();
		if (negative.length() > 0) {
			modeString.append("-");
			modeString.append(negative);
		}
		if (positive.length() > 0) {
			modeString.append("+");
			modeString.append(positive);
		}
		
		if (negativeParams.length() > 0 || positiveParams.length() > 0) {
			modeString.append(" ");
			if (negativeParams.length() > 0) {
				modeString.append(negativeParams);
			}
			if (positiveParams.length() > 0) {
				modeString.append(positiveParams);
			}
		}
		
		return modeString.toString();
	}
}
