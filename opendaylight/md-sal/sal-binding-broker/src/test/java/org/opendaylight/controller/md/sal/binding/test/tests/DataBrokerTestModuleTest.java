/*
 * Copyright (c) 2016 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.binding.test.tests;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Ignore;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.test.DataBrokerTestModule;

/**
 * Integration tests the DataBrokerTestModule.
 *
 * @author Michael Vorburger
 */
public class DataBrokerTestModuleTest {

    @Test
    public void ensureDataBrokerTestModuleWorksWithoutException() {
        assertThat(DataBrokerTestModule.dataBroker()).isNotNull();
    }

    @Test
    @Ignore // This test is flaky on build server VMs (although fine locally)
    public void slowYangLoadingShouldOnlyHappenOnceAndNotDelayEachDataBroker() {
        // TODO Write a lil' Timer utility class to make this kind of timing test code more readable
        long startAtMs = System.currentTimeMillis();
        DataBrokerTestModule.dataBroker();
        long firstDataBrokerAtMs = System.currentTimeMillis();
        long firstDataBrokerDurationMs = firstDataBrokerAtMs - startAtMs;
        DataBrokerTestModule.dataBroker();
        long secondDataBrokerDurationMs = System.currentTimeMillis() - firstDataBrokerAtMs;
        assertThat(Math.abs(secondDataBrokerDurationMs - firstDataBrokerDurationMs))
                .isLessThan(firstDataBrokerDurationMs / 4);
    }
}
