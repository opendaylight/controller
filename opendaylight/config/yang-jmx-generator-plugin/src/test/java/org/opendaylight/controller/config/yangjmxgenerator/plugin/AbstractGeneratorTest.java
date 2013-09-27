/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yangjmxgenerator.plugin;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.opendaylight.controller.config.yangjmxgenerator.AbstractYangTest;

public abstract class AbstractGeneratorTest extends AbstractYangTest {
    private static final File GENERATOR_OUTPUT_PATH_ROOT = new File(
            "target/testgen");
    protected final File generatorOutputPath;

    public AbstractGeneratorTest() {
        generatorOutputPath = new File(GENERATOR_OUTPUT_PATH_ROOT, getClass()
                .getSimpleName());
    }

    @Before
    public void cleanUpDirectory() throws Exception {
        FileUtils.deleteDirectory(generatorOutputPath);
    }

}
