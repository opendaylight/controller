/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.yang.parser.builder.impl;

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
import org.opendaylight.controller.yang.model.util.UnionType;
import org.opendaylight.controller.yang.parser.builder.api.AbstractTypeAwareBuilder;
import org.opendaylight.controller.yang.parser.builder.api.TypeDefinitionBuilder;
import org.opendaylight.controller.yang.parser.util.YangParseException;

/**
 * Builder for YANG union type. User can add type to this union as
 * TypeDefinition object (resolved type) or in form of TypeDefinitionBuilder.
 * When build is called, types in builder form will be built and add to resolved
 * types.
 */
public class UnionTypeBuilder extends AbstractTypeAwareBuilder implements
        TypeDefinitionBuilder {
    private final static String NAME = "union";

    private final int line;
    private final List<TypeDefinition<?>> types;
    private final List<TypeDefinitionBuilder> typedefs;
    private UnionType instance;
    private boolean built;

    private SchemaPath path;

    public UnionTypeBuilder(final int line) {
        this.line = line;
        types = new ArrayList<TypeDefinition<?>>();
        typedefs = new ArrayList<TypeDefinitionBuilder>();
    }

    @Override
    public int getLine() {
        return line;
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
    public void setType(final TypeDefinition<?> type) {
        types.add(type);
    }

    @Override
    public void setType(final TypeDefinitionBuilder tdb) {
        typedefs.add(tdb);
    }

    @Override
    public UnionType build() {
        if (built) {
            return instance;
        } else {
            instance = new UnionType(path, types);
            for (TypeDefinitionBuilder tdb : typedefs) {
                types.add(tdb.build());
            }
            built = true;
            return instance;
        }
    }

    @Override
    public void setPath(final SchemaPath schemaPath) {
        this.path = schemaPath;
    }

    @Override
    public void setDescription(final String description) {
        throw new YangParseException(line, "Can not set description to " + NAME);
    }

    @Override
    public void setReference(final String reference) {
        throw new YangParseException(line, "Can not set reference to " + NAME);
    }

    @Override
    public void setStatus(final Status status) {
        throw new YangParseException(line, "Can not set status to " + NAME);
    }

    @Override
    public void addUnknownSchemaNode(final UnknownSchemaNodeBuilder unknownNode) {
        throw new YangParseException(line, "Can not add unknown node to "
                + NAME);
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
        throw new YangParseException(line, "Can not set ranges to " + NAME);
    }

    @Override
    public List<LengthConstraint> getLengths() {
        return Collections.emptyList();
    }

    @Override
    public void setLengths(List<LengthConstraint> lengths) {
        throw new YangParseException(line, "Can not set lengths to " + NAME);
    }

    @Override
    public List<PatternConstraint> getPatterns() {
        return Collections.emptyList();
    }

    @Override
    public void setPatterns(List<PatternConstraint> patterns) {
        throw new YangParseException(line, "Can not set patterns to " + NAME);
    }

    @Override
    public Integer getFractionDigits() {
        return null;
    }

    @Override
    public void setFractionDigits(Integer fractionDigits) {
        throw new YangParseException(line, "Can not set fraction digits to "
                + NAME);
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
        throw new YangParseException(line, "Can not set default value to "
                + NAME);
    }

    @Override
    public String getUnits() {
        return null;
    }

    @Override
    public void setUnits(String units) {
        throw new YangParseException(line, "Can not set units to " + NAME);
    }

    @Override
    public String toString() {
        final StringBuilder result = new StringBuilder(
                UnionTypeBuilder.class.getSimpleName() + "[");
        result.append(", types=" + types);
        result.append(", typedefs=" + typedefs);
        result.append("]");
        return result.toString();
    }

}
