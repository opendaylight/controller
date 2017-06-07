/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.commands;

import com.google.common.base.MoreObjects;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.controller.cluster.access.ABIVersion;

public class ModifyTransactionRequestEmptyTest extends AbstractTransactionRequestTest<ModifyTransactionRequest> {
    private static final PersistenceProtocol PROTOCOL = PersistenceProtocol.ABORT;

    private static final ModifyTransactionRequest OBJECT = new ModifyTransactionRequest(
            TRANSACTION_IDENTIFIER, 0, ACTOR_REF, new ArrayList<>(), PROTOCOL);

    @Override
    protected ModifyTransactionRequest object() {
        return OBJECT;
    }

    @Test
    public void getPersistenceProtocolTest() throws Exception {
        final Optional<PersistenceProtocol> result = OBJECT.getPersistenceProtocol();
        Assert.assertTrue(result.isPresent());
        Assert.assertEquals(PROTOCOL, result.get());
    }

    @Test
    public void getModificationsTest() throws Exception {
        final List<TransactionModification> result = OBJECT.getModifications();
        Assert.assertNotNull(result);
        Assert.assertTrue(result.isEmpty());
    }

    @Test
    public void addToStringAttributesTest() {
        final MoreObjects.ToStringHelper result = OBJECT.addToStringAttributes(MoreObjects.toStringHelper(OBJECT));
        Assert.assertTrue(result.toString().contains("modifications=0"));
        Assert.assertTrue(result.toString().contains("protocol=" + PROTOCOL));
    }

    @Test
    public void cloneAsVersionTest() throws Exception {
        final ModifyTransactionRequest clone = OBJECT.cloneAsVersion(ABIVersion.BORON);
        Assert.assertEquals(OBJECT, clone);
    }

    @Override
    protected void doAdditionalAssertions(final Object deserialize) {
        Assert.assertTrue(deserialize instanceof ModifyTransactionRequest);
        final ModifyTransactionRequest casted = (ModifyTransactionRequest) deserialize;

        Assert.assertEquals(OBJECT.getReplyTo(), casted.getReplyTo());
        Assert.assertEquals(OBJECT.getModifications(), casted.getModifications());
        Assert.assertEquals(OBJECT.getPersistenceProtocol(), casted.getPersistenceProtocol());
    }
}