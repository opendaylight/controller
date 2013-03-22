/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.antlrv4.code.gen;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.TerminalNode;

public class YangParserBaseListener implements YangParserListener {
    @Override
    public void enterEnum_specification(YangParser.Enum_specificationContext ctx) {
    }

    @Override
    public void exitEnum_specification(YangParser.Enum_specificationContext ctx) {
    }

    @Override
    public void enterRefine_leaf_list_stmts(
            YangParser.Refine_leaf_list_stmtsContext ctx) {
    }

    @Override
    public void exitRefine_leaf_list_stmts(
            YangParser.Refine_leaf_list_stmtsContext ctx) {
    }

    @Override
    public void enterPosition_stmt(YangParser.Position_stmtContext ctx) {
    }

    @Override
    public void exitPosition_stmt(YangParser.Position_stmtContext ctx) {
    }

    @Override
    public void enterArgument_stmt(YangParser.Argument_stmtContext ctx) {
    }

    @Override
    public void exitArgument_stmt(YangParser.Argument_stmtContext ctx) {
    }

    @Override
    public void enterLeafref_specification(
            YangParser.Leafref_specificationContext ctx) {
    }

    @Override
    public void exitLeafref_specification(
            YangParser.Leafref_specificationContext ctx) {
    }

    @Override
    public void enterError_app_tag_stmt(YangParser.Error_app_tag_stmtContext ctx) {
    }

    @Override
    public void exitError_app_tag_stmt(YangParser.Error_app_tag_stmtContext ctx) {
    }

    @Override
    public void enterData_def_stmt(YangParser.Data_def_stmtContext ctx) {
    }

    @Override
    public void exitData_def_stmt(YangParser.Data_def_stmtContext ctx) {
    }

    @Override
    public void enterIdentity_stmt(YangParser.Identity_stmtContext ctx) {
    }

    @Override
    public void exitIdentity_stmt(YangParser.Identity_stmtContext ctx) {
    }

    @Override
    public void enterDeviate_not_supported_stmt(
            YangParser.Deviate_not_supported_stmtContext ctx) {
    }

    @Override
    public void exitDeviate_not_supported_stmt(
            YangParser.Deviate_not_supported_stmtContext ctx) {
    }

    @Override
    public void enterPrefix_stmt(YangParser.Prefix_stmtContext ctx) {
    }

    @Override
    public void exitPrefix_stmt(YangParser.Prefix_stmtContext ctx) {
    }

    @Override
    public void enterMeta_stmts(YangParser.Meta_stmtsContext ctx) {
    }

    @Override
    public void exitMeta_stmts(YangParser.Meta_stmtsContext ctx) {
    }

    @Override
    public void enterLinkage_stmts(YangParser.Linkage_stmtsContext ctx) {
    }

    @Override
    public void exitLinkage_stmts(YangParser.Linkage_stmtsContext ctx) {
    }

    @Override
    public void enterGrouping_stmt(YangParser.Grouping_stmtContext ctx) {
    }

    @Override
    public void exitGrouping_stmt(YangParser.Grouping_stmtContext ctx) {
    }

    @Override
    public void enterFeature_stmt(YangParser.Feature_stmtContext ctx) {
    }

    @Override
    public void exitFeature_stmt(YangParser.Feature_stmtContext ctx) {
    }

    @Override
    public void enterYang(YangParser.YangContext ctx) {
    }

    @Override
    public void exitYang(YangParser.YangContext ctx) {
    }

    @Override
    public void enterIdentityref_specification(
            YangParser.Identityref_specificationContext ctx) {
    }

    @Override
    public void exitIdentityref_specification(
            YangParser.Identityref_specificationContext ctx) {
    }

    @Override
    public void enterNumerical_restrictions(
            YangParser.Numerical_restrictionsContext ctx) {
    }

    @Override
    public void exitNumerical_restrictions(
            YangParser.Numerical_restrictionsContext ctx) {
    }

    @Override
    public void enterModule_header_stmts(
            YangParser.Module_header_stmtsContext ctx) {
    }

    @Override
    public void exitModule_header_stmts(
            YangParser.Module_header_stmtsContext ctx) {
    }

    @Override
    public void enterRequire_instance_stmt(
            YangParser.Require_instance_stmtContext ctx) {
    }

    @Override
    public void exitRequire_instance_stmt(
            YangParser.Require_instance_stmtContext ctx) {
    }

    @Override
    public void enterBit_stmt(YangParser.Bit_stmtContext ctx) {
    }

    @Override
    public void exitBit_stmt(YangParser.Bit_stmtContext ctx) {
    }

    @Override
    public void enterType_stmt(YangParser.Type_stmtContext ctx) {
    }

    @Override
    public void exitType_stmt(YangParser.Type_stmtContext ctx) {
    }

    @Override
    public void enterPattern_stmt(YangParser.Pattern_stmtContext ctx) {
    }

    @Override
    public void exitPattern_stmt(YangParser.Pattern_stmtContext ctx) {
    }

    @Override
    public void enterDeviation_stmt(YangParser.Deviation_stmtContext ctx) {
    }

    @Override
    public void exitDeviation_stmt(YangParser.Deviation_stmtContext ctx) {
    }

    @Override
    public void enterDeviate_replace_stmt(
            YangParser.Deviate_replace_stmtContext ctx) {
    }

    @Override
    public void exitDeviate_replace_stmt(
            YangParser.Deviate_replace_stmtContext ctx) {
    }

    @Override
    public void enterKey_stmt(YangParser.Key_stmtContext ctx) {
    }

    @Override
    public void exitKey_stmt(YangParser.Key_stmtContext ctx) {
    }

    @Override
    public void enterRequire_instance_arg(
            YangParser.Require_instance_argContext ctx) {
    }

    @Override
    public void exitRequire_instance_arg(
            YangParser.Require_instance_argContext ctx) {
    }

    @Override
    public void enterLeaf_list_stmt(YangParser.Leaf_list_stmtContext ctx) {
    }

    @Override
    public void exitLeaf_list_stmt(YangParser.Leaf_list_stmtContext ctx) {
    }

    @Override
    public void enterAugment_stmt(YangParser.Augment_stmtContext ctx) {
    }

    @Override
    public void exitAugment_stmt(YangParser.Augment_stmtContext ctx) {
    }

    @Override
    public void enterDeviate_delete_stmt(
            YangParser.Deviate_delete_stmtContext ctx) {
    }

    @Override
    public void exitDeviate_delete_stmt(
            YangParser.Deviate_delete_stmtContext ctx) {
    }

    @Override
    public void enterTypedef_stmt(YangParser.Typedef_stmtContext ctx) {
    }

    @Override
    public void exitTypedef_stmt(YangParser.Typedef_stmtContext ctx) {
    }

    @Override
    public void enterContainer_stmt(YangParser.Container_stmtContext ctx) {
    }

    @Override
    public void exitContainer_stmt(YangParser.Container_stmtContext ctx) {
    }

    @Override
    public void enterBelongs_to_stmt(YangParser.Belongs_to_stmtContext ctx) {
    }

    @Override
    public void exitBelongs_to_stmt(YangParser.Belongs_to_stmtContext ctx) {
    }

    @Override
    public void enterBase_stmt(YangParser.Base_stmtContext ctx) {
    }

    @Override
    public void exitBase_stmt(YangParser.Base_stmtContext ctx) {
    }

    @Override
    public void enterYang_version_stmt(YangParser.Yang_version_stmtContext ctx) {
    }

    @Override
    public void exitYang_version_stmt(YangParser.Yang_version_stmtContext ctx) {
    }

    @Override
    public void enterReference_stmt(YangParser.Reference_stmtContext ctx) {
    }

    @Override
    public void exitReference_stmt(YangParser.Reference_stmtContext ctx) {
    }

    @Override
    public void enterYin_element_stmt(YangParser.Yin_element_stmtContext ctx) {
    }

    @Override
    public void exitYin_element_stmt(YangParser.Yin_element_stmtContext ctx) {
    }

    @Override
    public void enterLeaf_stmt(YangParser.Leaf_stmtContext ctx) {
    }

    @Override
    public void exitLeaf_stmt(YangParser.Leaf_stmtContext ctx) {
    }

    @Override
    public void enterCase_stmt(YangParser.Case_stmtContext ctx) {
    }

    @Override
    public void exitCase_stmt(YangParser.Case_stmtContext ctx) {
    }

    @Override
    public void enterModule_stmt(YangParser.Module_stmtContext ctx) {
    }

    @Override
    public void exitModule_stmt(YangParser.Module_stmtContext ctx) {
    }

    @Override
    public void enterRpc_stmt(YangParser.Rpc_stmtContext ctx) {
    }

    @Override
    public void exitRpc_stmt(YangParser.Rpc_stmtContext ctx) {
    }

    @Override
    public void enterType_body_stmts(YangParser.Type_body_stmtsContext ctx) {
    }

    @Override
    public void exitType_body_stmts(YangParser.Type_body_stmtsContext ctx) {
    }

    @Override
    public void enterExtension_stmt(YangParser.Extension_stmtContext ctx) {
    }

    @Override
    public void exitExtension_stmt(YangParser.Extension_stmtContext ctx) {
    }

    @Override
    public void enterSubmodule_header_stmts(
            YangParser.Submodule_header_stmtsContext ctx) {
    }

    @Override
    public void exitSubmodule_header_stmts(
            YangParser.Submodule_header_stmtsContext ctx) {
    }

    @Override
    public void enterRefine_container_stmts(
            YangParser.Refine_container_stmtsContext ctx) {
    }

    @Override
    public void exitRefine_container_stmts(
            YangParser.Refine_container_stmtsContext ctx) {
    }

    @Override
    public void enterValue_stmt(YangParser.Value_stmtContext ctx) {
    }

    @Override
    public void exitValue_stmt(YangParser.Value_stmtContext ctx) {
    }

    @Override
    public void enterRefine_list_stmts(YangParser.Refine_list_stmtsContext ctx) {
    }

    @Override
    public void exitRefine_list_stmts(YangParser.Refine_list_stmtsContext ctx) {
    }

    @Override
    public void enterDefault_stmt(YangParser.Default_stmtContext ctx) {
    }

    @Override
    public void exitDefault_stmt(YangParser.Default_stmtContext ctx) {
    }

    @Override
    public void enterRevision_stmts(YangParser.Revision_stmtsContext ctx) {
    }

    @Override
    public void exitRevision_stmts(YangParser.Revision_stmtsContext ctx) {
    }

    @Override
    public void enterAnyxml_stmt(YangParser.Anyxml_stmtContext ctx) {
    }

    @Override
    public void exitAnyxml_stmt(YangParser.Anyxml_stmtContext ctx) {
    }

    @Override
    public void enterStatus_stmt(YangParser.Status_stmtContext ctx) {
    }

    @Override
    public void exitStatus_stmt(YangParser.Status_stmtContext ctx) {
    }

    @Override
    public void enterContact_stmt(YangParser.Contact_stmtContext ctx) {
    }

    @Override
    public void exitContact_stmt(YangParser.Contact_stmtContext ctx) {
    }

    @Override
    public void enterIdentifier_stmt(YangParser.Identifier_stmtContext ctx) {
    }

    @Override
    public void exitIdentifier_stmt(YangParser.Identifier_stmtContext ctx) {
    }

    @Override
    public void enterString(YangParser.StringContext ctx) {
    }

    @Override
    public void exitString(YangParser.StringContext ctx) {
    }

    @Override
    public void enterRefine_choice_stmts(
            YangParser.Refine_choice_stmtsContext ctx) {
    }

    @Override
    public void exitRefine_choice_stmts(
            YangParser.Refine_choice_stmtsContext ctx) {
    }

    @Override
    public void enterMandatory_arg(YangParser.Mandatory_argContext ctx) {
    }

    @Override
    public void exitMandatory_arg(YangParser.Mandatory_argContext ctx) {
    }

    @Override
    public void enterRefine_stmt(YangParser.Refine_stmtContext ctx) {
    }

    @Override
    public void exitRefine_stmt(YangParser.Refine_stmtContext ctx) {
    }

    @Override
    public void enterInstance_identifier_specification(
            YangParser.Instance_identifier_specificationContext ctx) {
    }

    @Override
    public void exitInstance_identifier_specification(
            YangParser.Instance_identifier_specificationContext ctx) {
    }

    @Override
    public void enterBits_specification(YangParser.Bits_specificationContext ctx) {
    }

    @Override
    public void exitBits_specification(YangParser.Bits_specificationContext ctx) {
    }

    @Override
    public void enterWhen_stmt(YangParser.When_stmtContext ctx) {
    }

    @Override
    public void exitWhen_stmt(YangParser.When_stmtContext ctx) {
    }

    @Override
    public void enterString_restrictions(
            YangParser.String_restrictionsContext ctx) {
    }

    @Override
    public void exitString_restrictions(
            YangParser.String_restrictionsContext ctx) {
    }

    @Override
    public void enterRefine_leaf_stmts(YangParser.Refine_leaf_stmtsContext ctx) {
    }

    @Override
    public void exitRefine_leaf_stmts(YangParser.Refine_leaf_stmtsContext ctx) {
    }

    @Override
    public void enterMandatory_stmt(YangParser.Mandatory_stmtContext ctx) {
    }

    @Override
    public void exitMandatory_stmt(YangParser.Mandatory_stmtContext ctx) {
    }

    @Override
    public void enterOrdered_by_arg(YangParser.Ordered_by_argContext ctx) {
    }

    @Override
    public void exitOrdered_by_arg(YangParser.Ordered_by_argContext ctx) {
    }

    @Override
    public void enterMin_elements_stmt(YangParser.Min_elements_stmtContext ctx) {
    }

    @Override
    public void exitMin_elements_stmt(YangParser.Min_elements_stmtContext ctx) {
    }

    @Override
    public void enterStmtend(YangParser.StmtendContext ctx) {
    }

    @Override
    public void exitStmtend(YangParser.StmtendContext ctx) {
    }

    @Override
    public void enterRefine_anyxml_stmts(
            YangParser.Refine_anyxml_stmtsContext ctx) {
    }

    @Override
    public void exitRefine_anyxml_stmts(
            YangParser.Refine_anyxml_stmtsContext ctx) {
    }

    @Override
    public void enterDescription_stmt(YangParser.Description_stmtContext ctx) {
    }

    @Override
    public void exitDescription_stmt(YangParser.Description_stmtContext ctx) {
    }

    @Override
    public void enterPath_stmt(YangParser.Path_stmtContext ctx) {
    }

    @Override
    public void exitPath_stmt(YangParser.Path_stmtContext ctx) {
    }

    @Override
    public void enterInclude_stmt(YangParser.Include_stmtContext ctx) {
    }

    @Override
    public void exitInclude_stmt(YangParser.Include_stmtContext ctx) {
    }

    @Override
    public void enterUnits_stmt(YangParser.Units_stmtContext ctx) {
    }

    @Override
    public void exitUnits_stmt(YangParser.Units_stmtContext ctx) {
    }

    @Override
    public void enterUses_stmt(YangParser.Uses_stmtContext ctx) {
    }

    @Override
    public void exitUses_stmt(YangParser.Uses_stmtContext ctx) {
    }

    @Override
    public void enterOrdered_by_stmt(YangParser.Ordered_by_stmtContext ctx) {
    }

    @Override
    public void exitOrdered_by_stmt(YangParser.Ordered_by_stmtContext ctx) {
    }

    @Override
    public void enterRange_stmt(YangParser.Range_stmtContext ctx) {
    }

    @Override
    public void exitRange_stmt(YangParser.Range_stmtContext ctx) {
    }

    @Override
    public void enterNamespace_stmt(YangParser.Namespace_stmtContext ctx) {
    }

    @Override
    public void exitNamespace_stmt(YangParser.Namespace_stmtContext ctx) {
    }

    @Override
    public void enterDeviate_add_stmt(YangParser.Deviate_add_stmtContext ctx) {
    }

    @Override
    public void exitDeviate_add_stmt(YangParser.Deviate_add_stmtContext ctx) {
    }

    @Override
    public void enterShort_case_stmt(YangParser.Short_case_stmtContext ctx) {
    }

    @Override
    public void exitShort_case_stmt(YangParser.Short_case_stmtContext ctx) {
    }

    @Override
    public void enterConfig_stmt(YangParser.Config_stmtContext ctx) {
    }

    @Override
    public void exitConfig_stmt(YangParser.Config_stmtContext ctx) {
    }

    @Override
    public void enterEnum_stmt(YangParser.Enum_stmtContext ctx) {
    }

    @Override
    public void exitEnum_stmt(YangParser.Enum_stmtContext ctx) {
    }

    @Override
    public void enterYin_element_arg(YangParser.Yin_element_argContext ctx) {
    }

    @Override
    public void exitYin_element_arg(YangParser.Yin_element_argContext ctx) {
    }

    @Override
    public void enterOrganization_stmt(YangParser.Organization_stmtContext ctx) {
    }

    @Override
    public void exitOrganization_stmt(YangParser.Organization_stmtContext ctx) {
    }

    @Override
    public void enterUnion_specification(
            YangParser.Union_specificationContext ctx) {
    }

    @Override
    public void exitUnion_specification(
            YangParser.Union_specificationContext ctx) {
    }

    @Override
    public void enterMax_value_arg(YangParser.Max_value_argContext ctx) {
    }

    @Override
    public void exitMax_value_arg(YangParser.Max_value_argContext ctx) {
    }

    @Override
    public void enterSubmodule_stmt(YangParser.Submodule_stmtContext ctx) {
    }

    @Override
    public void exitSubmodule_stmt(YangParser.Submodule_stmtContext ctx) {
    }

    @Override
    public void enterStatus_arg(YangParser.Status_argContext ctx) {
    }

    @Override
    public void exitStatus_arg(YangParser.Status_argContext ctx) {
    }

    @Override
    public void enterList_stmt(YangParser.List_stmtContext ctx) {
    }

    @Override
    public void exitList_stmt(YangParser.List_stmtContext ctx) {
    }

    @Override
    public void enterMax_elements_stmt(YangParser.Max_elements_stmtContext ctx) {
    }

    @Override
    public void exitMax_elements_stmt(YangParser.Max_elements_stmtContext ctx) {
    }

    @Override
    public void enterImport_stmt(YangParser.Import_stmtContext ctx) {
    }

    @Override
    public void exitImport_stmt(YangParser.Import_stmtContext ctx) {
    }

    @Override
    public void enterConfig_arg(YangParser.Config_argContext ctx) {
    }

    @Override
    public void exitConfig_arg(YangParser.Config_argContext ctx) {
    }

    @Override
    public void enterRevision_date_stmt(YangParser.Revision_date_stmtContext ctx) {
    }

    @Override
    public void exitRevision_date_stmt(YangParser.Revision_date_stmtContext ctx) {
    }

    @Override
    public void enterRefune_pom(YangParser.Refune_pomContext ctx) {
    }

    @Override
    public void exitRefune_pom(YangParser.Refune_pomContext ctx) {
    }

    @Override
    public void enterPresence_stmt(YangParser.Presence_stmtContext ctx) {
    }

    @Override
    public void exitPresence_stmt(YangParser.Presence_stmtContext ctx) {
    }

    @Override
    public void enterFraction_digits_stmt(
            YangParser.Fraction_digits_stmtContext ctx) {
    }

    @Override
    public void exitFraction_digits_stmt(
            YangParser.Fraction_digits_stmtContext ctx) {
    }

    @Override
    public void enterNotification_stmt(YangParser.Notification_stmtContext ctx) {
    }

    @Override
    public void exitNotification_stmt(YangParser.Notification_stmtContext ctx) {
    }

    @Override
    public void enterInput_stmt(YangParser.Input_stmtContext ctx) {
    }

    @Override
    public void exitInput_stmt(YangParser.Input_stmtContext ctx) {
    }

    @Override
    public void enterUses_augment_stmt(YangParser.Uses_augment_stmtContext ctx) {
    }

    @Override
    public void exitUses_augment_stmt(YangParser.Uses_augment_stmtContext ctx) {
    }

    @Override
    public void enterRefine_case_stmts(YangParser.Refine_case_stmtsContext ctx) {
    }

    @Override
    public void exitRefine_case_stmts(YangParser.Refine_case_stmtsContext ctx) {
    }

    @Override
    public void enterDecimal64_specification(
            YangParser.Decimal64_specificationContext ctx) {
    }

    @Override
    public void exitDecimal64_specification(
            YangParser.Decimal64_specificationContext ctx) {
    }

    @Override
    public void enterIf_feature_stmt(YangParser.If_feature_stmtContext ctx) {
    }

    @Override
    public void exitIf_feature_stmt(YangParser.If_feature_stmtContext ctx) {
    }

    @Override
    public void enterRevision_stmt(YangParser.Revision_stmtContext ctx) {
    }

    @Override
    public void exitRevision_stmt(YangParser.Revision_stmtContext ctx) {
    }

    @Override
    public void enterLength_stmt(YangParser.Length_stmtContext ctx) {
    }

    @Override
    public void exitLength_stmt(YangParser.Length_stmtContext ctx) {
    }

    @Override
    public void enterMust_stmt(YangParser.Must_stmtContext ctx) {
    }

    @Override
    public void exitMust_stmt(YangParser.Must_stmtContext ctx) {
    }

    @Override
    public void enterBody_stmts(YangParser.Body_stmtsContext ctx) {
    }

    @Override
    public void exitBody_stmts(YangParser.Body_stmtsContext ctx) {
    }

    @Override
    public void enterError_message_stmt(YangParser.Error_message_stmtContext ctx) {
    }

    @Override
    public void exitError_message_stmt(YangParser.Error_message_stmtContext ctx) {
    }

    @Override
    public void enterUnique_stmt(YangParser.Unique_stmtContext ctx) {
    }

    @Override
    public void exitUnique_stmt(YangParser.Unique_stmtContext ctx) {
    }

    @Override
    public void enterChoice_stmt(YangParser.Choice_stmtContext ctx) {
    }

    @Override
    public void exitChoice_stmt(YangParser.Choice_stmtContext ctx) {
    }

    @Override
    public void enterOutput_stmt(YangParser.Output_stmtContext ctx) {
    }

    @Override
    public void exitOutput_stmt(YangParser.Output_stmtContext ctx) {
    }

    @Override
    public void enterEveryRule(ParserRuleContext ctx) {
    }

    @Override
    public void exitEveryRule(ParserRuleContext ctx) {
    }

    @Override
    public void visitTerminal(TerminalNode node) {
    }

    @Override
    public void visitErrorNode(ErrorNode node) {
    }
}