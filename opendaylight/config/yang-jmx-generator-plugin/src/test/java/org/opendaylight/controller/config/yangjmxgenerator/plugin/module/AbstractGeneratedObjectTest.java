/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.yangjmxgenerator.plugin.module;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.google.common.io.Files;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import net.sourceforge.pmd.lang.Parser;
import net.sourceforge.pmd.lang.ParserOptions;
import net.sourceforge.pmd.lang.ast.Node;
import net.sourceforge.pmd.lang.java.Java17Parser;
import org.opendaylight.controller.config.yangjmxgenerator.plugin.AbstractGeneratorTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AbstractGeneratedObjectTest extends AbstractGeneratorTest {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractGeneratedObjectTest.class);

    protected void assertHasMethodNamed(Node c, String method) {
        assertTrue(c.hasDescendantMatchingXPath("//MethodDeclaration[MethodDeclarator[@Image='" +
                method +
                "']]"));
    }

    protected Node parse(File dstFile) throws IOException {
        assertNotNull(dstFile);
        LOG.debug(Files.toString(dstFile, StandardCharsets.UTF_8));
        Parser parser = new Java17Parser(new ParserOptions());
        return parser.parse(dstFile.toString(), new FileReader(dstFile));
    }


}
