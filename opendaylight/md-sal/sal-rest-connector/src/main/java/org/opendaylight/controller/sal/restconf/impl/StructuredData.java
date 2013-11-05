package org.opendaylight.controller.sal.restconf.impl;

import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;

public class StructuredData {
    
    private final CompositeNode data;
    private final DataSchemaNode schema;
    
    public StructuredData(CompositeNode data, DataSchemaNode schema) {
        this.data = data;
        this.schema = schema;
    }

    public CompositeNode getData() {
        return data;
    }

    public DataSchemaNode getSchema() {
        return schema;
    }
    
}
