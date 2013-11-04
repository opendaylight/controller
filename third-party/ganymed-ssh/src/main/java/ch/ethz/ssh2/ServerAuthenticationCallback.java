/*
 * Copyright (c) 2012-2013 Christian Plattner. All rights reserved.
 * Please refer to the LICENSE.txt for licensing details.
 */

package ch.ethz.ssh2;

/**
 * A callback used during the authentication phase (see RFC 4252) when
 * implementing a SSH server. 
 * 
 * @author Christian Plattner
 * @version 2.50, 03/15/10
 */
public interface ServerAuthenticationCallback
{
	/**
	 * The method name for host-based authentication. 
	 */
	public final String METHOD_HOSTBASED = "hostbased";

	/**
	 * The method name for public-key authentication. 
	 */
	public final String METHOD_PUBLICKEY = "publickey";

	/**
	 * The method name for password authentication. 
	 */
	public final String METHOD_PASSWORD = "password";

	/**
	 * Called when the client enters authentication.
	 * This gives you the chance to set a custom authentication banner
	 * for this SSH-2 session. This is the first method called in this interface.
	 * It will only called at most once per <code>ServerConnection</code>.
	 * 
	 * @param sc The corresponding <code>ServerConnection</code>
	 * @return The authentication banner or <code>NULL</code> in case no banner should be send.
	 */
	public String initAuthentication(ServerConnection sc);

	/**
	 * Return the authentication methods that are currently available to the client.
	 * Be prepared to return this information at any time during the authentication procedure.
	 * <p/>
	 * The returned name-list of 'method names' (see RFC4252) indicate the authentication methods
	 * that may productively continue the authentication dialog.
	 * </p>
	 * It is RECOMMENDED that servers only include those 'method name'
	 * values in the name-list that are actually useful.  However, it is not
	 * illegal to include 'method name' values that cannot be used to
	 * authenticate the user.
	 * <p/>
	 * Already successfully completed authentications SHOULD NOT be included
	 * in the name-list, unless they should be performed again for some reason.
	 * 
	 * @see #METHOD_HOSTBASED
	 * @see #METHOD_PASSWORD
	 * @see #METHOD_PUBLICKEY
	 * 
	 * @param sc
	 * @return A list of method names.
	 */
	public String[] getRemainingAuthMethods(ServerConnection sc);

	/**
	 * Typically, this will be called be the client to get the list of
	 * authentication methods that can continue. You should simply return
	 * {@link AuthenticationResult#FAILURE}.
	 * 
	 * @param sc
	 * @param username Name of the user that wants to log in with the "none" method.
	 * @return
	 */
	public AuthenticationResult authenticateWithNone(ServerConnection sc, String username);

	public AuthenticationResult authenticateWithPassword(ServerConnection sc, String username, String password);

	/**
	 * NOTE: Not implemented yet.
	 * 
	 * @param sc
	 * @param username
	 * @param algorithm
	 * @param publickey
	 * @param signature
	 * @return
	 */
	public AuthenticationResult authenticateWithPublicKey(ServerConnection sc, String username, String algorithm,
			byte[] publickey, byte[] signature);
}
