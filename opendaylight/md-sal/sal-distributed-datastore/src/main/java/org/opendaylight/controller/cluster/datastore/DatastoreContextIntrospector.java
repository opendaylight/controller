/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import com.google.common.primitives.Primitives;
import java.beans.BeanInfo;
import java.beans.ConstructorProperties;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.MethodDescriptor;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.text.WordUtils;
import org.opendaylight.controller.cluster.datastore.DatastoreContext.Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.distributed.datastore.provider.rev140612.DataStoreProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Introspects on a DatastoreContext instance to set its properties via reflection.
 * i
 * @author Thomas Pantelis
 */
public class DatastoreContextIntrospector {
    private static final Logger LOG = LoggerFactory.getLogger(DatastoreContextIntrospector.class);

    private static final Map<String, Class<?>> dataStorePropTypes = new HashMap<>();

    private static final Map<Class<?>, Constructor<?>> constructors = new HashMap<>();

    private static final Map<Class<?>, Method> yangTypeGetters = new HashMap<>();

    private static final Map<String, Method> builderSetters = new HashMap<>();

    static {
        try {
            introspectDatastoreContextBuilder();
            introspectDataStoreProperties();
            introspectPrimitiveTypes();
        } catch (IntrospectionException e) {
            LOG.error("Error initializing DatastoreContextIntrospector", e);
        }
    }

    private static void introspectPrimitiveTypes() {
        for(Class<?> primitive: Primitives.allWrapperTypes()) {
            try {
                processPropertyType(primitive);
            } catch (Exception e) {
                // Ignore primitives that can't be constructed from a String, eg Character and Void.
            }

        }
    }

    private static void introspectDatastoreContextBuilder() {
        for(Method method: Builder.class.getMethods()) {
            if(Builder.class.equals(method.getReturnType())) {
                builderSetters.put(method.getName(), method);
            }
        }
    }

    private static void introspectDataStoreProperties() throws IntrospectionException {
        BeanInfo beanInfo = Introspector.getBeanInfo(DataStoreProperties.class);
        for(PropertyDescriptor desc: beanInfo.getPropertyDescriptors()) {
            processDataStoreProperty(desc.getName(), desc.getPropertyType());
        }

        // Getter methods that return Boolean and start with "is" instead of "get" aren't recognized as
        // properties and thus aren't returned from getPropertyDescriptors. A getter starting with
        // "is" is only supported if it returns primitive boolean. So we'll check for these via
        // getMethodDescriptors.
        for(MethodDescriptor desc: beanInfo.getMethodDescriptors()) {
            String methodName = desc.getName();
            if(Boolean.class.equals(desc.getMethod().getReturnType()) && methodName.startsWith("is")) {
                String propertyName = WordUtils.uncapitalize(methodName.substring(2));
                processDataStoreProperty(propertyName, Boolean.class);
            }
        }
    }

    private static void processDataStoreProperty(String name, Class<?> propertyType) {
        if(!builderSetters.containsKey(name)) {
            LOG.error("DataStoreProperties property {} does not have corresponding setter in DatastoreContext.Builder",
                    name);
            return;
        }

        try {
            processPropertyType(propertyType);
            dataStorePropTypes.put(name, propertyType);
        } catch (Exception e) {
            LOG.error("Error finding constructor for type {}", propertyType, e);
        }
    }

    private static void processPropertyType(Class<?> propertyType) throws Exception {
        Class<?> wrappedType = Primitives.wrap(propertyType);
        if(constructors.containsKey(wrappedType)) {
            return;
        }

        if(propertyType.isPrimitive() || Primitives.isWrapperType(propertyType) ||
                propertyType.equals(String.class))
        {
            constructors.put(wrappedType, propertyType.getConstructor(String.class));
        } else {
            // Must be a yang-defined type - find constructor with ConstructorProperties annotation.

            for(Constructor<?> ctor: propertyType.getConstructors()) {
                ConstructorProperties ctorPropsAnnotation = ctor.getAnnotation(ConstructorProperties.class);
                if(ctor.getParameterTypes().length == 1 && ctorPropsAnnotation != null) {
                    findYangTypeGetter(propertyType, ctorPropsAnnotation);
                    constructors.put(propertyType, ctor);
                    break;
                }
            }
        }
    }

    private static void findYangTypeGetter(Class<?> type, ConstructorProperties ctorPropsAnnotation)
            throws Exception {
        for(PropertyDescriptor desc: Introspector.getBeanInfo(type).getPropertyDescriptors()) {
            if(desc.getName().equals(ctorPropsAnnotation.value()[0])) {
                yangTypeGetters.put(type, desc.getReadMethod());
                return;
            }
        }

        throw new IllegalArgumentException(String.format(
                "Getter method for constructor property %s not found for YANG type %s",
                ctorPropsAnnotation.value()[0], type));
    }

    private final DatastoreContext context;

    public DatastoreContextIntrospector(DatastoreContext context) {
        this.context = context;
    }

    public void update(Dictionary<String, Object> properties) {
        Builder builder = DatastoreContext.newBuilderFrom(context);

        for(String key: Collections.list(properties.keys())) {
            Object value = properties.get(key);
            try {
                if(key.startsWith(context.getDataStoreType())) {
                    key = WordUtils.uncapitalize(key.replaceFirst(context.getDataStoreType(), ""));
                }

                value = convertValue(key, value);
                if(value == null) {
                    continue;
                }

                LOG.debug("Converted value for property {}: {} ({})",
                        key, value, value.getClass().getSimpleName());

                Method setter = builderSetters.get(key);
                setter.invoke(builder, convertValueRecursively(
                        Primitives.wrap(setter.getParameterTypes()[0]), value.toString()));

            } catch (Exception e) {
                LOG.error("Error converting value ({}) for property {}", value, key, e);
            }
        }

        builder.build();
    }

    private Object convertValue(String name, Object from) throws Exception {
        Class<?> propertyType = dataStorePropTypes.get(name);
        if(propertyType == null) {
            LOG.debug("Property not found for {}", name);
            return null;
        }

        LOG.debug("Type for property {}: {}, converting value {} ({})",
                name, propertyType.getSimpleName(), from, from.getClass().getSimpleName());

        Object converted = convertValueRecursively(propertyType, from.toString());

        Method getter = yangTypeGetters.get(converted.getClass());
        if(getter != null) {
            converted = getter.invoke(converted);
        }

        return converted;
    }

    private Object convertValueRecursively(Class<?> toType, Object fromValue) throws Exception {
        LOG.debug("convertValueRecursively - toType: {}, fromValue {} ({})",
                toType.getSimpleName(), fromValue, fromValue.getClass().getSimpleName());

        Constructor<?> ctor = constructors.get(toType);

        LOG.debug("Found {}", ctor);

        if(ctor == null) {
            throw new IllegalArgumentException(String.format("Constructor not found for type %s", toType));
        }

        Object value = fromValue;
        if(!ctor.getParameterTypes()[0].equals(String.class)) {
            value = convertValueRecursively(ctor.getParameterTypes()[0], fromValue);
        }

        return ctor.newInstance(value);
    }
}
