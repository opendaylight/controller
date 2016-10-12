/*
 * Copyright (c) 2014, 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.node.utils;

import java.io.PrintStream;
import org.opendaylight.yangtools.yang.data.api.schema.LeafNode;
import org.opendaylight.yangtools.yang.data.api.schema.LeafSetEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

public class NormalizedNodePrinter implements NormalizedNodeVisitor {
    // This class is legacy and appears to serve as an example. This class will be removed at some point so ignoring
    // the CS warning re: use of System out.
    @SuppressWarnings("checkstyle:RegexpSingleLineJava")
    private static final PrintStream PRINTER = System.out;

    private static String spaces(int num) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < num; i++) {
            builder.append(' ');
        }
        return builder.toString();
    }

    @Override
    public void visitNode(int level, String parentPath, NormalizedNode<?, ?> normalizedNode) {
        PRINTER.println(spaces(level * 4) + normalizedNode.getClass().toString() + ":"
            + normalizedNode.getIdentifier());
        if (normalizedNode instanceof LeafNode || normalizedNode instanceof LeafSetEntryNode) {
            PRINTER.println(spaces(level * 4) + " parentPath = " + parentPath);
            PRINTER.println(spaces(level * 4) + " key = " + normalizedNode.getClass().toString() + ":"
                + normalizedNode.getIdentifier());
            PRINTER.println(spaces(level * 4) + " value = " + normalizedNode.getValue());
        }
    }
}
