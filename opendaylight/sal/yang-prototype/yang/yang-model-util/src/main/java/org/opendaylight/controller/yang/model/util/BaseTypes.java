/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.yang.model.util;

import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.opendaylight.controller.yang.common.QName;
import org.opendaylight.controller.yang.model.api.SchemaPath;

public final class BaseTypes {

    private BaseTypes() {}

    public static final URI BaseTypesNamespace = URI
            .create("urn:ietf:params:xml:ns:yang:1");

    /**
     * Construct QName for Built-in base Yang type. The namespace for
     * built-in base yang types is defined as: urn:ietf:params:xml:ns:yang:1
     * 
     * @param typeName yang type name
     * @return built-in base yang type QName.
     */
    public static final QName constructQName(final String typeName) {
        return new QName(BaseTypesNamespace, typeName);
    }

    /**
     * Creates Schema Path from Qname.
     * 
     * @param typeName yang type QName
     * @return Schema Path from Qname.
     */
    public static final SchemaPath schemaPath(final QName typeName) {
        final List<QName> pathList = new ArrayList<QName>();
        pathList.add(typeName);
        return new SchemaPath(pathList, true);
    }
    
    /**
     * Creates Schema Path from List of partial paths defined as Strings, module Namespace and
     * module latest Revision Date.
     * 
     * @param actualPath List of partial paths
     * @param namespace Module Namespace
     * @param revision Revision Date
     * @return Schema Path
     */
    public static final SchemaPath schemaPath(final List<String> actualPath, final URI namespace, final Date revision) {
        if (actualPath == null) {
            throw new IllegalArgumentException("The actual path List MUST be specified.");
        }
        final List<QName> pathList = new ArrayList<QName>();
        for (final String path : actualPath) {
            final QName qname = new QName(namespace, revision, path);
            if (qname != null) {
                pathList.add(qname);
            }
        }
        return new SchemaPath(pathList, true);
    }
}
