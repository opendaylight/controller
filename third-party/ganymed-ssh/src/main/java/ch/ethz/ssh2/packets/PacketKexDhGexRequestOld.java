/*
 * Copyright (c) 2006-2011 Christian Plattner. All rights reserved.
 * Please refer to the LICENSE.txt for licensing details.
 */
package ch.ethz.ssh2.packets;

import ch.ethz.ssh2.DHGexParameters;

/**
 * PacketKexDhGexRequestOld.
 * 
 * @author Christian Plattner
 * @version 2.50, 03/15/10
 */
public class PacketKexDhGexRequestOld
{
	byte[] payload;

	int n;

	public PacketKexDhGexRequestOld(DHGexParameters para)
	{
		this.n = para.getPref_group_len();
	}

	public byte[] getPayload()
	{
		if (payload == null)
		{
			TypesWriter tw = new TypesWriter();
			tw.writeByte(Packets.SSH_MSG_KEX_DH_GEX_REQUEST_OLD);
			tw.writeUINT32(n);
			payload = tw.getBytes();
		}
		return payload;
	}
}
