/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.yang.parser.builder.api;

import org.opendaylight.controller.yang.common.QName;
import org.opendaylight.controller.yang.model.api.TypeDefinition;

/**
 * Basic implementation for TypeAwareBuilder builders.
 */
public abstract class AbstractTypeAwareBuilder implements TypeAwareBuilder {
    protected final int line;
    protected final QName qname;
    protected Builder parent;
    protected TypeDefinition<?> type;
    protected TypeDefinitionBuilder typedef;

    public AbstractTypeAwareBuilder(final int line, final QName qname) {
        this.line = line;
        this.qname = qname;
    }

    @Override
    public int getLine() {
        return line;
    }

    @Override
    public Builder getParent() {
        return parent;
    }

    @Override
    public void setParent(final Builder parent) {
        this.parent = parent;
    }

    @Override
    public QName getQName() {
        return qname;
    }

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
    public void setTypedef(TypeDefinitionBuilder typedef) {
        this.typedef = typedef;
        this.type = null;
    }

}
