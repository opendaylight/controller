/*
 * Copyright (c) 2006-2011 Christian Plattner. All rights reserved.
 * Please refer to the LICENSE.txt for licensing details.
 */
package ch.ethz.ssh2.channel;

import java.io.IOException;
import java.io.InputStream;

/**
 * ChannelInputStream.
 * 
 * @author Christian Plattner
 * @version 2.50, 03/15/10
 */
public final class ChannelInputStream extends InputStream
{
	Channel c;

	boolean isClosed = false;
	boolean isEOF = false;
	boolean extendedFlag = false;

	ChannelInputStream(Channel c, boolean isExtended)
	{
		this.c = c;
		this.extendedFlag = isExtended;
	}

	@Override
	public int available() throws IOException
	{
		if (isEOF)
			return 0;

		int avail = c.cm.getAvailable(c, extendedFlag);

		/* We must not return -1 on EOF */

		return (avail > 0) ? avail : 0;
	}

	@Override
	public void close() throws IOException
	{
		isClosed = true;
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException
	{
		if (b == null)
			throw new NullPointerException();

		if ((off < 0) || (len < 0) || ((off + len) > b.length) || ((off + len) < 0) || (off > b.length))
			throw new IndexOutOfBoundsException();

		if (len == 0)
			return 0;

		if (isEOF)
			return -1;

		int ret = c.cm.getChannelData(c, extendedFlag, b, off, len);

		if (ret == -1)
		{
			isEOF = true;
		}

		return ret;
	}

	@Override
	public int read(byte[] b) throws IOException
	{
		return read(b, 0, b.length);
	}

	@Override
	public int read() throws IOException
	{
		/* Yes, this stream is pure and unbuffered, a single byte read() is slow */

		final byte b[] = new byte[1];

		int ret = read(b, 0, 1);

		if (ret != 1)
			return -1;

		return b[0] & 0xff;
	}
}
