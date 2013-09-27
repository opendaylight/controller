/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yangjmxgenerator.plugin;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.matchers.JUnitMatchers.containsString;

import org.junit.Test;
import org.opendaylight.controller.config.yangjmxgenerator.ConfigConstants;
import org.opendaylight.controller.config.yangjmxgenerator.PackageTranslatorTest;

import com.google.common.collect.Sets;

public class JMXGeneratorFileNamesValidationTest extends JMXGeneratorTest {

    @Test
    public void test() {
        map.clear();
        map.put(JMXGenerator.NAMESPACE_TO_PACKAGE_PREFIX + "1",
                ConfigConstants.CONFIG_NAMESPACE + ":test:files1"
                        + JMXGenerator.NAMESPACE_TO_PACKAGE_DIVIDER
                        + PackageTranslatorTest.EXPECTED_PACKAGE_PREFIX);
        map.put(JMXGenerator.NAMESPACE_TO_PACKAGE_PREFIX + "2",
                ConfigConstants.CONFIG_NAMESPACE + ":test:files"
                        + JMXGenerator.NAMESPACE_TO_PACKAGE_DIVIDER
                        + PackageTranslatorTest.EXPECTED_PACKAGE_PREFIX);

        map.put(JMXGenerator.MODULE_FACTORY_FILE_BOOLEAN, "randomValue");
        jmxGenerator.setAdditionalConfig(map);
        try {
            jmxGenerator.generateSources(context, outputBaseDir,
                    Sets.newHashSet(testFilesModule, testFiles1Module));
            fail();
        } catch (RuntimeException e) {
            final Throwable cause = e.getCause();
            assertNotNull(cause);
            assertTrue(cause instanceof IllegalStateException);
            assertThat(cause.getMessage(),
                    containsString("Name conflict in generated files"));
            assertThat(cause.getMessage(), containsString("DtoA.java"));
        }
    }

}
