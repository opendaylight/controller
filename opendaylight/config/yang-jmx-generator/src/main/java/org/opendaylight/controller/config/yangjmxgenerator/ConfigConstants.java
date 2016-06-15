/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yangjmxgenerator;

import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.QNameModule;

public final class ConfigConstants {

    private ConfigConstants() {
    }

    private static final QName DUMMY_CONFIG_QNAME = QName.create(
        "urn:opendaylight:params:xml:ns:yang:controller:config", "2013-04-05", "dummy").intern();
    private static final QName DUMM_RPC_QNAME = QName.create(
        "urn:ietf:params:xml:ns:yang:rpc-context", "2013-06-17", "dummy").intern();
    private static final QNameModule CONFIG_MOD = DUMMY_CONFIG_QNAME.getModule();
    private static final QNameModule RPC_MOD = DUMM_RPC_QNAME.getModule();

    public static final String CONFIG_NAMESPACE = CONFIG_MOD.getNamespace().toString();
    public static final String CONFIG_MODULE = "config";
    public static final String CONFIG_THREADS_MODULE = "config-threads";
    public static final String IETF_INET_TYPES = "ietf-inet-types";

    public static final QName SERVICE_TYPE_Q_NAME = createConfigQName("service-type").intern();
    public static final QName MODULE_TYPE_Q_NAME = createConfigQName("module-type").intern();
    public static final QName JAVA_CLASS_EXTENSION_QNAME = createConfigQName("java-class").intern();
    public static final QName DISABLE_OSGI_SERVICE_REG_QNAME = createConfigQName("disable-osgi-service-registration").intern();
    public static final QName REQUIRED_IDENTITY_EXTENSION_QNAME = createConfigQName("required-identity").intern();
    public static final QName INNER_STATE_BEAN_EXTENSION_QNAME = createConfigQName("inner-state-bean").intern();
    public static final QName PROVIDED_SERVICE_EXTENSION_QNAME = createConfigQName("provided-service").intern();
    public static final QName JAVA_NAME_PREFIX_EXTENSION_QNAME = createConfigQName("java-name-prefix").intern();
    public static final QName RPC_CONTEXT_REF_GROUPING_QNAME = createRpcXQName("rpc-context-ref").intern();
    public static final QName RPC_CONTEXT_REF_GROUPING_LEAF = createRpcXQName("context-instance").intern();
    public static final QName RPC_CONTEXT_INSTANCE_EXTENSION_QNAME = createRpcXQName("rpc-context-instance").intern();

    public static QName createConfigQName(final String localName) {
        return QName.create(CONFIG_MOD, localName);
    }

    public static QName createRpcXQName(final String localName) {
        return QName.create(RPC_MOD, localName);
    }
}
