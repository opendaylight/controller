/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.commands;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.opendaylight.controller.cluster.access.commands.TransactionModification.TYPE_WRITE;

import com.google.common.base.MoreObjects;
import java.util.List;
import java.util.Optional;
import org.junit.Test;
import org.opendaylight.controller.cluster.access.ABIVersion;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;

public class ModifyTransactionRequestTest extends AbstractTransactionRequestTest<ModifyTransactionRequest> {
    private static final ContainerNode NODE = Builders.containerBuilder().withNodeIdentifier(
            NodeIdentifier.create(QName.create("namespace", "localName"))).build();

    private static final List<TransactionModification> MODIFICATIONS = List.of(
            new TransactionWrite(YangInstanceIdentifier.empty(), NODE));

    private static final PersistenceProtocol PROTOCOL = PersistenceProtocol.ABORT;

    private static final ModifyTransactionRequest OBJECT = new ModifyTransactionRequest(
            TRANSACTION_IDENTIFIER, 0, ACTOR_REF, MODIFICATIONS, PROTOCOL);

    @Override
    protected ModifyTransactionRequest object() {
        return OBJECT;
    }

    @Test
    public void getPersistenceProtocolTest() {
        final Optional<PersistenceProtocol> result = OBJECT.getPersistenceProtocol();
        assertTrue(result.isPresent());
        assertEquals(PROTOCOL, result.get());
    }

    @Test
    public void getModificationsTest() {
        final List<TransactionModification> result = OBJECT.getModifications();
        assertNotNull(result);
        assertEquals(MODIFICATIONS, result);
    }

    @Test
    public void addToStringAttributesTest() {
        final var result = OBJECT.addToStringAttributes(MoreObjects.toStringHelper(OBJECT)).toString();
        assertThat(result, containsString("modifications=1"));
        assertThat(result, containsString("protocol=" + PROTOCOL));
    }

    @Test
    public void cloneAsVersionTest() {
        assertSame(OBJECT, OBJECT.cloneAsVersion(ABIVersion.MAGNESIUM));
    }

    @Override
    protected void doAdditionalAssertions(final Object deserialize) {
        assertThat(deserialize, instanceOf(ModifyTransactionRequest.class));
        final var casted = (ModifyTransactionRequest) deserialize;

        assertEquals(OBJECT.getReplyTo(), casted.getReplyTo());
        assertEquals(OBJECT.getPersistenceProtocol(), casted.getPersistenceProtocol());

        assertNotNull(casted.getModifications());
        assertEquals(1, casted.getModifications().size());
        final TransactionModification modification = casted.getModifications().get(0);
        assertEquals(YangInstanceIdentifier.empty(), modification.getPath());
        assertEquals(TYPE_WRITE, modification.getType());
    }
}
