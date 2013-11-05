/*
 * Copyright (c) 2006-2011 Christian Plattner. All rights reserved.
 * Please refer to the LICENSE.txt for licensing details.
 */
package ch.ethz.ssh2.packets;

import ch.ethz.ssh2.DHGexParameters;

/**
 * PacketKexDhGexRequest.
 * 
 * @author Christian Plattner
 * @version 2.50, 03/15/10
 */
public class PacketKexDhGexRequest
{
	byte[] payload;

	int min;
	int n;
	int max;

	public PacketKexDhGexRequest(DHGexParameters para)
	{
		this.min = para.getMin_group_len();
		this.n = para.getPref_group_len();
		this.max = para.getMax_group_len();
	}

	public byte[] getPayload()
	{
		if (payload == null)
		{
			TypesWriter tw = new TypesWriter();
			tw.writeByte(Packets.SSH_MSG_KEX_DH_GEX_REQUEST);
			tw.writeUINT32(min);
			tw.writeUINT32(n);
			tw.writeUINT32(max);
			payload = tw.getBytes();
		}
		return payload;
	}
}
