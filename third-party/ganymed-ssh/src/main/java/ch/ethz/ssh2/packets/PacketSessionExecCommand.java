/*
 * Copyright (c) 2006-2011 Christian Plattner. All rights reserved.
 * Please refer to the LICENSE.txt for licensing details.
 */
package ch.ethz.ssh2.packets;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

/**
 * PacketSessionExecCommand.
 *
 * @author Christian Plattner
 * @version $Id: PacketSessionExecCommand.java 5 2011-05-27 12:59:54Z dkocher@sudo.ch $
 */
public class PacketSessionExecCommand
{
	byte[] payload;

	public int recipientChannelID;
	public boolean wantReply;
	public String command;

	public PacketSessionExecCommand(int recipientChannelID, boolean wantReply, String command)
	{
		this.recipientChannelID = recipientChannelID;
		this.wantReply = wantReply;
		this.command = command;
	}

	public byte[] getPayload() throws IOException
	{
		return this.getPayload(null);
	}

	public byte[] getPayload(String charsetName) throws UnsupportedEncodingException
	{
		if (payload == null)
		{
			TypesWriter tw = new TypesWriter();
			tw.writeByte(Packets.SSH_MSG_CHANNEL_REQUEST);
			tw.writeUINT32(recipientChannelID);
			tw.writeString("exec");
			tw.writeBoolean(wantReply);
			tw.writeString(command, charsetName);
			payload = tw.getBytes();
		}
		return payload;
	}
}
