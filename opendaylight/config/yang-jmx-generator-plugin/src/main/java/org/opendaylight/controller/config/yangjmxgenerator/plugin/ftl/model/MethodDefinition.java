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

public class MethodDefinition implements Method {
    private final List<Modifier> modifiers;
    private final String returnType;
    private final String name;
    private final List<Field> parameters;
    private final List<String> throwsExceptions;
    private final String body;
    private String javadoc = null;
    private final List<Annotation> annotations;

    // TODO remove, Constructor is in separate class
    public static MethodDefinition createConstructor(String name,
            List<Field> parameters, String body) {
        return new MethodDefinition("", name, parameters, body);

    }

    public MethodDefinition(String returnType, String name,
            List<Field> parameters, String body) {
        this(Collections.emptyList(), returnType, name, parameters,
                Collections.emptyList(), Collections.emptyList(), body);
    }

    public MethodDefinition(String returnType, String name,
            List<Field> parameters, List<Annotation> annotations, String body) {
        this(Collections.emptyList(), returnType, name, parameters,
                Collections.emptyList(), annotations, body);
    }

    public MethodDefinition(List<Modifier> modifiers, String returnType,
            String name, List<Field> parameters, List<String> throwsExceptions,
            List<Annotation> annotations, String body) {
        this.modifiers = modifiers;
        this.returnType = returnType;
        this.name = name;
        this.parameters = parameters;
        this.throwsExceptions = throwsExceptions;
        this.body = body;
        this.annotations = annotations;
    }

    @Override
    public List<Annotation> getAnnotations() {
        return annotations;
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
        return Optional.of(Modifier.PUBLIC);
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
    public List<String> getThrowsExceptions() {
        return throwsExceptions;
    }

    @Override
    public Optional<String> getBody() {
        return Optional.of(body);
    }

    @Override
    public List<Modifier> getModifiers() {
        return modifiers;
    }

    @Override
    public String toString() {
        return MethodSerializer.toString(this);
    }
}
