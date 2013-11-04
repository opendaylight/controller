/*
 * Copyright (c) 2006-2011 Christian Plattner. All rights reserved.
 * Please refer to the LICENSE.txt for licensing details.
 */
package ch.ethz.ssh2.channel;

import java.io.IOException;
import java.io.OutputStream;

/**
 * ChannelOutputStream.
 * 
 * @author Christian Plattner
 * @version 2.50, 03/15/10
 */
public final class ChannelOutputStream extends OutputStream
{
	Channel c;

	boolean isClosed = false;
	
	ChannelOutputStream(Channel c)
	{
		this.c = c;
	}

	@Override
	public void write(int b) throws IOException
	{	
		byte[] buff = new byte[1];
		
		buff[0] = (byte) b;
		
		write(buff, 0, 1);
	}

	@Override
	public void close() throws IOException
	{
		if (isClosed == false)
		{
			isClosed = true;
			c.cm.sendEOF(c);
		}
	}

	@Override
	public void flush() throws IOException
	{
		if (isClosed)
			throw new IOException("This OutputStream is closed.");

		/* This is a no-op, since this stream is unbuffered */
	}

	@Override
	public void write(byte[] b, int off, int len) throws IOException
	{
		if (isClosed)
			throw new IOException("This OutputStream is closed.");
		
		if (b == null)
			throw new NullPointerException();

		if ((off < 0) || (len < 0) || ((off + len) > b.length) || ((off + len) < 0) || (off > b.length))
			throw new IndexOutOfBoundsException();

		if (len == 0)
			return;
		
		c.cm.sendData(c, b, off, len);
	}

	@Override
	public void write(byte[] b) throws IOException
	{
		write(b, 0, b.length);
	}
}
