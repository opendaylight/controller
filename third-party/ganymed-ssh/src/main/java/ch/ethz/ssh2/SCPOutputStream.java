/*
 * Copyright (c) 2011 David Kocher. All rights reserved.
 * Please refer to the LICENSE.txt for licensing details.
 */
package ch.ethz.ssh2;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;

import ch.ethz.ssh2.util.StringEncoder;

/**
 * @version $Id:$
 */
public class SCPOutputStream extends BufferedOutputStream
{

	private Session session;

	private SCPClient scp;

	public SCPOutputStream(SCPClient client, Session session, final String remoteFile, long length, String mode) throws IOException
	{
		super(session.getStdin(), 40000);
		this.session = session;
		this.scp = client;

		InputStream is = new BufferedInputStream(session.getStdout(), 512);

		scp.readResponse(is);

		String cline = "C" + mode + " " + length + " " + remoteFile + "\n";

		super.write(StringEncoder.GetBytes(cline));
		this.flush();

		scp.readResponse(is);
	}

	@Override
	public void close() throws IOException
	{
		try
		{
			this.write(0);
			this.flush();

			scp.readResponse(session.getStdout());

			this.write(StringEncoder.GetBytes("E\n"));
			this.flush();
		}
		finally
		{
			if (session != null)
				session.close();
		}
	}
}
