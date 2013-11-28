package org.opendaylight.controller.sal.rest.impl;

import java.util.*;

import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;

class SchemaLocation {
    final private List<String> location = new ArrayList<>();
    final private DataSchemaNode schema;

    public SchemaLocation(DataSchemaNode schema) {
        this.schema = schema;
    }

    DataSchemaNode getSchema() {
        return schema;
    }

    List<String> getLocation() {
        return location;
    }

    SchemaLocation addPathPart(String partOfPath) {
        location.add(partOfPath);
        return this;
    }

}
