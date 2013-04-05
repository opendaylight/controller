/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.yang2sources.plugin;

import static org.hamcrest.core.Is.*;
import static org.junit.Assert.*;

import java.io.File;
import java.util.Collection;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.yang2sources.plugin.ConfigArg.ResourceProviderArg;
import org.opendaylight.controller.yang2sources.spi.ResourceGenerator;

public class GenerateResourcesTest {

    private String yang;
    private YangToResourcesMojo mojo;
    private File outDir;

    @Before
    public void setUp() {
        yang = new File(getClass().getResource("/mock.yang").getFile())
                .getParent();
        outDir = new File("outputDir");
        mojo = new YangToResourcesMojo(
                new ResourceProviderArg[] {
                        new ResourceProviderArg(ProviderMock.class.getName(),
                                outDir),
                        new ResourceProviderArg(ProviderMock2.class.getName(),
                                outDir) }, yang);
    }

    @Test
    public void test() throws Exception {
        mojo.execute();
        assertThat(ProviderMock.called, is(1));
        assertThat(ProviderMock2.called, is(1));
        assertThat(ProviderMock2.baseDir, is(outDir));
        assertThat(ProviderMock.baseDir, is(outDir));
    }

    public static class ProviderMock implements ResourceGenerator {

        private static int called = 0;
        private static File baseDir;

        @Override
        public void generateResourceFiles(Collection<File> resources,
                File outputDir) {
            called++;
            baseDir = outputDir;
        }
    }

    public static class ProviderMock2 implements ResourceGenerator {

        private static int called = 0;
        private static File baseDir;

        @Override
        public void generateResourceFiles(Collection<File> resources,
                File outputDir) {
            called++;
            baseDir = outputDir;
        }
    }

}
