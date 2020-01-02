/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.entityownership.messages;

import static java.util.Objects.requireNonNull;

import java.util.Collection;
import org.opendaylight.controller.cluster.entityownership.selectionstrategy.EntityOwnerSelectionStrategy;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

/**
 * Message sent when a new owner needs to be selected.
 */
public class SelectOwner {
    private final YangInstanceIdentifier entityPath;
    private final Collection<String> allCandidates;
    private final EntityOwnerSelectionStrategy ownerSelectionStrategy;

    public SelectOwner(final YangInstanceIdentifier entityPath, final Collection<String> allCandidates,
                       final EntityOwnerSelectionStrategy ownerSelectionStrategy) {
        this.entityPath = requireNonNull(entityPath, "entityPath should not be null");
        this.allCandidates = requireNonNull(allCandidates, "allCandidates should not be null");
        this.ownerSelectionStrategy = requireNonNull(ownerSelectionStrategy,
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
        return "SelectOwner [entityPath=" + entityPath + ", allCandidates=" + allCandidates
                + ", ownerSelectionStrategy=" + ownerSelectionStrategy + "]";
    }
}
