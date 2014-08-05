/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.rest.gson;

import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;

class AnyXmlNodeDataWithSchema extends SimpleNodeDataWithSchema {

    public AnyXmlNodeDataWithSchema(final DataSchemaNode dataSchemaNode) {
        super(dataSchemaNode);
    }

    @Override
    public void writeToStream(NormalizedNodeStreamWriter nnStreamWriter) {
//        FIXME: should be changed according to format of value
        nnStreamWriter.anyxmlNode(provideNodeIdentifier(), value);
    }


}
