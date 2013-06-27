/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.yang.model.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.opendaylight.controller.yang.model.api.*;

public class DataNodeIterator implements Iterator<DataSchemaNode> {

    private final DataNodeContainer container;
    private final List<ListSchemaNode> allLists;
    private final List<ContainerSchemaNode> allContainers;
    private final List<ChoiceNode> allChoices;
    private final List<DataSchemaNode> allChilds;

    public DataNodeIterator(final DataNodeContainer container) {
        if (container == null) {
            throw new IllegalArgumentException("Data Node Container MUST be specified and cannot be NULL!");
        }

        this.allContainers = new ArrayList<>();
        this.allLists = new ArrayList<>();
        this.allChilds = new ArrayList<>();
        this.allChoices = new ArrayList<>();

        this.container = container;
        traverse(this.container);
    }

    public List<ContainerSchemaNode> allContainers() {
        return allContainers;
    }

    public List<ListSchemaNode> allLists() {
        return allLists;
    }

    public List<ChoiceNode> allChoices() {
        return allChoices;
    }

    private void traverse(final DataNodeContainer dataNode) {
        if (dataNode == null) {
            return;
        }

        final Set<DataSchemaNode> childNodes = dataNode.getChildNodes();
        if (childNodes != null) {
            for (DataSchemaNode childNode : childNodes) {
                if (childNode.isAugmenting()) {
                    continue;
                }
                allChilds.add(childNode);
                if (childNode instanceof ContainerSchemaNode) {
                    final ContainerSchemaNode container = (ContainerSchemaNode) childNode;
                    allContainers.add(container);
                    traverse(container);
                } else if (childNode instanceof ListSchemaNode) {
                    final ListSchemaNode list = (ListSchemaNode) childNode;
                    allLists.add(list);
                    traverse(list);
                } else if (childNode instanceof ChoiceNode) {
                    final ChoiceNode choiceNode = (ChoiceNode) childNode;
                    allChoices.add(choiceNode);
                    final Set<ChoiceCaseNode> cases = choiceNode.getCases();
                    if (cases != null) {
                        for (final ChoiceCaseNode caseNode : cases) {
                            traverse(caseNode);
                        }
                    }
                }
            }
        }

        final Set<GroupingDefinition> groupings = dataNode.getGroupings();
        if (groupings != null) {
            for (GroupingDefinition grouping : groupings) {
                traverse(grouping);
            }
        }
    }

    @Override
    public boolean hasNext() {
        if (container.getChildNodes() != null) {
            final Set<DataSchemaNode> childNodes = container.getChildNodes();

            if ((childNodes != null) && !childNodes.isEmpty()) {
                return childNodes.iterator().hasNext();
            }
        }
        return false;
    }

    @Override
    public DataSchemaNode next() {
        return allChilds.iterator().next();
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }
}
