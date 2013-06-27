/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.yang.model.util;

import java.util.Collections;
import java.util.List;

import org.opendaylight.controller.yang.common.QName;
import org.opendaylight.controller.yang.model.api.RevisionAwareXPath;
import org.opendaylight.controller.yang.model.api.SchemaPath;
import org.opendaylight.controller.yang.model.api.Status;
import org.opendaylight.controller.yang.model.api.UnknownSchemaNode;
import org.opendaylight.controller.yang.model.api.type.InstanceIdentifierTypeDefinition;

/**
 * The <code>default</code> implementation of Instance Identifier Type
 * Definition interface.
 *
 * @see InstanceIdentifierTypeDefinition
 */
public final class InstanceIdentifier implements InstanceIdentifierTypeDefinition {
    private static final QName name = BaseTypes.constructQName("instance-identifier");
    private static final String description = "The instance-identifier built-in type is used to "
            + "uniquely identify a particular instance node in the data tree.";
    private static final String reference = "https://tools.ietf.org/html/rfc6020#section-9.13";

    private final transient SchemaPath path;
    private final RevisionAwareXPath xpath;
    private final String units = "";
    private final InstanceIdentifierTypeDefinition baseType;
    private boolean requireInstance = true;

    public InstanceIdentifier(final SchemaPath path, final RevisionAwareXPath xpath) {
        super();
        this.path = path;
        this.xpath = xpath;
        this.baseType = this;
    }

    public InstanceIdentifier(final SchemaPath path, final RevisionAwareXPath xpath, final boolean requireInstance) {
        super();
        this.path = path;
        this.xpath = xpath;
        this.requireInstance = requireInstance;
        this.baseType = this;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.opendaylight.controller.yang.model.api.TypeDefinition#getBaseType()
     */
    @Override
    public InstanceIdentifierTypeDefinition getBaseType() {
        return baseType;
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
     * @see
     * org.opendaylight.controller.yang.model.api.TypeDefinition#getDefaultValue
     * ()
     */
    @Override
    public Object getDefaultValue() {
        return xpath;
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
     * @see
     * org.opendaylight.controller.yang.model.api.SchemaNode#getDescription()
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

    /*
     * (non-Javadoc)
     *
     * @see
     * org.opendaylight.controller.yang.model.api.SchemaNode#getExtensionSchemaNodes
     * ()
     */
    @Override
    public List<UnknownSchemaNode> getUnknownSchemaNodes() {
        return Collections.emptyList();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.opendaylight.controller.yang.model.api.type.
     * InstanceIdentifierTypeDefinition# getPathStatement()
     */
    @Override
    public RevisionAwareXPath getPathStatement() {
        return xpath;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.opendaylight.controller.yang.model.api.type.
     * InstanceIdentifierTypeDefinition# requireInstance()
     */
    @Override
    public boolean requireInstance() {
        return requireInstance;
    }

}
