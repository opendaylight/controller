/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.connect.netconf;

import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;

public class NetconfInventoryUtils {

    
    public static final QName NETCONF_MOUNT = null;
    public static final QName NETCONF_ENDPOINT = null;
    public static final QName NETCONF_ENDPOINT_ADDRESS = null;
    public static final QName NETCONF_ENDPOINT_PORT = null;


    public static String getEndpointAddress(CompositeNode node) {
        return node.getCompositesByName(NETCONF_ENDPOINT).get(0).getFirstSimpleByName(NETCONF_ENDPOINT_ADDRESS).getValue().toString();
    }
    
    public static String getEndpointPort(CompositeNode node) {
        return node.getCompositesByName(NETCONF_ENDPOINT).get(0).getFirstSimpleByName(NETCONF_ENDPOINT_PORT).getValue().toString();
    }
}
