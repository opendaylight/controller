/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import org.opendaylight.controller.cluster.access.concepts.ClientIdentifier;
import org.opendaylight.controller.cluster.access.concepts.LocalHistoryIdentifier;
import org.opendaylight.controller.cluster.access.concepts.RequestException;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;

/**
 * Abstract class for providing logical tracking of frontend local histories. This class is specialized for
 * standalong transactions and chained transactions.
 *
 * @author Robert Varga
 */
final class StandaloneFrontendHistory extends AbstractFrontendHistory {
    StandaloneFrontendHistory(final String persistenceId, final ClientIdentifier clientId) {
        super(persistenceId, new LocalHistoryIdentifier(clientId, 0));
    }

    @Override
    protected FrontendTransaction createTransaction(final TransactionIdentifier id) throws RequestException {
        return new FrontendTransaction(id);
    }
}
