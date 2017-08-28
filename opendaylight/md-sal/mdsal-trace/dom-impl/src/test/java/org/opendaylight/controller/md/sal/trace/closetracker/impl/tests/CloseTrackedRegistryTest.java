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

import java.util.Set;
import java.util.function.Predicate;
import org.junit.Test;
import org.opendaylight.controller.md.sal.trace.closetracker.impl.AbstractCloseTracked;
import org.opendaylight.controller.md.sal.trace.closetracker.impl.CloseTrackedRegistry;
import org.opendaylight.controller.md.sal.trace.closetracker.impl.CloseTrackedRegistryReportEntry;

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

    @Test
    public void testDuplicateAllocationContexts() {
        final CloseTrackedRegistry<SomethingClosable> registry =
                new CloseTrackedRegistry<>(this, "testDuplicateAllocationContexts", true);

        for (int i = 0; i < 100; i++) {
            SomethingClosable isClosedManyTimes = new SomethingClosable(registry);
            isClosedManyTimes.close();
            someOtherMethodWhichDoesNotClose(registry);
        }
        @SuppressWarnings({ "resource", "unused" })
        SomethingClosable forgotToCloseOnce = new SomethingClosable(registry);

        Set<CloseTrackedRegistryReportEntry<SomethingClosable>> uniqueNonClosed = registry.getAllUnique();
        assertThat(uniqueNonClosed).hasSize(2);
        assertThatIterableContains(uniqueNonClosed, entry ->
            entry.getNumberAddedNotRemoved() == 100 || entry.getNumberAddedNotRemoved() == 1);
        uniqueNonClosed.forEach(entry -> {
            if (entry.getNumberAddedNotRemoved() == 100) {
                assertThatIterableContains(entry.getStackTraceElements(),
                    element -> element.getMethodName().equals("someOtherMethodWhichDoesNotClose"));
            } else if (entry.getNumberAddedNotRemoved() == 1) {
                assertThatIterableContains(entry.getStackTraceElements(),
                    element -> element.getMethodName().equals("testDuplicateAllocationContexts"));
            } else {
                fail("Unexpected number of added, not removed: " + entry.getNumberAddedNotRemoved());
            }
        });
    }

    // Something like this really should be in Google Truth...
    private <T> void assertThatIterableContains(Iterable<T> iterable, Predicate<T> predicate) {
        for (T element : iterable) {
            if (predicate.test(element)) {
                return;
            }
        }
        fail("Iterable did not contain any element matching predicate");
    }

    @SuppressWarnings({ "resource", "unused" })
    private void someOtherMethodWhichDoesNotClose(CloseTrackedRegistry<SomethingClosable> registry) {
        new SomethingClosable(registry);
    }

    @Test
    @SuppressWarnings({ "unused", "resource" })
    public void testDebugContextDisabled() {
        final CloseTrackedRegistry<SomethingClosable> debugContextDisabledRegistry =
                new CloseTrackedRegistry<>(this, "testDebugContextDisabled", false);

        SomethingClosable forgotToCloseOnce = new SomethingClosable(debugContextDisabledRegistry);

        Set<CloseTrackedRegistryReportEntry<SomethingClosable>>
            closeRegistryReport = debugContextDisabledRegistry.getAllUnique();
        assertThat(closeRegistryReport).hasSize(1);

        CloseTrackedRegistryReportEntry<SomethingClosable>
            closeRegistryReportEntry1 = closeRegistryReport.iterator().next();
        assertThat(closeRegistryReportEntry1.getNumberAddedNotRemoved()).isEqualTo(1);
        assertThat(closeRegistryReportEntry1.getStackTraceElements()).isEmpty();
    }
}
