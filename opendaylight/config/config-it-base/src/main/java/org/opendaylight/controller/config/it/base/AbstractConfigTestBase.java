/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.it.base;

import static org.ops4j.pax.exam.CoreOptions.composite;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.editConfigurationFilePut;

import java.lang.management.ManagementFactory;
import java.util.concurrent.TimeUnit;

import javax.management.ObjectName;

import org.junit.Before;
import org.opendaylight.controller.config.api.ConfigRegistry;
import org.opendaylight.controller.config.util.ConfigRegistryJMXClient;
import org.opendaylight.controller.karaf.it.base.AbstractKarafTestBase;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.karaf.options.LogLevelOption.LogLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Stopwatch;

/**
 * Please use AbstractKarafTestBase instead.
 * @see AbstractKarafTestBase
 */
@Deprecated
public abstract class AbstractConfigTestBase extends AbstractKarafTestBase {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractConfigTestBase.class);

    /*
     * Wait up to 10s for our configured module to come up
     */
    private static final int MODULE_TIMEOUT_MILLIS = 60000;

    /**
     * This method need only be overridden if using the config system.
     *
     * @return the config module name
     */
    @Deprecated
    public String getModuleName() {
        return null;
    }

    /**
     * This method need only be overridden if using the config system.
     *
     * @return the config module instance name
     */
    @Deprecated
    public String getInstanceName() {
        return null;
    }

    @Override
    protected Option getLoggingOption() {
        Option option = editConfigurationFilePut(ORG_OPS4J_PAX_LOGGING_CFG,
                        logConfiguration(AbstractConfigTestBase.class),
                        LogLevel.INFO.name());
        option = composite(option, super.getLoggingOption());
        return option;
    }

    /**
     * Override this method to provide more options to config.
     *
     * @return An array of additional config options
     */
    protected Option[] getAdditionalOptions() {
        return null;
    }

    public String logConfiguration(Class<?> klazz) {
        return "log4j.logger." + klazz.getPackage().getName();
    }

    /**
     * Wait for module config to be pushed to the config datastore before running tests.
     * @throws Exception on timeout
     */
    @Before
    public void setup() throws Exception {
        String moduleName = getModuleName();
        String instanceName = getInstanceName();
        if (moduleName == null || instanceName == null) {
            return;
        }

        LOG.info("Module: {} Instance: {} attempting to configure.",
                moduleName, instanceName);
        Stopwatch stopWatch = Stopwatch.createStarted();
        ObjectName objectName = null;
        for (int i = 0; i < MODULE_TIMEOUT_MILLIS; i++) {
            try {
                ConfigRegistry configRegistryClient = new ConfigRegistryJMXClient(ManagementFactory
                        .getPlatformMBeanServer());
                objectName = configRegistryClient.lookupConfigBean(moduleName, instanceName);
                LOG.info("Module: {} Instance: {} ObjectName: {}.",
                        moduleName,instanceName,objectName);
                break;
            } catch (Exception e) {
                if (i < MODULE_TIMEOUT_MILLIS) {
                    Thread.sleep(1);
                    continue;
                } else {
                    throw e;
                }
            }
        }
        if (objectName != null) {
            LOG.info("Module: {} Instance: {} configured after {} ms",
                moduleName,instanceName,
                stopWatch.elapsed(TimeUnit.MILLISECONDS));
        } else {
            throw new RuntimeException("NOT FOUND Module: " + moduleName + " Instance: " + instanceName
                    + " configured after " + stopWatch.elapsed(TimeUnit.MILLISECONDS) + " ms");
        }
    }
}
