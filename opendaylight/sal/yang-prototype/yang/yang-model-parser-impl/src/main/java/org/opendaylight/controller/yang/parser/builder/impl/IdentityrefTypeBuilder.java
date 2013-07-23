/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.yang.parser.builder.impl;

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
import org.opendaylight.controller.yang.model.util.IdentityrefType;
import org.opendaylight.controller.yang.parser.builder.api.AbstractTypeAwareBuilder;
import org.opendaylight.controller.yang.parser.builder.api.TypeDefinitionBuilder;
import org.opendaylight.controller.yang.parser.util.YangParseException;

/**
 * Builder for YANG union type. User can add type to this union as
 * TypeDefinition object (resolved type) or in form of TypeDefinitionBuilder.
 * When build is called, types in builder form will be built and add to resolved
 * types.
 */
public final class IdentityrefTypeBuilder extends AbstractTypeAwareBuilder implements TypeDefinitionBuilder {
    private static final String NAME = "identityref";

    private final String baseString;
    private final SchemaPath schemaPath;
    private QName baseQName;

    IdentityrefTypeBuilder(final String moduleName, final int line, final String baseString, final SchemaPath schemaPath) {
        super(moduleName, line, null);
        this.baseString = baseString;
        this.schemaPath = schemaPath;
    }

    @Override
    public IdentityrefType build() {
        return new IdentityrefType(baseQName, schemaPath);
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
        throw new YangParseException(moduleName, line, "Can not set type to " + NAME);
    }

    @Override
    public void setTypedef(final TypeDefinitionBuilder tdb) {
        throw new YangParseException(moduleName, line, "Can not set type to " + NAME);
    }

    @Override
    public void setPath(final SchemaPath schemaPath) {
        throw new YangParseException(moduleName, line, "Can not set path to " + NAME);
    }

    @Override
    public void setDescription(final String description) {
        throw new YangParseException(moduleName, line, "Can not set description to " + NAME);
    }

    @Override
    public void setReference(final String reference) {
        throw new YangParseException(moduleName, line, "Can not set reference to " + NAME);
    }

    @Override
    public void setStatus(final Status status) {
        throw new YangParseException(moduleName, line, "Can not set status to " + NAME);
    }

    @Override
    public boolean isAddedByUses() {
        return false;
    }

    @Override
    public void setAddedByUses(final boolean addedByUses) {
        throw new YangParseException(moduleName, line, "Identityref type can not be added by uses.");
    }

    @Override
    public List<UnknownSchemaNode> getUnknownNodes() {
        return Collections.emptyList();
    }

    @Override
    public void addUnknownNodeBuilder(final UnknownSchemaNodeBuilder unknownNode) {
        throw new YangParseException(moduleName, line, "Can not add unknown node to " + NAME);
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
        throw new YangParseException(moduleName, line, "Can not set ranges to " + NAME);
    }

    @Override
    public List<LengthConstraint> getLengths() {
        return Collections.emptyList();
    }

    @Override
    public void setLengths(List<LengthConstraint> lengths) {
        throw new YangParseException(moduleName, line, "Can not set lengths to " + NAME);
    }

    @Override
    public List<PatternConstraint> getPatterns() {
        return Collections.emptyList();
    }

    @Override
    public void setPatterns(List<PatternConstraint> patterns) {
        throw new YangParseException(moduleName, line, "Can not set patterns to " + NAME);
    }

    @Override
    public Integer getFractionDigits() {
        return null;
    }

    @Override
    public void setFractionDigits(Integer fractionDigits) {
        throw new YangParseException(moduleName, line, "Can not set fraction digits to " + NAME);
    }

    @Override
    public List<UnknownSchemaNodeBuilder> getUnknownNodeBuilders() {
        return Collections.emptyList();
    }

    @Override
    public Object getDefaultValue() {
        return null;
    }

    @Override
    public void setDefaultValue(Object defaultValue) {
        throw new YangParseException(moduleName, line, "Can not set default value to " + NAME);
    }

    @Override
    public String getUnits() {
        return null;
    }

    @Override
    public void setUnits(String units) {
        throw new YangParseException(moduleName, line, "Can not set units to " + NAME);
    }

    @Override
    public String toString() {
        final StringBuilder result = new StringBuilder(IdentityrefTypeBuilder.class.getSimpleName() + "[");
        result.append(", base=" + baseQName);
        result.append("]");
        return result.toString();
    }

}
