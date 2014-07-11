/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.messages;

import akka.actor.ActorPath;
import akka.actor.ActorSystem;
import org.opendaylight.controller.protobuff.messages.transaction.ShardTransactionChainMessages;

public class CreateTransactionChainReply implements SerializableMessage {
  public static final Class SERIALIZABLE_CLASS = ShardTransactionChainMessages.CreateTransactionChainReply.class;
  private final ActorPath transactionChainPath;

  public CreateTransactionChainReply(ActorPath transactionChainPath) {
    this.transactionChainPath = transactionChainPath;
  }

  public ActorPath getTransactionChainPath() {
    return transactionChainPath;
  }

  @Override
  public ShardTransactionChainMessages.CreateTransactionChainReply toSerializable() {
    return ShardTransactionChainMessages.CreateTransactionChainReply.newBuilder()
        .setTransactionChainPath(transactionChainPath.toString()).build();
  }

  public static CreateTransactionChainReply fromSerializable(ActorSystem actorSystem,Object serializable){
    ShardTransactionChainMessages.CreateTransactionChainReply o = (ShardTransactionChainMessages.CreateTransactionChainReply) serializable;
    return new CreateTransactionChainReply(
        actorSystem.actorFor(o.getTransactionChainPath()).path());
  }

}
