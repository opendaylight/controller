/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.yang.model.parser.builder.impl;

import java.util.ArrayList;
import java.util.List;

import org.opendaylight.controller.yang.common.QName;
import org.opendaylight.controller.yang.model.api.TypeDefinition;
import org.opendaylight.controller.yang.model.parser.builder.api.Builder;
import org.opendaylight.controller.yang.model.parser.builder.api.TypeAwareBuilder;
import org.opendaylight.controller.yang.model.parser.builder.api.TypeDefinitionBuilder;
import org.opendaylight.controller.yang.model.util.UnionType;

public class UnionTypeBuilder implements TypeAwareBuilder, TypeDefinitionBuilder, Builder {

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
        return instance;
    }

    @Override
    public void setType(TypeDefinition<?> type) {
        types.add(type);
    }

    public void addType(TypeDefinitionBuilder tdb) {
        typedefs.add(tdb);
    }

    @Override
    public UnionType build() {
        for(TypeDefinitionBuilder tdb : typedefs) {
            types.add(tdb.build());
        }
        return instance;
    }

    @Override
    public QName getQName() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public TypeDefinition<?> getBaseType() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setUnits(String units) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setDefaultValue(Object defaultValue) {
        // TODO Auto-generated method stub

    }

}
