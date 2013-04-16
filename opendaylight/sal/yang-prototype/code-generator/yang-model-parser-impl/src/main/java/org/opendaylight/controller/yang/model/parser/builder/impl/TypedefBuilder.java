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
import org.opendaylight.controller.yang.model.api.UnknownSchemaNode;
import org.opendaylight.controller.yang.model.api.type.LengthConstraint;
import org.opendaylight.controller.yang.model.api.type.PatternConstraint;
import org.opendaylight.controller.yang.model.api.type.RangeConstraint;
import org.opendaylight.controller.yang.model.parser.builder.api.AbstractTypeAwareBuilder;
import org.opendaylight.controller.yang.model.parser.builder.api.TypeDefinitionBuilder;
import org.opendaylight.controller.yang.model.parser.util.YangParseException;
import org.opendaylight.controller.yang.model.util.ExtendedType;
import org.opendaylight.controller.yang.model.util.UnknownType;

public class TypedefBuilder extends AbstractTypeAwareBuilder implements
        TypeDefinitionBuilder {
    private final QName qname;
    private SchemaPath schemaPath;

    private final List<UnknownSchemaNodeBuilder> addedUnknownNodes = new ArrayList<UnknownSchemaNodeBuilder>();
    private List<RangeConstraint> ranges = Collections.emptyList();
    private List<LengthConstraint> lengths = Collections.emptyList();
    private List<PatternConstraint> patterns = Collections.emptyList();
    private Integer fractionDigits = null;

    private String description;
    private String reference;
    private Status status;
    private String units;
    private Object defaultValue;

    public TypedefBuilder(QName qname) {
        this.qname = qname;
    }

    @Override
    public TypeDefinition<? extends TypeDefinition<?>> build() {
        TypeDefinition<?> result = null;
        ExtendedType.Builder typeBuilder = null;
        if ((type == null || type instanceof UnknownType) && typedef == null) {
            throw new YangParseException("Unresolved type: '"
                    + qname.getLocalName() + "'.");
        }
        if (type == null || type instanceof UnknownType) {
            typeBuilder = new ExtendedType.Builder(qname, typedef.build(),
                    description, reference);
        } else {
            typeBuilder = new ExtendedType.Builder(qname, type, description,
                    reference);
        }
        typeBuilder.status(status);
        typeBuilder.units(units);
        typeBuilder.defaultValue(defaultValue);

        typeBuilder.ranges(ranges);
        typeBuilder.lengths(lengths);
        typeBuilder.patterns(patterns);

        // UNKNOWN NODES
        final List<UnknownSchemaNode> unknownNodes = new ArrayList<UnknownSchemaNode>();
        for (UnknownSchemaNodeBuilder b : addedUnknownNodes) {
            unknownNodes.add(b.build());
        }
        typeBuilder.unknownSchemaNodes(unknownNodes);
        result = typeBuilder.build();
        return result;
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
    public String getUnits() {
        return units;
    }

    @Override
    public void setUnits(String units) {
        this.units = units;
    }

    @Override
    public Object getDefaultValue() {
        return defaultValue;
    }

    @Override
    public void setDefaultValue(Object defaultValue) {
        this.defaultValue = defaultValue;
    }

    @Override
    public List<UnknownSchemaNodeBuilder> getUnknownNodes() {
        return addedUnknownNodes;
    }

    @Override
    public void addUnknownSchemaNode(UnknownSchemaNodeBuilder unknownNode) {
        addedUnknownNodes.add(unknownNode);
    }

    @Override
    public List<RangeConstraint> getRanges() {
        return ranges;
    }

    @Override
    public void setRanges(List<RangeConstraint> ranges) {
        if (ranges != null) {
            this.ranges = ranges;
        }
    }

    @Override
    public List<LengthConstraint> getLengths() {
        return lengths;
    }

    @Override
    public void setLengths(List<LengthConstraint> lengths) {
        if (lengths != null) {
            this.lengths = lengths;
        }
    }

    @Override
    public List<PatternConstraint> getPatterns() {
        return patterns;
    }

    @Override
    public void setPatterns(List<PatternConstraint> patterns) {
        if (patterns != null) {
            this.patterns = patterns;
        }
    }

    @Override
    public Integer getFractionDigits() {
        return fractionDigits;
    }

    @Override
    public void setFractionDigits(Integer fractionDigits) {
        this.fractionDigits = fractionDigits;
    }

    @Override
    public String toString() {
        String result = "TypedefBuilder[" + qname.getLocalName();
        result += ", type=";
        if (type == null) {
            result += typedef;
        } else {
            result += type;
        }
        result += "]";
        return result;
    }

}
