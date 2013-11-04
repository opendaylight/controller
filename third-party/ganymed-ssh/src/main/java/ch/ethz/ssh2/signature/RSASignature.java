/*
 * Copyright (c) 2006-2011 Christian Plattner. All rights reserved.
 * Please refer to the LICENSE.txt for licensing details.
 */
package ch.ethz.ssh2.signature;

import java.math.BigInteger;


/**
 * RSASignature.
 * 
 * @author Christian Plattner
 * @version 2.50, 03/15/10
 */

public class RSASignature
{
	BigInteger s;

	public BigInteger getS()
	{
		return s;
	}

	public RSASignature(BigInteger s)
	{
		this.s = s;
	}
}