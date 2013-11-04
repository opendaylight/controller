/*
 * Copyright (c) 2006-2011 Christian Plattner. All rights reserved.
 * Please refer to the LICENSE.txt for licensing details.
 */
package ch.ethz.ssh2.signature;

import java.io.IOException;
import java.math.BigInteger;

import ch.ethz.ssh2.crypto.SimpleDERReader;
import ch.ethz.ssh2.crypto.digest.SHA1;
import ch.ethz.ssh2.log.Logger;
import ch.ethz.ssh2.packets.TypesReader;
import ch.ethz.ssh2.packets.TypesWriter;

/**
 * RSASHA1Verify.
 * 
 * @author Christian Plattner
 * @version $Id: RSASHA1Verify.java 41 2011-06-02 10:36:41Z dkocher@sudo.ch $
 */
public class RSASHA1Verify
{
	private static final Logger log = Logger.getLogger(RSASHA1Verify.class);

	public static RSAPublicKey decodeSSHRSAPublicKey(byte[] key) throws IOException
	{
		TypesReader tr = new TypesReader(key);

		String key_format = tr.readString();

		if (key_format.equals("ssh-rsa") == false)
			throw new IllegalArgumentException("This is not a ssh-rsa public key");

		BigInteger e = tr.readMPINT();
		BigInteger n = tr.readMPINT();

		if (tr.remain() != 0)
			throw new IOException("Padding in RSA public key!");

		return new RSAPublicKey(e, n);
	}

	public static byte[] encodeSSHRSAPublicKey(RSAPublicKey pk) throws IOException
	{
		TypesWriter tw = new TypesWriter();

		tw.writeString("ssh-rsa");
		tw.writeMPInt(pk.getE());
		tw.writeMPInt(pk.getN());

		return tw.getBytes();
	}

	public static RSASignature decodeSSHRSASignature(byte[] sig) throws IOException
	{
		TypesReader tr = new TypesReader(sig);

		String sig_format = tr.readString();

		if (sig_format.equals("ssh-rsa") == false)
			throw new IOException("Peer sent wrong signature format");

		/* S is NOT an MPINT. "The value for 'rsa_signature_blob' is encoded as a string
		 * containing s (which is an integer, without lengths or padding, unsigned and in
		 * network byte order)." See also below.
		 */

		byte[] s = tr.readByteString();

		if (s.length == 0)
			throw new IOException("Error in RSA signature, S is empty.");

		if (log.isDebugEnabled())
		{
			log.debug("Decoding ssh-rsa signature string (length: " + s.length + ")");
		}

		if (tr.remain() != 0)
			throw new IOException("Padding in RSA signature!");

		return new RSASignature(new BigInteger(1, s));
	}

	public static byte[] encodeSSHRSASignature(RSASignature sig) throws IOException
	{
		TypesWriter tw = new TypesWriter();

		tw.writeString("ssh-rsa");

		/* S is NOT an MPINT. "The value for 'rsa_signature_blob' is encoded as a string
		 * containing s (which is an integer, without lengths or padding, unsigned and in
		 * network byte order)."
		 */

		byte[] s = sig.getS().toByteArray();

		/* Remove first zero sign byte, if present */

		if ((s.length > 1) && (s[0] == 0x00))
			tw.writeString(s, 1, s.length - 1);
		else
			tw.writeString(s, 0, s.length);

		return tw.getBytes();
	}

	public static RSASignature generateSignature(byte[] message, RSAPrivateKey pk) throws IOException
	{
		SHA1 md = new SHA1();
		md.update(message);
		byte[] sha_message = new byte[md.getDigestLength()];
		md.digest(sha_message);

		byte[] der_header = new byte[] { 0x30, 0x21, 0x30, 0x09, 0x06, 0x05, 0x2b, 0x0e, 0x03, 0x02, 0x1a, 0x05, 0x00,
				0x04, 0x14 };

		int rsa_block_len = (pk.getN().bitLength() + 7) / 8;

		int num_pad = rsa_block_len - (2 + der_header.length + sha_message.length) - 1;

		if (num_pad < 8)
			throw new IOException("Cannot sign with RSA, message too long");

		byte[] sig = new byte[der_header.length + sha_message.length + 2 + num_pad];

		sig[0] = 0x01;

		for (int i = 0; i < num_pad; i++)
		{
			sig[i + 1] = (byte) 0xff;
		}

		sig[num_pad + 1] = 0x00;

		System.arraycopy(der_header, 0, sig, 2 + num_pad, der_header.length);
		System.arraycopy(sha_message, 0, sig, 2 + num_pad + der_header.length, sha_message.length);

		BigInteger m = new BigInteger(1, sig);

		BigInteger s = m.modPow(pk.getD(), pk.getN());

		return new RSASignature(s);
	}

	public static boolean verifySignature(byte[] message, RSASignature ds, RSAPublicKey dpk) throws IOException
	{
		SHA1 md = new SHA1();
		md.update(message);
		byte[] sha_message = new byte[md.getDigestLength()];
		md.digest(sha_message);

		BigInteger n = dpk.getN();
		BigInteger e = dpk.getE();
		BigInteger s = ds.getS();

		if (n.compareTo(s) <= 0)
		{
			log.warning("ssh-rsa signature: n.compareTo(s) <= 0");
			return false;
		}

		int rsa_block_len = (n.bitLength() + 7) / 8;

		/* And now the show begins */

		if (rsa_block_len < 1)
		{
			log.warning("ssh-rsa signature: rsa_block_len < 1");
			return false;
		}

		byte[] v = s.modPow(e, n).toByteArray();

		int startpos = 0;

		if ((v.length > 0) && (v[0] == 0x00))
			startpos++;

		if ((v.length - startpos) != (rsa_block_len - 1))
		{
			log.warning("ssh-rsa signature: (v.length - startpos) != (rsa_block_len - 1)");
			return false;
		}

		if (v[startpos] != 0x01)
		{
			log.warning("ssh-rsa signature: v[startpos] != 0x01");
			return false;
		}

		int pos = startpos + 1;

		while (true)
		{
			if (pos >= v.length)
			{
				log.warning("ssh-rsa signature: pos >= v.length");
				return false;
			}
			if (v[pos] == 0x00)
				break;
			if (v[pos] != (byte) 0xff)
			{
				log.warning("ssh-rsa signature: v[pos] != (byte) 0xff");
				return false;
			}
			pos++;
		}

		int num_pad = pos - (startpos + 1);

		if (num_pad < 8)
		{
			log.warning("ssh-rsa signature: num_pad < 8");
			return false;
		}

		pos++;

		if (pos >= v.length)
		{
			log.warning("ssh-rsa signature: pos >= v.length");
			return false;
		}

		SimpleDERReader dr = new SimpleDERReader(v, pos, v.length - pos);

		byte[] seq = dr.readSequenceAsByteArray();

		if (dr.available() != 0)
		{
			log.warning("ssh-rsa signature: dr.available() != 0");
			return false;
		}

		dr.resetInput(seq);

		/* Read digestAlgorithm */

		byte digestAlgorithm[] = dr.readSequenceAsByteArray();

		/* Inspired by RFC 3347, however, ignoring the comment regarding old BER based implementations */

		if ((digestAlgorithm.length < 8) || (digestAlgorithm.length > 9))
		{
			log.warning("ssh-rsa signature: (digestAlgorithm.length < 8) || (digestAlgorithm.length > 9)");
			return false;
		}

		byte[] digestAlgorithm_sha1 = new byte[] { 0x06, 0x05, 0x2b, 0x0e, 0x03, 0x02, 0x1a, 0x05, 0x00 };

		for (int i = 0; i < digestAlgorithm.length; i++)
		{
			if (digestAlgorithm[i] != digestAlgorithm_sha1[i])
			{
				log.warning("ssh-rsa signature: digestAlgorithm[i] != digestAlgorithm_sha1[i]");
				return false;
			}
		}

		byte[] digest = dr.readOctetString();

		if (dr.available() != 0)
		{
			log.warning("ssh-rsa signature: dr.available() != 0 (II)");
			return false;
		}
			
		if (digest.length != sha_message.length)
		{
			log.warning("ssh-rsa signature: digest.length != sha_message.length");
			return false;
		}

		for (int i = 0; i < sha_message.length; i++)
		{
			if (sha_message[i] != digest[i])
			{
				log.warning("ssh-rsa signature: sha_message[i] != digest[i]");
				return false;
			}
		}

		return true;
	}
}
