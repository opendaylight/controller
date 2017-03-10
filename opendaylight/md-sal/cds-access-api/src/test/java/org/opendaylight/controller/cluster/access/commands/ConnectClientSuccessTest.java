/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.access.commands;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import akka.testkit.TestProbe;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import java.util.Collection;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.controller.cluster.access.ABIVersion;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTree;
import org.opendaylight.yangtools.yang.data.api.schema.tree.TreeType;
import org.opendaylight.yangtools.yang.data.impl.schema.tree.InMemoryDataTreeFactory;

public class ConnectClientSuccessTest extends AbstractRequestSuccessTest<ConnectClientSuccess> {

    private static final DataTree TREE = InMemoryDataTreeFactory.getInstance().create(TreeType.OPERATIONAL);
    private static final ActorSystem SYSTEM = ActorSystem.create("test");
    private static final ActorRef ACTOR_REF = TestProbe.apply(SYSTEM).ref();
    private static final List<ActorSelection> ALTERNATES = ImmutableList.of(ActorSelection.apply(ACTOR_REF, "foo"));
    private static final ConnectClientSuccess OBJECT = new ConnectClientSuccess(
            CLIENT_IDENTIFIER, 0L, ACTOR_REF, ALTERNATES, TREE, 10);

    @Test
    public void testGetAlternates() {
        final Collection<ActorSelection> alternates = OBJECT.getAlternates();
        alternates.containsAll(ALTERNATES);
    }

    @Test
    public void testGetBackend() {
        final ActorRef actorRef = OBJECT.getBackend();
        actorRef.compareTo(ACTOR_REF);
    }

    @Test
    public void testGetDataTree() {
        final DataTree tree = OBJECT.getDataTree().get();
        Assert.assertEquals(TREE, tree);
    }

    @Test
    public void testGetMaxMessages() {
        final int maxMessages = OBJECT.getMaxMessages();
        Assert.assertEquals(10, maxMessages);
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
        Assert.assertTrue(result.toString().contains("alternates"));
        Assert.assertTrue(result.toString().contains("dataTree"));
        Assert.assertTrue(result.toString().contains("maxMessages"));
    }

}
