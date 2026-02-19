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
import org.opendaylight.controller.cluster.access.ABIVersion;

class ExistsTransactionSuccessTest extends AbstractTransactionSuccessTest<ExistsTransactionSuccess> {
    private static final boolean EXISTS = true;

    private static final ExistsTransactionSuccess OBJECT = new ExistsTransactionSuccess(TRANSACTION_IDENTIFIER, 0,
        EXISTS);

    ExistsTransactionSuccessTest() {
        super(OBJECT, 99);
    }

    @Test
    void getExistsTest() {
        assertEquals(EXISTS, OBJECT.getExists());
    }

    @Test
    void cloneAsVersionTest() {
        final var clone = OBJECT.cloneAsVersion(ABIVersion.TEST_FUTURE_VERSION);
        assertEquals(OBJECT.getSequence(), clone.getSequence());
        assertEquals(OBJECT.getTarget(), clone.getTarget());
        assertEquals(OBJECT.getExists(), clone.getExists());
    }

    @Test
    void addToStringAttributesTest() {
        final var result = OBJECT.addToStringAttributes(MoreObjects.toStringHelper(OBJECT)).toString();
        assertThat(result).contains("exists=" + EXISTS);
    }

    @Override
    void doAdditionalAssertions(final ExistsTransactionSuccess deserialize) {
        assertEquals(OBJECT.getExists(), deserialize.getExists());
    }
}
