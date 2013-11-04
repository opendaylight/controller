/*
 * Copyright (c) 2006-2011 Christian Plattner. All rights reserved.
 * Please refer to the LICENSE.txt for licensing details.
 */
package ch.ethz.ssh2.transport;

import java.io.IOException;

/**
 * MessageHandler.
 * 
 * @author Christian Plattner
 * @version 2.50, 03/15/10
 */
public interface MessageHandler
{
	public void handleMessage(byte[] msg, int msglen) throws IOException;
}
