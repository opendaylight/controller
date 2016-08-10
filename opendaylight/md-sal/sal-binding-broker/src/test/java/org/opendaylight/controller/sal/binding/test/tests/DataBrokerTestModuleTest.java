/*
 * Copyright (c) 2016 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.binding.test.tests;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;
import org.opendaylight.controller.sal.binding.test.DataBrokerTestModule;

public class DataBrokerTestModuleTest {

    @Test
    public void ensureDataBrokerTestModuleWorksWithoutException() {
        assertThat(DataBrokerTestModule.dataBroker()).isNotNull();
    }
}
