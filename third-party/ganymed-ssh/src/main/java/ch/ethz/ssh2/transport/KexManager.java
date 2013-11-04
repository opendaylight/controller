/*
 * Copyright (c) 2006-2011 Christian Plattner. All rights reserved.
 * Please refer to the LICENSE.txt for licensing details.
 */
package ch.ethz.ssh2.transport;

import java.io.IOException;
import java.security.SecureRandom;

import ch.ethz.ssh2.ConnectionInfo;
import ch.ethz.ssh2.DHGexParameters;
import ch.ethz.ssh2.crypto.CryptoWishList;
import ch.ethz.ssh2.crypto.KeyMaterial;
import ch.ethz.ssh2.crypto.cipher.BlockCipher;
import ch.ethz.ssh2.crypto.cipher.BlockCipherFactory;
import ch.ethz.ssh2.crypto.digest.MAC;
import ch.ethz.ssh2.log.Logger;
import ch.ethz.ssh2.packets.PacketKexInit;
import ch.ethz.ssh2.packets.PacketNewKeys;
import ch.ethz.ssh2.signature.DSAPrivateKey;
import ch.ethz.ssh2.signature.RSAPrivateKey;

/**
 * KexManager.
 * 
 * @author Christian Plattner
 * @version $Id: KexManager.java 47 2013-07-31 23:59:52Z cleondris@gmail.com $
 */
public abstract class KexManager implements MessageHandler
{
	protected static final Logger log = Logger.getLogger(KexManager.class);

	KexState kxs;
	int kexCount = 0;
	KeyMaterial km;
	byte[] sessionId;
	ClientServerHello csh;

	final Object accessLock = new Object();
	ConnectionInfo lastConnInfo = null;

	boolean connectionClosed = false;

	boolean ignore_next_kex_packet = false;

	final TransportManager tm;

	CryptoWishList nextKEXcryptoWishList;
	DHGexParameters nextKEXdhgexParameters;
	DSAPrivateKey nextKEXdsakey;
	RSAPrivateKey nextKEXrsakey;

	final SecureRandom rnd;

	public KexManager(TransportManager tm, ClientServerHello csh, CryptoWishList initialCwl, SecureRandom rnd)
	{
		this.tm = tm;
		this.csh = csh;
		this.nextKEXcryptoWishList = initialCwl;
		this.nextKEXdhgexParameters = new DHGexParameters();
		this.rnd = rnd;
	}

	public ConnectionInfo getOrWaitForConnectionInfo(int minKexCount) throws IOException
	{
		boolean wasInterrupted = false;

		try
		{
			synchronized (accessLock)
			{
				while (true)
				{
					if ((lastConnInfo != null) && (lastConnInfo.keyExchangeCounter >= minKexCount))
						return lastConnInfo;

					if (connectionClosed)
						throw (IOException) new IOException("Key exchange was not finished, connection is closed.")
								.initCause(tm.getReasonClosedCause());

					try
					{
						accessLock.wait();
					}
					catch (InterruptedException e)
					{
						wasInterrupted = true;
					}
				}
			}
		}
		finally
		{
			if (wasInterrupted)
				Thread.currentThread().interrupt();
		}
	}

	private String getFirstMatch(String[] client, String[] server) throws NegotiateException
	{
		if (client == null || server == null)
			throw new IllegalArgumentException();

		if (client.length == 0)
			return null;

		for (int i = 0; i < client.length; i++)
		{
			for (int j = 0; j < server.length; j++)
			{
				if (client[i].equals(server[j]))
					return client[i];
			}
		}
		throw new NegotiateException();
	}

	private boolean compareFirstOfNameList(String[] a, String[] b)
	{
		if (a == null || b == null)
			throw new IllegalArgumentException();

		if ((a.length == 0) && (b.length == 0))
			return true;

		if ((a.length == 0) || (b.length == 0))
			return false;

		return (a[0].equals(b[0]));
	}

	private boolean isGuessOK(KexParameters cpar, KexParameters spar)
	{
		if (cpar == null || spar == null)
			throw new IllegalArgumentException();

		if (compareFirstOfNameList(cpar.kex_algorithms, spar.kex_algorithms) == false)
		{
			return false;
		}

		if (compareFirstOfNameList(cpar.server_host_key_algorithms, spar.server_host_key_algorithms) == false)
		{
			return false;
		}

		/*
		 * We do NOT check here if the other algorithms can be agreed on, this
		 * is just a check if kex_algorithms and server_host_key_algorithms were
		 * guessed right!
		 */

		return true;
	}

	protected NegotiatedParameters mergeKexParameters(KexParameters client, KexParameters server)
	{
		NegotiatedParameters np = new NegotiatedParameters();

		try
		{
			np.kex_algo = getFirstMatch(client.kex_algorithms, server.kex_algorithms);

			log.info("kex_algo=" + np.kex_algo);

			np.server_host_key_algo = getFirstMatch(client.server_host_key_algorithms,
					server.server_host_key_algorithms);

			log.info("server_host_key_algo=" + np.server_host_key_algo);

			np.enc_algo_client_to_server = getFirstMatch(client.encryption_algorithms_client_to_server,
					server.encryption_algorithms_client_to_server);
			np.enc_algo_server_to_client = getFirstMatch(client.encryption_algorithms_server_to_client,
					server.encryption_algorithms_server_to_client);

			log.info("enc_algo_client_to_server=" + np.enc_algo_client_to_server);
			log.info("enc_algo_server_to_client=" + np.enc_algo_server_to_client);

			np.mac_algo_client_to_server = getFirstMatch(client.mac_algorithms_client_to_server,
					server.mac_algorithms_client_to_server);
			np.mac_algo_server_to_client = getFirstMatch(client.mac_algorithms_server_to_client,
					server.mac_algorithms_server_to_client);

			log.info("mac_algo_client_to_server=" + np.mac_algo_client_to_server);
			log.info("mac_algo_server_to_client=" + np.mac_algo_server_to_client);

			np.comp_algo_client_to_server = getFirstMatch(client.compression_algorithms_client_to_server,
					server.compression_algorithms_client_to_server);
			np.comp_algo_server_to_client = getFirstMatch(client.compression_algorithms_server_to_client,
					server.compression_algorithms_server_to_client);

			log.info("comp_algo_client_to_server=" + np.comp_algo_client_to_server);
			log.info("comp_algo_server_to_client=" + np.comp_algo_server_to_client);

		}
		catch (NegotiateException e)
		{
			return null;
		}

		try
		{
			np.lang_client_to_server = getFirstMatch(client.languages_client_to_server,
					server.languages_client_to_server);
		}
		catch (NegotiateException e1)
		{
			np.lang_client_to_server = null;
		}

		try
		{
			np.lang_server_to_client = getFirstMatch(client.languages_server_to_client,
					server.languages_server_to_client);
		}
		catch (NegotiateException e2)
		{
			np.lang_server_to_client = null;
		}

		if (isGuessOK(client, server))
			np.guessOK = true;

		return np;
	}

	public synchronized void initiateKEX(CryptoWishList cwl, DHGexParameters dhgex, DSAPrivateKey dsa, RSAPrivateKey rsa)
			throws IOException
	{
		nextKEXcryptoWishList = cwl;
		nextKEXdhgexParameters = dhgex;
		nextKEXdsakey = dsa;
		nextKEXrsakey = rsa;
		
		if (kxs == null)
		{
			kxs = new KexState();
			kxs.local_dsa_key = dsa;
			kxs.local_rsa_key = rsa;
			kxs.dhgexParameters = nextKEXdhgexParameters;
			kxs.localKEX = new PacketKexInit(nextKEXcryptoWishList, rnd);
			tm.sendKexMessage(kxs.localKEX.getPayload());
		}
	}

	private boolean establishKeyMaterial()
	{
		try
		{
			int mac_cs_key_len = MAC.getKeyLen(kxs.np.mac_algo_client_to_server);
			int enc_cs_key_len = BlockCipherFactory.getKeySize(kxs.np.enc_algo_client_to_server);
			int enc_cs_block_len = BlockCipherFactory.getBlockSize(kxs.np.enc_algo_client_to_server);

			int mac_sc_key_len = MAC.getKeyLen(kxs.np.mac_algo_server_to_client);
			int enc_sc_key_len = BlockCipherFactory.getKeySize(kxs.np.enc_algo_server_to_client);
			int enc_sc_block_len = BlockCipherFactory.getBlockSize(kxs.np.enc_algo_server_to_client);

			km = KeyMaterial.create("SHA1", kxs.H, kxs.K, sessionId, enc_cs_key_len, enc_cs_block_len, mac_cs_key_len,
					enc_sc_key_len, enc_sc_block_len, mac_sc_key_len);
		}
		catch (IllegalArgumentException e)
		{
			return false;
		}
		return true;
	}

	protected void finishKex(boolean clientMode) throws IOException
	{
		if (sessionId == null)
			sessionId = kxs.H;

		establishKeyMaterial();

		/* Tell the other side that we start using the new material */

		PacketNewKeys ign = new PacketNewKeys();
		tm.sendKexMessage(ign.getPayload());

		BlockCipher cbc;
		MAC mac;

		try
		{
			cbc = BlockCipherFactory.createCipher(clientMode ? kxs.np.enc_algo_client_to_server
					: kxs.np.enc_algo_server_to_client, true, clientMode ? km.enc_key_client_to_server
					: km.enc_key_server_to_client, clientMode ? km.initial_iv_client_to_server
					: km.initial_iv_server_to_client);

			mac = new MAC(clientMode ? kxs.np.mac_algo_client_to_server : kxs.np.mac_algo_server_to_client, clientMode
					? km.integrity_key_client_to_server : km.integrity_key_server_to_client);

		}
		catch (IllegalArgumentException e1)
		{
			throw new IOException("Fatal error during MAC startup!");
		}

		tm.changeSendCipher(cbc, mac);
		tm.kexFinished();
	}

	public static final String[] getDefaultServerHostkeyAlgorithmList()
	{
		return new String[] { "ssh-rsa", "ssh-dss" };
	}

	public static final void checkServerHostkeyAlgorithmsList(String[] algos)
	{
		for (int i = 0; i < algos.length; i++)
		{
			if (("ssh-rsa".equals(algos[i]) == false) && ("ssh-dss".equals(algos[i]) == false))
				throw new IllegalArgumentException("Unknown server host key algorithm '" + algos[i] + "'");
		}
	}

	public static final String[] getDefaultClientKexAlgorithmList()
	{
		return new String[] { "diffie-hellman-group-exchange-sha1", "diffie-hellman-group14-sha1",
				"diffie-hellman-group1-sha1" };
	}

	public static final String[] getDefaultServerKexAlgorithmList()
	{
		return new String[] { "diffie-hellman-group14-sha1", "diffie-hellman-group1-sha1" };
	}

	public static final void checkKexAlgorithmList(String[] algos)
	{
		for (int i = 0; i < algos.length; i++)
		{
			if ("diffie-hellman-group-exchange-sha1".equals(algos[i]))
				continue;

			if ("diffie-hellman-group14-sha1".equals(algos[i]))
				continue;

			if ("diffie-hellman-group1-sha1".equals(algos[i]))
				continue;

			throw new IllegalArgumentException("Unknown kex algorithm '" + algos[i] + "'");
		}
	}

}
