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
import java.util.Optional;
import javax.lang.model.element.Modifier;

public class MethodDeclaration implements Method {
    private final String returnType;
    private final String name;
    private final List<Field> parameters;
    private String javadoc = null;
    private final List<Annotation> annotations;

    public MethodDeclaration(String returnType, String name,
            List<Field> parameters) {
        this(returnType, name, parameters, Collections.emptyList());
    }

    public MethodDeclaration(String returnType, String name,
            List<Field> parameters, List<Annotation> annotations) {
        this.returnType = returnType;
        this.name = name;
        this.parameters = parameters;
        this.annotations = annotations;
    }

    @Override
    public List<Annotation> getAnnotations() {
        return annotations;
    }

    @Override
    public List<String> getThrowsExceptions() {
        return Collections.emptyList();
    }

    @Override
    public Optional<String> getBody() {
        return Optional.empty();
    }

    @Override
    public String getJavadoc() {
        return javadoc;
    }

    public void setJavadoc(String javadoc) {
        this.javadoc = javadoc;
    }

    @Override
    public Optional<Modifier> getVisibility() {
        return Optional.empty();
    }

    @Override
    public String getReturnType() {
        return returnType;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public List<Field> getParameters() {
        return parameters;
    }

    @Override
    public List<Modifier> getModifiers() {
        return Collections.emptyList();
    }

    @Override
    public String toString() {
        return MethodSerializer.toString(this);
    }
}
