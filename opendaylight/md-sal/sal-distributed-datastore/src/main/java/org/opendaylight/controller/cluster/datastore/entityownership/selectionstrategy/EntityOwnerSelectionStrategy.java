/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.entityownership.selectionstrategy;

import java.util.Collection;

/**
 * An EntityOwnerSelectionStrategy is to be used by the EntityOwnershipShard to select a new owner from a collection
 * of candidates
 */
public interface EntityOwnerSelectionStrategy {
    /**
     *
     * @return true if owner selection should be delayed. If false owner selection should not be delayed.
     */
    boolean delaySelection();

    /**
     *
     * @return the time in millis owner selection should be delayed
     */
    long selectionDelayInMillis();


    /**
     *
     * @param viableCandidates a candidate that may be chosen as owner
     * @return the new owner
     */
    String newOwner(Collection<String> viableCandidates);
}
