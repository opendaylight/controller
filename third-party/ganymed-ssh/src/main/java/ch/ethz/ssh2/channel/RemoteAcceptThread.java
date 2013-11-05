/*
 * Copyright (c) 2006-2011 Christian Plattner. All rights reserved.
 * Please refer to the LICENSE.txt for licensing details.
 */
package ch.ethz.ssh2.channel;

import java.io.IOException;
import java.net.Socket;

import ch.ethz.ssh2.log.Logger;

/**
 * RemoteAcceptThread.
 *
 * @author Christian Plattner
 * @version $Id: RemoteAcceptThread.java 41 2011-06-02 10:36:41Z dkocher@sudo.ch $
 */
public class RemoteAcceptThread extends Thread
{
	private static final Logger log = Logger.getLogger(RemoteAcceptThread.class);

	Channel c;

	String remoteConnectedAddress;
	int remoteConnectedPort;
	String remoteOriginatorAddress;
	int remoteOriginatorPort;
	String targetAddress;
	int targetPort;

	Socket s;

	public RemoteAcceptThread(Channel c, String remoteConnectedAddress, int remoteConnectedPort,
							  String remoteOriginatorAddress, int remoteOriginatorPort, String targetAddress, int targetPort)
	{
		this.c = c;
		this.remoteConnectedAddress = remoteConnectedAddress;
		this.remoteConnectedPort = remoteConnectedPort;
		this.remoteOriginatorAddress = remoteOriginatorAddress;
		this.remoteOriginatorPort = remoteOriginatorPort;
		this.targetAddress = targetAddress;
		this.targetPort = targetPort;

		log.debug("RemoteAcceptThread: " + remoteConnectedAddress + "/" + remoteConnectedPort + ", R: "
				+ remoteOriginatorAddress + "/" + remoteOriginatorPort);
	}

	@Override
	public void run()
	{
		try
		{
			c.cm.sendOpenConfirmation(c);

			s = new Socket(targetAddress, targetPort);

			StreamForwarder r2l = new StreamForwarder(c, null, null, c.getStdoutStream(), s.getOutputStream(),
					"RemoteToLocal");
			StreamForwarder l2r = new StreamForwarder(c, null, null, s.getInputStream(), c.getStdinStream(),
					"LocalToRemote");

			/* No need to start two threads, one can be executed in the current thread */

			r2l.setDaemon(true);
			r2l.start();
			l2r.run();

			while (r2l.isAlive())
			{
				try
				{
					r2l.join();
				}
				catch (InterruptedException ignored)
				{
				}
			}

			/* If the channel is already closed, then this is a no-op */

			c.cm.closeChannel(c, "EOF on both streams reached.", true);
			s.close();
		}
		catch (IOException e)
		{
			log.warning("IOException in proxy code: " + e.getMessage());

			try
			{
				c.cm.closeChannel(c, "IOException in proxy code (" + e.getMessage() + ")", true);
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
