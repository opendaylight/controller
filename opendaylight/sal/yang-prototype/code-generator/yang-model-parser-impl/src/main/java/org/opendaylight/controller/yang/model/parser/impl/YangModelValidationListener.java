/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.yang.model.parser.impl;

import java.net.URI;
import java.net.URISyntaxException;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import org.antlr.v4.runtime.tree.ParseTree;
import org.opendaylight.controller.antlrv4.code.gen.YangParser;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Belongs_to_stmtContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Import_stmtContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Include_stmtContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Module_header_stmtsContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Namespace_stmtContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Prefix_stmtContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Revision_date_stmtContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Revision_stmtContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Revision_stmtsContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Submodule_header_stmtsContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Submodule_stmtContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Yang_version_stmtContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParserBaseListener;
import org.opendaylight.controller.yang.model.parser.util.YangModelBuilderUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Validation listener that validates yang statements according to RFC-6020.
 * This validator expects only one module or submodule per file.
 */

/*
 * TODO is this assumption(module per file) correct ? if so, should a check be
 * performed ?
 * 
 * TODO break into smaller classes e.g. class for header statements, body
 * statements...
 */
final class YangModelValidationListener extends YangParserBaseListener {

    private static final Logger logger = LoggerFactory
            .getLogger(YangModelValidationListener.class);

    private final Set<String> uniquePrefixes;
    private final Set<String> uniqueImports;
    private final Set<String> uniqueIncludes;

    public YangModelValidationListener() {
        super();
        uniquePrefixes = new HashSet<String>();
        uniqueImports = new HashSet<String>();
        uniqueIncludes = new HashSet<String>();
    }

    /**
     * Rules:
     * <ol>
     * <li>Identifier contains only permitted characters</li>
     * <li>One revision statements present</li>
     * <li>One header statements present</li>
     * </ol>
     */
    @Override
    public void enterModule_stmt(YangParser.Module_stmtContext ctx) {
        String moduleName = getName(ctx);

        checkIdentifier(moduleName, "Module");

        checkPresentChildOfType(ctx, Revision_stmtsContext.class,
                f("Missing revision statements in module:%s", moduleName), true);

        checkPresentChildOfType(ctx, Module_header_stmtsContext.class,
                f("Missing header statements in module:%s", moduleName), true);
    }

    /**
     * Rules:
     * <ol>
     * <li>Identifier contains only permitted characters</li>
     * <li>One revision statements present</li>
     * <li>One header statements present</li>
     * </ol>
     */
    @Override
    public void enterSubmodule_stmt(Submodule_stmtContext ctx) {
        String submoduleName = getName(ctx);

        checkIdentifier(submoduleName, "Submodule");

        checkPresentChildOfType(
                ctx,
                Revision_stmtsContext.class,
                f("Missing revision statements in submodule:%s", submoduleName),
                true);

        checkPresentChildOfType(ctx, Submodule_header_stmtsContext.class,
                f("Missing header statements in submodule:%s", submoduleName),
                true);
    }

    /**
     * Rules:
     * <ol>
     * <li>One Belongs-to statement present</li>
     * </ol>
     */
    @Override
    public void enterSubmodule_header_stmts(Submodule_header_stmtsContext ctx) {
        String submoduleName = getRootParentName(ctx);

        checkPresentChildOfType(
                ctx,
                Belongs_to_stmtContext.class,
                f("Missing belongs-to statement in submodule:%s", submoduleName),
                true);

        // check Yang version present, if not issue warning
        checkYangVersion(ctx, submoduleName);
    }

    /**
     * Rules:
     * <ol>
     * <li>Identifier contains only permitted characters</li>
     * <li>One Prefix statement child</li>
     * </ol>
     */
    @Override
    public void enterBelongs_to_stmt(Belongs_to_stmtContext ctx) {
        String belongToName = getName(ctx);
        String rootParentName = getRootParentName(ctx);

        checkIdentifier(belongToName,
                f("In (sub)module:%s , Belongs-to statement", rootParentName));

        checkPresentChildOfType(
                ctx,
                Prefix_stmtContext.class,
                f("Missing prefix statement in belongs-to:%s, in (sub)module:%s",
                        belongToName, rootParentName), true);
    }

    /**
     * Rules:
     * <ol>
     * <li>At least one Revision statement present</li>
     * </ol>
     */
    @Override
    public void enterRevision_stmts(Revision_stmtsContext ctx) {
        String rootParentName = getRootParentName(ctx);

        checkPresentChildOfType(
                ctx,
                Revision_stmtContext.class,
                f("Missing at least one revision statement in (sub)module:%s",
                        rootParentName), false);
    }

    /**
     * Rules:
     * <ol>
     * <li>One Namespace statement present</li>
     * </ol>
     */
    @Override
    public void enterModule_header_stmts(Module_header_stmtsContext ctx) {
        String moduleName = getRootParentName(ctx);

        checkPresentChildOfType(ctx, Namespace_stmtContext.class,
                f("Missing namespace statement in module:%s", moduleName), true);

        // check Yang version present, if not issue warning
        checkYangVersion(ctx, moduleName);
    }

    /**
     * Rules:
     * <ol>
     * <li>Namespace string can be parsed as URI</li>
     * </ol>
     */
    @Override
    public void enterNamespace_stmt(Namespace_stmtContext ctx) {
        String namespaceName = getName(ctx);
        String rootParentName = getRootParentName(ctx);

        try {
            new URI(namespaceName);
        } catch (URISyntaxException e) {
            throw new YangValidationException(f(
                    "Namespace:%s in module:%s cannot be parsed as URI",
                    namespaceName, rootParentName));
        }
    }

    /**
     * Rules:
     * <ol>
     * <li>Identifier contains only permitted characters</li>
     * <li>Every import(identified by identifier) within a module/submodule is
     * present only once</li>
     * <li>One prefix statement child</li>
     * <li>One revision-date statement child</li>
     * </ol>
     */
    @Override
    public void enterImport_stmt(Import_stmtContext ctx) {
        String importName = getName(ctx);
        String rootParentName = getRootParentName(ctx);

        checkIdentifier(importName,
                f("In (sub)module:%s , Import statement", rootParentName));

        if (uniqueImports.contains(importName))
            throw new YangValidationException(f(
                    "Module:%s imported twice in (sub)module:%s", importName,
                    rootParentName));
        uniqueImports.add(importName);

        checkPresentChildOfType(
                ctx,
                Prefix_stmtContext.class,
                f("Missing prefix statement in import:%s, in (sub)module:%s",
                        importName, rootParentName), true);
        //checkPresentChildOfType(
        //        ctx,
        //        Revision_date_stmtContext.class,
        //        f("Missing revision-date statement in import:%s, in (sub)module:%s",
        //                importName, rootParentName), true);
    }

    /**
     * Rules:
     * <ol>
     * <li>Date is in valid format</li>
     * </ol>
     */
    @Override
    public void enterRevision_date_stmt(Revision_date_stmtContext ctx) {
        String rootParentName = getRootParentName(ctx);
        String exceptionMessage = f(
                "Invalid date format for revision-date:%s in import/include statement:%s, in (sub)module:%s , expected date format is:%s",
                getName(ctx), getRootParentName(ctx), rootParentName,
                YangModelParserListenerImpl.simpleDateFormat.format(new Date()));

        validateDateFormat(getName(ctx),
                YangModelParserListenerImpl.simpleDateFormat, exceptionMessage);
    }

    /**
     * Rules:
     * <ol>
     * <li>Identifier contains only permitted characters</li>
     * <li>Every include(identified by identifier) within a module/submodule is
     * present only once</li>
     * <li>One Revision-date statement child</li>
     * </ol>
     */
    @Override
    public void enterInclude_stmt(Include_stmtContext ctx) {
        String includeName = getName(ctx);
        String rootParentName = getRootParentName(ctx);

        checkIdentifier(includeName,
                f("In (sub)module:%s , Include statement", rootParentName));

        if (uniqueIncludes.contains(includeName))
            throw new YangValidationException(f(
                    "Submodule:%s included twice in (sub)module:%s",
                    includeName, rootParentName));
        uniqueIncludes.add(includeName);

        checkPresentChildOfType(
                ctx,
                Revision_date_stmtContext.class,
                f("Missing revision-date statement in include:%s, in (sub)module:%s",
                        includeName, rootParentName), true);
    }

    static final String SUPPORTED_YANG_VERSION = "1";

    /**
     * Rules:
     * <ol>
     * <li>Yang-version is specified as 1</li>
     * </ol>
     */
    @Override
    public void enterYang_version_stmt(YangParser.Yang_version_stmtContext ctx) {
        String version = getName(ctx);
        String rootParentName = getRootParentName(ctx);
        if (!version.equals(SUPPORTED_YANG_VERSION)) {
            throw new YangValidationException(
                    f("Unsupported yang version:%s, in (sub)module:%s, supported version:%s",
                            version, rootParentName, SUPPORTED_YANG_VERSION));
        }
    }

    /**
     * Rules:
     * <ol>
     * <li>Date is in valid format</li>
     * </ol>
     */
    @Override
    public void enterRevision_stmt(YangParser.Revision_stmtContext ctx) {
        String parentName = getRootParentName(ctx);
        String exceptionMessage = f(
                "Invalid date format for revision:%s in (sub)module:%s, expected date format is:%s",
                getName(ctx), parentName,
                YangModelParserListenerImpl.simpleDateFormat.format(new Date()));

        validateDateFormat(getName(ctx),
                YangModelParserListenerImpl.simpleDateFormat, exceptionMessage);
    }

    /**
     * Rules:
     * <ol>
     * <li>Identifier contains only permitted characters</li>
     * <li>Every prefix(identified by identifier) within a module/submodule is
     * presented only once</li>
     * </ol>
     */
    @Override
    public void enterPrefix_stmt(Prefix_stmtContext ctx) {
        String name = getName(ctx);
        checkIdentifier(
                name,
                f("In module or import statement:%s , Prefix",
                        getRootParentName(ctx)));

        if (uniquePrefixes.contains(name))
            throw new YangValidationException(f(
                    "Not a unique prefix:%s, in (sub)module:%s", name,
                    getRootParentName(ctx)));
        uniquePrefixes.add(name);
    }

    private String getRootParentName(ParseTree ctx) {
        ParseTree root = ctx;
        while (root.getParent() != null) {
            root = root.getParent();
        }
        return getName(root);
    }

    private static String getName(ParseTree child) {
        return YangModelBuilderUtil.stringFromNode(child);
    }

    private static String f(String base, Object... args) {
        return String.format(base, args);
    }

    private static void checkYangVersion(ParseTree ctx, String moduleName) {
        if (!checkPresentChildOfType(ctx, Yang_version_stmtContext.class, true))
            logger.warn(f(
                    "Yang version statement not present in module:%s, Validating as yang version:%s",
                    moduleName, SUPPORTED_YANG_VERSION));
    }

    private static void validateDateFormat(String string, DateFormat format,
            String message) {
        try {
            format.parse(string);
        } catch (ParseException e) {
            throw new YangValidationException(message);
        }
    }

    private static Pattern identifierPattern = Pattern
            .compile("[a-zA-Z_][a-zA-Z0-9_.-]*");

    static void checkIdentifier(String name, String messagePrefix) {
        if (!identifierPattern.matcher(name).matches())
            throw new YangValidationException(f(
                    "%s identifier:%s is not in required format:%s",
                    messagePrefix, name, identifierPattern.toString()));
    }

    private static void checkPresentChildOfType(ParseTree ctx,
            Class<?> expectedChildType, String message, boolean atMostOne) {
        if (!checkPresentChildOfType(ctx, expectedChildType, atMostOne))
            throw new YangValidationException(message);
    }

    private static boolean checkPresentChildOfType(ParseTree ctx,
            Class<?> expectedChildType, boolean atMostOne) {

        int count = 0;

        for (int i = 0; i < ctx.getChildCount(); i++) {
            ParseTree child = ctx.getChild(i);
            if (expectedChildType.isInstance(child))
                count++;
        }

        return atMostOne ? count == 1 ? true : false : count != 0 ? true
                : false;
    }
}
