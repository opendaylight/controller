/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.yang.model.parser.util;

import java.util.ArrayList;
import java.util.List;

import org.opendaylight.controller.yang.model.api.MustDefinition;
import org.opendaylight.controller.yang.model.parser.builder.api.Builder;
import org.opendaylight.controller.yang.model.parser.builder.impl.UnknownSchemaNodeBuilder;

public final class RefineHolder implements Builder {
    private final String name;
    private final int line;
    private String defaultStr;
    private String description;
    private String reference;
    private Boolean config;
    private Boolean mandatory;
    private Boolean presence;
    private MustDefinition must;
    private Integer minElements;
    private Integer maxElements;
    private final List<UnknownSchemaNodeBuilder> addedUnknownNodes = new ArrayList<UnknownSchemaNodeBuilder>();

    public RefineHolder(final String name, final int line) {
        this.name = name;
        this.line = line;
    }

    @Override
    public int getLine() {
        return line;
    }

    public String getDefaultStr() {
        return defaultStr;
    }

    public void setDefaultStr(final String defaultStr) {
        this.defaultStr = defaultStr;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(final String reference) {
        this.reference = reference;
    }

    public Boolean isConfig() {
        return config;
    }

    public void setConfig(final Boolean config) {
        this.config = config;
    }

    public Boolean isMandatory() {
        return mandatory;
    }

    public void setMandatory(Boolean mandatory) {
        this.mandatory = mandatory;
    }

    public Boolean isPresence() {
        return presence;
    }

    public void setPresence(Boolean presence) {
        this.presence = presence;
    }

    public MustDefinition getMust() {
        return must;
    }

    public void setMust(MustDefinition must) {
        this.must = must;
    }

    public Integer getMinElements() {
        return minElements;
    }

    public void setMinElements(Integer minElements) {
        this.minElements = minElements;
    }

    public Integer getMaxElements() {
        return maxElements;
    }

    public void setMaxElements(Integer maxElements) {
        this.maxElements = maxElements;
    }

    public String getName() {
        return name;
    }

    public List<UnknownSchemaNodeBuilder> getUnknownNodes() {
        return addedUnknownNodes;
    }

    public void addUnknownSchemaNode(UnknownSchemaNodeBuilder unknownNode) {
        addedUnknownNodes.add(unknownNode);
    }

    @Override
    public Object build() {
        return null;
    }

}
