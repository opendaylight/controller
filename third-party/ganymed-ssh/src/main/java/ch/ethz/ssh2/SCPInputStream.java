/*
 * Copyright (c) 2011 David Kocher. All rights reserved.
 * Please refer to the LICENSE.txt for licensing details.
 */
package ch.ethz.ssh2;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * @version $Id:$
 */
public class SCPInputStream extends BufferedInputStream
{
	private Session session;

	/**
	 * Bytes remaining to be read from the stream
	 */
	private long remaining;

	public SCPInputStream(SCPClient client, Session session) throws IOException
	{
		super(session.getStdout());

		this.session = session;

		OutputStream os = new BufferedOutputStream(session.getStdin(), 512);

		os.write(0x0);
		os.flush();

		final SCPClient.LenNamePair lnp;

		while (true)
		{
			int c = session.getStdout().read();
			if (c < 0)
			{
				throw new IOException("Remote scp terminated unexpectedly.");
			}

			String line = client.receiveLine(session.getStdout());

			if (c == 'T')
			{
				/* Ignore modification times */
				continue;
			}

			if ((c == 1) || (c == 2))
			{
				throw new IOException("Remote SCP error: " + line);
			}

			if (c == 'C')
			{
				lnp = client.parseCLine(line);
				break;

			}
			throw new IOException("Remote SCP error: " + ((char) c) + line);
		}

		os.write(0x0);
		os.flush();

		this.remaining = lnp.length;
	}

	@Override
	public int read() throws IOException
	{
		if (!(remaining > 0))
		{
			return -1;
		}

		int read = super.read();
		if (read < 0)
		{
			throw new IOException("Remote scp terminated connection unexpectedly");
		}

		remaining -= read;

		return read;
	}

	@Override
	public int read(byte b[], int off, int len) throws IOException
	{
		if (!(remaining > 0))
		{
			return -1;
		}

		int trans = (int) remaining;
		if (remaining > len)
		{
			trans = len;
		}

		int read = super.read(b, off, trans);
		if (read < 0)
		{
			throw new IOException("Remote scp terminated connection unexpectedly");
		}

		remaining -= read;

		return read;
	}

	@Override
	public void close() throws IOException
	{
		try
		{
			session.getStdin().write(0x0);
			session.getStdin().flush();
		}
		finally
		{
			if (session != null)
			{
				session.close();
			}
		}
	}
}
