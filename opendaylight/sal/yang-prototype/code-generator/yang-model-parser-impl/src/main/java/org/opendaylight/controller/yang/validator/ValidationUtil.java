/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/eplv10.html
 */
package org.opendaylight.controller.yang.validator;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.antlr.v4.runtime.tree.ParseTree;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Module_stmtContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Submodule_stmtContext;
import org.opendaylight.controller.yang.parser.util.YangModelBuilderUtil;
import org.opendaylight.controller.yang.parser.util.YangValidationException;

/**
 * Validation utilities
 */
final class ValidationUtil {

    static void ex(String message) {
        throw new YangValidationException(message);
    }

    static Set<String> getDuplicates(Collection<String> keyList) {
        Set<String> all = new HashSet<String>();
        Set<String> duplicates = new HashSet<String>();

        for (String key : keyList) {
            if (!all.add(key))
                duplicates.add(key);
        }
        return duplicates;
    }

    static List<String> listKeysFromId(String keys) {
        return Arrays.asList(keys.split(" "));
    }

    static String getRootParentName(ParseTree ctx) {
        ParseTree root = getRootParent(ctx);
        return ValidationUtil.getName(root);
    }

    private static ParseTree getRootParent(ParseTree ctx) {
        ParseTree root = ctx;
        while (root.getParent() != null) {
            if (root.getClass().equals(Module_stmtContext.class)
                    || root.getClass().equals(Submodule_stmtContext.class))
                break;
            root = root.getParent();
        }
        return root;
    }

    static String getName(ParseTree child) {
        return YangModelBuilderUtil.stringFromNode(child);
    }

    static String f(String base, Object... args) {
        return String.format(base, args);
    }

    /**
     * Get simple name from statement class e.g. Module from Module_stmt_context
     */
    static String getSimpleStatementName(
            Class<? extends ParseTree> typeOfStatement) {

        String className = typeOfStatement.getSimpleName();
        int lastIndexOf = className.indexOf('$');
        className = lastIndexOf == -1 ? className : className
                .substring(lastIndexOf + 1);
        int indexOfStmt = className.indexOf("_stmt");
        int index = indexOfStmt == -1 ? className.indexOf("_arg") : indexOfStmt;
        return className.substring(0, index).replace('_', '-');
    }

    static int countPresentChildrenOfType(ParseTree parent,
            Set<Class<? extends ParseTree>> expectedChildTypes) {
        int foundChildrenOfType = 0;

        for (Class<? extends ParseTree> type : expectedChildTypes) {
            foundChildrenOfType += countPresentChildrenOfType(parent, type);
        }
        return foundChildrenOfType;
    }

    static int countPresentChildrenOfType(ParseTree parent,
            Class<? extends ParseTree> expectedChildType) {
        int foundChildrenOfType = 0;

        for (int i = 0; i < parent.getChildCount(); i++) {
            ParseTree child = parent.getChild(i);
            if (expectedChildType.isInstance(child))
                foundChildrenOfType++;
        }
        return foundChildrenOfType;
    }

}
