/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.md.sal.dom.broker.impl.legacy.sharded.adapter;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import javax.annotation.Nonnull;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChainListener;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBrokerExtension;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadOnlyTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadWriteTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMTransactionChain;
import org.opendaylight.mdsal.dom.api.DOMSchemaService;
import org.opendaylight.mdsal.dom.broker.ShardedDOMDataBrokerAdapter;

/**
 * DOMDataBroker implementation that forwards calls to
 * {@link org.opendaylight.mdsal.dom.broker.ShardedDOMDataBrokerAdapter},
 * which in turn translates calls to shard aware implementation of
 * {@link org.opendaylight.mdsal.dom.api.DOMDataTreeService}
 *
 * <p>
 * The incompatibility between first and latter APIs, puts restriction on {@link DOMDataReadWriteTransaction}
 * and {@link DOMDataReadOnlyTransaction} provided by this data broker. See
 * {@link ShardedDOMDataBrokerDelegatingReadWriteTransaction}
 * and {@link ShardedDOMDataBrokerDelegatingReadTransaction} respectively.
 */
// FIXME try to refactor some of the implementation to abstract class for better reusability
public class LegacyShardedDOMDataBrokerAdapter implements DOMDataBroker {

    private final org.opendaylight.mdsal.dom.api.DOMDataBroker delegateDataBroker;
    private final DOMSchemaService schemaService;
    private final AtomicLong txNum = new AtomicLong();
    private final AtomicLong chainNum = new AtomicLong();

    public LegacyShardedDOMDataBrokerAdapter(final ShardedDOMDataBrokerAdapter delegateDataBroker,
                                             final DOMSchemaService schemaService) {
        this.delegateDataBroker = checkNotNull(delegateDataBroker);
        this.schemaService = checkNotNull(schemaService);
    }

    @Override
    public DOMDataReadOnlyTransaction newReadOnlyTransaction() {
        return new ShardedDOMDataBrokerDelegatingReadTransaction(newTransactionIdentifier(),
                                                                 delegateDataBroker.newReadOnlyTransaction());
    }

    @Override
    public DOMDataReadWriteTransaction newReadWriteTransaction() {
        return new ShardedDOMDataBrokerDelegatingReadWriteTransaction(newTransactionIdentifier(),
                                                                      schemaService.getGlobalContext(),
                                                                      newReadOnlyTransaction(),
                                                                      newWriteOnlyTransaction());
    }

    @Override
    public DOMDataWriteTransaction newWriteOnlyTransaction() {
        return new ShardedDOMDataBrokerDelegatingWriteTransaction(newTransactionIdentifier(),
                                                                  delegateDataBroker.newWriteOnlyTransaction());
    }

    @Override
    public DOMTransactionChain createTransactionChain(final TransactionChainListener listener) {
        return new ShardedDOMDataBrokerDelegatingTransactionChain(chainNum.getAndIncrement(),
                                                                  schemaService.getGlobalContext(), delegateDataBroker,
                                                                  listener);
    }

    @Nonnull
    @Override
    public Map<Class<? extends DOMDataBrokerExtension>, DOMDataBrokerExtension> getSupportedExtensions() {
        return Collections.emptyMap();
    }

    private Object newTransactionIdentifier() {
        return "DOM-" + txNum.getAndIncrement();
    }
}
