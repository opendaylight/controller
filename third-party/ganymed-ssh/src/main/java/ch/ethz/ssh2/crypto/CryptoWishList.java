/*
 * Copyright (c) 2006-2011 Christian Plattner. All rights reserved.
 * Please refer to the LICENSE.txt for licensing details.
 */
package ch.ethz.ssh2.crypto;

import ch.ethz.ssh2.crypto.cipher.BlockCipherFactory;
import ch.ethz.ssh2.crypto.digest.MAC;
import ch.ethz.ssh2.transport.KexManager;

/**
 * CryptoWishList.
 * 
 * @author Christian Plattner
 * @version 2.50, 03/15/10
 */
public class CryptoWishList
{
	public String[] kexAlgorithms = KexManager.getDefaultClientKexAlgorithmList();
	public String[] serverHostKeyAlgorithms = KexManager.getDefaultServerHostkeyAlgorithmList();
	public String[] c2s_enc_algos = BlockCipherFactory.getDefaultCipherList();
	public String[] s2c_enc_algos = BlockCipherFactory.getDefaultCipherList();
	public String[] c2s_mac_algos = MAC.getMacList();
	public String[] s2c_mac_algos = MAC.getMacList();

	public static CryptoWishList forServer()
	{
		CryptoWishList cwl = new CryptoWishList();
		cwl.kexAlgorithms = KexManager.getDefaultServerKexAlgorithmList();
		return cwl;
	}
}
