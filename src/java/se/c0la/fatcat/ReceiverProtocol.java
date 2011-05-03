package se.c0la.fatcat;

import java.util.*;

import se.c0la.fatcat.context.*;

/**
 * The ReceiverProtocol is responsible for parsing the message
 * and sending whatever is specified by the protocol to the source
 * client. However, any further propagation of the message should
 * be done by the ServerContext.
 */
public interface ReceiverProtocol
{
	public void translateMessage(User user, String message);
}
