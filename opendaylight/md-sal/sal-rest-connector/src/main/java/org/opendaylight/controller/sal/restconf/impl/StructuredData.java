/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.restconf.impl;

import org.opendaylight.controller.sal.core.api.mount.MountInstance;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;

public class StructuredData {

    private final CompositeNode data;
    private final DataSchemaNode schema;
    private final MountInstance mountPoint;
    private final boolean whiteCharsIncluded;

    public StructuredData(final CompositeNode data, final DataSchemaNode schema, final MountInstance mountPoint) {
        this(data, schema, mountPoint, true);
    }

    public StructuredData(final CompositeNode data, final DataSchemaNode schema, final MountInstance mountPoint,
            final boolean whiteCharsIncluded) {
        this.data = data;
        this.schema = schema;
        this.mountPoint = mountPoint;
        this.whiteCharsIncluded = whiteCharsIncluded;
    }

    public CompositeNode getData() {
        return data;
    }

    public DataSchemaNode getSchema() {
        return schema;
    }

    public MountInstance getMountPoint() {
        return mountPoint;
    }

    public boolean isWhiteCharsIncluded() {
        return whiteCharsIncluded;
    }
}
