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
import org.junit.Test;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

public abstract class AbstractReadTransactionRequestTest<T extends AbstractReadPathTransactionRequest<T>>
        extends AbstractTransactionRequestTest<T> {
    protected static final YangInstanceIdentifier PATH = YangInstanceIdentifier.empty();
    protected static final boolean SNAPSHOT_ONLY = true;

    protected AbstractReadTransactionRequestTest(final T object, final int baseSize, final int legacySize) {
        super(object, baseSize, legacySize);
    }

    @Test
    public void getPathTest() {
        assertEquals(PATH, object().getPath());
    }

    @Test
    public void isSnapshotOnlyTest() {
        assertEquals(SNAPSHOT_ONLY, object().isSnapshotOnly());
    }

    @Test
    public void addToStringAttributesTest() {
        final var result = object().addToStringAttributes(MoreObjects.toStringHelper(object())).toString();
        assertThat(result, containsString("path=" + PATH));
    }
}
