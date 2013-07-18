/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.yang.parser.impl;

import static org.junit.Assert.*;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.opendaylight.controller.yang.parser.util.YangParseException;
import org.opendaylight.controller.yang.parser.util.YangValidationException;

public class YangParserNegativeTest {

    @Test
    public void testInvalidImport() throws IOException {
        try {
            try (InputStream stream = new FileInputStream(getClass().getResource("/negative-scenario/testfile1.yang")
                    .getPath())) {
                TestUtils.loadModule(stream);
                fail("ValidationException should by thrown");
            }
        } catch (YangValidationException e) {
            assertTrue(e.getMessage().contains("Not existing module imported"));
        }
    }

    @Test
    public void testTypeNotFound() throws IOException {
        try {
            try (InputStream stream = new FileInputStream(getClass().getResource("/negative-scenario/testfile2.yang")
                    .getPath())) {
                TestUtils.loadModule(stream);
                fail("YangParseException should by thrown");
            }
        } catch (YangParseException e) {
            assertEquals(e.getMessage(), "Error in module 'test2' at line 24: Referenced type 'int-ext' not found.");
        }
    }

    @Test
    public void testInvalidAugmentTarget() throws IOException {
        try {
            final List<InputStream> streams = new ArrayList<>(2);
            try (InputStream testFile0 = new FileInputStream(getClass()
                    .getResource("/negative-scenario/testfile0.yang").getPath())) {
                streams.add(testFile0);
                try (InputStream testFile3 = new FileInputStream(getClass().getResource(
                        "/negative-scenario/testfile3.yang").getPath())) {
                    streams.add(testFile3);
                    assertEquals("Expected loaded files count is 2", 2, streams.size());
                    TestUtils.loadModules(streams);
                    fail("YangParseException should by thrown");
                }
            }
        } catch (YangParseException e) {
            assertTrue(e.getMessage().contains("Failed to resolve augments in module 'test3'."));
        }
    }

    @Test
    public void testInvalidRefine() throws IOException {
        try {
            try (InputStream stream = new FileInputStream(getClass().getResource("/negative-scenario/testfile4.yang")
                    .getPath())) {
                TestUtils.loadModule(stream);
                fail("YangParseException should by thrown");
            }
        } catch (YangParseException e) {
            assertTrue(e.getMessage().contains("Can not refine 'presence' for 'node'."));
        }
    }

    @Test
    public void testInvalidLength() throws IOException {
        try {
            try (InputStream stream = new FileInputStream(getClass().getResource("/negative-scenario/testfile5.yang")
                    .getPath())) {
                TestUtils.loadModule(stream);
                fail("YangParseException should by thrown");
            }
        } catch (YangParseException e) {
            assertTrue(e.getMessage().contains("Invalid length constraint: <4, 10>"));
        }
    }

    @Test
    public void testInvalidRange() throws IOException {
        try {
            try (InputStream stream = new FileInputStream(getClass().getResource("/negative-scenario/testfile6.yang")
                    .getPath())) {
                TestUtils.loadModule(stream);
                fail("YangParseException should by thrown");
            }
        } catch (YangParseException e) {
            assertTrue(e.getMessage().contains("Invalid range constraint: <5, 20>"));
        }
    }

    @Test
    public void testDuplicateContainer() throws IOException {
        try {
            try (InputStream stream = new FileInputStream(getClass().getResource(
                    "/negative-scenario/duplicity/container.yang").getPath())) {
                TestUtils.loadModule(stream);
                fail("YangParseException should by thrown");
            }
        } catch (YangParseException e) {
            String expected = "Error in module 'container' at line 10: Can not add 'container foo': node with same name already declared at line 6";
            assertEquals(expected, e.getMessage());
        }
    }

    @Test
    public void testDuplicateContainerList() throws IOException {
        try {
            try (InputStream stream = new FileInputStream(getClass().getResource(
                    "/negative-scenario/duplicity/container-list.yang").getPath())) {
                TestUtils.loadModule(stream);
                fail("YangParseException should by thrown");
            }
        } catch (YangParseException e) {
            String expected = "Error in module 'container-list' at line 10: Can not add 'list foo': node with same name already declared at line 6";
            assertEquals(expected, e.getMessage());
        }
    }

    @Test
    public void testDuplicateContainerLeaf() throws IOException {
        try {
            try (InputStream stream = new FileInputStream(getClass().getResource(
                    "/negative-scenario/duplicity/container-leaf.yang").getPath())) {
                TestUtils.loadModule(stream);
                fail("YangParseException should by thrown");
            }
        } catch (YangParseException e) {
            String expected = "Error in module 'container-leaf' at line 10: Can not add 'leaf foo': node with same name already declared at line 6";
            assertEquals(expected, e.getMessage());
        }
    }

    @Test
    public void testDuplicateTypedef() throws IOException {
        try {
            try (InputStream stream = new FileInputStream(getClass().getResource(
                    "/negative-scenario/duplicity/typedef.yang").getPath())) {
                TestUtils.loadModule(stream);
                fail("YangParseException should by thrown");
            }
        } catch (YangParseException e) {
            String expected = "Error in module 'typedef' at line 10: typedef with same name 'int-ext' already declared at line 6";
            assertEquals(expected, e.getMessage());
        }
    }

    @Test
    public void testDuplicityInAugmentTarget1() throws Exception {
        try {
            try (InputStream stream1 = new FileInputStream(getClass().getResource(
                    "/negative-scenario/duplicity/augment0.yang").getPath());
                    InputStream stream2 = new FileInputStream(getClass().getResource(
                            "/negative-scenario/duplicity/augment1.yang").getPath())) {
                TestUtils.loadModules(Arrays.asList(stream1, stream2));
                fail("YangParseException should by thrown");
            }
        } catch (YangParseException e) {
            String expected = "Error in module 'augment1' at line 11: Can not add 'leaf id' to node 'bar' in module 'augment0': node with same name already declared at line 9";
            assertEquals(expected, e.getMessage());
        }
    }

    @Test
    public void testDuplicityInAugmentTarget2() throws Exception {
        try {
            try (InputStream stream1 = new FileInputStream(getClass().getResource(
                    "/negative-scenario/duplicity/augment0.yang").getPath());
                    InputStream stream2 = new FileInputStream(getClass().getResource(
                            "/negative-scenario/duplicity/augment2.yang").getPath())) {
                TestUtils.loadModules(Arrays.asList(stream1, stream2));
                fail("YangParseException should by thrown");
            }
        } catch (YangParseException e) {
            String expected = "Error in module 'augment2' at line 11: Can not add 'anyxml delta' to node 'choice-ext' in module 'augment0': case with same name already declared at line 18";
            assertEquals(expected, e.getMessage());
        }
    }

}
