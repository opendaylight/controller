/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.yang.model.parser.builder.impl;

import java.util.Collections;
import java.util.List;

import org.opendaylight.controller.yang.common.QName;
import org.opendaylight.controller.yang.model.api.SchemaPath;
import org.opendaylight.controller.yang.model.api.Status;
import org.opendaylight.controller.yang.model.api.TypeDefinition;
import org.opendaylight.controller.yang.model.api.type.LengthConstraint;
import org.opendaylight.controller.yang.model.api.type.PatternConstraint;
import org.opendaylight.controller.yang.model.api.type.RangeConstraint;
import org.opendaylight.controller.yang.model.parser.builder.api.AbstractTypeAwareBuilder;
import org.opendaylight.controller.yang.model.parser.builder.api.Builder;
import org.opendaylight.controller.yang.model.parser.builder.api.TypeDefinitionBuilder;
import org.opendaylight.controller.yang.model.util.IdentityrefType;

/**
 * Builder for YANG union type. User can add type to this union as
 * TypeDefinition object (resolved type) or in form of TypeDefinitionBuilder.
 * When build is called, types in builder form will be built and add to resolved
 * types.
 */
public class IdentityrefTypeBuilder extends AbstractTypeAwareBuilder implements
        TypeDefinitionBuilder, Builder {
    private final String baseString;
    private QName baseQName;

    public IdentityrefTypeBuilder(String baseString) {
        this.baseString = baseString;
    }

    public String getBaseString() {
        return baseString;
    }

    public void setBaseQName(QName baseQName) {
        this.baseQName = baseQName;
    }

    @Override
    public TypeDefinition<?> getType() {
        return null;
    }

    @Override
    public TypeDefinitionBuilder getTypedef() {
        return null;
    }

    @Override
    public void setType(final TypeDefinition<?> type) {
        throw new IllegalStateException("Can not set type to "
                + IdentityrefTypeBuilder.class.getSimpleName());
    }

    @Override
    public void setType(final TypeDefinitionBuilder tdb) {
        throw new IllegalStateException("Can not set type to "
                + IdentityrefTypeBuilder.class.getSimpleName());
    }

    @Override
    public IdentityrefType build() {
        return new IdentityrefType(baseQName);
    }

    @Override
    public void setPath(final SchemaPath schemaPath) {
        throw new IllegalStateException("Can not set path to "
                + IdentityrefTypeBuilder.class.getSimpleName());
    }

    @Override
    public void setDescription(final String description) {
        throw new IllegalStateException("Can not set description to "
                + IdentityrefTypeBuilder.class.getSimpleName());
    }

    @Override
    public void setReference(final String reference) {
        throw new IllegalStateException("Can not set reference to "
                + IdentityrefTypeBuilder.class.getSimpleName());
    }

    @Override
    public void setStatus(final Status status) {
        throw new IllegalStateException("Can not set status to "
                + IdentityrefTypeBuilder.class.getSimpleName());
    }

    @Override
    public void addUnknownSchemaNode(final UnknownSchemaNodeBuilder unknownNode) {
        throw new IllegalStateException("Can not add unknown node to "
                + IdentityrefTypeBuilder.class.getSimpleName());
    }

    @Override
    public QName getQName() {
        return null;
    }

    @Override
    public SchemaPath getPath() {
        return null;
    }

    @Override
    public String getDescription() {
        return null;
    }

    @Override
    public String getReference() {
        return null;
    }

    @Override
    public Status getStatus() {
        return null;
    }

    @Override
    public List<RangeConstraint> getRanges() {
        return Collections.emptyList();
    }

    @Override
    public void setRanges(List<RangeConstraint> ranges) {
        throw new IllegalStateException("Can not set ranges to "
                + IdentityrefTypeBuilder.class.getSimpleName());
    }

    @Override
    public List<LengthConstraint> getLengths() {
        return Collections.emptyList();
    }

    @Override
    public void setLengths(List<LengthConstraint> lengths) {
        throw new IllegalStateException("Can not set lengths to "
                + IdentityrefTypeBuilder.class.getSimpleName());
    }

    @Override
    public List<PatternConstraint> getPatterns() {
        return Collections.emptyList();
    }

    @Override
    public void setPatterns(List<PatternConstraint> patterns) {
        throw new IllegalStateException("Can not set patterns to "
                + IdentityrefTypeBuilder.class.getSimpleName());
    }

    @Override
    public Integer getFractionDigits() {
        return null;
    }

    @Override
    public void setFractionDigits(Integer fractionDigits) {
        throw new IllegalStateException("Can not set fraction digits to "
                + IdentityrefTypeBuilder.class.getSimpleName());
    }

    @Override
    public List<UnknownSchemaNodeBuilder> getUnknownNodes() {
        return Collections.emptyList();
    }

    @Override
    public Object getDefaultValue() {
        return null;
    }

    @Override
    public void setDefaultValue(Object defaultValue) {
        throw new IllegalStateException("Can not set default value to "
                + IdentityrefTypeBuilder.class.getSimpleName());
    }

    @Override
    public String getUnits() {
        return null;
    }

    @Override
    public void setUnits(String units) {
        throw new IllegalStateException("Can not set units to "
                + IdentityrefTypeBuilder.class.getSimpleName());
    }

    @Override
    public String toString() {
        final StringBuilder result = new StringBuilder(
                IdentityrefTypeBuilder.class.getSimpleName() + "[");
        result.append(", base=" + baseQName);
        result.append("]");
        return result.toString();
    }

}
