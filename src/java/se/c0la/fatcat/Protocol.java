package se.c0la.fatcat;

import java.io.*;
import java.net.*;

import se.c0la.fatcat.irc.*;
import se.c0la.fatcat.context.*;
import se.c0la.fatcat.async.*;

public interface Protocol
{
	public ReceiverProtocol getReceiverProtocol();
	public PropagationProtocol getPropagationProtocol();
}
