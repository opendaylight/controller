/*
 * Copyright (c) 2012-2013 Christian Plattner. All rights reserved.
 * Please refer to the LICENSE.txt for licensing details.
 */
package ch.ethz.ssh2;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * A ServerSession represents the server-side of a session channel.
 * 
 * @see Session
 * 
 * @author Christian
 *
 */
public interface ServerSession
{
	public InputStream getStdout();

	public InputStream getStderr();

	public OutputStream getStdin();

	public void close();
}