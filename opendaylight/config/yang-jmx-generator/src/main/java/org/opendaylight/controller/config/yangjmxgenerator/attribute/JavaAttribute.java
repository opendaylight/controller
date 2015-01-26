/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yangjmxgenerator.attribute;

import com.google.common.base.Preconditions;
import java.util.Arrays;
import java.util.List;
import javax.management.openmbean.ArrayType;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;
import org.opendaylight.controller.config.api.IdentityAttributeRef;
import org.opendaylight.controller.config.yangjmxgenerator.TypeProviderWrapper;
import org.opendaylight.yangtools.sal.binding.model.api.Type;
import org.opendaylight.yangtools.yang.model.api.LeafListSchemaNode;
import org.opendaylight.yangtools.yang.model.api.LeafSchemaNode;
import org.opendaylight.yangtools.yang.model.api.TypeDefinition;
import org.opendaylight.yangtools.yang.model.api.type.IdentityrefTypeDefinition;
import org.opendaylight.yangtools.yang.model.api.type.UnionTypeDefinition;

public class JavaAttribute extends AbstractAttribute implements TypedAttribute {

    public static final String DESCRIPTION_OF_VALUE_ATTRIBUTE_FOR_UNION = "valueOfArtificialUnionProperty";

    private final Type type;
    private final String nullableDescription, nullableDefault, nullableDefaultWrappedForCode;
    private final TypeProviderWrapper typeProviderWrapper;
    private final TypeDefinition<?> typeDefinition;

    public JavaAttribute(LeafSchemaNode leaf,
            TypeProviderWrapper typeProviderWrapper) {
        super(leaf);
        this.type = typeProviderWrapper.getType(leaf);

        this.typeDefinition = leaf.getType();
        this.typeProviderWrapper = typeProviderWrapper;
        this.nullableDefault = leaf.getDefault();
        this.nullableDefaultWrappedForCode = leaf.getDefault() == null ? null : typeProviderWrapper.getDefault(leaf);
        this.nullableDescription = leaf.getDescription();
    }

    public JavaAttribute(LeafListSchemaNode leaf,
            TypeProviderWrapper typeProviderWrapper) {
        super(leaf);
        this.type = typeProviderWrapper.getType(leaf);
        this.typeDefinition = leaf.getType();
        this.typeProviderWrapper = typeProviderWrapper;
        this.nullableDefault = nullableDefaultWrappedForCode = null;
        this.nullableDescription = leaf.getDescription();
    }

    public boolean isUnion() {
        TypeDefinition<?> base = getBaseType(typeProviderWrapper, typeDefinition);
        return base instanceof UnionTypeDefinition;
    }

    public TypeDefinition<?> getTypeDefinition() {
        return typeDefinition;
    }

    /**
     * Returns the most base type
     */
    private TypeDefinition<?> getBaseType(TypeProviderWrapper typeProviderWrapper, TypeDefinition<?> baseType) {
        while(baseType.getBaseType()!=null) {
            baseType = baseType.getBaseType();
        }
        return baseType;
    }

    public String getNullableDefaultWrappedForCode() {
        return nullableDefaultWrappedForCode;
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
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }

        JavaAttribute that = (JavaAttribute) o;

        if (nullableDefault != null ? !nullableDefault
                .equals(that.nullableDefault) : that.nullableDefault != null) {
            return false;
        }
        if (nullableDescription != null ? !nullableDescription
                .equals(that.nullableDescription)
                : that.nullableDescription != null) {
            return false;
        }
        if (type != null ? !type.equals(that.type) : that.type != null) {
            return false;
        }

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
        TypeDefinition<?> baseTypeDefinition = getBaseType(typeProviderWrapper, typeDefinition);
        Type baseType = typeProviderWrapper.getType(baseTypeDefinition, baseTypeDefinition);

        if (isArray()) {
            return getArrayType();
        } else if (isEnum(baseType)) {
            return getSimpleType(baseType);
        } else if (isUnion()) {
            return getCompositeTypeForUnion(baseTypeDefinition);
        } else if (isDerivedType(baseType, getType())) {
            return getCompositeType(baseType, baseTypeDefinition);
        } else if (isIdentityRef()) {
            return getCompositeTypeForIdentity();
        }

        return getSimpleType(getType());
    }

    public boolean isIdentityRef() {
        return typeDefinition instanceof IdentityrefTypeDefinition;
    }

    private OpenType<?> getCompositeTypeForUnion(TypeDefinition<?> baseTypeDefinition) {
        Preconditions.checkArgument(baseTypeDefinition instanceof UnionTypeDefinition,
                "Expected %s instance but was %s", UnionTypeDefinition.class, baseTypeDefinition);

        List<TypeDefinition<?>> types = ((UnionTypeDefinition) baseTypeDefinition).getTypes();

        String[] itemNames = new String[types.size()+1];
        OpenType<?>[] itemTypes = new OpenType[itemNames.length];

        addArtificialPropertyToUnionCompositeType(baseTypeDefinition, itemNames, itemTypes);

        String description = getNullableDescription() == null ? getAttributeYangName() : getNullableDescription();

        int i = 1;
        for (TypeDefinition<?> innerTypeDefinition : types) {

            Type innerType = typeProviderWrapper.getType(innerTypeDefinition, innerTypeDefinition);

            TypeDefinition<?> baseInnerTypeDefinition = getBaseType(typeProviderWrapper, innerTypeDefinition);
            Type innerTypeBaseType = typeProviderWrapper.getType(baseInnerTypeDefinition, baseInnerTypeDefinition);

            OpenType<?> innerCompositeType;

            if(isDerivedType(innerTypeBaseType, innerType)) {
                innerCompositeType = baseInnerTypeDefinition instanceof UnionTypeDefinition ?
                        getCompositeTypeForUnion(baseInnerTypeDefinition) :
                        getCompositeType(innerTypeBaseType, baseInnerTypeDefinition);
            } else {
                innerCompositeType = SimpleTypeResolver.getSimpleType(innerType);
            }

            itemNames[i] = typeProviderWrapper.getJMXParamForUnionInnerType(innerTypeDefinition);
            itemTypes[i++] = innerCompositeType;
        }

        String[] descriptions = itemNames.clone();
        descriptions[0] = DESCRIPTION_OF_VALUE_ATTRIBUTE_FOR_UNION;

        try {
            return new CompositeType(getUpperCaseCammelCase(), description, itemNames, descriptions, itemTypes);
        } catch (OpenDataException e) {
            throw new RuntimeException("Unable to create " + CompositeType.class + " with inner elements "
                    + Arrays.toString(itemTypes), e);
        }
    }

    public static final Class<Character> TYPE_OF_ARTIFICIAL_UNION_PROPERTY = char.class;

    private void addArtificialPropertyToUnionCompositeType(TypeDefinition<?> baseTypeDefinition, String[] itemNames, OpenType<?>[] itemTypes) {
        String artificialPropertyName = typeProviderWrapper.getJMXParamForBaseType(baseTypeDefinition);
        itemNames[0] = artificialPropertyName;

        OpenType<?> artificialPropertyType = getArrayOpenTypeForSimpleType(TYPE_OF_ARTIFICIAL_UNION_PROPERTY.getName(),
                SimpleTypeResolver.getSimpleType(TYPE_OF_ARTIFICIAL_UNION_PROPERTY.getName()));
        itemTypes[0] = artificialPropertyType;
    }

    private boolean isEnum(Type baseType) {
        return baseType.getFullyQualifiedName().equals(Enum.class.getName());
    }

    private OpenType<?> getSimpleType(Type type) {
        SimpleType<?> simpleType = SimpleTypeResolver.getSimpleType(type);
        return simpleType;
    }

    private OpenType<?> getCompositeType(Type baseType, TypeDefinition<?> baseTypeDefinition) {

        SimpleType<?> innerItemType = SimpleTypeResolver.getSimpleType(baseType);
        String innerItemName = typeProviderWrapper.getJMXParamForBaseType(baseTypeDefinition);

        String[] itemNames = new String[]{innerItemName};
        String description = getNullableDescription() == null ? getAttributeYangName() : getNullableDescription();

        OpenType<?>[] itemTypes = new OpenType[]{innerItemType};
        try {
            return new CompositeType(getUpperCaseCammelCase(), description, itemNames, itemNames, itemTypes);
        } catch (OpenDataException e) {
            throw new RuntimeException("Unable to create " + CompositeType.class + " with inner element of type "
                    + itemTypes, e);
        }
    }

    public OpenType<?> getCompositeTypeForIdentity() {
        String[] itemNames = new String[]{IdentityAttributeRef.QNAME_ATTR_NAME};
        String description = getNullableDescription() == null ? getAttributeYangName() : getNullableDescription();
        OpenType<?>[] itemTypes = new OpenType[]{SimpleType.STRING};

        try {
            return new CompositeType(getUpperCaseCammelCase(), description, itemNames, itemNames, itemTypes);
        } catch (OpenDataException e) {
            throw new RuntimeException("Unable to create " + CompositeType.class + " with inner element of type "
                    + itemTypes, e);
        }
    }

    private OpenType<?> getArrayType() {
        String innerTypeFullyQName = getInnerType(getType());
        SimpleType<?> innerSimpleType = SimpleTypeResolver.getSimpleType(innerTypeFullyQName);
        return getArrayOpenTypeForSimpleType(innerTypeFullyQName, innerSimpleType);
    }

    private OpenType<?> getArrayOpenTypeForSimpleType(String innerTypeFullyQName, SimpleType<?> innerSimpleType) {
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
        if (innerTypeFullyQName.contains(".")) {
            return false;
        }

        return true;
    }

    private boolean isArray() {
        return type.getName().endsWith("[]");
    }

    private boolean isDerivedType(Type baseType, Type currentType) {
        return baseType.equals(currentType) == false;
    }

    private static String getInnerType(Type type) {
        String fullyQualifiedName = type.getFullyQualifiedName();
        return fullyQualifiedName.substring(0, fullyQualifiedName.length() - 2);
    }

}
