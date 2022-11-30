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
import org.opendaylight.controller.cluster.access.ABIVersion;

public class ExistsTransactionSuccessTest extends AbstractTransactionSuccessTest<ExistsTransactionSuccess> {
    private static final boolean EXISTS = true;

    private static final ExistsTransactionSuccess OBJECT = new ExistsTransactionSuccess(TRANSACTION_IDENTIFIER, 0,
        EXISTS);

    public ExistsTransactionSuccessTest() {
        super(OBJECT, 99, 487);
    }

    @Test
    public void getExistsTest() {
        assertEquals(EXISTS, OBJECT.getExists());
    }

    @Test
    public void cloneAsVersionTest() {
        final var clone = OBJECT.cloneAsVersion(ABIVersion.BORON);
        assertEquals(OBJECT.getSequence(), clone.getSequence());
        assertEquals(OBJECT.getTarget(), clone.getTarget());
        assertEquals(OBJECT.getExists(), clone.getExists());
    }

    @Test
    public void addToStringAttributesTest() {
        final var result = OBJECT.addToStringAttributes(MoreObjects.toStringHelper(OBJECT)).toString();
        assertThat(result, containsString("exists=" + EXISTS));
    }

    @Override
    protected void doAdditionalAssertions(final ExistsTransactionSuccess deserialize) {
        assertEquals(OBJECT.getExists(), deserialize.getExists());
    }
}