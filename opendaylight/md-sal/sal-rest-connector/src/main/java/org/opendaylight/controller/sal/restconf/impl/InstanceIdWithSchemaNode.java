/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.restconf.impl;

import org.opendaylight.controller.md.sal.dom.api.DOMMountPoint;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;

public class InstanceIdWithSchemaNode {

    private final YangInstanceIdentifier instanceIdentifier;
    private final DataSchemaNode schemaNode;
    private final DOMMountPoint mountPoint;

    public InstanceIdWithSchemaNode(YangInstanceIdentifier instanceIdentifier, DataSchemaNode schemaNode,
            DOMMountPoint mountPoint) {
        this.instanceIdentifier = instanceIdentifier;
        this.schemaNode = schemaNode;
        this.mountPoint = mountPoint;
    }

    public YangInstanceIdentifier getInstanceIdentifier() {
        return instanceIdentifier;
    }

    public DataSchemaNode getSchemaNode() {
        return schemaNode;
    }

    public DOMMountPoint getMountPoint() {
        return mountPoint;
    }

}
