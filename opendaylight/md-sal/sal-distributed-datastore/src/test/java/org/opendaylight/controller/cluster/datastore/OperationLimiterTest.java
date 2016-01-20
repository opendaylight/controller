/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import static org.junit.Assert.assertEquals;
import org.junit.Test;
import org.opendaylight.controller.cluster.datastore.identifiers.TransactionIdentifier;
import org.opendaylight.controller.cluster.datastore.messages.BatchedModificationsReply;
import org.opendaylight.controller.cluster.datastore.messages.DataExistsReply;

/**
 * Unit tests for OperationCompleter.
 *
 * @author Thomas Pantelis
 */
public class OperationLimiterTest {

    @Test
    public void testOnComplete() throws Exception {
        int permits = 10;
        OperationLimiter limiter = new OperationLimiter(new TransactionIdentifier("foo", 1), permits, 1);
        limiter.acquire(permits);
        int availablePermits = 0;

        limiter.onComplete(null, new DataExistsReply(true, DataStoreVersions.CURRENT_VERSION));
        assertEquals("availablePermits", ++availablePermits, limiter.availablePermits());

        limiter.onComplete(null, new DataExistsReply(true, DataStoreVersions.CURRENT_VERSION));
        assertEquals("availablePermits", ++availablePermits, limiter.availablePermits());

        limiter.onComplete(null, new IllegalArgumentException());
        assertEquals("availablePermits", ++availablePermits, limiter.availablePermits());

        limiter.onComplete(null, new BatchedModificationsReply(4));
        availablePermits += 4;
        assertEquals("availablePermits", availablePermits, limiter.availablePermits());
    }

}
