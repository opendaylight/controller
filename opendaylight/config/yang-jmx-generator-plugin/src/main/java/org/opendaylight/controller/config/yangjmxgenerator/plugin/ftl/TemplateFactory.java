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
import org.opendaylight.mdsal.binding.model.api.ParameterizedType;
import org.opendaylight.mdsal.binding.model.api.Type;
import org.opendaylight.yangtools.yang.binding.BindingMapping;

public class TemplateFactory {

    /**
     * Get map of file name as key, FtlFile instance representing runtime mx
     * bean as value that should be persisted from this instance.
     */
    public static Map<String, FtlTemplate> getTOAndMXInterfaceFtlFiles(
            final RuntimeBeanEntry entry) {
        final Map<String, FtlTemplate> result = new HashMap<>();
        { // create GeneralInterfaceFtlFile for runtime MXBean. Attributes will
          // be transformed to getter methods
            final String mxBeanTypeName = entry.getJavaNameOfRuntimeMXBean();
            final List<String> extendedInterfaces = Collections.singletonList(RuntimeBean.class
                    .getCanonicalName());
            final List<MethodDeclaration> methods = new ArrayList<>();

            // convert attributes to getters
            for (final AttributeIfc attributeIfc : entry.getAttributes()) {
                String returnType;
                returnType = getReturnType(attributeIfc);
                final String getterName = "get"
                        + attributeIfc.getUpperCaseCammelCase();
                final MethodDeclaration getter = new MethodDeclaration(returnType,
                        getterName, Collections.<Field> emptyList());
                methods.add(getter);
            }

            // add rpc methods
            for (final Rpc rpc : entry.getRpcs()) {
                // convert JavaAttribute parameters into fields
                final List<Field> fields = new ArrayList<>();
                for (final JavaAttribute ja : rpc.getParameters()) {
                    final Field field = new Field(Collections.emptyList(),
                            ja.getType().getFullyQualifiedName(),
                            ja.getLowerCaseCammelCase(), ja.getNullableDefaultWrappedForCode());
                    fields.add(field);
                }
                final MethodDeclaration operation = new MethodDeclaration(
                        getReturnType(rpc.getReturnType()), rpc.getName(), fields);
                methods.add(operation);
            }

            // FIXME header
            final GeneralInterfaceTemplate runtimeMxBeanIfc = new GeneralInterfaceTemplate(
                    null, entry.getPackageName(), mxBeanTypeName,
                    extendedInterfaces, methods);

            result.put(runtimeMxBeanIfc.getTypeDeclaration().getName()
                    + ".java", runtimeMxBeanIfc);
        }

        result.putAll(TemplateFactory.tOsFromRbe(entry));

        return result;
    }

    // FIXME: put into Type.toString
    static String serializeType(final Type type, final boolean addWildcards) {
        if (type instanceof ParameterizedType){
            final ParameterizedType parameterizedType = (ParameterizedType) type;
            final StringBuilder sb = new StringBuilder();
            sb.append(parameterizedType.getRawType().getFullyQualifiedName());
            sb.append(addWildcards ? "<? extends " : "<");
            boolean first = true;
            for(final Type parameter: parameterizedType.getActualTypeArguments()) {
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

    static String serializeType(final Type type) {
        return serializeType(type, false);
    }

    private static String getReturnType(final AttributeIfc attributeIfc) {
        String returnType;
        if (attributeIfc instanceof TypedAttribute) {
            final Type type = ((TypedAttribute) attributeIfc).getType();
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
            final ServiceInterfaceEntry sie) {

        final List<String> extendedInterfaces = Lists
                .newArrayList(AbstractServiceInterface.class.getCanonicalName());
        if (sie.getBase().isPresent()) {
            extendedInterfaces.add(sie.getBase().get().getFullyQualifiedName());
        }

        // FIXME header
        final GeneralInterfaceTemplate sieTemplate = new GeneralInterfaceTemplate(
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
            final ModuleMXBeanEntry mbe) {
        final AbstractFactoryAttributesProcessor attrProcessor = new AbstractFactoryAttributesProcessor();
        attrProcessor.processAttributes(mbe.getAttributes());



        return new AbstractFactoryTemplate(getHeaderFromEntry(mbe),
                mbe.getPackageName(), mbe.getAbstractFactoryName(),
                attrProcessor.getFields()
        );
    }

    public static AbstractModuleTemplate abstractModuleTemplateFromMbe(
            final ModuleMXBeanEntry mbe) {
        final AbstractModuleAttributesProcessor attrProcessor = new AbstractModuleAttributesProcessor(mbe.getAttributes());

        final List<ModuleField> moduleFields = attrProcessor.getModuleFields();
        final List<String> implementedIfcs = Lists.newArrayList(
                mbe.getFullyQualifiedName(mbe.getMXBeanInterfaceName()));

        for (final String implementedService : mbe.getProvidedServices().keySet()) {
            implementedIfcs.add(implementedService);
        }

        boolean generateRuntime = false;
        String registratorFullyQualifiedName = null;
        if ((mbe.getRuntimeBeans() != null)
                && !mbe.getRuntimeBeans().isEmpty()) {
            generateRuntime = true;
            final RuntimeBeanEntry rootEntry = RuntimeRegistratorFtlTemplate
                    .findRoot(mbe.getRuntimeBeans());
            registratorFullyQualifiedName = rootEntry
                    .getPackageName()
                    .concat(".")
                    .concat(RuntimeRegistratorFtlTemplate.getJavaNameOfRuntimeRegistrator(rootEntry));
            implementedIfcs.add(RuntimeBeanRegistratorAwareModule.class
                    .getCanonicalName());
        }

        final List<String> extendedClasses = Collections.singletonList(AbstractModule.class.getCanonicalName() + "<" + mbe.getAbstractModuleName() + ">");

        final AbstractModuleTemplate abstractModuleTemplate = new AbstractModuleTemplate(
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
            final ModuleMXBeanEntry mbe) {
        return new StubFactoryTemplate(getHeaderFromEntry(mbe),
                mbe.getPackageName(), mbe.getStubFactoryName(),
                mbe.getFullyQualifiedName(mbe.getAbstractFactoryName())
        );
    }

    public static GeneralInterfaceTemplate mXBeanInterfaceTemplateFromMbe(
            final ModuleMXBeanEntry mbe) {
        final MXBeanInterfaceAttributesProcessor attrProcessor = new MXBeanInterfaceAttributesProcessor();
        attrProcessor.processAttributes(mbe.getAttributes());
        final GeneralInterfaceTemplate ifcTemplate = new GeneralInterfaceTemplate(
                getHeaderFromEntry(mbe), mbe.getPackageName(),
                mbe.getMXBeanInterfaceName(), Lists.<String> newArrayList(),
                attrProcessor.getMethods());
        ifcTemplate.setJavadoc(mbe.getNullableDescription());
        return ifcTemplate;
    }

    public static Map<String, GeneralClassTemplate> tOsFromMbe(
            final ModuleMXBeanEntry mbe) {
        final Map<String, GeneralClassTemplate> retVal = Maps.newHashMap();
        final TOAttributesProcessor processor = new TOAttributesProcessor();
        processor.processAttributes(mbe.getAttributes());
        for (final org.opendaylight.controller.config.yangjmxgenerator.plugin.ftl.TemplateFactory.TOAttributesProcessor.TOInternal to : processor
                .getTOs()) {
            final List<Constructor> constructors = Lists.newArrayList();
            constructors.add(new Constructor(to.getName(), "super();"));

            final Header header = getHeaderFromEntry(mbe);
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
            final RuntimeBeanEntry rbe) {
        final Map<String, GeneralClassTemplate> retVal = Maps.newHashMap();
        final TOAttributesProcessor processor = new TOAttributesProcessor();
        final Map<String, AttributeIfc> yangPropertiesToTypesMap = Maps.newHashMap(rbe.getYangPropertiesToTypesMap());

        // Add TOs from output parameters
        for (final Rpc rpc : rbe.getRpcs()) {
            final AttributeIfc returnType = rpc.getReturnType();

            if (returnType == VoidAttribute.getInstance()) {
                continue;
            }
            if (returnType instanceof JavaAttribute) {
                continue;
            }
            if ((returnType instanceof ListAttribute) && (returnType.getOpenType() instanceof SimpleType)) {
                continue;
            }

            Preconditions.checkState(!yangPropertiesToTypesMap.containsKey(returnType.getAttributeYangName()),
                    "Duplicate TO %s for %s", returnType.getAttributeYangName(), rbe);
            yangPropertiesToTypesMap.put(returnType.getAttributeYangName(), returnType);
        }

        processor.processAttributes(yangPropertiesToTypesMap);
        for (final org.opendaylight.controller.config.yangjmxgenerator.plugin.ftl.TemplateFactory.TOAttributesProcessor.TOInternal to : processor
                .getTOs()) {
            final List<Constructor> constructors = Lists.newArrayList();
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

    private static Header getHeaderFromEntry(final AbstractEntry mbe) {
        return new Header(mbe.getYangModuleName(), mbe.getYangModuleLocalname());
    }

    // TODO refactor attribute processors

    private static class TOAttributesProcessor {

        private final List<TOInternal> tos = Lists.newArrayList();

        void processAttributes(final Map<String, AttributeIfc> attributes) {
            for (final Entry<String, AttributeIfc> attrEntry : attributes.entrySet()) {
                final AttributeIfc attributeIfc = attrEntry.getValue();
                if (attributeIfc instanceof TOAttribute) {
                    createTOInternal((TOAttribute) attributeIfc);
                }
                if (attributeIfc instanceof ListAttribute) {
                    final AttributeIfc innerAttr = ((ListAttribute) attributeIfc)
                            .getInnerAttribute();
                    if (innerAttr instanceof TOAttribute) {
                        createTOInternal((TOAttribute) innerAttr);
                    }
                }
            }
        }

        private void createTOInternal(final TOAttribute toAttribute) {

            final Map<String, AttributeIfc> attrs = toAttribute.getCapitalizedPropertiesToTypesMap();
            // recursive processing of TO's attributes
            processAttributes(attrs);

            this.tos.add(new TOInternal(toAttribute.getType(), attrs));
        }

        List<TOInternal> getTOs() {
            return this.tos;
        }

        private static class TOInternal {
            private final String fullyQualifiedName, name;
            private List<Field> fields;
            private List<MethodDefinition> methods;

            public TOInternal(final Type type, final Map<String, AttributeIfc> attrs) {
                this(type.getFullyQualifiedName(), type.getName(), attrs, type.getPackageName());
            }

            public TOInternal(final String fullyQualifiedName, final String name,
                    final Map<String, AttributeIfc> attrs, final String packageName) {
                this.fullyQualifiedName = fullyQualifiedName;
                this.name = name;
                processAttrs(attrs, packageName);
            }

            private final static String dependencyResolverVarName = "dependencyResolver";
            private final static String dependencyResolverInjectMethodName = "injectDependencyResolver";

            private void processAttrs(final Map<String, AttributeIfc> attrs, final String packageName) {
                this.fields = Lists.newArrayList();
                this.methods = Lists.newArrayList();

                // FIXME conflict if "dependencyResolver" field from yang
                final Field depRes = new Field(DependencyResolver.class.getName(), dependencyResolverVarName);
                this.fields.add(depRes);
                this.methods.add(new MethodDefinition("void", dependencyResolverInjectMethodName, Lists.newArrayList(depRes),
                        "this." + dependencyResolverVarName + " = " + dependencyResolverVarName + ";"));

                for (final Entry<String, AttributeIfc> attrEntry : attrs.entrySet()) {
                    final String innerName = attrEntry.getKey();
                    final String varName = BindingMapping.getPropertyName(attrEntry.getKey());

                    String fullyQualifiedName, nullableDefault = null;
                    if (attrEntry.getValue() instanceof TypedAttribute) {
                        Type type = ((TypedAttribute) attrEntry.getValue()).getType();
                        if(attrEntry.getValue() instanceof JavaAttribute) {
                            nullableDefault = ((JavaAttribute)attrEntry.getValue()).getNullableDefaultWrappedForCode();
                            if(((JavaAttribute)attrEntry.getValue()).isIdentityRef()) {

                                final String fieldType = serializeType(type, true);
                                final String innerType = getInnerTypeFromIdentity(type);
                                this.methods.add(new MethodDefinition(fieldType, "resolve" + attrEntry.getKey(), Collections.<Field>emptyList(),
                                        "return " + varName + ".resolveIdentity(" + dependencyResolverVarName + "," +  innerType + ".class);"));
                                type = identityRefType;
                            }
                        }
                        fullyQualifiedName = serializeType(type);
                    } else {
                        fullyQualifiedName = FullyQualifiedNameHelper
                                .getFullyQualifiedName(packageName, attrEntry.getValue().getUpperCaseCammelCase());
                    }
                    this.fields.add(new Field(fullyQualifiedName, varName, nullableDefault, needsDepResolver(attrEntry.getValue())));

                    final String getterName = "get" + innerName;
                    final MethodDefinition getter = new MethodDefinition(
                            fullyQualifiedName, getterName,
                            Collections.<Field> emptyList(), "return "
                                    + varName + ";");

                    final String setterName = "set" + innerName;
                    final MethodDefinition setter = new MethodDefinition("void",
                            setterName, Lists.newArrayList(new Field(
                                    fullyQualifiedName, varName)), "this."
                                    + varName + " = " + varName + ";");
                    this.methods.add(getter);
                    this.methods.add(setter);
                }

                // Add hashCode
                final MethodDefinition hashCode = getHash(attrs);
                this.methods.add(hashCode);

                // Add equals
                final MethodDefinition equals = getEquals(attrs);
                this.methods.add(equals);
            }

            private MethodDefinition getEquals(final Map<String, AttributeIfc> attrs) {
                final StringBuilder equalsBodyBuilder = new StringBuilder(
                        "        if (this == o) { return true; }\n" +
                        "        if (o == null || getClass() != o.getClass()) { return false; }\n");
                equalsBodyBuilder.append(String.format(
                        "        final %s that = (%s) o;\n", this.name, this.name));
                for (final AttributeIfc s : attrs.values()) {
                    equalsBodyBuilder.append(String.format(
                            "        if (!java.util.Objects.equals(%1$s, that.%1$s)) {\n" +
                            "            return false;\n" +
                            "        }\n\n", s.getLowerCaseCammelCase()));
                }
                equalsBodyBuilder.append(
                        "       return true;\n");
                return new MethodDefinition("boolean", "equals", Collections.singletonList(new Field("Object", "o")),
                        Collections.singletonList(new Annotation("Override", Collections.<Parameter>emptyList())), equalsBodyBuilder.toString());
            }

            private static MethodDefinition getHash(final Map<String, AttributeIfc> attrs) {
                final StringBuilder hashBodyBuilder = new StringBuilder(
                        "        return java.util.Objects.hash(");
                for (final AttributeIfc s : attrs.values()) {
                    hashBodyBuilder.append(s.getLowerCaseCammelCase());
                    hashBodyBuilder.append(", ");
                }
                hashBodyBuilder.replace(hashBodyBuilder.length() - 2, hashBodyBuilder.length(), ");\n");
                return new MethodDefinition("int", "hashCode", Collections.<Field>emptyList(),
                        Collections.singletonList(new Annotation("Override", Collections.<Parameter>emptyList())), hashBodyBuilder.toString());
            }

            String getType() {
                return this.fullyQualifiedName;
            }

            String getName() {
                return this.name;
            }

            List<Field> getFields() {
                return this.fields;
            }

            List<MethodDefinition> getMethods() {
                return this.methods;
            }
        }
    }


    private static class MXBeanInterfaceAttributesProcessor {
        private final List<MethodDeclaration> methods = Lists.newArrayList();

        void processAttributes(final Map<String, AttributeIfc> attributes) {
            for (final Entry<String, AttributeIfc> attrEntry : attributes.entrySet()) {
                String returnType;
                final AttributeIfc attributeIfc = attrEntry.getValue();

                if (attributeIfc instanceof TypedAttribute) {
                    final TypedAttribute typedAttribute = (TypedAttribute) attributeIfc;
                    returnType = serializeType(typedAttribute.getType());

                    if ((attributeIfc instanceof JavaAttribute) && ((JavaAttribute)attrEntry.getValue()).isIdentityRef()) {
                        returnType = serializeType(identityRefType);
                    }

                } else {
                    throw new UnsupportedOperationException(
                            "Attribute not supported: "
                                    + attributeIfc.getClass());
                }

                final String getterName = "get"
                        + attributeIfc.getUpperCaseCammelCase();
                final MethodDeclaration getter = new MethodDeclaration(returnType,
                        getterName, Collections.<Field> emptyList());

                final String varName = BindingMapping.getPropertyName(attrEntry.getKey());
                final String setterName = "set"
                        + attributeIfc.getUpperCaseCammelCase();
                final MethodDeclaration setter = new MethodDeclaration("void",
                        setterName, Lists.newArrayList(new Field(returnType,
                                varName)));

                this.methods.add(getter);
                this.methods.add(setter);

                if (attributeIfc.getNullableDescription() != null) {
                    setter.setJavadoc(attrEntry.getValue()
                            .getNullableDescription());
                }
            }
        }

        List<MethodDeclaration> getMethods() {
            return this.methods;
        }
    }

    private static final Type identityRefType = new Type() {
        public final Class<IdentityAttributeRef> IDENTITY_ATTRIBUTE_REF_CLASS = IdentityAttributeRef.class;

        @Override
        public String getPackageName() {
            return this.IDENTITY_ATTRIBUTE_REF_CLASS.getPackage().getName();
        }

        @Override
        public String getName() {
            return this.IDENTITY_ATTRIBUTE_REF_CLASS.getSimpleName();
        }

        @Override
        public String getFullyQualifiedName() {
            return this.IDENTITY_ATTRIBUTE_REF_CLASS.getName();
        }
    };

    private static class AbstractFactoryAttributesProcessor {

        private final List<Field> fields = Lists.newArrayList();

        void processAttributes(final Map<String, AttributeIfc> attributes) {
            for (final AttributeIfc attributeIfc : attributes.values()) {
                if (attributeIfc instanceof TypedAttribute) {
                    final TypedAttribute typedAttribute = (TypedAttribute) attributeIfc;
                    final String type = serializeType(typedAttribute.getType());

                    this.fields.add(new Field(type, attributeIfc
                            .getUpperCaseCammelCase(), null));
                } else {
                    throw new UnsupportedOperationException(
                            "Attribute not supported: "
                                    + attributeIfc.getClass());
                }
            }
        }

        List<Field> getFields() {
            return this.fields;
        }
    }

    private static class AbstractModuleAttributesProcessor {
        private static class Holder {
            private final List<ModuleField> moduleFields;
            private final List<MethodDefinition> methods;

            private Holder(final List<ModuleField> moduleFields, final List<MethodDefinition> methods) {
                this.moduleFields = Collections.unmodifiableList(moduleFields);
                this.methods = Collections.unmodifiableList(methods);
            }
        }

        private final Holder holder;


        private AbstractModuleAttributesProcessor(final Map<String, AttributeIfc> attributes) {
            this.holder = processAttributes(attributes);
        }

        private static Holder processAttributes(final Map<String, AttributeIfc> attributes) {
            final List<ModuleField> moduleFields = new ArrayList<>();
            final List<MethodDefinition> methods = new ArrayList<>();
            for (final Entry<String, AttributeIfc> attrEntry : attributes.entrySet()) {
                String type, nullableDefaultWrapped = null;
                final AttributeIfc attributeIfc = attrEntry.getValue();
                boolean isIdentity = false;
                final boolean needsDepResolver = needsDepResolver(attrEntry.getValue());

                if (attributeIfc instanceof TypedAttribute) {
                    final TypedAttribute typedAttribute = (TypedAttribute) attributeIfc;
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
                final Annotation overrideAnnotation = new Annotation("Override",
                        Collections.<Parameter> emptyList());
                final List<Annotation> annotations = Lists
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

                final String varName = BindingMapping.getPropertyName(attrEntry.getKey());

                ModuleField field;
                if (isIdentity) {
                    final String identityBaseClass = getInnerTypeFromIdentity(((TypedAttribute) attributeIfc).getType());
                    final IdentityRefModuleField identityField = new IdentityRefModuleField(type, varName,
                            attributeIfc.getUpperCaseCammelCase(), identityBaseClass);

                    final String getterName = "get"
                            + attributeIfc.getUpperCaseCammelCase() + "Identity";
                    final MethodDefinition additionalGetter = new MethodDefinition(type, getterName, Collections.<Field> emptyList(),
                            Collections.<Annotation> emptyList(), "return " + identityField.getIdentityClassName()
                                    + ";");
                    methods.add(additionalGetter);

                    final String setterName = "set"
                            + attributeIfc.getUpperCaseCammelCase();

                    final String setterBody = "this." + identityField.getIdentityClassName() + " = " + identityField.getIdentityClassName() + ";";
                    final MethodDefinition additionalSetter = new MethodDefinition("void",
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


                final String getterName = "get"
                        + attributeIfc.getUpperCaseCammelCase();
                final MethodDefinition getter = new MethodDefinition(type,
                        getterName, Collections.<Field> emptyList(),
                        Lists.newArrayList(overrideAnnotation), "return "
                        + varName + ";");

                methods.add(getter);

                final String setterName = "set"
                        + attributeIfc.getUpperCaseCammelCase();

                if (attributeIfc.getNullableDescription() != null) {
                    annotations.add(Annotation
                            .createDescriptionAnnotation(attributeIfc.getNullableDescription()));
                }

                String setterBody = "this." + varName + " = " + varName + ";";
                if (isListOfDependencies) {
                    final String nullCheck = String.format("if (%s == null) {\n%s = new java.util.ArrayList<>(); \n}%n",
                            varName, varName);
                    setterBody = nullCheck + setterBody;
                }
                final MethodDefinition setter = new MethodDefinition("void",
                        setterName,
                        Lists.newArrayList(new Field(type, varName)),
                        annotations, setterBody);
                setter.setJavadoc(attributeIfc.getNullableDescription());

                methods.add(setter);
            }
            return new Holder(moduleFields, methods);
        }

        List<ModuleField> getModuleFields() {
            return this.holder.moduleFields;
        }

        List<MethodDefinition> getMethods() {
            return this.holder.methods;
        }

    }


    private static boolean needsDepResolver(final AttributeIfc value) {
        if(value instanceof TOAttribute) {
            return true;
        }
        if(value instanceof ListAttribute) {
            final AttributeIfc innerAttribute = ((ListAttribute) value).getInnerAttribute();
            return needsDepResolver(innerAttribute);
        }

        return false;
    }

    private static String getInnerTypeFromIdentity(final Type type) {
        Preconditions.checkArgument(type instanceof ParameterizedType);
        final Type[] args = ((ParameterizedType) type).getActualTypeArguments();
        Preconditions.checkArgument(args.length ==1);
        return serializeType(args[0]);
    }
}
