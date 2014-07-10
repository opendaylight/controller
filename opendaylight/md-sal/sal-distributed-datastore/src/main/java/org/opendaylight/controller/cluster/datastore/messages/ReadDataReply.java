/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.messages;

import org.opendaylight.controller.cluster.datastore.node.NormalizedNodeToNodeCodec;
import org.opendaylight.controller.protobuff.messages.transaction.ShardTransactionMessages;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

public class ReadDataReply implements SerializableMessage{

  private final NormalizedNode<?, ?> normalizedNode;
  private final SchemaContext schemaContext;
  public static final Class SERIALIZABLE_CLASS = ShardTransactionMessages.ReadDataReply.class;
  public ReadDataReply(SchemaContext context,NormalizedNode<?, ?> normalizedNode){

    this.normalizedNode = normalizedNode;
    this.schemaContext = context;
  }

  public NormalizedNode<?, ?> getNormalizedNode() {
    return normalizedNode;
  }

  public Object toSerializable(){
    if(normalizedNode != null) {
      return ShardTransactionMessages.ReadDataReply.newBuilder()
          .setNormalizedNode(new NormalizedNodeToNodeCodec(schemaContext)
                  .encode(YangInstanceIdentifier.builder().build(), normalizedNode).getNormalizedNode()
          ).build();
    }else{
      return ShardTransactionMessages.ReadDataReply.newBuilder().build();

    }

  }

  public static ReadDataReply fromSerializable(SchemaContext schemaContext,YangInstanceIdentifier id,Object serializable){
    ShardTransactionMessages.ReadDataReply o = (ShardTransactionMessages.ReadDataReply) serializable;
    return new ReadDataReply(schemaContext,new NormalizedNodeToNodeCodec(schemaContext).decode(id, o.getNormalizedNode()));
  }
}
