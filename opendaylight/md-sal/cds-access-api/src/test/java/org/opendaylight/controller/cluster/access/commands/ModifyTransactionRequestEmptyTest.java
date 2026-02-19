/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.commands;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.common.base.MoreObjects;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.opendaylight.controller.cluster.access.ABIVersion;

class ModifyTransactionRequestEmptyTest extends AbstractTransactionRequestTest<ModifyTransactionRequest> {
    private static final PersistenceProtocol PROTOCOL = PersistenceProtocol.ABORT;
    private static final ModifyTransactionRequest OBJECT = new ModifyTransactionRequest(TRANSACTION_IDENTIFIER, 0,
        ACTOR_REF, List.of(), PROTOCOL);

    ModifyTransactionRequestEmptyTest() {
        super(OBJECT, 108);
    }

    @Test
    void getPersistenceProtocolTest() {
        assertEquals(Optional.of(PROTOCOL), OBJECT.getPersistenceProtocol());
    }

    @Test
    void getModificationsTest() {
        assertEquals(List.of(), OBJECT.getModifications());
    }

    @Test
    void addToStringAttributesTest() {
        final var result = OBJECT.addToStringAttributes(MoreObjects.toStringHelper(OBJECT)).toString();
        assertThat(result).contains("modifications=0");
        assertThat(result).contains("protocol=" + PROTOCOL);
    }

    @Test
    void cloneAsVersionTest() {
        final var clone = OBJECT.cloneAsVersion(ABIVersion.TEST_FUTURE_VERSION);
        assertEquals(OBJECT.getSequence(), clone.getSequence());
        assertEquals(OBJECT.getTarget(), clone.getTarget());
        assertEquals(OBJECT.getReplyTo(), clone.getReplyTo());
        assertEquals(OBJECT.getPersistenceProtocol(), clone.getPersistenceProtocol());
    }

    @Override
    protected void doAdditionalAssertions(final ModifyTransactionRequest deserialize) {
        assertEquals(OBJECT.getReplyTo(), deserialize.getReplyTo());
        assertEquals(OBJECT.getModifications(), deserialize.getModifications());
        assertEquals(OBJECT.getPersistenceProtocol(), deserialize.getPersistenceProtocol());
    }
}
