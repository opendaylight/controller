/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yangjmxgenerator.plugin;

import java.io.File;
import org.junit.Before;
import org.opendaylight.controller.config.yangjmxgenerator.AbstractYangTest;

public abstract class AbstractGeneratorTest extends AbstractYangTest {
    private static final File GENERATOR_OUTPUT_PATH_ROOT = new File("target/testgen");
    protected final File generatorOutputPath;

    public AbstractGeneratorTest() {
        generatorOutputPath = new File(GENERATOR_OUTPUT_PATH_ROOT, getClass().getSimpleName());
    }

    @Before
    public void cleanUpDirectory() throws Exception {
        deleteFolder(generatorOutputPath);
    }

    public void deleteFolder(final File folder) {
        File[] files = folder.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory()) {
                    deleteFolder(f);
                } else {
                    f.delete();
                }
            }
        }
        folder.delete();
    }
}
