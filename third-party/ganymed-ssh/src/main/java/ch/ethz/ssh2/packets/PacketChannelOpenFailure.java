/*
 * Copyright (c) 2006-2011 Christian Plattner. All rights reserved.
 * Please refer to the LICENSE.txt for licensing details.
 */
package ch.ethz.ssh2.packets;

import java.io.IOException;

/**
 * PacketChannelOpenFailure.
 * 
 * @author Christian Plattner
 * @version 2.50, 03/15/10
 */
public class PacketChannelOpenFailure
{
	byte[] payload;

	public int recipientChannelID;
	public int reasonCode;
	public String description;
	public String languageTag;

	public PacketChannelOpenFailure(int recipientChannelID, int reasonCode, String description,
			String languageTag)
	{
		this.recipientChannelID = recipientChannelID;
		this.reasonCode = reasonCode;
		this.description = description;
		this.languageTag = languageTag;
	}

	public PacketChannelOpenFailure(byte payload[], int off, int len) throws IOException
	{
		this.payload = new byte[len];
		System.arraycopy(payload, off, this.payload, 0, len);

		TypesReader tr = new TypesReader(payload, off, len);

		int packet_type = tr.readByte();

		if (packet_type != Packets.SSH_MSG_CHANNEL_OPEN_FAILURE)
			throw new IOException(
					"This is not a SSH_MSG_CHANNEL_OPEN_FAILURE! ("
							+ packet_type + ")");

		recipientChannelID = tr.readUINT32();
		reasonCode = tr.readUINT32();
		description = tr.readString();
		languageTag = tr.readString();
		
		if (tr.remain() != 0)
			throw new IOException("Padding in SSH_MSG_CHANNEL_OPEN_FAILURE packet!");
	}

	public byte[] getPayload()
	{
		if (payload == null)
		{
			TypesWriter tw = new TypesWriter();
			tw.writeByte(Packets.SSH_MSG_CHANNEL_OPEN_FAILURE);
			tw.writeUINT32(recipientChannelID);
			tw.writeUINT32(reasonCode);
			tw.writeString(description);
			tw.writeString(languageTag);
			payload = tw.getBytes();
		}
		return payload;
	}
}
