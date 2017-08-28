/*
 * Copyright (c) 2017 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.trace.closetracker.impl.tests;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;

import java.util.List;
import org.junit.Test;
import org.opendaylight.controller.md.sal.trace.closetracker.impl.AbstractCloseTracked;
import org.opendaylight.controller.md.sal.trace.closetracker.impl.CloseTracked;
import org.opendaylight.controller.md.sal.trace.closetracker.impl.CloseTrackedRegistry;

public class CloseTrackedRegistryTest {

    private static class SomethingClosable extends AbstractCloseTracked<SomethingClosable> implements AutoCloseable {
        SomethingClosable(CloseTrackedRegistry<SomethingClosable> transactionChainRegistry) {
            super(transactionChainRegistry);
        }

        @Override
        public void close() {
            removeFromTrackedRegistry();
        }
    }

    private final CloseTrackedRegistry<SomethingClosable> registry =
            new CloseTrackedRegistry<>(this, "testDuplicateAllocationContexts", true);

    @Test
    public void testDuplicateAllocationContexts() {
        for (int i = 0; i < 100; i++) {
            @SuppressWarnings({ "resource", "unused" })
            SomethingClosable forgotToClose = new SomethingClosable(registry);
            someOtherMethod();
        }
        List<CloseTracked<SomethingClosable>> uniqueNonClosed = registry.getAllUnique();
        assertThat(uniqueNonClosed).hasSize(1);
        // TODO ... assertThat(uniqueNonClosed).containsExactly(varargs).

        fail("Not yet implemented");
    }

    private void someOtherMethod() {
        new SomethingClosable(registry).close();
    }
}
