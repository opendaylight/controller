/*
 * Copyright (c) 2006-2011 Christian Plattner. All rights reserved.
 * Please refer to the LICENSE.txt for licensing details.
 */
package ch.ethz.ssh2.channel;

/**
 * RemoteForwardingData. Data about a requested remote forwarding.
 * 
 * @author Christian Plattner
 * @version 2.50, 03/15/10
 */
public class RemoteForwardingData
{
	public String bindAddress;
	public int bindPort;

	String targetAddress;
	int targetPort;
}
