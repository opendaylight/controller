package org.opendaylight.controller.sal.restconf.impl;

import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.model.api.*;

public class StructuredData {

    private final CompositeNode data;
    private final DataSchemaNode schema;
    private final SchemaContext schemaContext;

    public StructuredData(CompositeNode data, DataSchemaNode schema, SchemaContext schemaContext) {
        this.data = data;
        this.schema = schema;
        this.schemaContext = schemaContext;
    }

    public CompositeNode getData() {
        return data;
    }

    public DataSchemaNode getSchema() {
        return schema;
    }

    public SchemaContext getSchemaContext() {
        return schemaContext;
    }
}
