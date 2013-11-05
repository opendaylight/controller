/*
 * Copyright (c) 2006-2011 Christian Plattner. All rights reserved.
 * Please refer to the LICENSE.txt for licensing details.
 */
package ch.ethz.ssh2.signature;

import java.math.BigInteger;

/**
 * RSAPublicKey.
 * 
 * @author Christian Plattner
 * @version 2.50, 03/15/10
 */
public class RSAPublicKey
{
	BigInteger e;
	BigInteger n;

	public RSAPublicKey(BigInteger e, BigInteger n)
	{
		this.e = e;
		this.n = n;
	}

	public BigInteger getE()
	{
		return e;
	}

	public BigInteger getN()
	{
		return n;
	}
}