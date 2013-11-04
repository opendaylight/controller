/*
 * Copyright (c) 2006-2011 Christian Plattner. All rights reserved.
 * Please refer to the LICENSE.txt for licensing details.
 */
package ch.ethz.ssh2.packets;
import java.io.IOException;

/**
 * PacketDisconnect.
 * 
 * @author Christian Plattner
 * @version 2.50, 03/15/10
 */
public class PacketDisconnect
{
	byte[] payload;

	int reason;
	String desc;
	String lang;

	public PacketDisconnect(byte payload[], int off, int len) throws IOException
	{
		this.payload = new byte[len];
		System.arraycopy(payload, off, this.payload, 0, len);

		TypesReader tr = new TypesReader(payload, off, len);

		int packet_type = tr.readByte();

		if (packet_type != Packets.SSH_MSG_DISCONNECT)
			throw new IOException("This is not a Disconnect Packet! ("
					+ packet_type + ")");

		reason = tr.readUINT32();
		desc = tr.readString();
		lang = tr.readString();
	}

	public PacketDisconnect(int reason, String desc, String lang)
	{
		this.reason = reason;
		this.desc = desc;
		this.lang = lang;
	}
	
	public byte[] getPayload()
	{
		if (payload == null)
		{
			TypesWriter tw = new TypesWriter();
			tw.writeByte(Packets.SSH_MSG_DISCONNECT);
			tw.writeUINT32(reason);
			tw.writeString(desc);
			tw.writeString(lang);
			payload = tw.getBytes();
		}
		return payload;
	}
}
