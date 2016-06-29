/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import javax.annotation.concurrent.NotThreadSafe;
import org.opendaylight.controller.cluster.access.commands.TransactionRequest;
import org.opendaylight.controller.cluster.access.commands.TransactionSuccess;
import org.opendaylight.controller.cluster.access.concepts.RequestException;

/**
 * Frontend transaction state as observed by the shard leader.
 *
 * @author Robert Varga
 */
@NotThreadSafe
final class FrontendTransaction {
    final long expectedSequence = 0;

    long getExpectedSequence() {
        return expectedSequence;
    }

    TransactionSuccess<?> handleRequest(final TransactionRequest<?> request) throws RequestException {
        // TODO Auto-generated method stub
        return null;
    }
}
