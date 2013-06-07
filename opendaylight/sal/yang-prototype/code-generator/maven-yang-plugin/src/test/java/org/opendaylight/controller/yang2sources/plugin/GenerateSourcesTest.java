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
import static org.junit.matchers.JUnitMatchers.*;
import static org.mockito.Mockito.*;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.yang.model.api.Module;
import org.opendaylight.controller.yang.model.api.SchemaContext;
import org.opendaylight.controller.yang2sources.plugin.ConfigArg.CodeGeneratorArg;
import org.opendaylight.controller.yang2sources.spi.CodeGenerator;

import com.google.common.collect.Lists;

public class GenerateSourcesTest {

    private String yang;
    private YangToSourcesMojo mojo;
    private File outDir;
    @Mock
    private MavenProject project;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        yang = new File(getClass().getResource("/mock.yang").getFile())
                .getParent();
        outDir = new File("/outputDir");
        mojo = new YangToSourcesMojo(
                new CodeGeneratorArg[] { new CodeGeneratorArg(
                        GeneratorMock.class.getName(), "outputDir") }, yang);
        doReturn(new File("")).when(project).getBasedir();
        mojo.project = project;
    }

    @Test
    public void test() throws Exception {
        mojo.execute();
        assertThat(GeneratorMock.called, is(1));
        assertThat(GeneratorMock.outputDir, is(outDir));
        assertNotNull(GeneratorMock.log);
        assertTrue(GeneratorMock.additionalCfg.isEmpty());
        assertThat(GeneratorMock.resourceBaseDir.toString(),
                containsString("target" + File.separator
                        + "generated-resources"));
    }

    public static class GeneratorMock implements CodeGenerator {

        private static int called = 0;
        private static File outputDir;
        private static Log log;
        private static Map<String, String> additionalCfg;
        private static File resourceBaseDir;

        @Override
        public Collection<File> generateSources(SchemaContext context,
                File outputBaseDir, Set<Module> currentModules)
                throws IOException {
            called++;
            outputDir = outputBaseDir;
            return Lists.newArrayList();
        }

        @Override
        public void setLog(Log log) {
            GeneratorMock.log = log;
        }

        @Override
        public void setAdditionalConfig(
                Map<String, String> additionalConfiguration) {
            GeneratorMock.additionalCfg = additionalConfiguration;
        }

        @Override
        public void setResourceBaseDir(File resourceBaseDir) {
            GeneratorMock.resourceBaseDir = resourceBaseDir;

        }
    }

}
