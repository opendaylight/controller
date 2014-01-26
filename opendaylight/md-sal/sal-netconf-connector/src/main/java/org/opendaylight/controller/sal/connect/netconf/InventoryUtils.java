/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.connect.netconf;

import java.net.URI;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;

public class InventoryUtils {

    private static final URI INVENTORY_NAMESPACE = URI.create("urn:opendaylight:inventory");
    private static final URI NETCONF_INVENTORY_NAMESPACE = URI.create("urn:opendaylight:netconf-node-inventory");
    private static final Date INVENTORY_REVISION = dateFromString("2013-08-19");
    private static final Date NETCONF_INVENTORY_REVISION = dateFromString("2014-01-08");
    public static final QName INVENTORY_NODES = new QName(INVENTORY_NAMESPACE, INVENTORY_REVISION, "nodes");
    public static final QName INVENTORY_NODE = new QName(INVENTORY_NAMESPACE, INVENTORY_REVISION, "node");
    public static final QName INVENTORY_ID = new QName(INVENTORY_NAMESPACE, INVENTORY_REVISION, "id");
    public static final QName INVENTORY_CONNECTED = new QName(NETCONF_INVENTORY_NAMESPACE, NETCONF_INVENTORY_REVISION,
            "connected");
    public static final QName NETCONF_INVENTORY_INITIAL_CAPABILITY = new QName(NETCONF_INVENTORY_NAMESPACE,
            NETCONF_INVENTORY_REVISION, "initial-capability");

    public static final InstanceIdentifier INVENTORY_PATH = InstanceIdentifier.builder().node(INVENTORY_NODES)
            .toInstance();
    public static final QName NETCONF_INVENTORY_MOUNT = null;

    /**
     * Converts date in string format yyyy-MM-dd to java.util.Date.
     * 
     * @return java.util.Date conformant to string formatted date yyyy-MM-dd.
     */
    private static Date dateFromString(final String date) {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        try {
            return formatter.parse(date);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }
}
