/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.util.handler.ssh.authentication;

import ch.ethz.ssh2.Connection;

import java.io.IOException;

/**
 * Class providing authentication facility to SSH handler.
 */
public abstract class AuthenticationHandler {
    public abstract void authenticate(Connection connection) throws IOException;
}
