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
 */
public class PacketUserauthSuccess
{
	byte[] payload;


	public PacketUserauthSuccess()
	{
	}

	public PacketUserauthSuccess(byte payload[], int off, int len) throws IOException
	{
		this.payload = new byte[len];
		System.arraycopy(payload, off, this.payload, 0, len);

		TypesReader tr = new TypesReader(payload, off, len);

		int packet_type = tr.readByte();

		if (packet_type != Packets.SSH_MSG_USERAUTH_SUCCESS)
			throw new IOException("This is not a SSH_MSG_USERAUTH_SUCCESS! (" + packet_type + ")");
		
		if (tr.remain() != 0)
			throw new IOException("Padding in SSH_MSG_USERAUTH_SUCCESS packet!");
	}
	
	public byte[] getPayload()
	{
		if (payload == null)
		{
			TypesWriter tw = new TypesWriter();
			tw.writeByte(Packets.SSH_MSG_USERAUTH_SUCCESS);
			payload = tw.getBytes();
		}
		return payload;
	}
}
