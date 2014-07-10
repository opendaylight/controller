/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.messages;

import com.google.common.base.Preconditions;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

public abstract class ModifyData implements SerializableMessage {
    protected final YangInstanceIdentifier path;
    protected final NormalizedNode<?, ?> data;
    protected final SchemaContext schemaContext;

    public ModifyData(YangInstanceIdentifier path, NormalizedNode<?, ?> data,
        SchemaContext context) {
        Preconditions.checkNotNull(context,
            "Cannot serialize an object which does not have a schema schemaContext");


        this.path = path;
        this.data = data;
        this.schemaContext = context;
    }

    public YangInstanceIdentifier getPath() {
        return path;
    }

    public NormalizedNode<?, ?> getData() {
        return data;
    }

}
