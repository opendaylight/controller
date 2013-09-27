/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yangjmxgenerator.plugin.ftl.model;

import java.util.List;

import com.google.common.collect.Lists;

public class Field {
    private final String type;
    private final String name;
    private final String definition;
    private final List<String> modifiers;

    public Field(String type, String name) {
        this(Lists.<String> newArrayList(), type, name, null);
    }

    public Field(List<String> modifiers, String type, String name) {
        this(modifiers, type, name, null);
    }

    public Field(List<String> modifiers, String type, String name,
            String definition) {
        this.modifiers = modifiers;
        this.type = type;
        this.name = name;
        this.definition = definition;
    }

    public String getType() {
        return type;
    }

    public List<String> getModifiers() {
        return modifiers;
    }

    public String getName() {
        return name;
    }

    public String getDefinition() {
        return definition;
    }
}
