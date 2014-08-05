/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.rest.gson;

import static org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeStreamWriter.UNKNOWN_SIZE;

import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;

class ContainerNodeDataWithSchema extends CompositeNodeDataWithSchema {

    public ContainerNodeDataWithSchema(DataSchemaNode schema) {
        super(schema);
    }

    @Override
    public void writeToStream(NormalizedNodeStreamWriter nnStreamWriter) {
        nnStreamWriter.startContainerNode(provideNodeIdentifier(), UNKNOWN_SIZE);
        super.writeToStream(nnStreamWriter);
        nnStreamWriter.endNode();
    }

}
