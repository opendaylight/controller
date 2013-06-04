/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.yang.parser.impl;

import static org.junit.Assert.*;

import java.io.IOException;

import org.junit.Test;
import org.opendaylight.controller.yang.parser.util.YangParseException;
import org.opendaylight.controller.yang.parser.util.YangValidationException;

public class YangParserNegativeTest {

    @Test
    public void testInvalidImport() throws IOException {
        try {
            TestUtils.loadModule("/negative-scenario/testfile1.yang");
            fail("ValidationException should by thrown");
        } catch(YangValidationException e) {
            assertTrue(e.getMessage().contains("Not existing module imported"));
        }
    }

    @Test
    public void testTypeNotFound() throws IOException {
        try {
            TestUtils.loadModule("/negative-scenario/testfile2.yang");
            fail("YangParseException should by thrown");
        } catch(YangParseException e) {
            assertTrue(e.getMessage().contains("Error in module 'test2' on line 24: Referenced type 'int-ext' not found."));
        }
    }

    @Test
    public void testInvalidAugmentTarget() throws IOException {
        try {
            TestUtils.loadModules("/negative-scenario/testfile0.yang", "/negative-scenario/testfile3.yang");
            fail("YangParseException should by thrown");
        } catch(YangParseException e) {
            assertTrue(e.getMessage().contains("Failed to resolve augments in module 'test3'."));
        }
    }

    @Test
    public void testInvalidRefine() throws IOException {
        try {
            TestUtils.loadModule("/negative-scenario/testfile4.yang");
            fail("YangParseException should by thrown");
        } catch(YangParseException e) {
            assertTrue(e.getMessage().contains("Can not refine 'presence' for 'node'."));
        }
    }

}
