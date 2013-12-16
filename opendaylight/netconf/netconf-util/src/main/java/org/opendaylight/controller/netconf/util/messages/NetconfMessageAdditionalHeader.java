/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.util.messages;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;

/**
 * Additional header can be used with hello message to carry information about
 * session's connection. Provided information can be reported via netconf
 * monitoring.
 * <pre>
 * It has pattern "[username; host-address:port; transport; session-identifier;]"
 * username - name of account on a remote
 * host-address - client's IP address
 * port - port number
 * transport - tcp, ssh
 * session-identifier - persister, client
 * Session-identifier is optional, others mandatory.
 * </pre>
 */
public class NetconfMessageAdditionalHeader {

    private static final String SC = ";";

    public static String toString(String userName, String hostAddress, String port, String transport,
            Optional<String> sessionIdentifier) {
        Preconditions.checkNotNull(userName);
        Preconditions.checkNotNull(hostAddress);
        Preconditions.checkNotNull(port);
        Preconditions.checkNotNull(transport);
        String identifier = sessionIdentifier.isPresent() ? sessionIdentifier.get() : "";
        return "[" + userName + SC + hostAddress + ":" + port + SC + transport + SC + identifier + SC + "]"
                + System.lineSeparator();
    }
}
