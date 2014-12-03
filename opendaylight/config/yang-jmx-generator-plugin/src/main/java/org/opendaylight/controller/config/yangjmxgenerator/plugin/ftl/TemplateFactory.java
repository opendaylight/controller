/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yangjmxgenerator.plugin.ftl;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.management.openmbean.SimpleType;
import org.opendaylight.controller.config.api.DependencyResolver;
import org.opendaylight.controller.config.api.IdentityAttributeRef;
import org.opendaylight.controller.config.api.RuntimeBeanRegistratorAwareModule;
import org.opendaylight.controller.config.api.annotations.AbstractServiceInterface;
import org.opendaylight.controller.config.api.runtime.RuntimeBean;
import org.opendaylight.controller.config.spi.AbstractModule;
import org.opendaylight.controller.config.yangjmxgenerator.AbstractEntry;
import org.opendaylight.controller.config.yangjmxgenerator.ModuleMXBeanEntry;
import org.opendaylight.controller.config.yangjmxgenerator.RuntimeBeanEntry;
import org.opendaylight.controller.config.yangjmxgenerator.RuntimeBeanEntry.Rpc;
import org.opendaylight.controller.config.yangjmxgenerator.ServiceInterfaceEntry;
import org.opendaylight.controller.config.yangjmxgenerator.attribute.AbstractDependencyAttribute;
import org.opendaylight.controller.config.yangjmxgenerator.attribute.AttributeIfc;
import org.opendaylight.controller.config.yangjmxgenerator.attribute.Dependency;
import org.opendaylight.controller.config.yangjmxgenerator.attribute.JavaAttribute;
import org.opendaylight.controller.config.yangjmxgenerator.attribute.ListAttribute;
import org.opendaylight.controller.config.yangjmxgenerator.attribute.ListDependenciesAttribute;
import org.opendaylight.controller.config.yangjmxgenerator.attribute.TOAttribute;
import org.opendaylight.controller.config.yangjmxgenerator.attribute.TypedAttribute;
import org.opendaylight.controller.config.yangjmxgenerator.attribute.VoidAttribute;
import org.opendaylight.controller.config.yangjmxgenerator.plugin.ftl.model.Annotation;
import org.opendaylight.controller.config.yangjmxgenerator.plugin.ftl.model.Annotation.Parameter;
import org.opendaylight.controller.config.yangjmxgenerator.plugin.ftl.model.Constructor;
import org.opendaylight.controller.config.yangjmxgenerator.plugin.ftl.model.Field;
import org.opendaylight.controller.config.yangjmxgenerator.plugin.ftl.model.Header;
import org.opendaylight.controller.config.yangjmxgenerator.plugin.ftl.model.IdentityRefModuleField;
import org.opendaylight.controller.config.yangjmxgenerator.plugin.ftl.model.MethodDeclaration;
import org.opendaylight.controller.config.yangjmxgenerator.plugin.ftl.model.MethodDefinition;
import org.opendaylight.controller.config.yangjmxgenerator.plugin.ftl.model.ModuleField;
import org.opendaylight.controller.config.yangjmxgenerator.plugin.util.FullyQualifiedNameHelper;
import org.opendaylight.yangtools.binding.generator.util.BindingGeneratorUtil;
import org.opendaylight.yangtools.sal.binding.model.api.ParameterizedType;
import org.opendaylight.yangtools.sal.binding.model.api.Type;

public class TemplateFactory {

    /**
     * Get map of file name as key, FtlFile instance representing runtime mx
     * bean as value that should be persisted from this instance.
     */
    public static Map<String, FtlTemplate> getTOAndMXInterfaceFtlFiles(
            RuntimeBeanEntry entry) {
        Map<String, FtlTemplate> result = new HashMap<>();
        { // create GeneralInterfaceFtlFile for runtime MXBean. Attributes will
          // be transformed to getter methods
            String mxBeanTypeName = entry.getJavaNameOfRuntimeMXBean();
            List<String> extendedInterfaces = Arrays.asList(RuntimeBean.class
                    .getCanonicalName());
            List<MethodDeclaration> methods = new ArrayList<>();

            // convert attributes to getters
            for (AttributeIfc attributeIfc : entry.getAttributes()) {
                String returnType;
                returnType = getReturnType(attributeIfc);
                String getterName = "get"
                        + attributeIfc.getUpperCaseCammelCase();
                MethodDeclaration getter = new MethodDeclaration(returnType,
                        getterName, Collections.<Field> emptyList());
                methods.add(getter);
            }

            // add rpc methods
            for (Rpc rpc : entry.getRpcs()) {
                // convert JavaAttribute parameters into fields
                List<Field> fields = new ArrayList<>();
                for (JavaAttribute ja : rpc.getParameters()) {
                    Field field = new Field(Collections.<String> emptyList(),
                            ja.getType().getFullyQualifiedName(),
                            ja.getLowerCaseCammelCase(), ja.getNullableDefaultWrappedForCode());
                    fields.add(field);
                }
                MethodDeclaration operation = new MethodDeclaration(
                        getReturnType(rpc.getReturnType()), rpc.getName(), fields);
                methods.add(operation);
            }

            // FIXME header
            GeneralInterfaceTemplate runtimeMxBeanIfc = new GeneralInterfaceTemplate(
                    null, entry.getPackageName(), mxBeanTypeName,
                    extendedInterfaces, methods);

            result.put(runtimeMxBeanIfc.getTypeDeclaration().getName()
                    + ".java", runtimeMxBeanIfc);
        }

        result.putAll(TemplateFactory.tOsFromRbe(entry));

        return result;
    }

    // FIXME: put into Type.toString
    static String serializeType(Type type, boolean addWildcards) {
        if (type instanceof ParameterizedType){
            ParameterizedType parameterizedType = (ParameterizedType) type;
            StringBuilder sb = new StringBuilder();
            sb.append(parameterizedType.getRawType().getFullyQualifiedName());
            sb.append(addWildcards ? "<? extends " : "<");
            boolean first = true;
            for(Type parameter: parameterizedType.getActualTypeArguments()) {
                if (first) {
                    first = false;
                } else {
                    sb.append(",");
                }
                sb.append(serializeType(parameter));
            }
            sb.append(">");
            return sb.toString();
        } else {
            return type.getFullyQualifiedName();
        }
    }

    static String serializeType(Type type) {
        return serializeType(type, false);
    }

    private static String getReturnType(AttributeIfc attributeIfc) {
        String returnType;
        if (attributeIfc instanceof TypedAttribute) {
            Type type = ((TypedAttribute) attributeIfc).getType();
            returnType = serializeType(type);
        } else if (attributeIfc == VoidAttribute.getInstance()) {
            return "void";
        } else {
            throw new UnsupportedOperationException(
                    "Attribute not supported: "
                            + attributeIfc.getClass());
        }
        return returnType;
    }

    public static GeneralInterfaceTemplate serviceInterfaceFromSie(
            ServiceInterfaceEntry sie) {

        List<String> extendedInterfaces = Lists
                .newArrayList(AbstractServiceInterface.class.getCanonicalName());
        if (sie.getBase().isPresent()) {
            extendedInterfaces.add(sie.getBase().get().getFullyQualifiedName());
        }

        // FIXME header
        GeneralInterfaceTemplate sieTemplate = new GeneralInterfaceTemplate(
                getHeaderFromEntry(sie), sie.getPackageName(),
                sie.getTypeName(), extendedInterfaces,
                Lists.<MethodDeclaration> newArrayList());
        sieTemplate.setJavadoc(sie.getNullableDescription());

        if (sie.getNullableDescription() != null) {
            sieTemplate.getAnnotations().add(
                    Annotation.createDescriptionAnnotation(sie
                            .getNullableDescription()));
        }
        sieTemplate.getAnnotations().addAll(Annotation.createSieAnnotations(sie));

        return sieTemplate;
    }

    public static AbstractFactoryTemplate abstractFactoryTemplateFromMbe(
            ModuleMXBeanEntry mbe) {
        AbstractFactoryAttributesProcessor attrProcessor = new AbstractFactoryAttributesProcessor();
        attrProcessor.processAttributes(mbe.getAttributes(),
                mbe.getPackageName());



        return new AbstractFactoryTemplate(getHeaderFromEntry(mbe),
                mbe.getPackageName(), mbe.getAbstractFactoryName(),
                attrProcessor.getFields()
        );
    }

    public static AbstractModuleTemplate abstractModuleTemplateFromMbe(
            ModuleMXBeanEntry mbe) {
        AbstractModuleAttributesProcessor attrProcessor = new AbstractModuleAttributesProcessor(mbe.getAttributes());

        List<ModuleField> moduleFields = attrProcessor.getModuleFields();
        List<String> implementedIfcs = Lists.newArrayList(
                mbe.getFullyQualifiedName(mbe.getMXBeanInterfaceName()));

        for (String implementedService : mbe.getProvidedServices().keySet()) {
            implementedIfcs.add(implementedService);
        }

        boolean generateRuntime = false;
        String registratorFullyQualifiedName = null;
        if (mbe.getRuntimeBeans() != null
                && mbe.getRuntimeBeans().isEmpty() == false) {
            generateRuntime = true;
            RuntimeBeanEntry rootEntry = RuntimeRegistratorFtlTemplate
                    .findRoot(mbe.getRuntimeBeans());
            registratorFullyQualifiedName = rootEntry
                    .getPackageName()
                    .concat(".")
                    .concat(RuntimeRegistratorFtlTemplate.getJavaNameOfRuntimeRegistrator(rootEntry));
            implementedIfcs.add(RuntimeBeanRegistratorAwareModule.class
                    .getCanonicalName());
        }

        List<String> extendedClasses = Collections.singletonList(AbstractModule.class.getCanonicalName() + "<" + mbe.getAbstractModuleName() + ">");

        AbstractModuleTemplate abstractModuleTemplate = new AbstractModuleTemplate(
                getHeaderFromEntry(mbe), mbe.getPackageName(),
                mbe.getAbstractModuleName(), extendedClasses, implementedIfcs, moduleFields,
                attrProcessor.getMethods(), generateRuntime,
                registratorFullyQualifiedName);

        if (mbe.getNullableDescription() != null) {
            abstractModuleTemplate.getAnnotations().add(
                    Annotation.createDescriptionAnnotation(mbe
                            .getNullableDescription()));
        }
        return abstractModuleTemplate;
    }

    public static StubFactoryTemplate stubFactoryTemplateFromMbe(
            ModuleMXBeanEntry mbe) {
        return new StubFactoryTemplate(getHeaderFromEntry(mbe),
                mbe.getPackageName(), mbe.getStubFactoryName(),
                mbe.getFullyQualifiedName(mbe.getAbstractFactoryName())
        );
    }

    public static GeneralInterfaceTemplate mXBeanInterfaceTemplateFromMbe(
            ModuleMXBeanEntry mbe) {
        MXBeanInterfaceAttributesProcessor attrProcessor = new MXBeanInterfaceAttributesProcessor();
        attrProcessor.processAttributes(mbe.getAttributes());
        GeneralInterfaceTemplate ifcTemplate = new GeneralInterfaceTemplate(
                getHeaderFromEntry(mbe), mbe.getPackageName(),
                mbe.getMXBeanInterfaceName(), Lists.<String> newArrayList(),
                attrProcessor.getMethods());
        ifcTemplate.setJavadoc(mbe.getNullableDescription());
        return ifcTemplate;
    }

    public static Map<String, GeneralClassTemplate> tOsFromMbe(
            ModuleMXBeanEntry mbe) {
        Map<String, GeneralClassTemplate> retVal = Maps.newHashMap();
        TOAttributesProcessor processor = new TOAttributesProcessor();
        processor.processAttributes(mbe.getAttributes());
        for (org.opendaylight.controller.config.yangjmxgenerator.plugin.ftl.TemplateFactory.TOAttributesProcessor.TOInternal to : processor
                .getTOs()) {
            List<Constructor> constructors = Lists.newArrayList();
            constructors.add(new Constructor(to.getName(), "super();"));

            Header header = getHeaderFromEntry(mbe);
            retVal.put(
                    to.getType(),
                    new GeneralClassTemplate(header, mbe.getPackageName(), to
                            .getName(), Collections.<String> emptyList(),
                            Collections.<String> emptyList(), to.getFields(),
                            to.getMethods(), false, false, constructors));
        }
        return retVal;
    }

    public static Map<String, GeneralClassTemplate> tOsFromRbe(
            RuntimeBeanEntry rbe) {
        Map<String, GeneralClassTemplate> retVal = Maps.newHashMap();
        TOAttributesProcessor processor = new TOAttributesProcessor();
        Map<String, AttributeIfc> yangPropertiesToTypesMap = Maps.newHashMap(rbe.getYangPropertiesToTypesMap());

        // Add TOs from output parameters
        for (Rpc rpc : rbe.getRpcs()) {
            AttributeIfc returnType = rpc.getReturnType();

            if (returnType == VoidAttribute.getInstance()) {
                continue;
            }
            if (returnType instanceof JavaAttribute) {
                continue;
            }
            if (returnType instanceof ListAttribute && returnType.getOpenType() instanceof SimpleType) {
                continue;
            }

            Preconditions.checkState(yangPropertiesToTypesMap.containsKey(returnType.getAttributeYangName()) == false,
                    "Duplicate TO %s for %s", returnType.getAttributeYangName(), rbe);
            yangPropertiesToTypesMap.put(returnType.getAttributeYangName(), returnType);
        }

        processor.processAttributes(yangPropertiesToTypesMap);
        for (org.opendaylight.controller.config.yangjmxgenerator.plugin.ftl.TemplateFactory.TOAttributesProcessor.TOInternal to : processor
                .getTOs()) {
            List<Constructor> constructors = Lists.newArrayList();
            constructors.add(new Constructor(to.getName(), "super();"));

            // TODO header
            retVal.put(
                    to.getType(),
                    new GeneralClassTemplate(null, rbe.getPackageName(), to
                            .getName(), Collections.<String> emptyList(),
                            Collections.<String> emptyList(), to.getFields(),
                            to.getMethods(), false, false, constructors));
        }
        return retVal;
    }

    private static Header getHeaderFromEntry(AbstractEntry mbe) {
        return new Header(mbe.getYangModuleName(), mbe.getYangModuleLocalname());
    }

    // TODO refactor attribute processors

    private static class TOAttributesProcessor {

        private final List<TOInternal> tos = Lists.newArrayList();

        void processAttributes(Map<String, AttributeIfc> attributes) {
            for (Entry<String, AttributeIfc> attrEntry : attributes.entrySet()) {
                AttributeIfc attributeIfc = attrEntry.getValue();
                if (attributeIfc instanceof TOAttribute) {
                    createTOInternal((TOAttribute) attributeIfc);
                }
                if (attributeIfc instanceof ListAttribute) {
                    AttributeIfc innerAttr = ((ListAttribute) attributeIfc)
                            .getInnerAttribute();
                    if (innerAttr instanceof TOAttribute) {
                        createTOInternal((TOAttribute) innerAttr);
                    }
                }
            }
        }

        private void createTOInternal(TOAttribute toAttribute) {

            Map<String, AttributeIfc> attrs = toAttribute.getCapitalizedPropertiesToTypesMap();
            // recursive processing of TO's attributes
            processAttributes(attrs);

            tos.add(new TOInternal(toAttribute.getType(), attrs));
        }

        List<TOInternal> getTOs() {
            return tos;
        }

        private static class TOInternal {
            private final String fullyQualifiedName, name;
            private List<Field> fields;
            private List<MethodDefinition> methods;

            public TOInternal(Type type, Map<String, AttributeIfc> attrs) {
                this(type.getFullyQualifiedName(), type.getName(), attrs, type.getPackageName());
            }

            public TOInternal(String fullyQualifiedName, String name,
                    Map<String, AttributeIfc> attrs, String packageName) {
                this.fullyQualifiedName = fullyQualifiedName;
                this.name = name;
                processAttrs(attrs, packageName);
            }

            private final static String dependencyResolverVarName = "dependencyResolver";
            private final static String dependencyResolverInjectMethodName = "injectDependencyResolver";

            private void processAttrs(Map<String, AttributeIfc> attrs, String packageName) {
                fields = Lists.newArrayList();
                methods = Lists.newArrayList();

                // FIXME conflict if "dependencyResolver" field from yang
                Field depRes = new Field(DependencyResolver.class.getName(), dependencyResolverVarName);
                fields.add(depRes);
                methods.add(new MethodDefinition("void", dependencyResolverInjectMethodName, Lists.newArrayList(depRes),
                        "this." + dependencyResolverVarName + " = " + dependencyResolverVarName + ";"));

                for (Entry<String, AttributeIfc> attrEntry : attrs.entrySet()) {
                    String innerName = attrEntry.getKey();
                    String varName = BindingGeneratorUtil
                            .parseToValidParamName(attrEntry.getKey());

                    String fullyQualifiedName, nullableDefault = null;
                    if (attrEntry.getValue() instanceof TypedAttribute) {
                        Type type = ((TypedAttribute) attrEntry.getValue()).getType();
                        if(attrEntry.getValue() instanceof JavaAttribute) {
                            nullableDefault = ((JavaAttribute)attrEntry.getValue()).getNullableDefaultWrappedForCode();
                            if(((JavaAttribute)attrEntry.getValue()).isIdentityRef()) {

                                String fieldType = serializeType(type, true);
                                String innerType = getInnerTypeFromIdentity(type);
                                methods.add(new MethodDefinition(fieldType, "resolve" + attrEntry.getKey(), Collections.<Field>emptyList(),
                                        "return " + varName + ".resolveIdentity(" + dependencyResolverVarName + "," +  innerType + ".class);"));
                                type = identityRefType;
                            }
                        }
                        fullyQualifiedName = serializeType(type);
                    } else {
                        fullyQualifiedName = FullyQualifiedNameHelper
                                .getFullyQualifiedName(packageName, attrEntry.getValue().getUpperCaseCammelCase());
                    }
                    fields.add(new Field(fullyQualifiedName, varName, nullableDefault, needsDepResolver(attrEntry.getValue())));

                    String getterName = "get" + innerName;
                    MethodDefinition getter = new MethodDefinition(
                            fullyQualifiedName, getterName,
                            Collections.<Field> emptyList(), "return "
                                    + varName + ";");

                    String setterName = "set" + innerName;
                    MethodDefinition setter = new MethodDefinition("void",
                            setterName, Lists.newArrayList(new Field(
                                    fullyQualifiedName, varName)), "this."
                                    + varName + " = " + varName + ";");
                    methods.add(getter);
                    methods.add(setter);
                }

            }

            String getType() {
                return fullyQualifiedName;
            }

            String getName() {
                return name;
            }

            List<Field> getFields() {
                return fields;
            }

            List<MethodDefinition> getMethods() {
                return methods;
            }
        }
    }


    private static class MXBeanInterfaceAttributesProcessor {
        private final List<MethodDeclaration> methods = Lists.newArrayList();

        void processAttributes(Map<String, AttributeIfc> attributes) {
            for (Entry<String, AttributeIfc> attrEntry : attributes.entrySet()) {
                String returnType;
                AttributeIfc attributeIfc = attrEntry.getValue();

                if (attributeIfc instanceof TypedAttribute) {
                    TypedAttribute typedAttribute = (TypedAttribute) attributeIfc;
                    returnType = serializeType(typedAttribute.getType());

                    if (attributeIfc instanceof JavaAttribute && ((JavaAttribute)attrEntry.getValue()).isIdentityRef()) {
                        returnType = serializeType(identityRefType);
                    }

                } else {
                    throw new UnsupportedOperationException(
                            "Attribute not supported: "
                                    + attributeIfc.getClass());
                }

                String getterName = "get"
                        + attributeIfc.getUpperCaseCammelCase();
                MethodDeclaration getter = new MethodDeclaration(returnType,
                        getterName, Collections.<Field> emptyList());

                String varName = BindingGeneratorUtil
                        .parseToValidParamName(attrEntry.getKey());
                String setterName = "set"
                        + attributeIfc.getUpperCaseCammelCase();
                MethodDeclaration setter = new MethodDeclaration("void",
                        setterName, Lists.newArrayList(new Field(returnType,
                                varName)));

                methods.add(getter);
                methods.add(setter);

                if (attributeIfc.getNullableDescription() != null) {
                    setter.setJavadoc(attrEntry.getValue()
                            .getNullableDescription());
                }
            }
        }

        List<MethodDeclaration> getMethods() {
            return methods;
        }
    }

    private static final Type identityRefType = new Type() {
        public final Class<IdentityAttributeRef> IDENTITY_ATTRIBUTE_REF_CLASS = IdentityAttributeRef.class;

        @Override
        public String getPackageName() {
            return IDENTITY_ATTRIBUTE_REF_CLASS.getPackage().getName();
        }

        @Override
        public String getName() {
            return IDENTITY_ATTRIBUTE_REF_CLASS.getSimpleName();
        }

        @Override
        public String getFullyQualifiedName() {
            return IDENTITY_ATTRIBUTE_REF_CLASS.getName();
        }
    };

    private static class AbstractFactoryAttributesProcessor {

        private final List<Field> fields = Lists.newArrayList();

        void processAttributes(Map<String, AttributeIfc> attributes,
                String packageName) {
            for (Entry<String, AttributeIfc> attrEntry : attributes.entrySet()) {
                String type;
                String nullableDefaultWrapped = null;
                AttributeIfc attributeIfc = attrEntry.getValue();

                if (attributeIfc instanceof TypedAttribute) {
                    TypedAttribute typedAttribute = (TypedAttribute) attributeIfc;
                    type = serializeType(typedAttribute.getType());
                } else {
                    throw new UnsupportedOperationException(
                            "Attribute not supported: "
                                    + attributeIfc.getClass());
                }

                fields.add(new Field(type, attributeIfc
                        .getUpperCaseCammelCase(), nullableDefaultWrapped));
            }
        }

        List<Field> getFields() {
            return fields;
        }
    }

    private static class AbstractModuleAttributesProcessor {
        private static class Holder {
            private final List<ModuleField> moduleFields;
            private final List<MethodDefinition> methods;

            private Holder(List<ModuleField> moduleFields, List<MethodDefinition> methods) {
                this.moduleFields = Collections.unmodifiableList(moduleFields);
                this.methods = Collections.unmodifiableList(methods);
            }
        }

        private final Holder holder;


        private AbstractModuleAttributesProcessor(Map<String, AttributeIfc> attributes) {
            this.holder = processAttributes(attributes);
        }

        private static Holder processAttributes(Map<String, AttributeIfc> attributes) {
            List<ModuleField> moduleFields = new ArrayList<>();
            List<MethodDefinition> methods = new ArrayList<>();
            for (Entry<String, AttributeIfc> attrEntry : attributes.entrySet()) {
                String type, nullableDefaultWrapped = null;
                AttributeIfc attributeIfc = attrEntry.getValue();
                boolean isIdentity = false;
                boolean needsDepResolver = needsDepResolver(attrEntry.getValue());

                if (attributeIfc instanceof TypedAttribute) {
                    TypedAttribute typedAttribute = (TypedAttribute) attributeIfc;
                    type = serializeType(typedAttribute.getType());
                    if (attributeIfc instanceof JavaAttribute) {
                        nullableDefaultWrapped = ((JavaAttribute) attributeIfc).getNullableDefaultWrappedForCode();
                        if(((JavaAttribute)attrEntry.getValue()).isIdentityRef()) {
                            isIdentity = true;
                            type = serializeType(typedAttribute.getType(), true);
                        }
                    }
                } else {
                    throw new UnsupportedOperationException(
                            "Attribute not supported: "
                                    + attributeIfc.getClass());
                }

                boolean isDependency = false;
                boolean isListOfDependencies = false;
                Dependency dependency = null;
                Annotation overrideAnnotation = new Annotation("Override",
                        Collections.<Parameter> emptyList());
                List<Annotation> annotations = Lists
                        .newArrayList(overrideAnnotation);

                if (attributeIfc instanceof AbstractDependencyAttribute) {
                    isDependency = true;
                    dependency = ((AbstractDependencyAttribute) attributeIfc)
                            .getDependency();
                    annotations.add(Annotation
                            .createRequireIfcAnnotation(dependency.getSie()));
                    if (attributeIfc instanceof ListDependenciesAttribute) {
                        isListOfDependencies = true;
                    }
                }

                String varName = BindingGeneratorUtil
                        .parseToValidParamName(attrEntry.getKey());

                ModuleField field;
                if (isIdentity) {
                    String identityBaseClass = getInnerTypeFromIdentity(((TypedAttribute) attributeIfc).getType());
                    IdentityRefModuleField identityField = new IdentityRefModuleField(type, varName,
                            attributeIfc.getUpperCaseCammelCase(), identityBaseClass);

                    String getterName = "get"
                            + attributeIfc.getUpperCaseCammelCase() + "Identity";
                    MethodDefinition additionalGetter = new MethodDefinition(type, getterName, Collections.<Field> emptyList(),
                            Collections.<Annotation> emptyList(), "return " + identityField.getIdentityClassName()
                                    + ";");
                    methods.add(additionalGetter);

                    String setterName = "set"
                            + attributeIfc.getUpperCaseCammelCase();

                    String setterBody = "this." + identityField.getIdentityClassName() + " = " + identityField.getIdentityClassName() + ";";
                    MethodDefinition additionalSetter = new MethodDefinition("void",
                            setterName,
                            Lists.newArrayList(new Field(type, identityField.getIdentityClassName())),
                            Collections.<Annotation> emptyList(), setterBody);
                    additionalSetter.setJavadoc(attributeIfc.getNullableDescription());

                    methods.add(additionalSetter);

                    type = serializeType(identityRefType);
                    field = identityField;
                } else {
                    field = new ModuleField(type, varName, attributeIfc.getUpperCaseCammelCase(),
                            nullableDefaultWrapped, isDependency, dependency, isListOfDependencies, needsDepResolver);
                }
                moduleFields.add(field);


                String getterName = "get"
                        + attributeIfc.getUpperCaseCammelCase();
                MethodDefinition getter = new MethodDefinition(type,
                        getterName, Collections.<Field> emptyList(),
                        Lists.newArrayList(overrideAnnotation), "return "
                        + varName + ";");

                methods.add(getter);

                String setterName = "set"
                        + attributeIfc.getUpperCaseCammelCase();

                if (attributeIfc.getNullableDescription() != null) {
                    annotations.add(Annotation
                            .createDescriptionAnnotation(attributeIfc.getNullableDescription()));
                }

                String setterBody = "this." + varName + " = " + varName + ";";
                if (isListOfDependencies) {
                    String nullCheck = String.format("if (%s == null) throw new IllegalArgumentException(\"Null not supported\");%n",
                            varName);
                    setterBody = nullCheck + setterBody;
                }
                MethodDefinition setter = new MethodDefinition("void",
                        setterName,
                        Lists.newArrayList(new Field(type, varName)),
                        annotations, setterBody);
                setter.setJavadoc(attributeIfc.getNullableDescription());

                methods.add(setter);
            }
            return new Holder(moduleFields, methods);
        }

        List<ModuleField> getModuleFields() {
            return holder.moduleFields;
        }

        List<MethodDefinition> getMethods() {
            return holder.methods;
        }

    }


    private static boolean needsDepResolver(AttributeIfc value) {
        if(value instanceof TOAttribute) {
            return true;
        }
        if(value instanceof ListAttribute) {
            AttributeIfc innerAttribute = ((ListAttribute) value).getInnerAttribute();
            return needsDepResolver(innerAttribute);
        }

        return false;
    }

    private static String getInnerTypeFromIdentity(Type type) {
        Preconditions.checkArgument(type instanceof ParameterizedType);
        Type[] args = ((ParameterizedType) type).getActualTypeArguments();
        Preconditions.checkArgument(args.length ==1);
        return serializeType(args[0]);
    }
}
