/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.messages;

import org.opendaylight.controller.cluster.datastore.node.NormalizedNodeToNodeCodec;
import org.opendaylight.controller.cluster.datastore.utils.InstanceIdentifierUtils;
import org.opendaylight.controller.protobuff.messages.common.NormalizedNodeMessages;
import org.opendaylight.controller.protobuff.messages.transaction.ShardTransactionMessages;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

public class WriteData extends ModifyData{

  public static final Class SERIALIZABLE_CLASS = ShardTransactionMessages.WriteData.class;

  public WriteData(YangInstanceIdentifier path, NormalizedNode<?, ?> data, SchemaContext schemaContext) {
    super(path, data, schemaContext);
  }

    @Override public Object toSerializable() {

        NormalizedNodeMessages.Node normalizedNode =
            new NormalizedNodeToNodeCodec(schemaContext).encode(path, data)
                .getNormalizedNode();
        return ShardTransactionMessages.WriteData.newBuilder()
            .setInstanceIdentifierPathArguments(InstanceIdentifierUtils.toSerializable(path))
            .setNormalizedNode(normalizedNode).build();

    }

    public static WriteData fromSerializable(Object serializable, SchemaContext schemaContext){
        ShardTransactionMessages.WriteData o = (ShardTransactionMessages.WriteData) serializable;
        YangInstanceIdentifier identifier = InstanceIdentifierUtils.fromSerializable(o.getInstanceIdentifierPathArguments());

        NormalizedNode<?, ?> normalizedNode =
            new NormalizedNodeToNodeCodec(schemaContext)
                .decode(identifier, o.getNormalizedNode());

        return new WriteData(identifier, normalizedNode, schemaContext);
    }

}
