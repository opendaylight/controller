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

import org.opendaylight.controller.binding.generator.util.generated.type.builder.GeneratedTOBuilderImpl;
import org.opendaylight.controller.sal.binding.model.api.ConcreteType;
import org.opendaylight.controller.sal.binding.model.api.GeneratedTransferObject;
import org.opendaylight.controller.sal.binding.model.api.ParameterizedType;
import org.opendaylight.controller.sal.binding.model.api.Type;
import org.opendaylight.controller.sal.binding.model.api.WildcardType;
import org.opendaylight.controller.yang.binding.Augmentable;
import org.opendaylight.controller.yang.binding.Augmentation;
import org.opendaylight.controller.yang.binding.BaseIdentity;
import org.opendaylight.controller.yang.binding.DataObject;

public final class Types {
    private static final Type SET_TYPE = typeForClass(Set.class);
    private static final Type LIST_TYPE = typeForClass(List.class);
    private static final Type MAP_TYPE = typeForClass(Map.class);
    public static final Type DATA_OBJECT = typeForClass(DataObject.class);

    public static ConcreteType voidType() {
        return new ConcreteTypeImpl(Void.class.getPackage().getName(),
                Void.class.getSimpleName());
    }

    public static final Type primitiveType(final String primitiveType) {
        return new ConcreteTypeImpl("", primitiveType);
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

    public static GeneratedTransferObject getBaseIdentityTO() {
        Class<BaseIdentity> cls = BaseIdentity.class;
        GeneratedTOBuilderImpl gto = new GeneratedTOBuilderImpl(cls.getPackage().getName(),
                cls.getSimpleName());
        return gto.toInstance();
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

    public static WildcardType wildcardTypeFor(String packageName, String typeName) {
        return new WildcardTypeImpl(packageName, typeName);
    }

    public static ParameterizedType augmentableTypeFor(Type valueType) {
        final Type augmentable = typeForClass(Augmentable.class);
        return parameterizedTypeFor(augmentable, valueType);
    }

    public static ParameterizedType augmentationTypeFor(Type valueType) {
        final Type augmentation = typeForClass(Augmentation.class);
        return parameterizedTypeFor(augmentation, valueType);
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

    private static class WildcardTypeImpl extends AbstractBaseType
            implements WildcardType {
        public WildcardTypeImpl(String packageName, String typeName) {
            super(packageName, typeName);
        }
    }

}
