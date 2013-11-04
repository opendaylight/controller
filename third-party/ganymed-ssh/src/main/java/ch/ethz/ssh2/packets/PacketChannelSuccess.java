/*
 * Copyright (c) 2006-2013 Christian Plattner. All rights reserved.
 * Please refer to the LICENSE.txt for licensing details.
 */

package ch.ethz.ssh2.packets;

/**
 * PacketChannelSuccess.
 * 
 * @author Christian Plattner
 * @version 2.8
 */
public class PacketChannelSuccess
{
	byte[] payload;

	public int recipientChannelID;

	public PacketChannelSuccess(int recipientChannelID)
	{
		this.recipientChannelID = recipientChannelID;
	}

	public byte[] getPayload()
	{
		if (payload == null)
		{
			TypesWriter tw = new TypesWriter();
			tw.writeByte(Packets.SSH_MSG_CHANNEL_SUCCESS);
			tw.writeUINT32(recipientChannelID);
			payload = tw.getBytes();
		}
		return payload;
	}
}
