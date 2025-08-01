/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.commands;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.apache.commons.lang3.SerializationUtils;
import org.junit.jupiter.api.Test;
import org.opendaylight.controller.cluster.access.ABIVersion;

abstract class AbstractLocalTransactionRequestTest<T extends AbstractLocalTransactionRequest<T>>
        extends AbstractTransactionRequestTest<T> {
    AbstractLocalTransactionRequestTest(final T object) {
        super(object, -1);
    }

    @Test
    void cloneAsVersionTest() {
        assertSame(object(), object().cloneAsVersion(ABIVersion.TEST_FUTURE_VERSION));
    }

    @Test
    @Override
    protected void serializationTest() {
        final var ex = assertThrows(UnsupportedOperationException.class, () -> SerializationUtils.clone(object()));
        assertThat(ex.getMessage()).startsWith("Local transaction request ").endsWith(" should never be serialized");
    }
}
