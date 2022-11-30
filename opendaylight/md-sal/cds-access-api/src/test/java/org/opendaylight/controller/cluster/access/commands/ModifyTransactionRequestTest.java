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
import static org.junit.Assert.assertNotNull;
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

    private static final ModifyTransactionRequest OBJECT = new ModifyTransactionRequest(TRANSACTION_IDENTIFIER, 0,
        ACTOR_REF, MODIFICATIONS, PROTOCOL);

    public ModifyTransactionRequestTest() {
        super(OBJECT, 140, 440);
    }

    @Test
    public void getPersistenceProtocolTest() {
        assertEquals(Optional.of(PROTOCOL), OBJECT.getPersistenceProtocol());
    }

    @Test
    public void getModificationsTest() {
        assertEquals(MODIFICATIONS, OBJECT.getModifications());
    }

    @Test
    public void addToStringAttributesTest() {
        final var result = OBJECT.addToStringAttributes(MoreObjects.toStringHelper(OBJECT)).toString();
        assertThat(result, containsString("modifications=1"));
        assertThat(result, containsString("protocol=" + PROTOCOL));
    }

    @Test
    public void cloneAsVersionTest() {
        assertEquals(OBJECT, OBJECT.cloneAsVersion(ABIVersion.BORON));
    }

    @Override
    protected void doAdditionalAssertions(final ModifyTransactionRequest deserialize) {
        assertEquals(OBJECT.getReplyTo(), deserialize.getReplyTo());
        assertEquals(OBJECT.getPersistenceProtocol(), deserialize.getPersistenceProtocol());

        assertNotNull(deserialize.getModifications());
        assertEquals(1, deserialize.getModifications().size());
        final var modification = deserialize.getModifications().get(0);
        assertEquals(YangInstanceIdentifier.empty(), modification.getPath());
        assertEquals(TYPE_WRITE, modification.getType());
    }
}
