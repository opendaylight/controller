/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.yang.parser.builder.api;

import java.util.HashSet;
import java.util.Set;

import org.opendaylight.controller.yang.common.QName;
import org.opendaylight.controller.yang.model.api.DataSchemaNode;
import org.opendaylight.controller.yang.model.api.GroupingDefinition;

public abstract class AbstractDataNodeContainerBuilder implements DataNodeContainerBuilder {
    private final QName qname;

    protected Set<DataSchemaNode> childNodes;
    protected final Set<DataSchemaNodeBuilder> addedChildNodes = new HashSet<DataSchemaNodeBuilder>();

    protected Set<GroupingDefinition> groupings;
    protected final Set<GroupingBuilder> addedGroupings = new HashSet<GroupingBuilder>();

    protected AbstractDataNodeContainerBuilder(QName qname) {
        this.qname = qname;
    }

    @Override
    public QName getQName() {
        return qname;
    }

    @Override
    public Set<DataSchemaNode> getChildNodes() {
        return childNodes;
    }

    @Override
    public Set<DataSchemaNodeBuilder> getChildNodeBuilders() {
        return addedChildNodes;
    }

    @Override
    public void addChildNode(DataSchemaNodeBuilder childNode) {
        addedChildNodes.add(childNode);
    }

    public void setChildNodes(Set<DataSchemaNode> childNodes) {
        this.childNodes = childNodes;
    }

    @Override
    public Set<GroupingDefinition> getGroupings() {
        return groupings;
    }

    public Set<GroupingBuilder> getGroupingBuilders() {
        return addedGroupings;
    }

    @Override
    public void addGrouping(GroupingBuilder grouping) {
        addedGroupings.add(grouping);
    }

    public void setGroupings(final Set<GroupingDefinition> groupings) {
        this.groupings = groupings;
    }

}
