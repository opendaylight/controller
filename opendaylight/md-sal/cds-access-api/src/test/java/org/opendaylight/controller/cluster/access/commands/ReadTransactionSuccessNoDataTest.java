/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.opendaylight.controller.cluster.access.ABIVersion;

class ReadTransactionSuccessNoDataTest extends AbstractTransactionSuccessTest<ReadTransactionSuccess> {
    private static final ReadTransactionSuccess OBJECT = new ReadTransactionSuccess(TRANSACTION_IDENTIFIER, 0,
        Optional.empty());

    ReadTransactionSuccessNoDataTest() {
        super(OBJECT, 99);
    }

    @Test
    void getDataTest() {
        assertEquals(Optional.empty(), OBJECT.getData());
    }

    @Test
    void cloneAsVersionTest() {
        final var clone = OBJECT.cloneAsVersion(ABIVersion.TEST_FUTURE_VERSION);
        assertEquals(OBJECT.getSequence(), clone.getSequence());
        assertEquals(OBJECT.getTarget(), clone.getTarget());
        assertEquals(OBJECT.getData(), clone.getData());
    }

    @Override
    void doAdditionalAssertions(final ReadTransactionSuccess deserialize) {
        assertEquals(OBJECT.getData(), deserialize.getData());
    }
}
