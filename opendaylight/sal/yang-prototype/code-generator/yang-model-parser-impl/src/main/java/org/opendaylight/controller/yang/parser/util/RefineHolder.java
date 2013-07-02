/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.yang.parser.util;

import java.util.ArrayList;
import java.util.List;

import org.opendaylight.controller.yang.model.api.MustDefinition;
import org.opendaylight.controller.yang.parser.builder.api.Builder;
import org.opendaylight.controller.yang.parser.builder.api.ConfigNode;
import org.opendaylight.controller.yang.parser.builder.impl.UnknownSchemaNodeBuilder;

public final class RefineHolder implements Builder, ConfigNode {
    private Builder parent;
    private final int line;
    private final String name;
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

    public RefineHolder(final int line, final String name) {
        this.name = name;
        this.line = line;
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

    @Override
    public Boolean isConfiguration() {
        return config;
    }

    @Override
    public void setConfiguration(final Boolean config) {
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

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((addedUnknownNodes == null) ? 0 : addedUnknownNodes.hashCode());
        result = prime * result + ((config == null) ? 0 : config.hashCode());
        result = prime * result + ((defaultStr == null) ? 0 : defaultStr.hashCode());
        result = prime * result + ((description == null) ? 0 : description.hashCode());
        result = prime * result + ((mandatory == null) ? 0 : mandatory.hashCode());
        result = prime * result + ((maxElements == null) ? 0 : maxElements.hashCode());
        result = prime * result + ((minElements == null) ? 0 : minElements.hashCode());
        result = prime * result + ((must == null) ? 0 : must.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((parent == null) ? 0 : parent.hashCode());
        result = prime * result + ((presence == null) ? 0 : presence.hashCode());
        result = prime * result + ((reference == null) ? 0 : reference.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        RefineHolder other = (RefineHolder) obj;
        if (addedUnknownNodes == null) {
            if (other.addedUnknownNodes != null)
                return false;
        } else if (!addedUnknownNodes.equals(other.addedUnknownNodes))
            return false;
        if (config == null) {
            if (other.config != null)
                return false;
        } else if (!config.equals(other.config))
            return false;
        if (defaultStr == null) {
            if (other.defaultStr != null)
                return false;
        } else if (!defaultStr.equals(other.defaultStr))
            return false;
        if (description == null) {
            if (other.description != null)
                return false;
        } else if (!description.equals(other.description))
            return false;
        if (mandatory == null) {
            if (other.mandatory != null)
                return false;
        } else if (!mandatory.equals(other.mandatory))
            return false;
        if (maxElements == null) {
            if (other.maxElements != null)
                return false;
        } else if (!maxElements.equals(other.maxElements))
            return false;
        if (minElements == null) {
            if (other.minElements != null)
                return false;
        } else if (!minElements.equals(other.minElements))
            return false;
        if (must == null) {
            if (other.must != null)
                return false;
        } else if (!must.equals(other.must))
            return false;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        if (parent == null) {
            if (other.parent != null)
                return false;
        } else if (!parent.equals(other.parent))
            return false;
        if (presence == null) {
            if (other.presence != null)
                return false;
        } else if (!presence.equals(other.presence))
            return false;
        if (reference == null) {
            if (other.reference != null)
                return false;
        } else if (!reference.equals(other.reference))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "revine " + name;
    }

}
