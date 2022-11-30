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

import com.google.common.base.MoreObjects;
import java.util.List;
import java.util.Optional;
import org.junit.Test;
import org.opendaylight.controller.cluster.access.ABIVersion;

public class ModifyTransactionRequestEmptyTest extends AbstractTransactionRequestTest<ModifyTransactionRequest> {
    private static final PersistenceProtocol PROTOCOL = PersistenceProtocol.ABORT;
    private static final ModifyTransactionRequest OBJECT = new ModifyTransactionRequest(TRANSACTION_IDENTIFIER, 0,
        ACTOR_REF, List.of(), PROTOCOL);

    public ModifyTransactionRequestEmptyTest() {
        super(OBJECT, 108, 408);
    }

    @Test
    public void getPersistenceProtocolTest() {
        assertEquals(Optional.of(PROTOCOL), OBJECT.getPersistenceProtocol());
    }

    @Test
    public void getModificationsTest() {
        assertEquals(List.of(), OBJECT.getModifications());
    }

    @Test
    public void addToStringAttributesTest() {
        final var result = OBJECT.addToStringAttributes(MoreObjects.toStringHelper(OBJECT)).toString();
        assertThat(result, containsString("modifications=0"));
        assertThat(result, containsString("protocol=" + PROTOCOL));
    }

    @Test
    public void cloneAsVersionTest() {
        assertEquals(OBJECT, OBJECT.cloneAsVersion(ABIVersion.BORON));
    }

    @Override
    protected void doAdditionalAssertions(final ModifyTransactionRequest deserialize) {
        assertEquals(OBJECT.getReplyTo(), deserialize.getReplyTo());
        assertEquals(OBJECT.getModifications(), deserialize.getModifications());
        assertEquals(OBJECT.getPersistenceProtocol(), deserialize.getPersistenceProtocol());
    }
}