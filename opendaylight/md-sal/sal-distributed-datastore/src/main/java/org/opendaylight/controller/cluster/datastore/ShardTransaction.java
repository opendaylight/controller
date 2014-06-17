/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore;

import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.japi.Creator;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadWriteTransaction;

/**
 * The ShardTransaction Actor represents a remote transaction
 *
 * The ShardTransaction Actor delegates all actions to DOMDataReadWriteTransaction
 *
 * Even though the DOMStore and the DOMStoreTransactionChain implement multiple types of transactions
 * the ShardTransaction Actor only works with read-write transactions. This is just to keep the logic simple. At this
 * time there are no known advantages for creating a read-only or write-only transaction which may change over time
 * at which point we can optimize things in the distributed store as well.
 */
public class ShardTransaction extends UntypedActor {

  private final DOMStoreReadWriteTransaction transaction;

  public ShardTransaction(DOMStoreReadWriteTransaction transaction) {
    this.transaction = transaction;
  }


  public static Props props(final DOMStoreReadWriteTransaction transaction){
    return Props.create(new Creator<ShardTransaction>(){

      @Override
      public ShardTransaction create() throws Exception {
        return new ShardTransaction(transaction);
      }
    });
  }

  @Override
  public void onReceive(Object message) throws Exception {
    throw new UnsupportedOperationException("onReceive");
  }
}
