/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.antlrv4.code.gen;

import org.antlr.v4.runtime.tree.ParseTreeListener;

public interface YangParserListener extends ParseTreeListener {
    void enterEnum_specification(YangParser.Enum_specificationContext ctx);

    void exitEnum_specification(YangParser.Enum_specificationContext ctx);

    void enterRefine_leaf_list_stmts(
            YangParser.Refine_leaf_list_stmtsContext ctx);

    void exitRefine_leaf_list_stmts(YangParser.Refine_leaf_list_stmtsContext ctx);

    void enterPosition_stmt(YangParser.Position_stmtContext ctx);

    void exitPosition_stmt(YangParser.Position_stmtContext ctx);

    void enterArgument_stmt(YangParser.Argument_stmtContext ctx);

    void exitArgument_stmt(YangParser.Argument_stmtContext ctx);

    void enterLeafref_specification(YangParser.Leafref_specificationContext ctx);

    void exitLeafref_specification(YangParser.Leafref_specificationContext ctx);

    void enterError_app_tag_stmt(YangParser.Error_app_tag_stmtContext ctx);

    void exitError_app_tag_stmt(YangParser.Error_app_tag_stmtContext ctx);

    void enterData_def_stmt(YangParser.Data_def_stmtContext ctx);

    void exitData_def_stmt(YangParser.Data_def_stmtContext ctx);

    void enterIdentity_stmt(YangParser.Identity_stmtContext ctx);

    void exitIdentity_stmt(YangParser.Identity_stmtContext ctx);

    void enterDeviate_not_supported_stmt(
            YangParser.Deviate_not_supported_stmtContext ctx);

    void exitDeviate_not_supported_stmt(
            YangParser.Deviate_not_supported_stmtContext ctx);

    void enterPrefix_stmt(YangParser.Prefix_stmtContext ctx);

    void exitPrefix_stmt(YangParser.Prefix_stmtContext ctx);

    void enterMeta_stmts(YangParser.Meta_stmtsContext ctx);

    void exitMeta_stmts(YangParser.Meta_stmtsContext ctx);

    void enterLinkage_stmts(YangParser.Linkage_stmtsContext ctx);

    void exitLinkage_stmts(YangParser.Linkage_stmtsContext ctx);

    void enterGrouping_stmt(YangParser.Grouping_stmtContext ctx);

    void exitGrouping_stmt(YangParser.Grouping_stmtContext ctx);

    void enterFeature_stmt(YangParser.Feature_stmtContext ctx);

    void exitFeature_stmt(YangParser.Feature_stmtContext ctx);

    void enterYang(YangParser.YangContext ctx);

    void exitYang(YangParser.YangContext ctx);

    void enterIdentityref_specification(
            YangParser.Identityref_specificationContext ctx);

    void exitIdentityref_specification(
            YangParser.Identityref_specificationContext ctx);

    void enterNumerical_restrictions(
            YangParser.Numerical_restrictionsContext ctx);

    void exitNumerical_restrictions(YangParser.Numerical_restrictionsContext ctx);

    void enterModule_header_stmts(YangParser.Module_header_stmtsContext ctx);

    void exitModule_header_stmts(YangParser.Module_header_stmtsContext ctx);

    void enterRequire_instance_stmt(YangParser.Require_instance_stmtContext ctx);

    void exitRequire_instance_stmt(YangParser.Require_instance_stmtContext ctx);

    void enterBit_stmt(YangParser.Bit_stmtContext ctx);

    void exitBit_stmt(YangParser.Bit_stmtContext ctx);

    void enterType_stmt(YangParser.Type_stmtContext ctx);

    void exitType_stmt(YangParser.Type_stmtContext ctx);

    void enterPattern_stmt(YangParser.Pattern_stmtContext ctx);

    void exitPattern_stmt(YangParser.Pattern_stmtContext ctx);

    void enterDeviation_stmt(YangParser.Deviation_stmtContext ctx);

    void exitDeviation_stmt(YangParser.Deviation_stmtContext ctx);

    void enterDeviate_replace_stmt(YangParser.Deviate_replace_stmtContext ctx);

    void exitDeviate_replace_stmt(YangParser.Deviate_replace_stmtContext ctx);

    void enterKey_stmt(YangParser.Key_stmtContext ctx);

    void exitKey_stmt(YangParser.Key_stmtContext ctx);

    void enterRequire_instance_arg(YangParser.Require_instance_argContext ctx);

    void exitRequire_instance_arg(YangParser.Require_instance_argContext ctx);

    void enterLeaf_list_stmt(YangParser.Leaf_list_stmtContext ctx);

    void exitLeaf_list_stmt(YangParser.Leaf_list_stmtContext ctx);

    void enterAugment_stmt(YangParser.Augment_stmtContext ctx);

    void exitAugment_stmt(YangParser.Augment_stmtContext ctx);

    void enterDeviate_delete_stmt(YangParser.Deviate_delete_stmtContext ctx);

    void exitDeviate_delete_stmt(YangParser.Deviate_delete_stmtContext ctx);

    void enterTypedef_stmt(YangParser.Typedef_stmtContext ctx);

    void exitTypedef_stmt(YangParser.Typedef_stmtContext ctx);

    void enterContainer_stmt(YangParser.Container_stmtContext ctx);

    void exitContainer_stmt(YangParser.Container_stmtContext ctx);

    void enterBelongs_to_stmt(YangParser.Belongs_to_stmtContext ctx);

    void exitBelongs_to_stmt(YangParser.Belongs_to_stmtContext ctx);

    void enterBase_stmt(YangParser.Base_stmtContext ctx);

    void exitBase_stmt(YangParser.Base_stmtContext ctx);

    void enterYang_version_stmt(YangParser.Yang_version_stmtContext ctx);

    void exitYang_version_stmt(YangParser.Yang_version_stmtContext ctx);

    void enterReference_stmt(YangParser.Reference_stmtContext ctx);

    void exitReference_stmt(YangParser.Reference_stmtContext ctx);

    void enterYin_element_stmt(YangParser.Yin_element_stmtContext ctx);

    void exitYin_element_stmt(YangParser.Yin_element_stmtContext ctx);

    void enterLeaf_stmt(YangParser.Leaf_stmtContext ctx);

    void exitLeaf_stmt(YangParser.Leaf_stmtContext ctx);

    void enterCase_stmt(YangParser.Case_stmtContext ctx);

    void exitCase_stmt(YangParser.Case_stmtContext ctx);

    void enterModule_stmt(YangParser.Module_stmtContext ctx);

    void exitModule_stmt(YangParser.Module_stmtContext ctx);

    void enterRpc_stmt(YangParser.Rpc_stmtContext ctx);

    void exitRpc_stmt(YangParser.Rpc_stmtContext ctx);

    void enterType_body_stmts(YangParser.Type_body_stmtsContext ctx);

    void exitType_body_stmts(YangParser.Type_body_stmtsContext ctx);

    void enterExtension_stmt(YangParser.Extension_stmtContext ctx);

    void exitExtension_stmt(YangParser.Extension_stmtContext ctx);

    void enterSubmodule_header_stmts(
            YangParser.Submodule_header_stmtsContext ctx);

    void exitSubmodule_header_stmts(YangParser.Submodule_header_stmtsContext ctx);

    void enterRefine_container_stmts(
            YangParser.Refine_container_stmtsContext ctx);

    void exitRefine_container_stmts(YangParser.Refine_container_stmtsContext ctx);

    void enterValue_stmt(YangParser.Value_stmtContext ctx);

    void exitValue_stmt(YangParser.Value_stmtContext ctx);

    void enterRefine_list_stmts(YangParser.Refine_list_stmtsContext ctx);

    void exitRefine_list_stmts(YangParser.Refine_list_stmtsContext ctx);

    void enterDefault_stmt(YangParser.Default_stmtContext ctx);

    void exitDefault_stmt(YangParser.Default_stmtContext ctx);

    void enterRevision_stmts(YangParser.Revision_stmtsContext ctx);

    void exitRevision_stmts(YangParser.Revision_stmtsContext ctx);

    void enterAnyxml_stmt(YangParser.Anyxml_stmtContext ctx);

    void exitAnyxml_stmt(YangParser.Anyxml_stmtContext ctx);

    void enterStatus_stmt(YangParser.Status_stmtContext ctx);

    void exitStatus_stmt(YangParser.Status_stmtContext ctx);

    void enterContact_stmt(YangParser.Contact_stmtContext ctx);

    void exitContact_stmt(YangParser.Contact_stmtContext ctx);

    void enterIdentifier_stmt(YangParser.Identifier_stmtContext ctx);

    void exitIdentifier_stmt(YangParser.Identifier_stmtContext ctx);

    void enterString(YangParser.StringContext ctx);

    void exitString(YangParser.StringContext ctx);

    void enterRefine_choice_stmts(YangParser.Refine_choice_stmtsContext ctx);

    void exitRefine_choice_stmts(YangParser.Refine_choice_stmtsContext ctx);

    void enterMandatory_arg(YangParser.Mandatory_argContext ctx);

    void exitMandatory_arg(YangParser.Mandatory_argContext ctx);

    void enterRefine_stmt(YangParser.Refine_stmtContext ctx);

    void exitRefine_stmt(YangParser.Refine_stmtContext ctx);

    void enterInstance_identifier_specification(
            YangParser.Instance_identifier_specificationContext ctx);

    void exitInstance_identifier_specification(
            YangParser.Instance_identifier_specificationContext ctx);

    void enterBits_specification(YangParser.Bits_specificationContext ctx);

    void exitBits_specification(YangParser.Bits_specificationContext ctx);

    void enterWhen_stmt(YangParser.When_stmtContext ctx);

    void exitWhen_stmt(YangParser.When_stmtContext ctx);

    void enterString_restrictions(YangParser.String_restrictionsContext ctx);

    void exitString_restrictions(YangParser.String_restrictionsContext ctx);

    void enterRefine_leaf_stmts(YangParser.Refine_leaf_stmtsContext ctx);

    void exitRefine_leaf_stmts(YangParser.Refine_leaf_stmtsContext ctx);

    void enterMandatory_stmt(YangParser.Mandatory_stmtContext ctx);

    void exitMandatory_stmt(YangParser.Mandatory_stmtContext ctx);

    void enterOrdered_by_arg(YangParser.Ordered_by_argContext ctx);

    void exitOrdered_by_arg(YangParser.Ordered_by_argContext ctx);

    void enterMin_elements_stmt(YangParser.Min_elements_stmtContext ctx);

    void exitMin_elements_stmt(YangParser.Min_elements_stmtContext ctx);

    void enterStmtend(YangParser.StmtendContext ctx);

    void exitStmtend(YangParser.StmtendContext ctx);

    void enterRefine_anyxml_stmts(YangParser.Refine_anyxml_stmtsContext ctx);

    void exitRefine_anyxml_stmts(YangParser.Refine_anyxml_stmtsContext ctx);

    void enterDescription_stmt(YangParser.Description_stmtContext ctx);

    void exitDescription_stmt(YangParser.Description_stmtContext ctx);

    void enterPath_stmt(YangParser.Path_stmtContext ctx);

    void exitPath_stmt(YangParser.Path_stmtContext ctx);

    void enterInclude_stmt(YangParser.Include_stmtContext ctx);

    void exitInclude_stmt(YangParser.Include_stmtContext ctx);

    void enterUnits_stmt(YangParser.Units_stmtContext ctx);

    void exitUnits_stmt(YangParser.Units_stmtContext ctx);

    void enterUses_stmt(YangParser.Uses_stmtContext ctx);

    void exitUses_stmt(YangParser.Uses_stmtContext ctx);

    void enterOrdered_by_stmt(YangParser.Ordered_by_stmtContext ctx);

    void exitOrdered_by_stmt(YangParser.Ordered_by_stmtContext ctx);

    void enterRange_stmt(YangParser.Range_stmtContext ctx);

    void exitRange_stmt(YangParser.Range_stmtContext ctx);

    void enterNamespace_stmt(YangParser.Namespace_stmtContext ctx);

    void exitNamespace_stmt(YangParser.Namespace_stmtContext ctx);

    void enterDeviate_add_stmt(YangParser.Deviate_add_stmtContext ctx);

    void exitDeviate_add_stmt(YangParser.Deviate_add_stmtContext ctx);

    void enterShort_case_stmt(YangParser.Short_case_stmtContext ctx);

    void exitShort_case_stmt(YangParser.Short_case_stmtContext ctx);

    void enterConfig_stmt(YangParser.Config_stmtContext ctx);

    void exitConfig_stmt(YangParser.Config_stmtContext ctx);

    void enterEnum_stmt(YangParser.Enum_stmtContext ctx);

    void exitEnum_stmt(YangParser.Enum_stmtContext ctx);

    void enterYin_element_arg(YangParser.Yin_element_argContext ctx);

    void exitYin_element_arg(YangParser.Yin_element_argContext ctx);

    void enterOrganization_stmt(YangParser.Organization_stmtContext ctx);

    void exitOrganization_stmt(YangParser.Organization_stmtContext ctx);

    void enterUnion_specification(YangParser.Union_specificationContext ctx);

    void exitUnion_specification(YangParser.Union_specificationContext ctx);

    void enterMax_value_arg(YangParser.Max_value_argContext ctx);

    void exitMax_value_arg(YangParser.Max_value_argContext ctx);

    void enterSubmodule_stmt(YangParser.Submodule_stmtContext ctx);

    void exitSubmodule_stmt(YangParser.Submodule_stmtContext ctx);

    void enterStatus_arg(YangParser.Status_argContext ctx);

    void exitStatus_arg(YangParser.Status_argContext ctx);

    void enterList_stmt(YangParser.List_stmtContext ctx);

    void exitList_stmt(YangParser.List_stmtContext ctx);

    void enterMax_elements_stmt(YangParser.Max_elements_stmtContext ctx);

    void exitMax_elements_stmt(YangParser.Max_elements_stmtContext ctx);

    void enterImport_stmt(YangParser.Import_stmtContext ctx);

    void exitImport_stmt(YangParser.Import_stmtContext ctx);

    void enterConfig_arg(YangParser.Config_argContext ctx);

    void exitConfig_arg(YangParser.Config_argContext ctx);

    void enterRevision_date_stmt(YangParser.Revision_date_stmtContext ctx);

    void exitRevision_date_stmt(YangParser.Revision_date_stmtContext ctx);

    void enterRefune_pom(YangParser.Refune_pomContext ctx);

    void exitRefune_pom(YangParser.Refune_pomContext ctx);

    void enterPresence_stmt(YangParser.Presence_stmtContext ctx);

    void exitPresence_stmt(YangParser.Presence_stmtContext ctx);

    void enterFraction_digits_stmt(YangParser.Fraction_digits_stmtContext ctx);

    void exitFraction_digits_stmt(YangParser.Fraction_digits_stmtContext ctx);

    void enterNotification_stmt(YangParser.Notification_stmtContext ctx);

    void exitNotification_stmt(YangParser.Notification_stmtContext ctx);

    void enterInput_stmt(YangParser.Input_stmtContext ctx);

    void exitInput_stmt(YangParser.Input_stmtContext ctx);

    void enterUses_augment_stmt(YangParser.Uses_augment_stmtContext ctx);

    void exitUses_augment_stmt(YangParser.Uses_augment_stmtContext ctx);

    void enterRefine_case_stmts(YangParser.Refine_case_stmtsContext ctx);

    void exitRefine_case_stmts(YangParser.Refine_case_stmtsContext ctx);

    void enterDecimal64_specification(
            YangParser.Decimal64_specificationContext ctx);

    void exitDecimal64_specification(
            YangParser.Decimal64_specificationContext ctx);

    void enterIf_feature_stmt(YangParser.If_feature_stmtContext ctx);

    void exitIf_feature_stmt(YangParser.If_feature_stmtContext ctx);

    void enterRevision_stmt(YangParser.Revision_stmtContext ctx);

    void exitRevision_stmt(YangParser.Revision_stmtContext ctx);

    void enterLength_stmt(YangParser.Length_stmtContext ctx);

    void exitLength_stmt(YangParser.Length_stmtContext ctx);

    void enterMust_stmt(YangParser.Must_stmtContext ctx);

    void exitMust_stmt(YangParser.Must_stmtContext ctx);

    void enterBody_stmts(YangParser.Body_stmtsContext ctx);

    void exitBody_stmts(YangParser.Body_stmtsContext ctx);

    void enterError_message_stmt(YangParser.Error_message_stmtContext ctx);

    void exitError_message_stmt(YangParser.Error_message_stmtContext ctx);

    void enterUnique_stmt(YangParser.Unique_stmtContext ctx);

    void exitUnique_stmt(YangParser.Unique_stmtContext ctx);

    void enterChoice_stmt(YangParser.Choice_stmtContext ctx);

    void exitChoice_stmt(YangParser.Choice_stmtContext ctx);

    void enterOutput_stmt(YangParser.Output_stmtContext ctx);

    void exitOutput_stmt(YangParser.Output_stmtContext ctx);
}