/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.yang.model.parser.util;

import org.opendaylight.controller.yang.model.api.MustDefinition;

public final class RefineHolder {
    private final String name;
    private Refine type;
    private String defaultStr;
    private String description;
    private String reference;
    private Boolean config;
    private Boolean mandatory;
    private Boolean presence;
    private MustDefinition must;
    private Integer minElements;
    private Integer maxElements;

    public RefineHolder(final String name) {
        this.name = name;
    }

    public Refine getType() {
        return type;
    }

    public void setType(final Refine type) {
        this.type = type;
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

    public enum Refine {
        CONTAINER, LEAF, LIST, LEAF_LIST, CHOICE, ANYXML
    }

}
