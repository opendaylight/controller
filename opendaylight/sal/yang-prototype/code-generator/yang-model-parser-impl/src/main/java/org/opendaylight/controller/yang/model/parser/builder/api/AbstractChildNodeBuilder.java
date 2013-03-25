/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.yang.model.parser.builder.api;

import java.util.HashSet;
import java.util.Set;

import org.opendaylight.controller.yang.common.QName;

public abstract class AbstractChildNodeBuilder implements ChildNodeBuilder {

    private final QName qname;
    protected final Set<DataSchemaNodeBuilder> childNodes = new HashSet<DataSchemaNodeBuilder>();
    protected final Set<GroupingBuilder> groupings = new HashSet<GroupingBuilder>();

    protected AbstractChildNodeBuilder(QName qname) {
        this.qname = qname;
    }

    @Override
    public QName getQName() {
        return qname;
    }

    @Override
    public void addChildNode(DataSchemaNodeBuilder childNode) {
        childNodes.add(childNode);
    }

    @Override
    public void addGrouping(GroupingBuilder grouping) {
        groupings.add(grouping);
    }

}
