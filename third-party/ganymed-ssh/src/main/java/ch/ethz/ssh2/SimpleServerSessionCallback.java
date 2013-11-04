/*
 * Copyright (c) 2006-2013 Christian Plattner. All rights reserved.
 * Please refer to the LICENSE.txt for licensing details.
 */

package ch.ethz.ssh2;

import java.io.IOException;

/**
 * A basic ServerSessionCallback implementation.
 * <p>
 * Note: you should derive from this class instead of implementing
 * the {@link ServerSessionCallback} interface directly. This way
 * your code works also in case the interface gets extended in future
 * versions.
 * 
 * @author Christian
 *
 */
public class SimpleServerSessionCallback implements ServerSessionCallback
{
	public Runnable requestShell(ServerSession ss) throws IOException
	{
		return null;
	}

	public Runnable requestExec(ServerSession ss, String command) throws IOException
	{
		return null;
	}

	public Runnable requestSubsystem(ServerSession ss, String subsystem) throws IOException
	{
		return null;
	}
	
	public Runnable requestPtyReq(ServerSession ss, PtySettings pty) throws IOException
	{
		return null;
	}

	/**
	 * By default, silently ignore passwd environment variables.
	 */
	public Runnable requestEnv(ServerSession ss, String name, String value) throws IOException
	{
		return new Runnable()
		{	
			public void run()
			{
				/* Do nothing */
			}
		};
	}

	public void requestWindowChange(ServerSession ss, int term_width_columns, int term_height_rows,
			int term_width_pixels, int term_height_pixels) throws IOException
	{
	}

}
