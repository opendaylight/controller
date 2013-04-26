/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.yang.model.validator;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Set;

import org.antlr.v4.runtime.tree.ParseTree;
import org.opendaylight.controller.antlrv4.code.gen.YangParser;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Anyxml_stmtContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Argument_stmtContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Augment_stmtContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Base_stmtContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Belongs_to_stmtContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Case_stmtContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Choice_stmtContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Config_argContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Container_stmtContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Default_stmtContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Deviate_add_stmtContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Deviation_stmtContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Extension_stmtContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Feature_stmtContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Grouping_stmtContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Identity_stmtContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.If_feature_stmtContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Import_stmtContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Include_stmtContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Key_stmtContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Leaf_list_stmtContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Leaf_stmtContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.List_stmtContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Mandatory_argContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Mandatory_stmtContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Module_header_stmtsContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Module_stmtContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Namespace_stmtContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Notification_stmtContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Ordered_by_argContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Prefix_stmtContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Refine_stmtContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Revision_date_stmtContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Revision_stmtContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Rpc_stmtContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Status_argContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Submodule_header_stmtsContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Submodule_stmtContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Type_stmtContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Typedef_stmtContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Unique_stmtContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Uses_stmtContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Yin_element_argContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParserBaseListener;
import org.opendaylight.controller.yang.model.parser.impl.YangModelParserListenerImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;

/**
 * Validation listener that validates yang statements according to RFC-6020.
 * This validator expects only one module or submodule per file and performs
 * only basic validation where context from all yang models is not present.
 */
final class YangModelBasicValidationListener extends YangParserBaseListener {

    private static final Logger logger = LoggerFactory
            .getLogger(YangModelBasicValidationListener.class);

    private final Set<String> uniquePrefixes;
    private final Set<String> uniqueImports;
    private final Set<String> uniqueIncludes;

    private String globalModuleId;

    YangModelBasicValidationListener() {
        super();
        uniquePrefixes = Sets.newHashSet();
        uniqueImports = Sets.newHashSet();
        uniqueIncludes = Sets.newHashSet();
    }

    /**
     * Constraints:
     * <ol>
     * <li>Identifier is in required format</li>
     * <li>Header statements present(mandatory prefix and namespace statements
     * are in header)</li>
     * <li>Only one module or submodule per file</li>
     * </ol>
     */
    @Override
    public void enterModule_stmt(Module_stmtContext ctx) {

        BasicValidations.checkIdentifier(ctx);

        BasicValidations.checkPresentChildOfType(ctx,
                Module_header_stmtsContext.class, true);

        String moduleName = ValidationUtil.getName(ctx);
        BasicValidations.checkOnlyOneModulePresent(moduleName, globalModuleId);
        globalModuleId = moduleName;
    }

    /**
     * Constraints:
     * <ol>
     * <li>Identifier is in required format</li>
     * <li>Header statements present(mandatory belongs-to statement is in
     * header)</li>
     * <li>Only one module or submodule per file</li>
     * </ol>
     */
    @Override
    public void enterSubmodule_stmt(Submodule_stmtContext ctx) {

        BasicValidations.checkIdentifier(ctx);

        BasicValidations.checkPresentChildOfType(ctx,
                Submodule_header_stmtsContext.class, true);

        String submoduleName = ValidationUtil.getName(ctx);
        BasicValidations.checkOnlyOneModulePresent(submoduleName,
                globalModuleId);
        globalModuleId = submoduleName;

    }

    /**
     * Constraints:
     * <ol>
     * <li>One Belongs-to statement present</li>
     * </ol>
     */
    @Override
    public void enterSubmodule_header_stmts(Submodule_header_stmtsContext ctx) {
        BasicValidations.checkPresentChildOfType(ctx,
                Belongs_to_stmtContext.class, true);

        // check Yang version present, if not log
        try {
            BasicValidations.checkPresentYangVersion(ctx,
                    ValidationUtil.getRootParentName(ctx));
        } catch (Exception e) {
            logger.debug(e.getMessage());
        }
    }

    /**
     * Constraints:
     * <ol>
     * <li>One Namespace statement present</li>
     * <li>One Prefix statement present</li>
     * </ol>
     */
    @Override
    public void enterModule_header_stmts(Module_header_stmtsContext ctx) {
        String moduleName = ValidationUtil.getRootParentName(ctx);

        BasicValidations.checkPresentChildOfType(ctx,
                Namespace_stmtContext.class, true);
        BasicValidations.checkPresentChildOfType(ctx, Prefix_stmtContext.class,
                true);

        // check Yang version present, if not log
        try {
            BasicValidations.checkPresentYangVersion(ctx, moduleName);
        } catch (Exception e) {
            logger.debug(e.getMessage());
        }
    }

    /**
     * Constraints:
     * <ol>
     * <li>Date is in valid format</li>
     * </ol>
     */
    @Override
    public void enterRevision_stmt(Revision_stmtContext ctx) {
        BasicValidations.checkDateFormat(ctx,
                YangModelParserListenerImpl.simpleDateFormat);

    }

    /**
     * Constraints:
     * <ol>
     * <li>Identifier is in required format</li>
     * <li>One Prefix statement child</li>
     * </ol>
     */
    @Override
    public void enterBelongs_to_stmt(Belongs_to_stmtContext ctx) {
        BasicValidations.checkIdentifier(ctx);

        BasicValidations.checkPresentChildOfType(ctx, Prefix_stmtContext.class,
                true);
    }

    /**
     * Constraints:
     * <ol>
     * <li>Namespace string can be parsed as URI</li>
     * </ol>
     */
    @Override
    public void enterNamespace_stmt(Namespace_stmtContext ctx) {
        String namespaceName = ValidationUtil.getName(ctx);
        String rootParentName = ValidationUtil.getRootParentName(ctx);

        try {
            new URI(namespaceName);
        } catch (URISyntaxException e) {
            ValidationUtil.ex(ValidationUtil.f(
                    "(In module:%s) Namespace:%s cannot be parsed as URI",
                    rootParentName, namespaceName));
        }
    }

    /**
     * Constraints:
     * <ol>
     * <li>Identifier is in required format</li>
     * <li>Every import(identified by identifier) within a module/submodule is
     * present only once</li>
     * <li>One prefix statement child</li>
     * </ol>
     */
    @Override
    public void enterImport_stmt(Import_stmtContext ctx) {

        BasicValidations.checkIdentifier(ctx);

        BasicValidations.checkUniquenessInNamespace(ctx, uniqueImports);

        BasicValidations.checkPresentChildOfType(ctx, Prefix_stmtContext.class,
                true);

    }

    /**
     * Constraints:
     * <ol>
     * <li>Date is in valid format</li>
     * </ol>
     */
    @Override
    public void enterRevision_date_stmt(Revision_date_stmtContext ctx) {
        BasicValidations.checkDateFormat(ctx,
                YangModelParserListenerImpl.simpleDateFormat);
    }

    /**
     * Constraints:
     * <ol>
     * <li>Identifier is in required format</li>
     * <li>Every include(identified by identifier) within a module/submodule is
     * present only once</li>
     * </ol>
     */
    @Override
    public void enterInclude_stmt(Include_stmtContext ctx) {

        BasicValidations.checkIdentifier(ctx);

        BasicValidations.checkUniquenessInNamespace(ctx, uniqueIncludes);
    }

    /**
     * Constraints:
     * <ol>
     * <li>Yang-version is specified as 1</li>
     * </ol>
     */
    @Override
    public void enterYang_version_stmt(YangParser.Yang_version_stmtContext ctx) {
        String version = ValidationUtil.getName(ctx);
        String rootParentName = ValidationUtil.getRootParentName(ctx);
        if (!version.equals(BasicValidations.SUPPORTED_YANG_VERSION)) {
            ValidationUtil
                    .ex(ValidationUtil
                            .f("(In (sub)module:%s) Unsupported yang version:%s, supported version:%s",
                                    rootParentName, version,
                                    BasicValidations.SUPPORTED_YANG_VERSION));
        }
    }

    /**
     * Constraints:
     * <ol>
     * <li>Identifier is in required format</li>
     * <li>Every prefix(identified by identifier) within a module/submodule is
     * presented only once</li>
     * </ol>
     */
    @Override
    public void enterPrefix_stmt(Prefix_stmtContext ctx) {

        BasicValidations.checkIdentifier(ctx);

        BasicValidations.checkUniquenessInNamespace(ctx, uniquePrefixes);
    }

    /**
     * Constraints:
     * <ol>
     * <li>Identifier is in required format</li>
     * <li>One type statement child</li>
     * </ol>
     */
    @Override
    public void enterTypedef_stmt(Typedef_stmtContext ctx) {

        BasicValidations.checkIdentifier(ctx);

        BasicValidations.checkPresentChildOfType(ctx, Type_stmtContext.class,
                true);
    }

    /**
     * Constraints:
     * <ol>
     * <li>(Prefix):Identifier is in required format</li>
     * </ol>
     */
    @Override
    public void enterType_stmt(Type_stmtContext ctx) {
        BasicValidations.checkPrefixedIdentifier(ctx);
    }

    /**
     * Constraints:
     * <ol>
     * <li>Identifier is in required format</li>
     * </ol>
     */
    @Override
    public void enterContainer_stmt(Container_stmtContext ctx) {
        BasicValidations.checkIdentifier(ctx);
    }

    /**
     * Constraints:
     * <ol>
     * <li>Identifier is in required format</li>
     * <li>One type statement child</li>
     * <li>Default statement must not be present if mandatory statement is</li>
     * </ol>
     */
    @Override
    public void enterLeaf_stmt(Leaf_stmtContext ctx) {
        BasicValidations.checkIdentifier(ctx);

        BasicValidations.checkPresentChildOfType(ctx, Type_stmtContext.class,
                true);

        BasicValidations.checkNotPresentBoth(ctx, Mandatory_stmtContext.class,
                Default_stmtContext.class);
    }

    /**
     * Constraints:
     * <ol>
     * <li>Identifier is in required format</li>
     * <li>One type statement child</li>
     * </ol>
     */
    @Override
    public void enterLeaf_list_stmt(Leaf_list_stmtContext ctx) {

        BasicValidations.checkIdentifier(ctx);

        BasicValidations.checkPresentChildOfType(ctx, Type_stmtContext.class,
                true);
    }

    private static final Set<String> permittedOrderByArgs = Sets.newHashSet(
            "system", "user");

    /**
     * Constraints:
     * <ol>
     * <li>Value must be one of: system, user</li>
     * </ol>
     */
    @Override
    public void enterOrdered_by_arg(Ordered_by_argContext ctx) {
        BasicValidations.checkOnlyPermittedValues(ctx, permittedOrderByArgs);
    }

    /**
     * Constraints:
     * <ol>
     * <li>Identifier is in required format</li>
     * </ol>
     */
    @Override
    public void enterList_stmt(List_stmtContext ctx) {
        BasicValidations.checkIdentifier(ctx);
        // TODO check: "if config==true then key must be present" could be
        // performed
    }

    /**
     * Constraints:
     * <ol>
     * <li>No duplicate keys</li>
     * </ol>
     */
    @Override
    public void enterKey_stmt(Key_stmtContext ctx) {
        BasicValidations.getAndCheckUniqueKeys(ctx);
    }

    /**
     * Constraints:
     * <ol>
     * <liNo duplicate uniques</li>
     * </ol>
     */
    @Override
    public void enterUnique_stmt(Unique_stmtContext ctx) {
        BasicValidations.getAndCheckUniqueKeys(ctx);
    }

    /**
     * Constraints:
     * <ol>
     * <li>Identifier is in required format</li>
     * <li>Default statement must not be present if mandatory statement is</li>
     * </ol>
     */
    @Override
    public void enterChoice_stmt(Choice_stmtContext ctx) {
        BasicValidations.checkIdentifier(ctx);

        BasicValidations.checkNotPresentBoth(ctx, Mandatory_stmtContext.class,
                Default_stmtContext.class);

    }

    /**
     * Constraints:
     * <ol>
     * <li>Identifier is in required format</li>
     * </ol>
     */
    @Override
    public void enterCase_stmt(Case_stmtContext ctx) {
        BasicValidations.checkIdentifier(ctx);
    }

    private static final Set<String> permittedBooleanArgs = Sets.newHashSet(
            "true", "false");

    /**
     * Constraints:
     * <ol>
     * <li>Value must be one of: true, false</li>
     * </ol>
     */
    @Override
    public void enterMandatory_arg(Mandatory_argContext ctx) {
        BasicValidations.checkOnlyPermittedValues(ctx, permittedBooleanArgs);
    }

    /**
     * Constraints:
     * <ol>
     * <li>Identifier is in required format</li>
     * </ol>
     */
    @Override
    public void enterAnyxml_stmt(Anyxml_stmtContext ctx) {
        BasicValidations.checkIdentifier(ctx);
    }

    /**
     * Constraints:
     * <ol>
     * <li>Identifier is in required format</li>
     * </ol>
     */
    @Override
    public void enterGrouping_stmt(Grouping_stmtContext ctx) {
        BasicValidations.checkIdentifier(ctx);
    }

    /**
     * Constraints:
     * <ol>
     * <li>(Prefix):Identifier is in required format</li>
     * </ol>
     */
    @Override
    public void enterUses_stmt(Uses_stmtContext ctx) {
        BasicValidations.checkPrefixedIdentifier(ctx);
    }

    /**
     * Constraints:
     * <ol>
     * <li>Identifier is in required format</li>
     * </ol>
     */
    @Override
    public void enterRefine_stmt(Refine_stmtContext ctx) {
        BasicValidations.checkIdentifier(ctx);
    }

    /**
     * Constraints:
     * <ol>
     * <li>Identifier is in required format</li>
     * </ol>
     */
    @Override
    public void enterRpc_stmt(Rpc_stmtContext ctx) {
        BasicValidations.checkIdentifier(ctx);
    }

    /**
     * Constraints:
     * <ol>
     * <li>Identifier is in required format</li>
     * </ol>
     */
    @Override
    public void enterNotification_stmt(Notification_stmtContext ctx) {
        BasicValidations.checkIdentifier(ctx);
    }

    /**
     * Constraints:
     * <ol>
     * <li>Schema Node Identifier is in required format</li>
     * </ol>
     */
    @Override
    public void enterAugment_stmt(Augment_stmtContext ctx) {
        BasicValidations.checkSchemaNodeIdentifier(ctx);
    }

    /**
     * Constraints:
     * <ol>
     * <li>Identifier is in required format</li>
     * </ol>
     */
    @Override
    public void enterIdentity_stmt(Identity_stmtContext ctx) {
        BasicValidations.checkIdentifier(ctx);
    }

    /**
     * Constraints:
     * <ol>
     * <li>(Prefix):Identifier is in required format</li>
     * </ol>
     */
    @Override
    public void enterBase_stmt(Base_stmtContext ctx) {
        BasicValidations.checkPrefixedIdentifier(ctx);

    }

    /**
     * Constraints:
     * <ol>
     * <li>Value must be one of: true, false</li>
     * </ol>
     */
    @Override
    public void enterYin_element_arg(Yin_element_argContext ctx) {
        BasicValidations.checkOnlyPermittedValues(ctx, permittedBooleanArgs);
    }

    /**
     * Constraints:
     * <ol>
     * <li>Identifier is in required format</li>
     * </ol>
     */
    @Override
    public void enterExtension_stmt(Extension_stmtContext ctx) {
        BasicValidations.checkIdentifier(ctx);
    }

    /**
     * Constraints:
     * <ol>
     * <li>Identifier is in required format</li>
     * </ol>
     */
    @Override
    public void enterArgument_stmt(Argument_stmtContext ctx) {
        BasicValidations.checkIdentifier(ctx);
    }

    /**
     * Constraints:
     * <ol>
     * <li>Identifier is in required format</li>
     * </ol>
     */
    @Override
    public void enterFeature_stmt(Feature_stmtContext ctx) {
        BasicValidations.checkIdentifier(ctx);

    }

    /**
     * Constraints:
     * <ol>
     * <li>(Prefix):Identifier is in required format</li>
     * </ol>
     */
    @Override
    public void enterIf_feature_stmt(If_feature_stmtContext ctx) {
        BasicValidations.checkPrefixedIdentifier(ctx);
    }

    /**
     * Constraints:
     * <ol>
     * <li>Schema Node Identifier is in required format</li>
     * <li>At least one deviate-* statement child</li>
     * </ol>
     */
    @Override
    public void enterDeviation_stmt(Deviation_stmtContext ctx) {
        BasicValidations.checkSchemaNodeIdentifier(ctx);

        Set<Class<? extends ParseTree>> types = Sets.newHashSet();
        types.add(Deviate_add_stmtContext.class);
        types.add(Deviate_add_stmtContext.class);
        BasicValidations.checkPresentChildOfTypes(ctx, types, false);
    }

    /**
     * Constraints:
     * <ol>
     * <li>Value must be one of: true, false</li>
     * </ol>
     */
    @Override
    public void enterConfig_arg(Config_argContext ctx) {
        BasicValidations.checkOnlyPermittedValues(ctx, permittedBooleanArgs);
    }

    private static final Set<String> permittedStatusArgs = Sets.newHashSet(
            "current", "deprecated", "obsolete");

    /**
     * Constraints:
     * <ol>
     * <li>Value must be one of: "current", "deprecated", "obsolete"</li>
     * </ol>
     */
    @Override
    public void enterStatus_arg(Status_argContext ctx) {
        BasicValidations.checkOnlyPermittedValues(ctx, permittedStatusArgs);
    }

}
