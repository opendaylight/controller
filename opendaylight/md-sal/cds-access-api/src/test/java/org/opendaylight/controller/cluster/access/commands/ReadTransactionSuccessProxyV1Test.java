/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.commands;

import com.google.common.base.Optional;
import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;

public class ReadTransactionSuccessProxyV1Test
        extends AbstractTransactionSuccessProxyTest<ReadTransactionSuccessProxyV1> {
    private static final NormalizedNode<?, ?> NODE = Builders.containerBuilder().withNodeIdentifier(
            YangInstanceIdentifier.NodeIdentifier.create(QName.create("namespace", "localName"))).build();
    private static final ReadTransactionSuccess REQUEST = new ReadTransactionSuccess(
            TRANSACTION_IDENTIFIER, 0, Optional.of(NODE));
    private static final ReadTransactionSuccessProxyV1 OBJECT = new ReadTransactionSuccessProxyV1(REQUEST);

    @Override
    ReadTransactionSuccessProxyV1 object() {
        return OBJECT;
    }

    @Test
    public void createSuccess() throws Exception {
        final ReadTransactionSuccess result = OBJECT.createSuccess(TRANSACTION_IDENTIFIER, 0);
        Assert.assertNotNull(result);
    }
}