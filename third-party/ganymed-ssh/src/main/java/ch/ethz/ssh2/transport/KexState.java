/*
 * Copyright (c) 2006-2011 Christian Plattner. All rights reserved.
 * Please refer to the LICENSE.txt for licensing details.
 */
package ch.ethz.ssh2.transport;

import ch.ethz.ssh2.DHGexParameters;
import ch.ethz.ssh2.crypto.dh.DhExchange;
import ch.ethz.ssh2.crypto.dh.DhGroupExchange;
import java.math.BigInteger;
import ch.ethz.ssh2.packets.PacketKexInit;
import ch.ethz.ssh2.signature.DSAPrivateKey;
import ch.ethz.ssh2.signature.RSAPrivateKey;

/**
 * KexState.
 * 
 * @author Christian Plattner
 * @version 2.50, 03/15/10
 */
public class KexState
{
	public PacketKexInit localKEX;
	public PacketKexInit remoteKEX;
	public NegotiatedParameters np;
	public int state = 0;

	public BigInteger K;
	public byte[] H;
	
	public byte[] remote_hostkey;
	
	public DhExchange dhx;
	public DhGroupExchange dhgx;
	public DHGexParameters dhgexParameters;
	
	public DSAPrivateKey local_dsa_key;
	public RSAPrivateKey local_rsa_key;
}
