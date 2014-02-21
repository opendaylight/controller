/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.yangjmxgenerator.plugin.gofactory

import com.google.common.base.Optional
import org.opendaylight.controller.config.yangjmxgenerator.plugin.ftl.FtlTemplate
import org.opendaylight.controller.config.yangjmxgenerator.plugin.java.FullyQualifiedName
import org.opendaylight.controller.config.yangjmxgenerator.plugin.java.GeneratedObject
import org.opendaylight.controller.config.yangjmxgenerator.plugin.java.GeneratedObjectBuilder
import org.opendaylight.controller.config.yangjmxgenerator.plugin.java.JavaFileInputBuilder

public class GenericGeneratedObjectFactory {

    public GeneratedObject toGeneratedObject(FtlTemplate template, Optional<String> copyright) {
        JavaFileInputBuilder b = new JavaFileInputBuilder();
        b.setHeader(template.headerString)
        b.setFqn(new FullyQualifiedName(template.packageName, template.typeDeclaration.name))
        b.setClassJavaDoc(template.maybeJavadoc)
        template.annotations.each { b.addClassAnnotation(it) }
        // type declaration
        template.typeDeclaration.extended.each { b.addExtendsFQN(FullyQualifiedName.fromString(it)) }
        template.typeDeclaration.implemented.each { b.addImplementsFQN(FullyQualifiedName.fromString(it)) }
        b.setCopyright(copyright);
        b.setTypeName(template.typeDeclaration.toTypeName())
        // fields
        template.fields.each { b.addToBody(it.toString()) }
        // constructors
        template.constructors.each { b.addToBody(it.toString()) }
        // methods
        template.methods.each { b.addToBody(it.toString()) }

        return new GeneratedObjectBuilder(b.build()).toGeneratedObject();
    }
}
