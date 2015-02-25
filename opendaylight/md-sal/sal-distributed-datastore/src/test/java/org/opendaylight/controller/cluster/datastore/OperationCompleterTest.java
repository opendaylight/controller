/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import static org.junit.Assert.assertEquals;
import java.util.concurrent.Semaphore;
import org.junit.Test;
import org.opendaylight.controller.cluster.datastore.messages.BatchedModificationsReply;
import org.opendaylight.controller.cluster.datastore.messages.DataExistsReply;

/**
 * Unit tests for OperationCompleter.
 *
 * @author Thomas Pantelis
 */
public class OperationCompleterTest {

    @Test
    public void testOnComplete() throws Exception {
        int permits = 10;
        Semaphore operationLimiter = new Semaphore(permits);
        operationLimiter.acquire(permits);
        int availablePermits = 0;

        OperationCompleter completer = new OperationCompleter(operationLimiter );

        completer.onComplete(null, new DataExistsReply(true));
        assertEquals("availablePermits", ++availablePermits, operationLimiter.availablePermits());

        completer.onComplete(null, new DataExistsReply(true));
        assertEquals("availablePermits", ++availablePermits, operationLimiter.availablePermits());

        completer.onComplete(null, new IllegalArgumentException());
        assertEquals("availablePermits", ++availablePermits, operationLimiter.availablePermits());

        completer.onComplete(null, new BatchedModificationsReply(4));
        availablePermits += 4;
        assertEquals("availablePermits", availablePermits, operationLimiter.availablePermits());
    }
}
