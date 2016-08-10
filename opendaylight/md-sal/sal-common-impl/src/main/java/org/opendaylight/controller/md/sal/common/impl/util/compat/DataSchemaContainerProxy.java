/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.common.impl.util.compat;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.model.api.DataNodeContainer;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.GroupingDefinition;
import org.opendaylight.yangtools.yang.model.api.TypeDefinition;
import org.opendaylight.yangtools.yang.model.api.UsesNode;

class DataSchemaContainerProxy implements DataNodeContainer {

    private final Set<DataSchemaNode> realChildSchemas;
    private final Map<QName, DataSchemaNode> mappedChildSchemas;

    public DataSchemaContainerProxy(final Set<DataSchemaNode> realChildSchema) {
        realChildSchemas = realChildSchema;
        mappedChildSchemas = new HashMap<QName, DataSchemaNode>();
        for(DataSchemaNode schema : realChildSchemas) {
            mappedChildSchemas.put(schema.getQName(),schema);
        }
    }

    @Override
    public DataSchemaNode getDataChildByName(final QName name) {
        return mappedChildSchemas.get(name);
    }

    @Override
    public Set<DataSchemaNode> getChildNodes() {
        return realChildSchemas;
    }

    @Override
    public Set<GroupingDefinition> getGroupings() {
        return Collections.emptySet();
    }

    @Override
    public Set<TypeDefinition<?>> getTypeDefinitions() {
        return Collections.emptySet();
    }

    @Override
    public Set<UsesNode> getUses() {
        return Collections.emptySet();
    }

}
