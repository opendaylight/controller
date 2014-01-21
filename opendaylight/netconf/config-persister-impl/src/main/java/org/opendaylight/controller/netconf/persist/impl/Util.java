/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.persist.impl;

import org.opendaylight.controller.netconf.client.NetconfClient;
import org.opendaylight.controller.netconf.client.NetconfClientDispatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

public final class Util {
    private static final Logger logger = LoggerFactory.getLogger(Util.class);


    public static boolean isSubset(NetconfClient netconfClient, Set<String> expectedCaps) {
        return isSubset(netconfClient.getCapabilities(), expectedCaps);

    }

    private static boolean isSubset(Set<String> currentCapabilities, Set<String> expectedCaps) {
        for (String exCap : expectedCaps) {
            if (currentCapabilities.contains(exCap) == false)
                return false;
        }
        return true;
    }

    public static void closeClientAndDispatcher(NetconfClient client) {
        NetconfClientDispatcher dispatcher = client.getNetconfClientDispatcher();
        Exception fromClient = null;
        try {
            client.close();
        } catch (Exception e) {
            fromClient = e;
        } finally {
            try {
                dispatcher.close();
            } catch (Exception e) {
                if (fromClient != null) {
                    e.addSuppressed(fromClient);
                }
                throw new RuntimeException("Error closing temporary client ", e);
            }
        }
    }
}
