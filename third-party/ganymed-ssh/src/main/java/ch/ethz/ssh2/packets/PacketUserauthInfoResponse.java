/*
 * Copyright (c) 2006-2011 Christian Plattner. All rights reserved.
 * Please refer to the LICENSE.txt for licensing details.
 */
package ch.ethz.ssh2.packets;

/**
 * PacketUserauthInfoResponse.
 * 
 * @author Christian Plattner
 * @version 2.50, 03/15/10
 */
public class PacketUserauthInfoResponse
{
	byte[] payload;

	String[] responses;

	public PacketUserauthInfoResponse(String[] responses)
	{
		this.responses = responses;
	}

	public byte[] getPayload()
	{
		if (payload == null)
		{
			TypesWriter tw = new TypesWriter();
			tw.writeByte(Packets.SSH_MSG_USERAUTH_INFO_RESPONSE);
			tw.writeUINT32(responses.length);
			for (int i = 0; i < responses.length; i++)
				tw.writeString(responses[i]);

			payload = tw.getBytes();
		}
		return payload;
	}
}
