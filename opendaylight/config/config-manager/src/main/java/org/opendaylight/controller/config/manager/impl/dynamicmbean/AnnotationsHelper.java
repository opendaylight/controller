/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.manager.impl.dynamicmbean;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSet.Builder;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.opendaylight.controller.config.api.annotations.Description;

public class AnnotationsHelper {

    private AnnotationsHelper() {
    }

    /**
     * Look for annotation specified by annotationType on method. First observe
     * method's class, then its super classes, then all provided interfaces.
     * Used for finding @Description and @RequireInterface
     *
     * @param <T>
     *            generic type of annotation
     * @return list of found annotations
     */
    static <T extends Annotation> List<T> findMethodAnnotationInSuperClassesAndIfcs(
            final Method setter, final Class<T> annotationType,
            final Set<Class<?>> inspectedInterfaces) {
        Builder<T> result = ImmutableSet.builder();
        Class<?> inspectedClass = setter.getDeclaringClass();
        do {
            try {
                Method foundSetter = inspectedClass.getMethod(setter.getName(),
                        setter.getParameterTypes());
                T annotation = foundSetter.getAnnotation(annotationType);
                if (annotation != null) {
                    result.add(annotation);
                }
                // we need to go deeper
                inspectedClass = inspectedClass.getSuperclass();
            } catch (NoSuchMethodException e) {
                inspectedClass = Object.class; // no need to go further
            }
        } while (!inspectedClass.equals(Object.class));

        // inspect interfaces
        for (Class<?> ifc : inspectedInterfaces) {
            if (ifc.isInterface() == false) {
                throw new IllegalArgumentException(ifc + " is not an interface");
            }
            try {
                Method foundSetter = ifc.getMethod(setter.getName(),
                        setter.getParameterTypes());
                T annotation = foundSetter.getAnnotation(annotationType);
                if (annotation != null) {
                    result.add(annotation);
                }
            } catch (NoSuchMethodException e) {

            }
        }
        return new ArrayList<>(result.build());
    }

    /**
     * Look for annotation specified by annotationType on type. First observe
     * class clazz, then its super classes, then all exported interfaces with
     * their super types. Used for finding @Description of modules.
     *
     * @return list of found annotations
     */
    static <T extends Annotation> List<T> findClassAnnotationInSuperClassesAndIfcs(
            final Class<?> clazz, final Class<T> annotationType, final Set<Class<?>> interfaces) {
        List<T> result = new ArrayList<T>();
        Class<?> declaringClass = clazz;
        do {
            T annotation = declaringClass.getAnnotation(annotationType);
            if (annotation != null) {
                result.add(annotation);
            }
            declaringClass = declaringClass.getSuperclass();
        } while (declaringClass.equals(Object.class) == false);
        // inspect interfaces
        for (Class<?> ifc : interfaces) {
            if (ifc.isInterface() == false) {
                throw new IllegalArgumentException(ifc + " is not an interface");
            }
            T annotation = ifc.getAnnotation(annotationType);
            if (annotation != null) {
                result.add(annotation);
            }
        }
        return result;
    }

    /**
     * @return empty string if no annotation is found, or list of descriptions
     *         separated by newline
     */
    static String aggregateDescriptions(final List<Description> descriptions) {
        StringBuilder builder = new StringBuilder();
        for (Description d : descriptions) {
            if (builder.length() != 0) {
                builder.append("\n");
            }
            builder.append(d.value());

        }
        return builder.toString();
    }
}
