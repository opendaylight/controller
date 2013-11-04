/*
 * Copyright (c) 2006-2011 Christian Plattner. All rights reserved.
 * Please refer to the LICENSE.txt for licensing details.
 */
package ch.ethz.ssh2;

/**
 * A <code>SFTPv3FileHandle</code>.
 *
 * @author Christian Plattner
 * @version 2.50, 03/15/10
 */

public class SFTPv3FileHandle
{
	protected final SFTPv3Client client;
	protected final byte[] fileHandle;
	protected boolean isClosed;

	protected SFTPv3FileHandle(SFTPv3Client client, byte[] h)
	{
		this.client = client;
		this.fileHandle = h;
	}

	/**
	 * Get the SFTPv3Client instance which created this handle.
	 *
	 * @return A SFTPv3Client instance.
	 */
	public SFTPv3Client getClient()
	{
		return client;
	}

	/**
	 * Check if this handle was closed with the {@link SFTPv3Client#closeFile(SFTPv3FileHandle)} method
	 * of the <code>SFTPv3Client</code> instance which created the handle.
	 *
	 * @return if the handle is closed.
	 */
	public boolean isClosed()
	{
		return isClosed;
	}
}
