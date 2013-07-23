/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.yang.parser.builder.api;

import java.util.List;

import org.opendaylight.controller.yang.model.api.TypeDefinition;
import org.opendaylight.controller.yang.model.api.UnknownSchemaNode;
import org.opendaylight.controller.yang.model.api.type.LengthConstraint;
import org.opendaylight.controller.yang.model.api.type.PatternConstraint;
import org.opendaylight.controller.yang.model.api.type.RangeConstraint;

/**
 * Interface for builders of 'typedef' statement.
 */
public interface TypeDefinitionBuilder extends TypeAwareBuilder, SchemaNodeBuilder, GroupingMember {

    TypeDefinition<?> build();

    List<RangeConstraint> getRanges();

    void setRanges(List<RangeConstraint> ranges);

    List<LengthConstraint> getLengths();

    void setLengths(List<LengthConstraint> lengths);

    List<PatternConstraint> getPatterns();

    void setPatterns(List<PatternConstraint> patterns);

    Integer getFractionDigits();

    void setFractionDigits(Integer fractionDigits);

    List<UnknownSchemaNode> getUnknownNodes();

    Object getDefaultValue();

    void setDefaultValue(Object defaultValue);

    String getUnits();

    void setUnits(String units);

}
