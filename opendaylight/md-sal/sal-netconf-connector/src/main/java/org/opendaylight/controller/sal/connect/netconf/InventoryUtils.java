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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InventoryUtils {
    private static final Logger LOG = LoggerFactory.getLogger(InventoryUtils.class);
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

    private InventoryUtils() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Converts date in string format yyyy-MM-dd to java.util.Date.
     *
     * @return java.util.Date conformant to string formatted date yyyy-MM-dd.
     */
    private static Date dateFromString(final String date) {
        // We do not reuse the formatter because it's not thread-safe
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        try {
            return formatter.parse(date);
        } catch (ParseException e) {
            LOG.error("Failed to parse date {}", date, e);
            return null;
        }
    }
}
