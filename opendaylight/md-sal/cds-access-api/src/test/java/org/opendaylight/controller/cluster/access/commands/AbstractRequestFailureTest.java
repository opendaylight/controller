/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.commands;

import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.commons.lang.SerializationUtils;
import org.junit.Test;
import org.opendaylight.controller.cluster.access.ABIVersion;
import org.opendaylight.controller.cluster.access.concepts.ClientIdentifier;
import org.opendaylight.controller.cluster.access.concepts.FrontendIdentifier;
import org.opendaylight.controller.cluster.access.concepts.FrontendType;
import org.opendaylight.controller.cluster.access.concepts.LocalHistoryIdentifier;
import org.opendaylight.controller.cluster.access.concepts.MemberName;
import org.opendaylight.controller.cluster.access.concepts.RequestException;
import org.opendaylight.controller.cluster.access.concepts.RequestFailure;
import org.opendaylight.controller.cluster.access.concepts.RuntimeRequestException;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;

public abstract class AbstractRequestFailureTest<T extends RequestFailure<?, T>> {
    private static final FrontendIdentifier FRONTEND_IDENTIFIER = FrontendIdentifier.create(
            MemberName.forName("member"), FrontendType.forName("frontend"));

    protected static final ClientIdentifier CLIENT_IDENTIFIER = ClientIdentifier.create(FRONTEND_IDENTIFIER, 0);
    protected static final LocalHistoryIdentifier HISTORY_IDENTIFIER = new LocalHistoryIdentifier(CLIENT_IDENTIFIER, 0);
    protected static final TransactionIdentifier TRANSACTION_IDENTIFIER = new TransactionIdentifier(
            HISTORY_IDENTIFIER, 0);
    protected static final RequestException CAUSE = new RuntimeRequestException("fail", new Throwable());
    private static final int CAUSE_SIZE = SerializationUtils.serialize(CAUSE).length;

    private final T object;
    private final int expectedSize;
    private final int legacySize;

    protected AbstractRequestFailureTest(final T object, final int baseSize, final int legacySize) {
        this.object = requireNonNull(object);
        this.expectedSize = baseSize + CAUSE_SIZE;
        this.legacySize = legacySize + CAUSE_SIZE;
    }

    @Test
    public void getCauseTest() {
        assertEquals(CAUSE, object.getCause());
    }

    @Test
    public void isHardFailureTest() {
        assertTrue(object.isHardFailure());
    }

    @Test
    public void serializationTest() {
        final var bytes = SerializationUtils.serialize(object);
        assertEquals(legacySize, bytes.length);
        assertEquals(expectedSize, SerializationUtils.serialize(object.toVersion(ABIVersion.CHLORINE_SR2)).length);

        @SuppressWarnings("unchecked")
        final var deserialize = (T) SerializationUtils.deserialize(bytes);

        assertEquals(object.getTarget(), deserialize.getTarget());
        assertEquals(object.getVersion(), deserialize.getVersion());
        assertEquals(object.getSequence(), deserialize.getSequence());
    }
}
