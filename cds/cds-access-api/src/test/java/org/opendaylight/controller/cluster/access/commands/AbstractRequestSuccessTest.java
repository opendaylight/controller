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

import org.apache.commons.lang3.SerializationUtils;
import org.eclipse.jdt.annotation.NonNull;
import org.junit.jupiter.api.Test;
import org.opendaylight.controller.cluster.access.concepts.ClientIdentifier;
import org.opendaylight.controller.cluster.access.concepts.FrontendIdentifier;
import org.opendaylight.controller.cluster.access.concepts.FrontendType;
import org.opendaylight.controller.cluster.access.concepts.LocalHistoryIdentifier;
import org.opendaylight.controller.cluster.access.concepts.MemberName;
import org.opendaylight.controller.cluster.access.concepts.RequestSuccess;

abstract class AbstractRequestSuccessTest<T extends RequestSuccess<?, T>> {
    private static final FrontendIdentifier FRONTEND_IDENTIFIER = FrontendIdentifier.create(
            MemberName.forName("test"), FrontendType.forName("one"));
    static final ClientIdentifier CLIENT_IDENTIFIER = ClientIdentifier.create(FRONTEND_IDENTIFIER, 0);
    static final LocalHistoryIdentifier HISTORY_IDENTIFIER = new LocalHistoryIdentifier(CLIENT_IDENTIFIER, 0);

    private final @NonNull T object;
    private final int expectedSize;

    AbstractRequestSuccessTest(final T object, final int expectedSize) {
        this.object = requireNonNull(object);
        this.expectedSize = expectedSize;
    }

    @Test
    void serializationTest() {
        final var bytes = SerializationUtils.serialize(object);
        assertEquals(expectedSize, bytes.length);

        @SuppressWarnings("unchecked")
        final var deserialize = (T) SerializationUtils.deserialize(bytes);

        assertEquals(object.getTarget(), deserialize.getTarget());
        assertEquals(object.getVersion(), deserialize.getVersion());
        assertEquals(object.getSequence(), deserialize.getSequence());
        doAdditionalAssertions(deserialize);
    }

    void doAdditionalAssertions(final T deserialize) {
        // No-op by default
    }
}
