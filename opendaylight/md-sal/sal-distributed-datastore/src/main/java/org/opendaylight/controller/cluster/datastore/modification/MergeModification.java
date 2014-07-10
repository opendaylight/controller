/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.modification;

import org.opendaylight.controller.cluster.datastore.node.NormalizedNodeToNodeCodec;
import org.opendaylight.controller.cluster.datastore.utils.InstanceIdentifierUtils;
import org.opendaylight.controller.protobuff.messages.common.NormalizedNodeMessages;
import org.opendaylight.controller.protobuff.messages.persistent.PersistentMessages;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreWriteTransaction;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

/**
 * MergeModification stores all the parameters required to merge data into the specified path
 */
public class MergeModification extends AbstractModification {
    private final NormalizedNode data;
    private final SchemaContext schemaContext;


    public MergeModification(YangInstanceIdentifier path, NormalizedNode data,
        SchemaContext schemaContext) {
        super(path);
        this.data = data;
        this.schemaContext = schemaContext;
    }

    @Override
    public void apply(DOMStoreWriteTransaction transaction) {
        transaction.merge(path, data);
    }

    @Override public Object toSerializable() {
        NormalizedNodeMessages.Container encode =
            new NormalizedNodeToNodeCodec(schemaContext).encode(
                path, data);

        return PersistentMessages.Modification.newBuilder()
            .setType(this.getClass().toString())
            .setPath(InstanceIdentifierUtils.toSerializable(this.path))
            .setData(encode.getNormalizedNode())
            .build();

    }

    public static MergeModification fromSerializable(
        Object serializable,
        SchemaContext schemaContext) {
        PersistentMessages.Modification o = (PersistentMessages.Modification) serializable;

        YangInstanceIdentifier path = InstanceIdentifierUtils.fromSerializable(o.getPath());
        NormalizedNode data = new NormalizedNodeToNodeCodec(schemaContext).decode(
            path, o.getData());

        return new MergeModification(path, data, schemaContext);
    }

}
