/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yang.logback.appender;

import ch.qos.logback.core.ConsoleAppender;
import nu.xom.Element;
import org.junit.Test;
import org.opendaylight.controller.config.api.jmx.CommitStatus;
import org.opendaylight.controller.config.spi.ModuleFactory;
import org.opendaylight.controller.config.util.ConfigTransactionJMXClient;
import org.opendaylight.controller.config.yang.logback.api.HasAppendersImpl;

import java.util.Collection;
import java.util.Map;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class ConsoleAppenderModuleTest extends AbstractAppenderModuleTest {
    private final ConsoleAppenderModuleFactory factory = new ConsoleAppenderModuleFactory();

    @Override
    protected Collection<? extends ModuleFactory> getTestedFactories() {
        return asList(factory);
    }

    private Collection<ConsoleAppenderTO> getTOs() throws Exception  {
        HasAppendersImpl<ConsoleAppenderTO> instance = getInstanceFromCurrentConfig();
        return instance.getAppenderTOs();
    }

    @Test
    public void createDefaultModule_noLogbackConfig() throws Exception {
        ConfigTransactionJMXClient transaction = configRegistryClient.createTransaction();
        CommitStatus status = transaction.commit();
        assertStatus(status, 1, 0, 0);
        Collection<ConsoleAppenderTO> appenderTOs = getTOs();
        assertEquals(0, appenderTOs.size());
    }

    /**
     * Loads logback file, creates empty transaction, checks created HasAppenderImpl instance creates
     * expected xml file.
     */
    @Test
    public void createDefaultModule_TwoAppenders() throws Exception {
        resettingLogbackTestBase.reconfigureUsingClassPathFile("/appender_discovery.xml");
        ConfigTransactionJMXClient transaction = configRegistryClient.createTransaction();
        CommitStatus status = transaction.commit();
        assertStatus(status, 1, 0, 0);
        Collection<ConsoleAppenderTO> appenderTOs = getTOs();
        assertEquals(2, appenderTOs.size());
        // STDOUT should have settings INFO, some pattern
        ConsoleAppenderTO stdout = findByName(appenderTOs, "STDOUT");
        assertEquals("INFO", stdout.getThresholdFilter());
        String stdoutPattern = "%level %logger - %msg%n";
        assertEquals(stdoutPattern, stdout.getEncoderPattern());
        // STDOUT2 should have no settings
        ConsoleAppenderTO stdout2 = findByName(appenderTOs, "STDOUT2");
        assertNull(stdout2.getThresholdFilter());
        assertEquals("%msg%n", stdout2.getEncoderPattern());

        // check output xml
        Map<String, Element> xmlRepresentationOfAppenders = getInstanceFromCurrentConfig().getXmlRepresentationOfAppenders();
        assertEquals(2, xmlRepresentationOfAppenders.size());

        Element stdoutElement = xmlRepresentationOfAppenders.get("STDOUT");
        assertNodeIs(ConsoleAppender.class.getCanonicalName(), stdoutElement.query("/appender/@class"));
        assertNodeIs("STDOUT", stdoutElement.query("/appender/@name"));
        assertNodeIs(stdoutPattern, stdoutElement.query("/appender/encoder/pattern"));
        assertNodeIs("INFO", stdoutElement.query("/appender/filter/level"));
    }


}
