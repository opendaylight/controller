/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.yang.model.parser.builder.impl;

import java.util.ArrayList;
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
import org.opendaylight.controller.yang.model.util.UnionType;

/**
 * Builder for YANG union type. User can add type to this union as
 * TypeDefinition object (resolved type) or in form of TypeDefinitionBuilder.
 * When build is called, types in builder form will be transformed to
 * TypeDefinition objects and add to resolved types.
 */
public class UnionTypeBuilder extends AbstractTypeAwareBuilder implements
        TypeDefinitionBuilder, Builder {
    private final List<TypeDefinition<?>> types;
    private final List<TypeDefinitionBuilder> typedefs;
    private final UnionType instance;

    public UnionTypeBuilder() {
        types = new ArrayList<TypeDefinition<?>>();
        typedefs = new ArrayList<TypeDefinitionBuilder>();
        instance = new UnionType(types);
    }

    public List<TypeDefinition<?>> getTypes() {
        return types;
    }

    @Override
    public TypeDefinition<?> getType() {
        return null;
    }

    public List<TypeDefinitionBuilder> getTypedefs() {
        return Collections.unmodifiableList(typedefs);
    }

    @Override
    public TypeDefinitionBuilder getTypedef() {
        return null;
    }

    @Override
    public void setType(TypeDefinition<?> type) {
        types.add(type);
    }

    @Override
    public void setType(TypeDefinitionBuilder tdb) {
        typedefs.add(tdb);
    }

    @Override
    public UnionType build() {
        for (TypeDefinitionBuilder tdb : typedefs) {
            types.add(tdb.build());
        }
        return instance;
    }

    @Override
    public void setPath(SchemaPath schemaPath) {
        // TODO Auto-generated method stub
    }

    @Override
    public void setDescription(String description) {
        // TODO Auto-generated method stub
    }

    @Override
    public void setReference(String reference) {
        // TODO Auto-generated method stub
    }

    @Override
    public void setStatus(Status status) {
        // TODO Auto-generated method stub
    }

    @Override
    public void addUnknownSchemaNode(
            UnknownSchemaNodeBuilder unknownSchemaNodeBuilder) {
        // TODO Auto-generated method stub
    }

    @Override
    public QName getQName() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public SchemaPath getPath() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getDescription() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getReference() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Status getStatus() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<RangeConstraint> getRanges() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setRanges(List<RangeConstraint> ranges) {
        // TODO Auto-generated method stub
    }

    @Override
    public List<LengthConstraint> getLengths() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setLengths(List<LengthConstraint> lengths) {
        // TODO Auto-generated method stub
    }

    @Override
    public List<PatternConstraint> getPatterns() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setPatterns(List<PatternConstraint> patterns) {
        // TODO Auto-generated method stub
    }

    @Override
    public Integer getFractionDigits() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setFractionDigits(Integer fractionDigits) {
        // TODO Auto-generated method stub
    }

    @Override
    public List<UnknownSchemaNodeBuilder> getUnknownNodes() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object getDefaultValue() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setDefaultValue(Object defaultValue) {
        // TODO Auto-generated method stub
    }

    @Override
    public String getUnits() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setUnits(String units) {
        // TODO Auto-generated method stub
    }

    @Override
    public String toString() {
        String result = "UnionTypeBuilder[";
        result += ", types=" + types;
        result += ", typedefs=" + typedefs;
        result += "]";
        return result;
    }

}
