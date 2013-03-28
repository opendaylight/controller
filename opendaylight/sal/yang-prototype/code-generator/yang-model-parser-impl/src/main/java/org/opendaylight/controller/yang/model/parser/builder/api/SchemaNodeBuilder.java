/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.yang.model.parser.builder.api;

import org.opendaylight.controller.yang.common.QName;
import org.opendaylight.controller.yang.model.api.SchemaPath;
import org.opendaylight.controller.yang.model.api.Status;
import org.opendaylight.controller.yang.model.parser.builder.impl.UnknownSchemaNodeBuilder;

/**
 * Interface for all builders of SchemaNode nodes.
 */
public interface SchemaNodeBuilder extends Builder {

    QName getQName();

    void setPath(SchemaPath schemaPath);

    void setDescription(String description);

    void setReference(String reference);

    void setStatus(Status status);

    void addUnknownSchemaNode(UnknownSchemaNodeBuilder unknownSchemaNodeBuilder);

}
