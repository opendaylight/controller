/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yangjmxgenerator.plugin.gofactory;

import static com.google.common.base.Preconditions.checkState;
import static java.lang.String.format;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.collect.Collections2;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.opendaylight.controller.config.api.DependencyResolver;
import org.opendaylight.controller.config.api.ModuleIdentifier;
import org.opendaylight.controller.config.api.annotations.Description;
import org.opendaylight.controller.config.api.runtime.RootRuntimeBeanRegistrator;
import org.opendaylight.controller.config.yangjmxgenerator.ModuleMXBeanEntry;
import org.opendaylight.controller.config.yangjmxgenerator.plugin.ftl.AbstractModuleTemplate;
import org.opendaylight.controller.config.yangjmxgenerator.plugin.ftl.TemplateFactory;
import org.opendaylight.controller.config.yangjmxgenerator.plugin.ftl.model.Annotation;
import org.opendaylight.controller.config.yangjmxgenerator.plugin.ftl.model.Field;
import org.opendaylight.controller.config.yangjmxgenerator.plugin.ftl.model.IdentityRefModuleField;
import org.opendaylight.controller.config.yangjmxgenerator.plugin.ftl.model.Method;
import org.opendaylight.controller.config.yangjmxgenerator.plugin.ftl.model.MethodDefinition;
import org.opendaylight.controller.config.yangjmxgenerator.plugin.ftl.model.ModuleField;
import org.opendaylight.controller.config.yangjmxgenerator.plugin.java.FullyQualifiedName;
import org.opendaylight.controller.config.yangjmxgenerator.plugin.java.GeneratedObject;
import org.opendaylight.controller.config.yangjmxgenerator.plugin.java.GeneratedObjectBuilder;
import org.opendaylight.controller.config.yangjmxgenerator.plugin.java.JavaFileInputBuilder;
import org.opendaylight.controller.config.yangjmxgenerator.plugin.java.TypeName;
import org.opendaylight.yangtools.yang.common.QName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AbsModuleGeneratedObjectFactory {

    private static final Function<String, FullyQualifiedName> FULLY_QUALIFIED_NAME_FUNCTION = new Function<String, FullyQualifiedName>() {
        @Override
        public FullyQualifiedName apply(final String input) {
            return FullyQualifiedName.fromString(input);
        }
    };

    public GeneratedObject toGeneratedObject(ModuleMXBeanEntry mbe, Optional<String> copyright) {
        FullyQualifiedName abstractFQN = new FullyQualifiedName(mbe.getPackageName(), mbe.getAbstractModuleName());
        Optional<String> classJavaDoc = Optional.fromNullable(mbe.getNullableDescription());
        AbstractModuleTemplate abstractModuleTemplate = TemplateFactory.abstractModuleTemplateFromMbe(mbe);
        Optional<String> header = abstractModuleTemplate.getHeaderString();

        List<FullyQualifiedName> implementedInterfaces = Lists.transform(abstractModuleTemplate.getTypeDeclaration().getImplemented(), FULLY_QUALIFIED_NAME_FUNCTION);

        Optional<FullyQualifiedName> extended =
                Optional.fromNullable(
                        Iterables.getFirst(
                                Collections2.transform(abstractModuleTemplate.getTypeDeclaration().getExtended(), FULLY_QUALIFIED_NAME_FUNCTION), null));

        Optional<FullyQualifiedName> maybeRegistratorType;
        if (abstractModuleTemplate.isRuntime()) {
            maybeRegistratorType = Optional.of(FullyQualifiedName.fromString(abstractModuleTemplate.getRegistratorType()));
        } else {
            maybeRegistratorType = Optional.absent();
        }

        return toGeneratedObject(abstractFQN, copyright, header, classJavaDoc, extended, implementedInterfaces,
                abstractModuleTemplate.getModuleFields(), maybeRegistratorType, abstractModuleTemplate.getMethods(),
                mbe.getYangModuleQName());
    }

    public GeneratedObject toGeneratedObject(FullyQualifiedName abstractFQN,
                                             Optional<String> copyright,
                                             Optional<String> header,
                                             Optional<String> classJavaDoc,
                                             Optional<FullyQualifiedName> extended,
                                             List<FullyQualifiedName> implementedInterfaces,
                                             List<ModuleField> moduleFields,
                                             Optional<FullyQualifiedName> maybeRegistratorType,
                                             List<? extends Method> methods,
                                             QName yangModuleQName) {
        JavaFileInputBuilder b = new JavaFileInputBuilder();

        Annotation moduleQNameAnnotation = Annotation.createModuleQNameANnotation(yangModuleQName);
        b.addClassAnnotation(moduleQNameAnnotation);

        b.setFqn(abstractFQN);
        b.setTypeName(TypeName.absClassType);

        b.setCopyright(copyright);
        b.setHeader(header);
        b.setClassJavaDoc(classJavaDoc);
        for(FullyQualifiedName implemented: implementedInterfaces) {
            b.addImplementsFQN(implemented);
        }
        if(extended.isPresent()) {
            b.addExtendsFQN(extended.get());
        }
        if (classJavaDoc.isPresent()) {
            b.addClassAnnotation(format("@%s(value=\"%s\")", Description.class.getCanonicalName(), classJavaDoc.get()));
        }

        // add logger:
        b.addToBody(getLoggerDefinition(abstractFQN));

        b.addToBody("//attributes start");
        for(ModuleField moduleField: moduleFields) {
            b.addToBody(moduleField.toString() +"\n");
        }

        b.addToBody("//attributes end");


        b.addToBody(getNewConstructor(abstractFQN));
        b.addToBody(getCopyFromOldConstructor(abstractFQN));

        b.addToBody(getRuntimeRegistratorCode(maybeRegistratorType));
        b.addToBody(getValidationMethods(moduleFields));

        b.addToBody(getCachesOfResolvedDependencies(moduleFields));
        b.addToBody(getCachesOfResolvedIdentityRefs(moduleFields));
        b.addToBody(getResolveDependencies(moduleFields));
        b.addToBody(getReuseLogic(moduleFields, abstractFQN));
        b.addToBody(getEqualsAndHashCode(abstractFQN));

        b.addToBody(getMethods(methods));
        b.addToBody(getGetLogger());

        return new GeneratedObjectBuilder(b.build()).toGeneratedObject();
    }

    private static String getMethods(List<? extends Method>  methods) {
        String result = "\n// getters and setters\n";
        for(Method method: methods) {
            result += method.toString()+"\n";
        }
        return result;
    }

    private static String getEqualsAndHashCode(FullyQualifiedName abstractFQN) {
        return "\n"+
            "@Override\n"+
            "public boolean equals(Object o) {\n"+
                "if (this == o) return true;\n"+
                "if (o == null || getClass() != o.getClass()) return false;\n"+
                format("%s that = (%1$s) o;\n", abstractFQN.getTypeName())+
                "return identifier.equals(that.identifier);\n"+
            "}\n"+
            "\n"+
            "@Override\n"+
            "public int hashCode() {\n"+
                "return identifier.hashCode();\n"+
            "}\n";
    }

    private static String getReuseLogic(List<ModuleField> moduleFields, FullyQualifiedName abstractFQN) {
        String result = "\n"+
            format("public boolean canReuseInstance(%s oldModule){\n", abstractFQN.getTypeName())+
                "// allow reusing of old instance if no parameters was changed\n"+
                "return isSame(oldModule);\n"+
            "}\n"+
            "\n"+
            format("public %s reuseInstance(%1$s oldInstance){\n", AutoCloseable.class.getCanonicalName())+
                "// implement if instance reuse should be supported. Override canReuseInstance to change the criteria.\n"+
                "return oldInstance;\n"+
            "}\n";
        // isSame method that detects changed fields
        result += "\n"+
            format("public boolean isSame(%s other) {\n", abstractFQN.getTypeName())+
                "if (other == null) {\n"+
                    "throw new IllegalArgumentException(\"Parameter 'other' is null\");\n"+
                "}\n";
        // loop through fields, do deep equals on each field

        for (ModuleField moduleField : moduleFields) {
            result += format(
                "if (java.util.Objects.deepEquals(%s, other.%1$s) == false) {\n"+
                    "return false;\n"+
                "}\n", moduleField.getName());

            if (moduleField.isListOfDependencies()) {
                result += format(
                        "for (int idx = 0; idx < %1$s.size(); idx++) {\n"+
                            "if (!dependencyResolver.canReuseDependency(%1$s.get(idx), %1$sJmxAttribute)) {\n"+
                                "return false;\n"+
                            "}\n"+
                        "}\n" , moduleField.getName());
            } else if (moduleField.isDependent()) {
                result += format(
                        // If a reference is null (ie optional reference) it makes no sens to call canReuse on it
                        // In such case we continue in the isSame method because if we have null here, the previous value was null as well
                        // If the previous value was not null and current is or vice verse, the deepEquals comparison would return false
                        "if(%1$s!= null) {\n" +
                            "if (!dependencyResolver.canReuseDependency(%1$s, %1$sJmxAttribute)) { // reference to dependency must be reusable as well\n" +
                                "return false;\n" +
                            "}\n" +
                        "}\n", moduleField.getName());
            }
        }

        result += "\n"+
                "return true;\n"+
            "}\n";

        return result;
    }

    private static String getResolveDependencies(final List<ModuleField> moduleFields) {
        // loop through dependent fields, use dependency resolver to instantiate dependencies. Do it in loop in case field represents list of dependencies.
        Map<ModuleField, String> resolveDependenciesMap = new HashMap<>();
        for(ModuleField moduleField: moduleFields) {
            if (moduleField.isDependent()) {
                String str;
                String osgi = moduleField.getDependency().getSie().getExportedOsgiClassName();
                if (moduleField.isList()) {
                    str = format(
                            "%sDependency = new java.util.ArrayList<%s>();\n"+
                                    "for(javax.management.ObjectName dep : %1$s) {\n"+
                                    "%1$sDependency.add(dependencyResolver.resolveInstance(%2$s.class, dep, %1$sJmxAttribute));\n"+
                                    "}\n", moduleField.getName(), osgi);
                } else {
                    str = format(
                            "%1$sDependency = dependencyResolver.resolveInstance(%2$s.class, %1$s, %1$sJmxAttribute);\n",
                            moduleField.getName(), osgi);
                }
                resolveDependenciesMap.put(moduleField, str);
            }
        }

        String result = "\n"
                + "protected final void resolveDependencies() {\n";
        // wrap each field resolvation statement with if !=null when dependency is not mandatory
        for (Map.Entry<ModuleField, String> entry : resolveDependenciesMap.entrySet()) {
            if (entry.getKey().getDependency().isMandatory() == false) {
                checkState(entry.getValue().endsWith(";\n"));
                result += format("if (%s!=null) {\n%s}\n", entry.getKey().getName(), entry.getValue());
            } else {
                result += entry.getValue();
            }
        }

        // add code to inject dependency resolver to fields that support it
        for(ModuleField moduleField: moduleFields) {
            if (moduleField.isNeedsDepResolver()) {
                result += format("if (%s!=null){\n", moduleField.getName());
                if (moduleField.isList()) {
                    result += format(
                            "for(%s candidate : %s) {\n"+
                                    "candidate.injectDependencyResolver(dependencyResolver);\n"+
                                    "}\n", moduleField.getGenericInnerType(), moduleField.getName());
                } else {
                    result += format("%s.injectDependencyResolver(dependencyResolver);\n", moduleField.getName());
                }
                result += "}\n";
            }
        }

        // identity refs need to be injected with dependencyResolver and base class
        for (ModuleField moduleField : moduleFields) {
            if (moduleField.isIdentityRef()) {
                result += format("if (%s!=null) {", moduleField.getName());
                result += format("set%s(%s.resolveIdentity(dependencyResolver, %s.class));",
                        moduleField.getAttributeName(), moduleField.getName(),
                        ((IdentityRefModuleField)moduleField).getIdentityBaseClass());
                result += "}\n";
            }
        }
        result += "}\n";
        return result;
    }

    private static String getCachesOfResolvedIdentityRefs(List<ModuleField> moduleFields) {
        StringBuilder result = new StringBuilder();
        for (ModuleField moduleField : moduleFields) {
            if (moduleField.isIdentityRef()) {
                IdentityRefModuleField field = (IdentityRefModuleField) moduleField;
                result.append(format("private %s %s;\n", field.getIdentityClassType(), field.getIdentityClassName()));
            }
        }
        return result.toString();
    }

    private static String getCachesOfResolvedDependencies(List<ModuleField> moduleFields) {
        StringBuilder result = new StringBuilder();
        for (ModuleField moduleField: moduleFields) {
            if (moduleField.isDependent()) {
                String osgi = moduleField.getDependency().getSie().getExportedOsgiClassName();
                if (moduleField.isList()) {
                    result
                            .append(format("private java.util.List<%s> %sDependency = new java.util.ArrayList<%s>();", osgi, moduleField.getName(), osgi))
                            .append(format("protected final java.util.List<%s> get%sDependency(){\n", osgi, moduleField.getAttributeName()))
                            .append(format("return %sDependency;\n", moduleField.getName()))
                            .append("}\n");
                } else {
                    result.append(format(
                        "private %s %sDependency;\n"+
                        "protected final %s get%sDependency(){\n"+
                            "return %sDependency;\n"+
                        "}",
                        osgi, moduleField.getName(), osgi, moduleField.getAttributeName(), moduleField.getName()));
                }
            }
        }
        return result.toString();
    }

    private static String getRuntimeRegistratorCode(Optional<FullyQualifiedName> maybeRegistratorType) {
        if (maybeRegistratorType.isPresent()) {
            String registratorType = maybeRegistratorType.get().toString();

            return "\n"+
                format("private %s rootRuntimeBeanRegistratorWrapper;\n", registratorType)+
                "\n"+
                format("public %s getRootRuntimeBeanRegistratorWrapper(){\n", registratorType)+
                    "return rootRuntimeBeanRegistratorWrapper;\n"+
                "}\n"+
                "\n"+
                "@Override\n"+
                format("public void setRuntimeBeanRegistrator(%s rootRuntimeRegistrator){\n", RootRuntimeBeanRegistrator.class.getCanonicalName())+
                    format("this.rootRuntimeBeanRegistratorWrapper = new %s(rootRuntimeRegistrator);\n", registratorType)+
                "}\n";
        } else {
            return "";
        }
    }

    private static String getValidationMethods(List<ModuleField> moduleFields) {
        String result = "\n"+
            "@Override\n"+
            "public void validate() {\n";
        // validate each mandatory dependency
        for(ModuleField moduleField: moduleFields) {
            if (moduleField.isDependent()) {
                if (moduleField.isList()) {
                    result += "" +
                            format("for(javax.management.ObjectName dep : %s) {\n", moduleField.getName()) +
                            format("    dependencyResolver.validateDependency(%s.class, dep, %sJmxAttribute);\n",
                                    moduleField.getDependency().getSie().getFullyQualifiedName(), moduleField.getName()) +
                            "}\n";
                } else {
                    if(moduleField.getDependency().isMandatory() == false) {
                        result += format("if(%s != null) {\n", moduleField.getName());
                    }
                    result += format("dependencyResolver.validateDependency(%s.class, %s, %sJmxAttribute);\n",
                            moduleField.getDependency().getSie().getFullyQualifiedName(), moduleField.getName(), moduleField.getName());
                    if(moduleField.getDependency().isMandatory() == false) {
                        result += "}\n";
                    }
                }
            }
        }
        result += "\n"+
                "customValidation();\n"+
            "}\n"+
            "\n"+
            "protected void customValidation() {\n"+
            "}\n";
        return result;
    }

    private static String getLoggerDefinition(FullyQualifiedName fqn) {
        return format("private static final %s LOGGER = %s.getLogger(%s.class);",
                Logger.class.getCanonicalName(), LoggerFactory.class.getCanonicalName(), fqn);
    }

    // assumes that each parameter name corresponds to an field in this class, constructs lines setting this.field = field;
    private static String getConstructorStart(FullyQualifiedName fqn,
                                              LinkedHashMap<String, String> parameters, String after) {
        String paramString = Joiner.on(",").withKeyValueSeparator(" ").join(parameters);
        return format("public %s(", fqn.getTypeName()) +
                paramString +
                ") {\n" +
                after +
                "}\n";
    }

    private static String getNewConstructor(FullyQualifiedName abstractFQN) {
        LinkedHashMap<String, String> parameters = new LinkedHashMap<>();
        parameters.put(ModuleIdentifier.class.getCanonicalName(), "identifier");
        parameters.put(DependencyResolver.class.getCanonicalName(), "dependencyResolver");
        String init = "super(identifier, dependencyResolver);\n";
        return getConstructorStart(abstractFQN, parameters, init);
    }

    private static String getCopyFromOldConstructor(FullyQualifiedName abstractFQN) {
        LinkedHashMap<String, String> parameters = new LinkedHashMap<>();
        parameters.put(ModuleIdentifier.class.getCanonicalName(), "identifier");
        parameters.put(DependencyResolver.class.getCanonicalName(), "dependencyResolver");
        parameters.put(abstractFQN.getTypeName(), "oldModule");
        parameters.put(AutoCloseable.class.getCanonicalName(), "oldInstance");
        String init = "super(identifier, dependencyResolver, oldModule, oldInstance);\n";
        return getConstructorStart(abstractFQN, parameters, init);
    }

    public String getGetLogger() {
        return new MethodDefinition(Logger.class.getCanonicalName(), "getLogger", Collections.<Field>emptyList(), "return LOGGER;").toString();
    }
}
