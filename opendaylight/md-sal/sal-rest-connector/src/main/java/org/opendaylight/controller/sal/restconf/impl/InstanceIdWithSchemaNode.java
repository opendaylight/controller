package org.opendaylight.controller.sal.restconf.impl;

import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;

public class InstanceIdWithSchemaNode {

    private final InstanceIdentifier instanceIdentifier;
    private final DataSchemaNode schemaNode;
    private final InstanceIdentifier mountPoint;

    public InstanceIdWithSchemaNode(InstanceIdentifier instanceIdentifier, DataSchemaNode schemaNode, InstanceIdentifier mountPoint) {
        this.instanceIdentifier = instanceIdentifier;
        this.schemaNode = schemaNode;
        this.mountPoint = mountPoint;
    }

    public InstanceIdentifier getInstanceIdentifier() {
        return instanceIdentifier;
    }

    public DataSchemaNode getSchemaNode() {
        return schemaNode;
    }

    public InstanceIdentifier getMountPoint() {
        return mountPoint;
    }

}
