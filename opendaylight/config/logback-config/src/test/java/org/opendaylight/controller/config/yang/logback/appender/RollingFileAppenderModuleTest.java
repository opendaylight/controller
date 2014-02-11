/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.yang.logback.appender;

import ch.qos.logback.core.rolling.RollingFileAppender;
import nu.xom.Element;
import nu.xom.Node;
import org.junit.Test;
import org.opendaylight.controller.config.api.jmx.CommitStatus;
import org.opendaylight.controller.config.spi.ModuleFactory;
import org.opendaylight.controller.config.util.ConfigTransactionJMXClient;
import org.opendaylight.controller.config.yang.logback.api.HasAppendersImpl;

import java.util.Collection;
import java.util.Map;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class RollingFileAppenderModuleTest extends AbstractAppenderModuleTest {
    private final RollingFileAppenderModuleFactory factory = new RollingFileAppenderModuleFactory();

    @Override
    protected Collection<? extends ModuleFactory> getTestedFactories() {
        return asList(factory);
    }

    private Collection<RollingFileAppenderTO> getTOs() throws Exception {
        HasAppendersImpl<RollingFileAppenderTO> instance = getInstanceFromCurrentConfig();
        return instance.getAppenderTOs();
    }

    @Test
    public void loadXML_validateOutputXML() throws Exception {
        resettingLogbackTestBase.reconfigureUsingClassPathFile("/rolling_file_appender.xml");
        ConfigTransactionJMXClient transaction = configRegistryClient.createTransaction();
        CommitStatus status = transaction.commit();
        assertStatus(status, 1, 0, 0);
        Collection<RollingFileAppenderTO> tos = getTOs();
        assertEquals(2, tos.size());

        RollingFileAppenderTO varlogfile = findByName(tos, "VARLOGFILE");
        assertEquals("varlogfile %msg%n", varlogfile.getEncoderPattern());
        assertNull(varlogfile.getThresholdFilter());
        String fileName = "target/osgi.log";
        assertEquals(fileName, varlogfile.getFile());


        // check output xml

        Map<String, Element> xmlRepresentationOfAppenders = getInstanceFromCurrentConfig().getXmlRepresentationOfAppenders();
        assertEquals(2, xmlRepresentationOfAppenders.size());
        Element varElement = xmlRepresentationOfAppenders.get("VARLOGFILE");
        assertNodeIs(RollingFileAppender.class.getCanonicalName(), varElement.query("/appender/@class"));
        assertNotNull(varElement);
        assertNodeIs(fileName, varElement.query("/appender/file"));
        assertEquals(1, varElement.query("//rollingPolicy").size());

        Node varRolllingPolicyElement = varElement.query("/appender/rollingPolicy[@class='" +
                ch.qos.logback.core.rolling.FixedWindowRollingPolicy.class.getCanonicalName() +
                "']").get(0);

        assertNodeIs("5", varRolllingPolicyElement.query("maxIndex"));
        assertNodeIs("1", varRolllingPolicyElement.query("minIndex"));
        assertNodeIs("target/osgi.log.%i.gz", varRolllingPolicyElement.query("fileNamePattern"));

        Node triggeringPolicy = varElement.query("/appender/triggeringPolicy[@class='" +
                ch.qos.logback.core.rolling.SizeBasedTriggeringPolicy.class.getCanonicalName() +
                "']").get(0);
        assertNodeIs("50MB", triggeringPolicy.query("maxFileSize"));

        Element bgpElement = xmlRepresentationOfAppenders.get("BGPDUMPFILE");
        assertNodeIs(RollingFileAppender.class.getCanonicalName(), bgpElement.query("/appender/@class"));
        assertEquals(1, bgpElement.query("//rollingPolicy").size());
        Node bgpRollingPolicyElement = bgpElement.query("/appender/rollingPolicy[@class='" +
                ch.qos.logback.core.rolling.TimeBasedRollingPolicy.class.getCanonicalName() +
                "']").get(0);
        assertNodeIs("30", bgpRollingPolicyElement.query("maxHistory"));
        assertNodeIs("target/bgp.log.%d{yyyy-MM-dd}.gz", bgpRollingPolicyElement.query("fileNamePattern"));

    }


}
