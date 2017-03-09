/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.commands;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.controller.cluster.access.TestUtils;
import org.opendaylight.controller.cluster.access.concepts.ClientIdentifier;
import org.opendaylight.controller.cluster.access.concepts.FrontendIdentifier;
import org.opendaylight.controller.cluster.access.concepts.FrontendType;
import org.opendaylight.controller.cluster.access.concepts.LocalHistoryIdentifier;
import org.opendaylight.controller.cluster.access.concepts.MemberName;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;

public abstract class AbstractTransactionSuccessProxyTest<T extends AbstractTransactionSuccessProxy> {
    private static final FrontendIdentifier FRONTEND_IDENTIFIER = FrontendIdentifier.create(
            MemberName.forName("member"), FrontendType.forName("frontend"));
    private static final ClientIdentifier CLIENT_IDENTIFIER = ClientIdentifier.create(
            FRONTEND_IDENTIFIER, 0);
    private static final LocalHistoryIdentifier HISTORY_IDENTIFIER = new LocalHistoryIdentifier(
            CLIENT_IDENTIFIER, 0);

    protected static final TransactionIdentifier TRANSACTION_IDENTIFIER = new TransactionIdentifier(
            HISTORY_IDENTIFIER, 0);

    abstract T object();

    @Test
    public void externalizableTest() throws Exception {
        final T copy = TestUtils.copy(object());
        Assert.assertNotNull(copy);
    }

    @Test
    public void readTargetTest() throws Exception {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (final ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            TRANSACTION_IDENTIFIER.writeTo(oos);
        }

        try (final ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bos.toByteArray()))) {
            final TransactionIdentifier result = object().readTarget(ois);
            Assert.assertNotNull(result);
        }
    }
}
