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
import org.opendaylight.mdsal.binding.model.api.Type;
import org.opendaylight.yangtools.yang.model.api.LeafListSchemaNode;
import org.opendaylight.yangtools.yang.model.api.LeafSchemaNode;
import org.opendaylight.yangtools.yang.model.api.TypeDefinition;
import org.opendaylight.yangtools.yang.model.api.type.EnumTypeDefinition;
import org.opendaylight.yangtools.yang.model.api.type.IdentityrefTypeDefinition;
import org.opendaylight.yangtools.yang.model.api.type.UnionTypeDefinition;
import org.opendaylight.yangtools.yang.model.util.type.CompatUtils;

public class JavaAttribute extends AbstractAttribute implements TypedAttribute {

    public static final String DESCRIPTION_OF_VALUE_ATTRIBUTE_FOR_UNION = "valueOfArtificialUnionProperty";

    private final Type type;
    private final String nullableDescription, nullableDefault, nullableDefaultWrappedForCode;
    private final TypeProviderWrapper typeProviderWrapper;
    private final TypeDefinition<?> typeDefinition;

    public JavaAttribute(final LeafSchemaNode leaf,
            final TypeProviderWrapper typeProviderWrapper) {
        super(leaf);
        this.type = typeProviderWrapper.getType(leaf);

        this.typeDefinition = CompatUtils.compatLeafType(leaf);
        this.typeProviderWrapper = typeProviderWrapper;
        this.nullableDefault = leaf.getDefault();
        this.nullableDefaultWrappedForCode = leaf.getDefault() == null ? null : typeProviderWrapper.getDefault(leaf);
        this.nullableDescription = leaf.getDescription();
    }

    public JavaAttribute(final LeafListSchemaNode leaf,
            final TypeProviderWrapper typeProviderWrapper) {
        super(leaf);
        this.type = typeProviderWrapper.getType(leaf);
        this.typeDefinition = leaf.getType();
        this.typeProviderWrapper = typeProviderWrapper;
        this.nullableDefault = this.nullableDefaultWrappedForCode = null;
        this.nullableDescription = leaf.getDescription();
    }

    public boolean isUnion() {
        final TypeDefinition<?> base = getBaseType(this.typeProviderWrapper, this.typeDefinition);
        return base instanceof UnionTypeDefinition;
    }

    public boolean isEnum() {
        final TypeDefinition<?> base = getBaseType(this.typeProviderWrapper, this.typeDefinition);
        return base instanceof EnumTypeDefinition;
    }

    public TypeDefinition<?> getTypeDefinition() {
        return this.typeDefinition;
    }

    /**
     * Returns the most base type
     */
    private TypeDefinition<?> getBaseType(final TypeProviderWrapper typeProviderWrapper, TypeDefinition<?> baseType) {
        while(baseType.getBaseType()!=null) {
            baseType = baseType.getBaseType();
        }
        return baseType;
    }

    public String getNullableDefaultWrappedForCode() {
        return this.nullableDefaultWrappedForCode;
    }

    @Override
    public Type getType() {
        return this.type;
    }

    @Override
    public String getNullableDescription() {
        return this.nullableDescription;
    }

    @Override
    public String getNullableDefault() {
        return this.nullableDefault;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if ((o == null) || (getClass() != o.getClass())) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }

        final JavaAttribute that = (JavaAttribute) o;

        if (this.nullableDefault != null ? !this.nullableDefault
                .equals(that.nullableDefault) : that.nullableDefault != null) {
            return false;
        }
        if (this.nullableDescription != null ? !this.nullableDescription
                .equals(that.nullableDescription)
                : that.nullableDescription != null) {
            return false;
        }
        if (this.type != null ? !this.type.equals(that.type) : that.type != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = (31 * result) + (this.type != null ? this.type.hashCode() : 0);
        result = (31
                * result)
                + (this.nullableDescription != null ? this.nullableDescription.hashCode()
                        : 0);
        result = (31 * result)
                + (this.nullableDefault != null ? this.nullableDefault.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "JavaAttribute{" + getAttributeYangName() + "," + "type=" + this.type
                + '}';
    }

    @Override
    public OpenType<?> getOpenType() {
        final TypeDefinition<?> baseTypeDefinition = getBaseType(this.typeProviderWrapper, this.typeDefinition);
        final Type baseType = this.typeProviderWrapper.getType(baseTypeDefinition, baseTypeDefinition);

        if (isArray()) {
            return getArrayType();
        } else if (isEnum()) {
            return getEnumType(baseTypeDefinition);
        } else if (isUnion()) {
            return getCompositeTypeForUnion(baseTypeDefinition);
        } else if (isDerivedType(baseType, getType())) {
            return getCompositeType(baseType, baseTypeDefinition);
        } else if (isIdentityRef()) {
            return getCompositeTypeForIdentity();
        }

        return getSimpleType(getType());
    }

    private OpenType<?> getEnumType(final TypeDefinition<?> baseType) {
        final String fullyQualifiedName = this.typeProviderWrapper.getType(this.node, getTypeDefinition()).getFullyQualifiedName();
        final String[] items = {"instance"};
        final String description = getNullableDescription() == null ? getAttributeYangName() : getNullableDescription();

        try {
            return new CompositeType(fullyQualifiedName, description, items, items, new OpenType[]{SimpleType.STRING});
        } catch (final OpenDataException e) {
            throw new RuntimeException("Unable to create enum type" + fullyQualifiedName + " as open type", e);
        }
    }

    public boolean isIdentityRef() {
        return this.typeDefinition instanceof IdentityrefTypeDefinition;
    }

    private OpenType<?> getCompositeTypeForUnion(final TypeDefinition<?> baseTypeDefinition) {
        Preconditions.checkArgument(baseTypeDefinition instanceof UnionTypeDefinition,
                "Expected %s instance but was %s", UnionTypeDefinition.class, baseTypeDefinition);

        final List<TypeDefinition<?>> types = ((UnionTypeDefinition) baseTypeDefinition).getTypes();

        final String[] itemNames = new String[types.size()+1];
        final OpenType<?>[] itemTypes = new OpenType[itemNames.length];

        addArtificialPropertyToUnionCompositeType(baseTypeDefinition, itemNames, itemTypes);

        final String description = getNullableDescription() == null ? getAttributeYangName() : getNullableDescription();

        int i = 1;
        for (final TypeDefinition<?> innerTypeDefinition : types) {

            final Type innerType = this.typeProviderWrapper.getType(innerTypeDefinition, innerTypeDefinition);

            final TypeDefinition<?> baseInnerTypeDefinition = getBaseType(this.typeProviderWrapper, innerTypeDefinition);
            final Type innerTypeBaseType = this.typeProviderWrapper.getType(baseInnerTypeDefinition, baseInnerTypeDefinition);

            OpenType<?> innerCompositeType;

            if(isDerivedType(innerTypeBaseType, innerType)) {
                innerCompositeType = baseInnerTypeDefinition instanceof UnionTypeDefinition ?
                        getCompositeTypeForUnion(baseInnerTypeDefinition) :
                        getCompositeType(innerTypeBaseType, baseInnerTypeDefinition);
            } else {
                innerCompositeType = SimpleTypeResolver.getSimpleType(innerType);
            }

            itemNames[i] = this.typeProviderWrapper.getJMXParamForUnionInnerType(innerTypeDefinition);
            itemTypes[i++] = innerCompositeType;
        }

        final String[] descriptions = itemNames.clone();
        descriptions[0] = DESCRIPTION_OF_VALUE_ATTRIBUTE_FOR_UNION;

        try {
            return new CompositeType(getUpperCaseCammelCase(), description, itemNames, descriptions, itemTypes);
        } catch (final OpenDataException e) {
            throw new RuntimeException("Unable to create " + CompositeType.class + " with inner elements "
                    + Arrays.toString(itemTypes), e);
        }
    }

    public static final Class<Character> TYPE_OF_ARTIFICIAL_UNION_PROPERTY = char.class;

    private void addArtificialPropertyToUnionCompositeType(final TypeDefinition<?> baseTypeDefinition, final String[] itemNames, final OpenType<?>[] itemTypes) {
        final String artificialPropertyName = this.typeProviderWrapper.getJMXParamForBaseType(baseTypeDefinition);
        itemNames[0] = artificialPropertyName;

        final OpenType<?> artificialPropertyType = getArrayOpenTypeForSimpleType(TYPE_OF_ARTIFICIAL_UNION_PROPERTY.getName(),
                SimpleTypeResolver.getSimpleType(TYPE_OF_ARTIFICIAL_UNION_PROPERTY.getName()));
        itemTypes[0] = artificialPropertyType;
    }

    private OpenType<?> getSimpleType(final Type type) {
        final SimpleType<?> simpleType = SimpleTypeResolver.getSimpleType(type);
        return simpleType;
    }

    private OpenType<?> getCompositeType(final Type baseType, final TypeDefinition<?> baseTypeDefinition) {

        final SimpleType<?> innerItemType = SimpleTypeResolver.getSimpleType(baseType);
        final String innerItemName = this.typeProviderWrapper.getJMXParamForBaseType(baseTypeDefinition);

        final String[] itemNames = new String[]{innerItemName};
        final String description = getNullableDescription() == null ? getAttributeYangName() : getNullableDescription();

        final OpenType<?>[] itemTypes = new OpenType[]{innerItemType};
        try {
            return new CompositeType(getUpperCaseCammelCase(), description, itemNames, itemNames, itemTypes);
        } catch (final OpenDataException e) {
            throw new RuntimeException("Unable to create " + CompositeType.class + " with inner element of type "
                    + itemTypes, e);
        }
    }

    public OpenType<?> getCompositeTypeForIdentity() {
        final String[] itemNames = new String[]{IdentityAttributeRef.QNAME_ATTR_NAME};
        final String description = getNullableDescription() == null ? getAttributeYangName() : getNullableDescription();
        final OpenType<?>[] itemTypes = new OpenType[]{SimpleType.STRING};

        try {
            return new CompositeType(getUpperCaseCammelCase(), description, itemNames, itemNames, itemTypes);
        } catch (final OpenDataException e) {
            throw new RuntimeException("Unable to create " + CompositeType.class + " with inner element of type "
                    + itemTypes, e);
        }
    }

    private OpenType<?> getArrayType() {
        final String innerTypeFullyQName = getInnerType(getType());
        final SimpleType<?> innerSimpleType = SimpleTypeResolver.getSimpleType(innerTypeFullyQName);
        return getArrayOpenTypeForSimpleType(innerTypeFullyQName, innerSimpleType);
    }

    private OpenType<?> getArrayOpenTypeForSimpleType(final String innerTypeFullyQName, final SimpleType<?> innerSimpleType) {
        try {
            final ArrayType<Object> arrayType = isPrimitive(innerTypeFullyQName) ? new ArrayType<>(innerSimpleType, true)
                    : new ArrayType<>(1, innerSimpleType);
            return arrayType;
        } catch (final OpenDataException e) {
            throw new RuntimeException("Unable to create " + ArrayType.class + " with inner element of type "
                    + innerSimpleType, e);
        }
    }

    // TODO verify
    private boolean isPrimitive(final String innerTypeFullyQName) {
        if (innerTypeFullyQName.contains(".")) {
            return false;
        }

        return true;
    }

    private boolean isArray() {
        return this.type.getName().endsWith("[]");
    }

    private boolean isDerivedType(final Type baseType, final Type currentType) {
        return baseType.equals(currentType) == false;
    }

    private static String getInnerType(final Type type) {
        final String fullyQualifiedName = type.getFullyQualifiedName();
        return fullyQualifiedName.substring(0, fullyQualifiedName.length() - 2);
    }

}
