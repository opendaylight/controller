/*
 * Copyright (c) 2006-2011 Christian Plattner. All rights reserved.
 * Please refer to the LICENSE.txt for licensing details.
 */
package ch.ethz.ssh2.crypto.cipher;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

/**
 * BlockCipherFactory.
 *
 * @author Christian Plattner
 * @version $Id: BlockCipherFactory.java 47 2013-07-31 23:59:52Z cleondris@gmail.com $
 */
public class BlockCipherFactory
{
	private static final class CipherEntry
	{
		String type;
		int blocksize;
		int keysize;
		String cipherClass;

		public CipherEntry(String type, int blockSize, int keySize, String cipherClass)
		{
			this.type = type;
			this.blocksize = blockSize;
			this.keysize = keySize;
			this.cipherClass = cipherClass;
		}
	}

	private static final List<CipherEntry> ciphers = new Vector<CipherEntry>();

	static
	{
		/* Higher Priority First */
		ciphers.add(new CipherEntry("aes128-ctr", 16, 16, "ch.ethz.ssh2.crypto.cipher.AES"));
		ciphers.add(new CipherEntry("aes192-ctr", 16, 24, "ch.ethz.ssh2.crypto.cipher.AES"));
		ciphers.add(new CipherEntry("aes256-ctr", 16, 32, "ch.ethz.ssh2.crypto.cipher.AES"));
		ciphers.add(new CipherEntry("blowfish-ctr", 8, 16, "ch.ethz.ssh2.crypto.cipher.BlowFish"));

		ciphers.add(new CipherEntry("aes128-cbc", 16, 16, "ch.ethz.ssh2.crypto.cipher.AES"));
		ciphers.add(new CipherEntry("aes192-cbc", 16, 24, "ch.ethz.ssh2.crypto.cipher.AES"));
		ciphers.add(new CipherEntry("aes256-cbc", 16, 32, "ch.ethz.ssh2.crypto.cipher.AES"));
		ciphers.add(new CipherEntry("blowfish-cbc", 8, 16, "ch.ethz.ssh2.crypto.cipher.BlowFish"));

		ciphers.add(new CipherEntry("3des-ctr", 8, 24, "ch.ethz.ssh2.crypto.cipher.DESede"));
		ciphers.add(new CipherEntry("3des-cbc", 8, 24, "ch.ethz.ssh2.crypto.cipher.DESede"));
	}

	public static String[] getDefaultCipherList()
	{
		List<String> list = new ArrayList<String>(ciphers.size());
		for (CipherEntry ce : ciphers)
		{
			list.add(ce.type);
		}
		return list.toArray(new String[ciphers.size()]);
	}

	public static void checkCipherList(String[] cipherCandidates)
	{
		for (String cipherCandidate : cipherCandidates)
		{
			getEntry(cipherCandidate);
		}
	}

	//	@SuppressWarnings("rawtypes")
	public static BlockCipher createCipher(String type, boolean encrypt, byte[] key, byte[] iv)
	{
		try
		{
			CipherEntry ce = getEntry(type);
			Class<?> cc = Class.forName(ce.cipherClass);
			BlockCipher bc = (BlockCipher) cc.newInstance();

			if (type.endsWith("-cbc"))
			{
				bc.init(encrypt, key);
				return new CBCMode(bc, iv, encrypt);
			}
			else if (type.endsWith("-ctr"))
			{
				bc.init(true, key);
				return new CTRMode(bc, iv, encrypt);
			}
			throw new IllegalArgumentException("Cannot instantiate " + type);
		}
		catch (ClassNotFoundException e)
		{
			throw new IllegalArgumentException("Cannot instantiate " + type, e);
		}
		catch (InstantiationException e)
		{
			throw new IllegalArgumentException("Cannot instantiate " + type, e);
		}
		catch (IllegalAccessException e)
		{
			throw new IllegalArgumentException("Cannot instantiate " + type, e);
		}
	}

	private static CipherEntry getEntry(String type)
	{
		for (CipherEntry ce : ciphers)
		{
			if (ce.type.equals(type))
			{
				return ce;
			}
		}
		throw new IllegalArgumentException("Unkown algorithm " + type);
	}

	public static int getBlockSize(String type)
	{
		CipherEntry ce = getEntry(type);
		return ce.blocksize;
	}

	public static int getKeySize(String type)
	{
		CipherEntry ce = getEntry(type);
		return ce.keysize;
	}
}
