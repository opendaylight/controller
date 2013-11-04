/*
 * Copyright (c) 2006-2011 Christian Plattner. All rights reserved.
 * Please refer to the LICENSE.txt for licensing details.
 */
package ch.ethz.ssh2.packets;

import java.io.IOException;

/**
 * PacketIgnore.
 * 
 * @author Christian Plattner
 * @version 2.50, 03/15/10
 */
public class PacketIgnore
{
	byte[] payload;

	byte[] data;

	public void setData(byte[] data)
	{
		this.data = data;
		payload = null;
	}

	public PacketIgnore()
	{
	}

	public PacketIgnore(byte payload[], int off, int len) throws IOException
	{
		this.payload = new byte[len];
		System.arraycopy(payload, off, this.payload, 0, len);

		TypesReader tr = new TypesReader(payload, off, len);

		int packet_type = tr.readByte();

		if (packet_type != Packets.SSH_MSG_IGNORE)
			throw new IOException("This is not a SSH_MSG_IGNORE packet! (" + packet_type + ")");

		/* Could parse String body */
	}

	public byte[] getPayload()
	{
		if (payload == null)
		{
			TypesWriter tw = new TypesWriter();
			tw.writeByte(Packets.SSH_MSG_IGNORE);

			if (data != null)
				tw.writeString(data, 0, data.length);
			else
				tw.writeString("");

			payload = tw.getBytes();
		}
		return payload;
	}
}
