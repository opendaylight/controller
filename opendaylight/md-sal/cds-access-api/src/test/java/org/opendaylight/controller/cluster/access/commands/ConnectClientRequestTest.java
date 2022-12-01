/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.commands;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableRangeSet;
import org.junit.Test;
import org.opendaylight.controller.cluster.access.ABIVersion;
import org.opendaylight.controller.cluster.access.concepts.AbstractRequestTest;
import org.opendaylight.controller.cluster.access.concepts.ClientIdentifier;
import org.opendaylight.controller.cluster.access.concepts.FrontendIdentifier;
import org.opendaylight.controller.cluster.access.concepts.FrontendType;
import org.opendaylight.controller.cluster.access.concepts.MemberName;

public class ConnectClientRequestTest extends AbstractRequestTest<ConnectClientRequest> {
    private static final FrontendIdentifier FRONTEND_IDENTIFIER = FrontendIdentifier.create(
            MemberName.forName("member"), FrontendType.forName("frontend"));
    private static final ClientIdentifier CLIENT_IDENTIFIER = ClientIdentifier.create(FRONTEND_IDENTIFIER, 0);

    private static final ABIVersion MIN_VERSION = ABIVersion.TEST_PAST_VERSION;
    private static final ABIVersion MAX_VERSION = ABIVersion.TEST_FUTURE_VERSION;

    private static final ConnectClientRequest OBJECT = new ConnectClientRequest(
            CLIENT_IDENTIFIER, 0, ACTOR_REF, MIN_VERSION, MAX_VERSION);

    public ConnectClientRequestTest() {
        super(OBJECT, 112, 310);
    }

    @Test
    public void getMinVersionTest() {
        assertEquals(MIN_VERSION, OBJECT.getMinVersion());
    }

    @Test
    public void getMaxVersionTest() {
        assertEquals(MAX_VERSION, OBJECT.getMaxVersion());
    }

    @Test
    public void toRequestFailureTest() {
        final var exception = new DeadTransactionException(ImmutableRangeSet.of());
        final var failure = OBJECT.toRequestFailure(exception);
        assertNotNull(failure);
    }

    @Test
    public void cloneAsVersionTest() {
        final var clone = OBJECT.cloneAsVersion(ABIVersion.BORON);
        assertNotNull(clone);
        assertEquals(ABIVersion.BORON, clone.getVersion());
    }

    @Test
    public void addToStringAttributesTest() {
        final var result = OBJECT.addToStringAttributes(MoreObjects.toStringHelper(OBJECT)).toString();
        assertThat(result, containsString("minVersion=" + MIN_VERSION));
        assertThat(result, containsString("maxVersion=" + MAX_VERSION));
    }

    @Override
    protected void doAdditionalAssertions(final ConnectClientRequest deserialize) {
        assertEquals(OBJECT.getMaxVersion(), deserialize.getMaxVersion());
        assertEquals(OBJECT.getMinVersion(), deserialize.getMinVersion());
        assertEquals(OBJECT.getReplyTo(), deserialize.getReplyTo());
    }
}