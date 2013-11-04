/*
 * Copyright (c) 2006-2011 Christian Plattner. All rights reserved.
 * Please refer to the LICENSE.txt for licensing details.
 */
package ch.ethz.ssh2.packets;

import java.io.IOException;
import java.math.BigInteger;

/**
 * PacketKexDHInit.
 * 
 * @author Christian Plattner
 * @version 2.50, 03/15/10
 */
public class PacketKexDHInit
{
	byte[] payload;

	BigInteger e;

	public PacketKexDHInit(BigInteger e)
	{
		this.e = e;
	}

	public PacketKexDHInit(byte payload[], int off, int len) throws IOException
	{
		this.payload = new byte[len];
		System.arraycopy(payload, off, this.payload, 0, len);

		TypesReader tr = new TypesReader(payload, off, len);

		int packet_type = tr.readByte();

		if (packet_type != Packets.SSH_MSG_KEXDH_INIT)
			throw new IOException("This is not a SSH_MSG_KEXDH_INIT! ("
					+ packet_type + ")");

		e = tr.readMPINT();

		if (tr.remain() != 0) throw new IOException("PADDING IN SSH_MSG_KEXDH_INIT!");
	}
	
	public BigInteger getE()
	{
		return e;
	}
	
	public byte[] getPayload()
	{
		if (payload == null)
		{
			TypesWriter tw = new TypesWriter();
			tw.writeByte(Packets.SSH_MSG_KEXDH_INIT);
			tw.writeMPInt(e);
			payload = tw.getBytes();
		}
		return payload;
	}
}
