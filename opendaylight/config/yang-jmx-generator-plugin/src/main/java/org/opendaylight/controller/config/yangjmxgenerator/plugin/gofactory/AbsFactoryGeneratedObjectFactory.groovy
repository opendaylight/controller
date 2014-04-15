/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.yangjmxgenerator.plugin.gofactory
import com.google.common.base.Optional
import org.opendaylight.controller.config.api.DependencyResolver
import org.opendaylight.controller.config.api.DynamicMBeanWithInstance
import org.opendaylight.controller.config.api.ModuleIdentifier
import org.opendaylight.controller.config.api.annotations.AbstractServiceInterface
import org.opendaylight.controller.config.api.annotations.Description
import org.opendaylight.controller.config.spi.Module
import org.opendaylight.controller.config.spi.ModuleFactory
import org.opendaylight.controller.config.yangjmxgenerator.ModuleMXBeanEntry
import org.opendaylight.controller.config.yangjmxgenerator.plugin.ftl.AbstractFactoryTemplate
import org.opendaylight.controller.config.yangjmxgenerator.plugin.ftl.TemplateFactory
import org.opendaylight.controller.config.yangjmxgenerator.plugin.ftl.model.Annotation
import org.opendaylight.controller.config.yangjmxgenerator.plugin.ftl.model.Field
import org.opendaylight.controller.config.yangjmxgenerator.plugin.java.*
import org.opendaylight.yangtools.yang.common.QName
import org.osgi.framework.BundleContext

public class AbsFactoryGeneratedObjectFactory {

    public GeneratedObject toGeneratedObject(ModuleMXBeanEntry mbe, Optional<String> copyright) {
        FullyQualifiedName absFactoryFQN = new FullyQualifiedName(mbe.packageName, mbe.abstractFactoryName)
        FullyQualifiedName moduleFQN = new FullyQualifiedName(mbe.packageName, mbe.stubModuleName)
        Optional<String> classJavaDoc = Optional.fromNullable(mbe.getNullableDescription())

        AbstractFactoryTemplate abstractFactoryTemplate = TemplateFactory.abstractFactoryTemplateFromMbe(mbe)
        Optional<String> header = abstractFactoryTemplate.headerString;
        List<FullyQualifiedName> providedServices = mbe.providedServices.keySet().collect {
            FullyQualifiedName.fromString(it)
        }


        return toGeneratedObject(absFactoryFQN, copyright,
                header, classJavaDoc, mbe.yangModuleQName,
                mbe.globallyUniqueName,
                providedServices,
                moduleFQN,
                abstractFactoryTemplate.fields)
    }

    public GeneratedObject toGeneratedObject(FullyQualifiedName absFactoryFQN, Optional<String> copyright,
                                             Optional<String> header, Optional<String> classJavaDoc, QName yangModuleQName,
                                             String globallyUniqueName,
                                             List<FullyQualifiedName> providedServices,
                                             FullyQualifiedName moduleFQN,
                                             List<Field> moduleFields) {
        JavaFileInputBuilder b = new JavaFileInputBuilder()
        Annotation moduleQNameAnnotation = Annotation.createModuleQNameANnotation(yangModuleQName)
        b.addClassAnnotation(moduleQNameAnnotation)

        b.setFqn(absFactoryFQN)
        b.setTypeName(TypeName.absClassType)

        b.setCopyright(copyright);
        b.setHeader(header);
        b.setClassJavaDoc(classJavaDoc);
        b.addImplementsFQN(new FullyQualifiedName(ModuleFactory))
        if (classJavaDoc.isPresent()) {
            b.addClassAnnotation("@${Description.canonicalName}(value=\"${classJavaDoc.get()}\")")
        }

        b.addToBody("public static final java.lang.String NAME = \"${globallyUniqueName}\";")
        b.addToBody("private static final java.util.Set<Class<? extends ${AbstractServiceInterface.canonicalName}>> serviceIfcs;")

        b.addToBody("@Override\n public final String getImplementationName() { \n return NAME; \n}")

        b.addToBody(getServiceIfcsInitialization(providedServices))

        // createModule
        b.addToBody("""
            @Override
            public ${Module.canonicalName} createModule(String instanceName, ${DependencyResolver.canonicalName} dependencyResolver, ${BundleContext.canonicalName} bundleContext) {
                return instantiateModule(instanceName, dependencyResolver, bundleContext);
            }
            """)

        b.addToBody(getCreateModule(moduleFQN, moduleFields))

        b.addToBody("""
            public ${moduleFQN} instantiateModule(String instanceName, ${DependencyResolver.canonicalName} dependencyResolver, ${moduleFQN} oldModule, ${AutoCloseable.canonicalName} oldInstance, ${BundleContext.canonicalName} bundleContext) {
                return new ${moduleFQN}(new ${ModuleIdentifier.canonicalName}(NAME, instanceName), dependencyResolver, oldModule, oldInstance);
            }
            """)

        b.addToBody("""
            public ${moduleFQN} instantiateModule(String instanceName, ${DependencyResolver.canonicalName} dependencyResolver, ${BundleContext.canonicalName} bundleContext) {
                return new ${moduleFQN}(new ${ModuleIdentifier.canonicalName}(NAME, instanceName), dependencyResolver);
            }
            """)

        b.addToBody("""
            public ${moduleFQN} handleChangedClass(${DynamicMBeanWithInstance.canonicalName} old) throws Exception {
                throw new UnsupportedOperationException("Class reloading is not supported");
            }
            """)

        b.addToBody("""
            @Override
            public java.util.Set<${moduleFQN}> getDefaultModules(org.opendaylight.controller.config.api.DependencyResolverFactory dependencyResolverFactory, ${BundleContext.canonicalName} bundleContext) {
                return new java.util.HashSet<${moduleFQN}>();
            }
            """)

        return new GeneratedObjectBuilder(b.build()).toGeneratedObject()
    }

    private static String getCreateModule(FullyQualifiedName moduleFQN, List<Field> moduleFields) {
        String result = """
            @Override
            public ${Module.canonicalName} createModule(String instanceName, ${DependencyResolver.canonicalName} dependencyResolver, ${DynamicMBeanWithInstance.canonicalName} old, ${BundleContext.canonicalName} bundleContext) throws Exception {
                ${moduleFQN} oldModule = null;
                try {
                    oldModule = (${moduleFQN}) old.getModule();
                } catch(Exception e) {
                    return handleChangedClass(old);
                }
                ${moduleFQN} module = instantiateModule(instanceName, dependencyResolver, oldModule, old.getInstance(), bundleContext);
            """
        result += moduleFields.collect{"module.set${it.name}(oldModule.get${it.name}());"}.join("\n")
        result += """
                return module;
            }
            """
        return result
    }

    private static String getServiceIfcsInitialization(List<FullyQualifiedName> providedServices) {
        String generic = "Class<? extends ${AbstractServiceInterface.canonicalName}>"

        String result = """static {
            java.util.Set<${generic}> serviceIfcs2 = new java.util.HashSet<${generic}>();
            """
        result += providedServices.collect{"serviceIfcs2.add(${it}.class);"}.join("\n")
        result += """serviceIfcs = java.util.Collections.unmodifiableSet(serviceIfcs2);
            }
            """

        // add isModuleImplementingServiceInterface and getImplementedServiceIntefaces methods

        result += """
            @Override
            public final boolean isModuleImplementingServiceInterface(Class<? extends ${AbstractServiceInterface.canonicalName}> serviceInterface) {
                for (Class<?> ifc: serviceIfcs) {
                    if (serviceInterface.isAssignableFrom(ifc)){
                        return true;
                    }
                }
                return false;
            }

            @Override
            public java.util.Set<Class<? extends ${AbstractServiceInterface.canonicalName}>> getImplementedServiceIntefaces() {
                return serviceIfcs;
            }
            """

        return result
    }

}
