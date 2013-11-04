/*
 * Copyright (c) 2006-2011 Christian Plattner. All rights reserved.
 * Please refer to the LICENSE.txt for licensing details.
 */

package ch.ethz.ssh2.log;

import java.util.logging.Level;

/**
 * Logger delegating to JRE logging. By default, this is disabled and only
 * used during development.
 *
 * @author Christian Plattner
 * @version $Id: Logger.java 49 2013-08-01 12:28:42Z cleondris@gmail.com $
 */
public class Logger
{
	private java.util.logging.Logger delegate;

	public static volatile boolean enabled = false;

	public static Logger getLogger(Class<?> x)
	{
		return new Logger(x);
	}

	public Logger(Class<?> x)
	{
		this.delegate = java.util.logging.Logger.getLogger(x.getName());
	}

	public boolean isDebugEnabled()
	{
		return enabled && delegate.isLoggable(Level.FINER);
	}

	public void debug(String message)
	{
		if (enabled)
			delegate.fine(message);
	}

	public boolean isInfoEnabled()
	{
		return enabled && delegate.isLoggable(Level.FINE);
	}

	public void info(String message)
	{
		if (enabled)
			delegate.info(message);
	}

	public boolean isWarningEnabled()
	{
		return enabled && delegate.isLoggable(Level.WARNING);
	}

	public void warning(String message)
	{
		if (enabled)
			delegate.warning(message);
	}
}