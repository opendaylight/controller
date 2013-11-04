/*
 * Copyright (c) 2006-2011 Christian Plattner. All rights reserved.
 * Please refer to the LICENSE.txt for licensing details.
 */
package ch.ethz.ssh2.packets;

/**
 * PacketUserauthRequestInteractive.
 * 
 * @author Christian Plattner
 * @version 2.50, 03/15/10
 */
public class PacketUserauthRequestInteractive
{
	byte[] payload;

	String userName;
	String serviceName;
	String[] submethods;

	public PacketUserauthRequestInteractive(String serviceName, String user, String[] submethods)
	{
		this.serviceName = serviceName;
		this.userName = user;
		this.submethods = submethods;
	}

	public byte[] getPayload()
	{
		if (payload == null)
		{
			TypesWriter tw = new TypesWriter();
			tw.writeByte(Packets.SSH_MSG_USERAUTH_REQUEST);
			tw.writeString(userName);
			tw.writeString(serviceName);
			tw.writeString("keyboard-interactive");
			tw.writeString(""); // draft-ietf-secsh-newmodes-04.txt says that
			// the language tag should be empty.
			tw.writeNameList(submethods);

			payload = tw.getBytes();
		}
		return payload;
	}
}
