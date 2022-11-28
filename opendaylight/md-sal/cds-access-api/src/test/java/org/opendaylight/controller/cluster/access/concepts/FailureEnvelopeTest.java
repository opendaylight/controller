/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.concepts;

import static org.junit.Assert.assertEquals;

import java.io.DataInput;
import java.io.IOException;
import org.opendaylight.controller.cluster.access.ABIVersion;
import org.opendaylight.yangtools.concepts.WritableIdentifier;

public class FailureEnvelopeTest extends AbstractEnvelopeTest<FailureEnvelope> {

    @Override
    protected FailureEnvelope createEnvelope() {
        final RequestFailure<?, ?> message =
                new MockFailure(OBJECT, new RuntimeRequestException("msg", new RuntimeException()), 42);
        return new FailureEnvelope(message, 1L, 2L, 11L);
    }

    @Override
    protected void doAdditionalAssertions(final FailureEnvelope envelope, final FailureEnvelope resolvedObject) {
        assertEquals(envelope.getExecutionTimeNanos(), resolvedObject.getExecutionTimeNanos());
        final RequestException expectedCause = envelope.getMessage().getCause();
        final RequestException actualCause = resolvedObject.getMessage().getCause();
        assertEquals(expectedCause.getMessage(), actualCause.getMessage());
        assertEquals(expectedCause.isRetriable(), actualCause.isRetriable());
    }

    private static class MockRequestFailureProxy extends AbstractRequestFailureProxy<WritableIdentifier, MockFailure> {
        private static final long serialVersionUID = 5015515628523887221L;

        @SuppressWarnings("checkstyle:RedundantModifier")
        public MockRequestFailureProxy() {
            //For Externalizable
        }

        private MockRequestFailureProxy(final MockFailure mockFailure) {
            super(mockFailure);
        }

        @Override
        protected MockFailure createFailure(final WritableIdentifier target, final long sequence,
                                            final RequestException failureCause) {
            return new MockFailure(target, failureCause, sequence);
        }

        @Override
        protected WritableIdentifier readTarget(final DataInput in) throws IOException {
            return TransactionIdentifier.readFrom(in);
        }
    }

    private static class MockFailure extends RequestFailure<WritableIdentifier, MockFailure> {
        private static final long serialVersionUID = 1L;

        MockFailure(final WritableIdentifier target, final RequestException cause, final long sequence) {
            super(target, sequence, cause);
        }

        @Override
        protected AbstractRequestFailureProxy<WritableIdentifier, MockFailure> externalizableProxy(
                final ABIVersion version) {
            return new MockRequestFailureProxy(this);
        }

        @Override
        protected MockFailure cloneAsVersion(final ABIVersion version) {
            return this;
        }
    }
}
