/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.entityownership.selectionstrategy;

import java.util.Collection;
import org.eclipse.jdt.annotation.Nullable;

/**
 * An EntityOwnerSelectionStrategy is to be used by the EntityOwnershipShard to select a new owner from a collection
 * of candidates.
 */
public interface EntityOwnerSelectionStrategy {
    /**
     * Returns the time in millis owner selection should be delayed.
     *
     * @return the time in millis owner selection should be delayed
     */
    long getSelectionDelayInMillis();

    /**
     * Selects a new owner from the list of viable candidates.
     *
     * @param currentOwner the current owner of the entity if any, null otherwise
     * @param viableCandidates the available candidates from which to choose the new owner
     * @return the new owner
     */
    String newOwner(@Nullable String currentOwner, Collection<String> viableCandidates);
}
