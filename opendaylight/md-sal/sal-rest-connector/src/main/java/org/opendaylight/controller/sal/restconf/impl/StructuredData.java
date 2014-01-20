package org.opendaylight.controller.sal.restconf.impl;

import org.opendaylight.controller.sal.core.api.mount.MountInstance;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.model.api.*;

public class StructuredData {

    private final CompositeNode data;
    private final DataSchemaNode schema;
    private final MountInstance mountPoint;

    public StructuredData(CompositeNode data, DataSchemaNode schema, MountInstance mountPoint) {
        this.data = data;
        this.schema = schema;
        this.mountPoint = mountPoint;
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
}
