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
import org.opendaylight.controller.config.api.ModuleIdentifier
import org.opendaylight.controller.config.api.annotations.Description
import org.opendaylight.controller.config.api.runtime.RootRuntimeBeanRegistrator
import org.opendaylight.controller.config.yangjmxgenerator.ModuleMXBeanEntry
import org.opendaylight.controller.config.yangjmxgenerator.plugin.ftl.AbstractModuleTemplate
import org.opendaylight.controller.config.yangjmxgenerator.plugin.ftl.TemplateFactory
import org.opendaylight.controller.config.yangjmxgenerator.plugin.ftl.model.Annotation
import org.opendaylight.controller.config.yangjmxgenerator.plugin.ftl.model.IdentityRefModuleField
import org.opendaylight.controller.config.yangjmxgenerator.plugin.ftl.model.Method
import org.opendaylight.controller.config.yangjmxgenerator.plugin.ftl.model.ModuleField
import org.opendaylight.controller.config.yangjmxgenerator.plugin.java.*
import org.opendaylight.yangtools.yang.common.QName
import org.slf4j.Logger
import org.slf4j.LoggerFactory

public class AbsModuleGeneratedObjectFactory {

    public GeneratedObject toGeneratedObject(ModuleMXBeanEntry mbe, Optional<String> copyright) {
        FullyQualifiedName abstractFQN = new FullyQualifiedName(mbe.getPackageName(), mbe.getAbstractModuleName())
        Optional<String> classJavaDoc = Optional.fromNullable(mbe.getNullableDescription())
        AbstractModuleTemplate abstractModuleTemplate = TemplateFactory.abstractModuleTemplateFromMbe(mbe)
        Optional<String> header = abstractModuleTemplate.headerString;
        List<FullyQualifiedName> implementedInterfaces = abstractModuleTemplate.getTypeDeclaration().getImplemented().collect {
            FullyQualifiedName.fromString(it)
        }
        Optional<FullyQualifiedName> maybeRegistratorType
        if (abstractModuleTemplate.isRuntime()) {
            maybeRegistratorType = Optional.of(FullyQualifiedName.fromString(abstractModuleTemplate.getRegistratorType()))
        } else {
            maybeRegistratorType = Optional.absent()
        }

        return toGeneratedObject(abstractFQN, copyright, header, classJavaDoc, implementedInterfaces,
                abstractModuleTemplate.getModuleFields(), maybeRegistratorType, abstractModuleTemplate.getMethods(),
                mbe.yangModuleQName
        )
    }

    public GeneratedObject toGeneratedObject(FullyQualifiedName abstractFQN,
                                             Optional<String> copyright,
                                             Optional<String> header,
                                             Optional<String> classJavaDoc,
                                             List<FullyQualifiedName> implementedInterfaces,
                                             List<ModuleField> moduleFields,
                                             Optional<FullyQualifiedName> maybeRegistratorType,
                                             List<Method> methods,
                                             QName yangModuleQName) {
        JavaFileInputBuilder b = new JavaFileInputBuilder()

        Annotation moduleQNameAnnotation = Annotation.createModuleQNameANnotation(yangModuleQName)
        b.addClassAnnotation(moduleQNameAnnotation)

        b.setFqn(abstractFQN)
        b.setTypeName(TypeName.absClassType)

        b.setCopyright(copyright);
        b.setHeader(header);
        b.setClassJavaDoc(classJavaDoc);
        implementedInterfaces.each { b.addImplementsFQN(it) }
        if (classJavaDoc.isPresent()) {
            b.addClassAnnotation("@${Description.canonicalName}(value=\"${classJavaDoc.get()}\")")
        }

        // add logger:
        b.addToBody(getLogger(abstractFQN));

        b.addToBody("//attributes start");

        b.addToBody(moduleFields.collect { it.toString() }.join("\n"))

        b.addToBody("//attributes end");


        b.addToBody(getCommonFields(abstractFQN));


        b.addToBody(getNewConstructor(abstractFQN))
        b.addToBody(getCopyFromOldConstructor(abstractFQN))

        b.addToBody(getRuntimeRegistratorCode(maybeRegistratorType))
        b.addToBody(getValidationMethods(moduleFields))

        b.addToBody(getCachesOfResolvedDependencies(moduleFields))
        b.addToBody(getCachesOfResolvedIdentityRefs(moduleFields))
        b.addToBody(getGetInstance(moduleFields))
        b.addToBody(getReuseLogic(moduleFields, abstractFQN))
        b.addToBody(getEqualsAndHashCode(abstractFQN))

        b.addToBody(getMethods(methods))

        return new GeneratedObjectBuilder(b.build()).toGeneratedObject()
    }

    private static String getMethods(List<Method> methods) {
        String result = """
            // getters and setters
        """
        result += methods.collect{it.toString()}.join("\n")
        return result
    }

    private static String getEqualsAndHashCode(FullyQualifiedName abstractFQN) {
        return """
            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;
                ${abstractFQN.typeName} that = (${abstractFQN.typeName}) o;
                return identifier.equals(that.identifier);
            }

            @Override
            public int hashCode() {
                return identifier.hashCode();
            }
        """
    }

    private static String getReuseLogic(List<ModuleField> moduleFields, FullyQualifiedName abstractFQN) {
        String result = """
            public boolean canReuseInstance(${abstractFQN.typeName} oldModule){
                // allow reusing of old instance if no parameters was changed
                return isSame(oldModule);
            }

            public ${AutoCloseable.canonicalName} reuseInstance(${AutoCloseable.canonicalName} oldInstance){
                // implement if instance reuse should be supported. Override canReuseInstance to change the criteria.
                return oldInstance;
            }
            """
        // isSame method that detects changed fields
        result += """
            public boolean isSame(${abstractFQN.typeName} other) {
                if (other == null) {
                    throw new IllegalArgumentException("Parameter 'other' is null");
                }
            """
        // loop through fields, do deep equals on each field
        result += moduleFields.collect { field ->
            if (field.isListOfDependencies()) {
                return """
                    if (${field.name}Dependency.equals(other.${field.name}Dependency) == false) {
                        return false;
                    }
                    for (int idx = 0; idx < ${field.name}Dependency.size(); idx++) {
                        if (${field.name}Dependency.get(idx) != other.${field.name}Dependency.get(idx)) {
                            return false;
                        }
                    }
                """
            } else if (field.isDependent()) {
                return """
                    if (${field.name}Dependency != other.${field.name}Dependency) { // reference to dependency must be same
                        return false;
                    }
                """
            } else {
                return """
                    if (java.util.Objects.deepEquals(${field.name}, other.${field.name}) == false) {
                        return false;
                    }
                """
            }
        }.join("\n")


        result += """
                return true;
            }
            """

        return result
    }

    private static String getGetInstance(List<ModuleField> moduleFields) {
        String result = """
            @Override
            public final ${AutoCloseable.canonicalName} getInstance() {
                if(instance==null) {
            """
        // create instance start

        // loop through dependent fields, use dependency resolver to instantiate dependencies. Do it in loop in case field represents list of dependencies.
        Map<ModuleField, String> resolveDependenciesMap = moduleFields.findAll {
            it.isDependent()
        }.collectEntries { ModuleField field ->
            [field, field.isList() ?
                    """
                ${field.name}Dependency = new java.util.ArrayList<${field.dependency.sie.exportedOsgiClassName}>();
                for(javax.management.ObjectName dep : ${field.name}) {
                    ${field.name}Dependency.add(dependencyResolver.resolveInstance(${
                        field.dependency.sie.exportedOsgiClassName
                    }.class, dep, ${field.name}JmxAttribute));
                }
                """
                    :
                    """
                ${field.name}Dependency = dependencyResolver.resolveInstance(${
                        field.dependency.sie.exportedOsgiClassName
                    }.class, ${field.name}, ${field.name}JmxAttribute);
                """
            ]
        }
        // wrap each field resolvation statement with if !=null when dependency is not mandatory
        def wrapWithNullCheckClosure = {Map<ModuleField, String> map, predicate -> map.collect { ModuleField key, String value ->
            predicate(key) ? """
                if(${key.name}!=null) {
                    ${value}
                }
                """ : value
            }.join("\n")
        }

        result += wrapWithNullCheckClosure(resolveDependenciesMap, {ModuleField key ->
            key.getDependency().isMandatory() == false} )

        // add code to inject dependency resolver to fields that support it
        Map<ModuleField, String> injectDepsMap = moduleFields.findAll { it.needsDepResolver }.collectEntries { field ->
            if (field.isList()) {
                return [field,"""
                for(${field.genericInnerType} candidate : ${field.name}) {
                    candidate.injectDependencyResolver(dependencyResolver);
                }
                """]
            } else {
                return [field, "${field.name}.injectDependencyResolver(dependencyResolver);"]
            }
        }

        result += wrapWithNullCheckClosure(injectDepsMap, {true})

        // identity refs need to be injected with dependencyResolver and base class
        Map<ModuleField, String> resolveIdentityMap = moduleFields.findAll { it.isIdentityRef() }.collectEntries { IdentityRefModuleField field ->
            [field,
            "set${field.attributeName}(${field.name}.resolveIdentity(dependencyResolver, ${field.identityBaseClass}.class));"]
        }

        result += wrapWithNullCheckClosure(resolveIdentityMap, {true})

        // create instance end: reuse and recreate logic
        result += """
                    if(oldInstance!=null && canReuseInstance(oldModule)) {
                        instance = reuseInstance(oldInstance);
                    } else {
                        if(oldInstance!=null) {
                            try {
                                oldInstance.close();
                            } catch(Exception e) {
                                logger.error("An error occurred while closing old instance " + oldInstance, e);
                            }
                        }
                        instance = createInstance();
                        if (instance == null) {
                            throw new IllegalStateException("Error in createInstance - null is not allowed as return value");
                        }
                    }
                }
                return instance;
            }
            public abstract ${AutoCloseable.canonicalName} createInstance();
            """
        return result
    }

    private static String getCommonFields(FullyQualifiedName abstractFQN) {
        return """
            private final ${abstractFQN.typeName} oldModule;
            private final ${AutoCloseable.canonicalName} oldInstance;
            private ${AutoCloseable.canonicalName} instance;
            private final ${DependencyResolver.canonicalName} dependencyResolver;
            private final ${ModuleIdentifier.canonicalName} identifier;
            @Override
            public ${ModuleIdentifier.canonicalName} getIdentifier() {
                return identifier;
            }
            """
    }

    private static String getCachesOfResolvedIdentityRefs(List<ModuleField> moduleFields) {
        return moduleFields.findAll { it.isIdentityRef() }.collect { IdentityRefModuleField field ->
            "private ${field.identityClassType} ${field.identityClassName};"
        }.join("\n")
    }

    private static String getCachesOfResolvedDependencies(List<ModuleField> moduleFields) {
        return moduleFields.findAll { it.dependent }.collect { field ->
            if (field.isList()) {
                return """
                    private java.util.List<${field.dependency.sie.exportedOsgiClassName}> ${
                    field.name
                }Dependency = new java.util.ArrayList<${field.dependency.sie.exportedOsgiClassName}>();
                    protected final java.util.List<${field.dependency.sie.exportedOsgiClassName}> get${
                    field.attributeName
                }Dependency(){
                        return ${field.name}Dependency;
                    }
                    """
            } else {
                return """
                    private ${field.dependency.sie.exportedOsgiClassName} ${field.name}Dependency;
                    protected final ${field.dependency.sie.exportedOsgiClassName} get${field.attributeName}Dependency(){
                        return ${field.name}Dependency;
                    }
                    """
            }
        }.join("\n")
    }

    private static String getRuntimeRegistratorCode(Optional<FullyQualifiedName> maybeRegistratorType) {
        if (maybeRegistratorType.isPresent()) {
            String registratorType = maybeRegistratorType.get()

            return """
                private ${registratorType} rootRuntimeBeanRegistratorWrapper;

                public ${registratorType} getRootRuntimeBeanRegistratorWrapper(){
                    return rootRuntimeBeanRegistratorWrapper;
                }

                @Override
                public void setRuntimeBeanRegistrator(${RootRuntimeBeanRegistrator.canonicalName} rootRuntimeRegistrator){
                    this.rootRuntimeBeanRegistratorWrapper = new ${registratorType}(rootRuntimeRegistrator);
                }
                """
        } else {
            return ""
        }
    }

    private static String getValidationMethods(List<ModuleField> moduleFields) {
        String result = """
            @Override
            public void validate() {
            """
        // validate each mandatory dependency
        List<String> lines = moduleFields.findAll{(it.dependent && it.dependency.mandatory)}.collect { field ->
            if (field.isList()) {
                return "" +
                        "for(javax.management.ObjectName dep : ${field.name}) {\n" +
                        "    dependencyResolver.validateDependency(${field.dependency.sie.fullyQualifiedName}.class, dep, ${field.name}JmxAttribute);\n" +
                        "}\n"
            } else {
                return "dependencyResolver.validateDependency(${field.dependency.sie.fullyQualifiedName}.class, ${field.name}, ${field.name}JmxAttribute);"
            }
        }
        result += lines.findAll { it.isEmpty() == false }.join("\n")
        result += """
                customValidation();
            }

            protected void customValidation(){
            }
        """
        return result
    }

    private static String getLogger(FullyQualifiedName fqn) {
        return "private static final ${Logger.canonicalName} logger = ${LoggerFactory.canonicalName}.getLogger(${fqn.toString()}.class);"
    }

    // assumes that each parameter name corresponds to an field in this class, constructs lines setting this.field = field;
    private static String getConstructorStart(FullyQualifiedName fqn,
                                              LinkedHashMap<String, String> parameters, String after) {
        return "public ${fqn.typeName}(" +
                parameters.collect { it.key + " " + it.value }.join(",") +
                ") {\n" +
                parameters.values().collect { "this.${it}=${it};\n" }.join() +
                after +
                "}\n"
    }

    private static String getNewConstructor(FullyQualifiedName abstractFQN) {
        LinkedHashMap<String, String> parameters = [
                (ModuleIdentifier.canonicalName): "identifier",
                (DependencyResolver.canonicalName): "dependencyResolver"
        ]
        String setToNulls = ["oldInstance", "oldModule"].collect { "this.${it}=null;\n" }.join()
        return getConstructorStart(abstractFQN, parameters, setToNulls)
    }

    private static String getCopyFromOldConstructor(FullyQualifiedName abstractFQN) {
        LinkedHashMap<String, String> parameters = [
                (ModuleIdentifier.canonicalName): "identifier",
                (DependencyResolver.canonicalName): "dependencyResolver",
                (abstractFQN.typeName): "oldModule",
                (AutoCloseable.canonicalName): "oldInstance"
        ]
        return getConstructorStart(abstractFQN, parameters, "")
    }
}
