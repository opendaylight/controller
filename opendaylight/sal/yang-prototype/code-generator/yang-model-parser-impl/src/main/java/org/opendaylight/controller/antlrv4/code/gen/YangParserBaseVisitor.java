/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.antlrv4.code.gen;

import org.antlr.v4.runtime.tree.AbstractParseTreeVisitor;

public class YangParserBaseVisitor<T> extends AbstractParseTreeVisitor<T>
        implements YangParserVisitor<T> {
    @Override
    public T visitEnum_specification(YangParser.Enum_specificationContext ctx) {
        return visitChildren(ctx);
    }

    @Override
    public T visitRefine_leaf_list_stmts(
            YangParser.Refine_leaf_list_stmtsContext ctx) {
        return visitChildren(ctx);
    }

    @Override
    public T visitPosition_stmt(YangParser.Position_stmtContext ctx) {
        return visitChildren(ctx);
    }

    @Override
    public T visitArgument_stmt(YangParser.Argument_stmtContext ctx) {
        return visitChildren(ctx);
    }

    @Override
    public T visitLeafref_specification(
            YangParser.Leafref_specificationContext ctx) {
        return visitChildren(ctx);
    }

    @Override
    public T visitError_app_tag_stmt(YangParser.Error_app_tag_stmtContext ctx) {
        return visitChildren(ctx);
    }

    @Override
    public T visitData_def_stmt(YangParser.Data_def_stmtContext ctx) {
        return visitChildren(ctx);
    }

    @Override
    public T visitIdentity_stmt(YangParser.Identity_stmtContext ctx) {
        return visitChildren(ctx);
    }

    @Override
    public T visitDeviate_not_supported_stmt(
            YangParser.Deviate_not_supported_stmtContext ctx) {
        return visitChildren(ctx);
    }

    @Override
    public T visitPrefix_stmt(YangParser.Prefix_stmtContext ctx) {
        return visitChildren(ctx);
    }

    @Override
    public T visitMeta_stmts(YangParser.Meta_stmtsContext ctx) {
        return visitChildren(ctx);
    }

    @Override
    public T visitLinkage_stmts(YangParser.Linkage_stmtsContext ctx) {
        return visitChildren(ctx);
    }

    @Override
    public T visitGrouping_stmt(YangParser.Grouping_stmtContext ctx) {
        return visitChildren(ctx);
    }

    @Override
    public T visitFeature_stmt(YangParser.Feature_stmtContext ctx) {
        return visitChildren(ctx);
    }

    @Override
    public T visitYang(YangParser.YangContext ctx) {
        return visitChildren(ctx);
    }

    @Override
    public T visitIdentityref_specification(
            YangParser.Identityref_specificationContext ctx) {
        return visitChildren(ctx);
    }

    @Override
    public T visitNumerical_restrictions(
            YangParser.Numerical_restrictionsContext ctx) {
        return visitChildren(ctx);
    }

    @Override
    public T visitModule_header_stmts(YangParser.Module_header_stmtsContext ctx) {
        return visitChildren(ctx);
    }

    @Override
    public T visitRequire_instance_stmt(
            YangParser.Require_instance_stmtContext ctx) {
        return visitChildren(ctx);
    }

    @Override
    public T visitBit_stmt(YangParser.Bit_stmtContext ctx) {
        return visitChildren(ctx);
    }

    @Override
    public T visitType_stmt(YangParser.Type_stmtContext ctx) {
        return visitChildren(ctx);
    }

    @Override
    public T visitPattern_stmt(YangParser.Pattern_stmtContext ctx) {
        return visitChildren(ctx);
    }

    @Override
    public T visitDeviation_stmt(YangParser.Deviation_stmtContext ctx) {
        return visitChildren(ctx);
    }

    @Override
    public T visitDeviate_replace_stmt(
            YangParser.Deviate_replace_stmtContext ctx) {
        return visitChildren(ctx);
    }

    @Override
    public T visitKey_stmt(YangParser.Key_stmtContext ctx) {
        return visitChildren(ctx);
    }

    @Override
    public T visitRequire_instance_arg(
            YangParser.Require_instance_argContext ctx) {
        return visitChildren(ctx);
    }

    @Override
    public T visitLeaf_list_stmt(YangParser.Leaf_list_stmtContext ctx) {
        return visitChildren(ctx);
    }

    @Override
    public T visitAugment_stmt(YangParser.Augment_stmtContext ctx) {
        return visitChildren(ctx);
    }

    @Override
    public T visitDeviate_delete_stmt(YangParser.Deviate_delete_stmtContext ctx) {
        return visitChildren(ctx);
    }

    @Override
    public T visitTypedef_stmt(YangParser.Typedef_stmtContext ctx) {
        return visitChildren(ctx);
    }

    @Override
    public T visitContainer_stmt(YangParser.Container_stmtContext ctx) {
        return visitChildren(ctx);
    }

    @Override
    public T visitBelongs_to_stmt(YangParser.Belongs_to_stmtContext ctx) {
        return visitChildren(ctx);
    }

    @Override
    public T visitBase_stmt(YangParser.Base_stmtContext ctx) {
        return visitChildren(ctx);
    }

    @Override
    public T visitYang_version_stmt(YangParser.Yang_version_stmtContext ctx) {
        return visitChildren(ctx);
    }

    @Override
    public T visitReference_stmt(YangParser.Reference_stmtContext ctx) {
        return visitChildren(ctx);
    }

    @Override
    public T visitYin_element_stmt(YangParser.Yin_element_stmtContext ctx) {
        return visitChildren(ctx);
    }

    @Override
    public T visitLeaf_stmt(YangParser.Leaf_stmtContext ctx) {
        return visitChildren(ctx);
    }

    @Override
    public T visitCase_stmt(YangParser.Case_stmtContext ctx) {
        return visitChildren(ctx);
    }

    @Override
    public T visitModule_stmt(YangParser.Module_stmtContext ctx) {
        return visitChildren(ctx);
    }

    @Override
    public T visitRpc_stmt(YangParser.Rpc_stmtContext ctx) {
        return visitChildren(ctx);
    }

    @Override
    public T visitType_body_stmts(YangParser.Type_body_stmtsContext ctx) {
        return visitChildren(ctx);
    }

    @Override
    public T visitExtension_stmt(YangParser.Extension_stmtContext ctx) {
        return visitChildren(ctx);
    }

    @Override
    public T visitSubmodule_header_stmts(
            YangParser.Submodule_header_stmtsContext ctx) {
        return visitChildren(ctx);
    }

    @Override
    public T visitRefine_container_stmts(
            YangParser.Refine_container_stmtsContext ctx) {
        return visitChildren(ctx);
    }

    @Override
    public T visitValue_stmt(YangParser.Value_stmtContext ctx) {
        return visitChildren(ctx);
    }

    @Override
    public T visitRefine_list_stmts(YangParser.Refine_list_stmtsContext ctx) {
        return visitChildren(ctx);
    }

    @Override
    public T visitDefault_stmt(YangParser.Default_stmtContext ctx) {
        return visitChildren(ctx);
    }

    @Override
    public T visitRevision_stmts(YangParser.Revision_stmtsContext ctx) {
        return visitChildren(ctx);
    }

    @Override
    public T visitAnyxml_stmt(YangParser.Anyxml_stmtContext ctx) {
        return visitChildren(ctx);
    }

    @Override
    public T visitStatus_stmt(YangParser.Status_stmtContext ctx) {
        return visitChildren(ctx);
    }

    @Override
    public T visitContact_stmt(YangParser.Contact_stmtContext ctx) {
        return visitChildren(ctx);
    }

    @Override
    public T visitIdentifier_stmt(YangParser.Identifier_stmtContext ctx) {
        return visitChildren(ctx);
    }

    @Override
    public T visitString(YangParser.StringContext ctx) {
        return visitChildren(ctx);
    }

    @Override
    public T visitRefine_choice_stmts(YangParser.Refine_choice_stmtsContext ctx) {
        return visitChildren(ctx);
    }

    @Override
    public T visitMandatory_arg(YangParser.Mandatory_argContext ctx) {
        return visitChildren(ctx);
    }

    @Override
    public T visitRefine_stmt(YangParser.Refine_stmtContext ctx) {
        return visitChildren(ctx);
    }

    @Override
    public T visitInstance_identifier_specification(
            YangParser.Instance_identifier_specificationContext ctx) {
        return visitChildren(ctx);
    }

    @Override
    public T visitBits_specification(YangParser.Bits_specificationContext ctx) {
        return visitChildren(ctx);
    }

    @Override
    public T visitWhen_stmt(YangParser.When_stmtContext ctx) {
        return visitChildren(ctx);
    }

    @Override
    public T visitString_restrictions(YangParser.String_restrictionsContext ctx) {
        return visitChildren(ctx);
    }

    @Override
    public T visitRefine_leaf_stmts(YangParser.Refine_leaf_stmtsContext ctx) {
        return visitChildren(ctx);
    }

    @Override
    public T visitMandatory_stmt(YangParser.Mandatory_stmtContext ctx) {
        return visitChildren(ctx);
    }

    @Override
    public T visitOrdered_by_arg(YangParser.Ordered_by_argContext ctx) {
        return visitChildren(ctx);
    }

    @Override
    public T visitMin_elements_stmt(YangParser.Min_elements_stmtContext ctx) {
        return visitChildren(ctx);
    }

    @Override
    public T visitStmtend(YangParser.StmtendContext ctx) {
        return visitChildren(ctx);
    }

    @Override
    public T visitRefine_anyxml_stmts(YangParser.Refine_anyxml_stmtsContext ctx) {
        return visitChildren(ctx);
    }

    @Override
    public T visitDescription_stmt(YangParser.Description_stmtContext ctx) {
        return visitChildren(ctx);
    }

    @Override
    public T visitPath_stmt(YangParser.Path_stmtContext ctx) {
        return visitChildren(ctx);
    }

    @Override
    public T visitInclude_stmt(YangParser.Include_stmtContext ctx) {
        return visitChildren(ctx);
    }

    @Override
    public T visitUnits_stmt(YangParser.Units_stmtContext ctx) {
        return visitChildren(ctx);
    }

    @Override
    public T visitUses_stmt(YangParser.Uses_stmtContext ctx) {
        return visitChildren(ctx);
    }

    @Override
    public T visitOrdered_by_stmt(YangParser.Ordered_by_stmtContext ctx) {
        return visitChildren(ctx);
    }

    @Override
    public T visitRange_stmt(YangParser.Range_stmtContext ctx) {
        return visitChildren(ctx);
    }

    @Override
    public T visitNamespace_stmt(YangParser.Namespace_stmtContext ctx) {
        return visitChildren(ctx);
    }

    @Override
    public T visitDeviate_add_stmt(YangParser.Deviate_add_stmtContext ctx) {
        return visitChildren(ctx);
    }

    @Override
    public T visitShort_case_stmt(YangParser.Short_case_stmtContext ctx) {
        return visitChildren(ctx);
    }

    @Override
    public T visitConfig_stmt(YangParser.Config_stmtContext ctx) {
        return visitChildren(ctx);
    }

    @Override
    public T visitEnum_stmt(YangParser.Enum_stmtContext ctx) {
        return visitChildren(ctx);
    }

    @Override
    public T visitYin_element_arg(YangParser.Yin_element_argContext ctx) {
        return visitChildren(ctx);
    }

    @Override
    public T visitOrganization_stmt(YangParser.Organization_stmtContext ctx) {
        return visitChildren(ctx);
    }

    @Override
    public T visitUnion_specification(YangParser.Union_specificationContext ctx) {
        return visitChildren(ctx);
    }

    @Override
    public T visitMax_value_arg(YangParser.Max_value_argContext ctx) {
        return visitChildren(ctx);
    }

    @Override
    public T visitSubmodule_stmt(YangParser.Submodule_stmtContext ctx) {
        return visitChildren(ctx);
    }

    @Override
    public T visitStatus_arg(YangParser.Status_argContext ctx) {
        return visitChildren(ctx);
    }

    @Override
    public T visitList_stmt(YangParser.List_stmtContext ctx) {
        return visitChildren(ctx);
    }

    @Override
    public T visitMax_elements_stmt(YangParser.Max_elements_stmtContext ctx) {
        return visitChildren(ctx);
    }

    @Override
    public T visitImport_stmt(YangParser.Import_stmtContext ctx) {
        return visitChildren(ctx);
    }

    @Override
    public T visitConfig_arg(YangParser.Config_argContext ctx) {
        return visitChildren(ctx);
    }

    @Override
    public T visitRevision_date_stmt(YangParser.Revision_date_stmtContext ctx) {
        return visitChildren(ctx);
    }

    @Override
    public T visitRefune_pom(YangParser.Refune_pomContext ctx) {
        return visitChildren(ctx);
    }

    @Override
    public T visitPresence_stmt(YangParser.Presence_stmtContext ctx) {
        return visitChildren(ctx);
    }

    @Override
    public T visitFraction_digits_stmt(
            YangParser.Fraction_digits_stmtContext ctx) {
        return visitChildren(ctx);
    }

    @Override
    public T visitNotification_stmt(YangParser.Notification_stmtContext ctx) {
        return visitChildren(ctx);
    }

    @Override
    public T visitInput_stmt(YangParser.Input_stmtContext ctx) {
        return visitChildren(ctx);
    }

    @Override
    public T visitUses_augment_stmt(YangParser.Uses_augment_stmtContext ctx) {
        return visitChildren(ctx);
    }

    @Override
    public T visitRefine_case_stmts(YangParser.Refine_case_stmtsContext ctx) {
        return visitChildren(ctx);
    }

    @Override
    public T visitDecimal64_specification(
            YangParser.Decimal64_specificationContext ctx) {
        return visitChildren(ctx);
    }

    @Override
    public T visitIf_feature_stmt(YangParser.If_feature_stmtContext ctx) {
        return visitChildren(ctx);
    }

    @Override
    public T visitRevision_stmt(YangParser.Revision_stmtContext ctx) {
        return visitChildren(ctx);
    }

    @Override
    public T visitLength_stmt(YangParser.Length_stmtContext ctx) {
        return visitChildren(ctx);
    }

    @Override
    public T visitMust_stmt(YangParser.Must_stmtContext ctx) {
        return visitChildren(ctx);
    }

    @Override
    public T visitBody_stmts(YangParser.Body_stmtsContext ctx) {
        return visitChildren(ctx);
    }

    @Override
    public T visitError_message_stmt(YangParser.Error_message_stmtContext ctx) {
        return visitChildren(ctx);
    }

    @Override
    public T visitUnique_stmt(YangParser.Unique_stmtContext ctx) {
        return visitChildren(ctx);
    }

    @Override
    public T visitChoice_stmt(YangParser.Choice_stmtContext ctx) {
        return visitChildren(ctx);
    }

    @Override
    public T visitOutput_stmt(YangParser.Output_stmtContext ctx) {
        return visitChildren(ctx);
    }
}