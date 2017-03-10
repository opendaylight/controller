/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
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
import com.google.common.collect.ImmutableList;
import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.controller.cluster.access.ABIVersion;

public class ConnectClientSuccessTest extends AbstractRequestSuccessTest<ConnectClientSuccess> {

    private static final ActorSystem SYSTEM = ActorSystem.create("test");
    private static final ActorRef ACTOR_REF = TestProbe.apply(SYSTEM).ref();
    private static final ConnectClientSuccess OBJECT = new ConnectClientSuccess(
            CLIENT_IDENTIFIER, 0L, ACTOR_REF, ImmutableList.of(), , 10);

    @Test
    public void testGetAlternates() {
//TODO: Test goes here...
    }

    @Test
    public void testGetBackend() {
//TODO: Test goes here...
    }

    @Test
    public void testGetDataTree() {
//TODO: Test goes here...
    }

    @Test
    public void testGetMaxMessages() {
//TODO: Test goes here...
    }

    @Test
    public void externalizableProxyTest() throws Exception {
        final ConnectClientSuccessProxyV1 proxy = OBJECT.externalizableProxy(ABIVersion.BORON);
        Assert.assertNotNull(proxy);
    }

    @Test
    public void cloneAsVersionTest() throws Exception {
        final ConnectClientSuccess clone = OBJECT.cloneAsVersion(ABIVersion.BORON);
        Assert.assertEquals(OBJECT, clone);
    }

    @Test
    public void addToStringAttributes() throws Exception {
        final MoreObjects.ToStringHelper result = OBJECT.addToStringAttributes(MoreObjects.toStringHelper(OBJECT));
        //  Assert.assertTrue(result.toString().contains("exists=" + EXISTS));
    }

}
