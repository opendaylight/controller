/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.commands;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.testkit.TestProbe;
import com.google.common.base.MoreObjects;
import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.controller.cluster.access.concepts.ClientIdentifier;
import org.opendaylight.controller.cluster.access.concepts.FrontendIdentifier;
import org.opendaylight.controller.cluster.access.concepts.FrontendType;
import org.opendaylight.controller.cluster.access.concepts.LocalHistoryIdentifier;
import org.opendaylight.controller.cluster.access.concepts.MemberName;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

public abstract class AbstractReadTransactionRequestTest<T extends AbstractReadTransactionRequest> {
    protected static final YangInstanceIdentifier PATH = YangInstanceIdentifier.EMPTY;
    protected static final boolean SNAPSHOT_ONLY = Boolean.TRUE;

    protected static final FrontendIdentifier FRONTEND_IDENTIFIER = FrontendIdentifier.create(
            MemberName.forName("test"), FrontendType.forName("one"));
    protected static final ClientIdentifier CLIENT_IDENTIFIER = ClientIdentifier.create(FRONTEND_IDENTIFIER, 0);
    protected static final LocalHistoryIdentifier HISTORY_IDENTIFIER = new LocalHistoryIdentifier(
            CLIENT_IDENTIFIER, 0);
    protected static final TransactionIdentifier TRANSACTION_IDENTIFIER = new TransactionIdentifier(
            HISTORY_IDENTIFIER, 0);

    protected static final ActorSystem SYSTEM = ActorSystem.create("test");
    protected static final ActorRef ACTOR_REF = TestProbe.apply(SYSTEM).ref();;

    abstract T object();

    @Test
    public void getPathTest() {
        Assert.assertEquals(PATH, object().getPath());
    }

    @Test
    public void isSnapshotOnlyTest() {
        Assert.assertEquals(SNAPSHOT_ONLY, object().isSnapshotOnly());
    }

    @Test
    public void addToStringAttributesTest() {
        final MoreObjects.ToStringHelper result = object().addToStringAttributes(MoreObjects.toStringHelper(object()));
        Assert.assertTrue(result.toString().contains("path=" + PATH));
    }
}
