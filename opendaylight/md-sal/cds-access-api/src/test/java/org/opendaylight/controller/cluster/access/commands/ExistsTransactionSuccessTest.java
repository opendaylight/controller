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
import org.opendaylight.controller.cluster.access.ABIVersion;

public class ExistsTransactionSuccessTest extends AbstractTransactionSuccessTest<ExistsTransactionSuccess> {
    private static final boolean EXISTS = true;

    private static final ExistsTransactionSuccess OBJECT = new ExistsTransactionSuccess(
            TRANSACTION_IDENTIFIER, 0, EXISTS);

    @Override
    protected ExistsTransactionSuccess object() {
        return OBJECT;
    }

    @Test
    public void getExistsTest() throws Exception {
        final boolean result = OBJECT.getExists();
        Assert.assertEquals(EXISTS, result);
    }

    @Test
    public void cloneAsVersionTest() throws Exception {
        final ExistsTransactionSuccess clone = OBJECT.cloneAsVersion(ABIVersion.BORON);
        Assert.assertEquals(OBJECT, clone);
    }

    @Test
    public void addToStringAttributesTest() throws Exception {
        final MoreObjects.ToStringHelper result = OBJECT.addToStringAttributes(MoreObjects.toStringHelper(OBJECT));
        Assert.assertTrue(result.toString().contains("exists=" + EXISTS));
    }

    @Override
    protected void doAdditionalAssertions(final Object deserialize) {
        Assert.assertTrue(deserialize instanceof ExistsTransactionSuccess);
        Assert.assertEquals(OBJECT.getExists(), ((ExistsTransactionSuccess) deserialize).getExists());
    }
}