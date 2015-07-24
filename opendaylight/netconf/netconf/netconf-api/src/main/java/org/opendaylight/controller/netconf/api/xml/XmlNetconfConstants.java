/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.api.xml;

import org.opendaylight.controller.config.util.xml.XmlMappingConstants;

public final class XmlNetconfConstants {

    private XmlNetconfConstants() {}

    public static final String CAPABILITY = "capability";
    public static final String CAPABILITIES = "capabilities";
    public static final String COMMIT = "commit";
    public static final String OPERATION_ATTR_KEY = "operation";
    public static final String CONFIG_KEY = "config";
    public static final String DATA_KEY = "data";
    public static final String OK = "ok";
    public static final String FILTER = "filter";
    public static final String SOURCE_KEY = "source";
    public static final String RPC_KEY = "rpc";
    public static final String NOTIFICATION_ELEMENT_NAME = "notification";

    public static final String MESSAGE_ID = "message-id";
    public static final String SESSION_ID = "session-id";

    public static final String GET = "get";
    public static final String GET_CONFIG = "get-config";

    public static final String URN_IETF_PARAMS_XML_NS_NETCONF_BASE_1_0 = XmlMappingConstants.URN_IETF_PARAMS_XML_NS_NETCONF_BASE_1_0;
    public static final String URN_IETF_PARAMS_NETCONF_BASE_1_0 = "urn:ietf:params:netconf:base:1.0";
    public static final String URN_IETF_PARAMS_NETCONF_BASE_1_1 = "urn:ietf:params:netconf:base:1.1";
    public static final String URN_IETF_PARAMS_XML_NS_NETCONF_EXI_1_0 = "urn:ietf:params:xml:ns:netconf:exi:1.0";

    public static final String URN_IETF_PARAMS_NETCONF_CAPABILITY_EXI_1_0 = "urn:ietf:params:netconf:capability:exi:1.0";
    public static final String URN_IETF_PARAMS_XML_NS_YANG_IETF_NETCONF_MONITORING = "urn:ietf:params:xml:ns:yang:ietf-netconf-monitoring";
}
