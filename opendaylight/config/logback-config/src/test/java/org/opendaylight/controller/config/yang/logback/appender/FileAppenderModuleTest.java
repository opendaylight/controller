/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.yang.logback.appender;

import ch.qos.logback.core.FileAppender;
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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class FileAppenderModuleTest extends AbstractAppenderModuleTest {
    private final FileAppenderModuleFactory factory = new FileAppenderModuleFactory();

    @Override
    protected Collection<? extends ModuleFactory> getTestedFactories() {
        return asList(factory);
    }

    private Collection<FileAppenderTO> getTOs() throws Exception  {
        HasAppendersImpl<FileAppenderTO> instance = getInstanceFromCurrentConfig();
        return instance.getAppenderTOs();
    }

    @Test
    public void loadXML_validateOutputXML() throws Exception {
        resettingLogbackTestBase.reconfigureUsingClassPathFile("/file_appender.xml");
        ConfigTransactionJMXClient transaction = configRegistryClient.createTransaction();
        CommitStatus status = transaction.commit();
        assertStatus(status, 1, 0, 0);
        Collection<FileAppenderTO> tos = getTOs();
        assertEquals(1, tos.size());
        FileAppenderTO fileTO = findByName(tos, "FILE");
        assertEquals("%msg%n", fileTO.getEncoderPattern());
        assertNull(fileTO.getThresholdFilter());
        String fileName = "target/testFile.log";
        assertEquals(fileName, fileTO.getFile());
        assertTrue(fileTO.getAppend());

        // check output xml
        Map<String, Element> xmlRepresentationOfAppenders = getInstanceFromCurrentConfig().getXmlRepresentationOfAppenders();
        assertEquals(1, xmlRepresentationOfAppenders.size());
        Element fileElement = xmlRepresentationOfAppenders.get("FILE");
        assertNotNull(fileElement);
        assertNodeIs(FileAppender.class.getCanonicalName(), fileElement.query("/appender/@class"));
        assertNodeIs(fileName, fileElement.query("/appender/file"));
        assertNodeIs(Boolean.TRUE.toString(), fileElement.query("/appender/append"));
    }

}
