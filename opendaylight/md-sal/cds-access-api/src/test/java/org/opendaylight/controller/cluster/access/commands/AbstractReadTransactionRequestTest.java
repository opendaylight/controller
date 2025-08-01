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

import com.google.common.base.MoreObjects;
import org.junit.jupiter.api.Test;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

abstract class AbstractReadTransactionRequestTest<T extends AbstractReadPathTransactionRequest<T>>
        extends AbstractTransactionRequestTest<T> {
    static final YangInstanceIdentifier PATH = YangInstanceIdentifier.of();
    static final boolean SNAPSHOT_ONLY = true;

    AbstractReadTransactionRequestTest(final T object, final int baseSize) {
        super(object, baseSize);
    }

    @Test
    void getPathTest() {
        assertEquals(PATH, object().getPath());
    }

    @Test
    void isSnapshotOnlyTest() {
        assertEquals(SNAPSHOT_ONLY, object().isSnapshotOnly());
    }

    @Test
    void addToStringAttributesTest() {
        final var result = object().addToStringAttributes(MoreObjects.toStringHelper(object())).toString();
        assertThat(result).contains("path=" + PATH);
    }
}
