/*
 * Copyright (c) 2006-2011 Christian Plattner. All rights reserved.
 * Please refer to the LICENSE.txt for licensing details.
 */
package ch.ethz.ssh2.packets;

/**
 * PacketSessionStartShell.
 * 
 * @author Christian Plattner
 * @version 2.50, 03/15/10
 */
public class PacketSessionStartShell
{
	byte[] payload;

	public int recipientChannelID;
	public boolean wantReply;

	public PacketSessionStartShell(int recipientChannelID, boolean wantReply)
	{
		this.recipientChannelID = recipientChannelID;
		this.wantReply = wantReply;
	}

	public byte[] getPayload()
	{
		if (payload == null)
		{
			TypesWriter tw = new TypesWriter();
			tw.writeByte(Packets.SSH_MSG_CHANNEL_REQUEST);
			tw.writeUINT32(recipientChannelID);
			tw.writeString("shell");
			tw.writeBoolean(wantReply);
			payload = tw.getBytes();
		}
		return payload;
	}
}
