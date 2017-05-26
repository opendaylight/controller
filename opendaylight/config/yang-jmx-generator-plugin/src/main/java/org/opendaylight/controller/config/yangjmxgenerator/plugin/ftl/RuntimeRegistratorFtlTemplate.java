/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yangjmxgenerator.plugin.ftl;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static java.lang.String.format;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import java.io.Closeable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import javax.lang.model.element.Modifier;
import org.opendaylight.controller.config.api.runtime.HierarchicalRuntimeBeanRegistration;
import org.opendaylight.controller.config.api.runtime.RootRuntimeBeanRegistrator;
import org.opendaylight.controller.config.yangjmxgenerator.RuntimeBeanEntry;
import org.opendaylight.controller.config.yangjmxgenerator.plugin.ftl.model.Annotation;
import org.opendaylight.controller.config.yangjmxgenerator.plugin.ftl.model.Field;
import org.opendaylight.controller.config.yangjmxgenerator.plugin.ftl.model.MethodDefinition;
import org.opendaylight.controller.config.yangjmxgenerator.plugin.util.FullyQualifiedNameHelper;

public class RuntimeRegistratorFtlTemplate extends GeneralClassTemplate {

    private RuntimeRegistratorFtlTemplate(RuntimeBeanEntry runtimeBeanEntry,
            String name, List<Field> fields, List<MethodDefinition> methods) {
        // TODO header
        super(null, runtimeBeanEntry.getPackageName(), name, Collections
                .emptyList(), Collections.singletonList(Closeable.class
                .getCanonicalName()), fields, methods);
    }

    public static RuntimeBeanEntry findRoot(
            Collection<RuntimeBeanEntry> runtimeBeanEntries) {
        RuntimeBeanEntry result = null;
        for (RuntimeBeanEntry rb : runtimeBeanEntries) {
            if (rb.isRoot()) {
                if (result != null) {
                    throw new IllegalArgumentException(
                            "More than one root runtime bean found");
                }
                result = rb;
            }
        }
        if (result != null) {
            return result;
        }
        throw new IllegalArgumentException("No root runtime bean found");
    }

    private static String constructConstructorBody(
            List<Field> constructorParameters) {
        StringBuilder constructorBody = new StringBuilder();
        for (Field field : constructorParameters) {
            constructorBody.append("this.");
            constructorBody.append(field.getName());
            constructorBody.append("=");
            constructorBody.append(field.getName());
            constructorBody.append(";\n");
        }
        return constructorBody.toString();
    }

    // TODO Move to factory
    /**
     * Get registrator and n registration ftls where n is equal to total number
     * of runtime beans in hierarchy.
     */
    public static Map<String, FtlTemplate> create(RuntimeBeanEntry rootRB) {
        checkArgument(rootRB.isRoot(), "RuntimeBeanEntry must be root");
        String registratorName = getJavaNameOfRuntimeRegistrator(rootRB);
        List<MethodDefinition> methods = new ArrayList<>();
        Field rootRuntimeBeanRegistratorField = new Field(
                Collections.singletonList(Modifier.FINAL),
                RootRuntimeBeanRegistrator.class.getName(),
                "rootRuntimeBeanRegistrator");
        List<Field> constructorParameters = Lists
                .newArrayList(rootRuntimeBeanRegistratorField);
        String constructorBody = constructConstructorBody(constructorParameters);
        MethodDefinition constructor = MethodDefinition.createConstructor(
                registratorName, constructorParameters, constructorBody);
        methods.add(constructor);

        LinkedHashMap<String, RuntimeRegistratorFtlTemplate> RuntimeRegistratorFtlTemplates =
                createRegistrationHierarchy(rootRB, Collections.emptySet());
        RuntimeRegistratorFtlTemplate rootFtlFile = RuntimeRegistratorFtlTemplates
                .values().iterator().next();

        {// add register(rootruntimemxbean)
            String fullyQualifiedNameOfMXBean = FullyQualifiedNameHelper
                    .getFullyQualifiedName(rootRB.getPackageName(), rootRB.getJavaNameOfRuntimeMXBean());
            String childRegistratorFQN = rootFtlFile.getFullyQualifiedName();
            Field rbParameter = new Field(fullyQualifiedNameOfMXBean, "rb");
            StringBuilder registerBody = new StringBuilder();
            registerBody.append(format("%s %s = this.%s.registerRoot(%s);\n",
                    HierarchicalRuntimeBeanRegistration.class
                            .getCanonicalName(), hierachicalRegistration
                            .getName(), rootRuntimeBeanRegistratorField
                            .getName(), rbParameter.getName()));
            registerBody.append(format("return new %s(%s);\n",
                    rootFtlFile.getFullyQualifiedName(),
                    hierachicalRegistration.getName()));

            MethodDefinition registerMethod = new MethodDefinition(
                    childRegistratorFQN, "register",
                    Collections.singletonList(rbParameter), registerBody.toString());
            methods.add(registerMethod);
        }

        MethodDefinition closeRegistrator = createCloseMethodToCloseField(rootRuntimeBeanRegistratorField);
        methods.add(closeRegistrator);

        // TODO add header
        GeneralClassTemplate registrator = new GeneralClassTemplate(null,
                rootRB.getPackageName(), registratorName,
                Collections.emptyList(), Collections.singletonList(Closeable.class
                .getCanonicalName()), constructorParameters, methods);

        checkState(!RuntimeRegistratorFtlTemplates.containsKey(registrator
                .getTypeDeclaration().getName()), "Name conflict: "
                + registrator.getTypeDeclaration().getName());
        Map<String, FtlTemplate> result = new HashMap<>();
        result.putAll(RuntimeRegistratorFtlTemplates);
        result.put(registrator.getTypeDeclaration().getName(), registrator);
        return result;
    }

    private static Field hierachicalRegistration = new Field(
            Collections.singletonList(Modifier.FINAL),
            HierarchicalRuntimeBeanRegistration.class.getCanonicalName(),
            "registration");

    // TODO move to factory + RuntimeBeanEntry
    /**
     * Create ftls representing registrations. First registration is represents
     * parent.
     *
     * @return map containing java class name as key, instance representing the
     *         java file as value.
     */
    private static LinkedHashMap<String, RuntimeRegistratorFtlTemplate> createRegistrationHierarchy(
            RuntimeBeanEntry parent, Set<String> occupiedKeys) {
        LinkedHashMap<String, RuntimeRegistratorFtlTemplate> unorderedResult = new LinkedHashMap<>();
        List<MethodDefinition> methods = new ArrayList<>();

        // hierarchy of ON is created as follows:
        // root RB: <domain>, type=RuntimeBean
        // 1st RB in hierarchy: <domain>, type=RuntimeBean, <java name of leaf
        // list>: key or counter
        // n-th RB in hierarchy has same ON as n-1, with added <java name of
        // leaf list>: key or counter
        if (occupiedKeys.contains(parent.getJavaNamePrefix())) {
            throw new IllegalArgumentException(
                    "Name conflict in runtime bean hierarchy - java name found more than "
                            + "once. Consider using java-name extension. Conflicting name: "
                            + parent.getJavaNamePrefix());
        }
        Set<String> currentOccupiedKeys = new HashSet<>(occupiedKeys);
        currentOccupiedKeys.add(parent.getJavaNamePrefix());

        Field registratorsMapField = new Field(Collections.singletonList(Modifier.FINAL),
                TypeHelper.getGenericType(Map.class, String.class,
                        AtomicInteger.class), "unkeyedMap", "new "
                        + TypeHelper.getGenericType(HashMap.class,
                                String.class, AtomicInteger.class) + "()");

        // create register methods for children
        for (RuntimeBeanEntry child : parent.getChildren()) {
            checkArgument(parent.getPackageName()
                    .equals(child.getPackageName()), "Invalid package name");

            // call itself recursively to generate child
            // registrators/registrations
            LinkedHashMap<String, RuntimeRegistratorFtlTemplate> childRegistratorMap = createRegistrationHierarchy(
                    child, currentOccupiedKeys);
            for (Entry<String, RuntimeRegistratorFtlTemplate> entry : childRegistratorMap
                    .entrySet()) {
                if (unorderedResult.containsKey(entry.getKey())) {
                    throw new IllegalStateException(
                            "Conflicting name found while generating runtime registration:"
                                    + entry.getKey());
                }
                unorderedResult.put(entry.getKey(), entry.getValue());
            }

            if (!childRegistratorMap.isEmpty()) {
                // first entry is the direct descendant according to the create
                // contract
                RuntimeRegistratorFtlTemplate childRegistrator = childRegistratorMap
                        .values().iterator().next();
                StringBuilder body = new StringBuilder();
                String key, value;
                key = child.getJavaNamePrefix();
                body.append(format(
                        "String key = \"%s\"; //TODO: check for conflicts\n",
                        key));

                Optional<String> childKeyJavaName = child.getKeyJavaName();
                if (childKeyJavaName.isPresent()) {
                    value = "bean.get" + childKeyJavaName.get() + "()";
                    value = "String.valueOf(" + value + ")";
                } else {
                    body.append("java.util.concurrent.atomic.AtomicInteger counter = unkeyedMap.get(key);\n"
                            + "if (counter==null){\n"
                            + "counter = new java.util.concurrent.atomic.AtomicInteger();\n"
                            + "unkeyedMap.put(key, counter);\n" + "}\n");
                    value = "String.valueOf(counter.incrementAndGet())";
                }
                body.append(format("String value = %s;\n", value));
                body.append(format("%s r = %s.register(key, value, bean);\n",
                        HierarchicalRuntimeBeanRegistration.class
                                .getCanonicalName(), hierachicalRegistration
                                .getName()));
                body.append(format("return new %s(r);",
                        childRegistrator.getFullyQualifiedName()));

                Field param = new Field(Collections.singletonList(Modifier.FINAL),
                        child.getJavaNameOfRuntimeMXBean(), "bean");
                MethodDefinition register = new MethodDefinition(
                        Collections.singletonList(Modifier.SYNCHRONIZED),
                        childRegistrator.getFullyQualifiedName(), "register",
                        Collections.singletonList(param), Collections.emptyList(),
                        Collections.emptyList(), body.toString());
                methods.add(register);

            }
        }

        // create parent registration
        String createdName = getJavaNameOfRuntimeRegistration(parent.getJavaNamePrefix());

        List<Field> constructorParameters = Collections.singletonList(hierachicalRegistration);
        String constructorBody = constructConstructorBody(constructorParameters);

        MethodDefinition constructor = MethodDefinition.createConstructor(
                createdName, constructorParameters, constructorBody);

        MethodDefinition closeRegistrator = createCloseMethodToCloseField(hierachicalRegistration);
        methods.add(closeRegistrator);
        methods.add(constructor);
        List<Field> privateFields = Lists.newArrayList(registratorsMapField);
        privateFields.addAll(constructorParameters);

        RuntimeRegistratorFtlTemplate created = new RuntimeRegistratorFtlTemplate(
                parent, createdName, privateFields, methods);

        LinkedHashMap<String, RuntimeRegistratorFtlTemplate> result = new LinkedHashMap<>();
        result.put(created.getTypeDeclaration().getName(), created);
        checkState(!unorderedResult.containsKey(created.getTypeDeclaration()
                .getName()), "Naming conflict: "
                + created.getTypeDeclaration().getName());
        result.putAll(unorderedResult);
        return result;
    }

    private static MethodDefinition createCloseMethodToCloseField(Field field) {
        String body = field.getName() + ".close();";
        // TODO Thrown exception breaks build
        // return new MethodDefinition(Collections.<String> emptyList(), "void",
        // "close", Collections.<Field> emptyList(),
        // Arrays.asList(IOException.class.getCanonicalName()),
        // Collections.<Annotation> emptyList(), body);
        List<Annotation> annotations = Lists.newArrayList(new Annotation(
                "Override", Collections.emptyList()));
        return new MethodDefinition(Collections.emptyList(), "void",
                "close", Collections.emptyList(),
                Collections.emptyList(), annotations, body);
    }

    @VisibleForTesting
    public static String getJavaNameOfRuntimeRegistration(String javaNamePrefix) {
        return javaNamePrefix + "RuntimeRegistration";
    }

    public static String getJavaNameOfRuntimeRegistrator(RuntimeBeanEntry rootRB) {
        checkArgument(rootRB.isRoot(), "RuntimeBeanEntry must be root");
        return rootRB.getJavaNamePrefix() + "RuntimeRegistrator";
    }
}
