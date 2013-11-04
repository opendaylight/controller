/*
 * Copyright (c) 2011 David Kocher. All rights reserved.
 * Please refer to the LICENSE.txt for licensing details.
 */

package ch.ethz.ssh2.channel;

import java.io.IOException;

/**
 * @version $Id: ChannelClosedException.java 3183 2007-07-30 19:22:34Z dkocher $
 */
public class ChannelClosedException extends IOException
{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public ChannelClosedException(String s)
	{
		super(s);
	}
}
