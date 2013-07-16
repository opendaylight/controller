/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.yang.parser.builder.api;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.opendaylight.controller.yang.common.QName;
import org.opendaylight.controller.yang.model.api.DataSchemaNode;
import org.opendaylight.controller.yang.model.api.GroupingDefinition;
import org.opendaylight.controller.yang.parser.util.YangParseException;

/**
 * Basic implementation of DataNodeContainerBuilder.
 */
public abstract class AbstractDataNodeContainerBuilder extends AbstractBuilder implements DataNodeContainerBuilder {
    protected final QName qname;

    protected Set<DataSchemaNode> childNodes;
    protected final Set<DataSchemaNodeBuilder> addedChildNodes = new HashSet<DataSchemaNodeBuilder>();

    protected Set<GroupingDefinition> groupings;
    protected final Set<GroupingBuilder> addedGroupings = new HashSet<GroupingBuilder>();

    protected AbstractDataNodeContainerBuilder(final String moduleName, final int line, final QName qname) {
        super(moduleName, line);
        this.qname = qname;
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
        String childName = child.getQName().getLocalName();
        for (DataSchemaNodeBuilder addedChildNode : addedChildNodes) {
            if (addedChildNode.getQName().getLocalName().equals(childName)) {
                throw new YangParseException(child.getModuleName(), child.getLine(), "Can not add '" + child
                        + "' to node '" + qname.getLocalName() + "' in module '" + moduleName
                        + "': node with same name already declared at line " + addedChildNode.getLine());
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
    public void addGrouping(GroupingBuilder grouping) {
        String groupingName = grouping.getQName().getLocalName();
        for (GroupingBuilder addedGrouping : addedGroupings) {
            if (addedGrouping.getQName().getLocalName().equals(groupingName)) {
                throw new YangParseException(grouping.getModuleName(), grouping.getLine(), "Can not add '" + grouping
                        + "': grouping with same name already declared in module '" + moduleName + "' at line "
                        + addedGrouping.getLine());
            }
        }
        addedGroupings.add(grouping);
    }

}
