/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.concepts;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.common.base.MoreObjects;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.actor.ExtendedActorSystem;
import org.apache.pekko.serialization.JavaSerializer;
import org.apache.pekko.testkit.TestProbe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public abstract class AbstractRequestTest<T extends Request<?, T>> {
    private static final ActorSystem SYSTEM = ActorSystem.create("test");
    protected static final ActorRef ACTOR_REF = TestProbe.apply(SYSTEM).ref();
    private static final int ACTOR_REF_SIZE = ACTOR_REF.path().toSerializationFormat().length();

    private final T object;
    private final int expectedSize;

    protected AbstractRequestTest(final T object, final int baseSize) {
        this.object = requireNonNull(object);
        expectedSize = baseSize + ACTOR_REF_SIZE;
    }

    protected final T object() {
        return object;
    }

    @BeforeEach
    void beforeEach() {
        JavaSerializer.currentSystem().value_$eq((ExtendedActorSystem) SYSTEM);
    }

    @Test
    void getReplyToTest() {
        assertEquals(ACTOR_REF, object.getReplyTo());
    }

    @Test
    void addToStringAttributesCommonTest() {
        final var result = object.addToStringAttributes(MoreObjects.toStringHelper(object));
        assertThat(result.toString()).contains("replyTo=" + ACTOR_REF);
    }

    @Test
    protected void serializationTest() {
        final byte[] bytes = SerializationUtils.serialize(object);
        assertEquals(expectedSize, bytes.length);
        @SuppressWarnings("unchecked")
        final T deserialize = (T) SerializationUtils.deserialize(bytes);

        assertEquals(object.getTarget(), deserialize.getTarget());
        assertEquals(object.getVersion(), deserialize.getVersion());
        assertEquals(object.getSequence(), deserialize.getSequence());
        doAdditionalAssertions(deserialize);
    }

    protected abstract void doAdditionalAssertions(T deserialize);
}
