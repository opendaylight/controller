/*
 * Copyright (c) 2006-2011 Christian Plattner. All rights reserved.
 * Please refer to the LICENSE.txt for licensing details.
 */
package ch.ethz.ssh2.packets;

import java.io.IOException;

import java.math.BigInteger;

/**
 * PacketKexDHReply.
 * 
 * @author Christian Plattner
 * @version 2.50, 03/15/10
 */
public class PacketKexDHReply
{
	byte[] payload;

	byte[] hostKey;
	BigInteger f;
	byte[] signature;
	
	public PacketKexDHReply(byte[] hostKey, BigInteger f, byte[] signature)
	{
		this.hostKey = hostKey;
		this.f = f;
		this.signature = signature;
	}
	
	public PacketKexDHReply(byte payload[], int off, int len) throws IOException
	{
		this.payload = new byte[len];
		System.arraycopy(payload, off, this.payload, 0, len);

		TypesReader tr = new TypesReader(payload, off, len);

		int packet_type = tr.readByte();

		if (packet_type != Packets.SSH_MSG_KEXDH_REPLY)
			throw new IOException("This is not a SSH_MSG_KEXDH_REPLY! ("
					+ packet_type + ")");

		hostKey = tr.readByteString();
		f = tr.readMPINT();
		signature = tr.readByteString();

		if (tr.remain() != 0) throw new IOException("PADDING IN SSH_MSG_KEXDH_REPLY!");
	}

	public byte[] getPayload()
	{
		if (payload == null)
		{
			TypesWriter tw = new TypesWriter();
			tw.writeByte(Packets.SSH_MSG_KEXDH_REPLY);
			tw.writeString(hostKey, 0, hostKey.length);
			tw.writeMPInt(f);
			tw.writeString(signature, 0, signature.length);
			payload = tw.getBytes();
		}
		return payload;
	}
	
	public BigInteger getF()
	{
		return f;
	}
	
	public byte[] getHostKey()
	{
		return hostKey;
	}

	public byte[] getSignature()
	{
		return signature;
	}
}
