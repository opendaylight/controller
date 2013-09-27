/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yangjmxgenerator;

import java.net.URI;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.opendaylight.yangtools.yang.common.QName;

public class ConfigConstants {

    public static final String CONFIG_NAMESPACE = "urn:opendaylight:params:xml:ns:yang:controller:config";

    public static final String CONFIG_MODULE = "config";
    public static final String CONFIG_THREADS_MODULE = "config-threads";
    public static final String IETF_INET_TYPES = "ietf-inet-types";

    public static final QName SERVICE_TYPE_Q_NAME = createConfigQName("service-type");
    public static final QName MODULE_TYPE_Q_NAME = createConfigQName("module-type");
    public static final QName JAVA_CLASS_EXTENSION_QNAME = createConfigQName("java-class");
    public static final QName REQUIRED_IDENTITY_EXTENSION_QNAME = createConfigQName("required-identity");
    public static final QName INNER_STATE_BEAN_EXTENSION_QNAME = createConfigQName("inner-state-bean");
    public static final QName PROVIDED_SERVICE_EXTENSION_QNAME = createConfigQName("provided-service");
    public static final QName JAVA_NAME_PREFIX_EXTENSION_QNAME = createConfigQName("java-name-prefix");
    public static final QName RPC_CONTEXT_REF_GROUPING_QNAME = createRpcXQName("rpc-context-ref");
    public static final QName RPC_CONTEXT_REF_GROUPING_LEAF = createRpcXQName("context-instance");
    public static final QName RPC_CONTEXT_INSTANCE_EXTENSION_QNAME = createRpcXQName("rpc-context-instance");

    public static QName createConfigQName(String localName) {
        return createQName(CONFIG_NAMESPACE, "2013-04-05", localName);
    }

    public static QName createRpcXQName(String localName) {
        return createQName("urn:ietf:params:xml:ns:yang:rpc-context",
                "2013-06-17", localName);
    }

    /**
     *
     * @param uri
     * @param revisionDate
     *            in format yyyy-MM-dd
     * @param localName
     * @return
     */
    private static QName createQName(String uri, String revisionDate,
            String localName) {
        SimpleDateFormat revisionFormat = new SimpleDateFormat("yyyy-MM-dd");
        Date revision;
        try {
            revision = revisionFormat.parse(revisionDate);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
        return new QName(URI.create(uri), revision, localName);
    }

}
