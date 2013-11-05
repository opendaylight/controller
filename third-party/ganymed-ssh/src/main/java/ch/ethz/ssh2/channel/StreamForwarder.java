/*
 * Copyright (c) 2006-2011 Christian Plattner. All rights reserved.
 * Please refer to the LICENSE.txt for licensing details.
 */
package ch.ethz.ssh2.channel;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

/**
 * A StreamForwarder forwards data between two given streams. 
 * If two StreamForwarder threads are used (one for each direction)
 * then one can be configured to shutdown the underlying channel/socket
 * if both threads have finished forwarding (EOF).
 * 
 * @author Christian Plattner
 * @version 2.50, 03/15/10
 */
public class StreamForwarder extends Thread
{
	OutputStream os;
	InputStream is;
	byte[] buffer = new byte[Channel.CHANNEL_BUFFER_SIZE];
	Channel c;
	StreamForwarder sibling;
	Socket s;
	String mode;

	StreamForwarder(Channel c, StreamForwarder sibling, Socket s, InputStream is, OutputStream os, String mode)
			throws IOException
	{
		this.is = is;
		this.os = os;
		this.mode = mode;
		this.c = c;
		this.sibling = sibling;
		this.s = s;
	}

	@Override
	public void run()
	{
		try
		{
			while (true)
			{
				int len = is.read(buffer);
				if (len <= 0)
					break;
				os.write(buffer, 0, len);
				os.flush();
			}
		}
		catch (IOException ignore)
		{
			try
			{
				c.cm.closeChannel(c, "Closed due to exception in StreamForwarder (" + mode + "): "
						+ ignore.getMessage(), true);
			}
			catch (IOException ignored)
			{
			}
		}
		finally
		{
			try
			{
				os.close();
			}
			catch (IOException ignored)
			{
			}
			try
			{
				is.close();
			}
			catch (IOException ignored)
			{
			}

			if (sibling != null)
			{
				while (sibling.isAlive())
				{
					try
					{
						sibling.join();
					}
					catch (InterruptedException ignored)
					{
					}
				}

				try
				{
					c.cm.closeChannel(c, "StreamForwarder (" + mode + ") is cleaning up the connection", true);
				}
				catch (IOException ignored)
				{
				}

				try
				{
					if (s != null)
						s.close();
				}
				catch (IOException ignored)
				{
				}
			}
		}
	}
}