/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yangjmxgenerator.plugin.ftl.model;

import java.util.Collections;
import java.util.List;

import org.opendaylight.controller.config.yangjmxgenerator.attribute.DependencyAttribute.Dependency;

public class ModuleField extends Field {

    private final String nullableDefault, attributeName;
    private final boolean dependent;
    private final Dependency dependency;

    public ModuleField(List<String> modifiers, String type, String name,
            String attributeName, String nullableDefault, boolean isDependency,
            Dependency dependency) {
        super(modifiers, type, name);
        this.nullableDefault = nullableDefault;
        this.dependent = isDependency;
        this.dependency = dependency;
        this.attributeName = attributeName;
    }

    public ModuleField(String type, String name, String attributeName,
            String nullableDefault, boolean isDependency, Dependency dependency) {
        this(Collections.<String> emptyList(), type, name, attributeName,
                nullableDefault, isDependency, dependency);
    }

    public Dependency getDependency() {
        return dependency;
    }

    public String getNullableDefault() {
        return nullableDefault;
    }

    public boolean isDependent() {
        return dependent;
    }

    public String getAttributeName() {
        return attributeName;
    }
}
