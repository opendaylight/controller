/*
  * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
  *
  * This program and the accompanying materials are made available under the
  * terms of the Eclipse Public License v1.0 which accompanies this distribution,
  * and is available at http://www.eclipse.org/legal/epl-v10.html
  */
package org.opendaylight.controller.model.util;

import java.util.Collections;
import java.util.List;

import org.opendaylight.controller.model.api.type.IdentityTypeDefinition;
import org.opendaylight.controller.model.api.type.IdentityrefTypeDefinition;
import org.opendaylight.controller.yang.common.QName;
import org.opendaylight.controller.yang.model.api.RevisionAwareXPath;
import org.opendaylight.controller.yang.model.api.SchemaPath;
import org.opendaylight.controller.yang.model.api.Status;
import org.opendaylight.controller.yang.model.api.UnknownSchemaNode;

public class Identityref implements IdentityrefTypeDefinition {

    private final QName name = BaseTypes.constructQName("identityref");
    private final SchemaPath path = BaseTypes.schemaPath(name);
    private final String description = "";
    private final String reference = "";

    private final IdentityTypeDefinition identity;
    private final RevisionAwareXPath xpath;

    private String units = "";

    public Identityref(RevisionAwareXPath xpath, IdentityTypeDefinition identity) {
        super();
        this.identity = identity;
        this.xpath = xpath;
    }
    
    public Identityref(RevisionAwareXPath xpath) {
        super();
        this.xpath = xpath;
        this.identity = null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.opendaylight.controller.yang.model.api.TypeDefinition#getBaseType()
     */
    @Override
    public IdentityTypeDefinition getBaseType() {
        return identity;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.opendaylight.controller.yang.model.api.TypeDefinition#getUnits()
     */
    @Override
    public String getUnits() {
        return units;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.opendaylight.controller.yang.model.api.TypeDefinition#getDefaultValue()
     */
    @Override
    public Object getDefaultValue() {
        return identity;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.opendaylight.controller.yang.model.api.SchemaNode#getQName()
     */
    @Override
    public QName getQName() {
        return name;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.opendaylight.controller.yang.model.api.SchemaNode#getPath()
     */
    @Override
    public SchemaPath getPath() {
        return path;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.opendaylight.controller.yang.model.api.SchemaNode#getDescription()
     */
    @Override
    public String getDescription() {
        return description;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.opendaylight.controller.yang.model.api.SchemaNode#getReference()
     */
    @Override
    public String getReference() {
        return reference;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.opendaylight.controller.yang.model.api.SchemaNode#getStatus()
     */
    @Override
    public Status getStatus() {
        return Status.CURRENT;
    }

    @Override
    public List<UnknownSchemaNode> getUnknownSchemaNodes() {
        return Collections.emptyList();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.opendaylight.controller.yang.model.base.type.api.IdentityrefTypeDefinition#getIdentityName
     * ()
     */
    @Override
    public IdentityTypeDefinition getIdentity() {
        return identity;
    }
    
    @Override
    public RevisionAwareXPath getPathStatement() {
        return xpath;
    }
    
    
}
