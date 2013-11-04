/*
 * Copyright (c) 2012-2013 Christian Plattner. All rights reserved.
 * Please refer to the LICENSE.txt for licensing details.
 */
package ch.ethz.ssh2;

public interface ServerConnectionCallback
{
	public ServerSessionCallback acceptSession(ServerSession session);
}
