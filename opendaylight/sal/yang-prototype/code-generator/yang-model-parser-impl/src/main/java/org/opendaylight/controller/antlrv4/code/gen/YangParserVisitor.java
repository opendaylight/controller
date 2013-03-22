/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.antlrv4.code.gen;

import org.antlr.v4.runtime.tree.ParseTreeVisitor;

public interface YangParserVisitor<T> extends ParseTreeVisitor<T> {
    T visitEnum_specification(YangParser.Enum_specificationContext ctx);

    T visitRefine_leaf_list_stmts(YangParser.Refine_leaf_list_stmtsContext ctx);

    T visitPosition_stmt(YangParser.Position_stmtContext ctx);

    T visitArgument_stmt(YangParser.Argument_stmtContext ctx);

    T visitLeafref_specification(YangParser.Leafref_specificationContext ctx);

    T visitError_app_tag_stmt(YangParser.Error_app_tag_stmtContext ctx);

    T visitData_def_stmt(YangParser.Data_def_stmtContext ctx);

    T visitIdentity_stmt(YangParser.Identity_stmtContext ctx);

    T visitDeviate_not_supported_stmt(
            YangParser.Deviate_not_supported_stmtContext ctx);

    T visitPrefix_stmt(YangParser.Prefix_stmtContext ctx);

    T visitMeta_stmts(YangParser.Meta_stmtsContext ctx);

    T visitLinkage_stmts(YangParser.Linkage_stmtsContext ctx);

    T visitGrouping_stmt(YangParser.Grouping_stmtContext ctx);

    T visitFeature_stmt(YangParser.Feature_stmtContext ctx);

    T visitYang(YangParser.YangContext ctx);

    T visitIdentityref_specification(
            YangParser.Identityref_specificationContext ctx);

    T visitNumerical_restrictions(YangParser.Numerical_restrictionsContext ctx);

    T visitModule_header_stmts(YangParser.Module_header_stmtsContext ctx);

    T visitRequire_instance_stmt(YangParser.Require_instance_stmtContext ctx);

    T visitBit_stmt(YangParser.Bit_stmtContext ctx);

    T visitType_stmt(YangParser.Type_stmtContext ctx);

    T visitPattern_stmt(YangParser.Pattern_stmtContext ctx);

    T visitDeviation_stmt(YangParser.Deviation_stmtContext ctx);

    T visitDeviate_replace_stmt(YangParser.Deviate_replace_stmtContext ctx);

    T visitKey_stmt(YangParser.Key_stmtContext ctx);

    T visitRequire_instance_arg(YangParser.Require_instance_argContext ctx);

    T visitLeaf_list_stmt(YangParser.Leaf_list_stmtContext ctx);

    T visitAugment_stmt(YangParser.Augment_stmtContext ctx);

    T visitDeviate_delete_stmt(YangParser.Deviate_delete_stmtContext ctx);

    T visitTypedef_stmt(YangParser.Typedef_stmtContext ctx);

    T visitContainer_stmt(YangParser.Container_stmtContext ctx);

    T visitBelongs_to_stmt(YangParser.Belongs_to_stmtContext ctx);

    T visitBase_stmt(YangParser.Base_stmtContext ctx);

    T visitYang_version_stmt(YangParser.Yang_version_stmtContext ctx);

    T visitReference_stmt(YangParser.Reference_stmtContext ctx);

    T visitYin_element_stmt(YangParser.Yin_element_stmtContext ctx);

    T visitLeaf_stmt(YangParser.Leaf_stmtContext ctx);

    T visitCase_stmt(YangParser.Case_stmtContext ctx);

    T visitModule_stmt(YangParser.Module_stmtContext ctx);

    T visitRpc_stmt(YangParser.Rpc_stmtContext ctx);

    T visitType_body_stmts(YangParser.Type_body_stmtsContext ctx);

    T visitExtension_stmt(YangParser.Extension_stmtContext ctx);

    T visitSubmodule_header_stmts(YangParser.Submodule_header_stmtsContext ctx);

    T visitRefine_container_stmts(YangParser.Refine_container_stmtsContext ctx);

    T visitValue_stmt(YangParser.Value_stmtContext ctx);

    T visitRefine_list_stmts(YangParser.Refine_list_stmtsContext ctx);

    T visitDefault_stmt(YangParser.Default_stmtContext ctx);

    T visitRevision_stmts(YangParser.Revision_stmtsContext ctx);

    T visitAnyxml_stmt(YangParser.Anyxml_stmtContext ctx);

    T visitStatus_stmt(YangParser.Status_stmtContext ctx);

    T visitContact_stmt(YangParser.Contact_stmtContext ctx);

    T visitIdentifier_stmt(YangParser.Identifier_stmtContext ctx);

    T visitString(YangParser.StringContext ctx);

    T visitRefine_choice_stmts(YangParser.Refine_choice_stmtsContext ctx);

    T visitMandatory_arg(YangParser.Mandatory_argContext ctx);

    T visitRefine_stmt(YangParser.Refine_stmtContext ctx);

    T visitInstance_identifier_specification(
            YangParser.Instance_identifier_specificationContext ctx);

    T visitBits_specification(YangParser.Bits_specificationContext ctx);

    T visitWhen_stmt(YangParser.When_stmtContext ctx);

    T visitString_restrictions(YangParser.String_restrictionsContext ctx);

    T visitRefine_leaf_stmts(YangParser.Refine_leaf_stmtsContext ctx);

    T visitMandatory_stmt(YangParser.Mandatory_stmtContext ctx);

    T visitOrdered_by_arg(YangParser.Ordered_by_argContext ctx);

    T visitMin_elements_stmt(YangParser.Min_elements_stmtContext ctx);

    T visitStmtend(YangParser.StmtendContext ctx);

    T visitRefine_anyxml_stmts(YangParser.Refine_anyxml_stmtsContext ctx);

    T visitDescription_stmt(YangParser.Description_stmtContext ctx);

    T visitPath_stmt(YangParser.Path_stmtContext ctx);

    T visitInclude_stmt(YangParser.Include_stmtContext ctx);

    T visitUnits_stmt(YangParser.Units_stmtContext ctx);

    T visitUses_stmt(YangParser.Uses_stmtContext ctx);

    T visitOrdered_by_stmt(YangParser.Ordered_by_stmtContext ctx);

    T visitRange_stmt(YangParser.Range_stmtContext ctx);

    T visitNamespace_stmt(YangParser.Namespace_stmtContext ctx);

    T visitDeviate_add_stmt(YangParser.Deviate_add_stmtContext ctx);

    T visitShort_case_stmt(YangParser.Short_case_stmtContext ctx);

    T visitConfig_stmt(YangParser.Config_stmtContext ctx);

    T visitEnum_stmt(YangParser.Enum_stmtContext ctx);

    T visitYin_element_arg(YangParser.Yin_element_argContext ctx);

    T visitOrganization_stmt(YangParser.Organization_stmtContext ctx);

    T visitUnion_specification(YangParser.Union_specificationContext ctx);

    T visitMax_value_arg(YangParser.Max_value_argContext ctx);

    T visitSubmodule_stmt(YangParser.Submodule_stmtContext ctx);

    T visitStatus_arg(YangParser.Status_argContext ctx);

    T visitList_stmt(YangParser.List_stmtContext ctx);

    T visitMax_elements_stmt(YangParser.Max_elements_stmtContext ctx);

    T visitImport_stmt(YangParser.Import_stmtContext ctx);

    T visitConfig_arg(YangParser.Config_argContext ctx);

    T visitRevision_date_stmt(YangParser.Revision_date_stmtContext ctx);

    T visitRefune_pom(YangParser.Refune_pomContext ctx);

    T visitPresence_stmt(YangParser.Presence_stmtContext ctx);

    T visitFraction_digits_stmt(YangParser.Fraction_digits_stmtContext ctx);

    T visitNotification_stmt(YangParser.Notification_stmtContext ctx);

    T visitInput_stmt(YangParser.Input_stmtContext ctx);

    T visitUses_augment_stmt(YangParser.Uses_augment_stmtContext ctx);

    T visitRefine_case_stmts(YangParser.Refine_case_stmtsContext ctx);

    T visitDecimal64_specification(YangParser.Decimal64_specificationContext ctx);

    T visitIf_feature_stmt(YangParser.If_feature_stmtContext ctx);

    T visitRevision_stmt(YangParser.Revision_stmtContext ctx);

    T visitLength_stmt(YangParser.Length_stmtContext ctx);

    T visitMust_stmt(YangParser.Must_stmtContext ctx);

    T visitBody_stmts(YangParser.Body_stmtsContext ctx);

    T visitError_message_stmt(YangParser.Error_message_stmtContext ctx);

    T visitUnique_stmt(YangParser.Unique_stmtContext ctx);

    T visitChoice_stmt(YangParser.Choice_stmtContext ctx);

    T visitOutput_stmt(YangParser.Output_stmtContext ctx);
}