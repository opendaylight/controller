/*
 * Copyright (c) 2017 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.binding.test;

/**
 * AbstractConcurrentDataBrokerTest.
 *
 * <p>Uses single thread executor for the Serialized DOM DataBroker (instead of the
 * direct executor used by the {@literal @}Deprecated AbstractDataBrokerTest) in order
 * to allow tests to use the DataBroker concurrently from several threads.
 *
 * <p>See also <a href="https://bugs.opendaylight.org/show_bug.cgi?id=7538">bug 7538</a> for more details.
 *
 * @author Michael Vorburger
 */
public abstract class AbstractConcurrentDataBrokerTest extends AbstractBaseDataBrokerTest {

    @Override
    protected AbstractDataBrokerTestCustomizer createDataBrokerTestCustomizer() {
        return new ConcurrentDataBrokerTestCustomizer();
    }

}
