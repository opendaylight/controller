/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.opendaylight.controller.cluster.access.ABIVersion;

class ExistsTransactionRequestTest extends AbstractReadTransactionRequestTest<ExistsTransactionRequest> {
    private static final ExistsTransactionRequest OBJECT = new ExistsTransactionRequest(TRANSACTION_IDENTIFIER, 0,
        ACTOR_REF, PATH, SNAPSHOT_ONLY);

    ExistsTransactionRequestTest() {
        super(OBJECT, 108);
    }

    @Test
    void cloneAsVersionTest() {
        final var cloneVersion = ABIVersion.TEST_FUTURE_VERSION;
        final var clone = OBJECT.cloneAsVersion(cloneVersion);
        assertEquals(cloneVersion, clone.getVersion());
        assertEquals(OBJECT.getPath(), clone.getPath());
        assertEquals(OBJECT.isSnapshotOnly(), clone.isSnapshotOnly());
    }

    @Override
    protected void doAdditionalAssertions(final ExistsTransactionRequest deserialize) {
        assertEquals(OBJECT.getReplyTo(), deserialize.getReplyTo());
        assertEquals(OBJECT.getPath(), deserialize.getPath());
    }
}
