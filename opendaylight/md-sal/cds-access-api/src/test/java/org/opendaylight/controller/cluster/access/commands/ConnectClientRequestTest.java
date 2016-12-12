/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.commands;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableRangeSet;
import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.controller.cluster.access.ABIVersion;
import org.opendaylight.controller.cluster.access.concepts.AbstractRequestTest;
import org.opendaylight.controller.cluster.access.concepts.ClientIdentifier;
import org.opendaylight.controller.cluster.access.concepts.FrontendIdentifier;
import org.opendaylight.controller.cluster.access.concepts.FrontendType;
import org.opendaylight.controller.cluster.access.concepts.MemberName;
import org.opendaylight.controller.cluster.access.concepts.RequestException;

public class ConnectClientRequestTest extends AbstractRequestTest<ConnectClientRequest> {
    private static final FrontendIdentifier FRONTEND_IDENTIFIER = FrontendIdentifier.create(
            MemberName.forName("member"), FrontendType.forName("frontend"));
    private static final ClientIdentifier CLIENT_IDENTIFIER = ClientIdentifier.create(FRONTEND_IDENTIFIER, 0);

    private static final ABIVersion MIN_VERSION = ABIVersion.TEST_PAST_VERSION;
    private static final ABIVersion MAX_VERSION = ABIVersion.TEST_FUTURE_VERSION;

    private static final ConnectClientRequest OBJECT = new ConnectClientRequest(
            CLIENT_IDENTIFIER, 0, ACTOR_REF, MIN_VERSION, MAX_VERSION);

    @Override
    protected ConnectClientRequest object() {
        return OBJECT;
    }

    @Test
    public void getMinVersionTest() throws Exception {
        Assert.assertEquals(MIN_VERSION, OBJECT.getMinVersion());
    }

    @Test
    public void getMaxVersionTest() throws Exception {
        Assert.assertEquals(MAX_VERSION, OBJECT.getMaxVersion());
    }

    @Test
    public void toRequestFailureTest() throws Exception {
        final RequestException exception = new DeadTransactionException(ImmutableRangeSet.of());
        final ConnectClientFailure failure = OBJECT.toRequestFailure(exception);
        Assert.assertNotNull(failure);
    }

    @Test
    public void cloneAsVersionTest() throws Exception {
        final ConnectClientRequest clone = OBJECT.cloneAsVersion(ABIVersion.BORON);
        Assert.assertNotNull(clone);
        Assert.assertEquals(ABIVersion.BORON, clone.getVersion());
    }

    @Test
    public void addToStringAttributesTest() throws Exception {
        final MoreObjects.ToStringHelper result = OBJECT.addToStringAttributes(MoreObjects.toStringHelper(OBJECT));
        Assert.assertTrue(result.toString().contains("minVersion=" + MIN_VERSION));
        Assert.assertTrue(result.toString().contains("maxVersion=" + MAX_VERSION));
    }

    @Override
    protected void doAdditionalAssertions(final Object deserialize) {
        Assert.assertTrue(deserialize instanceof ConnectClientRequest);
        final ConnectClientRequest casted = (ConnectClientRequest) deserialize;

        Assert.assertEquals(OBJECT.getMaxVersion(), casted.getMaxVersion());
        Assert.assertEquals(OBJECT.getMinVersion(), casted.getMinVersion());
        Assert.assertEquals(OBJECT.getReplyTo(), casted.getReplyTo());
    }
}