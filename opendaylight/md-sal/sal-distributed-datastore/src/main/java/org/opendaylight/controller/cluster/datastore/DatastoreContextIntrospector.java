/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.primitives.Primitives;
import java.beans.BeanInfo;
import java.beans.ConstructorProperties;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.MethodDescriptor;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
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

    /**
     * Introspects each primitive wrapper (ie Integer, Long etc) and String type to find the
     * constructor that takes a single String argument. For primitive wrappers, this constructor
     * converts from a String representation.
     */
    private static void introspectPrimitiveTypes() {

        Set<Class<?>> primitives = ImmutableSet.<Class<?>>builder().addAll(
                Primitives.allWrapperTypes()).add(String.class).build();
        for(Class<?> primitive: primitives) {
            try {
                processPropertyType(primitive);
            } catch (Exception e) {
                // Ignore primitives that can't be constructed from a String, eg Character and Void.
            }
        }
    }

    /**
     * Introspects the DatastoreContext.Builder class to find all its setter methods that we will
     * invoke via reflection. We can't use the bean Introspector here as the Builder setters don't
     * follow the bean property naming convention, ie setter prefixed with "set", so look for all
     * the methods that return Builder.
     */
    private static void introspectDatastoreContextBuilder() {
        for(Method method: Builder.class.getMethods()) {
            if(Builder.class.equals(method.getReturnType())) {
                builderSetters.put(method.getName(), method);
            }
        }
    }

    /**
     * Introspects the DataStoreProperties interface that is generated from the DataStoreProperties
     * yang grouping. We use the bean Introspector to find the types of all the properties defined
     * in the interface (this is the type returned from the getter method). For each type, we find
     * the appropriate constructor that we will use.
     */
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

    /**
     * Processes a property defined on the DataStoreProperties interface.
     */
    private static void processDataStoreProperty(String name, Class<?> propertyType) {
        Preconditions.checkArgument(builderSetters.containsKey(name), String.format(
                "DataStoreProperties property \"%s\" does not have corresponding setter in DatastoreContext.Builder", name));
        try {
            processPropertyType(propertyType);
            dataStorePropTypes.put(name, propertyType);
        } catch (Exception e) {
            LOG.error("Error finding constructor for type {}", propertyType, e);
        }
    }

    /**
     * Finds the appropriate constructor for the specified type that we will use to construct
     * instances.
     */
    private static void processPropertyType(Class<?> propertyType) throws Exception {
        Class<?> wrappedType = Primitives.wrap(propertyType);
        if(constructors.containsKey(wrappedType)) {
            return;
        }

        // If the type is a primitive (or String type), we look for the constructor that takes a
        // single String argument, which, for primitives, validates and converts from a String
        // representation which is the form we get on ingress.
        if(propertyType.isPrimitive() || Primitives.isWrapperType(propertyType) ||
                propertyType.equals(String.class))
        {
            constructors.put(wrappedType, propertyType.getConstructor(String.class));
        } else {
            // This must be a yang-defined type. We need to find the constructor that takes a
            // primitive as the only argument. This will be used to construct instances to perform
            // validation (eg range checking). The yang-generated types have a couple single-argument
            // constructors but the one we want has the bean ConstructorProperties annotation.
            for(Constructor<?> ctor: propertyType.getConstructors()) {
                ConstructorProperties ctorPropsAnnotation = ctor.getAnnotation(ConstructorProperties.class);
                if(ctor.getParameterTypes().length == 1 && ctorPropsAnnotation != null) {
                    findYangTypeGetter(propertyType, ctorPropsAnnotation.value()[0]);
                    constructors.put(propertyType, ctor);
                    break;
                }
            }
        }
    }

    /**
     * Finds the getter method on a yang-generated type for the specified property name.
     */
    private static void findYangTypeGetter(Class<?> type, String propertyName)
            throws Exception {
        for(PropertyDescriptor desc: Introspector.getBeanInfo(type).getPropertyDescriptors()) {
            if(desc.getName().equals(propertyName)) {
                yangTypeGetters.put(type, desc.getReadMethod());
                return;
            }
        }

        throw new IllegalArgumentException(String.format(
                "Getter method for constructor property %s not found for YANG type %s",
                propertyName, type));
    }

    private DatastoreContext context;
    private Map<String, Object> currentProperties;

    public DatastoreContextIntrospector(DatastoreContext context) {
        this.context = context;
    }

    public DatastoreContext getContext() {
        return context;
    }

    public DatastoreContextFactory newContextFactory() {
        return new DatastoreContextFactory(this);
    }

    public synchronized DatastoreContext getShardDatastoreContext(String forShardName) {
        if(currentProperties == null) {
            return context;
        }

        Builder builder = DatastoreContext.newBuilderFrom(context);
        String dataStoreTypePrefix = context.getDataStoreName() + '.';
        final String shardNamePrefix = forShardName + '.';

        List<String> keys = getSortedKeysByDatastoreType(currentProperties.keySet(), dataStoreTypePrefix);

        for(String key: keys) {
            Object value = currentProperties.get(key);
            if(key.startsWith(dataStoreTypePrefix)) {
                key = key.replaceFirst(dataStoreTypePrefix, "");
            }

            if(key.startsWith(shardNamePrefix)) {
                key = key.replaceFirst(shardNamePrefix, "");
                convertValueAndInvokeSetter(key, value, builder);
            }
        }

        return builder.build();
    }

    /**
     * Applies the given properties to the cached DatastoreContext and yields a new DatastoreContext
     * instance which can be obtained via {@link #getContext()}.
     *
     * @param properties the properties to apply
     * @return true if the cached DatastoreContext was updated, false otherwise.
     */
    public synchronized boolean update(Dictionary<String, Object> properties) {
        currentProperties = null;
        if(properties == null || properties.isEmpty()) {
            return false;
        }

        LOG.debug("In update: properties: {}", properties);

        ImmutableMap.Builder<String, Object> mapBuilder = ImmutableMap.<String, Object>builder();

        Builder builder = DatastoreContext.newBuilderFrom(context);

        final String dataStoreTypePrefix = context.getDataStoreName() + '.';

        List<String> keys = getSortedKeysByDatastoreType(Collections.list(properties.keys()), dataStoreTypePrefix);

        boolean updated = false;
        for(String key: keys) {
            Object value = properties.get(key);
            mapBuilder.put(key, value);

            // If the key is prefixed with the data store type, strip it off.
            if(key.startsWith(dataStoreTypePrefix)) {
                key = key.replaceFirst(dataStoreTypePrefix, "");
            }

            if(convertValueAndInvokeSetter(key, value, builder)) {
                updated = true;
            }
        }

        currentProperties = mapBuilder.build();

        if(updated) {
            context = builder.build();
        }

        return updated;
    }

    private static ArrayList<String> getSortedKeysByDatastoreType(Collection<String> inKeys,
            final String dataStoreTypePrefix) {
        // Sort the property keys by putting the names prefixed with the data store type last. This
        // is done so data store specific settings are applied after global settings.
        ArrayList<String> keys = new ArrayList<>(inKeys);
        Collections.sort(keys, new Comparator<String>() {
            @Override
            public int compare(String key1, String key2) {
                return key1.startsWith(dataStoreTypePrefix) ? 1 :
                           key2.startsWith(dataStoreTypePrefix) ? -1 : key1.compareTo(key2);
            }
        });
        return keys;
    }

    private boolean convertValueAndInvokeSetter(String inKey, Object inValue, Builder builder) {
        String key = convertToCamelCase(inKey);

        try {
            // Convert the value to the right type.
            Object value = convertValue(key, inValue);
            if(value == null) {
                return false;
            }

            LOG.debug("Converted value for property {}: {} ({})",
                    key, value, value.getClass().getSimpleName());

            // Call the setter method on the Builder instance.
            Method setter = builderSetters.get(key);
            setter.invoke(builder, constructorValueRecursively(
                    Primitives.wrap(setter.getParameterTypes()[0]), value.toString()));

            return true;
        } catch (Exception e) {
            LOG.error("Error converting value ({}) for property {}", inValue, key, e);
        }

        return false;
    }

    private static String convertToCamelCase(String inString) {
        String str = inString.trim();
        if(StringUtils.contains(str, '-') || StringUtils.contains(str, ' ')) {
            str = inString.replace('-', ' ');
            str = WordUtils.capitalizeFully(str);
            str = StringUtils.deleteWhitespace(str);
        }

        return StringUtils.uncapitalize(str);
    }

    private Object convertValue(String name, Object from) throws Exception {
        Class<?> propertyType = dataStorePropTypes.get(name);
        if(propertyType == null) {
            LOG.debug("Property not found for {}", name);
            return null;
        }

        LOG.debug("Type for property {}: {}, converting value {} ({})",
                name, propertyType.getSimpleName(), from, from.getClass().getSimpleName());

        // Recurse the chain of constructors depth-first to get the resulting value. Eg, if the
        // property type is the yang-generated NonZeroUint32Type, it's constructor takes a Long so
        // we have to first construct a Long instance from the input value.
        Object converted = constructorValueRecursively(propertyType, from.toString());

        // If the converted type is a yang-generated type, call the getter to obtain the actual value.
        Method getter = yangTypeGetters.get(converted.getClass());
        if(getter != null) {
            converted = getter.invoke(converted);
        }

        return converted;
    }

    private Object constructorValueRecursively(Class<?> toType, Object fromValue) throws Exception {
        LOG.trace("convertValueRecursively - toType: {}, fromValue {} ({})",
                toType.getSimpleName(), fromValue, fromValue.getClass().getSimpleName());

        Constructor<?> ctor = constructors.get(toType);

        LOG.trace("Found {}", ctor);

        if(ctor == null) {
            throw new IllegalArgumentException(String.format("Constructor not found for type %s", toType));
        }

        Object value = fromValue;

        // Since the original input type is a String, once we find a constructor that takes a String
        // argument, we're done recursing.
        if(!ctor.getParameterTypes()[0].equals(String.class)) {
            value = constructorValueRecursively(ctor.getParameterTypes()[0], fromValue);
        }

        return ctor.newInstance(value);
    }
}
