/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.yang.logback.appender;

import nu.xom.Nodes;
import org.junit.After;
import org.junit.Before;
import org.opendaylight.controller.config.api.ModuleIdentifier;
import org.opendaylight.controller.config.manager.impl.AbstractConfigTest;
import org.opendaylight.controller.config.manager.impl.factoriesresolver.HardcodedModuleFactoriesResolver;
import org.opendaylight.controller.config.spi.ModuleFactory;
import org.opendaylight.controller.config.yang.logback.ResettingLogbackTestBase;
import org.opendaylight.controller.config.yang.logback.api.HasAppendersImpl;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;

import static org.junit.Assert.assertEquals;

public abstract class AbstractAppenderModuleTest extends AbstractConfigTest {
    protected final ResettingLogbackTestBase resettingLogbackTestBase = new ResettingLogbackTestBase();

    public static void assertNodeIs(String text, Nodes nodes) {
        assertEquals(1, nodes.size());
        assertEquals(text, nodes.get(0).getValue());

    }

    @Before
    @After
    public void resetLogback() {
        resettingLogbackTestBase.resetLogback();
    }

    @Before
    public void setUp() {
        super.initConfigTransactionManagerImpl(new HardcodedModuleFactoriesResolver(getTestedFactories().toArray(new ModuleFactory[]{})));
    }

    protected abstract Collection<? extends ModuleFactory> getTestedFactories();

    protected <TO> HasAppendersImpl<TO> getInstanceFromCurrentConfig() throws Exception {
        assertEquals(1, getTestedFactories().size());
        ModuleFactory testedFactory = getTestedFactories().iterator().next();
        Field nameField = testedFactory.getClass().getField("NAME");
        String factoryName = (String) nameField.get(null);
        Field instanceField = testedFactory.getClass().getField("INSTANCE_NAME");
        String instanceName = (String) instanceField.get(null);
        return (HasAppendersImpl<TO>) getInstanceFromCurrentConfig(new ModuleIdentifier(factoryName, instanceName));
    }

    // simple filter method that returns first object whose getName()==name
    protected <TO> TO findByName(Collection<TO> tos, String name) throws Exception {
        for (TO to : tos) {
            Method nameMethod = to.getClass().getMethod("getName");
            String value = (String) nameMethod.invoke(to);
            if (value.equals(name)) {
                return to;
            }
        }
        throw new IllegalArgumentException(name + " not found");
    }

}
