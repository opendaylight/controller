/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.commands;

import com.google.common.base.MoreObjects;
import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

public abstract class AbstractReadTransactionRequestTest<T extends AbstractReadPathTransactionRequest>
        extends AbstractTransactionRequestTest {
    protected static final YangInstanceIdentifier PATH = YangInstanceIdentifier.EMPTY;
    protected static final boolean SNAPSHOT_ONLY = true;

    @Override
    protected abstract T object();

    @Test
    public void getPathTest() {
        Assert.assertEquals(PATH, object().getPath());
    }

    @Test
    public void isSnapshotOnlyTest() {
        Assert.assertEquals(SNAPSHOT_ONLY, object().isSnapshotOnly());
    }

    @Test
    public void addToStringAttributesTest() {
        final MoreObjects.ToStringHelper result = object().addToStringAttributes(MoreObjects.toStringHelper(object()));
        Assert.assertTrue(result.toString().contains("path=" + PATH));
    }
}
