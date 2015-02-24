/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.restconf.impl;

import org.opendaylight.controller.md.sal.dom.api.DOMMountPoint;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;

/**
 * @deprecated class will be removed in Lithium release
 */
@Deprecated
public class StructuredData {

    private final CompositeNode data;
    private final DataSchemaNode schema;
    private final DOMMountPoint mountPoint;
    private final boolean prettyPrintMode;

    public StructuredData(final CompositeNode data, final DataSchemaNode schema, final DOMMountPoint mountPoint) {
        this(data, schema, mountPoint, false);
    }

    public StructuredData(final CompositeNode data, final DataSchemaNode schema, final DOMMountPoint mountPoint,
            final boolean preattyPrintMode) {
        this.data = data;
        this.schema = schema;
        this.mountPoint = mountPoint;
        prettyPrintMode = preattyPrintMode;
    }

    public CompositeNode getData() {
        return data;
    }

    public DataSchemaNode getSchema() {
        return schema;
    }

    public DOMMountPoint getMountPoint() {
        return mountPoint;
    }

    public boolean isPrettyPrintMode() {
        return prettyPrintMode;
    }
}
