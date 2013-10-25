package org.opendaylight.controller.sal.restconf.impl;

import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;

public class InstanceIdWithSchemaNode {

    private final InstanceIdentifier instanceIdentifier;
    private final DataSchemaNode schemaNode;

    public InstanceIdWithSchemaNode(InstanceIdentifier instanceIdentifier, DataSchemaNode schemaNode) {
        this.instanceIdentifier = instanceIdentifier;
        this.schemaNode = schemaNode;
    }

    public InstanceIdentifier getInstanceIdentifier() {
        return instanceIdentifier;
    }

    public DataSchemaNode getSchemaNode() {
        return schemaNode;
    }

}
