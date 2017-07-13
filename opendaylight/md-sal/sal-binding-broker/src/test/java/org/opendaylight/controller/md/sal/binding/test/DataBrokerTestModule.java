/*
 * Copyright (c) 2016 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.binding.test;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;

public class DataBrokerTestModule {

    public static DataBroker dataBroker() {
        return new DataBrokerTestModule(false).getDataBroker();
    }

    private final boolean useMTDataTreeChangeListenerExecutor;

    public DataBrokerTestModule(boolean useMTDataTreeChangeListenerExecutor) {
        this.useMTDataTreeChangeListenerExecutor = useMTDataTreeChangeListenerExecutor;
    }

    // Suppress IllegalCatch because of AbstractDataBrokerTest (change later)
    @SuppressWarnings({ "checkstyle:IllegalCatch", "checkstyle:IllegalThrows" })
    public DataBroker getDataBroker() throws RuntimeException {
        try {
            // This is a little bit "upside down" - in the future,
            // we should probably put what is in AbstractDataBrokerTest
            // into this DataBrokerTestModule, and make AbstractDataBrokerTest
            // use it, instead of the way around it currently is (the opposite);
            // this is just for historical reasons... and works for now.
            ConstantSchemaAbstractDataBrokerTest dataBrokerTest
                = new ConstantSchemaAbstractDataBrokerTest(useMTDataTreeChangeListenerExecutor);
            dataBrokerTest.setup();
            return dataBrokerTest.getDataBroker();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
