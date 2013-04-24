/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.yang.model.parser.builder.api;

import org.opendaylight.controller.yang.model.api.TypeDefinition;

/**
 * Basic implementation for TypeAwareBuilder builders.
 */
public class AbstractTypeAwareBuilder implements TypeAwareBuilder {

    protected TypeDefinition<?> type;
    protected TypeDefinitionBuilder typedef;

    @Override
    public TypeDefinition<?> getType() {
        return type;
    }

    @Override
    public TypeDefinitionBuilder getTypedef() {
        return typedef;
    }

    @Override
    public void setType(TypeDefinition<?> type) {
        this.type = type;
        this.typedef = null;
    }

    @Override
    public void setType(TypeDefinitionBuilder typedef) {
        this.typedef = typedef;
        this.type = null;
    }

}
