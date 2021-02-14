/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.commands;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Optional;
import org.junit.Test;
import org.opendaylight.controller.cluster.access.ABIVersion;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

public class ReadTransactionSuccessNoDataTest extends AbstractTransactionSuccessTest<ReadTransactionSuccess> {
    private static final ReadTransactionSuccess OBJECT = new ReadTransactionSuccess(
            TRANSACTION_IDENTIFIER, 0, Optional.empty());

    @Override
    protected ReadTransactionSuccess object() {
        return OBJECT;
    }

    @Test
    public void getDataTest() {
        final Optional<NormalizedNode> result = OBJECT.getData();
        assertFalse(result.isPresent());
    }

    @Test
    public void cloneAsVersionTest() {
        final ReadTransactionSuccess clone = OBJECT.cloneAsVersion(ABIVersion.BORON);
        assertEquals(OBJECT, clone);
    }

    @Override
    protected void doAdditionalAssertions(final Object deserialize) {
        assertTrue(deserialize instanceof ReadTransactionSuccess);
        assertEquals(OBJECT.getData(), ((ReadTransactionSuccess) deserialize).getData());
    }
}
