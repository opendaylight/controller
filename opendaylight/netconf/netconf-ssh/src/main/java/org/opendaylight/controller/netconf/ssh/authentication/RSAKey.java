/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.ssh.authentication;

import ch.ethz.ssh2.signature.RSAPrivateKey;

import java.math.BigInteger;

public class RSAKey implements KeyStoreHandler {

    private static RSAPrivateKey hostkey = null;
    private static String user = "netconf";
    private static String password = "netconf";
    static {

        BigInteger p = new BigInteger("2967886344240998436887630478678331145236162666668503940430852241825039192450179076148979094256007292741704260675085192441025058193581327559331546948442042987131728039318861235625879376246169858586459472691398815098207618446039");    //.BigInteger.probablePrime(N / 2, rnd);
        BigInteger q = new BigInteger("4311534819291430017572425052029278681302539382618633848168923130451247487970187151403375389974616614405320169278870943605377518341666894603659873284783174749122655429409273983428000534304828056597676444751611433784228298909767"); //BigInteger.probablePrime(N / 2, rnd);
        BigInteger phi = (p.subtract(BigInteger.ONE)).multiply(q.subtract(BigInteger.ONE));

        BigInteger n = p.multiply(q);
        BigInteger e = new BigInteger("65537");
        BigInteger d = e.modInverse(phi);

        hostkey = new RSAPrivateKey(d, e, n);
    }

    @Override
    public RSAPrivateKey getPrivateKey() {
        return hostkey;
    }
}
