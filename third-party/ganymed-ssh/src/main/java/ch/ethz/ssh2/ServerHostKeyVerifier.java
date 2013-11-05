/*
 * Copyright (c) 2006-2011 Christian Plattner. All rights reserved.
 * Please refer to the LICENSE.txt for licensing details.
 */
package ch.ethz.ssh2;

/**
 * A callback interface used to implement a client specific method of checking
 * server host keys.
 * 
 * @author Christian Plattner
 * @version 2.50, 03/15/10
 */

public interface ServerHostKeyVerifier
{
	/**
	 * The actual verifier method, it will be called by the key exchange code
	 * on EVERY key exchange - this can happen several times during the lifetime
	 * of a connection.
	 * <p>
	 * Note: SSH-2 servers are allowed to change their hostkey at ANY time.
	 * 
	 * @param hostname the hostname used to create the {@link Connection} object
	 * @param port the remote TCP port
	 * @param serverHostKeyAlgorithm the public key algorithm (<code>ssh-rsa</code> or <code>ssh-dss</code>)
	 * @param serverHostKey the server's public key blob
	 * @return if the client wants to accept the server's host key - if not, the
	 *         connection will be closed.
	 * @throws Exception Will be wrapped with an IOException, extended version of returning false =)
	 */
	public boolean verifyServerHostKey(String hostname, int port, String serverHostKeyAlgorithm, byte[] serverHostKey)
			throws Exception;
}
