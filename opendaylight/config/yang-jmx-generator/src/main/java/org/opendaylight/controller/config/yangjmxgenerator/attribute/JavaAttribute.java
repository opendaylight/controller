/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yangjmxgenerator.attribute;

import com.google.common.collect.Lists;
import org.opendaylight.controller.config.yangjmxgenerator.TypeProviderWrapper;
import org.opendaylight.yangtools.sal.binding.model.api.Type;
import org.opendaylight.yangtools.yang.model.api.LeafListSchemaNode;
import org.opendaylight.yangtools.yang.model.api.LeafSchemaNode;
import org.opendaylight.yangtools.yang.model.api.TypeDefinition;

import javax.management.openmbean.ArrayType;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;
import java.util.List;

public class JavaAttribute extends AbstractAttribute implements TypedAttribute {

    private final Type type;
    private final String nullableDescription, nullableDefault;
    private final TypeProviderWrapper typeProviderWrapper;
    private final TypeDefinition<?> typeDefinition;

    public JavaAttribute(LeafSchemaNode leaf,
            TypeProviderWrapper typeProviderWrapper) {
        super(leaf);
        this.type = typeProviderWrapper.getType(leaf);
        this.typeDefinition = leaf.getType();
        this.typeProviderWrapper = typeProviderWrapper;
        this.nullableDefault = leaf.getDefault();
        this.nullableDescription = leaf.getDescription();
    }

    public JavaAttribute(LeafListSchemaNode leaf,
            TypeProviderWrapper typeProviderWrapper) {
        super(leaf);
        this.type = typeProviderWrapper.getType(leaf);
        this.typeDefinition = leaf.getType();
        this.typeProviderWrapper = typeProviderWrapper;
        this.nullableDefault = null;
        this.nullableDescription = leaf.getDescription();
    }

    private List<Type> getBaseTypes(TypeProviderWrapper typeProviderWrapper, TypeDefinition<?> baseType) {
        List<Type> baseTypes = Lists.newArrayList();

        while(baseType!=null) {
            baseTypes.add(typeProviderWrapper.getType(baseType, baseType));
            baseType = baseType.getBaseType();
        }
        return Lists.reverse(baseTypes);
    }

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public String getNullableDescription() {
        return nullableDescription;
    }

    @Override
    public String getNullableDefault() {
        return nullableDefault;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        if (!super.equals(o))
            return false;

        JavaAttribute that = (JavaAttribute) o;

        if (nullableDefault != null ? !nullableDefault
                .equals(that.nullableDefault) : that.nullableDefault != null)
            return false;
        if (nullableDescription != null ? !nullableDescription
                .equals(that.nullableDescription)
                : that.nullableDescription != null)
            return false;
        if (type != null ? !type.equals(that.type) : that.type != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (type != null ? type.hashCode() : 0);
        result = 31
                * result
                + (nullableDescription != null ? nullableDescription.hashCode()
                        : 0);
        result = 31 * result
                + (nullableDefault != null ? nullableDefault.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "JavaAttribute{" + getAttributeYangName() + "," + "type=" + type
                + '}';
    }

    @Override
    public OpenType<?> getOpenType() {
        List<Type> baseTypes = getBaseTypes(typeProviderWrapper, typeDefinition.getBaseType());


        if (isArray()) {
            return getArrayType();
        } else if (isDerivedType(baseTypes)) {
            return getCompositeType(baseTypes);
        }

        return getSimpleType();
    }

    private OpenType<?> getSimpleType() {
        SimpleType<?> simpleType = SimpleTypeResolver.getSimpleType(getType());
        return simpleType;
    }

    // every base type is wrapped as composite type
//    private OpenType<?> getCompositeType(List<Type> baseTypes) {
//        OpenType<?> lastOpenType = null;
//
//        for (Type baseType : baseTypes) {
//            OpenType<?> innerItemType = SimpleTypeResolver.canResolve(baseType) ? SimpleTypeResolver.getSimpleType(baseType) : lastOpenType;
//            String description = getNullableDescription() == null ? getAttributeYangName() : getNullableDescription();
//
//            String innerItemName = typeProviderWrapper.getJMXParamForBaseType(baseType);
//
//            String[] itemNames = new String[]{innerItemName};
//
//            OpenType<?>[] itemTypes = new OpenType[]{innerItemType};
//            try {
//                lastOpenType = new CompositeType(getUpperCaseCammelCase(), description, itemNames, itemNames, itemTypes);
//            } catch (OpenDataException e) {
//                throw new RuntimeException("Unable to create " + CompositeType.class + " with inner element of type "
//                        + itemTypes, e);
//            }
//        }
//
//        return lastOpenType;
//    }

    // multiple base types are ignored, only last base type is considered
    private OpenType<?> getCompositeType(List<Type> baseTypes) {
        Type lastBaseType = null;

        for (Type baseType : baseTypes) {
            lastBaseType = baseType;

            if(SimpleTypeResolver.canResolve(baseType))
            {
                SimpleType<?> innerItemType = SimpleTypeResolver.getSimpleType(baseType);
                String innerItemName = typeProviderWrapper.getJMXParamForBaseType(baseType);

                String[] itemNames = new String[]{innerItemName};
                String description = getNullableDescription() == null ? getAttributeYangName() : getNullableDescription();

                OpenType<?>[] itemTypes = new OpenType[]{innerItemType};
                try {
                    return new CompositeType(getUpperCaseCammelCase(), description, itemNames, itemNames, itemTypes);
                } catch (OpenDataException e) {
                    throw new RuntimeException("Unable to create " + CompositeType.class + " with inner element of type "
                            + itemTypes, e);
                }

            } else continue;
        }

        throw new RuntimeException("Unable to create " + CompositeType.class + ", Cannot resolve most base type" + lastBaseType);

    }

    private OpenType<?> getArrayType() {
        String innerTypeFullyQName = getInnerType(getType());
        SimpleType<?> innerSimpleType = SimpleTypeResolver.getSimpleType(innerTypeFullyQName);
        try {
            ArrayType<Object> arrayType = isPrimitive(innerTypeFullyQName) ? new ArrayType<>(innerSimpleType, true)
                    : new ArrayType<>(1, innerSimpleType);
            return arrayType;
        } catch (OpenDataException e) {
            throw new RuntimeException("Unable to create " + ArrayType.class + " with inner element of type "
                    + innerSimpleType, e);
        }
    }

    // TODO verify
    private boolean isPrimitive(String innerTypeFullyQName) {
        if (innerTypeFullyQName.contains("."))
            return false;

        return true;
    }

    private boolean isArray() {
        return type.getName().endsWith("[]");
    }

    private static boolean isDerivedType(List<Type> baseTypes) {
        return baseTypes.isEmpty() == false;
    }

    private static String getInnerType(Type type) {
        String fullyQualifiedName = type.getFullyQualifiedName();
        return fullyQualifiedName.substring(0, fullyQualifiedName.length() - 2);
    }

}
