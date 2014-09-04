/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.test.tool;

import org.opendaylight.controller.netconf.auth.AuthProvider;

class AcceptingAuthProvider implements AuthProvider {

    @Override
    public synchronized boolean authenticated(final String username, final String password) {
        return true;
    }

}
