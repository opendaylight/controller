/*
 * Copyright (c) 2012-2013 Christian Plattner. All rights reserved.
 * Please refer to the LICENSE.txt for licensing details.
 */

package ch.ethz.ssh2;

import java.io.CharArrayWriter;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.Socket;

import ch.ethz.ssh2.crypto.CryptoWishList;
import ch.ethz.ssh2.crypto.PEMDecoder;
import ch.ethz.ssh2.server.ServerConnectionState;
import ch.ethz.ssh2.signature.DSAPrivateKey;
import ch.ethz.ssh2.signature.RSAPrivateKey;
import ch.ethz.ssh2.transport.TransportManager;

/**
 * A server-side SSH-2 connection.
 * 
 * @author Christian
 *
 */
public class ServerConnection
{
	/**
	 * The softwareversion presented to the SSH-2 client.
	 */
	private String softwareversion = String.format("Ganymed_SSHD_%s", Version.getSpecification());

	private final ServerConnectionState state = new ServerConnectionState(this);

	/**
	 * Creates a new <code>ServerConnection</code> that will communicate
	 * with the client over the given <code>Socket</code>.
	 * <p>
	 * Note: you need to call {@link #connect()} or {@link #connect(int)} to
	 * perform the initial handshake and establish the encrypted communication.
	 * 
	 * @see #connect(int)
	 * 
	 * @param s The socket
	 */
	public ServerConnection(Socket s)
	{
		this(s, null, null);
	}

    public ServerConnection(Socket s, String softwareversion) {
        this(s, null, null);
        this.softwareversion = softwareversion;
    }

    /**
	 * Creates a new <code>ServerConnection</code> that will communicate
	 * with the client over the given <code>Socket</code>.
	 * <p>
	 * Note: you need to call {@link #connect()} or {@link #connect(int)} to
	 * perform the initial handshake and establish the encrypted communication.
	 * <p>
	 * Please read the javadoc for the {@link #connect(int)} method.
	 * 
	 * @see #connect(int)
	 *  
	 * @param s The socket
	 * @param dsa_key The DSA hostkey, may be <code>NULL</code>
	 * @param rsa_key The RSA hostkey, may be <code>NULL</code>
	 */
	public ServerConnection(Socket s, DSAPrivateKey dsa_key, RSAPrivateKey rsa_key)
	{
		state.s = s;
		state.softwareversion = softwareversion;
		state.next_dsa_key = dsa_key;
		state.next_rsa_key = rsa_key;
		fixCryptoWishList(state.next_cryptoWishList, state.next_dsa_key, state.next_rsa_key);
	}

	/**
	 * Establish the connection and block until the first handshake has completed.
	 * <p>
	 * Note: this is a wrapper that calls <code>connect(0)</code> (i.e., connect with no timeout).
	 * <p>
	 * Please read the javadoc for the {@link #connect(int)} method.
	 * 
	 * @see #connect(int)
	 * 
	 * @throws IOException
	 */
	public synchronized void connect() throws IOException
	{
		connect(0);
	}

	/**
	 * Establish the connection and block until the first handshake has completed.
	 * <p>
	 * Note 1: either a DSA or a RSA (or both) hostkey must be set before calling this method.
	 * <p>
	 * Note 2: You must set the callbacks for authentication ({@link #setAuthenticationCallback(ServerAuthenticationCallback)})
	 * and connection events ({@link #setServerConnectionCallback(ServerConnectionCallback)}).
	 * 
	 * @see #setPEMHostKey(char[], String)
	 * @see #setPEMHostKey(File, String)
	 * @see #setRsaHostKey(RSAPrivateKey)
	 * @see #setDsaHostKey(DSAPrivateKey)
	 * 
	 * @param timeout_milliseconds Timeout in milliseconds, <code>0</code> means no timeout.
	 * @throws IOException
	 */
	public synchronized void connect(int timeout_milliseconds) throws IOException
	{
		synchronized (state)
		{
			if (state.cb_conn == null)
				throw new IllegalStateException("The callback for connection events has not been set.");

			if (state.cb_auth == null)
				throw new IllegalStateException("The callback for authentication events has not been set.");

			if (state.tm != null)
				throw new IllegalStateException("The initial handshake has already been started.");

			if ((state.next_dsa_key == null) && (state.next_rsa_key == null))
				throw new IllegalStateException("Neither a RSA nor a DSA host key has been specified!");

			state.tm = new TransportManager();
		}

		//tm.setSoTimeout(connectTimeout);
		//tm.setConnectionMonitors(connectionMonitors);

		state.tm.setTcpNoDelay(true);
		state.tm.serverInit(state);

		/* Wait until first KEX has finished */

		state.tm.getConnectionInfo(1);
	}

	/**
	 * Retrieve the underlying socket.
	 * 
	 * @return the socket that has been passed to the constructor.
	 */
	public Socket getSocket()
	{
		return state.s;
	}

	/**
	 * Force an asynchronous key re-exchange (the call does not block). The
	 * latest values set for MAC, Cipher and DH group exchange parameters will
	 * be used. If a key exchange is currently in progress, then this method has
	 * the only effect that the so far specified parameters will be used for the
	 * next (client driven) key exchange. You may call this method only after
	 * the initial key exchange has been established.
	 * <p>
	 * Note: This implementation will never start automatically a key exchange (other than the initial one)
	 * unless you or the connected SSH-2 client ask for it.
	 * 
	 * @throws IOException
	 *             In case of any failure behind the scenes.
	 */
	public synchronized void forceKeyExchange() throws IOException
	{
		synchronized (state)
		{
			if (state.tm == null)
				throw new IllegalStateException(
						"Cannot force another key exchange, you need to start the key exchange first.");

			state.tm.forceKeyExchange(state.next_cryptoWishList, null, state.next_dsa_key, state.next_rsa_key);
		}
	}

	/**
	 * Returns a {@link ConnectionInfo} object containing the details of
	 * the connection. May be called as soon as the first key exchange has been
	 * started. The method blocks in case the first key exchange has not been completed. 
	 * <p>
	 * Note: upon return of this method, authentication may still be pending.
	 * 
	 * @return A {@link ConnectionInfo} object.
	 * @throws IOException
	 *             In case of any failure behind the scenes; e.g., first key exchange was aborted.
	 */
	public synchronized ConnectionInfo getConnectionInfo() throws IOException
	{
		synchronized (state)
		{
			if (state.tm == null)
				throw new IllegalStateException(
						"Cannot get details of connection, you need to start the key exchange first.");
		}

		return state.tm.getConnectionInfo(1);
	}

	/**
	 * Change the current DSA hostkey. Either a DSA or RSA private key must be set for a successful handshake with
	 * the client. 
	 * <p>
	 * Note: You can change an existing DSA hostkey after the initial kex exchange (the new value will
	 * be used during the next server initiated key exchange), but you cannot remove (i.e., set to <code>null</code>) the
	 * current DSA key, otherwise the next key exchange may fail in case the client supports only DSA hostkeys.
	 * 
	 * @param dsa_hostkey
	 */
	public synchronized void setDsaHostKey(DSAPrivateKey dsa_hostkey)
	{
		synchronized (state)
		{
			if ((dsa_hostkey == null) && (state.next_dsa_key != null) && (state.tm != null))
				throw new IllegalStateException("Cannot remove DSA hostkey after first key exchange.");

			state.next_dsa_key = dsa_hostkey;
			fixCryptoWishList(state.next_cryptoWishList, state.next_dsa_key, state.next_rsa_key);
		}
	}

	/**
	 * Change the current RSA hostkey. Either a DSA or RSA private key must be set for a successful handshake with
	 * the client. 
	 * <p>
	 * Note: You can change an existing RSA hostkey after the initial kex exchange (the new value will
	 * be used during the next server initiated key exchange), but you cannot remove (i.e., set to <code>null</code>) the
	 * current RSA key, otherwise the next key exchange may fail in case the client supports only RSA hostkeys.
	 * 
	 * @param rsa_hostkey
	 */
	public synchronized void setRsaHostKey(RSAPrivateKey rsa_hostkey)
	{
		synchronized (state)
		{
			if ((rsa_hostkey == null) && (state.next_rsa_key != null) && (state.tm != null))
				throw new IllegalStateException("Cannot remove RSA hostkey after first key exchange.");

			state.next_rsa_key = rsa_hostkey;
			fixCryptoWishList(state.next_cryptoWishList, state.next_dsa_key, state.next_rsa_key);
		}
	}

	/**
	 * Utility method that loads a PEM based hostkey (either RSA or DSA based) and
	 * calls either <code>setRsaHostKey()</code> or <code>setDsaHostKey()</code>.
	 * 
	 * @param pemfile The PEM data
	 * @param password Password, may be null in case the PEM data is not password protected
	 * @throws IOException In case of any error.
	 */
	public void setPEMHostKey(char[] pemdata, String password) throws IOException
	{
		Object key = PEMDecoder.decode(pemdata, password);

		if (key instanceof DSAPrivateKey)
			setDsaHostKey((DSAPrivateKey) key);

		if (key instanceof RSAPrivateKey)
			setRsaHostKey((RSAPrivateKey) key);
	}

	/**
	 * Utility method that loads a hostkey from a PEM file (either RSA or DSA based) and
	 * calls either <code>setRsaHostKey()</code> or <code>setDsaHostKey()</code>.
	 * 
	 * @param pemFile The PEM file
	 * @param password Password, may be null in case the PEM file is not password protected
	 * @throws IOException
	 */
	public void setPEMHostKey(File pemFile, String password) throws IOException
	{
		if (pemFile == null)
			throw new IllegalArgumentException("pemfile argument is null");

		char[] buff = new char[256];

		CharArrayWriter cw = new CharArrayWriter();

		FileReader fr = new FileReader(pemFile);

		while (true)
		{
			int len = fr.read(buff);
			if (len < 0)
				break;
			cw.write(buff, 0, len);
		}

		fr.close();

		setPEMHostKey(cw.toCharArray(), password);
	}

	private void fixCryptoWishList(CryptoWishList next_cryptoWishList, DSAPrivateKey next_dsa_key,
			RSAPrivateKey next_rsa_key)
	{
		if ((next_dsa_key != null) && (next_rsa_key != null))
			next_cryptoWishList.serverHostKeyAlgorithms = new String[] { "ssh-rsa", "ssh-dss" };
		else if (next_dsa_key != null)
			next_cryptoWishList.serverHostKeyAlgorithms = new String[] { "ssh-dss" };
		else if (next_rsa_key != null)
			next_cryptoWishList.serverHostKeyAlgorithms = new String[] { "ssh-rsa" };
		else
			next_cryptoWishList.serverHostKeyAlgorithms = new String[0];
	}

	/**
	 * Callback interface with methods that will be called upon events
	 * generated by the client (e.g., client opens a new Session which results in a <code>ServerSession</code>).
	 * <p>
	 * Note: This must be set before the first handshake.
	 * 
	 * @param cb The callback implementation
	 */
	public synchronized void setServerConnectionCallback(ServerConnectionCallback cb)
	{
		synchronized (state)
		{
			state.cb_conn = cb;
		}
	}

	/**
	 * Callback interface with methods that will be called upon authentication events.
	 * <p>
	 * Note: This must be set before the first handshake.
	 * 
	 * @param cb The callback implementation
	 */
	public synchronized void setAuthenticationCallback(ServerAuthenticationCallback cb)
	{
		synchronized (state)
		{
			state.cb_auth = cb;
		}
	}

	/**
	 * Close the connection to the SSH-2 server. All assigned sessions will be
	 * closed, too. Can be called at any time. Don't forget to call this once
	 * you don't need a connection anymore - otherwise the receiver thread may
	 * run forever.
	 */
	public void close()
	{
		Throwable t = new Throwable("Closed due to user request.");
		close(t, false);
	}

	public void close(Throwable t, boolean hard)
	{
		synchronized (state)
		{
			if (state.cm != null)
				state.cm.closeAllChannels();

			if (state.tm != null)
			{
				state.tm.close(t, hard == false);
			}
		}
	}
}
