/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.yangjmxgenerator.plugin.java;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Optional;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.opendaylight.controller.config.yangjmxgenerator.plugin.ftl.model.Annotation;

public class JavaFileInputBuilder {

    private Optional<String> copyright = Optional.absent(), header = Optional.absent(), classJavaDoc = Optional.absent();

    private TypeName typeName = TypeName.classType;

    private FullyQualifiedName fqn;

    private final List<String> classAnnotations = new ArrayList<>();

    private final List<FullyQualifiedName> extendsFQNs = new ArrayList<>();

    private final List<FullyQualifiedName> implementsFQNs = new ArrayList<>();

    private final List<String> bodyElements = new ArrayList<>();

    public void addToBody(String element) {
        bodyElements.add(element + "\n");
    }

    public void addClassAnnotation(Annotation annotation) {
        addClassAnnotation(annotation.toString());
    }

    public void addClassAnnotation(String annotation) {
        classAnnotations.add(checkNotNull(annotation));
    }

    public void addExtendsFQN(FullyQualifiedName fqn) {
        extendsFQNs.add(fqn);
    }

    public void addImplementsFQN(FullyQualifiedName fqn) {
        implementsFQNs.add(fqn);
    }

    public Optional<String> getCopyright() {
        return copyright;
    }

    public void setCopyright(Optional<String> copyright) {
        this.copyright = checkNotNull(copyright);
    }

    public Optional<String> getHeader() {
        return header;
    }

    public void setHeader(Optional<String> header) {
        this.header = checkNotNull(header);
    }


    public Optional<String> getClassJavaDoc() {
        return classJavaDoc;
    }

    public void setClassJavaDoc(Optional<String> classJavaDoc) {
        this.classJavaDoc = checkNotNull(classJavaDoc);
    }


    public FullyQualifiedName getFqn() {
        return fqn;
    }

    public void setFqn(FullyQualifiedName fqn) {
        this.fqn = fqn;
    }

    public List<FullyQualifiedName> getExtendsFQNs() {
        return extendsFQNs;
    }


    public List<FullyQualifiedName> getImplementsFQNs() {
        return implementsFQNs;
    }


    public TypeName getTypeName() {
        return typeName;
    }

    public void setTypeName(TypeName typeName) {
        this.typeName = typeName;
    }


    public JavaFileInput build() {
        checkNotNull(copyright);
        checkNotNull(header);
        checkNotNull(classJavaDoc);
        checkNotNull(typeName);
        checkNotNull(fqn);

        return new JavaFileInput() {

            @Override
            public FullyQualifiedName getFQN() {
                return fqn;
            }

            @Override
            public Optional<String> getCopyright() {
                return copyright;
            }

            @Override
            public Optional<String> getHeader() {
                return header;
            }

            @Override
            public Optional<String> getClassJavaDoc() {
                return classJavaDoc;
            }

            @Override
            public TypeName getType() {
                return typeName;
            }

            @Override
            public List<FullyQualifiedName> getExtends() {
                return Collections.unmodifiableList(extendsFQNs);
            }

            @Override
            public List<FullyQualifiedName> getImplements() {
                return Collections.unmodifiableList(implementsFQNs);
            }

            @Override
            public List<String> getClassAnnotations() {
                return Collections.unmodifiableList(classAnnotations);
            }

            @Override
            public List<String> getBodyElements() {
                return Collections.unmodifiableList(bodyElements);
            }
        };
    }
}
