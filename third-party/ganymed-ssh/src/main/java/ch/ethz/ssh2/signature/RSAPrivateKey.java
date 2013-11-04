/*
 * Copyright (c) 2006-2013 Christian Plattner. All rights reserved.
 * Please refer to the LICENSE.txt for licensing details.
 */
package ch.ethz.ssh2.signature;

import java.math.BigInteger;
import java.security.SecureRandom;

/**
 * RSAPrivateKey.
 * 
 * @author Christian Plattner
 * @version 2.50, 03/15/10
 */
public class RSAPrivateKey
{
	private BigInteger d;
	private BigInteger e;
	private BigInteger n;

	public RSAPrivateKey(BigInteger d, BigInteger e, BigInteger n)
	{
		this.d = d;
		this.e = e;
		this.n = n;
	}

	public BigInteger getD()
	{
		return d;
	}

	public BigInteger getE()
	{
		return e;
	}

	public BigInteger getN()
	{
		return n;
	}

	public RSAPublicKey getPublicKey()
	{
		return new RSAPublicKey(e, n);
	}

	/**
	 * Generate an RSA hostkey for testing purposes only.
	 * 
	 * @param numbits Key length in bits
	 * @return
	 */
	public static RSAPrivateKey generateKey(int numbits)
	{
		return generateKey(new SecureRandom(), numbits);
	}

	/**
	 *  Generate an RSA hostkey for testing purposes only.
	 *  
	 * @param rnd Source for random bits
	 * @param numbits Key length in bits
	 * @return
	 */
	public static RSAPrivateKey generateKey(SecureRandom rnd, int numbits)
	{
		BigInteger p = BigInteger.probablePrime(numbits / 2, rnd);
		BigInteger q = BigInteger.probablePrime(numbits / 2, rnd);
		BigInteger phi = (p.subtract(BigInteger.ONE)).multiply(q.subtract(BigInteger.ONE));

		BigInteger n = p.multiply(q);
		BigInteger e = new BigInteger("65537");
		BigInteger d = e.modInverse(phi);

		return new RSAPrivateKey(d, e, n);
	}
}