/*
 * Copyright (c) 2006-2011 Christian Plattner. All rights reserved.
 * Please refer to the LICENSE.txt for licensing details.
 */
package ch.ethz.ssh2.signature;

import java.math.BigInteger;

/**
 * DSAPrivateKey.
 * 
 * @author Christian Plattner
 * @version 2.50, 03/15/10
 */
public class DSAPrivateKey
{
	private BigInteger p;
	private BigInteger q;
	private BigInteger g;
	private BigInteger x;
	private BigInteger y;

	public DSAPrivateKey(BigInteger p, BigInteger q, BigInteger g,
			BigInteger y, BigInteger x)
	{
		this.p = p;
		this.q = q;
		this.g = g;
		this.y = y;
		this.x = x;
	}

	public BigInteger getP()
	{
		return p;
	}

	public BigInteger getQ()
	{
		return q;
	}
	
	public BigInteger getG()
	{
		return g;
	}

	public BigInteger getY()
	{
		return y;
	}
	
	public BigInteger getX()
	{
		return x;
	}
	
	public DSAPublicKey getPublicKey()
	{
		return new DSAPublicKey(p, q, g, y);
	}
}