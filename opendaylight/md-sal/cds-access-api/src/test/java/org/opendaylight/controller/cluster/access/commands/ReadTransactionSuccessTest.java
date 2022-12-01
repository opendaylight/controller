/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.commands;

import static org.junit.Assert.assertEquals;

import java.util.Optional;
import org.junit.Test;
import org.opendaylight.controller.cluster.access.ABIVersion;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;

public class ReadTransactionSuccessTest extends AbstractTransactionSuccessTest<ReadTransactionSuccess> {
    private static final ContainerNode NODE = Builders.containerBuilder()
        .withNodeIdentifier(NodeIdentifier.create(QName.create("namespace", "localName")))
        .build();

    private static final ReadTransactionSuccess OBJECT = new ReadTransactionSuccess(TRANSACTION_IDENTIFIER, 0,
        Optional.of(NODE));

    public ReadTransactionSuccessTest() {
        super(OBJECT, 129, 515);
    }

    @Test
    public void getDataTest() {
        assertEquals(Optional.of(NODE), OBJECT.getData());
    }

    @Test
    public void cloneAsVersionTest() {
        final var clone = OBJECT.cloneAsVersion(ABIVersion.MAGNESIUM);
        assertEquals(OBJECT.getSequence(), clone.getSequence());
        assertEquals(OBJECT.getTarget(), clone.getTarget());
        assertEquals(OBJECT.getData(), clone.getData());
    }

    @Override
    protected void doAdditionalAssertions(final ReadTransactionSuccess deserialize) {
        assertEquals(OBJECT.getData(), deserialize.getData());
    }
}
