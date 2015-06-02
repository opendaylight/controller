/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.restconf.impl;

import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.SchemaNode;

public class NormalizedNodeContext {

    private final InstanceIdentifierContext<? extends SchemaNode> context;
    private final NormalizedNode<?,?> data;
    private final WriterParameters writerParameters;

    public NormalizedNodeContext(final InstanceIdentifierContext<? extends SchemaNode> context,
                                 final NormalizedNode<?, ?> data, WriterParameters writerParameters) {
        this.context = context;
        this.data = data;
        this.writerParameters = writerParameters;
    }

    public NormalizedNodeContext(final InstanceIdentifierContext<? extends SchemaNode> context,
                                 final NormalizedNode<?, ?> data) {
        this.context = context;
        this.data = data;
        // default writer parameters
        this.writerParameters = new WriterParameters.WriterParametersBuilder().build();
    }

    public InstanceIdentifierContext<? extends SchemaNode> getInstanceIdentifierContext() {
        return context;
    }

    public NormalizedNode<?, ?> getData() {
        return data;
    }

    public WriterParameters getWriterParameters() {
        return writerParameters;
    }
}
