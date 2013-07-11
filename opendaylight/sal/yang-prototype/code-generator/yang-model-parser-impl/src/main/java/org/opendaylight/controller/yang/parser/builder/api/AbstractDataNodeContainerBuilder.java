/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.yang.parser.builder.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.opendaylight.controller.yang.common.QName;
import org.opendaylight.controller.yang.model.api.DataSchemaNode;
import org.opendaylight.controller.yang.model.api.GroupingDefinition;
import org.opendaylight.controller.yang.model.api.UnknownSchemaNode;
import org.opendaylight.controller.yang.parser.builder.impl.UnknownSchemaNodeBuilder;
import org.opendaylight.controller.yang.parser.util.YangParseException;

/**
 * Basic implementation of DataNodeContainerBuilder.
 */
public abstract class AbstractDataNodeContainerBuilder implements DataNodeContainerBuilder {
    protected final int line;
    protected final QName qname;
    protected Builder parent;

    protected Set<DataSchemaNode> childNodes;
    protected final Set<DataSchemaNodeBuilder> addedChildNodes = new HashSet<DataSchemaNodeBuilder>();

    protected Set<GroupingDefinition> groupings;
    protected final Set<GroupingBuilder> addedGroupings = new HashSet<GroupingBuilder>();

    protected List<UnknownSchemaNode> unknownNodes;
    protected final List<UnknownSchemaNodeBuilder> addedUnknownNodes = new ArrayList<UnknownSchemaNodeBuilder>();

    protected AbstractDataNodeContainerBuilder(final int line, final QName qname) {
        this.line = line;
        this.qname = qname;
    }

    @Override
    public int getLine() {
        return line;
    }

    @Override
    public Builder getParent() {
        return parent;
    }

    @Override
    public void setParent(final Builder parent) {
        this.parent = parent;
    }

    @Override
    public QName getQName() {
        return qname;
    }

    @Override
    public Set<DataSchemaNode> getChildNodes() {
        if (childNodes == null) {
            return Collections.emptySet();
        }
        return childNodes;
    }

    public void setChildNodes(Set<DataSchemaNode> childNodes) {
        this.childNodes = childNodes;
    }

    @Override
    public Set<DataSchemaNodeBuilder> getChildNodeBuilders() {
        return addedChildNodes;
    }

    @Override
    public DataSchemaNodeBuilder getDataChildByName(final String name) {
        for (DataSchemaNodeBuilder child : addedChildNodes) {
            if (child.getQName().getLocalName().equals(name)) {
                return child;
            }
        }
        return null;
    }

    @Override
    public void addChildNode(DataSchemaNodeBuilder child) {
        for (DataSchemaNodeBuilder childNode : addedChildNodes) {
            if (childNode.getQName().getLocalName().equals(child.getQName().getLocalName())) {
                throw new YangParseException(child.getLine(), "Duplicate node found at line " + childNode.getLine());
            }
        }
        addedChildNodes.add(child);
    }

    @Override
    public Set<GroupingDefinition> getGroupings() {
        if (groupings == null) {
            return Collections.emptySet();
        }
        return groupings;
    }

    public void setGroupings(final Set<GroupingDefinition> groupings) {
        this.groupings = groupings;
    }

    public Set<GroupingBuilder> getGroupingBuilders() {
        return addedGroupings;
    }

    @Override
    public void addGrouping(GroupingBuilder groupingBuilder) {
        for (GroupingBuilder gb : addedGroupings) {
            if (gb.getQName().getLocalName().equals(groupingBuilder.getQName().getLocalName())) {
                throw new YangParseException(groupingBuilder.getLine(), "Duplicate node found at line " + gb.getLine());
            }
        }
        addedGroupings.add(groupingBuilder);
    }

    @Override
    public List<UnknownSchemaNodeBuilder> getUnknownNodeBuilders() {
        return addedUnknownNodes;
    }

    @Override
    public void addUnknownNodeBuilder(UnknownSchemaNodeBuilder unknownNode) {
        addedUnknownNodes.add(unknownNode);
    }

    public void setUnknownNodes(List<UnknownSchemaNode> unknownNodes) {
        this.unknownNodes = unknownNodes;
    }

}
