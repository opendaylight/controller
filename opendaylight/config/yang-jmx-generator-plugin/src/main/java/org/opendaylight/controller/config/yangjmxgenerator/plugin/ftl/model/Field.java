/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yangjmxgenerator.plugin.ftl.model;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.Lists;
import java.util.List;

public class Field {
    private final String type;
    private final String name;
    private final String definition;
    private final List<String> modifiers;
    private final boolean needsDepResolver;

    public Field(String type, String name) {
        this(Lists.<String> newArrayList(), type, name, null, false);
    }

    public Field(String type, String name, String definition) {
        this(Lists.<String> newArrayList(), type, name, definition, false);
    }

    public Field(List<String> modifiers, String type, String name) {
        this(modifiers, type, name, null, false);
    }

    public Field(List<String> modifiers, String type, String name,
            String definition) {
        this(modifiers, type, name, definition, false);
    }

    public Field(List<String> modifiers, String type, String name,
            String nullableDefinition, boolean needsDepResolver) {
        this.modifiers = checkNotNull(modifiers);
        this.type = checkNotNull(type);
        this.name = checkNotNull(name);
        this.definition = nullableDefinition;
        this.needsDepResolver = needsDepResolver;
    }

    public Field(String type, String name, String definition, boolean needsDepResolver) {
        this(Lists.<String> newArrayList(), type, name, definition, needsDepResolver);
    }

    public boolean isNeedsDepResolver() {
        return needsDepResolver;
    }

    public String getType() {
        return type;
    }

    public String getGenericInnerType() {
        return type.substring(type.indexOf("<") + 1, type.indexOf(">"));
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

    public boolean isArray() {
        return type.endsWith("[]");
    }

    @Override
    public String toString() {
        return FieldSerializer.toString(this);
    }
}
