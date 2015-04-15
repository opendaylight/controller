/**
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.md.sal.rest.common;

import com.google.common.base.Splitter;
import java.net.URI;
import java.text.SimpleDateFormat;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * sal-rest-connector
 * org.opendaylight.controller.md.sal.rest.common
 *
 * Class is designed as a project internal constant holder for comfortable manipulation
 * in future changes. Class also provides a one place for all needed specific
 * constants mostly used for parsing URIs.
 *
 * @author <a href="mailto:vdemcak@cisco.com">Vaclav Demcak</a>
 *
 * Created: Feb 24, 2015
 */
public final class RestconfInternalConstants {

    private static final Logger LOG = LoggerFactory.getLogger(RestconfInternalConstants.class);

    // TODO : redirect all possible references

    private RestconfInternalConstants () {
        throw new UnsupportedOperationException("Utility class");
    }

    public static final SimpleDateFormat REVISION_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    public static final Splitter SLASH_SPLITTER = Splitter.on('/');

    public static final Splitter AT_SPLITTER = Splitter.on("@");

    public static final String NULL_VALUE = "null";

    public static final String MOUNT_MODULE = "yang-ext";

    public static final String MOUNT_NODE = "mount";

    public static final String MOUNT = "yang-ext:mount";

    public static final String URI_ENCODING_CHAR_SET = "ISO-8859-1";

    public static final String NETCONF_BASE_PAYLOAD_NAME = "data";

    public static final String NETCONF_BASE = "urn:ietf:params:xml:ns:netconf:base:1.0";

    public static final String SCOPE_PARAM_NAME = "scope";

    public static final String DATASTORE_PARAM_NAME = "datastore";

    public static final URI NAMESPACE_EVENT_SUBSCRIPTION_AUGMENT = URI.create("urn:sal:restconf:event:subscription");

    public static final LogicalDatastoreType DEFAULT_DATASTORE = LogicalDatastoreType.CONFIGURATION;

    public static final DataChangeScope DEFAULT_SCOPE = DataChangeScope.BASE;

    public static final String SAL_REMOTE_RPC_SUBSRCIBE = "create-data-change-event-subscription";

    public static final String SAL_REMOTE_NAMESPACE = "urn:opendaylight:params:xml:ns:yang:controller:md:sal:remote";

    public static final String MOUNT_POINT_MODULE_NAME = "ietf-netconf";

    public static final int CHAR_NOT_FOUND = -1;

    public static final int NOTIFICATION_PORT = 8181;

    public static final String URI_PARAM_PRETTY_PRINT = "prettyPrint";

    public static final String URI_PARAM_DEPTH = "depth";

}
