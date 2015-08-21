/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.yangjmxgenerator.plugin.gofactory;

import static java.lang.String.format;

import com.google.common.base.Optional;
import java.util.ArrayList;
import java.util.List;
import org.opendaylight.controller.config.api.DependencyResolver;
import org.opendaylight.controller.config.api.DynamicMBeanWithInstance;
import org.opendaylight.controller.config.api.ModuleIdentifier;
import org.opendaylight.controller.config.api.annotations.AbstractServiceInterface;
import org.opendaylight.controller.config.api.annotations.Description;
import org.opendaylight.controller.config.spi.Module;
import org.opendaylight.controller.config.spi.ModuleFactory;
import org.opendaylight.controller.config.yangjmxgenerator.ModuleMXBeanEntry;
import org.opendaylight.controller.config.yangjmxgenerator.plugin.ftl.AbstractFactoryTemplate;
import org.opendaylight.controller.config.yangjmxgenerator.plugin.ftl.TemplateFactory;
import org.opendaylight.controller.config.yangjmxgenerator.plugin.ftl.model.Annotation;
import org.opendaylight.controller.config.yangjmxgenerator.plugin.ftl.model.Field;
import org.opendaylight.controller.config.yangjmxgenerator.plugin.java.FullyQualifiedName;
import org.opendaylight.controller.config.yangjmxgenerator.plugin.java.GeneratedObject;
import org.opendaylight.controller.config.yangjmxgenerator.plugin.java.GeneratedObjectBuilder;
import org.opendaylight.controller.config.yangjmxgenerator.plugin.java.JavaFileInputBuilder;
import org.opendaylight.controller.config.yangjmxgenerator.plugin.java.TypeName;
import org.opendaylight.yangtools.yang.common.QName;

public class AbsFactoryGeneratedObjectFactory {
    private static final String BUNDLE_CONTEXT = "org.osgi.framework.BundleContext";

    public GeneratedObject toGeneratedObject(ModuleMXBeanEntry mbe, Optional<String> copyright) {
        FullyQualifiedName absFactoryFQN = new FullyQualifiedName(mbe.getPackageName(), mbe.getAbstractFactoryName());
        FullyQualifiedName moduleFQN = new FullyQualifiedName(mbe.getPackageName(), mbe.getStubModuleName());
        Optional<String> classJavaDoc = Optional.fromNullable(mbe.getNullableDescription());

        AbstractFactoryTemplate abstractFactoryTemplate = TemplateFactory.abstractFactoryTemplateFromMbe(mbe);
        Optional<String> header = abstractFactoryTemplate.getHeaderString();

        List<FullyQualifiedName> providedServices = new ArrayList<>();
        for(String providedService: mbe.getProvidedServices().keySet()) {
            providedServices.add(FullyQualifiedName.fromString(providedService));
        }

        return toGeneratedObject(absFactoryFQN, copyright,
                header, classJavaDoc, mbe.getYangModuleQName(),
                mbe.getGloballyUniqueName(),
                providedServices,
                moduleFQN,
                abstractFactoryTemplate.getFields());
    }

    public GeneratedObject toGeneratedObject(FullyQualifiedName absFactoryFQN, Optional<String> copyright,
                                             Optional<String> header, Optional<String> classJavaDoc, QName yangModuleQName,
                                             String globallyUniqueName,
                                             List<FullyQualifiedName> providedServices,
                                             FullyQualifiedName moduleFQN,
                                             List<Field> moduleFields) {
        JavaFileInputBuilder b = new JavaFileInputBuilder();
        Annotation moduleQNameAnnotation = Annotation.createModuleQNameANnotation(yangModuleQName);
        b.addClassAnnotation(moduleQNameAnnotation);

        b.setFqn(absFactoryFQN);
        b.setTypeName(TypeName.absClassType);

        b.setCopyright(copyright);
        b.setHeader(header);
        b.setClassJavaDoc(classJavaDoc);
        b.addImplementsFQN(new FullyQualifiedName(ModuleFactory.class));
        if (classJavaDoc.isPresent()) {
            b.addClassAnnotation(format("@%s(value=\"%s\")", Description.class.getCanonicalName(), classJavaDoc.get()));
        }

        b.addToBody(format("public static final java.lang.String NAME = \"%s\";", globallyUniqueName));
        b.addToBody(format("private static final java.util.Set<Class<? extends %s>> serviceIfcs;",
                AbstractServiceInterface.class.getCanonicalName()));

        b.addToBody("@Override\n public final String getImplementationName() { \n return NAME; \n}");

        b.addToBody(getServiceIfcsInitialization(providedServices));

        // createModule
        b.addToBody(format("\n"+
            "@Override\n"+
            "public %s createModule(String instanceName, %s dependencyResolver, %s bundleContext) {\n"+
                "return instantiateModule(instanceName, dependencyResolver, bundleContext);\n"+
            "}\n",
                Module.class.getCanonicalName(), DependencyResolver.class.getCanonicalName(), BUNDLE_CONTEXT));

        b.addToBody(getCreateModule(moduleFQN, moduleFields));

        b.addToBody(format("\n"+
            "public %s instantiateModule(String instanceName, %s dependencyResolver, %s oldModule, %s oldInstance, %s bundleContext) {\n"+
                "return new %s(new %s(NAME, instanceName), dependencyResolver, oldModule, oldInstance);\n"+
            "}\n",
                moduleFQN, DependencyResolver.class.getCanonicalName(), moduleFQN, AutoCloseable.class.getCanonicalName(),
                BUNDLE_CONTEXT, moduleFQN, ModuleIdentifier.class.getCanonicalName()));

        b.addToBody(format("\n"+
            "public %s instantiateModule(String instanceName, %s dependencyResolver, %s bundleContext) {\n"+
                "return new %s(new %s(NAME, instanceName), dependencyResolver);\n"+
            "}\n", moduleFQN, DependencyResolver.class.getCanonicalName(), BUNDLE_CONTEXT,
                moduleFQN, ModuleIdentifier.class.getCanonicalName()
        ));

        b.addToBody(format("\n"+
            "public %s handleChangedClass(%s old) throws Exception {\n"+
                "throw new UnsupportedOperationException(\"Class reloading is not supported\");\n"+
            "}\n", moduleFQN, DynamicMBeanWithInstance.class.getCanonicalName()));

        // TODO The generic specifier in HashSet<> isn't necessary, but the Eclipse AST parser used in the unit tests doesn't support this
        b.addToBody(format("\n"+
            "@Override\n"+
            "public java.util.Set<%s> getDefaultModules(org.opendaylight.controller.config.api.DependencyResolverFactory dependencyResolverFactory, %s bundleContext) {\n"+
                "return new java.util.HashSet<%1$s>();\n"+
            "}\n", moduleFQN, BUNDLE_CONTEXT));

        return new GeneratedObjectBuilder(b.build()).toGeneratedObject();
    }

    private static String getCreateModule(FullyQualifiedName moduleFQN, List<Field> moduleFields) {
        String result = "\n"+
            "@Override\n"+
            format("public %s createModule(String instanceName, %s dependencyResolver, %s old, %s bundleContext) throws Exception {\n",
                                Module.class.getCanonicalName(), DependencyResolver.class.getCanonicalName(),
                                DynamicMBeanWithInstance.class.getCanonicalName(), BUNDLE_CONTEXT)+
                format("%s oldModule;\n",moduleFQN)+
                "try {\n"+
                    format("oldModule = (%s) old.getModule();\n",moduleFQN)+
                "} catch(Exception e) {\n"+
                    "return handleChangedClass(old);\n"+
                "}\n"+
            format("%s module = instantiateModule(instanceName, dependencyResolver, oldModule, old.getInstance(), bundleContext);\n", moduleFQN);

        for(Field field: moduleFields) {
            result += format("module.set%s(oldModule.get%1$s());\n", field.getName());
        }

        result += "\n"+
                "return module;\n"+
            "}\n";
        return result;
    }

    private static String getServiceIfcsInitialization(List<FullyQualifiedName> providedServices) {
        String generic = format("Class<? extends %s>", AbstractServiceInterface.class.getCanonicalName());

        String result = "static {\n";
        if (!providedServices.isEmpty()) {
            // TODO The generic specifier in HashSet<> isn't necessary, but the Eclipse AST parser used in the unit tests doesn't support this
            result += format("java.util.Set<%1$s> serviceIfcs2 = new java.util.HashSet<%1$s>();\n", generic);

            for(FullyQualifiedName fqn: providedServices) {
                result += format("serviceIfcs2.add(%s.class);\n", fqn);
            }

            result += "serviceIfcs = java.util.Collections.unmodifiableSet(serviceIfcs2);\n";
        } else {
            result += "serviceIfcs = java.util.Collections.emptySet();\n";
        }
        result += "}\n";

        // add isModuleImplementingServiceInterface and getImplementedServiceIntefaces methods

        result += format("\n"+
            "@Override\n"+
            "public final boolean isModuleImplementingServiceInterface(Class<? extends %1$s> serviceInterface) {\n"+
                "for (Class<?> ifc: serviceIfcs) {\n"+
                    "if (serviceInterface.isAssignableFrom(ifc)){\n"+
                        "return true;\n"+
                    "}\n"+
                "}\n"+
                "return false;\n"+
            "}\n"+
            "\n"+
            "@Override\n"+
            "public java.util.Set<Class<? extends %1$s>> getImplementedServiceIntefaces() {\n"+
                "return serviceIfcs;\n"+
            "}\n", AbstractServiceInterface.class.getCanonicalName());

        return result;
    }

}
