/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yangjmxgenerator.plugin.ftl.model;

import java.util.List;

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

    @Override
    public String toString() {
        return "TypeDeclaration{" + "type='" + type + '\'' + ", name='" + name
                + '\'' + '}';
    }
}
