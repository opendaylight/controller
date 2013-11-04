/*
 * Copyright (c) 2006-2013 Christian Plattner. All rights reserved.
 * Please refer to the LICENSE.txt for licensing details.
 */
package ch.ethz.ssh2.packets;

import java.io.IOException;

/**
 * PacketUserauthBanner.
 * 
 * @author Christian Plattner
 * @version 2.50, 03/15/10
 */
public class PacketUserauthFailure
{
	byte[] payload;

	String[] authThatCanContinue;
	boolean partialSuccess;

	public PacketUserauthFailure(String[] authThatCanContinue, boolean partialSuccess)
	{
		this.authThatCanContinue = authThatCanContinue;
		this.partialSuccess = partialSuccess;
	}

	public PacketUserauthFailure(byte payload[], int off, int len) throws IOException
	{
		this.payload = new byte[len];
		System.arraycopy(payload, off, this.payload, 0, len);

		TypesReader tr = new TypesReader(payload, off, len);

		int packet_type = tr.readByte();

		if (packet_type != Packets.SSH_MSG_USERAUTH_FAILURE)
			throw new IOException("This is not a SSH_MSG_USERAUTH_FAILURE! (" + packet_type + ")");

		authThatCanContinue = tr.readNameList();
		partialSuccess = tr.readBoolean();

		if (tr.remain() != 0)
			throw new IOException("Padding in SSH_MSG_USERAUTH_FAILURE packet!");
	}

	public byte[] getPayload()
	{
		if (payload == null)
		{
			TypesWriter tw = new TypesWriter();
			tw.writeByte(Packets.SSH_MSG_USERAUTH_FAILURE);
			tw.writeNameList(authThatCanContinue);
			tw.writeBoolean(partialSuccess);
			payload = tw.getBytes();
		}
		return payload;
	}

	public String[] getAuthThatCanContinue()
	{
		return authThatCanContinue;
	}

	public boolean isPartialSuccess()
	{
		return partialSuccess;
	}
}
