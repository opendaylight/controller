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
import org.opendaylight.controller.cluster.access.ABIVersion;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;

public class ReadTransactionSuccessTest extends AbstractTransactionSuccessTest<ReadTransactionSuccess> {
    private static final NormalizedNode<?, ?> NODE = Builders.containerBuilder().withNodeIdentifier(
            YangInstanceIdentifier.NodeIdentifier.create(QName.create("namespace", "localName"))).build();

    private static final ReadTransactionSuccess OBJECT = new ReadTransactionSuccess(
            TRANSACTION_IDENTIFIER, 0, Optional.of(NODE));

    @Test
    public void getData() throws Exception {
        final Optional<NormalizedNode<?, ?>> result = OBJECT.getData();
        Assert.assertTrue(result.isPresent());
        Assert.assertEquals(NODE.getValue(), result.get().getValue());
    }

    @Test
    public void externalizableProxy() throws Exception {
        final AbstractTransactionSuccessProxy<ReadTransactionSuccess> proxy = OBJECT.externalizableProxy(
                ABIVersion.BORON);
        Assert.assertNotNull(proxy);
    }

    @Test
    public void cloneAsVersion() throws Exception {
        final ReadTransactionSuccess clone = OBJECT.cloneAsVersion(ABIVersion.BORON);
        Assert.assertEquals(OBJECT, clone);
    }
}