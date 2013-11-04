/*
 * Copyright (c) 2006-2013 Christian Plattner. All rights reserved.
 * Please refer to the LICENSE.txt for licensing details.
 */
package ch.ethz.ssh2.crypto.dh;

import java.math.BigInteger;
import java.security.SecureRandom;

import ch.ethz.ssh2.crypto.digest.HashForSSH2Types;
import ch.ethz.ssh2.log.Logger;
import ch.ethz.ssh2.util.StringEncoder;

/**
 * @author Christian Plattner
 * @version $Id: DhExchange.java 47 2013-07-31 23:59:52Z cleondris@gmail.com $
 */
public class DhExchange
{
	private static final Logger log = Logger.getLogger(DhExchange.class);

	/* Given by the standard */

	static final BigInteger p1, p14;
	static final BigInteger g;

	BigInteger p;

	/* Client public and private */

	BigInteger e;
	BigInteger x;

	/* Server public and private */

	BigInteger f;
	BigInteger y;
	
	/* Shared secret */

	BigInteger k;

	static
	{
		final String p1_string = "17976931348623159077083915679378745319786029604875"
				+ "60117064444236841971802161585193689478337958649255415021805654859805036464"
				+ "40548199239100050792877003355816639229553136239076508735759914822574862575"
				+ "00742530207744771258955095793777842444242661733472762929938766870920560605"
				+ "0270810842907692932019128194467627007";

		final String p14_string = "FFFFFFFFFFFFFFFFC90FDAA22168C234C4C6628B80DC1CD129"
				+ "024E088A67CC74020BBEA63B139B22514A08798E3404DDEF9519B3CD3A431B302B0"
				+ "A6DF25F14374FE1356D6D51C245E485B576625E7EC6F44C42E9A637ED6B0BFF5CB"
				+ "6F406B7EDEE386BFB5A899FA5AE9F24117C4B1FE649286651ECE45B3DC2007CB8A"
				+ "163BF0598DA48361C55D39A69163FA8FD24CF5F83655D23DCA3AD961C62F356208"
				+ "552BB9ED529077096966D670C354E4ABC9804F1746C08CA18217C32905E462E36C"
				+ "E3BE39E772C180E86039B2783A2EC07A28FB5C55DF06F4C52C9DE2BCBF69558171"
				+ "83995497CEA956AE515D2261898FA051015728E5A8AACAA68FFFFFFFFFFFFFFFF";

		p1 = new BigInteger(p1_string);
		p14 = new BigInteger(p14_string, 16);
		g = new BigInteger("2");
	}

	public DhExchange()
	{
	}

	public void clientInit(int group, SecureRandom rnd)
	{
		k = null;

		if (group == 1)
			p = p1;
		else if (group == 14)
			p = p14;
		else
			throw new IllegalArgumentException("Unknown DH group " + group);

		while(true)
		{
			x = new BigInteger(p.bitLength() - 1, rnd);
			if (x.compareTo(BigInteger.ONE) > 0)
				break;
		}
		
		e = g.modPow(x, p);
	}
	
	public void serverInit(int group, SecureRandom rnd)
	{
		k = null;

		if (group == 1)
			p = p1;
		else if (group == 14)
			p = p14;
		else
			throw new IllegalArgumentException("Unknown DH group " + group);

		y = new BigInteger(p.bitLength() - 1, rnd);

		f = g.modPow(y, p);
	}
	
	/**
	 * @return Returns the e.
	 * @throws IllegalStateException
	 */
	public BigInteger getE()
	{
		if (e == null)
			throw new IllegalStateException("DhDsaExchange not initialized!");

		return e;
	}

	/**
	 * @return Returns the f.
	 * @throws IllegalStateException
	 */
	public BigInteger getF()
	{
		if (f == null)
			throw new IllegalStateException("DhDsaExchange not initialized!");

		return f;
	}
	
	/**
	 * @return Returns the shared secret k.
	 * @throws IllegalStateException
	 */
	public BigInteger getK()
	{
		if (k == null)
			throw new IllegalStateException("Shared secret not yet known, need f first!");

		return k;
	}

	/**
	 * @param f
	 */
	public void setF(BigInteger f)
	{
		if (e == null)
			throw new IllegalStateException("DhDsaExchange not initialized!");

		if (BigInteger.ZERO.compareTo(f) >= 0 || p.compareTo(f) <= 0)
			throw new IllegalArgumentException("Invalid f specified!");

		this.f = f;
		this.k = f.modPow(x, p);
	}
	
	/**
	 * @param e
	 */
	public void setE(BigInteger e)
	{
		if (f == null)
			throw new IllegalStateException("DhDsaExchange not initialized!");

		if (BigInteger.ZERO.compareTo(e) >= 0 || p.compareTo(e) <= 0)
			throw new IllegalArgumentException("Invalid e specified!");

		this.e = e;
		this.k = e.modPow(y, p);
	}

	public byte[] calculateH(byte[] clientversion, byte[] serverversion, byte[] clientKexPayload,
			byte[] serverKexPayload, byte[] hostKey)
	{
		HashForSSH2Types hash = new HashForSSH2Types("SHA1");

		if (log.isInfoEnabled())
		{
			log.info("Client: '" + StringEncoder.GetString(clientversion) + "'");
		    log.info("Server: '" + StringEncoder.GetString(serverversion) + "'");
		}

		hash.updateByteString(clientversion);
		hash.updateByteString(serverversion);
		hash.updateByteString(clientKexPayload);
		hash.updateByteString(serverKexPayload);
		hash.updateByteString(hostKey);
		hash.updateBigInt(e);
		hash.updateBigInt(f);
		hash.updateBigInt(k);

		return hash.getDigest();
	}
}
