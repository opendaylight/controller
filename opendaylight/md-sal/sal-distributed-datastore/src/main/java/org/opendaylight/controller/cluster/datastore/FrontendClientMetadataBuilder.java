/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import com.google.common.base.Preconditions;
import com.google.common.collect.Collections2;
import com.google.common.collect.Range;
import com.google.common.collect.RangeSet;
import com.google.common.collect.TreeRangeSet;
import com.google.common.primitives.UnsignedLong;
import java.util.HashMap;
import java.util.Map;
import org.opendaylight.controller.cluster.access.concepts.ClientIdentifier;
import org.opendaylight.controller.cluster.access.concepts.LocalHistoryIdentifier;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.controller.cluster.datastore.persisted.FrontendClientMetadata;
import org.opendaylight.controller.cluster.datastore.persisted.FrontendHistoryMetadata;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.concepts.Identifiable;

final class FrontendClientMetadataBuilder implements Builder<FrontendClientMetadata>, Identifiable<ClientIdentifier> {
    private final Map<LocalHistoryIdentifier, FrontendHistoryMetadataBuilder> currentHistories = new HashMap<>();
    private final RangeSet<UnsignedLong> purgedHistories;
    private final ClientIdentifier identifier;

    FrontendClientMetadataBuilder(final ClientIdentifier identifier) {
        this.identifier = Preconditions.checkNotNull(identifier);
        purgedHistories = TreeRangeSet.create();
    }

    FrontendClientMetadataBuilder(final FrontendClientMetadata meta) {
        this.identifier = Preconditions.checkNotNull(meta.getIdentifier());
        purgedHistories = TreeRangeSet.create(meta.getPurgedHistories());

        for (FrontendHistoryMetadata h : meta.getCurrentHistories()) {
            final FrontendHistoryMetadataBuilder b = new FrontendHistoryMetadataBuilder(identifier, h);
            currentHistories.put(b.getIdentifier(), b);
        }
    }

    @Override
    public FrontendClientMetadata build() {
        return new FrontendClientMetadata(identifier, purgedHistories,
            Collections2.transform(currentHistories.values(), FrontendHistoryMetadataBuilder::build));
    }

    @Override
    public ClientIdentifier getIdentifier() {
        return identifier;
    }

    void onHistoryClosed(final LocalHistoryIdentifier historyId) {
        ensureHistory(historyId).onHistoryClosed();
    }

    void onHistoryPurged(final LocalHistoryIdentifier historyId) {
        currentHistories.remove(historyId);
        // XXX: do we need to account for cookies?
        purgedHistories.add(Range.singleton(UnsignedLong.fromLongBits(historyId.getHistoryId())));
    }

    void onTransactionCommitted(final TransactionIdentifier txId) {
        ensureHistory(txId.getHistoryId()).onTransactionCommitted(txId);
    }

    private FrontendHistoryMetadataBuilder ensureHistory(final LocalHistoryIdentifier historyId) {
        final FrontendHistoryMetadataBuilder existing = currentHistories.get(historyId);
        if (existing != null) {
            return existing;
        }

        final FrontendHistoryMetadataBuilder ret = new FrontendHistoryMetadataBuilder(historyId);
        currentHistories.put(historyId, ret);
        return ret;
    }
}
