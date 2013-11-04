/*
 * Copyright (c) 2006-2013 Christian Plattner. All rights reserved.
 * Please refer to the LICENSE.txt for licensing details.
 */

package ch.ethz.ssh2;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;

import ch.ethz.ssh2.channel.ChannelManager;
import ch.ethz.ssh2.channel.LocalAcceptThread;

/**
 * A <code>LocalPortForwarder</code> forwards TCP/IP connections to a local
 * port via the secure tunnel to another host (which may or may not be identical
 * to the remote SSH-2 server). Checkout {@link Connection#createLocalPortForwarder(int, String, int)}
 * on how to create one.
 * 
 * @author Christian Plattner
 * @version 2.50, 03/15/10
 */
public class LocalPortForwarder
{
	final ChannelManager cm;

	final String host_to_connect;

	final int port_to_connect;

	final LocalAcceptThread lat;

	LocalPortForwarder(ChannelManager cm, int local_port, String host_to_connect, int port_to_connect)
			throws IOException
	{
		this.cm = cm;
		this.host_to_connect = host_to_connect;
		this.port_to_connect = port_to_connect;

		lat = new LocalAcceptThread(cm, local_port, host_to_connect, port_to_connect);
		lat.setDaemon(true);
		lat.start();
	}

	LocalPortForwarder(ChannelManager cm, InetSocketAddress addr, String host_to_connect, int port_to_connect)
			throws IOException
	{
		this.cm = cm;
		this.host_to_connect = host_to_connect;
		this.port_to_connect = port_to_connect;

		lat = new LocalAcceptThread(cm, addr, host_to_connect, port_to_connect);
		lat.setDaemon(true);
		lat.start();
	}

	/**
	 * Return the local socket address of the {@link ServerSocket} used to accept connections.
	 * @return
	 */
	public InetSocketAddress getLocalSocketAddress()
	{
		return (InetSocketAddress) lat.getServerSocket().getLocalSocketAddress();
	}

	/**
	 * Stop TCP/IP forwarding of newly arriving connections.
	 * 
	 * @throws IOException
	 */
	public void close() throws IOException
	{
		lat.stopWorking();
	}
}
