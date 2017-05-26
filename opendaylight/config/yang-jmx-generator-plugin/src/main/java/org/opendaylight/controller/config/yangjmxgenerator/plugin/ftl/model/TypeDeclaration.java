/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yangjmxgenerator.plugin.ftl.model;

import java.util.List;
import org.opendaylight.controller.config.yangjmxgenerator.plugin.java.TypeName;

public class TypeDeclaration {
    private final String type, name;
    private final List<String> extended, implemented;
    private final boolean isAbstract, isFinal;

    public TypeDeclaration(String type, String name, List<String> extended,
            List<String> implemented, boolean isAbstract, boolean isFinal) {
        super();
        this.type = type;
        this.name = name;
        this.extended = extended;
        this.implemented = implemented;
        this.isAbstract = isAbstract;
        this.isFinal = isFinal;
    }

    public TypeDeclaration(String type, String name, List<String> extended,
            List<String> implemented) {
        this(type, name, extended, implemented, false, false);
    }

    public boolean isAbstract() {
        return isAbstract;
    }

    public boolean isFinal() {
        return isFinal;
    }

    public String getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public List<String> getExtended() {
        return extended;
    }

    public List<String> getImplemented() {
        return implemented;
    }

    public TypeName toTypeName() {
        if ("interface".equals(type)) {
            return TypeName.interfaceType;
        } else if ("class".equals(type)) {
            if (isAbstract) {
                return TypeName.absClassType;
            } else if (isFinal) {
                return TypeName.finalClassType;
            } else {
                return TypeName.classType;
            }
        } else if ("enum".equals(type)) {
            return TypeName.enumType;
        } else {
            throw new IllegalStateException("Type not supported: " + type);
        }
    }

    @Override
    public String toString() {
        return "TypeDeclaration{" + "type='" + type + '\'' + ", name='" + name
                + '\'' + '}';
    }
}
