/*
 * Copyright (c) 2015, 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.facade.xml.mapping.attributes;

import javax.management.openmbean.ArrayType;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;
import org.opendaylight.controller.config.yangjmxgenerator.attribute.AttributeIfc;
import org.opendaylight.controller.config.yangjmxgenerator.attribute.DependencyAttribute;
import org.opendaylight.controller.config.yangjmxgenerator.attribute.JavaAttribute;
import org.opendaylight.controller.config.yangjmxgenerator.attribute.ListAttribute;
import org.opendaylight.controller.config.yangjmxgenerator.attribute.ListDependenciesAttribute;
import org.opendaylight.controller.config.yangjmxgenerator.attribute.TOAttribute;
import org.opendaylight.yangtools.yang.model.api.type.BinaryTypeDefinition;

public abstract class AttributeIfcSwitchStatement<T> {

    private AttributeIfc lastAttribute;

    public T switchAttribute(final AttributeIfc attributeIfc) {

        this.lastAttribute = attributeIfc;

        OpenType<?> openType = attributeIfc.getOpenType();

        if (attributeIfc instanceof JavaAttribute) {
            try {
                if (((JavaAttribute) attributeIfc).getTypeDefinition() instanceof BinaryTypeDefinition) {
                    return caseJavaBinaryAttribute(openType);
                } else if (((JavaAttribute) attributeIfc).isUnion()) {
                    return caseJavaUnionAttribute(openType);
                } else if (((JavaAttribute) attributeIfc).isIdentityRef()) {
                    return caseJavaIdentityRefAttribute(openType);
                } else if (((JavaAttribute) attributeIfc).isEnum()) {
                    return caseJavaEnumAttribute(openType);
                } else {
                    return caseJavaAttribute(openType);
                }
            } catch (final UnknownOpenTypeException e) {
                throw getIllegalArgumentException(attributeIfc);
            }

        } else if (attributeIfc instanceof DependencyAttribute) {
            return caseDependencyAttribute(((DependencyAttribute) attributeIfc).getOpenType());
        } else if (attributeIfc instanceof ListAttribute) {
            return caseListAttribute((ArrayType<?>) openType);
        } else if (attributeIfc instanceof ListDependenciesAttribute) {
            return caseListDependeciesAttribute((ArrayType<?>) openType);
        } else if (attributeIfc instanceof TOAttribute) {
            return caseTOAttribute(((TOAttribute) attributeIfc).getOpenType());
        }

        throw getIllegalArgumentException(attributeIfc);
    }

    public AttributeIfc getLastAttribute() {
        return lastAttribute;
    }

    protected T caseJavaIdentityRefAttribute(final OpenType<?> openType) {
        return caseJavaAttribute(openType);
    }

    protected T caseJavaUnionAttribute(final OpenType<?> openType) {
        return caseJavaAttribute(openType);
    }

    protected T caseJavaEnumAttribute(final OpenType<?> openType) {
        return caseJavaAttribute(openType);
    }

    protected T caseJavaBinaryAttribute(final OpenType<?> openType) {
        return caseJavaAttribute(openType);
    }

    private IllegalArgumentException getIllegalArgumentException(final AttributeIfc attributeIfc) {
        return new IllegalArgumentException("Unknown attribute type " + attributeIfc.getClass() + ", " + attributeIfc
                + " with open type:" + attributeIfc.getOpenType());
    }

    public final T caseJavaAttribute(final OpenType<?> openType) {
        if (openType instanceof SimpleType<?>) {
            return caseJavaSimpleAttribute((SimpleType<?>) openType);
        } else if (openType instanceof ArrayType<?>) {
            return caseJavaArrayAttribute((ArrayType<?>) openType);
        } else if (openType instanceof CompositeType) {
            return caseJavaCompositeAttribute((CompositeType) openType);
        }

        throw new UnknownOpenTypeException("Unknown attribute open type " + openType);
    }

    protected abstract T caseJavaSimpleAttribute(SimpleType<?> openType);

    protected abstract T caseJavaArrayAttribute(ArrayType<?> openType);

    protected abstract T caseJavaCompositeAttribute(CompositeType openType);

    protected abstract T caseDependencyAttribute(SimpleType<?> attributeIfc);

    protected abstract T caseTOAttribute(CompositeType openType);

    protected abstract T caseListAttribute(ArrayType<?> openType);

    protected abstract T caseListDependeciesAttribute(ArrayType<?> openType);

    private static class UnknownOpenTypeException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        UnknownOpenTypeException(final String message) {
            super(message);
        }
    }
}
