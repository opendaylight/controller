/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker.actors.dds;

import com.google.common.base.Preconditions;
import java.util.HashMap;
import java.util.Map;
import org.opendaylight.controller.cluster.access.concepts.LocalHistoryIdentifier;
import org.opendaylight.yangtools.concepts.Identifiable;

/**
 * Abstract base class for client view of a history. This class has two implementations, one for normal local histories
 * and the other for single transactions.
 *
 * @author Robert Varga
 */
abstract class AbstractClientHistory implements Identifiable<LocalHistoryIdentifier> {
    private final Map<Long, LocalHistoryIdentifier> histories = new HashMap<>();
    private final DistributedDataStoreClientBehavior client;
    private final LocalHistoryIdentifier identifier;

    AbstractClientHistory(final DistributedDataStoreClientBehavior client, final LocalHistoryIdentifier identifier) {
        this.client = Preconditions.checkNotNull(client);
        this.identifier = Preconditions.checkNotNull(identifier);
        Preconditions.checkArgument(identifier.getCookie() == 0);
    }

    final LocalHistoryIdentifier getHistoryForCookie(final Long cookie) {
        LocalHistoryIdentifier ret = histories.get(cookie);
        if (ret == null) {
            ret = new LocalHistoryIdentifier(identifier.getClientId(), identifier.getHistoryId(), cookie);
            histories.put(cookie, ret);
        }

        return ret;
    }

    @Override
    public final LocalHistoryIdentifier getIdentifier() {
        return identifier;
    }

    final DistributedDataStoreClientBehavior getClient() {
        return client;
    }

    /**
     * Callback invoked from {@link ClientTransaction} when a transaction has been completed.
     *
     * @param transaction Transaction handle
     */
    void transactionComplete(final ClientTransaction transaction) {
        client.transactionComplete(transaction);
    }
}
