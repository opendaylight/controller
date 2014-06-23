/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore;

import akka.actor.ActorRef;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.persistence.UntypedProcessor;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import org.opendaylight.controller.cluster.datastore.messages.CreateTransactionChain;
import org.opendaylight.controller.cluster.datastore.messages.CreateTransactionChainReply;
import org.opendaylight.controller.cluster.datastore.messages.RegisterChangeListener;
import org.opendaylight.controller.cluster.datastore.messages.RegisterChangeListenerReply;
import org.opendaylight.controller.cluster.datastore.messages.UpdateSchemaContext;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeListener;
import org.opendaylight.controller.md.sal.dom.store.impl.InMemoryDOMDataStore;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreTransactionChain;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

import java.util.concurrent.Executors;

/**
 * A Shard represents a portion of the logical data tree
 * <p/>
 * Our Shard uses InMemoryDataStore as it's internal representation and delegates all requests it
 */
public class Shard extends UntypedProcessor {

  ListeningExecutorService storeExecutor = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(2));

  private final InMemoryDOMDataStore store = new InMemoryDOMDataStore("OPER", storeExecutor);

  LoggingAdapter log = Logging.getLogger(getContext().system(), this);

  @Override
  public void onReceive(Object message) throws Exception {
    if (message instanceof CreateTransactionChain) {
      createTransactionChain();
    } else if(message instanceof RegisterChangeListener){
      registerChangeListener((RegisterChangeListener) message);
    } else if(message instanceof UpdateSchemaContext){
      store.onGlobalContextUpdated(((UpdateSchemaContext) message).getSchemaContext());
    }
  }

  private void registerChangeListener(RegisterChangeListener registerChangeListener) {
    org.opendaylight.yangtools.concepts.ListenerRegistration<AsyncDataChangeListener<InstanceIdentifier, NormalizedNode<?, ?>>> registration =
            store.registerChangeListener(registerChangeListener.getPath(), registerChangeListener.getListener(), registerChangeListener.getScope());
    // TODO: Construct a ListenerRegistration actor with the actual registration returned when registering a listener with the datastore
    ActorRef listenerRegistration = getContext().actorOf(ListenerRegistration.props(null));
    getSender().tell(new RegisterChangeListenerReply(listenerRegistration.path()), getSelf());
  }

  private void createTransactionChain() {
    DOMStoreTransactionChain chain = store.createTransactionChain();
    ActorRef transactionChain = getContext().actorOf(TransactionChain.props(chain));
    getSender().tell(new CreateTransactionChainReply(transactionChain.path()), getSelf());
  }
}
