/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.concepts;

import static org.junit.Assert.assertEquals;

import org.opendaylight.controller.cluster.access.commands.TransactionAbortSuccess;

public class SuccessEnvelopeTest extends AbstractEnvelopeTest<SuccessEnvelope> {
    @Override
    protected EnvelopeDetails<SuccessEnvelope> createEnvelope() {
        return new EnvelopeDetails<>(new SuccessEnvelope(new TransactionAbortSuccess(OBJECT, 2L), 1L, 2L, 11L), 769);
    }

    @Override
    protected void doAdditionalAssertions(final SuccessEnvelope envelope, final SuccessEnvelope resolvedObject) {
        assertEquals(envelope.getExecutionTimeNanos(), resolvedObject.getExecutionTimeNanos());
    }
}