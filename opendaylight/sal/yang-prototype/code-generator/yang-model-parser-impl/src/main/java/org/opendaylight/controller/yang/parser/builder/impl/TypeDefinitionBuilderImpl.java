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
import org.opendaylight.controller.yang.model.api.UnknownSchemaNode;
import org.opendaylight.controller.yang.model.api.type.LengthConstraint;
import org.opendaylight.controller.yang.model.api.type.PatternConstraint;
import org.opendaylight.controller.yang.model.api.type.RangeConstraint;
import org.opendaylight.controller.yang.model.util.ExtendedType;
import org.opendaylight.controller.yang.model.util.UnknownType;
import org.opendaylight.controller.yang.parser.builder.api.AbstractTypeAwareBuilder;
import org.opendaylight.controller.yang.parser.builder.api.TypeDefinitionBuilder;
import org.opendaylight.controller.yang.parser.util.YangParseException;

public final class TypeDefinitionBuilderImpl extends AbstractTypeAwareBuilder implements TypeDefinitionBuilder {
    private final int line;
    private final QName qname;
    private SchemaPath schemaPath;

    private List<UnknownSchemaNode> unknownNodes;
    private final List<UnknownSchemaNodeBuilder> addedUnknownNodes = new ArrayList<UnknownSchemaNodeBuilder>();
    private List<RangeConstraint> ranges = Collections.emptyList();
    private List<LengthConstraint> lengths = Collections.emptyList();
    private List<PatternConstraint> patterns = Collections.emptyList();
    private Integer fractionDigits = null;

    private String description;
    private String reference;
    private Status status = Status.CURRENT;
    private String units;
    private Object defaultValue;
    private boolean addedByUses;

    public TypeDefinitionBuilderImpl(final QName qname, final int line) {
        this.qname = qname;
        this.line = line;
    }

    public TypeDefinitionBuilderImpl(TypeDefinitionBuilder tdb) {
        qname = tdb.getQName();
        line = tdb.getLine();
        schemaPath = tdb.getPath();

        type = tdb.getType();
        typedef = tdb.getTypedef();

        unknownNodes = tdb.getUnknownNodes();
        for (UnknownSchemaNodeBuilder usnb : tdb.getUnknownNodeBuilders()) {
            addedUnknownNodes.add(usnb);
        }
        ranges = tdb.getRanges();
        lengths = tdb.getLengths();
        patterns = tdb.getPatterns();
        fractionDigits = tdb.getFractionDigits();

        description = tdb.getDescription();
        reference = tdb.getReference();
        status = tdb.getStatus();
        units = tdb.getUnits();
        defaultValue = tdb.getDefaultValue();
        addedByUses = tdb.isAddedByUses();
    }

    @Override
    public TypeDefinition<? extends TypeDefinition<?>> build() {
        TypeDefinition<?> result = null;
        ExtendedType.Builder typeBuilder = null;
        if ((type == null || type instanceof UnknownType) && typedef == null) {
            throw new YangParseException("Unresolved type: '" + qname.getLocalName() + "'.");
        }
        if (type == null || type instanceof UnknownType) {
            type = typedef.build();
        }

        typeBuilder = new ExtendedType.Builder(qname, type, description, reference, schemaPath);

        typeBuilder.status(status);
        typeBuilder.units(units);
        typeBuilder.defaultValue(defaultValue);
        typeBuilder.addedByUses(addedByUses);

        typeBuilder.ranges(ranges);
        typeBuilder.lengths(lengths);
        typeBuilder.patterns(patterns);
        typeBuilder.fractionDigits(fractionDigits);

        // UNKNOWN NODES
        if (unknownNodes == null) {
            unknownNodes = new ArrayList<UnknownSchemaNode>();
            for (UnknownSchemaNodeBuilder b : addedUnknownNodes) {
                unknownNodes.add(b.build());
            }
        }
        typeBuilder.unknownSchemaNodes(unknownNodes);
        result = typeBuilder.build();
        return result;
    }

    @Override
    public int getLine() {
        return line;
    }

    @Override
    public QName getQName() {
        return qname;
    }

    @Override
    public SchemaPath getPath() {
        return schemaPath;
    }

    @Override
    public void setPath(final SchemaPath schemaPath) {
        this.schemaPath = schemaPath;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public void setDescription(final String description) {
        this.description = description;
    }

    @Override
    public String getReference() {
        return reference;
    }

    @Override
    public void setReference(final String reference) {
        this.reference = reference;
    }

    @Override
    public Status getStatus() {
        return status;
    }

    @Override
    public void setStatus(final Status status) {
        if (status != null) {
            this.status = status;
        }
    }

    @Override
    public boolean isAddedByUses() {
        return addedByUses;
    }

    @Override
    public void setAddedByUses(final boolean addedByUses) {
        this.addedByUses = addedByUses;
    }

    @Override
    public String getUnits() {
        return units;
    }

    @Override
    public void setUnits(final String units) {
        this.units = units;
    }

    @Override
    public Object getDefaultValue() {
        return defaultValue;
    }

    @Override
    public void setDefaultValue(final Object defaultValue) {
        this.defaultValue = defaultValue;
    }

    @Override
    public List<UnknownSchemaNode> getUnknownNodes() {
        return Collections.emptyList();
    }

    @Override
    public List<UnknownSchemaNodeBuilder> getUnknownNodeBuilders() {
        return addedUnknownNodes;
    }

    @Override
    public void addUnknownSchemaNode(final UnknownSchemaNodeBuilder unknownNode) {
        addedUnknownNodes.add(unknownNode);
    }

    public void setUnknownNodes(List<UnknownSchemaNode> unknownNodes) {
        this.unknownNodes = unknownNodes;
    }

    @Override
    public List<RangeConstraint> getRanges() {
        return ranges;
    }

    @Override
    public void setRanges(final List<RangeConstraint> ranges) {
        if (ranges != null) {
            this.ranges = ranges;
        }
    }

    @Override
    public List<LengthConstraint> getLengths() {
        return lengths;
    }

    @Override
    public void setLengths(final List<LengthConstraint> lengths) {
        if (lengths != null) {
            this.lengths = lengths;
        }
    }

    @Override
    public List<PatternConstraint> getPatterns() {
        return patterns;
    }

    @Override
    public void setPatterns(final List<PatternConstraint> patterns) {
        if (patterns != null) {
            this.patterns = patterns;
        }
    }

    @Override
    public Integer getFractionDigits() {
        return fractionDigits;
    }

    @Override
    public void setFractionDigits(final Integer fractionDigits) {
        this.fractionDigits = fractionDigits;
    }

    @Override
    public String toString() {
        final StringBuilder result = new StringBuilder("TypedefBuilder[" + qname.getLocalName());
        result.append(", type=");
        if (type == null) {
            result.append(typedef);
        } else {
            result.append(type);
        }
        result.append("]");
        return result.toString();
    }

}
