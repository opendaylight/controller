/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.concepts;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.testkit.TestProbe;
import com.google.common.base.MoreObjects;
import org.apache.commons.lang.SerializationUtils;
import org.junit.Assert;
import org.junit.Test;

public abstract class AbstractRequestTest<T extends Request> {
    private static final ActorSystem SYSTEM = ActorSystem.create("test");
    protected static final ActorRef ACTOR_REF = TestProbe.apply(SYSTEM).ref();

    protected abstract T object();

    @Test
    public void getReplyToTest() {}

    @Test
    public void addToStringAttributesCommonTest() {
        final MoreObjects.ToStringHelper result = object().addToStringAttributes(MoreObjects.toStringHelper(object()));
        Assert.assertTrue(result.toString().contains("replyTo=" + ACTOR_REF));
    }
}
