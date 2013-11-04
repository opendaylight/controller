/*
 * Copyright (c) 2006-2011 Christian Plattner. All rights reserved.
 * Please refer to the LICENSE.txt for licensing details.
 */
package ch.ethz.ssh2.signature;

import java.io.IOException;
import java.math.BigInteger;
import java.security.SecureRandom;

import ch.ethz.ssh2.crypto.digest.SHA1;
import ch.ethz.ssh2.log.Logger;
import ch.ethz.ssh2.packets.TypesReader;
import ch.ethz.ssh2.packets.TypesWriter;

/**
 * DSASHA1Verify.
 * 
 * @author Christian Plattner
 * @version $Id: DSASHA1Verify.java 41 2011-06-02 10:36:41Z dkocher@sudo.ch $
 */
public class DSASHA1Verify
{
	private static final Logger log = Logger.getLogger(DSASHA1Verify.class);

	public static DSAPublicKey decodeSSHDSAPublicKey(byte[] key) throws IOException
	{
		TypesReader tr = new TypesReader(key);

		String key_format = tr.readString();

		if (key_format.equals("ssh-dss") == false)
			throw new IllegalArgumentException("This is not a ssh-dss public key!");

		BigInteger p = tr.readMPINT();
		BigInteger q = tr.readMPINT();
		BigInteger g = tr.readMPINT();
		BigInteger y = tr.readMPINT();

		if (tr.remain() != 0)
			throw new IOException("Padding in DSA public key!");

		return new DSAPublicKey(p, q, g, y);
	}

	public static byte[] encodeSSHDSAPublicKey(DSAPublicKey pk) throws IOException
	{
		TypesWriter tw = new TypesWriter();

		tw.writeString("ssh-dss");
		tw.writeMPInt(pk.getP());
		tw.writeMPInt(pk.getQ());
		tw.writeMPInt(pk.getG());
		tw.writeMPInt(pk.getY());

		return tw.getBytes();
	}

	public static byte[] encodeSSHDSASignature(DSASignature ds)
	{
		TypesWriter tw = new TypesWriter();

		tw.writeString("ssh-dss");

		byte[] r = ds.getR().toByteArray();
		byte[] s = ds.getS().toByteArray();

		byte[] a40 = new byte[40];

		/* Patch (unsigned) r and s into the target array. */

		int r_copylen = (r.length < 20) ? r.length : 20;
		int s_copylen = (s.length < 20) ? s.length : 20;

		System.arraycopy(r, r.length - r_copylen, a40, 20 - r_copylen, r_copylen);
		System.arraycopy(s, s.length - s_copylen, a40, 40 - s_copylen, s_copylen);

		tw.writeString(a40, 0, 40);

		return tw.getBytes();
	}

	public static DSASignature decodeSSHDSASignature(byte[] sig) throws IOException
	{
		byte[] rsArray;

		if (sig.length == 40)
		{
			rsArray = sig;
		}
		else
		{
			TypesReader tr = new TypesReader(sig);

			String sig_format = tr.readString();

			if (sig_format.equals("ssh-dss") == false)
				throw new IOException("Peer sent wrong signature format");

			rsArray = tr.readByteString();

			if (rsArray.length != 40)
				throw new IOException("Peer sent corrupt signature");

			if (tr.remain() != 0)
				throw new IOException("Padding in DSA signature!");
		}

		/* Remember, s and r are unsigned ints. */

		byte[] tmp = new byte[20];

		System.arraycopy(rsArray, 0, tmp, 0, 20);
		BigInteger r = new BigInteger(1, tmp);

		System.arraycopy(rsArray, 20, tmp, 0, 20);
		BigInteger s = new BigInteger(1, tmp);

		if (log.isDebugEnabled())
		{
			log.debug("decoded ssh-dss signature: first bytes r(" + ((rsArray[0]) & 0xff) + "), s("
					+ ((rsArray[20]) & 0xff) + ")");
		}

		return new DSASignature(r, s);
	}

	public static boolean verifySignature(byte[] message, DSASignature ds, DSAPublicKey dpk) throws IOException
	{
		/* Inspired by Bouncycastle's DSASigner class */

		SHA1 md = new SHA1();
		md.update(message);
		byte[] sha_message = new byte[md.getDigestLength()];
		md.digest(sha_message);

		BigInteger m = new BigInteger(1, sha_message);

		BigInteger r = ds.getR();
		BigInteger s = ds.getS();

		BigInteger g = dpk.getG();
		BigInteger p = dpk.getP();
		BigInteger q = dpk.getQ();
		BigInteger y = dpk.getY();

		BigInteger zero = BigInteger.ZERO;

		if (log.isDebugEnabled())
		{
			log.debug("ssh-dss signature: m: " + m.toString(16));
			log.debug("ssh-dss signature: r: " + r.toString(16));
			log.debug("ssh-dss signature: s: " + s.toString(16));
			log.debug("ssh-dss signature: g: " + g.toString(16));
			log.debug("ssh-dss signature: p: " + p.toString(16));
			log.debug("ssh-dss signature: q: " + q.toString(16));
			log.debug("ssh-dss signature: y: " + y.toString(16));
		}

		if (zero.compareTo(r) >= 0 || q.compareTo(r) <= 0)
		{
			log.warning("ssh-dss signature: zero.compareTo(r) >= 0 || q.compareTo(r) <= 0");
			return false;
		}

		if (zero.compareTo(s) >= 0 || q.compareTo(s) <= 0)
		{
			log.warning("ssh-dss signature: zero.compareTo(s) >= 0 || q.compareTo(s) <= 0");
			return false;
		}

		BigInteger w = s.modInverse(q);

		BigInteger u1 = m.multiply(w).mod(q);
		BigInteger u2 = r.multiply(w).mod(q);

		u1 = g.modPow(u1, p);
		u2 = y.modPow(u2, p);

		BigInteger v = u1.multiply(u2).mod(p).mod(q);

		return v.equals(r);
	}

	public static DSASignature generateSignature(byte[] message, DSAPrivateKey pk, SecureRandom rnd)
	{
		SHA1 md = new SHA1();
		md.update(message);
		byte[] sha_message = new byte[md.getDigestLength()];
		md.digest(sha_message);

		BigInteger m = new BigInteger(1, sha_message);
		BigInteger k;
		int qBitLength = pk.getQ().bitLength();

		do
		{
			k = new BigInteger(qBitLength, rnd);
		}
		while (k.compareTo(pk.getQ()) >= 0);

		BigInteger r = pk.getG().modPow(k, pk.getP()).mod(pk.getQ());

		k = k.modInverse(pk.getQ()).multiply(m.add((pk).getX().multiply(r)));

		BigInteger s = k.mod(pk.getQ());

		return new DSASignature(r, s);
	}
}
