
package ch.ethz.ssh2;

public enum AuthenticationResult {

	/**
	 * 
	 */
	SUCCESS,
	/**
	 * The authentication request to which this is a response was successful, however, more
	 * authentication requests are needed (multi-method authentication sequence).
	 * 
	 * @see ServerAuthenticationCallback#getRemainingAuthMethods(ServerConnection)
	 */
	PARTIAL_SUCCESS,
	/**
	 * The server rejected the authentication request.
	 */
	FAILURE
}
