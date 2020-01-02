/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.entityownership.selectionstrategy;

import java.util.Map;

public abstract class AbstractEntityOwnerSelectionStrategy implements EntityOwnerSelectionStrategy {

    private final long selectionDelayInMillis;
    private final Map<String, Long> initialStatistics;

    protected AbstractEntityOwnerSelectionStrategy(final long selectionDelayInMillis,
            final Map<String, Long> initialStatistics) {
        this.selectionDelayInMillis = selectionDelayInMillis;
        this.initialStatistics = initialStatistics;
    }

    @Override
    public long getSelectionDelayInMillis() {
        return selectionDelayInMillis;
    }

    public Map<String, Long> getInitialStatistics() {
        return initialStatistics;
    }
}
