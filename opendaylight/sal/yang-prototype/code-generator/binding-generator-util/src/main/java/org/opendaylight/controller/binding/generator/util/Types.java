/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.binding.generator.util;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.opendaylight.controller.sal.binding.model.api.ConcreteType;
import org.opendaylight.controller.sal.binding.model.api.ParameterizedType;
import org.opendaylight.controller.sal.binding.model.api.Type;

public class Types {
    private static final Type SET_TYPE = typeForClass(Set.class);
    private static final Type LIST_TYPE = typeForClass(List.class);
    private static final Type MAP_TYPE = typeForClass(Map.class);

    private Types() {
    }

    public static ConcreteType voidType() {
        return new ConcreteTypeImpl(Void.class.getPackage().getName(),
                Void.class.getSimpleName());
    }

    /**
     * Returns an instance of {@link ConcreteType} describing the class
     * 
     * @param cls
     *            Class to describe
     * @return Description of class
     */
    public static ConcreteType typeForClass(Class<?> cls) {
        return new ConcreteTypeImpl(cls.getPackage().getName(),
                cls.getSimpleName());
    }

    /**
     * Returns an instance of {@link ParameterizedType} describing the typed
     * {@link Map}<K,V>
     * 
     * @param keyType
     *            Key Type
     * @param valueType
     *            Value Type
     * @return Description of generic type instance
     */
    public static ParameterizedType mapTypeFor(Type keyType, Type valueType) {
        return parameterizedTypeFor(MAP_TYPE, keyType, valueType);
    }

    /**
     * Returns an instance of {@link ParameterizedType} describing the typed
     * {@link Set}<V> with concrete type of value.
     * 
     * @param valueType
     *            Value Type
     * @return Description of generic type instance of Set
     */
    public static ParameterizedType setTypeFor(Type valueType) {
        return parameterizedTypeFor(SET_TYPE, valueType);
    }

    /**
     * Returns an instance of {@link ParameterizedType} describing the typed
     * {@link List}<V> with concrete type of value.
     * 
     * @param valueType
     *            Value Type
     * @return Description of type instance of List
     */
    public static ParameterizedType listTypeFor(Type valueType) {
        return parameterizedTypeFor(LIST_TYPE, valueType);
    }

    /**
     * 
     * @param type
     * @param parameters
     * @return
     */
    public static ParameterizedType parameterizedTypeFor(Type type,
            Type... parameters) {
        return new ParametrizedTypeImpl(type, parameters);
    }

    private static class ConcreteTypeImpl extends AbstractBaseType implements
            ConcreteType {
        private ConcreteTypeImpl(String pkName, String name) {
            super(pkName, name);
        }
    }

    private static class ParametrizedTypeImpl extends AbstractBaseType
            implements ParameterizedType {
        private Type[] actualTypes;
        private Type rawType;

        @Override
        public Type[] getActualTypeArguments() {

            return actualTypes;
        }

        @Override
        public Type getRawType() {
            return rawType;
        }

        public ParametrizedTypeImpl(Type rawType, Type[] actTypes) {
            super(rawType.getPackageName(), rawType.getName());
            this.rawType = rawType;
            this.actualTypes = actTypes;
        }

    }
}
