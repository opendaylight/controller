/*
 *
 *  Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

package org.opendaylight.controller.cluster.datastore;

import akka.actor.ActorRef;
import akka.actor.PoisonPill;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import org.opendaylight.controller.cluster.datastore.messages.CloseTransaction;
import org.opendaylight.controller.cluster.datastore.messages.CloseTransactionReply;
import org.opendaylight.controller.cluster.datastore.messages.ReadData;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreTransactionChain;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

/**
 * @author: syedbahm
 * Date: 8/6/14
 */
public class ShardReadTransaction extends ShardTransaction {
  private final DOMStoreReadTransaction transaction;
  private final LoggingAdapter log =
      Logging.getLogger(getContext().system(), this);

  public ShardReadTransaction(DOMStoreReadTransaction transaction, ActorRef shardActor, SchemaContext schemaContext) {
    super(shardActor, schemaContext);
    this.transaction = transaction;

  }

  public ShardReadTransaction(DOMStoreTransactionChain transactionChain, DOMStoreReadTransaction transaction, ActorRef shardActor, SchemaContext schemaContext) {
    super(transactionChain, shardActor, schemaContext);
    this.transaction = transaction;
  }

  @Override
  public void handleReceive(Object message) throws Exception {
    if (ReadData.SERIALIZABLE_CLASS.equals(message.getClass())) {
      readData(transaction,ReadData.fromSerializable(message));
    } else {
      super.handleReceive(message);
    }
  }
  protected void closeTransaction(CloseTransaction message) {
    transaction.close();
    getSender().tell(new CloseTransactionReply().toSerializable(), getSelf());
    getSelf().tell(PoisonPill.getInstance(), getSelf());
  }

  //default scope test method to check if we get correct exception
  void forUnitTestOnlyExplicitTransactionClose(){
      transaction.close();
  }

}
