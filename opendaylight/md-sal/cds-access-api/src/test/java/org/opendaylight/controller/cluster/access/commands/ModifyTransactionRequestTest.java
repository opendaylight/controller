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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.opendaylight.controller.cluster.access.commands.TransactionModification.TYPE_WRITE;

import com.google.common.base.MoreObjects;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.opendaylight.controller.cluster.access.ABIVersion;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.spi.node.ImmutableNodes;

class ModifyTransactionRequestTest extends AbstractTransactionRequestTest<ModifyTransactionRequest> {
    private static final ContainerNode NODE = ImmutableNodes.newContainerBuilder()
        .withNodeIdentifier(new NodeIdentifier(QName.create("namespace", "localName")))
        .build();

    private static final List<TransactionModification> MODIFICATIONS = List.of(
            new TransactionWrite(YangInstanceIdentifier.of(), NODE));

    private static final PersistenceProtocol PROTOCOL = PersistenceProtocol.ABORT;

    private static final ModifyTransactionRequest OBJECT = new ModifyTransactionRequest(TRANSACTION_IDENTIFIER, 0,
        ACTOR_REF, MODIFICATIONS, PROTOCOL);

    ModifyTransactionRequestTest() {
        super(OBJECT, 140);
    }

    @Test
    void getPersistenceProtocolTest() {
        assertEquals(PROTOCOL, OBJECT.persistenceProtocol());
    }

    @Test
    void getModificationsTest() {
        assertEquals(MODIFICATIONS, OBJECT.modifications());
    }

    @Test
    void addToStringAttributesTest() {
        final var result = OBJECT.addToStringAttributes(MoreObjects.toStringHelper(OBJECT)).toString();
        assertThat(result).contains("modifications=1");
        assertThat(result).contains("protocol=" + PROTOCOL);
    }

    @Test
    void cloneAsVersionTest() {
        final var clone = OBJECT.cloneAsVersion(ABIVersion.TEST_FUTURE_VERSION);
        assertEquals(OBJECT.getSequence(), clone.getSequence());
        assertEquals(OBJECT.getTarget(), clone.getTarget());
        assertEquals(OBJECT.getReplyTo(), clone.getReplyTo());
        assertEquals(OBJECT.modifications(), clone.modifications());
        assertEquals(OBJECT.persistenceProtocol(), clone.persistenceProtocol());
    }

    @Override
    protected void doAdditionalAssertions(final ModifyTransactionRequest deserialize) {
        assertEquals(OBJECT.getReplyTo(), deserialize.getReplyTo());
        assertEquals(OBJECT.persistenceProtocol(), deserialize.persistenceProtocol());
        assertNotNull(deserialize.modifications());
        assertEquals(1, deserialize.modifications().size());
        final var modification = deserialize.modifications().getFirst();
        assertEquals(YangInstanceIdentifier.of(), modification.getPath());
        assertEquals(TYPE_WRITE, modification.getType());
    }
}
