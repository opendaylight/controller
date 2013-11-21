/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.util.xml;

public class XmlNetconfConstants {

    public static final String MOUNTPOINTS = "mountpoints";
    public static final String MOUNTPOINT = "mountpoint";
    public static final String ID = "id";
    public static final String CAPABILITY = "capability";
    public static final String CAPABILITIES = "capabilities";
    public static final String COMMIT = "commit";
    public static final String TYPE_KEY = "type";
    public static final String MODULE_KEY = "module";
    public static final String INSTANCE_KEY = "instance";
    public static final String OPERATION_ATTR_KEY = "operation";
    public static final String SERVICES_KEY = "services";
    public static final String CONFIG_KEY = "config";
    public static final String MODULES_KEY = "modules";
    public static final String CONFIGURATION_KEY = "configuration";
    public static final String DATA_KEY = "data";
    public static final String OK = "ok";
    public static final String FILTER = "filter";
    public static final String SOURCE_KEY = "source";
    public static final String RPC_KEY = "rpc";
    public static final String RPC_REPLY_KEY = "rpc-reply";
    public static final String RPC_ERROR = "rpc-error";
    public static final String NAME_KEY = "name";
    public static final String NOTIFICATION_ELEMENT_NAME = "notification";

    public static final String PREFIX = "prefix";

    //
    //
    public static final String RFC4741_TARGET_NAMESPACE = "urn:ietf:params:xml:ns:netconf:base:1.0";
    public static final String URN_IETF_PARAMS_XML_NS_NETCONF_BASE_1_0 = "urn:ietf:params:xml:ns:netconf:base:1.0";

    public static final String URN_IETF_PARAMS_XML_NS_YANG_IETF_NETCONF_MONITORING = "urn:ietf:params:xml:ns:yang:ietf-netconf-monitoring";
    // TODO where to store namespace of config ?
    public static final String URN_OPENDAYLIGHT_PARAMS_XML_NS_YANG_CONTROLLER_CONFIG = "urn:opendaylight:params:xml:ns:yang:controller:config";
}
