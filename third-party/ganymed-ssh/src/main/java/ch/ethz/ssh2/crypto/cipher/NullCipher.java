/*
 * Copyright (c) 2006-2011 Christian Plattner. All rights reserved.
 * Please refer to the LICENSE.txt for licensing details.
 */
package ch.ethz.ssh2.crypto.cipher;

/**
 * NullCipher.
 * 
 * @author Christian Plattner
 * @version 2.50, 03/15/10
 */
public class NullCipher implements BlockCipher
{
	private int blockSize = 8;
	
	public NullCipher()
	{
	}

	public NullCipher(int blockSize)
	{
		this.blockSize = blockSize;
	}
	
	public void init(boolean forEncryption, byte[] key)
	{
	}

	public int getBlockSize()
	{
		return blockSize;
	}

	public void transformBlock(byte[] src, int srcoff, byte[] dst, int dstoff)
	{
		System.arraycopy(src, srcoff, dst, dstoff, blockSize);
	}
}
