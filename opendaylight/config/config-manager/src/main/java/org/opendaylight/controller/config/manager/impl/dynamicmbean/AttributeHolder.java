/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.manager.impl.dynamicmbean;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import javax.management.MBeanAttributeInfo;
import javax.management.ObjectName;
import org.opendaylight.controller.config.api.annotations.Description;
import org.opendaylight.controller.config.api.annotations.RequireInterface;

@Immutable
class AttributeHolder {

    private final String name;
    private final String description;
    private final Object object;
    private final boolean writable;

    @Nullable
    private final RequireInterface requireInterfaceAnnotation;
    private final String attributeType;

    public static final Set<Class<?>> PERMITTED_PARAMETER_TYPES_FOR_DEPENDENCY_SETTER = new HashSet<>();

    static {
        PERMITTED_PARAMETER_TYPES_FOR_DEPENDENCY_SETTER.add(ObjectName.class);
        PERMITTED_PARAMETER_TYPES_FOR_DEPENDENCY_SETTER.add(ObjectName[].class);
        PERMITTED_PARAMETER_TYPES_FOR_DEPENDENCY_SETTER.add(List.class);
    }

    public AttributeHolder(String name, Object object, String returnType,
                           boolean writable,
                           @Nullable RequireInterface requireInterfaceAnnotation,
                           String description) {
        if (name == null) {
            throw new NullPointerException();
        }
        this.name = name;
        if (object == null) {
            throw new NullPointerException();
        }
        this.object = object;
        this.writable = writable;
        this.requireInterfaceAnnotation = requireInterfaceAnnotation;
        this.attributeType = returnType;
        this.description = description;
    }

    public MBeanAttributeInfo toMBeanAttributeInfo() {
        MBeanAttributeInfo info = new MBeanAttributeInfo(name, attributeType,
                description, true, true, false);
        return info;
    }

    /**
     * @return annotation if setter sets ObjectName or ObjectName[], and is
     * annotated. Return null otherwise.
     */
    RequireInterface getRequireInterfaceOrNull() {
        return requireInterfaceAnnotation;
    }

    public String getName() {
        return name;
    }

    public Object getObject() {
        return object;
    }

    public String getAttributeType() {
        return attributeType;
    }

    public boolean isWritable() {
        return writable;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Find @Description annotations in method class and all its exported
     * interfaces.
     *
     * @param setter
     * @param jmxInterfaces
     * @return empty string if no annotation is found, or list of descriptions
     * separated by newline
     */
    static String findDescription(Method setter, Set<Class<?>> jmxInterfaces) {
        List<Description> descriptions = AnnotationsHelper
                .findMethodAnnotationInSuperClassesAndIfcs(setter, Description.class, jmxInterfaces);
        return AnnotationsHelper.aggregateDescriptions(descriptions);
    }

    /**
     * Find @RequireInterface annotation by searching method class and all
     * exported interfaces.
     *
     * @param setter
     * @param inspectedInterfaces
     * @return null if no annotation is found, otherwise return the annotation
     * @throws IllegalStateException    if more than one value is specified by found annotations
     * @throws IllegalArgumentException if set of exported interfaces contains non interface type
     */
    static RequireInterface findRequireInterfaceAnnotation(final Method setter,
                                                           Set<Class<?>> inspectedInterfaces) {

        // only allow setX(ObjectName y) or setX(ObjectName[] y) or setX(List<ObjectName> y) to continue

        if (setter.getParameterTypes().length > 1) {
            return null;
        }
        if (PERMITTED_PARAMETER_TYPES_FOR_DEPENDENCY_SETTER.contains(setter.getParameterTypes()[0]) == false) {
            return null;
        }

        List<RequireInterface> foundRequireInterfaces = AnnotationsHelper
                .findMethodAnnotationInSuperClassesAndIfcs(setter, RequireInterface.class, inspectedInterfaces);
        // make sure the list if not empty contains always annotation with same
        // value
        Set<Class<?>> foundValues = new HashSet<>();
        for (RequireInterface ri : foundRequireInterfaces) {
            foundValues.add(ri.value());
        }
        if (foundValues.isEmpty()) {
            return null;
        } else if (foundValues.size() > 1) {
            throw new IllegalStateException("Error finding @RequireInterface. "
                    + "More than one value specified as required interface "
                    + foundValues + " of " + setter + " of "
                    + setter.getDeclaringClass());
        } else {
            return foundRequireInterfaces.get(0);
        }
    }
}
