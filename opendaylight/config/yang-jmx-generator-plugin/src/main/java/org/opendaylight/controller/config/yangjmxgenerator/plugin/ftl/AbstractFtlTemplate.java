/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yangjmxgenerator.plugin.ftl;

import java.io.File;
import java.util.List;

import org.opendaylight.controller.config.yangjmxgenerator.plugin.ftl.model.Annotation;
import org.opendaylight.controller.config.yangjmxgenerator.plugin.ftl.model.Field;
import org.opendaylight.controller.config.yangjmxgenerator.plugin.ftl.model.Header;
import org.opendaylight.controller.config.yangjmxgenerator.plugin.ftl.model.Method;
import org.opendaylight.controller.config.yangjmxgenerator.plugin.ftl.model.TypeDeclaration;
import org.opendaylight.controller.config.yangjmxgenerator.plugin.util.FullyQualifiedNameHelper;

import com.google.common.collect.Lists;

public abstract class AbstractFtlTemplate implements FtlTemplate {
    private final String packageName;
    private final List<Field> fields;
    private final List<Annotation> annotations;
    private final List<? extends Method> methods;
    private String javadoc = null;
    private final TypeDeclaration typeDeclaration;
    private final Header header;

    protected AbstractFtlTemplate(Header header, String packageName,
            List<Field> fields, List<? extends Method> methods,
            TypeDeclaration typeDeclaration) {
        this.packageName = packageName;
        this.fields = fields;
        this.methods = methods;
        this.annotations = Lists.newArrayList();
        this.typeDeclaration = typeDeclaration;
        this.header = header;
    }

    @Override
    public Header getHeader() {
        return header;
    }

    @Override
    public String getFullyQualifiedName() {
        return FullyQualifiedNameHelper.getFullyQualifiedName(getPackageName(),
                getTypeDeclaration().getName());
    }

    @Override
    public String getPackageName() {
        return packageName;
    }

    @Override
    public TypeDeclaration getTypeDeclaration() {
        return typeDeclaration;
    }

    @Override
    public String getJavadoc() {
        return javadoc;
    }

    public void setJavadoc(String javadoc) {
        this.javadoc = javadoc;
    }

    @Override
    public List<Annotation> getAnnotations() {
        return annotations;
    }

    @Override
    public List<Field> getFields() {
        return fields;
    }

    @Override
    public List<? extends Method> getMethods() {
        return methods;
    }

    @Override
    public File getRelativeFile() {
        return new File(packageName.replace(".", File.separator),
                getTypeDeclaration().getName() + ".java");
    }

    @Override
    public String getFtlTempleteLocation() {
        return "abstract_ftl_file.ftl";
    }

    @Override
    public String toString() {
        return "AbstractFtlTemplate{" + "typeDeclaration=" + typeDeclaration
                + ", packageName='" + packageName + '\'' + '}';
    }
}
