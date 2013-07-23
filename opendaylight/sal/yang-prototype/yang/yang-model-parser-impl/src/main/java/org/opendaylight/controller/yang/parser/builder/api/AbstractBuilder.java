/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.yang.parser.builder.api;

import java.util.ArrayList;
import java.util.List;

import org.opendaylight.controller.yang.model.api.UnknownSchemaNode;
import org.opendaylight.controller.yang.parser.builder.impl.UnknownSchemaNodeBuilder;

/**
 * Basic implementation of Builder.
 */
public abstract class AbstractBuilder implements Builder {
    protected String moduleName;
    protected final int line;
    protected Builder parent;

    protected List<UnknownSchemaNode> unknownNodes;
    protected final List<UnknownSchemaNodeBuilder> addedUnknownNodes = new ArrayList<UnknownSchemaNodeBuilder>();

    protected AbstractBuilder(final String moduleName, final int line) {
        this.moduleName = moduleName;
        this.line = line;
    }

    @Override
    public String getModuleName() {
        return moduleName;
    }

    @Override
    public void setModuleName(final String moduleName) {
        this.moduleName = moduleName;
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
