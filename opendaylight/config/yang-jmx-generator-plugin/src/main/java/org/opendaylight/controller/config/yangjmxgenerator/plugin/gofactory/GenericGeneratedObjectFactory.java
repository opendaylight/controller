/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.yangjmxgenerator.plugin.gofactory;

import com.google.common.base.Optional;
import org.opendaylight.controller.config.yangjmxgenerator.plugin.ftl.FtlTemplate;
import org.opendaylight.controller.config.yangjmxgenerator.plugin.ftl.model.Annotation;
import org.opendaylight.controller.config.yangjmxgenerator.plugin.ftl.model.Constructor;
import org.opendaylight.controller.config.yangjmxgenerator.plugin.ftl.model.Field;
import org.opendaylight.controller.config.yangjmxgenerator.plugin.ftl.model.Method;
import org.opendaylight.controller.config.yangjmxgenerator.plugin.java.FullyQualifiedName;
import org.opendaylight.controller.config.yangjmxgenerator.plugin.java.GeneratedObject;
import org.opendaylight.controller.config.yangjmxgenerator.plugin.java.GeneratedObjectBuilder;
import org.opendaylight.controller.config.yangjmxgenerator.plugin.java.JavaFileInputBuilder;

public class GenericGeneratedObjectFactory {

    public GeneratedObject toGeneratedObject(FtlTemplate template, Optional<String> copyright) {
        JavaFileInputBuilder b = new JavaFileInputBuilder();
        b.setHeader(template.getHeaderString());
        b.setFqn(new FullyQualifiedName(template.getPackageName(), template.getTypeDeclaration().getName()));
        b.setClassJavaDoc(template.getMaybeJavadoc());
        for (Annotation annotation : template.getAnnotations()) {
            b.addClassAnnotation(annotation);
        }
        // type declaration
        for (String extended : template.getTypeDeclaration().getExtended()) {
            b.addExtendsFQN(FullyQualifiedName.fromString(extended));
        }
        for (String implemented : template.getTypeDeclaration().getImplemented()) {
            b.addImplementsFQN(FullyQualifiedName.fromString(implemented));
        }
        b.setCopyright(copyright);
        b.setTypeName(template.getTypeDeclaration().toTypeName());
        // fields
        for (Field field : template.getFields()) {
            b.addToBody(field.toString());
        }
        // constructors
        for (Constructor constructor : template.getConstructors()) {
            b.addToBody(constructor.toString());
        }
        // methods
        for (Method method : template.getMethods()) {
            b.addToBody(method.toString());
        }
        return new GeneratedObjectBuilder(b.build()).toGeneratedObject();
    }
}
