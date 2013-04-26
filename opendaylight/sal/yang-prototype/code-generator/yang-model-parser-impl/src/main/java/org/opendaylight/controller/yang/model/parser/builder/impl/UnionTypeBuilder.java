/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.yang.model.parser.builder.impl;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
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
 * When build is called, types in builder form will be built and add to resolved
 * types.
 */
public class UnionTypeBuilder extends AbstractTypeAwareBuilder implements
        TypeDefinitionBuilder, Builder {
    private final List<TypeDefinition<?>> types;
    private final List<TypeDefinitionBuilder> typedefs;
    private final UnionType instance;

    private final List<String> actualPath;
    private final URI namespace;
    private final Date revision;

    public UnionTypeBuilder(final List<String> actualPath, final URI namespace,
            final Date revision) {
        types = new ArrayList<TypeDefinition<?>>();
        typedefs = new ArrayList<TypeDefinitionBuilder>();
        instance = new UnionType(actualPath, namespace, revision, types);

        this.actualPath = actualPath;
        this.namespace = namespace;
        this.revision = revision;
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
        for (TypeDefinitionBuilder tdb : typedefs) {
            types.add(tdb.build());
        }
        return instance;
    }

    @Override
    public void setPath(final SchemaPath schemaPath) {
        throw new IllegalStateException("Can not set path to "
                + UnionTypeBuilder.class.getSimpleName());
    }

    @Override
    public void setDescription(final String description) {
        throw new IllegalStateException("Can not set description to "
                + UnionTypeBuilder.class.getSimpleName());
    }

    @Override
    public void setReference(final String reference) {
        throw new IllegalStateException("Can not set reference to "
                + UnionTypeBuilder.class.getSimpleName());
    }

    @Override
    public void setStatus(final Status status) {
        throw new IllegalStateException("Can not set status to "
                + UnionTypeBuilder.class.getSimpleName());
    }

    @Override
    public void addUnknownSchemaNode(final UnknownSchemaNodeBuilder unknownNode) {
        throw new IllegalStateException("Can not add unknown node to "
                + UnionTypeBuilder.class.getSimpleName());
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
                + UnionTypeBuilder.class.getSimpleName());
    }

    @Override
    public List<LengthConstraint> getLengths() {
        return Collections.emptyList();
    }

    @Override
    public void setLengths(List<LengthConstraint> lengths) {
        throw new IllegalStateException("Can not set lengths to "
                + UnionTypeBuilder.class.getSimpleName());
    }

    @Override
    public List<PatternConstraint> getPatterns() {
        return Collections.emptyList();
    }

    @Override
    public void setPatterns(List<PatternConstraint> patterns) {
        throw new IllegalStateException("Can not set patterns to "
                + UnionTypeBuilder.class.getSimpleName());
    }

    @Override
    public Integer getFractionDigits() {
        return null;
    }

    @Override
    public void setFractionDigits(Integer fractionDigits) {
        throw new IllegalStateException("Can not set fraction digits to "
                + UnionTypeBuilder.class.getSimpleName());
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
                + UnionTypeBuilder.class.getSimpleName());
    }

    @Override
    public String getUnits() {
        return null;
    }

    @Override
    public void setUnits(String units) {
        throw new IllegalStateException("Can not set units to "
                + UnionTypeBuilder.class.getSimpleName());
    }

    public List<String> getActualPath() {
        return actualPath;
    }

    public URI getNamespace() {
        return namespace;
    }

    public Date getRevision() {
        return revision;
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
