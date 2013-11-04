/*
 * Copyright (c) 2006-2011 Christian Plattner. All rights reserved.
 * Please refer to the LICENSE.txt for licensing details.
 */

package ch.ethz.ssh2.transport;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import ch.ethz.ssh2.util.StringEncoder;

/**
 * ClientServerHello.
 * 
 * @author Christian Plattner
 * @version $Id: ClientServerHello.java 52 2013-08-01 13:26:01Z cleondris@gmail.com $
 */
public class ClientServerHello
{
	String client_line;
	String server_line;

	private ClientServerHello(String client_line, String server_line)
	{
		this.client_line = client_line;
		this.server_line = server_line;
	}

	public final static int readLineRN(InputStream is, byte[] buffer) throws IOException
	{
		int pos = 0;
		boolean need10 = false;
		int len = 0;
		while (true)
		{
			int c = is.read();
			if (c == -1)
				throw new IOException("Premature connection close");

			buffer[pos++] = (byte) c;

			if (c == 13)
			{
				need10 = true;
				continue;
			}

			if (c == 10)
				break;

			if (need10 == true)
				throw new IOException("Malformed line received, the line does not end correctly.");

			len++;
			if (pos >= buffer.length)
				throw new IOException("The other party sent a too long line.");
		}

		return len;
	}

	public static ClientServerHello clientHello(String softwareversion, InputStream bi, OutputStream bo)
			throws IOException
	{
		return exchange(softwareversion, bi, bo, true);
	}

	public static ClientServerHello serverHello(String softwareversion, InputStream bi, OutputStream bo)
			throws IOException
	{
		return exchange(softwareversion, bi, bo, false);
	}

	private static ClientServerHello exchange(String softwareversion, InputStream bi, OutputStream bo, boolean clientMode)
			throws IOException
	{
		String localIdentifier = "SSH-2.0-" + softwareversion;
		String remoteIdentifier = null;

		bo.write(StringEncoder.GetBytes(localIdentifier + "\r\n"));
		bo.flush();

		byte[] remoteData = new byte[1024];

		for (int i = 0; i < 50; i++)
		{
			int len = readLineRN(bi, remoteData);

			remoteIdentifier = StringEncoder.GetString(remoteData, 0, len);

			if (remoteIdentifier.startsWith("SSH-"))
				break;
		}

		if (remoteIdentifier.startsWith("SSH-") == false)
			throw new IOException(
					"Malformed SSH identification string. There was no line starting with 'SSH-' amongst the first 50 lines.");

		if (!remoteIdentifier.startsWith("SSH-1.99-") && !remoteIdentifier.startsWith("SSH-2.0-"))
			throw new IOException("Remote party uses incompatible protocol, it is not SSH-2 compatible.");

		if (clientMode)
			return new ClientServerHello(localIdentifier, remoteIdentifier);
		else
			return new ClientServerHello(remoteIdentifier, localIdentifier);
	}

	/**
	 * @return Returns the client_versioncomment.
	 */
	public byte[] getClientString()
	{
		return StringEncoder.GetBytes(client_line);
	}

	/**
	 * @return Returns the server_versioncomment.
	 */
	public byte[] getServerString()
	{
		return StringEncoder.GetBytes(server_line);
	}
}
