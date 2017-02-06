/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.binding.test;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

/**
 * AbstractDataBrokerTest.
 *
 * @deprecated Please now use the AbstractConcurrentDataBrokerTest instead of
 *             this. Normally in a well written test this should be a drop-in
 *             replacement. Instead of {@literal @}Override
 *             setupWithDataBroker(DataBroker dataBroker), please just use
 *             JUnit's {@literal @}Before before() { ... getDataBroker() }. Some
 *             tests which relied on the Test DataBroker being synchronous,
 *             contrary to its specification as well as the production
 *             implementation, may require changes to e.g. use get() on
 *             submit()'ed transaction to make the test wait before asserts. See
 *             also
 *             <a href="https://bugs.opendaylight.org/show_bug.cgi?id=7538">bug
 *             7538</a> for more details.
 */
@Deprecated
public class AbstractDataBrokerTest extends AbstractBaseDataBrokerTest {

    @Override
    protected AbstractDataBrokerTestCustomizer createDataBrokerTestCustomizer() {
        return new DataBrokerTestCustomizer();
    }

    @Override
    protected void setupWithSchema(SchemaContext context) {
        super.setupWithSchema(context);
        setupWithDataBroker(getDataBroker());
    }

    protected void setupWithDataBroker(final DataBroker dataBroker) {
        // Intentionally left No-op, subclasses may customize it
    }

}
