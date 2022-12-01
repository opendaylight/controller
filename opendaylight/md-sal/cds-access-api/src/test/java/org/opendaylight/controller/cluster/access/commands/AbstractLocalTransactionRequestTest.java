/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.commands;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;

import org.apache.commons.lang.SerializationUtils;
import org.junit.Test;
import org.opendaylight.controller.cluster.access.ABIVersion;

public abstract class AbstractLocalTransactionRequestTest<T extends AbstractLocalTransactionRequest<T>>
        extends AbstractTransactionRequestTest<T> {
    protected AbstractLocalTransactionRequestTest(final T object) {
        super(object, -1);
    }

    @Test
    public void cloneAsVersionTest() {
        assertSame(object(), object().cloneAsVersion(ABIVersion.TEST_FUTURE_VERSION));
    }

    @Override
    @Test
    public void serializationTest() {
        final var ex = assertThrows(UnsupportedOperationException.class, () -> SerializationUtils.clone(object()));
        assertThat(ex.getMessage(), allOf(
            startsWith("Local transaction request "),
            endsWith(" should never be serialized")));
    }
}
