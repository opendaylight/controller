/*
 * Copyright (c) 2013, 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.yangjmxgenerator.plugin.gofactory;

import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import java.util.LinkedHashMap;
import org.opendaylight.controller.config.api.DependencyResolver;
import org.opendaylight.controller.config.api.ModuleIdentifier;
import org.opendaylight.controller.config.yangjmxgenerator.ModuleMXBeanEntry;
import org.opendaylight.controller.config.yangjmxgenerator.plugin.java.FullyQualifiedName;
import org.opendaylight.controller.config.yangjmxgenerator.plugin.java.GeneratedObject;
import org.opendaylight.controller.config.yangjmxgenerator.plugin.java.GeneratedObjectBuilder;
import org.opendaylight.controller.config.yangjmxgenerator.plugin.java.JavaFileInputBuilder;
import org.opendaylight.controller.config.yangjmxgenerator.plugin.java.TypeName;

public class ConcreteModuleGeneratedObjectFactory {

    public GeneratedObject toGeneratedObject(ModuleMXBeanEntry mbe, Optional<String> copyright, Optional<String> header) {
        FullyQualifiedName concreteFQN = new FullyQualifiedName(mbe.getPackageName(), mbe.getStubModuleName());
        FullyQualifiedName abstractFQN = new FullyQualifiedName(mbe.getPackageName(), mbe.getAbstractModuleName());
        Optional<String> classJavaDoc = Optional.fromNullable(mbe.getNullableDescription());
        return toGeneratedObject(concreteFQN, abstractFQN, copyright, header, classJavaDoc);
    }

    GeneratedObject toGeneratedObject(FullyQualifiedName concreteFQN,
                                             FullyQualifiedName abstractFQN,
                                             Optional<String> copyright,
                                             Optional<String> header,
                                             Optional<String> classJavaDoc) {
        // there are two constructors and two methods
        JavaFileInputBuilder builder = new JavaFileInputBuilder();
        builder.setTypeName(TypeName.classType);
        builder.setFqn(concreteFQN);
        builder.addExtendsFQN(abstractFQN);

        builder.setCopyright(copyright);
        builder.setHeader(header);
        builder.setClassJavaDoc(classJavaDoc);

        builder.addToBody(getNewCtor(concreteFQN));
        builder.addToBody(getCopyCtor(concreteFQN));
        builder.addToBody(getCustomValidationStub());
        builder.addToBody(getCreateInstanceStub());

        return new GeneratedObjectBuilder(builder.build()).toGeneratedObject();
    }

    private static String getNewCtor(FullyQualifiedName fqn) {
        LinkedHashMap<String, String> parameters = new LinkedHashMap<>();
        parameters.put(ModuleIdentifier.class.getCanonicalName(), "identifier");
        parameters.put(DependencyResolver.class.getCanonicalName(), "dependencyResolver");

        StringBuilder stringBuilder = getCtor(fqn, parameters);
        return stringBuilder.toString();
    }

    private static StringBuilder getCtor(FullyQualifiedName fqn , LinkedHashMap<String, String> parameters) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("public ").append(fqn.getTypeName()).append("(");
        // parameters
        stringBuilder.append(Joiner.on(", ").withKeyValueSeparator(" ").join(parameters));
        stringBuilder.append(") {\n");
        if (!parameters.isEmpty()) {
            stringBuilder.append("super(");
            stringBuilder.append(Joiner.on(", ").join(parameters.values()));
            stringBuilder.append(");\n");
        }
        stringBuilder.append("}");
        return stringBuilder;
    }

    private static String getCopyCtor(final FullyQualifiedName fqn) {
        LinkedHashMap<String, String> parameters = new LinkedHashMap<>();
        parameters.put(ModuleIdentifier.class.getCanonicalName(), "identifier");
        parameters.put(DependencyResolver.class.getCanonicalName(), "dependencyResolver");
        parameters.put(fqn.toString(), "oldModule");
        parameters.put(AutoCloseable.class.getCanonicalName(), "oldInstance");
        StringBuilder stringBuilder = getCtor(fqn, parameters);
        return stringBuilder.toString();
    }

    private static String getCustomValidationStub() {
        return "@Override\n" +
                "public void customValidation() {\n" +
                "// add custom validation form module attributes here.\n" +
                "}";
    }

    private static String getCreateInstanceStub() {
        return "@Override\n" +
                "public " + AutoCloseable.class.getCanonicalName() + " createInstance() {\n" +
                "// TODO:implement\n" +
                "throw new " + UnsupportedOperationException.class.getCanonicalName() + "();\n"+
                "}";
    }
}
