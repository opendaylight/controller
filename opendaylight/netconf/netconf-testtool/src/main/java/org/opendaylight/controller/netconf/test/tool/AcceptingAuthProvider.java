/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.test.tool;

import java.io.File;
import java.io.IOException;
import org.opendaylight.controller.netconf.ssh.authentication.AuthProvider;
import org.opendaylight.controller.netconf.ssh.authentication.PEMGenerator;

class AcceptingAuthProvider implements AuthProvider {
    private final String privateKeyPEMString;

    public AcceptingAuthProvider() {
        try {
            this.privateKeyPEMString = PEMGenerator.readOrGeneratePK(new File("PK"));
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public synchronized boolean authenticated(final String username, final String password) {
        return true;
    }

    @Override
    public char[] getPEMAsCharArray() {
        return privateKeyPEMString.toCharArray();
    }
}
