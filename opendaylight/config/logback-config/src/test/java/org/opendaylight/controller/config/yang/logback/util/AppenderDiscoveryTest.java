/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yang.logback.util;

import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.FileAppender;
import org.junit.Test;
import org.opendaylight.controller.config.yang.logback.ResettingLogbackTestBase;
import org.opendaylight.controller.config.yang.logback.memoryappender.MemoryAppender;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class AppenderDiscoveryTest extends ResettingLogbackTestBase {

    @Test
    public void testDiscovery() throws Exception {
        reconfigureUsingClassPathFile("/appender_discovery.xml");
        AppenderDiscovery tested = new AppenderDiscovery();
        Map<String,ConsoleAppender> consoleAppenders = tested.findAppenders(ConsoleAppender.class);
        assertEquals(2, consoleAppenders.size());
        ConsoleAppender stdout = consoleAppenders.get("STDOUT");
        assertNotNull(stdout);
        ConsoleAppender stdout2 = consoleAppenders.get("STDOUT2");
        assertNotNull(stdout2);

        assertEquals(1, tested.findAppenders(MemoryAppender.class).size());
        assertEquals(0, tested.findAppenders(FileAppender.class).size());

    }

}
