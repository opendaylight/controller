/*
 * Copyright (c) 2006-2013 Christian Plattner. All rights reserved.
 * Please refer to the LICENSE.txt for licensing details.
 */
package ch.ethz.ssh2.transport;

import java.io.IOException;
import java.security.SecureRandom;

import ch.ethz.ssh2.ConnectionInfo;
import ch.ethz.ssh2.ServerHostKeyVerifier;
import ch.ethz.ssh2.crypto.CryptoWishList;
import ch.ethz.ssh2.crypto.cipher.BlockCipher;
import ch.ethz.ssh2.crypto.cipher.BlockCipherFactory;
import ch.ethz.ssh2.crypto.dh.DhExchange;
import ch.ethz.ssh2.crypto.dh.DhGroupExchange;
import ch.ethz.ssh2.crypto.digest.MAC;
import ch.ethz.ssh2.packets.PacketKexDHInit;
import ch.ethz.ssh2.packets.PacketKexDHReply;
import ch.ethz.ssh2.packets.PacketKexDhGexGroup;
import ch.ethz.ssh2.packets.PacketKexDhGexInit;
import ch.ethz.ssh2.packets.PacketKexDhGexReply;
import ch.ethz.ssh2.packets.PacketKexDhGexRequest;
import ch.ethz.ssh2.packets.PacketKexDhGexRequestOld;
import ch.ethz.ssh2.packets.PacketKexInit;
import ch.ethz.ssh2.packets.Packets;
import ch.ethz.ssh2.signature.DSAPublicKey;
import ch.ethz.ssh2.signature.DSASHA1Verify;
import ch.ethz.ssh2.signature.DSASignature;
import ch.ethz.ssh2.signature.RSAPublicKey;
import ch.ethz.ssh2.signature.RSASHA1Verify;
import ch.ethz.ssh2.signature.RSASignature;

public class ClientKexManager extends KexManager
{
	ServerHostKeyVerifier verifier;
	final String hostname;
	final int port;
	
	public ClientKexManager(TransportManager tm, ClientServerHello csh, CryptoWishList initialCwl, String hostname, int port,
			ServerHostKeyVerifier keyVerifier, SecureRandom rnd)
	{
		super(tm, csh, initialCwl, rnd);
		
		this.hostname = hostname;
		this.port = port;
		this.verifier = keyVerifier;
	}
	
	protected boolean verifySignature(byte[] sig, byte[] hostkey) throws IOException
	{
		if (kxs.np.server_host_key_algo.equals("ssh-rsa"))
		{
			RSASignature rs = RSASHA1Verify.decodeSSHRSASignature(sig);
			RSAPublicKey rpk = RSASHA1Verify.decodeSSHRSAPublicKey(hostkey);

			log.debug("Verifying ssh-rsa signature");

			return RSASHA1Verify.verifySignature(kxs.H, rs, rpk);
		}

		if (kxs.np.server_host_key_algo.equals("ssh-dss"))
		{
			DSASignature ds = DSASHA1Verify.decodeSSHDSASignature(sig);
			DSAPublicKey dpk = DSASHA1Verify.decodeSSHDSAPublicKey(hostkey);

			log.debug("Verifying ssh-dss signature");

			return DSASHA1Verify.verifySignature(kxs.H, ds, dpk);
		}

		throw new IOException("Unknown server host key algorithm '" + kxs.np.server_host_key_algo + "'");
	}

	
	public synchronized void handleMessage(byte[] msg, int msglen) throws IOException
	{
		PacketKexInit kip;

		if (msg == null)
		{
			synchronized (accessLock)
			{
				connectionClosed = true;
				accessLock.notifyAll();
				return;
			}
		}

		if ((kxs == null) && (msg[0] != Packets.SSH_MSG_KEXINIT))
			throw new IOException("Unexpected KEX message (type " + msg[0] + ")");

		if (ignore_next_kex_packet)
		{
			ignore_next_kex_packet = false;
			return;
		}

		if (msg[0] == Packets.SSH_MSG_KEXINIT)
		{
			if ((kxs != null) && (kxs.state != 0))
				throw new IOException("Unexpected SSH_MSG_KEXINIT message during on-going kex exchange!");

			if (kxs == null)
			{
				/*
				 * Ah, OK, peer wants to do KEX. Let's be nice and play
				 * together.
				 */
				kxs = new KexState();
				kxs.dhgexParameters = nextKEXdhgexParameters;
				kip = new PacketKexInit(nextKEXcryptoWishList, rnd);
				kxs.localKEX = kip;
				tm.sendKexMessage(kip.getPayload());
			}

			kip = new PacketKexInit(msg, 0, msglen);
			kxs.remoteKEX = kip;

			kxs.np = mergeKexParameters(kxs.localKEX.getKexParameters(), kxs.remoteKEX.getKexParameters());

			if (kxs.np == null)
				throw new IOException("Cannot negotiate, proposals do not match.");

			if (kxs.remoteKEX.isFirst_kex_packet_follows() && (kxs.np.guessOK == false))
			{
				/*
				 * Guess was wrong, we need to ignore the next kex packet.
				 */

				ignore_next_kex_packet = true;
			}

			if (kxs.np.kex_algo.equals("diffie-hellman-group-exchange-sha1"))
			{
				if (kxs.dhgexParameters.getMin_group_len() == 0)
				{
					PacketKexDhGexRequestOld dhgexreq = new PacketKexDhGexRequestOld(kxs.dhgexParameters);
					tm.sendKexMessage(dhgexreq.getPayload());

				}
				else
				{
					PacketKexDhGexRequest dhgexreq = new PacketKexDhGexRequest(kxs.dhgexParameters);
					tm.sendKexMessage(dhgexreq.getPayload());
				}
				kxs.state = 1;
				return;
			}

			if (kxs.np.kex_algo.equals("diffie-hellman-group1-sha1")
					|| kxs.np.kex_algo.equals("diffie-hellman-group14-sha1"))
			{
				kxs.dhx = new DhExchange();

				if (kxs.np.kex_algo.equals("diffie-hellman-group1-sha1"))
					kxs.dhx.clientInit(1, rnd);
				else
					kxs.dhx.clientInit(14, rnd);

				PacketKexDHInit kp = new PacketKexDHInit(kxs.dhx.getE());
				tm.sendKexMessage(kp.getPayload());
				kxs.state = 1;
				return;
			}

			throw new IllegalStateException("Unkown KEX method!");
		}

		if (msg[0] == Packets.SSH_MSG_NEWKEYS)
		{
			if (km == null)
				throw new IOException("Peer sent SSH_MSG_NEWKEYS, but I have no key material ready!");

			BlockCipher cbc;
			MAC mac;

			try
			{
				cbc = BlockCipherFactory.createCipher(kxs.np.enc_algo_server_to_client, false,
						km.enc_key_server_to_client, km.initial_iv_server_to_client);

				mac = new MAC(kxs.np.mac_algo_server_to_client, km.integrity_key_server_to_client);

			}
			catch (IllegalArgumentException e1)
			{
				throw new IOException("Fatal error during MAC startup!");
			}

			tm.changeRecvCipher(cbc, mac);

			ConnectionInfo sci = new ConnectionInfo();

			kexCount++;

			sci.keyExchangeAlgorithm = kxs.np.kex_algo;
			sci.keyExchangeCounter = kexCount;
			sci.clientToServerCryptoAlgorithm = kxs.np.enc_algo_client_to_server;
			sci.serverToClientCryptoAlgorithm = kxs.np.enc_algo_server_to_client;
			sci.clientToServerMACAlgorithm = kxs.np.mac_algo_client_to_server;
			sci.serverToClientMACAlgorithm = kxs.np.mac_algo_server_to_client;
			sci.serverHostKeyAlgorithm = kxs.np.server_host_key_algo;
			sci.serverHostKey = kxs.remote_hostkey;

			synchronized (accessLock)
			{
				lastConnInfo = sci;
				accessLock.notifyAll();
			}

			kxs = null;
			return;
		}

		if ((kxs == null) || (kxs.state == 0))
			throw new IOException("Unexpected Kex submessage!");

		if (kxs.np.kex_algo.equals("diffie-hellman-group-exchange-sha1"))
		{
			if (kxs.state == 1)
			{
				PacketKexDhGexGroup dhgexgrp = new PacketKexDhGexGroup(msg, 0, msglen);
				kxs.dhgx = new DhGroupExchange(dhgexgrp.getP(), dhgexgrp.getG());
				kxs.dhgx.init(rnd);
				PacketKexDhGexInit dhgexinit = new PacketKexDhGexInit(kxs.dhgx.getE());
				tm.sendKexMessage(dhgexinit.getPayload());
				kxs.state = 2;
				return;
			}

			if (kxs.state == 2)
			{
				PacketKexDhGexReply dhgexrpl = new PacketKexDhGexReply(msg, 0, msglen);

				kxs.remote_hostkey = dhgexrpl.getHostKey();

				if (verifier != null)
				{
					boolean vres = false;

					try
					{
						vres = verifier.verifyServerHostKey(hostname, port, kxs.np.server_host_key_algo, kxs.remote_hostkey);
					}
					catch (Exception e)
					{
						throw (IOException) new IOException(
								"The server hostkey was not accepted by the verifier callback.").initCause(e);
					}

					if (vres == false)
						throw new IOException("The server hostkey was not accepted by the verifier callback");
				}

				kxs.dhgx.setF(dhgexrpl.getF());

				try
				{
					kxs.H = kxs.dhgx.calculateH(csh.getClientString(), csh.getServerString(),
							kxs.localKEX.getPayload(), kxs.remoteKEX.getPayload(), dhgexrpl.getHostKey(),
							kxs.dhgexParameters);
				}
				catch (IllegalArgumentException e)
				{
					throw (IOException) new IOException("KEX error.").initCause(e);
				}

				boolean res = verifySignature(dhgexrpl.getSignature(), kxs.remote_hostkey);

				if (res == false)
					throw new IOException("Hostkey signature sent by remote is wrong!");

				kxs.K = kxs.dhgx.getK();

				finishKex(true);
				kxs.state = -1;
				return;
			}

			throw new IllegalStateException("Illegal State in KEX Exchange!");
		}

		if (kxs.np.kex_algo.equals("diffie-hellman-group1-sha1")
				|| kxs.np.kex_algo.equals("diffie-hellman-group14-sha1"))
		{
			if (kxs.state == 1)
			{

				PacketKexDHReply dhr = new PacketKexDHReply(msg, 0, msglen);

				kxs.remote_hostkey = dhr.getHostKey();

				if (verifier != null)
				{
					boolean vres = false;

					try
					{
						vres = verifier.verifyServerHostKey(hostname, port, kxs.np.server_host_key_algo, kxs.remote_hostkey);
					}
					catch (Exception e)
					{
						throw (IOException) new IOException(
								"The server hostkey was not accepted by the verifier callback.").initCause(e);
					}

					if (vres == false)
						throw new IOException("The server hostkey was not accepted by the verifier callback");
				}

				kxs.dhx.setF(dhr.getF());

				try
				{
					kxs.H = kxs.dhx.calculateH(csh.getClientString(), csh.getServerString(), kxs.localKEX.getPayload(),
							kxs.remoteKEX.getPayload(), dhr.getHostKey());
				}
				catch (IllegalArgumentException e)
				{
					throw (IOException) new IOException("KEX error.").initCause(e);
				}

				boolean res = verifySignature(dhr.getSignature(), kxs.remote_hostkey);

				if (res == false)
					throw new IOException("Hostkey signature sent by remote is wrong!");

				kxs.K = kxs.dhx.getK();

				finishKex(true);
				kxs.state = -1;
				return;
			}
		}

		throw new IllegalStateException("Unkown KEX method! (" + kxs.np.kex_algo + ")");
	}
}
