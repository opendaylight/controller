package org.opendaylight.controller.sal.restconf.impl;

import org.opendaylight.controller.sal.core.api.mount.MountInstance;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;

public class InstanceIdWithSchemaNode {

    private final InstanceIdentifier instanceIdentifier;
    private final DataSchemaNode schemaNode;
    private final MountInstance mountPoint;

    public InstanceIdWithSchemaNode(InstanceIdentifier instanceIdentifier, DataSchemaNode schemaNode, MountInstance mountPoint) {
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

    public MountInstance getMountPoint() {
        return mountPoint;
    }

}
