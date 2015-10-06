/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.entityownership.messages;

import com.google.common.base.Preconditions;
import java.util.Collection;
import org.opendaylight.controller.cluster.datastore.entityownership.selectionstrategy.EntityOwnerSelectionStrategy;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

/**
 * Message sent when a new owner needs to be selected
 */
public class SelectOwner {
    private final YangInstanceIdentifier entityPath;
    private final Collection<String> allCandidates;
    private final EntityOwnerSelectionStrategy ownerSelectionStrategy;

    public SelectOwner(YangInstanceIdentifier entityPath, Collection<String> allCandidates,
                       EntityOwnerSelectionStrategy ownerSelectionStrategy) {

        this.entityPath = Preconditions.checkNotNull(entityPath, "entityPath should not be null");
        this.allCandidates = Preconditions.checkNotNull(allCandidates, "allCandidates should not be null");
        this.ownerSelectionStrategy = Preconditions.checkNotNull(ownerSelectionStrategy,
                "ownerSelectionStrategy should not be null");
    }

    public YangInstanceIdentifier getEntityPath() {
        return entityPath;
    }

    public Collection<String> getAllCandidates() {
        return allCandidates;
    }

    public EntityOwnerSelectionStrategy getOwnerSelectionStrategy() {
        return ownerSelectionStrategy;
    }

    @Override
    public String toString() {
        return "SelectOwner{" +
                "entityPath=" + entityPath +
                ", allCandidates=" + allCandidates +
                ", ownerSelectionStrategy=" + ownerSelectionStrategy +
                '}';
    }
}
