/*
 * Copyright (c) 2006-2011 Christian Plattner. All rights reserved.
 * Please refer to the LICENSE.txt for licensing details.
 */
package ch.ethz.ssh2;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;

import ch.ethz.ssh2.channel.Channel;
import ch.ethz.ssh2.channel.ChannelManager;

/**
 * A <code>LocalStreamForwarder</code> forwards an Input- and Outputstream
 * pair via the secure tunnel to another host (which may or may not be identical
 * to the remote SSH-2 server).
 *
 * @author Christian Plattner
 * @version 2.50, 03/15/10
 */
public class LocalStreamForwarder
{
	private ChannelManager cm;

	private Channel cn;

	LocalStreamForwarder(ChannelManager cm, String host_to_connect, int port_to_connect) throws IOException
	{
		this.cm = cm;
		cn = cm.openDirectTCPIPChannel(host_to_connect, port_to_connect,
				InetAddress.getLocalHost().getHostAddress(), 0);
	}

	/**
	 * @return An <code>InputStream</code> object.
	 * @throws IOException
	 */
	public InputStream getInputStream() throws IOException
	{
		return cn.getStdoutStream();
	}

	/**
	 * Get the OutputStream. Please be aware that the implementation MAY use an
	 * internal buffer. To make sure that the buffered data is sent over the
	 * tunnel, you have to call the <code>flush</code> method of the
	 * <code>OutputStream</code>. To signal EOF, please use the
	 * <code>close</code> method of the <code>OutputStream</code>.
	 *
	 * @return An <code>OutputStream</code> object.
	 * @throws IOException
	 */
	public OutputStream getOutputStream() throws IOException
	{
		return cn.getStdinStream();
	}

	/**
	 * Close the underlying SSH forwarding channel and free up resources.
	 * You can also use this method to force the shutdown of the underlying
	 * forwarding channel. Pending output (OutputStream not flushed) will NOT
	 * be sent. Pending input (InputStream) can still be read. If the shutdown
	 * operation is already in progress (initiated from either side), then this
	 * call is a no-op.
	 *
	 * @throws IOException
	 */
	public void close() throws IOException
	{
		cm.closeChannel(cn, "Closed due to user request.", true);
	}
}
