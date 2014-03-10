/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.persist.impl;

import java.util.Set;
import org.opendaylight.controller.netconf.client.NetconfClient;
import org.opendaylight.controller.netconf.client.NetconfClientDispatcher;

public final class Util {

    private Util(){
        // not called - private constructor for utility class
    }
    public static boolean isSubset(NetconfClient netconfClient, Set<String> expectedCaps) {
        return isSubset(netconfClient.getCapabilities(), expectedCaps);

    }

    private static boolean isSubset(Set<String> currentCapabilities, Set<String> expectedCaps) {
        for (String exCap : expectedCaps) {
            if (!currentCapabilities.contains(exCap)){
                return false;
            }
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
                throw new IllegalStateException("Error closing temporary client ", e);
            }
        }
    }
}
