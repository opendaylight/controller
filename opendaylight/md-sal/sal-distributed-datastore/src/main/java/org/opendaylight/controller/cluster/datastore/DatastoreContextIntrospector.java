/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.primitives.Primitives;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;
import javax.management.ConstructorParameters;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.WordUtils;
import org.checkerframework.checker.lock.qual.GuardedBy;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.controller.cluster.datastore.DatastoreContext.Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.distributed.datastore.provider.rev250130.DataStoreProperties;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.distributed.datastore.provider.rev250130.DataStorePropertiesContainer;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.opendaylight.yangtools.yang.common.Uint64;
import org.opendaylight.yangtools.yang.common.Uint8;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Introspects on a DatastoreContext instance to set its properties via reflection.
 * i
 * @author Thomas Pantelis
 */
public class DatastoreContextIntrospector {
    private static final Logger LOG = LoggerFactory.getLogger(DatastoreContextIntrospector.class);

    private static final Map<String, Entry<Class<?>, Method>> DATA_STORE_PROP_INFO = new HashMap<>();

    private static final Map<Class<?>, Constructor<?>> CONSTRUCTORS = new HashMap<>();

    private static final Map<Class<?>, Method> YANG_TYPE_GETTERS = new HashMap<>();

    private static final Map<String, Method> BUILDER_SETTERS = new HashMap<>();

    private static final ImmutableMap<Class<?>, Function<String, Object>> UINT_FACTORIES =
            ImmutableMap.<Class<?>, Function<String, Object>>builder()
            .put(Uint8.class, Uint8::valueOf)
            .put(Uint16.class, Uint16::valueOf)
            .put(Uint32.class, Uint32::valueOf)
            .put(Uint64.class, Uint64::valueOf)
            .build();

    static {
        try {
            introspectDatastoreContextBuilder();
            introspectDataStoreProperties();
            introspectPrimitiveTypes();
        } catch (final IllegalArgumentException e) {
            LOG.error("Error initializing DatastoreContextIntrospector", e);
        }
    }

    /**
     * Introspects each primitive wrapper (ie Integer, Long etc) and String type to find the
     * constructor that takes a single String argument. For primitive wrappers, this constructor
     * converts from a String representation.
     */
    // Disables "Either log or rethrow this exception" sonar warning
    @SuppressWarnings("squid:S1166")
    private static void introspectPrimitiveTypes() {
        final Set<Class<?>> primitives = ImmutableSet.<Class<?>>builder().addAll(
                Primitives.allWrapperTypes()).add(String.class).build();
        for (final Class<?> primitive : primitives) {
            try {
                processPropertyType(primitive);
            } catch (final NoSuchMethodException e) {
                // Ignore primitives that can't be constructed from a String, eg Character and Void.
            } catch (SecurityException | IllegalArgumentException e) {
                LOG.error("Error introspect primitive type {}", primitive, e);
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
        for (final Method method: Builder.class.getMethods()) {
            if (Builder.class.equals(method.getReturnType())) {
                BUILDER_SETTERS.put(method.getName(), method);
            }
        }
    }

    /**
     * Introspects the DataStoreProperties interface that is generated from the DataStoreProperties
     * yang grouping. We use the bean Introspector to find the types of all the properties defined
     * in the interface (this is the type returned from the getter method). For each type, we find
     * the appropriate constructor that we will use.
     *
     * @throws IllegalArgumentException if failed to process yang-defined property
     */
    private static void introspectDataStoreProperties() {
        for (final Method method : DataStoreProperties.class.getDeclaredMethods()) {
            final String propertyName = getPropertyName(method);
            if (propertyName != null) {
                processDataStoreProperty(propertyName, method.getReturnType(), method);
            }
        }
    }

    private static String getPropertyName(final Method method) {
        final String methodName = method.getName();
        if (Boolean.class.equals(method.getReturnType()) && methodName.startsWith("is")) {
            return WordUtils.uncapitalize(methodName.substring(2));
        } else if (methodName.startsWith("get")) {
            return WordUtils.uncapitalize(methodName.substring(3));
        }
        return null;
    }

    /**
     * Processes a property defined on the DataStoreProperties interface.
     */
    @SuppressWarnings("checkstyle:IllegalCatch")
    private static void processDataStoreProperty(final String name, final Class<?> propertyType,
            final Method readMethod) {
        checkArgument(BUILDER_SETTERS.containsKey(name),
                "DataStoreProperties property \"%s\" does not have corresponding setter in DatastoreContext.Builder",
                name);
        try {
            processPropertyType(propertyType);
            DATA_STORE_PROP_INFO.put(name, new SimpleImmutableEntry<>(propertyType, readMethod));
        } catch (final Exception e) {
            LOG.error("Error finding constructor for type {}", propertyType, e);
        }
    }

    /**
     * Finds the appropriate constructor for the specified type that we will use to construct
     * instances.
     *
     * @throws IllegalArgumentException if yang-defined type has no property, annotated by ConstructorParameters
     */
    private static void processPropertyType(final Class<?> propertyType)
            throws NoSuchMethodException, SecurityException {
        final Class<?> wrappedType = Primitives.wrap(propertyType);
        if (CONSTRUCTORS.containsKey(wrappedType)) {
            return;
        }

        // If the type is a primitive (or String type), we look for the constructor that takes a
        // single String argument, which, for primitives, validates and converts from a String
        // representation which is the form we get on ingress.
        if (propertyType.isPrimitive() || Primitives.isWrapperType(propertyType) || propertyType.equals(String.class)) {
            CONSTRUCTORS.put(wrappedType, propertyType.getConstructor(String.class));
        } else {
            // This must be a yang-defined type. We need to find the constructor that takes a
            // primitive as the only argument. This will be used to construct instances to perform
            // validation (eg range checking). The yang-generated types have a couple single-argument
            // constructors but the one we want has the ConstructorParameters annotation.
            for (final Constructor<?> ctor: propertyType.getConstructors()) {
                final ConstructorParameters ctorParAnnotation = ctor.getAnnotation(ConstructorParameters.class);
                if (ctor.getParameterCount() == 1 && ctorParAnnotation != null) {
                    findYangTypeGetter(propertyType, ctorParAnnotation.value()[0]);
                    CONSTRUCTORS.put(propertyType, ctor);
                    break;
                }
            }
        }
    }

    /**
     * Finds the getter method on a yang-generated type for the specified property name.
     *
     * @throws IllegalArgumentException if passed type has no passed property
     */
    private static void findYangTypeGetter(final Class<?> type, final String propertyName) {
        for (Method method : type.getDeclaredMethods()) {
            final String property = getPropertyName(method);
            if (property != null && property.equals(propertyName)) {
                YANG_TYPE_GETTERS.put(type, method);
                return;
            }
        }

        throw new IllegalArgumentException(String.format(
                "Getter method for constructor property %s not found for YANG type %s",
                propertyName, type));
    }

    private @GuardedBy("this") DatastoreContext context;
    private @GuardedBy("this") Map<String, Object> currentProperties;

    public DatastoreContextIntrospector(final DatastoreContext context,
            final DataStorePropertiesContainer defaultPropsContainer) {

        final Builder builder = DatastoreContext.newBuilderFrom(context);
        for (Entry<String, Entry<Class<?>, Method>> entry: DATA_STORE_PROP_INFO.entrySet()) {
            Object value;
            try {
                value = entry.getValue().getValue().invoke(defaultPropsContainer);
            } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                LOG.error("Error obtaining default value for property {}", entry.getKey(), e);
                value = null;
            }

            if (value != null) {
                convertValueAndInvokeSetter(entry.getKey(), value, builder);
            }
        }

        this.context = builder.build();
    }

    public synchronized DatastoreContext getContext() {
        return context;
    }

    public DatastoreContextFactory newContextFactory() {
        return new DatastoreContextFactory(this);
    }

    public synchronized DatastoreContext getShardDatastoreContext(final String forShardName) {
        if (currentProperties == null) {
            return context;
        }

        final Builder builder = DatastoreContext.newBuilderFrom(context);
        final String dataStoreTypePrefix = context.getDataStoreName() + '.';
        final String shardNamePrefix = forShardName + '.';

        final List<String> keys = getSortedKeysByDatastoreType(currentProperties.keySet(), dataStoreTypePrefix);

        for (String key: keys) {
            final Object value = currentProperties.get(key);
            if (key.startsWith(dataStoreTypePrefix)) {
                key = key.replaceFirst(dataStoreTypePrefix, "");
            }

            if (key.startsWith(shardNamePrefix)) {
                key = key.replaceFirst(shardNamePrefix, "");
                convertValueAndInvokeSetter(key, value.toString(), builder);
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
    public synchronized boolean update(final @Nullable Map<String, Object> properties) {
        currentProperties = null;
        if (properties == null || properties.isEmpty()) {
            return false;
        }

        LOG.debug("In update: properties: {}", properties);

        final ImmutableMap.Builder<String, Object> mapBuilder = ImmutableMap.<String, Object>builder();

        final Builder builder = DatastoreContext.newBuilderFrom(context);

        final String dataStoreTypePrefix = context.getDataStoreName() + '.';

        final List<String> keys = getSortedKeysByDatastoreType(properties.keySet(), dataStoreTypePrefix);

        boolean updated = false;
        for (String key: keys) {
            final Object value = properties.get(key);
            mapBuilder.put(key, value);

            // If the key is prefixed with the data store type, strip it off.
            if (key.startsWith(dataStoreTypePrefix)) {
                key = key.replaceFirst(dataStoreTypePrefix, "");
            }

            if (convertValueAndInvokeSetter(key, value.toString(), builder)) {
                updated = true;
            }
        }

        currentProperties = mapBuilder.build();

        if (updated) {
            context = builder.build();
        }

        return updated;
    }

    private static ArrayList<String> getSortedKeysByDatastoreType(final Collection<String> inKeys,
            final String dataStoreTypePrefix) {
        // Sort the property keys by putting the names prefixed with the data store type last. This
        // is done so data store specific settings are applied after global settings.
        final ArrayList<String> keys = new ArrayList<>(inKeys);
        keys.sort((key1, key2) -> key1.startsWith(dataStoreTypePrefix) ? 1 :
            key2.startsWith(dataStoreTypePrefix) ? -1 : key1.compareTo(key2));
        return keys;
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    private boolean convertValueAndInvokeSetter(final String inKey, final Object inValue, final Builder builder) {
        final String key = convertToCamelCase(inKey);

        try {
            // Convert the value to the right type.
            final Object value = convertValue(key, inValue);
            if (value == null) {
                return false;
            }

            LOG.debug("Converted value for property {}: {} ({})",
                    key, value, value.getClass().getSimpleName());

            // Call the setter method on the Builder instance.
            final Method setter = BUILDER_SETTERS.get(key);
            if (value.getClass().isEnum()) {
                setter.invoke(builder, value);
            } else {
                setter.invoke(builder, constructorValueRecursively(
                        Primitives.wrap(setter.getParameterTypes()[0]), value.toString()));
            }

            return true;
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException
                | InstantiationException e) {
            LOG.error("Error converting value ({}) for property {}", inValue, key, e);
        }

        return false;
    }

    private static String convertToCamelCase(final String inString) {
        String str = inString.trim();
        if (StringUtils.contains(str, '-') || StringUtils.contains(str, ' ')) {
            str = inString.replace('-', ' ');
            str = WordUtils.capitalizeFully(str);
            str = StringUtils.deleteWhitespace(str);
        }

        return StringUtils.uncapitalize(str);
    }

    private Object convertValue(final String name, final Object from)
            throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        final Entry<Class<?>, Method> propertyInfo = DATA_STORE_PROP_INFO.get(name);
        if (propertyInfo == null) {
            LOG.debug("Property not found for {}", name);
            return null;
        }

        final Class<?> propertyType = propertyInfo.getKey();

        LOG.debug("Type for property {}: {}, converting value {} ({})",
                name, propertyType.getSimpleName(), from, from.getClass().getSimpleName());

        if (propertyType.isEnum()) {
            try {
                final Method enumConstructor = propertyType.getDeclaredMethod("forName", String.class);
                if (enumConstructor.getReturnType().equals(propertyType)) {
                    return enumConstructor.invoke(null, from.toString().toLowerCase(Locale.ROOT));
                }
            } catch (NoSuchMethodException e) {
                LOG.error("Error constructing value ({}) for enum {}", from, propertyType);
            }
        }

        // Recurse the chain of constructors depth-first to get the resulting value. Eg, if the
        // property type is the yang-generated NonZeroUint32Type, it's constructor takes a Long so
        // we have to first construct a Long instance from the input value.
        Object converted = constructorValueRecursively(propertyType, from);

        // If the converted type is a yang-generated type, call the getter to obtain the actual value.
        final Method getter = YANG_TYPE_GETTERS.get(converted.getClass());
        if (getter != null) {
            converted = getter.invoke(converted);
        }

        return converted;
    }

    private Object constructorValueRecursively(final Class<?> toType, final Object fromValue)
            throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        LOG.trace("convertValueRecursively - toType: {}, fromValue {} ({})",
                toType.getSimpleName(), fromValue, fromValue.getClass().getSimpleName());

        if (toType.equals(fromValue.getClass())) {
            return fromValue;
        }

        final Constructor<?> ctor = CONSTRUCTORS.get(toType);
        if (ctor == null) {
            if (fromValue instanceof String) {
                final Function<String, Object> factory = UINT_FACTORIES.get(toType);
                if (factory != null) {
                    return factory.apply((String) fromValue);
                }
            }

            throw new IllegalArgumentException(String.format("Constructor not found for type %s", toType));
        }

        LOG.trace("Found {}", ctor);
        Object value = fromValue;

        // Once we find a constructor that takes the original type as an argument, we're done recursing.
        if (!ctor.getParameterTypes()[0].equals(fromValue.getClass())) {
            value = constructorValueRecursively(ctor.getParameterTypes()[0], fromValue);
        }

        return ctor.newInstance(value);
    }
}
