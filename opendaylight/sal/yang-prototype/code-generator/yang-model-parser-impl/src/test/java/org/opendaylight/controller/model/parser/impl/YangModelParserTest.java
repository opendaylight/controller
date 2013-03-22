/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.model.parser.impl;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.junit.Test;
import org.opendaylight.controller.antlrv4.code.gen.YangLexer;
import org.opendaylight.controller.antlrv4.code.gen.YangParser;
import org.opendaylight.controller.sal.binding.model.api.GeneratedType;

public class YangModelParserTest {

    @Test
    public void testPackageNameConstruction() {
        try {
            final InputStream inStream = getClass().getResourceAsStream(
                    "/simple-list-demo.yang");
            if (inStream != null) {
                ANTLRInputStream input = new ANTLRInputStream(inStream);
                final YangLexer lexer = new YangLexer(input);
                final CommonTokenStream tokens = new CommonTokenStream(lexer);
                final YangParser parser = new YangParser(tokens);

                final ParseTree tree = parser.yang();
                final ParseTreeWalker walker = new ParseTreeWalker();

                // final YangModelParserImpl modelParser = new
                // YangModelParserImpl(tree, new TypeProviderImpl());
                // walker.walk(modelParser, tree);
                // final Set<GeneratedType> genTypes =
                // modelParser.generatedTypes();

                // getTypesTest(genTypes);

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void getTypesTest(final Set<GeneratedType> genTypes) {
        int typesCount = 0;
        for (final GeneratedType type : genTypes) {
            if (type.getName().equals("Topology")) {
                assertEquals(4, type.getMethodDefinitions().size());
                ++typesCount;
            }
            if (type.getName().equals("NetworkNodes")) {
                assertEquals(2, type.getMethodDefinitions().size());
                ++typesCount;
            }
            if (type.getName().equals("NetworkNode")) {
                assertEquals(1, type.getMethodDefinitions().size());
                ++typesCount;
            }
            if (type.getName().equals("NodeAttributes")) {
                assertEquals(2, type.getMethodDefinitions().size());
                ++typesCount;
            }
            if (type.getName().equals("NetworkLinks")) {
                assertEquals(2, type.getMethodDefinitions().size());
                ++typesCount;
            }
            if (type.getName().equals("NetworkLink")) {
                assertEquals(3, type.getMethodDefinitions().size());
                ++typesCount;
            }
            if (type.getName().equals("Source")) {
                assertEquals(2, type.getMethodDefinitions().size());
                ++typesCount;
            }
            if (type.getName().equals("Destination")) {
                assertEquals(2, type.getMethodDefinitions().size());
                ++typesCount;
            }
            if (type.getName().equals("LinkAttributes")) {
                assertEquals(0, type.getMethodDefinitions().size());
                ++typesCount;
            }
        }
        assertEquals(9, typesCount);
    }
}
