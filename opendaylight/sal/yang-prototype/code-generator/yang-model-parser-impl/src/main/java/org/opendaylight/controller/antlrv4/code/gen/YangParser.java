/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.antlrv4.code.gen;

import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.*;
import org.antlr.v4.runtime.tree.*;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;

@SuppressWarnings({ "all", "warnings", "unchecked", "unused", "cast" })
public class YangParser extends Parser {
    protected static final DFA[] _decisionToDFA;
    protected static final PredictionContextCache _sharedContextCache = new PredictionContextCache();
    public static final int CHOICE_KEYWORD = 65, YIN_ELEMENT_KEYWORD = 8,
            WHEN_KEYWORD = 10, REVISION_KEYWORD = 21, DESCRIPTION_KEYWORD = 60,
            NAMESPACE_KEYWORD = 35, MODULE_KEYWORD = 37,
            REFERENCE_KEYWORD = 24, CONTACT_KEYWORD = 63,
            LEAF_LIST_KEYWORD = 43, REVISION_DATE_KEYWORD = 20,
            BELONGS_TO_KEYWORD = 68, LEAF_KEYWORD = 44, PREFIX_KEYWORD = 27,
            DEFAULT_KEYWORD = 61, PRESENCE_KEYWORD = 26, ARGUMENT_KEYWORD = 71,
            NOTIFICATION_KEYWORD = 34, RPC_KEYWORD = 19,
            CONTAINER_KEYWORD = 62, DEVIATION_KEYWORD = 55,
            STATUS_KEYWORD = 18, IDENTITY_KEYWORD = 50, IDENTIFIER = 73,
            REFINE_KEYWORD = 23, USES_KEYWORD = 12, VALUE_KEYWORD = 11,
            IMPORT_KEYWORD = 48, INPUT_KEYWORD = 46, IF_FEATURE_KEYWORD = 49,
            PLUS = 4, PATTERN_KEYWORD = 29, LENGTH_KEYWORD = 42,
            FEATURE_KEYWORD = 53, REQUIRE_INSTANCE_KEYWORD = 22,
            ORGANIZATION_KEYWORD = 32, UNIQUE_KEYWORD = 14,
            SUBMODULE_KEYWORD = 17, TYPE_KEYWORD = 16, RIGHT_BRACE = 3,
            ERROR_MESSAGE_KEYWORD = 57, LINE_COMMENT = 6, OUTPUT_KEYWORD = 31,
            MIN_ELEMENTS_KEYWORD = 38, MUST_KEYWORD = 36, SEMICOLON = 1,
            POSITION_KEYWORD = 28, PATH_KEYWORD = 30, S = 75, KEY_KEYWORD = 45,
            EXTENSION_KEYWORD = 56, START_BLOCK_COMMENT = 7, WS = 5,
            MANDATORY_KEYWORD = 40, ORDERED_BY_KEYWORD = 33,
            ERROR_APP_TAG_KEYWORD = 58, INCLUDE_KEYWORD = 47,
            ANYXML_KEYWORD = 72, AUGMENT_KEYWORD = 70, DEVIATE_KEYWORD = 54,
            LEFT_BRACE = 2, YANG_VERSION_KEYWORD = 9, LIST_KEYWORD = 41,
            TYPEDEF_KEYWORD = 15, MAX_ELEMENTS_KEYWORD = 39, ENUM_KEYWORD = 59,
            CASE_KEYWORD = 66, UNITS_KEYWORD = 13, GROUPING_KEYWORD = 51,
            END_BLOCK_COMMENT = 76, BASE_KEYWORD = 69, RANGE_KEYWORD = 25,
            FRACTION_DIGITS_KEYWORD = 52, CONFIG_KEYWORD = 64,
            BIT_KEYWORD = 67, STRING = 74;
    public static final String[] tokenNames = { "<INVALID>", "SEMICOLON",
            "LEFT_BRACE", "'}'", "'+'", "WS", "LINE_COMMENT", "'/*'",
            "'yin-element'", "'yang-version'", "'when'", "'value'", "'uses'",
            "'units'", "'unique'", "'typedef'", "'type'", "'submodule'",
            "'status'", "'rpc'", "'revision-date'", "'revision'",
            "'require-instance'", "'refine'", "'reference'", "'range'",
            "'presence'", "'prefix'", "'position'", "'pattern'", "'path'",
            "'output'", "'organization'", "'ordered-by'", "'notification'",
            "'namespace'", "'must'", "'module'", "'min-elements'",
            "'max-elements'", "'mandatory'", "'list'", "'length'",
            "'leaf-list'", "'leaf'", "'key'", "'input'", "'include'",
            "'import'", "'if-feature'", "'identity'", "'grouping'",
            "'fraction-digits'", "'feature'", "'deviate'", "'deviation'",
            "'extension'", "'error-message'", "'error-app-tag'", "'enum'",
            "'description'", "'default'", "'container'", "'contact'",
            "'config'", "'choice'", "'case'", "'bit'", "'belongs-to'",
            "'base'", "'augment'", "'argument'", "'anyxml'", "IDENTIFIER",
            "STRING", "S", "'*/'" };
    public static final int RULE_yang = 0, RULE_string = 1,
            RULE_identifier_stmt = 2, RULE_stmtend = 3,
            RULE_deviate_replace_stmt = 4, RULE_deviate_delete_stmt = 5,
            RULE_deviate_add_stmt = 6, RULE_deviate_not_supported_stmt = 7,
            RULE_deviation_stmt = 8, RULE_notification_stmt = 9,
            RULE_output_stmt = 10, RULE_input_stmt = 11, RULE_rpc_stmt = 12,
            RULE_when_stmt = 13, RULE_augment_stmt = 14,
            RULE_uses_augment_stmt = 15, RULE_refine_anyxml_stmts = 16,
            RULE_refine_case_stmts = 17, RULE_refine_choice_stmts = 18,
            RULE_refine_list_stmts = 19, RULE_refine_leaf_list_stmts = 20,
            RULE_refine_leaf_stmts = 21, RULE_refine_container_stmts = 22,
            RULE_refune_pom = 23, RULE_refine_stmt = 24, RULE_uses_stmt = 25,
            RULE_anyxml_stmt = 26, RULE_case_stmt = 27,
            RULE_short_case_stmt = 28, RULE_choice_stmt = 29,
            RULE_unique_stmt = 30, RULE_key_stmt = 31, RULE_list_stmt = 32,
            RULE_leaf_list_stmt = 33, RULE_leaf_stmt = 34,
            RULE_container_stmt = 35, RULE_grouping_stmt = 36,
            RULE_value_stmt = 37, RULE_max_value_arg = 38,
            RULE_max_elements_stmt = 39, RULE_min_elements_stmt = 40,
            RULE_error_app_tag_stmt = 41, RULE_error_message_stmt = 42,
            RULE_must_stmt = 43, RULE_ordered_by_arg = 44,
            RULE_ordered_by_stmt = 45, RULE_presence_stmt = 46,
            RULE_mandatory_arg = 47, RULE_mandatory_stmt = 48,
            RULE_config_arg = 49, RULE_config_stmt = 50, RULE_status_arg = 51,
            RULE_status_stmt = 52, RULE_position_stmt = 53, RULE_bit_stmt = 54,
            RULE_bits_specification = 55, RULE_union_specification = 56,
            RULE_identityref_specification = 57,
            RULE_instance_identifier_specification = 58,
            RULE_require_instance_arg = 59, RULE_require_instance_stmt = 60,
            RULE_path_stmt = 61, RULE_leafref_specification = 62,
            RULE_enum_stmt = 63, RULE_enum_specification = 64,
            RULE_default_stmt = 65, RULE_pattern_stmt = 66,
            RULE_length_stmt = 67, RULE_string_restrictions = 68,
            RULE_fraction_digits_stmt = 69, RULE_decimal64_specification = 70,
            RULE_range_stmt = 71, RULE_numerical_restrictions = 72,
            RULE_type_body_stmts = 73, RULE_type_stmt = 74,
            RULE_typedef_stmt = 75, RULE_if_feature_stmt = 76,
            RULE_feature_stmt = 77, RULE_base_stmt = 78,
            RULE_identity_stmt = 79, RULE_yin_element_arg = 80,
            RULE_yin_element_stmt = 81, RULE_argument_stmt = 82,
            RULE_extension_stmt = 83, RULE_revision_date_stmt = 84,
            RULE_revision_stmt = 85, RULE_units_stmt = 86,
            RULE_reference_stmt = 87, RULE_description_stmt = 88,
            RULE_contact_stmt = 89, RULE_organization_stmt = 90,
            RULE_belongs_to_stmt = 91, RULE_prefix_stmt = 92,
            RULE_namespace_stmt = 93, RULE_include_stmt = 94,
            RULE_import_stmt = 95, RULE_yang_version_stmt = 96,
            RULE_data_def_stmt = 97, RULE_body_stmts = 98,
            RULE_revision_stmts = 99, RULE_linkage_stmts = 100,
            RULE_meta_stmts = 101, RULE_submodule_header_stmts = 102,
            RULE_module_header_stmts = 103, RULE_submodule_stmt = 104,
            RULE_module_stmt = 105;
    public static final String[] ruleNames = { "yang", "string",
            "identifier_stmt", "stmtend", "deviate_replace_stmt",
            "deviate_delete_stmt", "deviate_add_stmt",
            "deviate_not_supported_stmt", "deviation_stmt",
            "notification_stmt", "output_stmt", "input_stmt", "rpc_stmt",
            "when_stmt", "augment_stmt", "uses_augment_stmt",
            "refine_anyxml_stmts", "refine_case_stmts", "refine_choice_stmts",
            "refine_list_stmts", "refine_leaf_list_stmts", "refine_leaf_stmts",
            "refine_container_stmts", "refune_pom", "refine_stmt", "uses_stmt",
            "anyxml_stmt", "case_stmt", "short_case_stmt", "choice_stmt",
            "unique_stmt", "key_stmt", "list_stmt", "leaf_list_stmt",
            "leaf_stmt", "container_stmt", "grouping_stmt", "value_stmt",
            "max_value_arg", "max_elements_stmt", "min_elements_stmt",
            "error_app_tag_stmt", "error_message_stmt", "must_stmt",
            "ordered_by_arg", "ordered_by_stmt", "presence_stmt",
            "mandatory_arg", "mandatory_stmt", "config_arg", "config_stmt",
            "status_arg", "status_stmt", "position_stmt", "bit_stmt",
            "bits_specification", "union_specification",
            "identityref_specification", "instance_identifier_specification",
            "require_instance_arg", "require_instance_stmt", "path_stmt",
            "leafref_specification", "enum_stmt", "enum_specification",
            "default_stmt", "pattern_stmt", "length_stmt",
            "string_restrictions", "fraction_digits_stmt",
            "decimal64_specification", "range_stmt", "numerical_restrictions",
            "type_body_stmts", "type_stmt", "typedef_stmt", "if_feature_stmt",
            "feature_stmt", "base_stmt", "identity_stmt", "yin_element_arg",
            "yin_element_stmt", "argument_stmt", "extension_stmt",
            "revision_date_stmt", "revision_stmt", "units_stmt",
            "reference_stmt", "description_stmt", "contact_stmt",
            "organization_stmt", "belongs_to_stmt", "prefix_stmt",
            "namespace_stmt", "include_stmt", "import_stmt",
            "yang_version_stmt", "data_def_stmt", "body_stmts",
            "revision_stmts", "linkage_stmts", "meta_stmts",
            "submodule_header_stmts", "module_header_stmts", "submodule_stmt",
            "module_stmt" };

    @Override
    public String getGrammarFileName() {
        return "yangParser.g4";
    }

    @Override
    public String[] getTokenNames() {
        return tokenNames;
    }

    @Override
    public String[] getRuleNames() {
        return ruleNames;
    }

    @Override
    public ATN getATN() {
        return _ATN;
    }

    public YangParser(TokenStream input) {
        super(input);
        _interp = new ParserATNSimulator(this, _ATN, _decisionToDFA,
                _sharedContextCache);
    }

    public static class YangContext extends ParserRuleContext {
        public Submodule_stmtContext submodule_stmt() {
            return getRuleContext(Submodule_stmtContext.class, 0);
        }

        public Module_stmtContext module_stmt() {
            return getRuleContext(Module_stmtContext.class, 0);
        }

        public YangContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_yang;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).enterYang(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).exitYang(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof YangParserVisitor)
                return ((YangParserVisitor<? extends T>) visitor)
                        .visitYang(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final YangContext yang() throws RecognitionException {
        YangContext _localctx = new YangContext(_ctx, getState());
        enterRule(_localctx, 0, RULE_yang);
        try {
            setState(214);
            switch (_input.LA(1)) {
            case MODULE_KEYWORD:
                enterOuterAlt(_localctx, 1);
                {
                    setState(212);
                    module_stmt();
                }
                break;
            case SUBMODULE_KEYWORD:
                enterOuterAlt(_localctx, 2);
                {
                    setState(213);
                    submodule_stmt();
                }
                break;
            default:
                throw new NoViableAltException(this);
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    public static class StringContext extends ParserRuleContext {
        public List<TerminalNode> PLUS() {
            return getTokens(YangParser.PLUS);
        }

        public TerminalNode STRING(int i) {
            return getToken(YangParser.STRING, i);
        }

        public TerminalNode PLUS(int i) {
            return getToken(YangParser.PLUS, i);
        }

        public List<TerminalNode> STRING() {
            return getTokens(YangParser.STRING);
        }

        public StringContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_string;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).enterString(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).exitString(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof YangParserVisitor)
                return ((YangParserVisitor<? extends T>) visitor)
                        .visitString(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final StringContext string() throws RecognitionException {
        StringContext _localctx = new StringContext(_ctx, getState());
        enterRule(_localctx, 2, RULE_string);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(216);
                match(STRING);
                setState(221);
                _errHandler.sync(this);
                _la = _input.LA(1);
                while (_la == PLUS) {
                    {
                        {
                            setState(217);
                            match(PLUS);
                            setState(218);
                            match(STRING);
                        }
                    }
                    setState(223);
                    _errHandler.sync(this);
                    _la = _input.LA(1);
                }
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    public static class Identifier_stmtContext extends ParserRuleContext {
        public StringContext string() {
            return getRuleContext(StringContext.class, 0);
        }

        public StmtendContext stmtend() {
            return getRuleContext(StmtendContext.class, 0);
        }

        public TerminalNode IDENTIFIER() {
            return getToken(YangParser.IDENTIFIER, 0);
        }

        public Identifier_stmtContext(ParserRuleContext parent,
                int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_identifier_stmt;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).enterIdentifier_stmt(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).exitIdentifier_stmt(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof YangParserVisitor)
                return ((YangParserVisitor<? extends T>) visitor)
                        .visitIdentifier_stmt(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final Identifier_stmtContext identifier_stmt()
            throws RecognitionException {
        Identifier_stmtContext _localctx = new Identifier_stmtContext(_ctx,
                getState());
        enterRule(_localctx, 4, RULE_identifier_stmt);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(224);
                match(IDENTIFIER);
                setState(226);
                _la = _input.LA(1);
                if (_la == STRING) {
                    {
                        setState(225);
                        string();
                    }
                }

                setState(228);
                stmtend();
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    public static class StmtendContext extends ParserRuleContext {
        public TerminalNode RIGHT_BRACE() {
            return getToken(YangParser.RIGHT_BRACE, 0);
        }

        public TerminalNode SEMICOLON() {
            return getToken(YangParser.SEMICOLON, 0);
        }

        public Identifier_stmtContext identifier_stmt() {
            return getRuleContext(Identifier_stmtContext.class, 0);
        }

        public TerminalNode LEFT_BRACE() {
            return getToken(YangParser.LEFT_BRACE, 0);
        }

        public StmtendContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_stmtend;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).enterStmtend(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).exitStmtend(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof YangParserVisitor)
                return ((YangParserVisitor<? extends T>) visitor)
                        .visitStmtend(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final StmtendContext stmtend() throws RecognitionException {
        StmtendContext _localctx = new StmtendContext(_ctx, getState());
        enterRule(_localctx, 6, RULE_stmtend);
        int _la;
        try {
            setState(239);
            switch (_input.LA(1)) {
            case SEMICOLON:
                enterOuterAlt(_localctx, 1);
                {
                    {
                        setState(230);
                        match(SEMICOLON);
                        setState(232);
                        switch (getInterpreter().adaptivePredict(_input, 3,
                                _ctx)) {
                        case 1: {
                            setState(231);
                            identifier_stmt();
                        }
                            break;
                        }
                    }
                }
                break;
            case LEFT_BRACE:
                enterOuterAlt(_localctx, 2);
                {
                    {
                        setState(234);
                        match(LEFT_BRACE);
                        setState(236);
                        _la = _input.LA(1);
                        if (_la == IDENTIFIER) {
                            {
                                setState(235);
                                identifier_stmt();
                            }
                        }

                        setState(238);
                        match(RIGHT_BRACE);
                    }
                }
                break;
            default:
                throw new NoViableAltException(this);
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    public static class Deviate_replace_stmtContext extends ParserRuleContext {
        public TerminalNode RIGHT_BRACE() {
            return getToken(YangParser.RIGHT_BRACE, 0);
        }

        public Units_stmtContext units_stmt(int i) {
            return getRuleContext(Units_stmtContext.class, i);
        }

        public Default_stmtContext default_stmt(int i) {
            return getRuleContext(Default_stmtContext.class, i);
        }

        public List<Units_stmtContext> units_stmt() {
            return getRuleContexts(Units_stmtContext.class);
        }

        public List<Max_elements_stmtContext> max_elements_stmt() {
            return getRuleContexts(Max_elements_stmtContext.class);
        }

        public Type_stmtContext type_stmt(int i) {
            return getRuleContext(Type_stmtContext.class, i);
        }

        public TerminalNode DEVIATE_KEYWORD() {
            return getToken(YangParser.DEVIATE_KEYWORD, 0);
        }

        public TerminalNode LEFT_BRACE() {
            return getToken(YangParser.LEFT_BRACE, 0);
        }

        public List<Mandatory_stmtContext> mandatory_stmt() {
            return getRuleContexts(Mandatory_stmtContext.class);
        }

        public List<Type_stmtContext> type_stmt() {
            return getRuleContexts(Type_stmtContext.class);
        }

        public Min_elements_stmtContext min_elements_stmt(int i) {
            return getRuleContext(Min_elements_stmtContext.class, i);
        }

        public List<Default_stmtContext> default_stmt() {
            return getRuleContexts(Default_stmtContext.class);
        }

        public Mandatory_stmtContext mandatory_stmt(int i) {
            return getRuleContext(Mandatory_stmtContext.class, i);
        }

        public Config_stmtContext config_stmt(int i) {
            return getRuleContext(Config_stmtContext.class, i);
        }

        public TerminalNode SEMICOLON() {
            return getToken(YangParser.SEMICOLON, 0);
        }

        public List<Min_elements_stmtContext> min_elements_stmt() {
            return getRuleContexts(Min_elements_stmtContext.class);
        }

        public StringContext string() {
            return getRuleContext(StringContext.class, 0);
        }

        public List<Config_stmtContext> config_stmt() {
            return getRuleContexts(Config_stmtContext.class);
        }

        public Max_elements_stmtContext max_elements_stmt(int i) {
            return getRuleContext(Max_elements_stmtContext.class, i);
        }

        public Deviate_replace_stmtContext(ParserRuleContext parent,
                int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_deviate_replace_stmt;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).enterDeviate_replace_stmt(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).exitDeviate_replace_stmt(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof YangParserVisitor)
                return ((YangParserVisitor<? extends T>) visitor)
                        .visitDeviate_replace_stmt(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final Deviate_replace_stmtContext deviate_replace_stmt()
            throws RecognitionException {
        Deviate_replace_stmtContext _localctx = new Deviate_replace_stmtContext(
                _ctx, getState());
        enterRule(_localctx, 8, RULE_deviate_replace_stmt);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(241);
                match(DEVIATE_KEYWORD);
                setState(242);
                string();
                setState(258);
                switch (_input.LA(1)) {
                case SEMICOLON: {
                    setState(243);
                    match(SEMICOLON);
                }
                    break;
                case LEFT_BRACE: {
                    {
                        setState(244);
                        match(LEFT_BRACE);
                        setState(254);
                        _errHandler.sync(this);
                        _la = _input.LA(1);
                        while (((((_la - 13)) & ~0x3f) == 0 && ((1L << (_la - 13)) & ((1L << (UNITS_KEYWORD - 13))
                                | (1L << (TYPE_KEYWORD - 13))
                                | (1L << (MIN_ELEMENTS_KEYWORD - 13))
                                | (1L << (MAX_ELEMENTS_KEYWORD - 13))
                                | (1L << (MANDATORY_KEYWORD - 13))
                                | (1L << (DEFAULT_KEYWORD - 13)) | (1L << (CONFIG_KEYWORD - 13)))) != 0)) {
                            {
                                setState(252);
                                switch (_input.LA(1)) {
                                case TYPE_KEYWORD: {
                                    setState(245);
                                    type_stmt();
                                }
                                    break;
                                case UNITS_KEYWORD: {
                                    setState(246);
                                    units_stmt();
                                }
                                    break;
                                case DEFAULT_KEYWORD: {
                                    setState(247);
                                    default_stmt();
                                }
                                    break;
                                case CONFIG_KEYWORD: {
                                    setState(248);
                                    config_stmt();
                                }
                                    break;
                                case MANDATORY_KEYWORD: {
                                    setState(249);
                                    mandatory_stmt();
                                }
                                    break;
                                case MIN_ELEMENTS_KEYWORD: {
                                    setState(250);
                                    min_elements_stmt();
                                }
                                    break;
                                case MAX_ELEMENTS_KEYWORD: {
                                    setState(251);
                                    max_elements_stmt();
                                }
                                    break;
                                default:
                                    throw new NoViableAltException(this);
                                }
                            }
                            setState(256);
                            _errHandler.sync(this);
                            _la = _input.LA(1);
                        }
                        setState(257);
                        match(RIGHT_BRACE);
                    }
                }
                    break;
                default:
                    throw new NoViableAltException(this);
                }
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    public static class Deviate_delete_stmtContext extends ParserRuleContext {
        public Units_stmtContext units_stmt(int i) {
            return getRuleContext(Units_stmtContext.class, i);
        }

        public TerminalNode RIGHT_BRACE() {
            return getToken(YangParser.RIGHT_BRACE, 0);
        }

        public List<Default_stmtContext> default_stmt() {
            return getRuleContexts(Default_stmtContext.class);
        }

        public Default_stmtContext default_stmt(int i) {
            return getRuleContext(Default_stmtContext.class, i);
        }

        public List<Units_stmtContext> units_stmt() {
            return getRuleContexts(Units_stmtContext.class);
        }

        public TerminalNode SEMICOLON() {
            return getToken(YangParser.SEMICOLON, 0);
        }

        public List<Must_stmtContext> must_stmt() {
            return getRuleContexts(Must_stmtContext.class);
        }

        public StringContext string() {
            return getRuleContext(StringContext.class, 0);
        }

        public List<Unique_stmtContext> unique_stmt() {
            return getRuleContexts(Unique_stmtContext.class);
        }

        public TerminalNode DEVIATE_KEYWORD() {
            return getToken(YangParser.DEVIATE_KEYWORD, 0);
        }

        public TerminalNode LEFT_BRACE() {
            return getToken(YangParser.LEFT_BRACE, 0);
        }

        public Must_stmtContext must_stmt(int i) {
            return getRuleContext(Must_stmtContext.class, i);
        }

        public Unique_stmtContext unique_stmt(int i) {
            return getRuleContext(Unique_stmtContext.class, i);
        }

        public Deviate_delete_stmtContext(ParserRuleContext parent,
                int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_deviate_delete_stmt;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).enterDeviate_delete_stmt(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).exitDeviate_delete_stmt(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof YangParserVisitor)
                return ((YangParserVisitor<? extends T>) visitor)
                        .visitDeviate_delete_stmt(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final Deviate_delete_stmtContext deviate_delete_stmt()
            throws RecognitionException {
        Deviate_delete_stmtContext _localctx = new Deviate_delete_stmtContext(
                _ctx, getState());
        enterRule(_localctx, 10, RULE_deviate_delete_stmt);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(260);
                match(DEVIATE_KEYWORD);
                setState(261);
                string();
                setState(274);
                switch (_input.LA(1)) {
                case SEMICOLON: {
                    setState(262);
                    match(SEMICOLON);
                }
                    break;
                case LEFT_BRACE: {
                    {
                        setState(263);
                        match(LEFT_BRACE);
                        setState(270);
                        _errHandler.sync(this);
                        _la = _input.LA(1);
                        while ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << UNITS_KEYWORD)
                                | (1L << UNIQUE_KEYWORD) | (1L << MUST_KEYWORD) | (1L << DEFAULT_KEYWORD))) != 0)) {
                            {
                                setState(268);
                                switch (_input.LA(1)) {
                                case UNITS_KEYWORD: {
                                    setState(264);
                                    units_stmt();
                                }
                                    break;
                                case MUST_KEYWORD: {
                                    setState(265);
                                    must_stmt();
                                }
                                    break;
                                case UNIQUE_KEYWORD: {
                                    setState(266);
                                    unique_stmt();
                                }
                                    break;
                                case DEFAULT_KEYWORD: {
                                    setState(267);
                                    default_stmt();
                                }
                                    break;
                                default:
                                    throw new NoViableAltException(this);
                                }
                            }
                            setState(272);
                            _errHandler.sync(this);
                            _la = _input.LA(1);
                        }
                        setState(273);
                        match(RIGHT_BRACE);
                    }
                }
                    break;
                default:
                    throw new NoViableAltException(this);
                }
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    public static class Deviate_add_stmtContext extends ParserRuleContext {
        public TerminalNode RIGHT_BRACE() {
            return getToken(YangParser.RIGHT_BRACE, 0);
        }

        public Units_stmtContext units_stmt(int i) {
            return getRuleContext(Units_stmtContext.class, i);
        }

        public Default_stmtContext default_stmt(int i) {
            return getRuleContext(Default_stmtContext.class, i);
        }

        public List<Units_stmtContext> units_stmt() {
            return getRuleContexts(Units_stmtContext.class);
        }

        public List<Max_elements_stmtContext> max_elements_stmt() {
            return getRuleContexts(Max_elements_stmtContext.class);
        }

        public TerminalNode DEVIATE_KEYWORD() {
            return getToken(YangParser.DEVIATE_KEYWORD, 0);
        }

        public TerminalNode LEFT_BRACE() {
            return getToken(YangParser.LEFT_BRACE, 0);
        }

        public List<Mandatory_stmtContext> mandatory_stmt() {
            return getRuleContexts(Mandatory_stmtContext.class);
        }

        public Must_stmtContext must_stmt(int i) {
            return getRuleContext(Must_stmtContext.class, i);
        }

        public Min_elements_stmtContext min_elements_stmt(int i) {
            return getRuleContext(Min_elements_stmtContext.class, i);
        }

        public List<Default_stmtContext> default_stmt() {
            return getRuleContexts(Default_stmtContext.class);
        }

        public Mandatory_stmtContext mandatory_stmt(int i) {
            return getRuleContext(Mandatory_stmtContext.class, i);
        }

        public Config_stmtContext config_stmt(int i) {
            return getRuleContext(Config_stmtContext.class, i);
        }

        public TerminalNode SEMICOLON() {
            return getToken(YangParser.SEMICOLON, 0);
        }

        public List<Min_elements_stmtContext> min_elements_stmt() {
            return getRuleContexts(Min_elements_stmtContext.class);
        }

        public List<Config_stmtContext> config_stmt() {
            return getRuleContexts(Config_stmtContext.class);
        }

        public StringContext string() {
            return getRuleContext(StringContext.class, 0);
        }

        public List<Must_stmtContext> must_stmt() {
            return getRuleContexts(Must_stmtContext.class);
        }

        public List<Unique_stmtContext> unique_stmt() {
            return getRuleContexts(Unique_stmtContext.class);
        }

        public Max_elements_stmtContext max_elements_stmt(int i) {
            return getRuleContext(Max_elements_stmtContext.class, i);
        }

        public Unique_stmtContext unique_stmt(int i) {
            return getRuleContext(Unique_stmtContext.class, i);
        }

        public Deviate_add_stmtContext(ParserRuleContext parent,
                int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_deviate_add_stmt;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).enterDeviate_add_stmt(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).exitDeviate_add_stmt(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof YangParserVisitor)
                return ((YangParserVisitor<? extends T>) visitor)
                        .visitDeviate_add_stmt(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final Deviate_add_stmtContext deviate_add_stmt()
            throws RecognitionException {
        Deviate_add_stmtContext _localctx = new Deviate_add_stmtContext(_ctx,
                getState());
        enterRule(_localctx, 12, RULE_deviate_add_stmt);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(276);
                match(DEVIATE_KEYWORD);
                setState(277);
                string();
                setState(294);
                switch (_input.LA(1)) {
                case SEMICOLON: {
                    setState(278);
                    match(SEMICOLON);
                }
                    break;
                case LEFT_BRACE: {
                    {
                        setState(279);
                        match(LEFT_BRACE);
                        setState(290);
                        _errHandler.sync(this);
                        _la = _input.LA(1);
                        while (((((_la - 13)) & ~0x3f) == 0 && ((1L << (_la - 13)) & ((1L << (UNITS_KEYWORD - 13))
                                | (1L << (UNIQUE_KEYWORD - 13))
                                | (1L << (MUST_KEYWORD - 13))
                                | (1L << (MIN_ELEMENTS_KEYWORD - 13))
                                | (1L << (MAX_ELEMENTS_KEYWORD - 13))
                                | (1L << (MANDATORY_KEYWORD - 13))
                                | (1L << (DEFAULT_KEYWORD - 13)) | (1L << (CONFIG_KEYWORD - 13)))) != 0)) {
                            {
                                setState(288);
                                switch (_input.LA(1)) {
                                case UNITS_KEYWORD: {
                                    setState(280);
                                    units_stmt();
                                }
                                    break;
                                case MUST_KEYWORD: {
                                    setState(281);
                                    must_stmt();
                                }
                                    break;
                                case UNIQUE_KEYWORD: {
                                    setState(282);
                                    unique_stmt();
                                }
                                    break;
                                case DEFAULT_KEYWORD: {
                                    setState(283);
                                    default_stmt();
                                }
                                    break;
                                case CONFIG_KEYWORD: {
                                    setState(284);
                                    config_stmt();
                                }
                                    break;
                                case MANDATORY_KEYWORD: {
                                    setState(285);
                                    mandatory_stmt();
                                }
                                    break;
                                case MIN_ELEMENTS_KEYWORD: {
                                    setState(286);
                                    min_elements_stmt();
                                }
                                    break;
                                case MAX_ELEMENTS_KEYWORD: {
                                    setState(287);
                                    max_elements_stmt();
                                }
                                    break;
                                default:
                                    throw new NoViableAltException(this);
                                }
                            }
                            setState(292);
                            _errHandler.sync(this);
                            _la = _input.LA(1);
                        }
                        setState(293);
                        match(RIGHT_BRACE);
                    }
                }
                    break;
                default:
                    throw new NoViableAltException(this);
                }
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    public static class Deviate_not_supported_stmtContext extends
            ParserRuleContext {
        public TerminalNode RIGHT_BRACE() {
            return getToken(YangParser.RIGHT_BRACE, 0);
        }

        public TerminalNode SEMICOLON() {
            return getToken(YangParser.SEMICOLON, 0);
        }

        public Identifier_stmtContext identifier_stmt() {
            return getRuleContext(Identifier_stmtContext.class, 0);
        }

        public StringContext string() {
            return getRuleContext(StringContext.class, 0);
        }

        public TerminalNode DEVIATE_KEYWORD() {
            return getToken(YangParser.DEVIATE_KEYWORD, 0);
        }

        public TerminalNode LEFT_BRACE() {
            return getToken(YangParser.LEFT_BRACE, 0);
        }

        public Deviate_not_supported_stmtContext(ParserRuleContext parent,
                int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_deviate_not_supported_stmt;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener)
                        .enterDeviate_not_supported_stmt(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener)
                        .exitDeviate_not_supported_stmt(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof YangParserVisitor)
                return ((YangParserVisitor<? extends T>) visitor)
                        .visitDeviate_not_supported_stmt(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final Deviate_not_supported_stmtContext deviate_not_supported_stmt()
            throws RecognitionException {
        Deviate_not_supported_stmtContext _localctx = new Deviate_not_supported_stmtContext(
                _ctx, getState());
        enterRule(_localctx, 14, RULE_deviate_not_supported_stmt);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(296);
                match(DEVIATE_KEYWORD);
                setState(297);
                string();
                setState(304);
                switch (_input.LA(1)) {
                case SEMICOLON: {
                    setState(298);
                    match(SEMICOLON);
                }
                    break;
                case LEFT_BRACE: {
                    {
                        setState(299);
                        match(LEFT_BRACE);
                        setState(301);
                        _la = _input.LA(1);
                        if (_la == IDENTIFIER) {
                            {
                                setState(300);
                                identifier_stmt();
                            }
                        }

                        setState(303);
                        match(RIGHT_BRACE);
                    }
                }
                    break;
                default:
                    throw new NoViableAltException(this);
                }
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    public static class Deviation_stmtContext extends ParserRuleContext {
        public TerminalNode RIGHT_BRACE() {
            return getToken(YangParser.RIGHT_BRACE, 0);
        }

        public List<Reference_stmtContext> reference_stmt() {
            return getRuleContexts(Reference_stmtContext.class);
        }

        public Description_stmtContext description_stmt(int i) {
            return getRuleContext(Description_stmtContext.class, i);
        }

        public TerminalNode DEVIATION_KEYWORD() {
            return getToken(YangParser.DEVIATION_KEYWORD, 0);
        }

        public Deviate_replace_stmtContext deviate_replace_stmt(int i) {
            return getRuleContext(Deviate_replace_stmtContext.class, i);
        }

        public TerminalNode LEFT_BRACE() {
            return getToken(YangParser.LEFT_BRACE, 0);
        }

        public Deviate_delete_stmtContext deviate_delete_stmt(int i) {
            return getRuleContext(Deviate_delete_stmtContext.class, i);
        }

        public List<Deviate_delete_stmtContext> deviate_delete_stmt() {
            return getRuleContexts(Deviate_delete_stmtContext.class);
        }

        public Deviate_add_stmtContext deviate_add_stmt(int i) {
            return getRuleContext(Deviate_add_stmtContext.class, i);
        }

        public List<Deviate_add_stmtContext> deviate_add_stmt() {
            return getRuleContexts(Deviate_add_stmtContext.class);
        }

        public StringContext string() {
            return getRuleContext(StringContext.class, 0);
        }

        public List<Deviate_replace_stmtContext> deviate_replace_stmt() {
            return getRuleContexts(Deviate_replace_stmtContext.class);
        }

        public List<Description_stmtContext> description_stmt() {
            return getRuleContexts(Description_stmtContext.class);
        }

        public List<Deviate_not_supported_stmtContext> deviate_not_supported_stmt() {
            return getRuleContexts(Deviate_not_supported_stmtContext.class);
        }

        public Deviate_not_supported_stmtContext deviate_not_supported_stmt(
                int i) {
            return getRuleContext(Deviate_not_supported_stmtContext.class, i);
        }

        public Reference_stmtContext reference_stmt(int i) {
            return getRuleContext(Reference_stmtContext.class, i);
        }

        public Deviation_stmtContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_deviation_stmt;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).enterDeviation_stmt(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).exitDeviation_stmt(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof YangParserVisitor)
                return ((YangParserVisitor<? extends T>) visitor)
                        .visitDeviation_stmt(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final Deviation_stmtContext deviation_stmt()
            throws RecognitionException {
        Deviation_stmtContext _localctx = new Deviation_stmtContext(_ctx,
                getState());
        enterRule(_localctx, 16, RULE_deviation_stmt);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(306);
                match(DEVIATION_KEYWORD);
                setState(307);
                string();
                setState(308);
                match(LEFT_BRACE);
                setState(315);
                _errHandler.sync(this);
                _la = _input.LA(1);
                do {
                    {
                        setState(315);
                        switch (getInterpreter().adaptivePredict(_input, 17,
                                _ctx)) {
                        case 1: {
                            setState(309);
                            description_stmt();
                        }
                            break;

                        case 2: {
                            setState(310);
                            reference_stmt();
                        }
                            break;

                        case 3: {
                            setState(311);
                            deviate_not_supported_stmt();
                        }
                            break;

                        case 4: {
                            setState(312);
                            deviate_add_stmt();
                        }
                            break;

                        case 5: {
                            setState(313);
                            deviate_replace_stmt();
                        }
                            break;

                        case 6: {
                            setState(314);
                            deviate_delete_stmt();
                        }
                            break;
                        }
                    }
                    setState(317);
                    _errHandler.sync(this);
                    _la = _input.LA(1);
                } while ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << REFERENCE_KEYWORD)
                        | (1L << DEVIATE_KEYWORD) | (1L << DESCRIPTION_KEYWORD))) != 0));
                setState(319);
                match(RIGHT_BRACE);
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    public static class Notification_stmtContext extends ParserRuleContext {
        public List<Grouping_stmtContext> grouping_stmt() {
            return getRuleContexts(Grouping_stmtContext.class);
        }

        public TerminalNode RIGHT_BRACE() {
            return getToken(YangParser.RIGHT_BRACE, 0);
        }

        public List<Reference_stmtContext> reference_stmt() {
            return getRuleContexts(Reference_stmtContext.class);
        }

        public Typedef_stmtContext typedef_stmt(int i) {
            return getRuleContext(Typedef_stmtContext.class, i);
        }

        public Description_stmtContext description_stmt(int i) {
            return getRuleContext(Description_stmtContext.class, i);
        }

        public Grouping_stmtContext grouping_stmt(int i) {
            return getRuleContext(Grouping_stmtContext.class, i);
        }

        public If_feature_stmtContext if_feature_stmt(int i) {
            return getRuleContext(If_feature_stmtContext.class, i);
        }

        public TerminalNode LEFT_BRACE() {
            return getToken(YangParser.LEFT_BRACE, 0);
        }

        public Data_def_stmtContext data_def_stmt(int i) {
            return getRuleContext(Data_def_stmtContext.class, i);
        }

        public List<Typedef_stmtContext> typedef_stmt() {
            return getRuleContexts(Typedef_stmtContext.class);
        }

        public Status_stmtContext status_stmt(int i) {
            return getRuleContext(Status_stmtContext.class, i);
        }

        public List<If_feature_stmtContext> if_feature_stmt() {
            return getRuleContexts(If_feature_stmtContext.class);
        }

        public List<Data_def_stmtContext> data_def_stmt() {
            return getRuleContexts(Data_def_stmtContext.class);
        }

        public TerminalNode SEMICOLON() {
            return getToken(YangParser.SEMICOLON, 0);
        }

        public List<Status_stmtContext> status_stmt() {
            return getRuleContexts(Status_stmtContext.class);
        }

        public StringContext string() {
            return getRuleContext(StringContext.class, 0);
        }

        public List<Description_stmtContext> description_stmt() {
            return getRuleContexts(Description_stmtContext.class);
        }

        public Reference_stmtContext reference_stmt(int i) {
            return getRuleContext(Reference_stmtContext.class, i);
        }

        public TerminalNode NOTIFICATION_KEYWORD() {
            return getToken(YangParser.NOTIFICATION_KEYWORD, 0);
        }

        public Notification_stmtContext(ParserRuleContext parent,
                int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_notification_stmt;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).enterNotification_stmt(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).exitNotification_stmt(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof YangParserVisitor)
                return ((YangParserVisitor<? extends T>) visitor)
                        .visitNotification_stmt(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final Notification_stmtContext notification_stmt()
            throws RecognitionException {
        Notification_stmtContext _localctx = new Notification_stmtContext(_ctx,
                getState());
        enterRule(_localctx, 18, RULE_notification_stmt);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(321);
                match(NOTIFICATION_KEYWORD);
                setState(322);
                string();
                setState(338);
                switch (_input.LA(1)) {
                case SEMICOLON: {
                    setState(323);
                    match(SEMICOLON);
                }
                    break;
                case LEFT_BRACE: {
                    {
                        setState(324);
                        match(LEFT_BRACE);
                        setState(334);
                        _errHandler.sync(this);
                        _la = _input.LA(1);
                        while (((((_la - 12)) & ~0x3f) == 0 && ((1L << (_la - 12)) & ((1L << (USES_KEYWORD - 12))
                                | (1L << (TYPEDEF_KEYWORD - 12))
                                | (1L << (STATUS_KEYWORD - 12))
                                | (1L << (REFERENCE_KEYWORD - 12))
                                | (1L << (LIST_KEYWORD - 12))
                                | (1L << (LEAF_LIST_KEYWORD - 12))
                                | (1L << (LEAF_KEYWORD - 12))
                                | (1L << (IF_FEATURE_KEYWORD - 12))
                                | (1L << (GROUPING_KEYWORD - 12))
                                | (1L << (DESCRIPTION_KEYWORD - 12))
                                | (1L << (CONTAINER_KEYWORD - 12))
                                | (1L << (CHOICE_KEYWORD - 12)) | (1L << (ANYXML_KEYWORD - 12)))) != 0)) {
                            {
                                setState(332);
                                switch (_input.LA(1)) {
                                case IF_FEATURE_KEYWORD: {
                                    setState(325);
                                    if_feature_stmt();
                                }
                                    break;
                                case STATUS_KEYWORD: {
                                    setState(326);
                                    status_stmt();
                                }
                                    break;
                                case DESCRIPTION_KEYWORD: {
                                    setState(327);
                                    description_stmt();
                                }
                                    break;
                                case REFERENCE_KEYWORD: {
                                    setState(328);
                                    reference_stmt();
                                }
                                    break;
                                case TYPEDEF_KEYWORD: {
                                    setState(329);
                                    typedef_stmt();
                                }
                                    break;
                                case GROUPING_KEYWORD: {
                                    setState(330);
                                    grouping_stmt();
                                }
                                    break;
                                case USES_KEYWORD:
                                case LIST_KEYWORD:
                                case LEAF_LIST_KEYWORD:
                                case LEAF_KEYWORD:
                                case CONTAINER_KEYWORD:
                                case CHOICE_KEYWORD:
                                case ANYXML_KEYWORD: {
                                    setState(331);
                                    data_def_stmt();
                                }
                                    break;
                                default:
                                    throw new NoViableAltException(this);
                                }
                            }
                            setState(336);
                            _errHandler.sync(this);
                            _la = _input.LA(1);
                        }
                        setState(337);
                        match(RIGHT_BRACE);
                    }
                }
                    break;
                default:
                    throw new NoViableAltException(this);
                }
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    public static class Output_stmtContext extends ParserRuleContext {
        public List<Grouping_stmtContext> grouping_stmt() {
            return getRuleContexts(Grouping_stmtContext.class);
        }

        public TerminalNode RIGHT_BRACE() {
            return getToken(YangParser.RIGHT_BRACE, 0);
        }

        public Typedef_stmtContext typedef_stmt(int i) {
            return getRuleContext(Typedef_stmtContext.class, i);
        }

        public List<Data_def_stmtContext> data_def_stmt() {
            return getRuleContexts(Data_def_stmtContext.class);
        }

        public Grouping_stmtContext grouping_stmt(int i) {
            return getRuleContext(Grouping_stmtContext.class, i);
        }

        public TerminalNode OUTPUT_KEYWORD() {
            return getToken(YangParser.OUTPUT_KEYWORD, 0);
        }

        public TerminalNode LEFT_BRACE() {
            return getToken(YangParser.LEFT_BRACE, 0);
        }

        public Data_def_stmtContext data_def_stmt(int i) {
            return getRuleContext(Data_def_stmtContext.class, i);
        }

        public List<Typedef_stmtContext> typedef_stmt() {
            return getRuleContexts(Typedef_stmtContext.class);
        }

        public Output_stmtContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_output_stmt;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).enterOutput_stmt(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).exitOutput_stmt(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof YangParserVisitor)
                return ((YangParserVisitor<? extends T>) visitor)
                        .visitOutput_stmt(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final Output_stmtContext output_stmt() throws RecognitionException {
        Output_stmtContext _localctx = new Output_stmtContext(_ctx, getState());
        enterRule(_localctx, 20, RULE_output_stmt);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(340);
                match(OUTPUT_KEYWORD);
                setState(341);
                match(LEFT_BRACE);
                setState(345);
                _errHandler.sync(this);
                _la = _input.LA(1);
                do {
                    {
                        setState(345);
                        switch (_input.LA(1)) {
                        case TYPEDEF_KEYWORD: {
                            setState(342);
                            typedef_stmt();
                        }
                            break;
                        case GROUPING_KEYWORD: {
                            setState(343);
                            grouping_stmt();
                        }
                            break;
                        case USES_KEYWORD:
                        case LIST_KEYWORD:
                        case LEAF_LIST_KEYWORD:
                        case LEAF_KEYWORD:
                        case CONTAINER_KEYWORD:
                        case CHOICE_KEYWORD:
                        case ANYXML_KEYWORD: {
                            setState(344);
                            data_def_stmt();
                        }
                            break;
                        default:
                            throw new NoViableAltException(this);
                        }
                    }
                    setState(347);
                    _errHandler.sync(this);
                    _la = _input.LA(1);
                } while (((((_la - 12)) & ~0x3f) == 0 && ((1L << (_la - 12)) & ((1L << (USES_KEYWORD - 12))
                        | (1L << (TYPEDEF_KEYWORD - 12))
                        | (1L << (LIST_KEYWORD - 12))
                        | (1L << (LEAF_LIST_KEYWORD - 12))
                        | (1L << (LEAF_KEYWORD - 12))
                        | (1L << (GROUPING_KEYWORD - 12))
                        | (1L << (CONTAINER_KEYWORD - 12))
                        | (1L << (CHOICE_KEYWORD - 12)) | (1L << (ANYXML_KEYWORD - 12)))) != 0));
                setState(349);
                match(RIGHT_BRACE);
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    public static class Input_stmtContext extends ParserRuleContext {
        public List<Grouping_stmtContext> grouping_stmt() {
            return getRuleContexts(Grouping_stmtContext.class);
        }

        public TerminalNode RIGHT_BRACE() {
            return getToken(YangParser.RIGHT_BRACE, 0);
        }

        public Typedef_stmtContext typedef_stmt(int i) {
            return getRuleContext(Typedef_stmtContext.class, i);
        }

        public TerminalNode INPUT_KEYWORD() {
            return getToken(YangParser.INPUT_KEYWORD, 0);
        }

        public List<Data_def_stmtContext> data_def_stmt() {
            return getRuleContexts(Data_def_stmtContext.class);
        }

        public Grouping_stmtContext grouping_stmt(int i) {
            return getRuleContext(Grouping_stmtContext.class, i);
        }

        public TerminalNode LEFT_BRACE() {
            return getToken(YangParser.LEFT_BRACE, 0);
        }

        public Data_def_stmtContext data_def_stmt(int i) {
            return getRuleContext(Data_def_stmtContext.class, i);
        }

        public List<Typedef_stmtContext> typedef_stmt() {
            return getRuleContexts(Typedef_stmtContext.class);
        }

        public Input_stmtContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_input_stmt;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).enterInput_stmt(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).exitInput_stmt(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof YangParserVisitor)
                return ((YangParserVisitor<? extends T>) visitor)
                        .visitInput_stmt(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final Input_stmtContext input_stmt() throws RecognitionException {
        Input_stmtContext _localctx = new Input_stmtContext(_ctx, getState());
        enterRule(_localctx, 22, RULE_input_stmt);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(351);
                match(INPUT_KEYWORD);
                setState(352);
                match(LEFT_BRACE);
                setState(356);
                _errHandler.sync(this);
                _la = _input.LA(1);
                do {
                    {
                        setState(356);
                        switch (_input.LA(1)) {
                        case TYPEDEF_KEYWORD: {
                            setState(353);
                            typedef_stmt();
                        }
                            break;
                        case GROUPING_KEYWORD: {
                            setState(354);
                            grouping_stmt();
                        }
                            break;
                        case USES_KEYWORD:
                        case LIST_KEYWORD:
                        case LEAF_LIST_KEYWORD:
                        case LEAF_KEYWORD:
                        case CONTAINER_KEYWORD:
                        case CHOICE_KEYWORD:
                        case ANYXML_KEYWORD: {
                            setState(355);
                            data_def_stmt();
                        }
                            break;
                        default:
                            throw new NoViableAltException(this);
                        }
                    }
                    setState(358);
                    _errHandler.sync(this);
                    _la = _input.LA(1);
                } while (((((_la - 12)) & ~0x3f) == 0 && ((1L << (_la - 12)) & ((1L << (USES_KEYWORD - 12))
                        | (1L << (TYPEDEF_KEYWORD - 12))
                        | (1L << (LIST_KEYWORD - 12))
                        | (1L << (LEAF_LIST_KEYWORD - 12))
                        | (1L << (LEAF_KEYWORD - 12))
                        | (1L << (GROUPING_KEYWORD - 12))
                        | (1L << (CONTAINER_KEYWORD - 12))
                        | (1L << (CHOICE_KEYWORD - 12)) | (1L << (ANYXML_KEYWORD - 12)))) != 0));
                setState(360);
                match(RIGHT_BRACE);
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    public static class Rpc_stmtContext extends ParserRuleContext {
        public List<Grouping_stmtContext> grouping_stmt() {
            return getRuleContexts(Grouping_stmtContext.class);
        }

        public TerminalNode RIGHT_BRACE() {
            return getToken(YangParser.RIGHT_BRACE, 0);
        }

        public Output_stmtContext output_stmt(int i) {
            return getRuleContext(Output_stmtContext.class, i);
        }

        public List<Reference_stmtContext> reference_stmt() {
            return getRuleContexts(Reference_stmtContext.class);
        }

        public Typedef_stmtContext typedef_stmt(int i) {
            return getRuleContext(Typedef_stmtContext.class, i);
        }

        public Description_stmtContext description_stmt(int i) {
            return getRuleContext(Description_stmtContext.class, i);
        }

        public Grouping_stmtContext grouping_stmt(int i) {
            return getRuleContext(Grouping_stmtContext.class, i);
        }

        public Input_stmtContext input_stmt(int i) {
            return getRuleContext(Input_stmtContext.class, i);
        }

        public List<Input_stmtContext> input_stmt() {
            return getRuleContexts(Input_stmtContext.class);
        }

        public If_feature_stmtContext if_feature_stmt(int i) {
            return getRuleContext(If_feature_stmtContext.class, i);
        }

        public TerminalNode LEFT_BRACE() {
            return getToken(YangParser.LEFT_BRACE, 0);
        }

        public List<Typedef_stmtContext> typedef_stmt() {
            return getRuleContexts(Typedef_stmtContext.class);
        }

        public Status_stmtContext status_stmt(int i) {
            return getRuleContext(Status_stmtContext.class, i);
        }

        public List<If_feature_stmtContext> if_feature_stmt() {
            return getRuleContexts(If_feature_stmtContext.class);
        }

        public TerminalNode SEMICOLON() {
            return getToken(YangParser.SEMICOLON, 0);
        }

        public List<Status_stmtContext> status_stmt() {
            return getRuleContexts(Status_stmtContext.class);
        }

        public StringContext string() {
            return getRuleContext(StringContext.class, 0);
        }

        public List<Description_stmtContext> description_stmt() {
            return getRuleContexts(Description_stmtContext.class);
        }

        public Reference_stmtContext reference_stmt(int i) {
            return getRuleContext(Reference_stmtContext.class, i);
        }

        public List<Output_stmtContext> output_stmt() {
            return getRuleContexts(Output_stmtContext.class);
        }

        public TerminalNode RPC_KEYWORD() {
            return getToken(YangParser.RPC_KEYWORD, 0);
        }

        public Rpc_stmtContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_rpc_stmt;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).enterRpc_stmt(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).exitRpc_stmt(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof YangParserVisitor)
                return ((YangParserVisitor<? extends T>) visitor)
                        .visitRpc_stmt(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final Rpc_stmtContext rpc_stmt() throws RecognitionException {
        Rpc_stmtContext _localctx = new Rpc_stmtContext(_ctx, getState());
        enterRule(_localctx, 24, RULE_rpc_stmt);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(362);
                match(RPC_KEYWORD);
                setState(363);
                string();
                setState(380);
                switch (_input.LA(1)) {
                case SEMICOLON: {
                    setState(364);
                    match(SEMICOLON);
                }
                    break;
                case LEFT_BRACE: {
                    {
                        setState(365);
                        match(LEFT_BRACE);
                        setState(376);
                        _errHandler.sync(this);
                        _la = _input.LA(1);
                        while ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << TYPEDEF_KEYWORD)
                                | (1L << STATUS_KEYWORD)
                                | (1L << REFERENCE_KEYWORD)
                                | (1L << OUTPUT_KEYWORD)
                                | (1L << INPUT_KEYWORD)
                                | (1L << IF_FEATURE_KEYWORD)
                                | (1L << GROUPING_KEYWORD) | (1L << DESCRIPTION_KEYWORD))) != 0)) {
                            {
                                setState(374);
                                switch (_input.LA(1)) {
                                case IF_FEATURE_KEYWORD: {
                                    setState(366);
                                    if_feature_stmt();
                                }
                                    break;
                                case STATUS_KEYWORD: {
                                    setState(367);
                                    status_stmt();
                                }
                                    break;
                                case DESCRIPTION_KEYWORD: {
                                    setState(368);
                                    description_stmt();
                                }
                                    break;
                                case REFERENCE_KEYWORD: {
                                    setState(369);
                                    reference_stmt();
                                }
                                    break;
                                case TYPEDEF_KEYWORD: {
                                    setState(370);
                                    typedef_stmt();
                                }
                                    break;
                                case GROUPING_KEYWORD: {
                                    setState(371);
                                    grouping_stmt();
                                }
                                    break;
                                case INPUT_KEYWORD: {
                                    setState(372);
                                    input_stmt();
                                }
                                    break;
                                case OUTPUT_KEYWORD: {
                                    setState(373);
                                    output_stmt();
                                }
                                    break;
                                default:
                                    throw new NoViableAltException(this);
                                }
                            }
                            setState(378);
                            _errHandler.sync(this);
                            _la = _input.LA(1);
                        }
                        setState(379);
                        match(RIGHT_BRACE);
                    }
                }
                    break;
                default:
                    throw new NoViableAltException(this);
                }
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    public static class When_stmtContext extends ParserRuleContext {
        public TerminalNode RIGHT_BRACE() {
            return getToken(YangParser.RIGHT_BRACE, 0);
        }

        public List<Reference_stmtContext> reference_stmt() {
            return getRuleContexts(Reference_stmtContext.class);
        }

        public Description_stmtContext description_stmt(int i) {
            return getRuleContext(Description_stmtContext.class, i);
        }

        public TerminalNode WHEN_KEYWORD() {
            return getToken(YangParser.WHEN_KEYWORD, 0);
        }

        public TerminalNode SEMICOLON() {
            return getToken(YangParser.SEMICOLON, 0);
        }

        public StringContext string() {
            return getRuleContext(StringContext.class, 0);
        }

        public List<Description_stmtContext> description_stmt() {
            return getRuleContexts(Description_stmtContext.class);
        }

        public TerminalNode LEFT_BRACE() {
            return getToken(YangParser.LEFT_BRACE, 0);
        }

        public Reference_stmtContext reference_stmt(int i) {
            return getRuleContext(Reference_stmtContext.class, i);
        }

        public When_stmtContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_when_stmt;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).enterWhen_stmt(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).exitWhen_stmt(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof YangParserVisitor)
                return ((YangParserVisitor<? extends T>) visitor)
                        .visitWhen_stmt(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final When_stmtContext when_stmt() throws RecognitionException {
        When_stmtContext _localctx = new When_stmtContext(_ctx, getState());
        enterRule(_localctx, 26, RULE_when_stmt);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(382);
                match(WHEN_KEYWORD);
                setState(383);
                string();
                setState(394);
                switch (_input.LA(1)) {
                case SEMICOLON: {
                    setState(384);
                    match(SEMICOLON);
                }
                    break;
                case LEFT_BRACE: {
                    {
                        setState(385);
                        match(LEFT_BRACE);
                        setState(390);
                        _errHandler.sync(this);
                        _la = _input.LA(1);
                        while (_la == REFERENCE_KEYWORD
                                || _la == DESCRIPTION_KEYWORD) {
                            {
                                setState(388);
                                switch (_input.LA(1)) {
                                case DESCRIPTION_KEYWORD: {
                                    setState(386);
                                    description_stmt();
                                }
                                    break;
                                case REFERENCE_KEYWORD: {
                                    setState(387);
                                    reference_stmt();
                                }
                                    break;
                                default:
                                    throw new NoViableAltException(this);
                                }
                            }
                            setState(392);
                            _errHandler.sync(this);
                            _la = _input.LA(1);
                        }
                        setState(393);
                        match(RIGHT_BRACE);
                    }
                }
                    break;
                default:
                    throw new NoViableAltException(this);
                }
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    public static class Augment_stmtContext extends ParserRuleContext {
        public TerminalNode RIGHT_BRACE() {
            return getToken(YangParser.RIGHT_BRACE, 0);
        }

        public List<Reference_stmtContext> reference_stmt() {
            return getRuleContexts(Reference_stmtContext.class);
        }

        public Description_stmtContext description_stmt(int i) {
            return getRuleContext(Description_stmtContext.class, i);
        }

        public List<When_stmtContext> when_stmt() {
            return getRuleContexts(When_stmtContext.class);
        }

        public List<Case_stmtContext> case_stmt() {
            return getRuleContexts(Case_stmtContext.class);
        }

        public TerminalNode AUGMENT_KEYWORD() {
            return getToken(YangParser.AUGMENT_KEYWORD, 0);
        }

        public If_feature_stmtContext if_feature_stmt(int i) {
            return getRuleContext(If_feature_stmtContext.class, i);
        }

        public TerminalNode LEFT_BRACE() {
            return getToken(YangParser.LEFT_BRACE, 0);
        }

        public Data_def_stmtContext data_def_stmt(int i) {
            return getRuleContext(Data_def_stmtContext.class, i);
        }

        public Status_stmtContext status_stmt(int i) {
            return getRuleContext(Status_stmtContext.class, i);
        }

        public List<If_feature_stmtContext> if_feature_stmt() {
            return getRuleContexts(If_feature_stmtContext.class);
        }

        public Identifier_stmtContext identifier_stmt(int i) {
            return getRuleContext(Identifier_stmtContext.class, i);
        }

        public List<Data_def_stmtContext> data_def_stmt() {
            return getRuleContexts(Data_def_stmtContext.class);
        }

        public List<Status_stmtContext> status_stmt() {
            return getRuleContexts(Status_stmtContext.class);
        }

        public StringContext string() {
            return getRuleContext(StringContext.class, 0);
        }

        public List<Identifier_stmtContext> identifier_stmt() {
            return getRuleContexts(Identifier_stmtContext.class);
        }

        public List<Description_stmtContext> description_stmt() {
            return getRuleContexts(Description_stmtContext.class);
        }

        public Reference_stmtContext reference_stmt(int i) {
            return getRuleContext(Reference_stmtContext.class, i);
        }

        public When_stmtContext when_stmt(int i) {
            return getRuleContext(When_stmtContext.class, i);
        }

        public Case_stmtContext case_stmt(int i) {
            return getRuleContext(Case_stmtContext.class, i);
        }

        public Augment_stmtContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_augment_stmt;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).enterAugment_stmt(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).exitAugment_stmt(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof YangParserVisitor)
                return ((YangParserVisitor<? extends T>) visitor)
                        .visitAugment_stmt(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final Augment_stmtContext augment_stmt() throws RecognitionException {
        Augment_stmtContext _localctx = new Augment_stmtContext(_ctx,
                getState());
        enterRule(_localctx, 28, RULE_augment_stmt);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(396);
                match(AUGMENT_KEYWORD);
                setState(397);
                string();
                setState(398);
                match(LEFT_BRACE);
                setState(407);
                _errHandler.sync(this);
                _la = _input.LA(1);
                do {
                    {
                        setState(407);
                        switch (_input.LA(1)) {
                        case IDENTIFIER: {
                            setState(399);
                            identifier_stmt();
                        }
                            break;
                        case WHEN_KEYWORD: {
                            setState(400);
                            when_stmt();
                        }
                            break;
                        case IF_FEATURE_KEYWORD: {
                            setState(401);
                            if_feature_stmt();
                        }
                            break;
                        case STATUS_KEYWORD: {
                            setState(402);
                            status_stmt();
                        }
                            break;
                        case DESCRIPTION_KEYWORD: {
                            setState(403);
                            description_stmt();
                        }
                            break;
                        case REFERENCE_KEYWORD: {
                            setState(404);
                            reference_stmt();
                        }
                            break;
                        case USES_KEYWORD:
                        case LIST_KEYWORD:
                        case LEAF_LIST_KEYWORD:
                        case LEAF_KEYWORD:
                        case CONTAINER_KEYWORD:
                        case CHOICE_KEYWORD:
                        case ANYXML_KEYWORD: {
                            setState(405);
                            data_def_stmt();
                        }
                            break;
                        case CASE_KEYWORD: {
                            setState(406);
                            case_stmt();
                        }
                            break;
                        default:
                            throw new NoViableAltException(this);
                        }
                    }
                    setState(409);
                    _errHandler.sync(this);
                    _la = _input.LA(1);
                } while (((((_la - 10)) & ~0x3f) == 0 && ((1L << (_la - 10)) & ((1L << (WHEN_KEYWORD - 10))
                        | (1L << (USES_KEYWORD - 10))
                        | (1L << (STATUS_KEYWORD - 10))
                        | (1L << (REFERENCE_KEYWORD - 10))
                        | (1L << (LIST_KEYWORD - 10))
                        | (1L << (LEAF_LIST_KEYWORD - 10))
                        | (1L << (LEAF_KEYWORD - 10))
                        | (1L << (IF_FEATURE_KEYWORD - 10))
                        | (1L << (DESCRIPTION_KEYWORD - 10))
                        | (1L << (CONTAINER_KEYWORD - 10))
                        | (1L << (CHOICE_KEYWORD - 10))
                        | (1L << (CASE_KEYWORD - 10))
                        | (1L << (ANYXML_KEYWORD - 10)) | (1L << (IDENTIFIER - 10)))) != 0));
                setState(411);
                match(RIGHT_BRACE);
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    public static class Uses_augment_stmtContext extends ParserRuleContext {
        public TerminalNode RIGHT_BRACE() {
            return getToken(YangParser.RIGHT_BRACE, 0);
        }

        public List<Reference_stmtContext> reference_stmt() {
            return getRuleContexts(Reference_stmtContext.class);
        }

        public Description_stmtContext description_stmt(int i) {
            return getRuleContext(Description_stmtContext.class, i);
        }

        public List<When_stmtContext> when_stmt() {
            return getRuleContexts(When_stmtContext.class);
        }

        public List<Case_stmtContext> case_stmt() {
            return getRuleContexts(Case_stmtContext.class);
        }

        public TerminalNode AUGMENT_KEYWORD() {
            return getToken(YangParser.AUGMENT_KEYWORD, 0);
        }

        public If_feature_stmtContext if_feature_stmt(int i) {
            return getRuleContext(If_feature_stmtContext.class, i);
        }

        public TerminalNode LEFT_BRACE() {
            return getToken(YangParser.LEFT_BRACE, 0);
        }

        public Data_def_stmtContext data_def_stmt(int i) {
            return getRuleContext(Data_def_stmtContext.class, i);
        }

        public Status_stmtContext status_stmt(int i) {
            return getRuleContext(Status_stmtContext.class, i);
        }

        public List<If_feature_stmtContext> if_feature_stmt() {
            return getRuleContexts(If_feature_stmtContext.class);
        }

        public Identifier_stmtContext identifier_stmt(int i) {
            return getRuleContext(Identifier_stmtContext.class, i);
        }

        public List<Data_def_stmtContext> data_def_stmt() {
            return getRuleContexts(Data_def_stmtContext.class);
        }

        public List<Status_stmtContext> status_stmt() {
            return getRuleContexts(Status_stmtContext.class);
        }

        public StringContext string() {
            return getRuleContext(StringContext.class, 0);
        }

        public List<Identifier_stmtContext> identifier_stmt() {
            return getRuleContexts(Identifier_stmtContext.class);
        }

        public List<Description_stmtContext> description_stmt() {
            return getRuleContexts(Description_stmtContext.class);
        }

        public Reference_stmtContext reference_stmt(int i) {
            return getRuleContext(Reference_stmtContext.class, i);
        }

        public When_stmtContext when_stmt(int i) {
            return getRuleContext(When_stmtContext.class, i);
        }

        public Case_stmtContext case_stmt(int i) {
            return getRuleContext(Case_stmtContext.class, i);
        }

        public Uses_augment_stmtContext(ParserRuleContext parent,
                int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_uses_augment_stmt;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).enterUses_augment_stmt(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).exitUses_augment_stmt(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof YangParserVisitor)
                return ((YangParserVisitor<? extends T>) visitor)
                        .visitUses_augment_stmt(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final Uses_augment_stmtContext uses_augment_stmt()
            throws RecognitionException {
        Uses_augment_stmtContext _localctx = new Uses_augment_stmtContext(_ctx,
                getState());
        enterRule(_localctx, 30, RULE_uses_augment_stmt);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(413);
                match(AUGMENT_KEYWORD);
                setState(414);
                string();
                setState(415);
                match(LEFT_BRACE);
                setState(424);
                _errHandler.sync(this);
                _la = _input.LA(1);
                do {
                    {
                        setState(424);
                        switch (_input.LA(1)) {
                        case IDENTIFIER: {
                            setState(416);
                            identifier_stmt();
                        }
                            break;
                        case WHEN_KEYWORD: {
                            setState(417);
                            when_stmt();
                        }
                            break;
                        case IF_FEATURE_KEYWORD: {
                            setState(418);
                            if_feature_stmt();
                        }
                            break;
                        case STATUS_KEYWORD: {
                            setState(419);
                            status_stmt();
                        }
                            break;
                        case DESCRIPTION_KEYWORD: {
                            setState(420);
                            description_stmt();
                        }
                            break;
                        case REFERENCE_KEYWORD: {
                            setState(421);
                            reference_stmt();
                        }
                            break;
                        case USES_KEYWORD:
                        case LIST_KEYWORD:
                        case LEAF_LIST_KEYWORD:
                        case LEAF_KEYWORD:
                        case CONTAINER_KEYWORD:
                        case CHOICE_KEYWORD:
                        case ANYXML_KEYWORD: {
                            setState(422);
                            data_def_stmt();
                        }
                            break;
                        case CASE_KEYWORD: {
                            setState(423);
                            case_stmt();
                        }
                            break;
                        default:
                            throw new NoViableAltException(this);
                        }
                    }
                    setState(426);
                    _errHandler.sync(this);
                    _la = _input.LA(1);
                } while (((((_la - 10)) & ~0x3f) == 0 && ((1L << (_la - 10)) & ((1L << (WHEN_KEYWORD - 10))
                        | (1L << (USES_KEYWORD - 10))
                        | (1L << (STATUS_KEYWORD - 10))
                        | (1L << (REFERENCE_KEYWORD - 10))
                        | (1L << (LIST_KEYWORD - 10))
                        | (1L << (LEAF_LIST_KEYWORD - 10))
                        | (1L << (LEAF_KEYWORD - 10))
                        | (1L << (IF_FEATURE_KEYWORD - 10))
                        | (1L << (DESCRIPTION_KEYWORD - 10))
                        | (1L << (CONTAINER_KEYWORD - 10))
                        | (1L << (CHOICE_KEYWORD - 10))
                        | (1L << (CASE_KEYWORD - 10))
                        | (1L << (ANYXML_KEYWORD - 10)) | (1L << (IDENTIFIER - 10)))) != 0));
                setState(428);
                match(RIGHT_BRACE);
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    public static class Refine_anyxml_stmtsContext extends ParserRuleContext {
        public List<Reference_stmtContext> reference_stmt() {
            return getRuleContexts(Reference_stmtContext.class);
        }

        public Description_stmtContext description_stmt(int i) {
            return getRuleContext(Description_stmtContext.class, i);
        }

        public Mandatory_stmtContext mandatory_stmt(int i) {
            return getRuleContext(Mandatory_stmtContext.class, i);
        }

        public Config_stmtContext config_stmt(int i) {
            return getRuleContext(Config_stmtContext.class, i);
        }

        public List<Config_stmtContext> config_stmt() {
            return getRuleContexts(Config_stmtContext.class);
        }

        public List<Must_stmtContext> must_stmt() {
            return getRuleContexts(Must_stmtContext.class);
        }

        public List<Description_stmtContext> description_stmt() {
            return getRuleContexts(Description_stmtContext.class);
        }

        public Reference_stmtContext reference_stmt(int i) {
            return getRuleContext(Reference_stmtContext.class, i);
        }

        public List<Mandatory_stmtContext> mandatory_stmt() {
            return getRuleContexts(Mandatory_stmtContext.class);
        }

        public Must_stmtContext must_stmt(int i) {
            return getRuleContext(Must_stmtContext.class, i);
        }

        public Refine_anyxml_stmtsContext(ParserRuleContext parent,
                int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_refine_anyxml_stmts;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).enterRefine_anyxml_stmts(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).exitRefine_anyxml_stmts(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof YangParserVisitor)
                return ((YangParserVisitor<? extends T>) visitor)
                        .visitRefine_anyxml_stmts(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final Refine_anyxml_stmtsContext refine_anyxml_stmts()
            throws RecognitionException {
        Refine_anyxml_stmtsContext _localctx = new Refine_anyxml_stmtsContext(
                _ctx, getState());
        enterRule(_localctx, 32, RULE_refine_anyxml_stmts);
        try {
            int _alt;
            enterOuterAlt(_localctx, 1);
            {
                setState(437);
                _errHandler.sync(this);
                _alt = getInterpreter().adaptivePredict(_input, 37, _ctx);
                while (_alt != 2 && _alt != -1) {
                    if (_alt == 1) {
                        {
                            setState(435);
                            switch (_input.LA(1)) {
                            case MUST_KEYWORD: {
                                setState(430);
                                must_stmt();
                            }
                                break;
                            case CONFIG_KEYWORD: {
                                setState(431);
                                config_stmt();
                            }
                                break;
                            case MANDATORY_KEYWORD: {
                                setState(432);
                                mandatory_stmt();
                            }
                                break;
                            case DESCRIPTION_KEYWORD: {
                                setState(433);
                                description_stmt();
                            }
                                break;
                            case REFERENCE_KEYWORD: {
                                setState(434);
                                reference_stmt();
                            }
                                break;
                            default:
                                throw new NoViableAltException(this);
                            }
                        }
                    }
                    setState(439);
                    _errHandler.sync(this);
                    _alt = getInterpreter().adaptivePredict(_input, 37, _ctx);
                }
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    public static class Refine_case_stmtsContext extends ParserRuleContext {
        public List<Reference_stmtContext> reference_stmt() {
            return getRuleContexts(Reference_stmtContext.class);
        }

        public Description_stmtContext description_stmt(int i) {
            return getRuleContext(Description_stmtContext.class, i);
        }

        public List<Description_stmtContext> description_stmt() {
            return getRuleContexts(Description_stmtContext.class);
        }

        public Reference_stmtContext reference_stmt(int i) {
            return getRuleContext(Reference_stmtContext.class, i);
        }

        public Refine_case_stmtsContext(ParserRuleContext parent,
                int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_refine_case_stmts;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).enterRefine_case_stmts(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).exitRefine_case_stmts(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof YangParserVisitor)
                return ((YangParserVisitor<? extends T>) visitor)
                        .visitRefine_case_stmts(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final Refine_case_stmtsContext refine_case_stmts()
            throws RecognitionException {
        Refine_case_stmtsContext _localctx = new Refine_case_stmtsContext(_ctx,
                getState());
        enterRule(_localctx, 34, RULE_refine_case_stmts);
        try {
            int _alt;
            enterOuterAlt(_localctx, 1);
            {
                setState(444);
                _errHandler.sync(this);
                _alt = getInterpreter().adaptivePredict(_input, 39, _ctx);
                while (_alt != 2 && _alt != -1) {
                    if (_alt == 1) {
                        {
                            setState(442);
                            switch (_input.LA(1)) {
                            case DESCRIPTION_KEYWORD: {
                                setState(440);
                                description_stmt();
                            }
                                break;
                            case REFERENCE_KEYWORD: {
                                setState(441);
                                reference_stmt();
                            }
                                break;
                            default:
                                throw new NoViableAltException(this);
                            }
                        }
                    }
                    setState(446);
                    _errHandler.sync(this);
                    _alt = getInterpreter().adaptivePredict(_input, 39, _ctx);
                }
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    public static class Refine_choice_stmtsContext extends ParserRuleContext {
        public List<Reference_stmtContext> reference_stmt() {
            return getRuleContexts(Reference_stmtContext.class);
        }

        public List<Default_stmtContext> default_stmt() {
            return getRuleContexts(Default_stmtContext.class);
        }

        public Description_stmtContext description_stmt(int i) {
            return getRuleContext(Description_stmtContext.class, i);
        }

        public Default_stmtContext default_stmt(int i) {
            return getRuleContext(Default_stmtContext.class, i);
        }

        public Mandatory_stmtContext mandatory_stmt(int i) {
            return getRuleContext(Mandatory_stmtContext.class, i);
        }

        public Config_stmtContext config_stmt(int i) {
            return getRuleContext(Config_stmtContext.class, i);
        }

        public List<Config_stmtContext> config_stmt() {
            return getRuleContexts(Config_stmtContext.class);
        }

        public List<Description_stmtContext> description_stmt() {
            return getRuleContexts(Description_stmtContext.class);
        }

        public Reference_stmtContext reference_stmt(int i) {
            return getRuleContext(Reference_stmtContext.class, i);
        }

        public List<Mandatory_stmtContext> mandatory_stmt() {
            return getRuleContexts(Mandatory_stmtContext.class);
        }

        public Refine_choice_stmtsContext(ParserRuleContext parent,
                int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_refine_choice_stmts;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).enterRefine_choice_stmts(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).exitRefine_choice_stmts(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof YangParserVisitor)
                return ((YangParserVisitor<? extends T>) visitor)
                        .visitRefine_choice_stmts(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final Refine_choice_stmtsContext refine_choice_stmts()
            throws RecognitionException {
        Refine_choice_stmtsContext _localctx = new Refine_choice_stmtsContext(
                _ctx, getState());
        enterRule(_localctx, 36, RULE_refine_choice_stmts);
        try {
            int _alt;
            enterOuterAlt(_localctx, 1);
            {
                setState(454);
                _errHandler.sync(this);
                _alt = getInterpreter().adaptivePredict(_input, 41, _ctx);
                while (_alt != 2 && _alt != -1) {
                    if (_alt == 1) {
                        {
                            setState(452);
                            switch (_input.LA(1)) {
                            case DEFAULT_KEYWORD: {
                                setState(447);
                                default_stmt();
                            }
                                break;
                            case CONFIG_KEYWORD: {
                                setState(448);
                                config_stmt();
                            }
                                break;
                            case MANDATORY_KEYWORD: {
                                setState(449);
                                mandatory_stmt();
                            }
                                break;
                            case DESCRIPTION_KEYWORD: {
                                setState(450);
                                description_stmt();
                            }
                                break;
                            case REFERENCE_KEYWORD: {
                                setState(451);
                                reference_stmt();
                            }
                                break;
                            default:
                                throw new NoViableAltException(this);
                            }
                        }
                    }
                    setState(456);
                    _errHandler.sync(this);
                    _alt = getInterpreter().adaptivePredict(_input, 41, _ctx);
                }
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    public static class Refine_list_stmtsContext extends ParserRuleContext {
        public List<Reference_stmtContext> reference_stmt() {
            return getRuleContexts(Reference_stmtContext.class);
        }

        public Description_stmtContext description_stmt(int i) {
            return getRuleContext(Description_stmtContext.class, i);
        }

        public Config_stmtContext config_stmt(int i) {
            return getRuleContext(Config_stmtContext.class, i);
        }

        public List<Min_elements_stmtContext> min_elements_stmt() {
            return getRuleContexts(Min_elements_stmtContext.class);
        }

        public List<Max_elements_stmtContext> max_elements_stmt() {
            return getRuleContexts(Max_elements_stmtContext.class);
        }

        public List<Config_stmtContext> config_stmt() {
            return getRuleContexts(Config_stmtContext.class);
        }

        public List<Must_stmtContext> must_stmt() {
            return getRuleContexts(Must_stmtContext.class);
        }

        public Max_elements_stmtContext max_elements_stmt(int i) {
            return getRuleContext(Max_elements_stmtContext.class, i);
        }

        public List<Description_stmtContext> description_stmt() {
            return getRuleContexts(Description_stmtContext.class);
        }

        public Reference_stmtContext reference_stmt(int i) {
            return getRuleContext(Reference_stmtContext.class, i);
        }

        public Must_stmtContext must_stmt(int i) {
            return getRuleContext(Must_stmtContext.class, i);
        }

        public Min_elements_stmtContext min_elements_stmt(int i) {
            return getRuleContext(Min_elements_stmtContext.class, i);
        }

        public Refine_list_stmtsContext(ParserRuleContext parent,
                int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_refine_list_stmts;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).enterRefine_list_stmts(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).exitRefine_list_stmts(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof YangParserVisitor)
                return ((YangParserVisitor<? extends T>) visitor)
                        .visitRefine_list_stmts(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final Refine_list_stmtsContext refine_list_stmts()
            throws RecognitionException {
        Refine_list_stmtsContext _localctx = new Refine_list_stmtsContext(_ctx,
                getState());
        enterRule(_localctx, 38, RULE_refine_list_stmts);
        try {
            int _alt;
            enterOuterAlt(_localctx, 1);
            {
                setState(465);
                _errHandler.sync(this);
                _alt = getInterpreter().adaptivePredict(_input, 43, _ctx);
                while (_alt != 2 && _alt != -1) {
                    if (_alt == 1) {
                        {
                            setState(463);
                            switch (_input.LA(1)) {
                            case MUST_KEYWORD: {
                                setState(457);
                                must_stmt();
                            }
                                break;
                            case CONFIG_KEYWORD: {
                                setState(458);
                                config_stmt();
                            }
                                break;
                            case MIN_ELEMENTS_KEYWORD: {
                                setState(459);
                                min_elements_stmt();
                            }
                                break;
                            case MAX_ELEMENTS_KEYWORD: {
                                setState(460);
                                max_elements_stmt();
                            }
                                break;
                            case DESCRIPTION_KEYWORD: {
                                setState(461);
                                description_stmt();
                            }
                                break;
                            case REFERENCE_KEYWORD: {
                                setState(462);
                                reference_stmt();
                            }
                                break;
                            default:
                                throw new NoViableAltException(this);
                            }
                        }
                    }
                    setState(467);
                    _errHandler.sync(this);
                    _alt = getInterpreter().adaptivePredict(_input, 43, _ctx);
                }
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    public static class Refine_leaf_list_stmtsContext extends ParserRuleContext {
        public List<Reference_stmtContext> reference_stmt() {
            return getRuleContexts(Reference_stmtContext.class);
        }

        public Description_stmtContext description_stmt(int i) {
            return getRuleContext(Description_stmtContext.class, i);
        }

        public Config_stmtContext config_stmt(int i) {
            return getRuleContext(Config_stmtContext.class, i);
        }

        public List<Min_elements_stmtContext> min_elements_stmt() {
            return getRuleContexts(Min_elements_stmtContext.class);
        }

        public List<Max_elements_stmtContext> max_elements_stmt() {
            return getRuleContexts(Max_elements_stmtContext.class);
        }

        public List<Config_stmtContext> config_stmt() {
            return getRuleContexts(Config_stmtContext.class);
        }

        public List<Must_stmtContext> must_stmt() {
            return getRuleContexts(Must_stmtContext.class);
        }

        public Max_elements_stmtContext max_elements_stmt(int i) {
            return getRuleContext(Max_elements_stmtContext.class, i);
        }

        public List<Description_stmtContext> description_stmt() {
            return getRuleContexts(Description_stmtContext.class);
        }

        public Reference_stmtContext reference_stmt(int i) {
            return getRuleContext(Reference_stmtContext.class, i);
        }

        public Must_stmtContext must_stmt(int i) {
            return getRuleContext(Must_stmtContext.class, i);
        }

        public Min_elements_stmtContext min_elements_stmt(int i) {
            return getRuleContext(Min_elements_stmtContext.class, i);
        }

        public Refine_leaf_list_stmtsContext(ParserRuleContext parent,
                int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_refine_leaf_list_stmts;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener)
                        .enterRefine_leaf_list_stmts(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener)
                        .exitRefine_leaf_list_stmts(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof YangParserVisitor)
                return ((YangParserVisitor<? extends T>) visitor)
                        .visitRefine_leaf_list_stmts(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final Refine_leaf_list_stmtsContext refine_leaf_list_stmts()
            throws RecognitionException {
        Refine_leaf_list_stmtsContext _localctx = new Refine_leaf_list_stmtsContext(
                _ctx, getState());
        enterRule(_localctx, 40, RULE_refine_leaf_list_stmts);
        try {
            int _alt;
            enterOuterAlt(_localctx, 1);
            {
                setState(476);
                _errHandler.sync(this);
                _alt = getInterpreter().adaptivePredict(_input, 45, _ctx);
                while (_alt != 2 && _alt != -1) {
                    if (_alt == 1) {
                        {
                            setState(474);
                            switch (_input.LA(1)) {
                            case MUST_KEYWORD: {
                                setState(468);
                                must_stmt();
                            }
                                break;
                            case CONFIG_KEYWORD: {
                                setState(469);
                                config_stmt();
                            }
                                break;
                            case MIN_ELEMENTS_KEYWORD: {
                                setState(470);
                                min_elements_stmt();
                            }
                                break;
                            case MAX_ELEMENTS_KEYWORD: {
                                setState(471);
                                max_elements_stmt();
                            }
                                break;
                            case DESCRIPTION_KEYWORD: {
                                setState(472);
                                description_stmt();
                            }
                                break;
                            case REFERENCE_KEYWORD: {
                                setState(473);
                                reference_stmt();
                            }
                                break;
                            default:
                                throw new NoViableAltException(this);
                            }
                        }
                    }
                    setState(478);
                    _errHandler.sync(this);
                    _alt = getInterpreter().adaptivePredict(_input, 45, _ctx);
                }
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    public static class Refine_leaf_stmtsContext extends ParserRuleContext {
        public List<Reference_stmtContext> reference_stmt() {
            return getRuleContexts(Reference_stmtContext.class);
        }

        public List<Default_stmtContext> default_stmt() {
            return getRuleContexts(Default_stmtContext.class);
        }

        public Description_stmtContext description_stmt(int i) {
            return getRuleContext(Description_stmtContext.class, i);
        }

        public Default_stmtContext default_stmt(int i) {
            return getRuleContext(Default_stmtContext.class, i);
        }

        public Mandatory_stmtContext mandatory_stmt(int i) {
            return getRuleContext(Mandatory_stmtContext.class, i);
        }

        public Config_stmtContext config_stmt(int i) {
            return getRuleContext(Config_stmtContext.class, i);
        }

        public List<Config_stmtContext> config_stmt() {
            return getRuleContexts(Config_stmtContext.class);
        }

        public List<Must_stmtContext> must_stmt() {
            return getRuleContexts(Must_stmtContext.class);
        }

        public List<Description_stmtContext> description_stmt() {
            return getRuleContexts(Description_stmtContext.class);
        }

        public Reference_stmtContext reference_stmt(int i) {
            return getRuleContext(Reference_stmtContext.class, i);
        }

        public List<Mandatory_stmtContext> mandatory_stmt() {
            return getRuleContexts(Mandatory_stmtContext.class);
        }

        public Must_stmtContext must_stmt(int i) {
            return getRuleContext(Must_stmtContext.class, i);
        }

        public Refine_leaf_stmtsContext(ParserRuleContext parent,
                int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_refine_leaf_stmts;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).enterRefine_leaf_stmts(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).exitRefine_leaf_stmts(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof YangParserVisitor)
                return ((YangParserVisitor<? extends T>) visitor)
                        .visitRefine_leaf_stmts(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final Refine_leaf_stmtsContext refine_leaf_stmts()
            throws RecognitionException {
        Refine_leaf_stmtsContext _localctx = new Refine_leaf_stmtsContext(_ctx,
                getState());
        enterRule(_localctx, 42, RULE_refine_leaf_stmts);
        try {
            int _alt;
            enterOuterAlt(_localctx, 1);
            {
                setState(487);
                _errHandler.sync(this);
                _alt = getInterpreter().adaptivePredict(_input, 47, _ctx);
                while (_alt != 2 && _alt != -1) {
                    if (_alt == 1) {
                        {
                            setState(485);
                            switch (_input.LA(1)) {
                            case MUST_KEYWORD: {
                                setState(479);
                                must_stmt();
                            }
                                break;
                            case DEFAULT_KEYWORD: {
                                setState(480);
                                default_stmt();
                            }
                                break;
                            case CONFIG_KEYWORD: {
                                setState(481);
                                config_stmt();
                            }
                                break;
                            case MANDATORY_KEYWORD: {
                                setState(482);
                                mandatory_stmt();
                            }
                                break;
                            case DESCRIPTION_KEYWORD: {
                                setState(483);
                                description_stmt();
                            }
                                break;
                            case REFERENCE_KEYWORD: {
                                setState(484);
                                reference_stmt();
                            }
                                break;
                            default:
                                throw new NoViableAltException(this);
                            }
                        }
                    }
                    setState(489);
                    _errHandler.sync(this);
                    _alt = getInterpreter().adaptivePredict(_input, 47, _ctx);
                }
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    public static class Refine_container_stmtsContext extends ParserRuleContext {
        public List<Reference_stmtContext> reference_stmt() {
            return getRuleContexts(Reference_stmtContext.class);
        }

        public List<Presence_stmtContext> presence_stmt() {
            return getRuleContexts(Presence_stmtContext.class);
        }

        public Description_stmtContext description_stmt(int i) {
            return getRuleContext(Description_stmtContext.class, i);
        }

        public Config_stmtContext config_stmt(int i) {
            return getRuleContext(Config_stmtContext.class, i);
        }

        public Presence_stmtContext presence_stmt(int i) {
            return getRuleContext(Presence_stmtContext.class, i);
        }

        public List<Config_stmtContext> config_stmt() {
            return getRuleContexts(Config_stmtContext.class);
        }

        public List<Must_stmtContext> must_stmt() {
            return getRuleContexts(Must_stmtContext.class);
        }

        public List<Description_stmtContext> description_stmt() {
            return getRuleContexts(Description_stmtContext.class);
        }

        public Reference_stmtContext reference_stmt(int i) {
            return getRuleContext(Reference_stmtContext.class, i);
        }

        public Must_stmtContext must_stmt(int i) {
            return getRuleContext(Must_stmtContext.class, i);
        }

        public Refine_container_stmtsContext(ParserRuleContext parent,
                int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_refine_container_stmts;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener)
                        .enterRefine_container_stmts(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener)
                        .exitRefine_container_stmts(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof YangParserVisitor)
                return ((YangParserVisitor<? extends T>) visitor)
                        .visitRefine_container_stmts(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final Refine_container_stmtsContext refine_container_stmts()
            throws RecognitionException {
        Refine_container_stmtsContext _localctx = new Refine_container_stmtsContext(
                _ctx, getState());
        enterRule(_localctx, 44, RULE_refine_container_stmts);
        try {
            int _alt;
            enterOuterAlt(_localctx, 1);
            {
                setState(497);
                _errHandler.sync(this);
                _alt = getInterpreter().adaptivePredict(_input, 49, _ctx);
                while (_alt != 2 && _alt != -1) {
                    if (_alt == 1) {
                        {
                            setState(495);
                            switch (_input.LA(1)) {
                            case MUST_KEYWORD: {
                                setState(490);
                                must_stmt();
                            }
                                break;
                            case PRESENCE_KEYWORD: {
                                setState(491);
                                presence_stmt();
                            }
                                break;
                            case CONFIG_KEYWORD: {
                                setState(492);
                                config_stmt();
                            }
                                break;
                            case DESCRIPTION_KEYWORD: {
                                setState(493);
                                description_stmt();
                            }
                                break;
                            case REFERENCE_KEYWORD: {
                                setState(494);
                                reference_stmt();
                            }
                                break;
                            default:
                                throw new NoViableAltException(this);
                            }
                        }
                    }
                    setState(499);
                    _errHandler.sync(this);
                    _alt = getInterpreter().adaptivePredict(_input, 49, _ctx);
                }
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    public static class Refune_pomContext extends ParserRuleContext {
        public Refine_list_stmtsContext refine_list_stmts() {
            return getRuleContext(Refine_list_stmtsContext.class, 0);
        }

        public Refine_choice_stmtsContext refine_choice_stmts() {
            return getRuleContext(Refine_choice_stmtsContext.class, 0);
        }

        public Refine_leaf_list_stmtsContext refine_leaf_list_stmts() {
            return getRuleContext(Refine_leaf_list_stmtsContext.class, 0);
        }

        public Refine_case_stmtsContext refine_case_stmts() {
            return getRuleContext(Refine_case_stmtsContext.class, 0);
        }

        public Refine_leaf_stmtsContext refine_leaf_stmts() {
            return getRuleContext(Refine_leaf_stmtsContext.class, 0);
        }

        public Refine_anyxml_stmtsContext refine_anyxml_stmts() {
            return getRuleContext(Refine_anyxml_stmtsContext.class, 0);
        }

        public Refine_container_stmtsContext refine_container_stmts() {
            return getRuleContext(Refine_container_stmtsContext.class, 0);
        }

        public Refune_pomContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_refune_pom;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).enterRefune_pom(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).exitRefune_pom(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof YangParserVisitor)
                return ((YangParserVisitor<? extends T>) visitor)
                        .visitRefune_pom(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final Refune_pomContext refune_pom() throws RecognitionException {
        Refune_pomContext _localctx = new Refune_pomContext(_ctx, getState());
        enterRule(_localctx, 46, RULE_refune_pom);
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(507);
                switch (getInterpreter().adaptivePredict(_input, 50, _ctx)) {
                case 1: {
                    setState(500);
                    refine_container_stmts();
                }
                    break;

                case 2: {
                    setState(501);
                    refine_leaf_stmts();
                }
                    break;

                case 3: {
                    setState(502);
                    refine_leaf_list_stmts();
                }
                    break;

                case 4: {
                    setState(503);
                    refine_list_stmts();
                }
                    break;

                case 5: {
                    setState(504);
                    refine_choice_stmts();
                }
                    break;

                case 6: {
                    setState(505);
                    refine_case_stmts();
                }
                    break;

                case 7: {
                    setState(506);
                    refine_anyxml_stmts();
                }
                    break;
                }
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    public static class Refine_stmtContext extends ParserRuleContext {
        public List<Refune_pomContext> refune_pom() {
            return getRuleContexts(Refune_pomContext.class);
        }

        public TerminalNode RIGHT_BRACE() {
            return getToken(YangParser.RIGHT_BRACE, 0);
        }

        public TerminalNode SEMICOLON() {
            return getToken(YangParser.SEMICOLON, 0);
        }

        public StringContext string() {
            return getRuleContext(StringContext.class, 0);
        }

        public Refune_pomContext refune_pom(int i) {
            return getRuleContext(Refune_pomContext.class, i);
        }

        public TerminalNode LEFT_BRACE() {
            return getToken(YangParser.LEFT_BRACE, 0);
        }

        public TerminalNode REFINE_KEYWORD() {
            return getToken(YangParser.REFINE_KEYWORD, 0);
        }

        public Refine_stmtContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_refine_stmt;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).enterRefine_stmt(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).exitRefine_stmt(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof YangParserVisitor)
                return ((YangParserVisitor<? extends T>) visitor)
                        .visitRefine_stmt(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final Refine_stmtContext refine_stmt() throws RecognitionException {
        Refine_stmtContext _localctx = new Refine_stmtContext(_ctx, getState());
        enterRule(_localctx, 48, RULE_refine_stmt);
        try {
            int _alt;
            enterOuterAlt(_localctx, 1);
            {
                setState(509);
                match(REFINE_KEYWORD);
                setState(510);
                string();
                setState(520);
                switch (_input.LA(1)) {
                case SEMICOLON: {
                    setState(511);
                    match(SEMICOLON);
                }
                    break;
                case LEFT_BRACE: {
                    {
                        setState(512);
                        match(LEFT_BRACE);
                        setState(514);
                        _errHandler.sync(this);
                        _alt = getInterpreter().adaptivePredict(_input, 51,
                                _ctx);
                        do {
                            switch (_alt) {
                            case 1: {
                                {
                                    setState(513);
                                    refune_pom();
                                }
                            }
                                break;
                            default:
                                throw new NoViableAltException(this);
                            }
                            setState(516);
                            _errHandler.sync(this);
                            _alt = getInterpreter().adaptivePredict(_input, 51,
                                    _ctx);
                        } while (_alt != 2 && _alt != -1);
                        setState(518);
                        match(RIGHT_BRACE);
                    }
                }
                    break;
                default:
                    throw new NoViableAltException(this);
                }
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    public static class Uses_stmtContext extends ParserRuleContext {
        public TerminalNode RIGHT_BRACE() {
            return getToken(YangParser.RIGHT_BRACE, 0);
        }

        public List<Reference_stmtContext> reference_stmt() {
            return getRuleContexts(Reference_stmtContext.class);
        }

        public Description_stmtContext description_stmt(int i) {
            return getRuleContext(Description_stmtContext.class, i);
        }

        public List<When_stmtContext> when_stmt() {
            return getRuleContexts(When_stmtContext.class);
        }

        public List<Uses_augment_stmtContext> uses_augment_stmt() {
            return getRuleContexts(Uses_augment_stmtContext.class);
        }

        public If_feature_stmtContext if_feature_stmt(int i) {
            return getRuleContext(If_feature_stmtContext.class, i);
        }

        public TerminalNode USES_KEYWORD() {
            return getToken(YangParser.USES_KEYWORD, 0);
        }

        public TerminalNode LEFT_BRACE() {
            return getToken(YangParser.LEFT_BRACE, 0);
        }

        public Status_stmtContext status_stmt(int i) {
            return getRuleContext(Status_stmtContext.class, i);
        }

        public List<If_feature_stmtContext> if_feature_stmt() {
            return getRuleContexts(If_feature_stmtContext.class);
        }

        public Identifier_stmtContext identifier_stmt(int i) {
            return getRuleContext(Identifier_stmtContext.class, i);
        }

        public TerminalNode SEMICOLON() {
            return getToken(YangParser.SEMICOLON, 0);
        }

        public List<Status_stmtContext> status_stmt() {
            return getRuleContexts(Status_stmtContext.class);
        }

        public Refine_stmtContext refine_stmt(int i) {
            return getRuleContext(Refine_stmtContext.class, i);
        }

        public StringContext string() {
            return getRuleContext(StringContext.class, 0);
        }

        public List<Identifier_stmtContext> identifier_stmt() {
            return getRuleContexts(Identifier_stmtContext.class);
        }

        public List<Description_stmtContext> description_stmt() {
            return getRuleContexts(Description_stmtContext.class);
        }

        public Reference_stmtContext reference_stmt(int i) {
            return getRuleContext(Reference_stmtContext.class, i);
        }

        public When_stmtContext when_stmt(int i) {
            return getRuleContext(When_stmtContext.class, i);
        }

        public Uses_augment_stmtContext uses_augment_stmt(int i) {
            return getRuleContext(Uses_augment_stmtContext.class, i);
        }

        public List<Refine_stmtContext> refine_stmt() {
            return getRuleContexts(Refine_stmtContext.class);
        }

        public Uses_stmtContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_uses_stmt;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).enterUses_stmt(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).exitUses_stmt(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof YangParserVisitor)
                return ((YangParserVisitor<? extends T>) visitor)
                        .visitUses_stmt(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final Uses_stmtContext uses_stmt() throws RecognitionException {
        Uses_stmtContext _localctx = new Uses_stmtContext(_ctx, getState());
        enterRule(_localctx, 50, RULE_uses_stmt);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(522);
                match(USES_KEYWORD);
                setState(523);
                string();
                setState(540);
                switch (_input.LA(1)) {
                case SEMICOLON: {
                    setState(524);
                    match(SEMICOLON);
                }
                    break;
                case LEFT_BRACE: {
                    {
                        setState(525);
                        match(LEFT_BRACE);
                        setState(536);
                        _errHandler.sync(this);
                        _la = _input.LA(1);
                        while (((((_la - 10)) & ~0x3f) == 0 && ((1L << (_la - 10)) & ((1L << (WHEN_KEYWORD - 10))
                                | (1L << (STATUS_KEYWORD - 10))
                                | (1L << (REFINE_KEYWORD - 10))
                                | (1L << (REFERENCE_KEYWORD - 10))
                                | (1L << (IF_FEATURE_KEYWORD - 10))
                                | (1L << (DESCRIPTION_KEYWORD - 10))
                                | (1L << (AUGMENT_KEYWORD - 10)) | (1L << (IDENTIFIER - 10)))) != 0)) {
                            {
                                setState(534);
                                switch (_input.LA(1)) {
                                case IDENTIFIER: {
                                    setState(526);
                                    identifier_stmt();
                                }
                                    break;
                                case WHEN_KEYWORD: {
                                    setState(527);
                                    when_stmt();
                                }
                                    break;
                                case IF_FEATURE_KEYWORD: {
                                    setState(528);
                                    if_feature_stmt();
                                }
                                    break;
                                case STATUS_KEYWORD: {
                                    setState(529);
                                    status_stmt();
                                }
                                    break;
                                case DESCRIPTION_KEYWORD: {
                                    setState(530);
                                    description_stmt();
                                }
                                    break;
                                case REFERENCE_KEYWORD: {
                                    setState(531);
                                    reference_stmt();
                                }
                                    break;
                                case REFINE_KEYWORD: {
                                    setState(532);
                                    refine_stmt();
                                }
                                    break;
                                case AUGMENT_KEYWORD: {
                                    setState(533);
                                    uses_augment_stmt();
                                }
                                    break;
                                default:
                                    throw new NoViableAltException(this);
                                }
                            }
                            setState(538);
                            _errHandler.sync(this);
                            _la = _input.LA(1);
                        }
                        setState(539);
                        match(RIGHT_BRACE);
                    }
                }
                    break;
                default:
                    throw new NoViableAltException(this);
                }
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    public static class Anyxml_stmtContext extends ParserRuleContext {
        public TerminalNode RIGHT_BRACE() {
            return getToken(YangParser.RIGHT_BRACE, 0);
        }

        public List<Reference_stmtContext> reference_stmt() {
            return getRuleContexts(Reference_stmtContext.class);
        }

        public Description_stmtContext description_stmt(int i) {
            return getRuleContext(Description_stmtContext.class, i);
        }

        public List<When_stmtContext> when_stmt() {
            return getRuleContexts(When_stmtContext.class);
        }

        public TerminalNode ANYXML_KEYWORD() {
            return getToken(YangParser.ANYXML_KEYWORD, 0);
        }

        public If_feature_stmtContext if_feature_stmt(int i) {
            return getRuleContext(If_feature_stmtContext.class, i);
        }

        public TerminalNode LEFT_BRACE() {
            return getToken(YangParser.LEFT_BRACE, 0);
        }

        public Status_stmtContext status_stmt(int i) {
            return getRuleContext(Status_stmtContext.class, i);
        }

        public List<Mandatory_stmtContext> mandatory_stmt() {
            return getRuleContexts(Mandatory_stmtContext.class);
        }

        public Must_stmtContext must_stmt(int i) {
            return getRuleContext(Must_stmtContext.class, i);
        }

        public List<If_feature_stmtContext> if_feature_stmt() {
            return getRuleContexts(If_feature_stmtContext.class);
        }

        public Identifier_stmtContext identifier_stmt(int i) {
            return getRuleContext(Identifier_stmtContext.class, i);
        }

        public List<Status_stmtContext> status_stmt() {
            return getRuleContexts(Status_stmtContext.class);
        }

        public Mandatory_stmtContext mandatory_stmt(int i) {
            return getRuleContext(Mandatory_stmtContext.class, i);
        }

        public Config_stmtContext config_stmt(int i) {
            return getRuleContext(Config_stmtContext.class, i);
        }

        public TerminalNode SEMICOLON() {
            return getToken(YangParser.SEMICOLON, 0);
        }

        public List<Config_stmtContext> config_stmt() {
            return getRuleContexts(Config_stmtContext.class);
        }

        public StringContext string() {
            return getRuleContext(StringContext.class, 0);
        }

        public List<Identifier_stmtContext> identifier_stmt() {
            return getRuleContexts(Identifier_stmtContext.class);
        }

        public List<Must_stmtContext> must_stmt() {
            return getRuleContexts(Must_stmtContext.class);
        }

        public List<Description_stmtContext> description_stmt() {
            return getRuleContexts(Description_stmtContext.class);
        }

        public Reference_stmtContext reference_stmt(int i) {
            return getRuleContext(Reference_stmtContext.class, i);
        }

        public When_stmtContext when_stmt(int i) {
            return getRuleContext(When_stmtContext.class, i);
        }

        public Anyxml_stmtContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_anyxml_stmt;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).enterAnyxml_stmt(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).exitAnyxml_stmt(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof YangParserVisitor)
                return ((YangParserVisitor<? extends T>) visitor)
                        .visitAnyxml_stmt(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final Anyxml_stmtContext anyxml_stmt() throws RecognitionException {
        Anyxml_stmtContext _localctx = new Anyxml_stmtContext(_ctx, getState());
        enterRule(_localctx, 52, RULE_anyxml_stmt);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(542);
                match(ANYXML_KEYWORD);
                setState(543);
                string();
                setState(561);
                switch (_input.LA(1)) {
                case SEMICOLON: {
                    setState(544);
                    match(SEMICOLON);
                }
                    break;
                case LEFT_BRACE: {
                    {
                        setState(545);
                        match(LEFT_BRACE);
                        setState(557);
                        _errHandler.sync(this);
                        _la = _input.LA(1);
                        while (((((_la - 10)) & ~0x3f) == 0 && ((1L << (_la - 10)) & ((1L << (WHEN_KEYWORD - 10))
                                | (1L << (STATUS_KEYWORD - 10))
                                | (1L << (REFERENCE_KEYWORD - 10))
                                | (1L << (MUST_KEYWORD - 10))
                                | (1L << (MANDATORY_KEYWORD - 10))
                                | (1L << (IF_FEATURE_KEYWORD - 10))
                                | (1L << (DESCRIPTION_KEYWORD - 10))
                                | (1L << (CONFIG_KEYWORD - 10)) | (1L << (IDENTIFIER - 10)))) != 0)) {
                            {
                                setState(555);
                                switch (_input.LA(1)) {
                                case IDENTIFIER: {
                                    setState(546);
                                    identifier_stmt();
                                }
                                    break;
                                case WHEN_KEYWORD: {
                                    setState(547);
                                    when_stmt();
                                }
                                    break;
                                case IF_FEATURE_KEYWORD: {
                                    setState(548);
                                    if_feature_stmt();
                                }
                                    break;
                                case MUST_KEYWORD: {
                                    setState(549);
                                    must_stmt();
                                }
                                    break;
                                case CONFIG_KEYWORD: {
                                    setState(550);
                                    config_stmt();
                                }
                                    break;
                                case MANDATORY_KEYWORD: {
                                    setState(551);
                                    mandatory_stmt();
                                }
                                    break;
                                case STATUS_KEYWORD: {
                                    setState(552);
                                    status_stmt();
                                }
                                    break;
                                case DESCRIPTION_KEYWORD: {
                                    setState(553);
                                    description_stmt();
                                }
                                    break;
                                case REFERENCE_KEYWORD: {
                                    setState(554);
                                    reference_stmt();
                                }
                                    break;
                                default:
                                    throw new NoViableAltException(this);
                                }
                            }
                            setState(559);
                            _errHandler.sync(this);
                            _la = _input.LA(1);
                        }
                        setState(560);
                        match(RIGHT_BRACE);
                    }
                }
                    break;
                default:
                    throw new NoViableAltException(this);
                }
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    public static class Case_stmtContext extends ParserRuleContext {
        public TerminalNode RIGHT_BRACE() {
            return getToken(YangParser.RIGHT_BRACE, 0);
        }

        public List<Reference_stmtContext> reference_stmt() {
            return getRuleContexts(Reference_stmtContext.class);
        }

        public Description_stmtContext description_stmt(int i) {
            return getRuleContext(Description_stmtContext.class, i);
        }

        public List<When_stmtContext> when_stmt() {
            return getRuleContexts(When_stmtContext.class);
        }

        public If_feature_stmtContext if_feature_stmt(int i) {
            return getRuleContext(If_feature_stmtContext.class, i);
        }

        public TerminalNode LEFT_BRACE() {
            return getToken(YangParser.LEFT_BRACE, 0);
        }

        public Data_def_stmtContext data_def_stmt(int i) {
            return getRuleContext(Data_def_stmtContext.class, i);
        }

        public Status_stmtContext status_stmt(int i) {
            return getRuleContext(Status_stmtContext.class, i);
        }

        public List<If_feature_stmtContext> if_feature_stmt() {
            return getRuleContexts(If_feature_stmtContext.class);
        }

        public Identifier_stmtContext identifier_stmt(int i) {
            return getRuleContext(Identifier_stmtContext.class, i);
        }

        public TerminalNode CASE_KEYWORD() {
            return getToken(YangParser.CASE_KEYWORD, 0);
        }

        public List<Data_def_stmtContext> data_def_stmt() {
            return getRuleContexts(Data_def_stmtContext.class);
        }

        public TerminalNode SEMICOLON() {
            return getToken(YangParser.SEMICOLON, 0);
        }

        public List<Status_stmtContext> status_stmt() {
            return getRuleContexts(Status_stmtContext.class);
        }

        public StringContext string() {
            return getRuleContext(StringContext.class, 0);
        }

        public List<Identifier_stmtContext> identifier_stmt() {
            return getRuleContexts(Identifier_stmtContext.class);
        }

        public List<Description_stmtContext> description_stmt() {
            return getRuleContexts(Description_stmtContext.class);
        }

        public Reference_stmtContext reference_stmt(int i) {
            return getRuleContext(Reference_stmtContext.class, i);
        }

        public When_stmtContext when_stmt(int i) {
            return getRuleContext(When_stmtContext.class, i);
        }

        public Case_stmtContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_case_stmt;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).enterCase_stmt(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).exitCase_stmt(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof YangParserVisitor)
                return ((YangParserVisitor<? extends T>) visitor)
                        .visitCase_stmt(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final Case_stmtContext case_stmt() throws RecognitionException {
        Case_stmtContext _localctx = new Case_stmtContext(_ctx, getState());
        enterRule(_localctx, 54, RULE_case_stmt);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(563);
                match(CASE_KEYWORD);
                setState(564);
                string();
                setState(580);
                switch (_input.LA(1)) {
                case SEMICOLON: {
                    setState(565);
                    match(SEMICOLON);
                }
                    break;
                case LEFT_BRACE: {
                    {
                        setState(566);
                        match(LEFT_BRACE);
                        setState(576);
                        _errHandler.sync(this);
                        _la = _input.LA(1);
                        while (((((_la - 10)) & ~0x3f) == 0 && ((1L << (_la - 10)) & ((1L << (WHEN_KEYWORD - 10))
                                | (1L << (USES_KEYWORD - 10))
                                | (1L << (STATUS_KEYWORD - 10))
                                | (1L << (REFERENCE_KEYWORD - 10))
                                | (1L << (LIST_KEYWORD - 10))
                                | (1L << (LEAF_LIST_KEYWORD - 10))
                                | (1L << (LEAF_KEYWORD - 10))
                                | (1L << (IF_FEATURE_KEYWORD - 10))
                                | (1L << (DESCRIPTION_KEYWORD - 10))
                                | (1L << (CONTAINER_KEYWORD - 10))
                                | (1L << (CHOICE_KEYWORD - 10))
                                | (1L << (ANYXML_KEYWORD - 10)) | (1L << (IDENTIFIER - 10)))) != 0)) {
                            {
                                setState(574);
                                switch (_input.LA(1)) {
                                case IDENTIFIER: {
                                    setState(567);
                                    identifier_stmt();
                                }
                                    break;
                                case WHEN_KEYWORD: {
                                    setState(568);
                                    when_stmt();
                                }
                                    break;
                                case IF_FEATURE_KEYWORD: {
                                    setState(569);
                                    if_feature_stmt();
                                }
                                    break;
                                case STATUS_KEYWORD: {
                                    setState(570);
                                    status_stmt();
                                }
                                    break;
                                case DESCRIPTION_KEYWORD: {
                                    setState(571);
                                    description_stmt();
                                }
                                    break;
                                case REFERENCE_KEYWORD: {
                                    setState(572);
                                    reference_stmt();
                                }
                                    break;
                                case USES_KEYWORD:
                                case LIST_KEYWORD:
                                case LEAF_LIST_KEYWORD:
                                case LEAF_KEYWORD:
                                case CONTAINER_KEYWORD:
                                case CHOICE_KEYWORD:
                                case ANYXML_KEYWORD: {
                                    setState(573);
                                    data_def_stmt();
                                }
                                    break;
                                default:
                                    throw new NoViableAltException(this);
                                }
                            }
                            setState(578);
                            _errHandler.sync(this);
                            _la = _input.LA(1);
                        }
                        setState(579);
                        match(RIGHT_BRACE);
                    }
                }
                    break;
                default:
                    throw new NoViableAltException(this);
                }
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    public static class Short_case_stmtContext extends ParserRuleContext {
        public Anyxml_stmtContext anyxml_stmt() {
            return getRuleContext(Anyxml_stmtContext.class, 0);
        }

        public List_stmtContext list_stmt() {
            return getRuleContext(List_stmtContext.class, 0);
        }

        public Leaf_stmtContext leaf_stmt() {
            return getRuleContext(Leaf_stmtContext.class, 0);
        }

        public Container_stmtContext container_stmt() {
            return getRuleContext(Container_stmtContext.class, 0);
        }

        public Leaf_list_stmtContext leaf_list_stmt() {
            return getRuleContext(Leaf_list_stmtContext.class, 0);
        }

        public Short_case_stmtContext(ParserRuleContext parent,
                int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_short_case_stmt;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).enterShort_case_stmt(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).exitShort_case_stmt(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof YangParserVisitor)
                return ((YangParserVisitor<? extends T>) visitor)
                        .visitShort_case_stmt(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final Short_case_stmtContext short_case_stmt()
            throws RecognitionException {
        Short_case_stmtContext _localctx = new Short_case_stmtContext(_ctx,
                getState());
        enterRule(_localctx, 56, RULE_short_case_stmt);
        try {
            setState(587);
            switch (_input.LA(1)) {
            case CONTAINER_KEYWORD:
                enterOuterAlt(_localctx, 1);
                {
                    setState(582);
                    container_stmt();
                }
                break;
            case LEAF_KEYWORD:
                enterOuterAlt(_localctx, 2);
                {
                    setState(583);
                    leaf_stmt();
                }
                break;
            case LEAF_LIST_KEYWORD:
                enterOuterAlt(_localctx, 3);
                {
                    setState(584);
                    leaf_list_stmt();
                }
                break;
            case LIST_KEYWORD:
                enterOuterAlt(_localctx, 4);
                {
                    setState(585);
                    list_stmt();
                }
                break;
            case ANYXML_KEYWORD:
                enterOuterAlt(_localctx, 5);
                {
                    setState(586);
                    anyxml_stmt();
                }
                break;
            default:
                throw new NoViableAltException(this);
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    public static class Choice_stmtContext extends ParserRuleContext {
        public TerminalNode RIGHT_BRACE() {
            return getToken(YangParser.RIGHT_BRACE, 0);
        }

        public List<Reference_stmtContext> reference_stmt() {
            return getRuleContexts(Reference_stmtContext.class);
        }

        public Description_stmtContext description_stmt(int i) {
            return getRuleContext(Description_stmtContext.class, i);
        }

        public Default_stmtContext default_stmt(int i) {
            return getRuleContext(Default_stmtContext.class, i);
        }

        public TerminalNode CHOICE_KEYWORD() {
            return getToken(YangParser.CHOICE_KEYWORD, 0);
        }

        public List<When_stmtContext> when_stmt() {
            return getRuleContexts(When_stmtContext.class);
        }

        public List<Case_stmtContext> case_stmt() {
            return getRuleContexts(Case_stmtContext.class);
        }

        public If_feature_stmtContext if_feature_stmt(int i) {
            return getRuleContext(If_feature_stmtContext.class, i);
        }

        public TerminalNode LEFT_BRACE() {
            return getToken(YangParser.LEFT_BRACE, 0);
        }

        public Status_stmtContext status_stmt(int i) {
            return getRuleContext(Status_stmtContext.class, i);
        }

        public List<Mandatory_stmtContext> mandatory_stmt() {
            return getRuleContexts(Mandatory_stmtContext.class);
        }

        public List<If_feature_stmtContext> if_feature_stmt() {
            return getRuleContexts(If_feature_stmtContext.class);
        }

        public Identifier_stmtContext identifier_stmt(int i) {
            return getRuleContext(Identifier_stmtContext.class, i);
        }

        public List<Default_stmtContext> default_stmt() {
            return getRuleContexts(Default_stmtContext.class);
        }

        public List<Status_stmtContext> status_stmt() {
            return getRuleContexts(Status_stmtContext.class);
        }

        public Mandatory_stmtContext mandatory_stmt(int i) {
            return getRuleContext(Mandatory_stmtContext.class, i);
        }

        public Config_stmtContext config_stmt(int i) {
            return getRuleContext(Config_stmtContext.class, i);
        }

        public TerminalNode SEMICOLON() {
            return getToken(YangParser.SEMICOLON, 0);
        }

        public Short_case_stmtContext short_case_stmt(int i) {
            return getRuleContext(Short_case_stmtContext.class, i);
        }

        public List<Short_case_stmtContext> short_case_stmt() {
            return getRuleContexts(Short_case_stmtContext.class);
        }

        public List<Config_stmtContext> config_stmt() {
            return getRuleContexts(Config_stmtContext.class);
        }

        public StringContext string() {
            return getRuleContext(StringContext.class, 0);
        }

        public List<Identifier_stmtContext> identifier_stmt() {
            return getRuleContexts(Identifier_stmtContext.class);
        }

        public List<Description_stmtContext> description_stmt() {
            return getRuleContexts(Description_stmtContext.class);
        }

        public Reference_stmtContext reference_stmt(int i) {
            return getRuleContext(Reference_stmtContext.class, i);
        }

        public When_stmtContext when_stmt(int i) {
            return getRuleContext(When_stmtContext.class, i);
        }

        public Case_stmtContext case_stmt(int i) {
            return getRuleContext(Case_stmtContext.class, i);
        }

        public Choice_stmtContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_choice_stmt;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).enterChoice_stmt(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).exitChoice_stmt(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof YangParserVisitor)
                return ((YangParserVisitor<? extends T>) visitor)
                        .visitChoice_stmt(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final Choice_stmtContext choice_stmt() throws RecognitionException {
        Choice_stmtContext _localctx = new Choice_stmtContext(_ctx, getState());
        enterRule(_localctx, 58, RULE_choice_stmt);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(589);
                match(CHOICE_KEYWORD);
                setState(590);
                string();
                setState(610);
                switch (_input.LA(1)) {
                case SEMICOLON: {
                    setState(591);
                    match(SEMICOLON);
                }
                    break;
                case LEFT_BRACE: {
                    {
                        setState(592);
                        match(LEFT_BRACE);
                        setState(606);
                        _errHandler.sync(this);
                        _la = _input.LA(1);
                        while (((((_la - 10)) & ~0x3f) == 0 && ((1L << (_la - 10)) & ((1L << (WHEN_KEYWORD - 10))
                                | (1L << (STATUS_KEYWORD - 10))
                                | (1L << (REFERENCE_KEYWORD - 10))
                                | (1L << (MANDATORY_KEYWORD - 10))
                                | (1L << (LIST_KEYWORD - 10))
                                | (1L << (LEAF_LIST_KEYWORD - 10))
                                | (1L << (LEAF_KEYWORD - 10))
                                | (1L << (IF_FEATURE_KEYWORD - 10))
                                | (1L << (DESCRIPTION_KEYWORD - 10))
                                | (1L << (DEFAULT_KEYWORD - 10))
                                | (1L << (CONTAINER_KEYWORD - 10))
                                | (1L << (CONFIG_KEYWORD - 10))
                                | (1L << (CASE_KEYWORD - 10))
                                | (1L << (ANYXML_KEYWORD - 10)) | (1L << (IDENTIFIER - 10)))) != 0)) {
                            {
                                setState(604);
                                switch (_input.LA(1)) {
                                case IDENTIFIER: {
                                    setState(593);
                                    identifier_stmt();
                                }
                                    break;
                                case WHEN_KEYWORD: {
                                    setState(594);
                                    when_stmt();
                                }
                                    break;
                                case IF_FEATURE_KEYWORD: {
                                    setState(595);
                                    if_feature_stmt();
                                }
                                    break;
                                case DEFAULT_KEYWORD: {
                                    setState(596);
                                    default_stmt();
                                }
                                    break;
                                case CONFIG_KEYWORD: {
                                    setState(597);
                                    config_stmt();
                                }
                                    break;
                                case MANDATORY_KEYWORD: {
                                    setState(598);
                                    mandatory_stmt();
                                }
                                    break;
                                case STATUS_KEYWORD: {
                                    setState(599);
                                    status_stmt();
                                }
                                    break;
                                case DESCRIPTION_KEYWORD: {
                                    setState(600);
                                    description_stmt();
                                }
                                    break;
                                case REFERENCE_KEYWORD: {
                                    setState(601);
                                    reference_stmt();
                                }
                                    break;
                                case LIST_KEYWORD:
                                case LEAF_LIST_KEYWORD:
                                case LEAF_KEYWORD:
                                case CONTAINER_KEYWORD:
                                case ANYXML_KEYWORD: {
                                    setState(602);
                                    short_case_stmt();
                                }
                                    break;
                                case CASE_KEYWORD: {
                                    setState(603);
                                    case_stmt();
                                }
                                    break;
                                default:
                                    throw new NoViableAltException(this);
                                }
                            }
                            setState(608);
                            _errHandler.sync(this);
                            _la = _input.LA(1);
                        }
                        setState(609);
                        match(RIGHT_BRACE);
                    }
                }
                    break;
                default:
                    throw new NoViableAltException(this);
                }
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    public static class Unique_stmtContext extends ParserRuleContext {
        public StringContext string() {
            return getRuleContext(StringContext.class, 0);
        }

        public StmtendContext stmtend() {
            return getRuleContext(StmtendContext.class, 0);
        }

        public TerminalNode UNIQUE_KEYWORD() {
            return getToken(YangParser.UNIQUE_KEYWORD, 0);
        }

        public Unique_stmtContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_unique_stmt;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).enterUnique_stmt(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).exitUnique_stmt(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof YangParserVisitor)
                return ((YangParserVisitor<? extends T>) visitor)
                        .visitUnique_stmt(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final Unique_stmtContext unique_stmt() throws RecognitionException {
        Unique_stmtContext _localctx = new Unique_stmtContext(_ctx, getState());
        enterRule(_localctx, 60, RULE_unique_stmt);
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(612);
                match(UNIQUE_KEYWORD);
                setState(613);
                string();
                setState(614);
                stmtend();
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    public static class Key_stmtContext extends ParserRuleContext {
        public StringContext string() {
            return getRuleContext(StringContext.class, 0);
        }

        public StmtendContext stmtend() {
            return getRuleContext(StmtendContext.class, 0);
        }

        public TerminalNode KEY_KEYWORD() {
            return getToken(YangParser.KEY_KEYWORD, 0);
        }

        public Key_stmtContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_key_stmt;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).enterKey_stmt(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).exitKey_stmt(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof YangParserVisitor)
                return ((YangParserVisitor<? extends T>) visitor)
                        .visitKey_stmt(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final Key_stmtContext key_stmt() throws RecognitionException {
        Key_stmtContext _localctx = new Key_stmtContext(_ctx, getState());
        enterRule(_localctx, 62, RULE_key_stmt);
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(616);
                match(KEY_KEYWORD);
                setState(617);
                string();
                setState(618);
                stmtend();
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    public static class List_stmtContext extends ParserRuleContext {
        public TerminalNode RIGHT_BRACE() {
            return getToken(YangParser.RIGHT_BRACE, 0);
        }

        public List<When_stmtContext> when_stmt() {
            return getRuleContexts(When_stmtContext.class);
        }

        public List<Max_elements_stmtContext> max_elements_stmt() {
            return getRuleContexts(Max_elements_stmtContext.class);
        }

        public Data_def_stmtContext data_def_stmt(int i) {
            return getRuleContext(Data_def_stmtContext.class, i);
        }

        public List<Typedef_stmtContext> typedef_stmt() {
            return getRuleContexts(Typedef_stmtContext.class);
        }

        public Min_elements_stmtContext min_elements_stmt(int i) {
            return getRuleContext(Min_elements_stmtContext.class, i);
        }

        public List<Data_def_stmtContext> data_def_stmt() {
            return getRuleContexts(Data_def_stmtContext.class);
        }

        public Config_stmtContext config_stmt(int i) {
            return getRuleContext(Config_stmtContext.class, i);
        }

        public List<Min_elements_stmtContext> min_elements_stmt() {
            return getRuleContexts(Min_elements_stmtContext.class);
        }

        public List<Description_stmtContext> description_stmt() {
            return getRuleContexts(Description_stmtContext.class);
        }

        public When_stmtContext when_stmt(int i) {
            return getRuleContext(When_stmtContext.class, i);
        }

        public Unique_stmtContext unique_stmt(int i) {
            return getRuleContext(Unique_stmtContext.class, i);
        }

        public List<Grouping_stmtContext> grouping_stmt() {
            return getRuleContexts(Grouping_stmtContext.class);
        }

        public List<Reference_stmtContext> reference_stmt() {
            return getRuleContexts(Reference_stmtContext.class);
        }

        public Typedef_stmtContext typedef_stmt(int i) {
            return getRuleContext(Typedef_stmtContext.class, i);
        }

        public Description_stmtContext description_stmt(int i) {
            return getRuleContext(Description_stmtContext.class, i);
        }

        public Ordered_by_stmtContext ordered_by_stmt(int i) {
            return getRuleContext(Ordered_by_stmtContext.class, i);
        }

        public Grouping_stmtContext grouping_stmt(int i) {
            return getRuleContext(Grouping_stmtContext.class, i);
        }

        public If_feature_stmtContext if_feature_stmt(int i) {
            return getRuleContext(If_feature_stmtContext.class, i);
        }

        public TerminalNode LEFT_BRACE() {
            return getToken(YangParser.LEFT_BRACE, 0);
        }

        public Status_stmtContext status_stmt(int i) {
            return getRuleContext(Status_stmtContext.class, i);
        }

        public List<Ordered_by_stmtContext> ordered_by_stmt() {
            return getRuleContexts(Ordered_by_stmtContext.class);
        }

        public Must_stmtContext must_stmt(int i) {
            return getRuleContext(Must_stmtContext.class, i);
        }

        public List<If_feature_stmtContext> if_feature_stmt() {
            return getRuleContexts(If_feature_stmtContext.class);
        }

        public TerminalNode LIST_KEYWORD() {
            return getToken(YangParser.LIST_KEYWORD, 0);
        }

        public Key_stmtContext key_stmt(int i) {
            return getRuleContext(Key_stmtContext.class, i);
        }

        public Identifier_stmtContext identifier_stmt(int i) {
            return getRuleContext(Identifier_stmtContext.class, i);
        }

        public List<Status_stmtContext> status_stmt() {
            return getRuleContexts(Status_stmtContext.class);
        }

        public List<Must_stmtContext> must_stmt() {
            return getRuleContexts(Must_stmtContext.class);
        }

        public List<Identifier_stmtContext> identifier_stmt() {
            return getRuleContexts(Identifier_stmtContext.class);
        }

        public StringContext string() {
            return getRuleContext(StringContext.class, 0);
        }

        public List<Config_stmtContext> config_stmt() {
            return getRuleContexts(Config_stmtContext.class);
        }

        public List<Unique_stmtContext> unique_stmt() {
            return getRuleContexts(Unique_stmtContext.class);
        }

        public Max_elements_stmtContext max_elements_stmt(int i) {
            return getRuleContext(Max_elements_stmtContext.class, i);
        }

        public List<Key_stmtContext> key_stmt() {
            return getRuleContexts(Key_stmtContext.class);
        }

        public Reference_stmtContext reference_stmt(int i) {
            return getRuleContext(Reference_stmtContext.class, i);
        }

        public List_stmtContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_list_stmt;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).enterList_stmt(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).exitList_stmt(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof YangParserVisitor)
                return ((YangParserVisitor<? extends T>) visitor)
                        .visitList_stmt(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final List_stmtContext list_stmt() throws RecognitionException {
        List_stmtContext _localctx = new List_stmtContext(_ctx, getState());
        enterRule(_localctx, 64, RULE_list_stmt);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(620);
                match(LIST_KEYWORD);
                setState(621);
                string();
                setState(622);
                match(LEFT_BRACE);
                setState(639);
                _errHandler.sync(this);
                _la = _input.LA(1);
                do {
                    {
                        setState(639);
                        switch (_input.LA(1)) {
                        case IDENTIFIER: {
                            setState(623);
                            identifier_stmt();
                        }
                            break;
                        case WHEN_KEYWORD: {
                            setState(624);
                            when_stmt();
                        }
                            break;
                        case IF_FEATURE_KEYWORD: {
                            setState(625);
                            if_feature_stmt();
                        }
                            break;
                        case MUST_KEYWORD: {
                            setState(626);
                            must_stmt();
                        }
                            break;
                        case KEY_KEYWORD: {
                            setState(627);
                            key_stmt();
                        }
                            break;
                        case UNIQUE_KEYWORD: {
                            setState(628);
                            unique_stmt();
                        }
                            break;
                        case CONFIG_KEYWORD: {
                            setState(629);
                            config_stmt();
                        }
                            break;
                        case MIN_ELEMENTS_KEYWORD: {
                            setState(630);
                            min_elements_stmt();
                        }
                            break;
                        case MAX_ELEMENTS_KEYWORD: {
                            setState(631);
                            max_elements_stmt();
                        }
                            break;
                        case ORDERED_BY_KEYWORD: {
                            setState(632);
                            ordered_by_stmt();
                        }
                            break;
                        case STATUS_KEYWORD: {
                            setState(633);
                            status_stmt();
                        }
                            break;
                        case DESCRIPTION_KEYWORD: {
                            setState(634);
                            description_stmt();
                        }
                            break;
                        case REFERENCE_KEYWORD: {
                            setState(635);
                            reference_stmt();
                        }
                            break;
                        case TYPEDEF_KEYWORD: {
                            setState(636);
                            typedef_stmt();
                        }
                            break;
                        case GROUPING_KEYWORD: {
                            setState(637);
                            grouping_stmt();
                        }
                            break;
                        case USES_KEYWORD:
                        case LIST_KEYWORD:
                        case LEAF_LIST_KEYWORD:
                        case LEAF_KEYWORD:
                        case CONTAINER_KEYWORD:
                        case CHOICE_KEYWORD:
                        case ANYXML_KEYWORD: {
                            setState(638);
                            data_def_stmt();
                        }
                            break;
                        default:
                            throw new NoViableAltException(this);
                        }
                    }
                    setState(641);
                    _errHandler.sync(this);
                    _la = _input.LA(1);
                } while (((((_la - 10)) & ~0x3f) == 0 && ((1L << (_la - 10)) & ((1L << (WHEN_KEYWORD - 10))
                        | (1L << (USES_KEYWORD - 10))
                        | (1L << (UNIQUE_KEYWORD - 10))
                        | (1L << (TYPEDEF_KEYWORD - 10))
                        | (1L << (STATUS_KEYWORD - 10))
                        | (1L << (REFERENCE_KEYWORD - 10))
                        | (1L << (ORDERED_BY_KEYWORD - 10))
                        | (1L << (MUST_KEYWORD - 10))
                        | (1L << (MIN_ELEMENTS_KEYWORD - 10))
                        | (1L << (MAX_ELEMENTS_KEYWORD - 10))
                        | (1L << (LIST_KEYWORD - 10))
                        | (1L << (LEAF_LIST_KEYWORD - 10))
                        | (1L << (LEAF_KEYWORD - 10))
                        | (1L << (KEY_KEYWORD - 10))
                        | (1L << (IF_FEATURE_KEYWORD - 10))
                        | (1L << (GROUPING_KEYWORD - 10))
                        | (1L << (DESCRIPTION_KEYWORD - 10))
                        | (1L << (CONTAINER_KEYWORD - 10))
                        | (1L << (CONFIG_KEYWORD - 10))
                        | (1L << (CHOICE_KEYWORD - 10))
                        | (1L << (ANYXML_KEYWORD - 10)) | (1L << (IDENTIFIER - 10)))) != 0));
                setState(643);
                match(RIGHT_BRACE);
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    public static class Leaf_list_stmtContext extends ParserRuleContext {
        public TerminalNode RIGHT_BRACE() {
            return getToken(YangParser.RIGHT_BRACE, 0);
        }

        public List<When_stmtContext> when_stmt() {
            return getRuleContexts(When_stmtContext.class);
        }

        public Type_stmtContext type_stmt(int i) {
            return getRuleContext(Type_stmtContext.class, i);
        }

        public List<Max_elements_stmtContext> max_elements_stmt() {
            return getRuleContexts(Max_elements_stmtContext.class);
        }

        public Min_elements_stmtContext min_elements_stmt(int i) {
            return getRuleContext(Min_elements_stmtContext.class, i);
        }

        public TerminalNode LEAF_LIST_KEYWORD() {
            return getToken(YangParser.LEAF_LIST_KEYWORD, 0);
        }

        public Config_stmtContext config_stmt(int i) {
            return getRuleContext(Config_stmtContext.class, i);
        }

        public List<Min_elements_stmtContext> min_elements_stmt() {
            return getRuleContexts(Min_elements_stmtContext.class);
        }

        public List<Description_stmtContext> description_stmt() {
            return getRuleContexts(Description_stmtContext.class);
        }

        public When_stmtContext when_stmt(int i) {
            return getRuleContext(When_stmtContext.class, i);
        }

        public Units_stmtContext units_stmt(int i) {
            return getRuleContext(Units_stmtContext.class, i);
        }

        public List<Reference_stmtContext> reference_stmt() {
            return getRuleContexts(Reference_stmtContext.class);
        }

        public Description_stmtContext description_stmt(int i) {
            return getRuleContext(Description_stmtContext.class, i);
        }

        public Ordered_by_stmtContext ordered_by_stmt(int i) {
            return getRuleContext(Ordered_by_stmtContext.class, i);
        }

        public List<Units_stmtContext> units_stmt() {
            return getRuleContexts(Units_stmtContext.class);
        }

        public If_feature_stmtContext if_feature_stmt(int i) {
            return getRuleContext(If_feature_stmtContext.class, i);
        }

        public TerminalNode LEFT_BRACE() {
            return getToken(YangParser.LEFT_BRACE, 0);
        }

        public Status_stmtContext status_stmt(int i) {
            return getRuleContext(Status_stmtContext.class, i);
        }

        public List<Ordered_by_stmtContext> ordered_by_stmt() {
            return getRuleContexts(Ordered_by_stmtContext.class);
        }

        public List<Type_stmtContext> type_stmt() {
            return getRuleContexts(Type_stmtContext.class);
        }

        public List<If_feature_stmtContext> if_feature_stmt() {
            return getRuleContexts(If_feature_stmtContext.class);
        }

        public Must_stmtContext must_stmt(int i) {
            return getRuleContext(Must_stmtContext.class, i);
        }

        public Identifier_stmtContext identifier_stmt(int i) {
            return getRuleContext(Identifier_stmtContext.class, i);
        }

        public List<Status_stmtContext> status_stmt() {
            return getRuleContexts(Status_stmtContext.class);
        }

        public List<Identifier_stmtContext> identifier_stmt() {
            return getRuleContexts(Identifier_stmtContext.class);
        }

        public StringContext string() {
            return getRuleContext(StringContext.class, 0);
        }

        public List<Must_stmtContext> must_stmt() {
            return getRuleContexts(Must_stmtContext.class);
        }

        public List<Config_stmtContext> config_stmt() {
            return getRuleContexts(Config_stmtContext.class);
        }

        public Max_elements_stmtContext max_elements_stmt(int i) {
            return getRuleContext(Max_elements_stmtContext.class, i);
        }

        public Reference_stmtContext reference_stmt(int i) {
            return getRuleContext(Reference_stmtContext.class, i);
        }

        public Leaf_list_stmtContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_leaf_list_stmt;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).enterLeaf_list_stmt(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).exitLeaf_list_stmt(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof YangParserVisitor)
                return ((YangParserVisitor<? extends T>) visitor)
                        .visitLeaf_list_stmt(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final Leaf_list_stmtContext leaf_list_stmt()
            throws RecognitionException {
        Leaf_list_stmtContext _localctx = new Leaf_list_stmtContext(_ctx,
                getState());
        enterRule(_localctx, 66, RULE_leaf_list_stmt);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(645);
                match(LEAF_LIST_KEYWORD);
                setState(646);
                string();
                setState(647);
                match(LEFT_BRACE);
                setState(663);
                _errHandler.sync(this);
                _la = _input.LA(1);
                while (((((_la - 10)) & ~0x3f) == 0 && ((1L << (_la - 10)) & ((1L << (WHEN_KEYWORD - 10))
                        | (1L << (UNITS_KEYWORD - 10))
                        | (1L << (TYPE_KEYWORD - 10))
                        | (1L << (STATUS_KEYWORD - 10))
                        | (1L << (REFERENCE_KEYWORD - 10))
                        | (1L << (ORDERED_BY_KEYWORD - 10))
                        | (1L << (MUST_KEYWORD - 10))
                        | (1L << (MIN_ELEMENTS_KEYWORD - 10))
                        | (1L << (MAX_ELEMENTS_KEYWORD - 10))
                        | (1L << (IF_FEATURE_KEYWORD - 10))
                        | (1L << (DESCRIPTION_KEYWORD - 10))
                        | (1L << (CONFIG_KEYWORD - 10)) | (1L << (IDENTIFIER - 10)))) != 0)) {
                    {
                        setState(661);
                        switch (_input.LA(1)) {
                        case IDENTIFIER: {
                            setState(648);
                            identifier_stmt();
                        }
                            break;
                        case WHEN_KEYWORD: {
                            setState(649);
                            when_stmt();
                        }
                            break;
                        case IF_FEATURE_KEYWORD: {
                            setState(650);
                            if_feature_stmt();
                        }
                            break;
                        case TYPE_KEYWORD: {
                            setState(651);
                            type_stmt();
                        }
                            break;
                        case UNITS_KEYWORD: {
                            setState(652);
                            units_stmt();
                        }
                            break;
                        case MUST_KEYWORD: {
                            setState(653);
                            must_stmt();
                        }
                            break;
                        case CONFIG_KEYWORD: {
                            setState(654);
                            config_stmt();
                        }
                            break;
                        case MIN_ELEMENTS_KEYWORD: {
                            setState(655);
                            min_elements_stmt();
                        }
                            break;
                        case MAX_ELEMENTS_KEYWORD: {
                            setState(656);
                            max_elements_stmt();
                        }
                            break;
                        case ORDERED_BY_KEYWORD: {
                            setState(657);
                            ordered_by_stmt();
                        }
                            break;
                        case STATUS_KEYWORD: {
                            setState(658);
                            status_stmt();
                        }
                            break;
                        case DESCRIPTION_KEYWORD: {
                            setState(659);
                            description_stmt();
                        }
                            break;
                        case REFERENCE_KEYWORD: {
                            setState(660);
                            reference_stmt();
                        }
                            break;
                        default:
                            throw new NoViableAltException(this);
                        }
                    }
                    setState(665);
                    _errHandler.sync(this);
                    _la = _input.LA(1);
                }
                setState(666);
                match(RIGHT_BRACE);
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    public static class Leaf_stmtContext extends ParserRuleContext {
        public TerminalNode RIGHT_BRACE() {
            return getToken(YangParser.RIGHT_BRACE, 0);
        }

        public List<When_stmtContext> when_stmt() {
            return getRuleContexts(When_stmtContext.class);
        }

        public Type_stmtContext type_stmt(int i) {
            return getRuleContext(Type_stmtContext.class, i);
        }

        public List<Mandatory_stmtContext> mandatory_stmt() {
            return getRuleContexts(Mandatory_stmtContext.class);
        }

        public Config_stmtContext config_stmt(int i) {
            return getRuleContext(Config_stmtContext.class, i);
        }

        public TerminalNode LEAF_KEYWORD() {
            return getToken(YangParser.LEAF_KEYWORD, 0);
        }

        public List<Description_stmtContext> description_stmt() {
            return getRuleContexts(Description_stmtContext.class);
        }

        public When_stmtContext when_stmt(int i) {
            return getRuleContext(When_stmtContext.class, i);
        }

        public Units_stmtContext units_stmt(int i) {
            return getRuleContext(Units_stmtContext.class, i);
        }

        public List<Reference_stmtContext> reference_stmt() {
            return getRuleContexts(Reference_stmtContext.class);
        }

        public Description_stmtContext description_stmt(int i) {
            return getRuleContext(Description_stmtContext.class, i);
        }

        public Default_stmtContext default_stmt(int i) {
            return getRuleContext(Default_stmtContext.class, i);
        }

        public List<Units_stmtContext> units_stmt() {
            return getRuleContexts(Units_stmtContext.class);
        }

        public If_feature_stmtContext if_feature_stmt(int i) {
            return getRuleContext(If_feature_stmtContext.class, i);
        }

        public TerminalNode LEFT_BRACE() {
            return getToken(YangParser.LEFT_BRACE, 0);
        }

        public Status_stmtContext status_stmt(int i) {
            return getRuleContext(Status_stmtContext.class, i);
        }

        public List<Type_stmtContext> type_stmt() {
            return getRuleContexts(Type_stmtContext.class);
        }

        public List<If_feature_stmtContext> if_feature_stmt() {
            return getRuleContexts(If_feature_stmtContext.class);
        }

        public Must_stmtContext must_stmt(int i) {
            return getRuleContext(Must_stmtContext.class, i);
        }

        public Identifier_stmtContext identifier_stmt(int i) {
            return getRuleContext(Identifier_stmtContext.class, i);
        }

        public List<Default_stmtContext> default_stmt() {
            return getRuleContexts(Default_stmtContext.class);
        }

        public Mandatory_stmtContext mandatory_stmt(int i) {
            return getRuleContext(Mandatory_stmtContext.class, i);
        }

        public List<Status_stmtContext> status_stmt() {
            return getRuleContexts(Status_stmtContext.class);
        }

        public List<Identifier_stmtContext> identifier_stmt() {
            return getRuleContexts(Identifier_stmtContext.class);
        }

        public StringContext string() {
            return getRuleContext(StringContext.class, 0);
        }

        public List<Must_stmtContext> must_stmt() {
            return getRuleContexts(Must_stmtContext.class);
        }

        public List<Config_stmtContext> config_stmt() {
            return getRuleContexts(Config_stmtContext.class);
        }

        public Reference_stmtContext reference_stmt(int i) {
            return getRuleContext(Reference_stmtContext.class, i);
        }

        public Leaf_stmtContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_leaf_stmt;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).enterLeaf_stmt(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).exitLeaf_stmt(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof YangParserVisitor)
                return ((YangParserVisitor<? extends T>) visitor)
                        .visitLeaf_stmt(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final Leaf_stmtContext leaf_stmt() throws RecognitionException {
        Leaf_stmtContext _localctx = new Leaf_stmtContext(_ctx, getState());
        enterRule(_localctx, 68, RULE_leaf_stmt);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(668);
                match(LEAF_KEYWORD);
                setState(669);
                string();
                setState(670);
                match(LEFT_BRACE);
                setState(685);
                _errHandler.sync(this);
                _la = _input.LA(1);
                while (((((_la - 10)) & ~0x3f) == 0 && ((1L << (_la - 10)) & ((1L << (WHEN_KEYWORD - 10))
                        | (1L << (UNITS_KEYWORD - 10))
                        | (1L << (TYPE_KEYWORD - 10))
                        | (1L << (STATUS_KEYWORD - 10))
                        | (1L << (REFERENCE_KEYWORD - 10))
                        | (1L << (MUST_KEYWORD - 10))
                        | (1L << (MANDATORY_KEYWORD - 10))
                        | (1L << (IF_FEATURE_KEYWORD - 10))
                        | (1L << (DESCRIPTION_KEYWORD - 10))
                        | (1L << (DEFAULT_KEYWORD - 10))
                        | (1L << (CONFIG_KEYWORD - 10)) | (1L << (IDENTIFIER - 10)))) != 0)) {
                    {
                        setState(683);
                        switch (_input.LA(1)) {
                        case IDENTIFIER: {
                            setState(671);
                            identifier_stmt();
                        }
                            break;
                        case WHEN_KEYWORD: {
                            setState(672);
                            when_stmt();
                        }
                            break;
                        case IF_FEATURE_KEYWORD: {
                            setState(673);
                            if_feature_stmt();
                        }
                            break;
                        case TYPE_KEYWORD: {
                            setState(674);
                            type_stmt();
                        }
                            break;
                        case UNITS_KEYWORD: {
                            setState(675);
                            units_stmt();
                        }
                            break;
                        case MUST_KEYWORD: {
                            setState(676);
                            must_stmt();
                        }
                            break;
                        case DEFAULT_KEYWORD: {
                            setState(677);
                            default_stmt();
                        }
                            break;
                        case CONFIG_KEYWORD: {
                            setState(678);
                            config_stmt();
                        }
                            break;
                        case MANDATORY_KEYWORD: {
                            setState(679);
                            mandatory_stmt();
                        }
                            break;
                        case STATUS_KEYWORD: {
                            setState(680);
                            status_stmt();
                        }
                            break;
                        case DESCRIPTION_KEYWORD: {
                            setState(681);
                            description_stmt();
                        }
                            break;
                        case REFERENCE_KEYWORD: {
                            setState(682);
                            reference_stmt();
                        }
                            break;
                        default:
                            throw new NoViableAltException(this);
                        }
                    }
                    setState(687);
                    _errHandler.sync(this);
                    _la = _input.LA(1);
                }
                setState(688);
                match(RIGHT_BRACE);
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    public static class Container_stmtContext extends ParserRuleContext {
        public TerminalNode RIGHT_BRACE() {
            return getToken(YangParser.RIGHT_BRACE, 0);
        }

        public List<When_stmtContext> when_stmt() {
            return getRuleContexts(When_stmtContext.class);
        }

        public Data_def_stmtContext data_def_stmt(int i) {
            return getRuleContext(Data_def_stmtContext.class, i);
        }

        public List<Typedef_stmtContext> typedef_stmt() {
            return getRuleContexts(Typedef_stmtContext.class);
        }

        public List<Data_def_stmtContext> data_def_stmt() {
            return getRuleContexts(Data_def_stmtContext.class);
        }

        public TerminalNode SEMICOLON() {
            return getToken(YangParser.SEMICOLON, 0);
        }

        public Config_stmtContext config_stmt(int i) {
            return getRuleContext(Config_stmtContext.class, i);
        }

        public List<Description_stmtContext> description_stmt() {
            return getRuleContexts(Description_stmtContext.class);
        }

        public When_stmtContext when_stmt(int i) {
            return getRuleContext(When_stmtContext.class, i);
        }

        public List<Grouping_stmtContext> grouping_stmt() {
            return getRuleContexts(Grouping_stmtContext.class);
        }

        public TerminalNode CONTAINER_KEYWORD() {
            return getToken(YangParser.CONTAINER_KEYWORD, 0);
        }

        public List<Presence_stmtContext> presence_stmt() {
            return getRuleContexts(Presence_stmtContext.class);
        }

        public List<Reference_stmtContext> reference_stmt() {
            return getRuleContexts(Reference_stmtContext.class);
        }

        public Typedef_stmtContext typedef_stmt(int i) {
            return getRuleContext(Typedef_stmtContext.class, i);
        }

        public Description_stmtContext description_stmt(int i) {
            return getRuleContext(Description_stmtContext.class, i);
        }

        public Grouping_stmtContext grouping_stmt(int i) {
            return getRuleContext(Grouping_stmtContext.class, i);
        }

        public If_feature_stmtContext if_feature_stmt(int i) {
            return getRuleContext(If_feature_stmtContext.class, i);
        }

        public TerminalNode LEFT_BRACE() {
            return getToken(YangParser.LEFT_BRACE, 0);
        }

        public Status_stmtContext status_stmt(int i) {
            return getRuleContext(Status_stmtContext.class, i);
        }

        public List<If_feature_stmtContext> if_feature_stmt() {
            return getRuleContexts(If_feature_stmtContext.class);
        }

        public Must_stmtContext must_stmt(int i) {
            return getRuleContext(Must_stmtContext.class, i);
        }

        public Identifier_stmtContext identifier_stmt(int i) {
            return getRuleContext(Identifier_stmtContext.class, i);
        }

        public List<Status_stmtContext> status_stmt() {
            return getRuleContexts(Status_stmtContext.class);
        }

        public Presence_stmtContext presence_stmt(int i) {
            return getRuleContext(Presence_stmtContext.class, i);
        }

        public List<Must_stmtContext> must_stmt() {
            return getRuleContexts(Must_stmtContext.class);
        }

        public List<Identifier_stmtContext> identifier_stmt() {
            return getRuleContexts(Identifier_stmtContext.class);
        }

        public StringContext string() {
            return getRuleContext(StringContext.class, 0);
        }

        public List<Config_stmtContext> config_stmt() {
            return getRuleContexts(Config_stmtContext.class);
        }

        public Reference_stmtContext reference_stmt(int i) {
            return getRuleContext(Reference_stmtContext.class, i);
        }

        public Container_stmtContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_container_stmt;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).enterContainer_stmt(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).exitContainer_stmt(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof YangParserVisitor)
                return ((YangParserVisitor<? extends T>) visitor)
                        .visitContainer_stmt(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final Container_stmtContext container_stmt()
            throws RecognitionException {
        Container_stmtContext _localctx = new Container_stmtContext(_ctx,
                getState());
        enterRule(_localctx, 70, RULE_container_stmt);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(690);
                match(CONTAINER_KEYWORD);
                setState(691);
                string();
                setState(712);
                switch (_input.LA(1)) {
                case SEMICOLON: {
                    setState(692);
                    match(SEMICOLON);
                }
                    break;
                case LEFT_BRACE: {
                    {
                        setState(693);
                        match(LEFT_BRACE);
                        setState(708);
                        _errHandler.sync(this);
                        _la = _input.LA(1);
                        while (((((_la - 10)) & ~0x3f) == 0 && ((1L << (_la - 10)) & ((1L << (WHEN_KEYWORD - 10))
                                | (1L << (USES_KEYWORD - 10))
                                | (1L << (TYPEDEF_KEYWORD - 10))
                                | (1L << (STATUS_KEYWORD - 10))
                                | (1L << (REFERENCE_KEYWORD - 10))
                                | (1L << (PRESENCE_KEYWORD - 10))
                                | (1L << (MUST_KEYWORD - 10))
                                | (1L << (LIST_KEYWORD - 10))
                                | (1L << (LEAF_LIST_KEYWORD - 10))
                                | (1L << (LEAF_KEYWORD - 10))
                                | (1L << (IF_FEATURE_KEYWORD - 10))
                                | (1L << (GROUPING_KEYWORD - 10))
                                | (1L << (DESCRIPTION_KEYWORD - 10))
                                | (1L << (CONTAINER_KEYWORD - 10))
                                | (1L << (CONFIG_KEYWORD - 10))
                                | (1L << (CHOICE_KEYWORD - 10))
                                | (1L << (ANYXML_KEYWORD - 10)) | (1L << (IDENTIFIER - 10)))) != 0)) {
                            {
                                setState(706);
                                switch (_input.LA(1)) {
                                case IDENTIFIER: {
                                    setState(694);
                                    identifier_stmt();
                                }
                                    break;
                                case WHEN_KEYWORD: {
                                    setState(695);
                                    when_stmt();
                                }
                                    break;
                                case IF_FEATURE_KEYWORD: {
                                    setState(696);
                                    if_feature_stmt();
                                }
                                    break;
                                case MUST_KEYWORD: {
                                    setState(697);
                                    must_stmt();
                                }
                                    break;
                                case PRESENCE_KEYWORD: {
                                    setState(698);
                                    presence_stmt();
                                }
                                    break;
                                case CONFIG_KEYWORD: {
                                    setState(699);
                                    config_stmt();
                                }
                                    break;
                                case STATUS_KEYWORD: {
                                    setState(700);
                                    status_stmt();
                                }
                                    break;
                                case DESCRIPTION_KEYWORD: {
                                    setState(701);
                                    description_stmt();
                                }
                                    break;
                                case REFERENCE_KEYWORD: {
                                    setState(702);
                                    reference_stmt();
                                }
                                    break;
                                case TYPEDEF_KEYWORD: {
                                    setState(703);
                                    typedef_stmt();
                                }
                                    break;
                                case GROUPING_KEYWORD: {
                                    setState(704);
                                    grouping_stmt();
                                }
                                    break;
                                case USES_KEYWORD:
                                case LIST_KEYWORD:
                                case LEAF_LIST_KEYWORD:
                                case LEAF_KEYWORD:
                                case CONTAINER_KEYWORD:
                                case CHOICE_KEYWORD:
                                case ANYXML_KEYWORD: {
                                    setState(705);
                                    data_def_stmt();
                                }
                                    break;
                                default:
                                    throw new NoViableAltException(this);
                                }
                            }
                            setState(710);
                            _errHandler.sync(this);
                            _la = _input.LA(1);
                        }
                        setState(711);
                        match(RIGHT_BRACE);
                    }
                }
                    break;
                default:
                    throw new NoViableAltException(this);
                }
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    public static class Grouping_stmtContext extends ParserRuleContext {
        public List<Grouping_stmtContext> grouping_stmt() {
            return getRuleContexts(Grouping_stmtContext.class);
        }

        public TerminalNode RIGHT_BRACE() {
            return getToken(YangParser.RIGHT_BRACE, 0);
        }

        public List<Reference_stmtContext> reference_stmt() {
            return getRuleContexts(Reference_stmtContext.class);
        }

        public Typedef_stmtContext typedef_stmt(int i) {
            return getRuleContext(Typedef_stmtContext.class, i);
        }

        public Description_stmtContext description_stmt(int i) {
            return getRuleContext(Description_stmtContext.class, i);
        }

        public Grouping_stmtContext grouping_stmt(int i) {
            return getRuleContext(Grouping_stmtContext.class, i);
        }

        public TerminalNode LEFT_BRACE() {
            return getToken(YangParser.LEFT_BRACE, 0);
        }

        public Data_def_stmtContext data_def_stmt(int i) {
            return getRuleContext(Data_def_stmtContext.class, i);
        }

        public List<Typedef_stmtContext> typedef_stmt() {
            return getRuleContexts(Typedef_stmtContext.class);
        }

        public Status_stmtContext status_stmt(int i) {
            return getRuleContext(Status_stmtContext.class, i);
        }

        public Identifier_stmtContext identifier_stmt(int i) {
            return getRuleContext(Identifier_stmtContext.class, i);
        }

        public List<Data_def_stmtContext> data_def_stmt() {
            return getRuleContexts(Data_def_stmtContext.class);
        }

        public TerminalNode GROUPING_KEYWORD() {
            return getToken(YangParser.GROUPING_KEYWORD, 0);
        }

        public TerminalNode SEMICOLON() {
            return getToken(YangParser.SEMICOLON, 0);
        }

        public List<Status_stmtContext> status_stmt() {
            return getRuleContexts(Status_stmtContext.class);
        }

        public StringContext string() {
            return getRuleContext(StringContext.class, 0);
        }

        public List<Identifier_stmtContext> identifier_stmt() {
            return getRuleContexts(Identifier_stmtContext.class);
        }

        public List<Description_stmtContext> description_stmt() {
            return getRuleContexts(Description_stmtContext.class);
        }

        public Reference_stmtContext reference_stmt(int i) {
            return getRuleContext(Reference_stmtContext.class, i);
        }

        public Grouping_stmtContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_grouping_stmt;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).enterGrouping_stmt(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).exitGrouping_stmt(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof YangParserVisitor)
                return ((YangParserVisitor<? extends T>) visitor)
                        .visitGrouping_stmt(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final Grouping_stmtContext grouping_stmt()
            throws RecognitionException {
        Grouping_stmtContext _localctx = new Grouping_stmtContext(_ctx,
                getState());
        enterRule(_localctx, 72, RULE_grouping_stmt);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(714);
                match(GROUPING_KEYWORD);
                setState(715);
                string();
                setState(731);
                switch (_input.LA(1)) {
                case SEMICOLON: {
                    setState(716);
                    match(SEMICOLON);
                }
                    break;
                case LEFT_BRACE: {
                    {
                        setState(717);
                        match(LEFT_BRACE);
                        setState(727);
                        _errHandler.sync(this);
                        _la = _input.LA(1);
                        while (((((_la - 12)) & ~0x3f) == 0 && ((1L << (_la - 12)) & ((1L << (USES_KEYWORD - 12))
                                | (1L << (TYPEDEF_KEYWORD - 12))
                                | (1L << (STATUS_KEYWORD - 12))
                                | (1L << (REFERENCE_KEYWORD - 12))
                                | (1L << (LIST_KEYWORD - 12))
                                | (1L << (LEAF_LIST_KEYWORD - 12))
                                | (1L << (LEAF_KEYWORD - 12))
                                | (1L << (GROUPING_KEYWORD - 12))
                                | (1L << (DESCRIPTION_KEYWORD - 12))
                                | (1L << (CONTAINER_KEYWORD - 12))
                                | (1L << (CHOICE_KEYWORD - 12))
                                | (1L << (ANYXML_KEYWORD - 12)) | (1L << (IDENTIFIER - 12)))) != 0)) {
                            {
                                setState(725);
                                switch (_input.LA(1)) {
                                case IDENTIFIER: {
                                    setState(718);
                                    identifier_stmt();
                                }
                                    break;
                                case STATUS_KEYWORD: {
                                    setState(719);
                                    status_stmt();
                                }
                                    break;
                                case DESCRIPTION_KEYWORD: {
                                    setState(720);
                                    description_stmt();
                                }
                                    break;
                                case REFERENCE_KEYWORD: {
                                    setState(721);
                                    reference_stmt();
                                }
                                    break;
                                case TYPEDEF_KEYWORD: {
                                    setState(722);
                                    typedef_stmt();
                                }
                                    break;
                                case GROUPING_KEYWORD: {
                                    setState(723);
                                    grouping_stmt();
                                }
                                    break;
                                case USES_KEYWORD:
                                case LIST_KEYWORD:
                                case LEAF_LIST_KEYWORD:
                                case LEAF_KEYWORD:
                                case CONTAINER_KEYWORD:
                                case CHOICE_KEYWORD:
                                case ANYXML_KEYWORD: {
                                    setState(724);
                                    data_def_stmt();
                                }
                                    break;
                                default:
                                    throw new NoViableAltException(this);
                                }
                            }
                            setState(729);
                            _errHandler.sync(this);
                            _la = _input.LA(1);
                        }
                        setState(730);
                        match(RIGHT_BRACE);
                    }
                }
                    break;
                default:
                    throw new NoViableAltException(this);
                }
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    public static class Value_stmtContext extends ParserRuleContext {
        public StringContext string() {
            return getRuleContext(StringContext.class, 0);
        }

        public StmtendContext stmtend() {
            return getRuleContext(StmtendContext.class, 0);
        }

        public TerminalNode VALUE_KEYWORD() {
            return getToken(YangParser.VALUE_KEYWORD, 0);
        }

        public Value_stmtContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_value_stmt;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).enterValue_stmt(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).exitValue_stmt(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof YangParserVisitor)
                return ((YangParserVisitor<? extends T>) visitor)
                        .visitValue_stmt(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final Value_stmtContext value_stmt() throws RecognitionException {
        Value_stmtContext _localctx = new Value_stmtContext(_ctx, getState());
        enterRule(_localctx, 74, RULE_value_stmt);
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(733);
                match(VALUE_KEYWORD);
                setState(734);
                string();
                setState(735);
                stmtend();
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    public static class Max_value_argContext extends ParserRuleContext {
        public StringContext string() {
            return getRuleContext(StringContext.class, 0);
        }

        public Max_value_argContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_max_value_arg;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).enterMax_value_arg(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).exitMax_value_arg(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof YangParserVisitor)
                return ((YangParserVisitor<? extends T>) visitor)
                        .visitMax_value_arg(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final Max_value_argContext max_value_arg()
            throws RecognitionException {
        Max_value_argContext _localctx = new Max_value_argContext(_ctx,
                getState());
        enterRule(_localctx, 76, RULE_max_value_arg);
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(737);
                string();
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    public static class Max_elements_stmtContext extends ParserRuleContext {
        public TerminalNode MAX_ELEMENTS_KEYWORD() {
            return getToken(YangParser.MAX_ELEMENTS_KEYWORD, 0);
        }

        public Max_value_argContext max_value_arg() {
            return getRuleContext(Max_value_argContext.class, 0);
        }

        public StmtendContext stmtend() {
            return getRuleContext(StmtendContext.class, 0);
        }

        public Max_elements_stmtContext(ParserRuleContext parent,
                int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_max_elements_stmt;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).enterMax_elements_stmt(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).exitMax_elements_stmt(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof YangParserVisitor)
                return ((YangParserVisitor<? extends T>) visitor)
                        .visitMax_elements_stmt(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final Max_elements_stmtContext max_elements_stmt()
            throws RecognitionException {
        Max_elements_stmtContext _localctx = new Max_elements_stmtContext(_ctx,
                getState());
        enterRule(_localctx, 78, RULE_max_elements_stmt);
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(739);
                match(MAX_ELEMENTS_KEYWORD);
                setState(740);
                max_value_arg();
                setState(741);
                stmtend();
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    public static class Min_elements_stmtContext extends ParserRuleContext {
        public StringContext string() {
            return getRuleContext(StringContext.class, 0);
        }

        public StmtendContext stmtend() {
            return getRuleContext(StmtendContext.class, 0);
        }

        public TerminalNode MIN_ELEMENTS_KEYWORD() {
            return getToken(YangParser.MIN_ELEMENTS_KEYWORD, 0);
        }

        public Min_elements_stmtContext(ParserRuleContext parent,
                int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_min_elements_stmt;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).enterMin_elements_stmt(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).exitMin_elements_stmt(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof YangParserVisitor)
                return ((YangParserVisitor<? extends T>) visitor)
                        .visitMin_elements_stmt(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final Min_elements_stmtContext min_elements_stmt()
            throws RecognitionException {
        Min_elements_stmtContext _localctx = new Min_elements_stmtContext(_ctx,
                getState());
        enterRule(_localctx, 80, RULE_min_elements_stmt);
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(743);
                match(MIN_ELEMENTS_KEYWORD);
                setState(744);
                string();
                setState(745);
                stmtend();
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    public static class Error_app_tag_stmtContext extends ParserRuleContext {
        public StringContext string() {
            return getRuleContext(StringContext.class, 0);
        }

        public TerminalNode ERROR_APP_TAG_KEYWORD() {
            return getToken(YangParser.ERROR_APP_TAG_KEYWORD, 0);
        }

        public StmtendContext stmtend() {
            return getRuleContext(StmtendContext.class, 0);
        }

        public Error_app_tag_stmtContext(ParserRuleContext parent,
                int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_error_app_tag_stmt;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).enterError_app_tag_stmt(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).exitError_app_tag_stmt(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof YangParserVisitor)
                return ((YangParserVisitor<? extends T>) visitor)
                        .visitError_app_tag_stmt(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final Error_app_tag_stmtContext error_app_tag_stmt()
            throws RecognitionException {
        Error_app_tag_stmtContext _localctx = new Error_app_tag_stmtContext(
                _ctx, getState());
        enterRule(_localctx, 82, RULE_error_app_tag_stmt);
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(747);
                match(ERROR_APP_TAG_KEYWORD);
                setState(748);
                string();
                setState(749);
                stmtend();
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    public static class Error_message_stmtContext extends ParserRuleContext {
        public StringContext string() {
            return getRuleContext(StringContext.class, 0);
        }

        public TerminalNode ERROR_MESSAGE_KEYWORD() {
            return getToken(YangParser.ERROR_MESSAGE_KEYWORD, 0);
        }

        public StmtendContext stmtend() {
            return getRuleContext(StmtendContext.class, 0);
        }

        public Error_message_stmtContext(ParserRuleContext parent,
                int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_error_message_stmt;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).enterError_message_stmt(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).exitError_message_stmt(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof YangParserVisitor)
                return ((YangParserVisitor<? extends T>) visitor)
                        .visitError_message_stmt(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final Error_message_stmtContext error_message_stmt()
            throws RecognitionException {
        Error_message_stmtContext _localctx = new Error_message_stmtContext(
                _ctx, getState());
        enterRule(_localctx, 84, RULE_error_message_stmt);
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(751);
                match(ERROR_MESSAGE_KEYWORD);
                setState(752);
                string();
                setState(753);
                stmtend();
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    public static class Must_stmtContext extends ParserRuleContext {
        public TerminalNode RIGHT_BRACE() {
            return getToken(YangParser.RIGHT_BRACE, 0);
        }

        public List<Reference_stmtContext> reference_stmt() {
            return getRuleContexts(Reference_stmtContext.class);
        }

        public Description_stmtContext description_stmt(int i) {
            return getRuleContext(Description_stmtContext.class, i);
        }

        public TerminalNode LEFT_BRACE() {
            return getToken(YangParser.LEFT_BRACE, 0);
        }

        public List<Error_app_tag_stmtContext> error_app_tag_stmt() {
            return getRuleContexts(Error_app_tag_stmtContext.class);
        }

        public TerminalNode MUST_KEYWORD() {
            return getToken(YangParser.MUST_KEYWORD, 0);
        }

        public Error_message_stmtContext error_message_stmt(int i) {
            return getRuleContext(Error_message_stmtContext.class, i);
        }

        public Identifier_stmtContext identifier_stmt(int i) {
            return getRuleContext(Identifier_stmtContext.class, i);
        }

        public TerminalNode SEMICOLON() {
            return getToken(YangParser.SEMICOLON, 0);
        }

        public StringContext string() {
            return getRuleContext(StringContext.class, 0);
        }

        public List<Identifier_stmtContext> identifier_stmt() {
            return getRuleContexts(Identifier_stmtContext.class);
        }

        public Error_app_tag_stmtContext error_app_tag_stmt(int i) {
            return getRuleContext(Error_app_tag_stmtContext.class, i);
        }

        public List<Error_message_stmtContext> error_message_stmt() {
            return getRuleContexts(Error_message_stmtContext.class);
        }

        public List<Description_stmtContext> description_stmt() {
            return getRuleContexts(Description_stmtContext.class);
        }

        public Reference_stmtContext reference_stmt(int i) {
            return getRuleContext(Reference_stmtContext.class, i);
        }

        public Must_stmtContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_must_stmt;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).enterMust_stmt(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).exitMust_stmt(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof YangParserVisitor)
                return ((YangParserVisitor<? extends T>) visitor)
                        .visitMust_stmt(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final Must_stmtContext must_stmt() throws RecognitionException {
        Must_stmtContext _localctx = new Must_stmtContext(_ctx, getState());
        enterRule(_localctx, 86, RULE_must_stmt);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(755);
                match(MUST_KEYWORD);
                setState(756);
                string();
                setState(770);
                switch (_input.LA(1)) {
                case SEMICOLON: {
                    setState(757);
                    match(SEMICOLON);
                }
                    break;
                case LEFT_BRACE: {
                    {
                        setState(758);
                        match(LEFT_BRACE);
                        setState(766);
                        _errHandler.sync(this);
                        _la = _input.LA(1);
                        while (((((_la - 24)) & ~0x3f) == 0 && ((1L << (_la - 24)) & ((1L << (REFERENCE_KEYWORD - 24))
                                | (1L << (ERROR_MESSAGE_KEYWORD - 24))
                                | (1L << (ERROR_APP_TAG_KEYWORD - 24))
                                | (1L << (DESCRIPTION_KEYWORD - 24)) | (1L << (IDENTIFIER - 24)))) != 0)) {
                            {
                                setState(764);
                                switch (_input.LA(1)) {
                                case IDENTIFIER: {
                                    setState(759);
                                    identifier_stmt();
                                }
                                    break;
                                case ERROR_MESSAGE_KEYWORD: {
                                    setState(760);
                                    error_message_stmt();
                                }
                                    break;
                                case ERROR_APP_TAG_KEYWORD: {
                                    setState(761);
                                    error_app_tag_stmt();
                                }
                                    break;
                                case DESCRIPTION_KEYWORD: {
                                    setState(762);
                                    description_stmt();
                                }
                                    break;
                                case REFERENCE_KEYWORD: {
                                    setState(763);
                                    reference_stmt();
                                }
                                    break;
                                default:
                                    throw new NoViableAltException(this);
                                }
                            }
                            setState(768);
                            _errHandler.sync(this);
                            _la = _input.LA(1);
                        }
                        setState(769);
                        match(RIGHT_BRACE);
                    }
                }
                    break;
                default:
                    throw new NoViableAltException(this);
                }
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    public static class Ordered_by_argContext extends ParserRuleContext {
        public StringContext string() {
            return getRuleContext(StringContext.class, 0);
        }

        public Ordered_by_argContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_ordered_by_arg;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).enterOrdered_by_arg(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).exitOrdered_by_arg(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof YangParserVisitor)
                return ((YangParserVisitor<? extends T>) visitor)
                        .visitOrdered_by_arg(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final Ordered_by_argContext ordered_by_arg()
            throws RecognitionException {
        Ordered_by_argContext _localctx = new Ordered_by_argContext(_ctx,
                getState());
        enterRule(_localctx, 88, RULE_ordered_by_arg);
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(772);
                string();
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    public static class Ordered_by_stmtContext extends ParserRuleContext {
        public Ordered_by_argContext ordered_by_arg() {
            return getRuleContext(Ordered_by_argContext.class, 0);
        }

        public TerminalNode ORDERED_BY_KEYWORD() {
            return getToken(YangParser.ORDERED_BY_KEYWORD, 0);
        }

        public StmtendContext stmtend() {
            return getRuleContext(StmtendContext.class, 0);
        }

        public Ordered_by_stmtContext(ParserRuleContext parent,
                int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_ordered_by_stmt;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).enterOrdered_by_stmt(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).exitOrdered_by_stmt(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof YangParserVisitor)
                return ((YangParserVisitor<? extends T>) visitor)
                        .visitOrdered_by_stmt(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final Ordered_by_stmtContext ordered_by_stmt()
            throws RecognitionException {
        Ordered_by_stmtContext _localctx = new Ordered_by_stmtContext(_ctx,
                getState());
        enterRule(_localctx, 90, RULE_ordered_by_stmt);
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(774);
                match(ORDERED_BY_KEYWORD);
                setState(775);
                ordered_by_arg();
                setState(776);
                stmtend();
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    public static class Presence_stmtContext extends ParserRuleContext {
        public StringContext string() {
            return getRuleContext(StringContext.class, 0);
        }

        public StmtendContext stmtend() {
            return getRuleContext(StmtendContext.class, 0);
        }

        public TerminalNode PRESENCE_KEYWORD() {
            return getToken(YangParser.PRESENCE_KEYWORD, 0);
        }

        public Presence_stmtContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_presence_stmt;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).enterPresence_stmt(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).exitPresence_stmt(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof YangParserVisitor)
                return ((YangParserVisitor<? extends T>) visitor)
                        .visitPresence_stmt(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final Presence_stmtContext presence_stmt()
            throws RecognitionException {
        Presence_stmtContext _localctx = new Presence_stmtContext(_ctx,
                getState());
        enterRule(_localctx, 92, RULE_presence_stmt);
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(778);
                match(PRESENCE_KEYWORD);
                setState(779);
                string();
                setState(780);
                stmtend();
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    public static class Mandatory_argContext extends ParserRuleContext {
        public StringContext string() {
            return getRuleContext(StringContext.class, 0);
        }

        public Mandatory_argContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_mandatory_arg;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).enterMandatory_arg(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).exitMandatory_arg(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof YangParserVisitor)
                return ((YangParserVisitor<? extends T>) visitor)
                        .visitMandatory_arg(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final Mandatory_argContext mandatory_arg()
            throws RecognitionException {
        Mandatory_argContext _localctx = new Mandatory_argContext(_ctx,
                getState());
        enterRule(_localctx, 94, RULE_mandatory_arg);
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(782);
                string();
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    public static class Mandatory_stmtContext extends ParserRuleContext {
        public TerminalNode MANDATORY_KEYWORD() {
            return getToken(YangParser.MANDATORY_KEYWORD, 0);
        }

        public Mandatory_argContext mandatory_arg() {
            return getRuleContext(Mandatory_argContext.class, 0);
        }

        public StmtendContext stmtend() {
            return getRuleContext(StmtendContext.class, 0);
        }

        public Mandatory_stmtContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_mandatory_stmt;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).enterMandatory_stmt(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).exitMandatory_stmt(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof YangParserVisitor)
                return ((YangParserVisitor<? extends T>) visitor)
                        .visitMandatory_stmt(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final Mandatory_stmtContext mandatory_stmt()
            throws RecognitionException {
        Mandatory_stmtContext _localctx = new Mandatory_stmtContext(_ctx,
                getState());
        enterRule(_localctx, 96, RULE_mandatory_stmt);
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(784);
                match(MANDATORY_KEYWORD);
                setState(785);
                mandatory_arg();
                setState(786);
                stmtend();
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    public static class Config_argContext extends ParserRuleContext {
        public StringContext string() {
            return getRuleContext(StringContext.class, 0);
        }

        public Config_argContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_config_arg;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).enterConfig_arg(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).exitConfig_arg(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof YangParserVisitor)
                return ((YangParserVisitor<? extends T>) visitor)
                        .visitConfig_arg(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final Config_argContext config_arg() throws RecognitionException {
        Config_argContext _localctx = new Config_argContext(_ctx, getState());
        enterRule(_localctx, 98, RULE_config_arg);
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(788);
                string();
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    public static class Config_stmtContext extends ParserRuleContext {
        public Config_argContext config_arg() {
            return getRuleContext(Config_argContext.class, 0);
        }

        public TerminalNode CONFIG_KEYWORD() {
            return getToken(YangParser.CONFIG_KEYWORD, 0);
        }

        public StmtendContext stmtend() {
            return getRuleContext(StmtendContext.class, 0);
        }

        public Config_stmtContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_config_stmt;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).enterConfig_stmt(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).exitConfig_stmt(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof YangParserVisitor)
                return ((YangParserVisitor<? extends T>) visitor)
                        .visitConfig_stmt(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final Config_stmtContext config_stmt() throws RecognitionException {
        Config_stmtContext _localctx = new Config_stmtContext(_ctx, getState());
        enterRule(_localctx, 100, RULE_config_stmt);
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(790);
                match(CONFIG_KEYWORD);
                setState(791);
                config_arg();
                setState(792);
                stmtend();
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    public static class Status_argContext extends ParserRuleContext {
        public StringContext string() {
            return getRuleContext(StringContext.class, 0);
        }

        public Status_argContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_status_arg;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).enterStatus_arg(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).exitStatus_arg(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof YangParserVisitor)
                return ((YangParserVisitor<? extends T>) visitor)
                        .visitStatus_arg(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final Status_argContext status_arg() throws RecognitionException {
        Status_argContext _localctx = new Status_argContext(_ctx, getState());
        enterRule(_localctx, 102, RULE_status_arg);
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(794);
                string();
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    public static class Status_stmtContext extends ParserRuleContext {
        public Status_argContext status_arg() {
            return getRuleContext(Status_argContext.class, 0);
        }

        public TerminalNode STATUS_KEYWORD() {
            return getToken(YangParser.STATUS_KEYWORD, 0);
        }

        public StmtendContext stmtend() {
            return getRuleContext(StmtendContext.class, 0);
        }

        public Status_stmtContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_status_stmt;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).enterStatus_stmt(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).exitStatus_stmt(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof YangParserVisitor)
                return ((YangParserVisitor<? extends T>) visitor)
                        .visitStatus_stmt(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final Status_stmtContext status_stmt() throws RecognitionException {
        Status_stmtContext _localctx = new Status_stmtContext(_ctx, getState());
        enterRule(_localctx, 104, RULE_status_stmt);
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(796);
                match(STATUS_KEYWORD);
                setState(797);
                status_arg();
                setState(798);
                stmtend();
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    public static class Position_stmtContext extends ParserRuleContext {
        public TerminalNode POSITION_KEYWORD() {
            return getToken(YangParser.POSITION_KEYWORD, 0);
        }

        public StringContext string() {
            return getRuleContext(StringContext.class, 0);
        }

        public StmtendContext stmtend() {
            return getRuleContext(StmtendContext.class, 0);
        }

        public Position_stmtContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_position_stmt;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).enterPosition_stmt(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).exitPosition_stmt(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof YangParserVisitor)
                return ((YangParserVisitor<? extends T>) visitor)
                        .visitPosition_stmt(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final Position_stmtContext position_stmt()
            throws RecognitionException {
        Position_stmtContext _localctx = new Position_stmtContext(_ctx,
                getState());
        enterRule(_localctx, 106, RULE_position_stmt);
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(800);
                match(POSITION_KEYWORD);
                setState(801);
                string();
                setState(802);
                stmtend();
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    public static class Bit_stmtContext extends ParserRuleContext {
        public TerminalNode RIGHT_BRACE() {
            return getToken(YangParser.RIGHT_BRACE, 0);
        }

        public List<Reference_stmtContext> reference_stmt() {
            return getRuleContexts(Reference_stmtContext.class);
        }

        public Position_stmtContext position_stmt(int i) {
            return getRuleContext(Position_stmtContext.class, i);
        }

        public Description_stmtContext description_stmt(int i) {
            return getRuleContext(Description_stmtContext.class, i);
        }

        public List<Position_stmtContext> position_stmt() {
            return getRuleContexts(Position_stmtContext.class);
        }

        public TerminalNode LEFT_BRACE() {
            return getToken(YangParser.LEFT_BRACE, 0);
        }

        public Status_stmtContext status_stmt(int i) {
            return getRuleContext(Status_stmtContext.class, i);
        }

        public Identifier_stmtContext identifier_stmt(int i) {
            return getRuleContext(Identifier_stmtContext.class, i);
        }

        public TerminalNode SEMICOLON() {
            return getToken(YangParser.SEMICOLON, 0);
        }

        public List<Status_stmtContext> status_stmt() {
            return getRuleContexts(Status_stmtContext.class);
        }

        public StringContext string() {
            return getRuleContext(StringContext.class, 0);
        }

        public List<Identifier_stmtContext> identifier_stmt() {
            return getRuleContexts(Identifier_stmtContext.class);
        }

        public List<Description_stmtContext> description_stmt() {
            return getRuleContexts(Description_stmtContext.class);
        }

        public Reference_stmtContext reference_stmt(int i) {
            return getRuleContext(Reference_stmtContext.class, i);
        }

        public TerminalNode BIT_KEYWORD() {
            return getToken(YangParser.BIT_KEYWORD, 0);
        }

        public Bit_stmtContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_bit_stmt;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).enterBit_stmt(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).exitBit_stmt(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof YangParserVisitor)
                return ((YangParserVisitor<? extends T>) visitor)
                        .visitBit_stmt(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final Bit_stmtContext bit_stmt() throws RecognitionException {
        Bit_stmtContext _localctx = new Bit_stmtContext(_ctx, getState());
        enterRule(_localctx, 108, RULE_bit_stmt);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(804);
                match(BIT_KEYWORD);
                setState(805);
                string();
                setState(819);
                switch (_input.LA(1)) {
                case SEMICOLON: {
                    setState(806);
                    match(SEMICOLON);
                }
                    break;
                case LEFT_BRACE: {
                    {
                        setState(807);
                        match(LEFT_BRACE);
                        setState(815);
                        _errHandler.sync(this);
                        _la = _input.LA(1);
                        while (((((_la - 18)) & ~0x3f) == 0 && ((1L << (_la - 18)) & ((1L << (STATUS_KEYWORD - 18))
                                | (1L << (REFERENCE_KEYWORD - 18))
                                | (1L << (POSITION_KEYWORD - 18))
                                | (1L << (DESCRIPTION_KEYWORD - 18)) | (1L << (IDENTIFIER - 18)))) != 0)) {
                            {
                                setState(813);
                                switch (_input.LA(1)) {
                                case IDENTIFIER: {
                                    setState(808);
                                    identifier_stmt();
                                }
                                    break;
                                case POSITION_KEYWORD: {
                                    setState(809);
                                    position_stmt();
                                }
                                    break;
                                case STATUS_KEYWORD: {
                                    setState(810);
                                    status_stmt();
                                }
                                    break;
                                case DESCRIPTION_KEYWORD: {
                                    setState(811);
                                    description_stmt();
                                }
                                    break;
                                case REFERENCE_KEYWORD: {
                                    setState(812);
                                    reference_stmt();
                                }
                                    break;
                                default:
                                    throw new NoViableAltException(this);
                                }
                            }
                            setState(817);
                            _errHandler.sync(this);
                            _la = _input.LA(1);
                        }
                        setState(818);
                        match(RIGHT_BRACE);
                    }
                }
                    break;
                default:
                    throw new NoViableAltException(this);
                }
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    public static class Bits_specificationContext extends ParserRuleContext {
        public Bit_stmtContext bit_stmt(int i) {
            return getRuleContext(Bit_stmtContext.class, i);
        }

        public List<Bit_stmtContext> bit_stmt() {
            return getRuleContexts(Bit_stmtContext.class);
        }

        public Bits_specificationContext(ParserRuleContext parent,
                int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_bits_specification;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).enterBits_specification(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).exitBits_specification(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof YangParserVisitor)
                return ((YangParserVisitor<? extends T>) visitor)
                        .visitBits_specification(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final Bits_specificationContext bits_specification()
            throws RecognitionException {
        Bits_specificationContext _localctx = new Bits_specificationContext(
                _ctx, getState());
        enterRule(_localctx, 110, RULE_bits_specification);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(822);
                _errHandler.sync(this);
                _la = _input.LA(1);
                do {
                    {
                        {
                            setState(821);
                            bit_stmt();
                        }
                    }
                    setState(824);
                    _errHandler.sync(this);
                    _la = _input.LA(1);
                } while (_la == BIT_KEYWORD);
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    public static class Union_specificationContext extends ParserRuleContext {
        public Type_stmtContext type_stmt(int i) {
            return getRuleContext(Type_stmtContext.class, i);
        }

        public List<Type_stmtContext> type_stmt() {
            return getRuleContexts(Type_stmtContext.class);
        }

        public Union_specificationContext(ParserRuleContext parent,
                int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_union_specification;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).enterUnion_specification(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).exitUnion_specification(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof YangParserVisitor)
                return ((YangParserVisitor<? extends T>) visitor)
                        .visitUnion_specification(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final Union_specificationContext union_specification()
            throws RecognitionException {
        Union_specificationContext _localctx = new Union_specificationContext(
                _ctx, getState());
        enterRule(_localctx, 112, RULE_union_specification);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(827);
                _errHandler.sync(this);
                _la = _input.LA(1);
                do {
                    {
                        {
                            setState(826);
                            type_stmt();
                        }
                    }
                    setState(829);
                    _errHandler.sync(this);
                    _la = _input.LA(1);
                } while (_la == TYPE_KEYWORD);
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    public static class Identityref_specificationContext extends
            ParserRuleContext {
        public Base_stmtContext base_stmt() {
            return getRuleContext(Base_stmtContext.class, 0);
        }

        public Identityref_specificationContext(ParserRuleContext parent,
                int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_identityref_specification;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener)
                        .enterIdentityref_specification(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener)
                        .exitIdentityref_specification(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof YangParserVisitor)
                return ((YangParserVisitor<? extends T>) visitor)
                        .visitIdentityref_specification(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final Identityref_specificationContext identityref_specification()
            throws RecognitionException {
        Identityref_specificationContext _localctx = new Identityref_specificationContext(
                _ctx, getState());
        enterRule(_localctx, 114, RULE_identityref_specification);
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(831);
                base_stmt();
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    public static class Instance_identifier_specificationContext extends
            ParserRuleContext {
        public Require_instance_stmtContext require_instance_stmt() {
            return getRuleContext(Require_instance_stmtContext.class, 0);
        }

        public Instance_identifier_specificationContext(
                ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_instance_identifier_specification;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener)
                        .enterInstance_identifier_specification(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener)
                        .exitInstance_identifier_specification(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof YangParserVisitor)
                return ((YangParserVisitor<? extends T>) visitor)
                        .visitInstance_identifier_specification(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final Instance_identifier_specificationContext instance_identifier_specification()
            throws RecognitionException {
        Instance_identifier_specificationContext _localctx = new Instance_identifier_specificationContext(
                _ctx, getState());
        enterRule(_localctx, 116, RULE_instance_identifier_specification);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(834);
                _la = _input.LA(1);
                if (_la == REQUIRE_INSTANCE_KEYWORD) {
                    {
                        setState(833);
                        require_instance_stmt();
                    }
                }

            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    public static class Require_instance_argContext extends ParserRuleContext {
        public StringContext string() {
            return getRuleContext(StringContext.class, 0);
        }

        public Require_instance_argContext(ParserRuleContext parent,
                int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_require_instance_arg;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).enterRequire_instance_arg(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).exitRequire_instance_arg(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof YangParserVisitor)
                return ((YangParserVisitor<? extends T>) visitor)
                        .visitRequire_instance_arg(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final Require_instance_argContext require_instance_arg()
            throws RecognitionException {
        Require_instance_argContext _localctx = new Require_instance_argContext(
                _ctx, getState());
        enterRule(_localctx, 118, RULE_require_instance_arg);
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(836);
                string();
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    public static class Require_instance_stmtContext extends ParserRuleContext {
        public Require_instance_argContext require_instance_arg() {
            return getRuleContext(Require_instance_argContext.class, 0);
        }

        public TerminalNode REQUIRE_INSTANCE_KEYWORD() {
            return getToken(YangParser.REQUIRE_INSTANCE_KEYWORD, 0);
        }

        public StmtendContext stmtend() {
            return getRuleContext(StmtendContext.class, 0);
        }

        public Require_instance_stmtContext(ParserRuleContext parent,
                int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_require_instance_stmt;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener)
                        .enterRequire_instance_stmt(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).exitRequire_instance_stmt(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof YangParserVisitor)
                return ((YangParserVisitor<? extends T>) visitor)
                        .visitRequire_instance_stmt(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final Require_instance_stmtContext require_instance_stmt()
            throws RecognitionException {
        Require_instance_stmtContext _localctx = new Require_instance_stmtContext(
                _ctx, getState());
        enterRule(_localctx, 120, RULE_require_instance_stmt);
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(838);
                match(REQUIRE_INSTANCE_KEYWORD);
                setState(839);
                require_instance_arg();
                setState(840);
                stmtend();
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    public static class Path_stmtContext extends ParserRuleContext {
        public TerminalNode PATH_KEYWORD() {
            return getToken(YangParser.PATH_KEYWORD, 0);
        }

        public StringContext string() {
            return getRuleContext(StringContext.class, 0);
        }

        public StmtendContext stmtend() {
            return getRuleContext(StmtendContext.class, 0);
        }

        public Path_stmtContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_path_stmt;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).enterPath_stmt(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).exitPath_stmt(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof YangParserVisitor)
                return ((YangParserVisitor<? extends T>) visitor)
                        .visitPath_stmt(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final Path_stmtContext path_stmt() throws RecognitionException {
        Path_stmtContext _localctx = new Path_stmtContext(_ctx, getState());
        enterRule(_localctx, 122, RULE_path_stmt);
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(842);
                match(PATH_KEYWORD);
                setState(843);
                string();
                setState(844);
                stmtend();
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    public static class Leafref_specificationContext extends ParserRuleContext {
        public Path_stmtContext path_stmt() {
            return getRuleContext(Path_stmtContext.class, 0);
        }

        public Leafref_specificationContext(ParserRuleContext parent,
                int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_leafref_specification;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener)
                        .enterLeafref_specification(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).exitLeafref_specification(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof YangParserVisitor)
                return ((YangParserVisitor<? extends T>) visitor)
                        .visitLeafref_specification(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final Leafref_specificationContext leafref_specification()
            throws RecognitionException {
        Leafref_specificationContext _localctx = new Leafref_specificationContext(
                _ctx, getState());
        enterRule(_localctx, 124, RULE_leafref_specification);
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(846);
                path_stmt();
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    public static class Enum_stmtContext extends ParserRuleContext {
        public TerminalNode RIGHT_BRACE() {
            return getToken(YangParser.RIGHT_BRACE, 0);
        }

        public List<Reference_stmtContext> reference_stmt() {
            return getRuleContexts(Reference_stmtContext.class);
        }

        public Description_stmtContext description_stmt(int i) {
            return getRuleContext(Description_stmtContext.class, i);
        }

        public TerminalNode LEFT_BRACE() {
            return getToken(YangParser.LEFT_BRACE, 0);
        }

        public Status_stmtContext status_stmt(int i) {
            return getRuleContext(Status_stmtContext.class, i);
        }

        public Value_stmtContext value_stmt(int i) {
            return getRuleContext(Value_stmtContext.class, i);
        }

        public List<Value_stmtContext> value_stmt() {
            return getRuleContexts(Value_stmtContext.class);
        }

        public TerminalNode ENUM_KEYWORD() {
            return getToken(YangParser.ENUM_KEYWORD, 0);
        }

        public Identifier_stmtContext identifier_stmt(int i) {
            return getRuleContext(Identifier_stmtContext.class, i);
        }

        public TerminalNode SEMICOLON() {
            return getToken(YangParser.SEMICOLON, 0);
        }

        public List<Status_stmtContext> status_stmt() {
            return getRuleContexts(Status_stmtContext.class);
        }

        public StringContext string() {
            return getRuleContext(StringContext.class, 0);
        }

        public List<Identifier_stmtContext> identifier_stmt() {
            return getRuleContexts(Identifier_stmtContext.class);
        }

        public List<Description_stmtContext> description_stmt() {
            return getRuleContexts(Description_stmtContext.class);
        }

        public Reference_stmtContext reference_stmt(int i) {
            return getRuleContext(Reference_stmtContext.class, i);
        }

        public Enum_stmtContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_enum_stmt;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).enterEnum_stmt(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).exitEnum_stmt(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof YangParserVisitor)
                return ((YangParserVisitor<? extends T>) visitor)
                        .visitEnum_stmt(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final Enum_stmtContext enum_stmt() throws RecognitionException {
        Enum_stmtContext _localctx = new Enum_stmtContext(_ctx, getState());
        enterRule(_localctx, 126, RULE_enum_stmt);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(848);
                match(ENUM_KEYWORD);
                setState(849);
                string();
                setState(863);
                switch (_input.LA(1)) {
                case SEMICOLON: {
                    setState(850);
                    match(SEMICOLON);
                }
                    break;
                case LEFT_BRACE: {
                    {
                        setState(851);
                        match(LEFT_BRACE);
                        setState(859);
                        _errHandler.sync(this);
                        _la = _input.LA(1);
                        while (((((_la - 11)) & ~0x3f) == 0 && ((1L << (_la - 11)) & ((1L << (VALUE_KEYWORD - 11))
                                | (1L << (STATUS_KEYWORD - 11))
                                | (1L << (REFERENCE_KEYWORD - 11))
                                | (1L << (DESCRIPTION_KEYWORD - 11)) | (1L << (IDENTIFIER - 11)))) != 0)) {
                            {
                                setState(857);
                                switch (_input.LA(1)) {
                                case IDENTIFIER: {
                                    setState(852);
                                    identifier_stmt();
                                }
                                    break;
                                case VALUE_KEYWORD: {
                                    setState(853);
                                    value_stmt();
                                }
                                    break;
                                case STATUS_KEYWORD: {
                                    setState(854);
                                    status_stmt();
                                }
                                    break;
                                case DESCRIPTION_KEYWORD: {
                                    setState(855);
                                    description_stmt();
                                }
                                    break;
                                case REFERENCE_KEYWORD: {
                                    setState(856);
                                    reference_stmt();
                                }
                                    break;
                                default:
                                    throw new NoViableAltException(this);
                                }
                            }
                            setState(861);
                            _errHandler.sync(this);
                            _la = _input.LA(1);
                        }
                        setState(862);
                        match(RIGHT_BRACE);
                    }
                }
                    break;
                default:
                    throw new NoViableAltException(this);
                }
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    public static class Enum_specificationContext extends ParserRuleContext {
        public List<Enum_stmtContext> enum_stmt() {
            return getRuleContexts(Enum_stmtContext.class);
        }

        public Enum_stmtContext enum_stmt(int i) {
            return getRuleContext(Enum_stmtContext.class, i);
        }

        public Enum_specificationContext(ParserRuleContext parent,
                int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_enum_specification;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).enterEnum_specification(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).exitEnum_specification(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof YangParserVisitor)
                return ((YangParserVisitor<? extends T>) visitor)
                        .visitEnum_specification(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final Enum_specificationContext enum_specification()
            throws RecognitionException {
        Enum_specificationContext _localctx = new Enum_specificationContext(
                _ctx, getState());
        enterRule(_localctx, 128, RULE_enum_specification);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(866);
                _errHandler.sync(this);
                _la = _input.LA(1);
                do {
                    {
                        {
                            setState(865);
                            enum_stmt();
                        }
                    }
                    setState(868);
                    _errHandler.sync(this);
                    _la = _input.LA(1);
                } while (_la == ENUM_KEYWORD);
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    public static class Default_stmtContext extends ParserRuleContext {
        public StringContext string() {
            return getRuleContext(StringContext.class, 0);
        }

        public StmtendContext stmtend() {
            return getRuleContext(StmtendContext.class, 0);
        }

        public TerminalNode DEFAULT_KEYWORD() {
            return getToken(YangParser.DEFAULT_KEYWORD, 0);
        }

        public Default_stmtContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_default_stmt;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).enterDefault_stmt(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).exitDefault_stmt(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof YangParserVisitor)
                return ((YangParserVisitor<? extends T>) visitor)
                        .visitDefault_stmt(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final Default_stmtContext default_stmt() throws RecognitionException {
        Default_stmtContext _localctx = new Default_stmtContext(_ctx,
                getState());
        enterRule(_localctx, 130, RULE_default_stmt);
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(870);
                match(DEFAULT_KEYWORD);
                setState(871);
                string();
                setState(872);
                stmtend();
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    public static class Pattern_stmtContext extends ParserRuleContext {
        public TerminalNode RIGHT_BRACE() {
            return getToken(YangParser.RIGHT_BRACE, 0);
        }

        public List<Reference_stmtContext> reference_stmt() {
            return getRuleContexts(Reference_stmtContext.class);
        }

        public Description_stmtContext description_stmt(int i) {
            return getRuleContext(Description_stmtContext.class, i);
        }

        public TerminalNode LEFT_BRACE() {
            return getToken(YangParser.LEFT_BRACE, 0);
        }

        public List<Error_app_tag_stmtContext> error_app_tag_stmt() {
            return getRuleContexts(Error_app_tag_stmtContext.class);
        }

        public Error_message_stmtContext error_message_stmt(int i) {
            return getRuleContext(Error_message_stmtContext.class, i);
        }

        public Identifier_stmtContext identifier_stmt(int i) {
            return getRuleContext(Identifier_stmtContext.class, i);
        }

        public TerminalNode PATTERN_KEYWORD() {
            return getToken(YangParser.PATTERN_KEYWORD, 0);
        }

        public TerminalNode SEMICOLON() {
            return getToken(YangParser.SEMICOLON, 0);
        }

        public StringContext string() {
            return getRuleContext(StringContext.class, 0);
        }

        public List<Identifier_stmtContext> identifier_stmt() {
            return getRuleContexts(Identifier_stmtContext.class);
        }

        public Error_app_tag_stmtContext error_app_tag_stmt(int i) {
            return getRuleContext(Error_app_tag_stmtContext.class, i);
        }

        public List<Error_message_stmtContext> error_message_stmt() {
            return getRuleContexts(Error_message_stmtContext.class);
        }

        public List<Description_stmtContext> description_stmt() {
            return getRuleContexts(Description_stmtContext.class);
        }

        public Reference_stmtContext reference_stmt(int i) {
            return getRuleContext(Reference_stmtContext.class, i);
        }

        public Pattern_stmtContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_pattern_stmt;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).enterPattern_stmt(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).exitPattern_stmt(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof YangParserVisitor)
                return ((YangParserVisitor<? extends T>) visitor)
                        .visitPattern_stmt(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final Pattern_stmtContext pattern_stmt() throws RecognitionException {
        Pattern_stmtContext _localctx = new Pattern_stmtContext(_ctx,
                getState());
        enterRule(_localctx, 132, RULE_pattern_stmt);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(874);
                match(PATTERN_KEYWORD);
                setState(875);
                string();
                setState(889);
                switch (_input.LA(1)) {
                case SEMICOLON: {
                    setState(876);
                    match(SEMICOLON);
                }
                    break;
                case LEFT_BRACE: {
                    {
                        setState(877);
                        match(LEFT_BRACE);
                        setState(885);
                        _errHandler.sync(this);
                        _la = _input.LA(1);
                        while (((((_la - 24)) & ~0x3f) == 0 && ((1L << (_la - 24)) & ((1L << (REFERENCE_KEYWORD - 24))
                                | (1L << (ERROR_MESSAGE_KEYWORD - 24))
                                | (1L << (ERROR_APP_TAG_KEYWORD - 24))
                                | (1L << (DESCRIPTION_KEYWORD - 24)) | (1L << (IDENTIFIER - 24)))) != 0)) {
                            {
                                setState(883);
                                switch (_input.LA(1)) {
                                case IDENTIFIER: {
                                    setState(878);
                                    identifier_stmt();
                                }
                                    break;
                                case ERROR_MESSAGE_KEYWORD: {
                                    setState(879);
                                    error_message_stmt();
                                }
                                    break;
                                case ERROR_APP_TAG_KEYWORD: {
                                    setState(880);
                                    error_app_tag_stmt();
                                }
                                    break;
                                case DESCRIPTION_KEYWORD: {
                                    setState(881);
                                    description_stmt();
                                }
                                    break;
                                case REFERENCE_KEYWORD: {
                                    setState(882);
                                    reference_stmt();
                                }
                                    break;
                                default:
                                    throw new NoViableAltException(this);
                                }
                            }
                            setState(887);
                            _errHandler.sync(this);
                            _la = _input.LA(1);
                        }
                        setState(888);
                        match(RIGHT_BRACE);
                    }
                }
                    break;
                default:
                    throw new NoViableAltException(this);
                }
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    public static class Length_stmtContext extends ParserRuleContext {
        public TerminalNode RIGHT_BRACE() {
            return getToken(YangParser.RIGHT_BRACE, 0);
        }

        public List<Reference_stmtContext> reference_stmt() {
            return getRuleContexts(Reference_stmtContext.class);
        }

        public Description_stmtContext description_stmt(int i) {
            return getRuleContext(Description_stmtContext.class, i);
        }

        public TerminalNode LEFT_BRACE() {
            return getToken(YangParser.LEFT_BRACE, 0);
        }

        public List<Error_app_tag_stmtContext> error_app_tag_stmt() {
            return getRuleContexts(Error_app_tag_stmtContext.class);
        }

        public Error_message_stmtContext error_message_stmt(int i) {
            return getRuleContext(Error_message_stmtContext.class, i);
        }

        public Identifier_stmtContext identifier_stmt(int i) {
            return getRuleContext(Identifier_stmtContext.class, i);
        }

        public TerminalNode SEMICOLON() {
            return getToken(YangParser.SEMICOLON, 0);
        }

        public StringContext string() {
            return getRuleContext(StringContext.class, 0);
        }

        public List<Identifier_stmtContext> identifier_stmt() {
            return getRuleContexts(Identifier_stmtContext.class);
        }

        public TerminalNode LENGTH_KEYWORD() {
            return getToken(YangParser.LENGTH_KEYWORD, 0);
        }

        public Error_app_tag_stmtContext error_app_tag_stmt(int i) {
            return getRuleContext(Error_app_tag_stmtContext.class, i);
        }

        public List<Error_message_stmtContext> error_message_stmt() {
            return getRuleContexts(Error_message_stmtContext.class);
        }

        public List<Description_stmtContext> description_stmt() {
            return getRuleContexts(Description_stmtContext.class);
        }

        public Reference_stmtContext reference_stmt(int i) {
            return getRuleContext(Reference_stmtContext.class, i);
        }

        public Length_stmtContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_length_stmt;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).enterLength_stmt(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).exitLength_stmt(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof YangParserVisitor)
                return ((YangParserVisitor<? extends T>) visitor)
                        .visitLength_stmt(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final Length_stmtContext length_stmt() throws RecognitionException {
        Length_stmtContext _localctx = new Length_stmtContext(_ctx, getState());
        enterRule(_localctx, 134, RULE_length_stmt);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(891);
                match(LENGTH_KEYWORD);
                setState(892);
                string();
                setState(906);
                switch (_input.LA(1)) {
                case SEMICOLON: {
                    setState(893);
                    match(SEMICOLON);
                }
                    break;
                case LEFT_BRACE: {
                    {
                        setState(894);
                        match(LEFT_BRACE);
                        setState(902);
                        _errHandler.sync(this);
                        _la = _input.LA(1);
                        while (((((_la - 24)) & ~0x3f) == 0 && ((1L << (_la - 24)) & ((1L << (REFERENCE_KEYWORD - 24))
                                | (1L << (ERROR_MESSAGE_KEYWORD - 24))
                                | (1L << (ERROR_APP_TAG_KEYWORD - 24))
                                | (1L << (DESCRIPTION_KEYWORD - 24)) | (1L << (IDENTIFIER - 24)))) != 0)) {
                            {
                                setState(900);
                                switch (_input.LA(1)) {
                                case IDENTIFIER: {
                                    setState(895);
                                    identifier_stmt();
                                }
                                    break;
                                case ERROR_MESSAGE_KEYWORD: {
                                    setState(896);
                                    error_message_stmt();
                                }
                                    break;
                                case ERROR_APP_TAG_KEYWORD: {
                                    setState(897);
                                    error_app_tag_stmt();
                                }
                                    break;
                                case DESCRIPTION_KEYWORD: {
                                    setState(898);
                                    description_stmt();
                                }
                                    break;
                                case REFERENCE_KEYWORD: {
                                    setState(899);
                                    reference_stmt();
                                }
                                    break;
                                default:
                                    throw new NoViableAltException(this);
                                }
                            }
                            setState(904);
                            _errHandler.sync(this);
                            _la = _input.LA(1);
                        }
                        setState(905);
                        match(RIGHT_BRACE);
                    }
                }
                    break;
                default:
                    throw new NoViableAltException(this);
                }
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    public static class String_restrictionsContext extends ParserRuleContext {
        public Length_stmtContext length_stmt(int i) {
            return getRuleContext(Length_stmtContext.class, i);
        }

        public List<Pattern_stmtContext> pattern_stmt() {
            return getRuleContexts(Pattern_stmtContext.class);
        }

        public List<Length_stmtContext> length_stmt() {
            return getRuleContexts(Length_stmtContext.class);
        }

        public Pattern_stmtContext pattern_stmt(int i) {
            return getRuleContext(Pattern_stmtContext.class, i);
        }

        public String_restrictionsContext(ParserRuleContext parent,
                int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_string_restrictions;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).enterString_restrictions(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).exitString_restrictions(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof YangParserVisitor)
                return ((YangParserVisitor<? extends T>) visitor)
                        .visitString_restrictions(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final String_restrictionsContext string_restrictions()
            throws RecognitionException {
        String_restrictionsContext _localctx = new String_restrictionsContext(
                _ctx, getState());
        enterRule(_localctx, 136, RULE_string_restrictions);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(912);
                _errHandler.sync(this);
                _la = _input.LA(1);
                while (_la == PATTERN_KEYWORD || _la == LENGTH_KEYWORD) {
                    {
                        setState(910);
                        switch (_input.LA(1)) {
                        case LENGTH_KEYWORD: {
                            setState(908);
                            length_stmt();
                        }
                            break;
                        case PATTERN_KEYWORD: {
                            setState(909);
                            pattern_stmt();
                        }
                            break;
                        default:
                            throw new NoViableAltException(this);
                        }
                    }
                    setState(914);
                    _errHandler.sync(this);
                    _la = _input.LA(1);
                }
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    public static class Fraction_digits_stmtContext extends ParserRuleContext {
        public StringContext string() {
            return getRuleContext(StringContext.class, 0);
        }

        public TerminalNode FRACTION_DIGITS_KEYWORD() {
            return getToken(YangParser.FRACTION_DIGITS_KEYWORD, 0);
        }

        public StmtendContext stmtend() {
            return getRuleContext(StmtendContext.class, 0);
        }

        public Fraction_digits_stmtContext(ParserRuleContext parent,
                int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_fraction_digits_stmt;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).enterFraction_digits_stmt(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).exitFraction_digits_stmt(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof YangParserVisitor)
                return ((YangParserVisitor<? extends T>) visitor)
                        .visitFraction_digits_stmt(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final Fraction_digits_stmtContext fraction_digits_stmt()
            throws RecognitionException {
        Fraction_digits_stmtContext _localctx = new Fraction_digits_stmtContext(
                _ctx, getState());
        enterRule(_localctx, 138, RULE_fraction_digits_stmt);
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(915);
                match(FRACTION_DIGITS_KEYWORD);
                setState(916);
                string();
                setState(917);
                stmtend();
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    public static class Decimal64_specificationContext extends
            ParserRuleContext {
        public Fraction_digits_stmtContext fraction_digits_stmt() {
            return getRuleContext(Fraction_digits_stmtContext.class, 0);
        }

        public Numerical_restrictionsContext numerical_restrictions() {
            return getRuleContext(Numerical_restrictionsContext.class, 0);
        }

        public Decimal64_specificationContext(ParserRuleContext parent,
                int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_decimal64_specification;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener)
                        .enterDecimal64_specification(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener)
                        .exitDecimal64_specification(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof YangParserVisitor)
                return ((YangParserVisitor<? extends T>) visitor)
                        .visitDecimal64_specification(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final Decimal64_specificationContext decimal64_specification()
            throws RecognitionException {
        Decimal64_specificationContext _localctx = new Decimal64_specificationContext(
                _ctx, getState());
        enterRule(_localctx, 140, RULE_decimal64_specification);
        int _la;
        try {
            setState(927);
            switch (getInterpreter().adaptivePredict(_input, 101, _ctx)) {
            case 1:
                enterOuterAlt(_localctx, 1);
                {
                    setState(920);
                    _la = _input.LA(1);
                    if (_la == RANGE_KEYWORD) {
                        {
                            setState(919);
                            numerical_restrictions();
                        }
                    }

                    setState(922);
                    fraction_digits_stmt();
                }
                break;

            case 2:
                enterOuterAlt(_localctx, 2);
                {
                    setState(923);
                    fraction_digits_stmt();
                    setState(925);
                    _la = _input.LA(1);
                    if (_la == RANGE_KEYWORD) {
                        {
                            setState(924);
                            numerical_restrictions();
                        }
                    }

                }
                break;
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    public static class Range_stmtContext extends ParserRuleContext {
        public TerminalNode RIGHT_BRACE() {
            return getToken(YangParser.RIGHT_BRACE, 0);
        }

        public List<Reference_stmtContext> reference_stmt() {
            return getRuleContexts(Reference_stmtContext.class);
        }

        public Description_stmtContext description_stmt(int i) {
            return getRuleContext(Description_stmtContext.class, i);
        }

        public TerminalNode LEFT_BRACE() {
            return getToken(YangParser.LEFT_BRACE, 0);
        }

        public List<Error_app_tag_stmtContext> error_app_tag_stmt() {
            return getRuleContexts(Error_app_tag_stmtContext.class);
        }

        public Error_message_stmtContext error_message_stmt(int i) {
            return getRuleContext(Error_message_stmtContext.class, i);
        }

        public Identifier_stmtContext identifier_stmt(int i) {
            return getRuleContext(Identifier_stmtContext.class, i);
        }

        public TerminalNode SEMICOLON() {
            return getToken(YangParser.SEMICOLON, 0);
        }

        public TerminalNode RANGE_KEYWORD() {
            return getToken(YangParser.RANGE_KEYWORD, 0);
        }

        public StringContext string() {
            return getRuleContext(StringContext.class, 0);
        }

        public List<Identifier_stmtContext> identifier_stmt() {
            return getRuleContexts(Identifier_stmtContext.class);
        }

        public Error_app_tag_stmtContext error_app_tag_stmt(int i) {
            return getRuleContext(Error_app_tag_stmtContext.class, i);
        }

        public List<Error_message_stmtContext> error_message_stmt() {
            return getRuleContexts(Error_message_stmtContext.class);
        }

        public List<Description_stmtContext> description_stmt() {
            return getRuleContexts(Description_stmtContext.class);
        }

        public Reference_stmtContext reference_stmt(int i) {
            return getRuleContext(Reference_stmtContext.class, i);
        }

        public Range_stmtContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_range_stmt;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).enterRange_stmt(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).exitRange_stmt(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof YangParserVisitor)
                return ((YangParserVisitor<? extends T>) visitor)
                        .visitRange_stmt(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final Range_stmtContext range_stmt() throws RecognitionException {
        Range_stmtContext _localctx = new Range_stmtContext(_ctx, getState());
        enterRule(_localctx, 142, RULE_range_stmt);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(929);
                match(RANGE_KEYWORD);
                setState(930);
                string();
                setState(944);
                switch (_input.LA(1)) {
                case SEMICOLON: {
                    setState(931);
                    match(SEMICOLON);
                }
                    break;
                case LEFT_BRACE: {
                    {
                        setState(932);
                        match(LEFT_BRACE);
                        setState(940);
                        _errHandler.sync(this);
                        _la = _input.LA(1);
                        while (((((_la - 24)) & ~0x3f) == 0 && ((1L << (_la - 24)) & ((1L << (REFERENCE_KEYWORD - 24))
                                | (1L << (ERROR_MESSAGE_KEYWORD - 24))
                                | (1L << (ERROR_APP_TAG_KEYWORD - 24))
                                | (1L << (DESCRIPTION_KEYWORD - 24)) | (1L << (IDENTIFIER - 24)))) != 0)) {
                            {
                                setState(938);
                                switch (_input.LA(1)) {
                                case IDENTIFIER: {
                                    setState(933);
                                    identifier_stmt();
                                }
                                    break;
                                case ERROR_MESSAGE_KEYWORD: {
                                    setState(934);
                                    error_message_stmt();
                                }
                                    break;
                                case ERROR_APP_TAG_KEYWORD: {
                                    setState(935);
                                    error_app_tag_stmt();
                                }
                                    break;
                                case DESCRIPTION_KEYWORD: {
                                    setState(936);
                                    description_stmt();
                                }
                                    break;
                                case REFERENCE_KEYWORD: {
                                    setState(937);
                                    reference_stmt();
                                }
                                    break;
                                default:
                                    throw new NoViableAltException(this);
                                }
                            }
                            setState(942);
                            _errHandler.sync(this);
                            _la = _input.LA(1);
                        }
                        setState(943);
                        match(RIGHT_BRACE);
                    }
                }
                    break;
                default:
                    throw new NoViableAltException(this);
                }
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    public static class Numerical_restrictionsContext extends ParserRuleContext {
        public Range_stmtContext range_stmt() {
            return getRuleContext(Range_stmtContext.class, 0);
        }

        public Numerical_restrictionsContext(ParserRuleContext parent,
                int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_numerical_restrictions;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener)
                        .enterNumerical_restrictions(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener)
                        .exitNumerical_restrictions(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof YangParserVisitor)
                return ((YangParserVisitor<? extends T>) visitor)
                        .visitNumerical_restrictions(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final Numerical_restrictionsContext numerical_restrictions()
            throws RecognitionException {
        Numerical_restrictionsContext _localctx = new Numerical_restrictionsContext(
                _ctx, getState());
        enterRule(_localctx, 144, RULE_numerical_restrictions);
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(946);
                range_stmt();
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    public static class Type_body_stmtsContext extends ParserRuleContext {
        public Bits_specificationContext bits_specification() {
            return getRuleContext(Bits_specificationContext.class, 0);
        }

        public Identityref_specificationContext identityref_specification() {
            return getRuleContext(Identityref_specificationContext.class, 0);
        }

        public Enum_specificationContext enum_specification() {
            return getRuleContext(Enum_specificationContext.class, 0);
        }

        public Numerical_restrictionsContext numerical_restrictions() {
            return getRuleContext(Numerical_restrictionsContext.class, 0);
        }

        public String_restrictionsContext string_restrictions() {
            return getRuleContext(String_restrictionsContext.class, 0);
        }

        public Leafref_specificationContext leafref_specification() {
            return getRuleContext(Leafref_specificationContext.class, 0);
        }

        public Decimal64_specificationContext decimal64_specification() {
            return getRuleContext(Decimal64_specificationContext.class, 0);
        }

        public Union_specificationContext union_specification() {
            return getRuleContext(Union_specificationContext.class, 0);
        }

        public Instance_identifier_specificationContext instance_identifier_specification() {
            return getRuleContext(
                    Instance_identifier_specificationContext.class, 0);
        }

        public Type_body_stmtsContext(ParserRuleContext parent,
                int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_type_body_stmts;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).enterType_body_stmts(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).exitType_body_stmts(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof YangParserVisitor)
                return ((YangParserVisitor<? extends T>) visitor)
                        .visitType_body_stmts(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final Type_body_stmtsContext type_body_stmts()
            throws RecognitionException {
        Type_body_stmtsContext _localctx = new Type_body_stmtsContext(_ctx,
                getState());
        enterRule(_localctx, 146, RULE_type_body_stmts);
        try {
            setState(957);
            switch (getInterpreter().adaptivePredict(_input, 105, _ctx)) {
            case 1:
                enterOuterAlt(_localctx, 1);
                {
                    setState(948);
                    numerical_restrictions();
                }
                break;

            case 2:
                enterOuterAlt(_localctx, 2);
                {
                    setState(949);
                    decimal64_specification();
                }
                break;

            case 3:
                enterOuterAlt(_localctx, 3);
                {
                    setState(950);
                    string_restrictions();
                }
                break;

            case 4:
                enterOuterAlt(_localctx, 4);
                {
                    setState(951);
                    enum_specification();
                }
                break;

            case 5:
                enterOuterAlt(_localctx, 5);
                {
                    setState(952);
                    leafref_specification();
                }
                break;

            case 6:
                enterOuterAlt(_localctx, 6);
                {
                    setState(953);
                    identityref_specification();
                }
                break;

            case 7:
                enterOuterAlt(_localctx, 7);
                {
                    setState(954);
                    instance_identifier_specification();
                }
                break;

            case 8:
                enterOuterAlt(_localctx, 8);
                {
                    setState(955);
                    bits_specification();
                }
                break;

            case 9:
                enterOuterAlt(_localctx, 9);
                {
                    setState(956);
                    union_specification();
                }
                break;
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    public static class Type_stmtContext extends ParserRuleContext {
        public TerminalNode RIGHT_BRACE() {
            return getToken(YangParser.RIGHT_BRACE, 0);
        }

        public TerminalNode TYPE_KEYWORD() {
            return getToken(YangParser.TYPE_KEYWORD, 0);
        }

        public TerminalNode SEMICOLON() {
            return getToken(YangParser.SEMICOLON, 0);
        }

        public StringContext string() {
            return getRuleContext(StringContext.class, 0);
        }

        public TerminalNode LEFT_BRACE() {
            return getToken(YangParser.LEFT_BRACE, 0);
        }

        public Type_body_stmtsContext type_body_stmts() {
            return getRuleContext(Type_body_stmtsContext.class, 0);
        }

        public Type_stmtContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_type_stmt;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).enterType_stmt(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).exitType_stmt(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof YangParserVisitor)
                return ((YangParserVisitor<? extends T>) visitor)
                        .visitType_stmt(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final Type_stmtContext type_stmt() throws RecognitionException {
        Type_stmtContext _localctx = new Type_stmtContext(_ctx, getState());
        enterRule(_localctx, 148, RULE_type_stmt);
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(959);
                match(TYPE_KEYWORD);
                setState(960);
                string();
                setState(966);
                switch (_input.LA(1)) {
                case SEMICOLON: {
                    setState(961);
                    match(SEMICOLON);
                }
                    break;
                case LEFT_BRACE: {
                    {
                        setState(962);
                        match(LEFT_BRACE);
                        setState(963);
                        type_body_stmts();
                        setState(964);
                        match(RIGHT_BRACE);
                    }
                }
                    break;
                default:
                    throw new NoViableAltException(this);
                }
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    public static class Typedef_stmtContext extends ParserRuleContext {
        public TerminalNode RIGHT_BRACE() {
            return getToken(YangParser.RIGHT_BRACE, 0);
        }

        public Units_stmtContext units_stmt(int i) {
            return getRuleContext(Units_stmtContext.class, i);
        }

        public List<Reference_stmtContext> reference_stmt() {
            return getRuleContexts(Reference_stmtContext.class);
        }

        public Description_stmtContext description_stmt(int i) {
            return getRuleContext(Description_stmtContext.class, i);
        }

        public Default_stmtContext default_stmt(int i) {
            return getRuleContext(Default_stmtContext.class, i);
        }

        public List<Units_stmtContext> units_stmt() {
            return getRuleContexts(Units_stmtContext.class);
        }

        public Type_stmtContext type_stmt(int i) {
            return getRuleContext(Type_stmtContext.class, i);
        }

        public TerminalNode LEFT_BRACE() {
            return getToken(YangParser.LEFT_BRACE, 0);
        }

        public Status_stmtContext status_stmt(int i) {
            return getRuleContext(Status_stmtContext.class, i);
        }

        public List<Type_stmtContext> type_stmt() {
            return getRuleContexts(Type_stmtContext.class);
        }

        public TerminalNode TYPEDEF_KEYWORD() {
            return getToken(YangParser.TYPEDEF_KEYWORD, 0);
        }

        public List<Default_stmtContext> default_stmt() {
            return getRuleContexts(Default_stmtContext.class);
        }

        public List<Status_stmtContext> status_stmt() {
            return getRuleContexts(Status_stmtContext.class);
        }

        public StringContext string() {
            return getRuleContext(StringContext.class, 0);
        }

        public List<Description_stmtContext> description_stmt() {
            return getRuleContexts(Description_stmtContext.class);
        }

        public Reference_stmtContext reference_stmt(int i) {
            return getRuleContext(Reference_stmtContext.class, i);
        }

        public Typedef_stmtContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_typedef_stmt;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).enterTypedef_stmt(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).exitTypedef_stmt(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof YangParserVisitor)
                return ((YangParserVisitor<? extends T>) visitor)
                        .visitTypedef_stmt(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final Typedef_stmtContext typedef_stmt() throws RecognitionException {
        Typedef_stmtContext _localctx = new Typedef_stmtContext(_ctx,
                getState());
        enterRule(_localctx, 150, RULE_typedef_stmt);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(968);
                match(TYPEDEF_KEYWORD);
                setState(969);
                string();
                setState(970);
                match(LEFT_BRACE);
                setState(977);
                _errHandler.sync(this);
                _la = _input.LA(1);
                do {
                    {
                        setState(977);
                        switch (_input.LA(1)) {
                        case TYPE_KEYWORD: {
                            setState(971);
                            type_stmt();
                        }
                            break;
                        case UNITS_KEYWORD: {
                            setState(972);
                            units_stmt();
                        }
                            break;
                        case DEFAULT_KEYWORD: {
                            setState(973);
                            default_stmt();
                        }
                            break;
                        case STATUS_KEYWORD: {
                            setState(974);
                            status_stmt();
                        }
                            break;
                        case DESCRIPTION_KEYWORD: {
                            setState(975);
                            description_stmt();
                        }
                            break;
                        case REFERENCE_KEYWORD: {
                            setState(976);
                            reference_stmt();
                        }
                            break;
                        default:
                            throw new NoViableAltException(this);
                        }
                    }
                    setState(979);
                    _errHandler.sync(this);
                    _la = _input.LA(1);
                } while ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << UNITS_KEYWORD)
                        | (1L << TYPE_KEYWORD)
                        | (1L << STATUS_KEYWORD)
                        | (1L << REFERENCE_KEYWORD)
                        | (1L << DESCRIPTION_KEYWORD) | (1L << DEFAULT_KEYWORD))) != 0));
                setState(981);
                match(RIGHT_BRACE);
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    public static class If_feature_stmtContext extends ParserRuleContext {
        public TerminalNode IF_FEATURE_KEYWORD() {
            return getToken(YangParser.IF_FEATURE_KEYWORD, 0);
        }

        public StringContext string() {
            return getRuleContext(StringContext.class, 0);
        }

        public StmtendContext stmtend() {
            return getRuleContext(StmtendContext.class, 0);
        }

        public If_feature_stmtContext(ParserRuleContext parent,
                int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_if_feature_stmt;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).enterIf_feature_stmt(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).exitIf_feature_stmt(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof YangParserVisitor)
                return ((YangParserVisitor<? extends T>) visitor)
                        .visitIf_feature_stmt(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final If_feature_stmtContext if_feature_stmt()
            throws RecognitionException {
        If_feature_stmtContext _localctx = new If_feature_stmtContext(_ctx,
                getState());
        enterRule(_localctx, 152, RULE_if_feature_stmt);
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(983);
                match(IF_FEATURE_KEYWORD);
                setState(984);
                string();
                setState(985);
                stmtend();
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    public static class Feature_stmtContext extends ParserRuleContext {
        public TerminalNode RIGHT_BRACE() {
            return getToken(YangParser.RIGHT_BRACE, 0);
        }

        public List<Reference_stmtContext> reference_stmt() {
            return getRuleContexts(Reference_stmtContext.class);
        }

        public Description_stmtContext description_stmt(int i) {
            return getRuleContext(Description_stmtContext.class, i);
        }

        public If_feature_stmtContext if_feature_stmt(int i) {
            return getRuleContext(If_feature_stmtContext.class, i);
        }

        public TerminalNode LEFT_BRACE() {
            return getToken(YangParser.LEFT_BRACE, 0);
        }

        public Status_stmtContext status_stmt(int i) {
            return getRuleContext(Status_stmtContext.class, i);
        }

        public List<If_feature_stmtContext> if_feature_stmt() {
            return getRuleContexts(If_feature_stmtContext.class);
        }

        public TerminalNode SEMICOLON() {
            return getToken(YangParser.SEMICOLON, 0);
        }

        public List<Status_stmtContext> status_stmt() {
            return getRuleContexts(Status_stmtContext.class);
        }

        public StringContext string() {
            return getRuleContext(StringContext.class, 0);
        }

        public TerminalNode FEATURE_KEYWORD() {
            return getToken(YangParser.FEATURE_KEYWORD, 0);
        }

        public List<Description_stmtContext> description_stmt() {
            return getRuleContexts(Description_stmtContext.class);
        }

        public Reference_stmtContext reference_stmt(int i) {
            return getRuleContext(Reference_stmtContext.class, i);
        }

        public Feature_stmtContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_feature_stmt;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).enterFeature_stmt(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).exitFeature_stmt(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof YangParserVisitor)
                return ((YangParserVisitor<? extends T>) visitor)
                        .visitFeature_stmt(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final Feature_stmtContext feature_stmt() throws RecognitionException {
        Feature_stmtContext _localctx = new Feature_stmtContext(_ctx,
                getState());
        enterRule(_localctx, 154, RULE_feature_stmt);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(987);
                match(FEATURE_KEYWORD);
                setState(988);
                string();
                setState(1001);
                switch (_input.LA(1)) {
                case SEMICOLON: {
                    setState(989);
                    match(SEMICOLON);
                }
                    break;
                case LEFT_BRACE: {
                    {
                        setState(990);
                        match(LEFT_BRACE);
                        setState(997);
                        _errHandler.sync(this);
                        _la = _input.LA(1);
                        while ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << STATUS_KEYWORD)
                                | (1L << REFERENCE_KEYWORD)
                                | (1L << IF_FEATURE_KEYWORD) | (1L << DESCRIPTION_KEYWORD))) != 0)) {
                            {
                                setState(995);
                                switch (_input.LA(1)) {
                                case IF_FEATURE_KEYWORD: {
                                    setState(991);
                                    if_feature_stmt();
                                }
                                    break;
                                case STATUS_KEYWORD: {
                                    setState(992);
                                    status_stmt();
                                }
                                    break;
                                case DESCRIPTION_KEYWORD: {
                                    setState(993);
                                    description_stmt();
                                }
                                    break;
                                case REFERENCE_KEYWORD: {
                                    setState(994);
                                    reference_stmt();
                                }
                                    break;
                                default:
                                    throw new NoViableAltException(this);
                                }
                            }
                            setState(999);
                            _errHandler.sync(this);
                            _la = _input.LA(1);
                        }
                        setState(1000);
                        match(RIGHT_BRACE);
                    }
                }
                    break;
                default:
                    throw new NoViableAltException(this);
                }
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    public static class Base_stmtContext extends ParserRuleContext {
        public TerminalNode BASE_KEYWORD() {
            return getToken(YangParser.BASE_KEYWORD, 0);
        }

        public StringContext string() {
            return getRuleContext(StringContext.class, 0);
        }

        public StmtendContext stmtend() {
            return getRuleContext(StmtendContext.class, 0);
        }

        public Base_stmtContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_base_stmt;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).enterBase_stmt(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).exitBase_stmt(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof YangParserVisitor)
                return ((YangParserVisitor<? extends T>) visitor)
                        .visitBase_stmt(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final Base_stmtContext base_stmt() throws RecognitionException {
        Base_stmtContext _localctx = new Base_stmtContext(_ctx, getState());
        enterRule(_localctx, 156, RULE_base_stmt);
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(1003);
                match(BASE_KEYWORD);
                setState(1004);
                string();
                setState(1005);
                stmtend();
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    public static class Identity_stmtContext extends ParserRuleContext {
        public TerminalNode RIGHT_BRACE() {
            return getToken(YangParser.RIGHT_BRACE, 0);
        }

        public List<Reference_stmtContext> reference_stmt() {
            return getRuleContexts(Reference_stmtContext.class);
        }

        public Description_stmtContext description_stmt(int i) {
            return getRuleContext(Description_stmtContext.class, i);
        }

        public Base_stmtContext base_stmt(int i) {
            return getRuleContext(Base_stmtContext.class, i);
        }

        public TerminalNode IDENTITY_KEYWORD() {
            return getToken(YangParser.IDENTITY_KEYWORD, 0);
        }

        public TerminalNode LEFT_BRACE() {
            return getToken(YangParser.LEFT_BRACE, 0);
        }

        public Status_stmtContext status_stmt(int i) {
            return getRuleContext(Status_stmtContext.class, i);
        }

        public TerminalNode SEMICOLON() {
            return getToken(YangParser.SEMICOLON, 0);
        }

        public List<Status_stmtContext> status_stmt() {
            return getRuleContexts(Status_stmtContext.class);
        }

        public StringContext string() {
            return getRuleContext(StringContext.class, 0);
        }

        public List<Base_stmtContext> base_stmt() {
            return getRuleContexts(Base_stmtContext.class);
        }

        public List<Description_stmtContext> description_stmt() {
            return getRuleContexts(Description_stmtContext.class);
        }

        public Reference_stmtContext reference_stmt(int i) {
            return getRuleContext(Reference_stmtContext.class, i);
        }

        public Identity_stmtContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_identity_stmt;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).enterIdentity_stmt(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).exitIdentity_stmt(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof YangParserVisitor)
                return ((YangParserVisitor<? extends T>) visitor)
                        .visitIdentity_stmt(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final Identity_stmtContext identity_stmt()
            throws RecognitionException {
        Identity_stmtContext _localctx = new Identity_stmtContext(_ctx,
                getState());
        enterRule(_localctx, 158, RULE_identity_stmt);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(1007);
                match(IDENTITY_KEYWORD);
                setState(1008);
                string();
                setState(1021);
                switch (_input.LA(1)) {
                case SEMICOLON: {
                    setState(1009);
                    match(SEMICOLON);
                }
                    break;
                case LEFT_BRACE: {
                    {
                        setState(1010);
                        match(LEFT_BRACE);
                        setState(1017);
                        _errHandler.sync(this);
                        _la = _input.LA(1);
                        while (((((_la - 18)) & ~0x3f) == 0 && ((1L << (_la - 18)) & ((1L << (STATUS_KEYWORD - 18))
                                | (1L << (REFERENCE_KEYWORD - 18))
                                | (1L << (DESCRIPTION_KEYWORD - 18)) | (1L << (BASE_KEYWORD - 18)))) != 0)) {
                            {
                                setState(1015);
                                switch (_input.LA(1)) {
                                case BASE_KEYWORD: {
                                    setState(1011);
                                    base_stmt();
                                }
                                    break;
                                case STATUS_KEYWORD: {
                                    setState(1012);
                                    status_stmt();
                                }
                                    break;
                                case DESCRIPTION_KEYWORD: {
                                    setState(1013);
                                    description_stmt();
                                }
                                    break;
                                case REFERENCE_KEYWORD: {
                                    setState(1014);
                                    reference_stmt();
                                }
                                    break;
                                default:
                                    throw new NoViableAltException(this);
                                }
                            }
                            setState(1019);
                            _errHandler.sync(this);
                            _la = _input.LA(1);
                        }
                        setState(1020);
                        match(RIGHT_BRACE);
                    }
                }
                    break;
                default:
                    throw new NoViableAltException(this);
                }
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    public static class Yin_element_argContext extends ParserRuleContext {
        public StringContext string() {
            return getRuleContext(StringContext.class, 0);
        }

        public Yin_element_argContext(ParserRuleContext parent,
                int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_yin_element_arg;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).enterYin_element_arg(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).exitYin_element_arg(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof YangParserVisitor)
                return ((YangParserVisitor<? extends T>) visitor)
                        .visitYin_element_arg(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final Yin_element_argContext yin_element_arg()
            throws RecognitionException {
        Yin_element_argContext _localctx = new Yin_element_argContext(_ctx,
                getState());
        enterRule(_localctx, 160, RULE_yin_element_arg);
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(1023);
                string();
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    public static class Yin_element_stmtContext extends ParserRuleContext {
        public TerminalNode YIN_ELEMENT_KEYWORD() {
            return getToken(YangParser.YIN_ELEMENT_KEYWORD, 0);
        }

        public Yin_element_argContext yin_element_arg() {
            return getRuleContext(Yin_element_argContext.class, 0);
        }

        public StmtendContext stmtend() {
            return getRuleContext(StmtendContext.class, 0);
        }

        public Yin_element_stmtContext(ParserRuleContext parent,
                int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_yin_element_stmt;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).enterYin_element_stmt(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).exitYin_element_stmt(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof YangParserVisitor)
                return ((YangParserVisitor<? extends T>) visitor)
                        .visitYin_element_stmt(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final Yin_element_stmtContext yin_element_stmt()
            throws RecognitionException {
        Yin_element_stmtContext _localctx = new Yin_element_stmtContext(_ctx,
                getState());
        enterRule(_localctx, 162, RULE_yin_element_stmt);
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(1025);
                match(YIN_ELEMENT_KEYWORD);
                setState(1026);
                yin_element_arg();
                setState(1027);
                stmtend();
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    public static class Argument_stmtContext extends ParserRuleContext {
        public TerminalNode RIGHT_BRACE() {
            return getToken(YangParser.RIGHT_BRACE, 0);
        }

        public Yin_element_stmtContext yin_element_stmt() {
            return getRuleContext(Yin_element_stmtContext.class, 0);
        }

        public TerminalNode SEMICOLON() {
            return getToken(YangParser.SEMICOLON, 0);
        }

        public StringContext string() {
            return getRuleContext(StringContext.class, 0);
        }

        public TerminalNode LEFT_BRACE() {
            return getToken(YangParser.LEFT_BRACE, 0);
        }

        public TerminalNode ARGUMENT_KEYWORD() {
            return getToken(YangParser.ARGUMENT_KEYWORD, 0);
        }

        public Argument_stmtContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_argument_stmt;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).enterArgument_stmt(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).exitArgument_stmt(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof YangParserVisitor)
                return ((YangParserVisitor<? extends T>) visitor)
                        .visitArgument_stmt(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final Argument_stmtContext argument_stmt()
            throws RecognitionException {
        Argument_stmtContext _localctx = new Argument_stmtContext(_ctx,
                getState());
        enterRule(_localctx, 164, RULE_argument_stmt);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(1029);
                match(ARGUMENT_KEYWORD);
                setState(1030);
                string();
                setState(1037);
                switch (_input.LA(1)) {
                case SEMICOLON: {
                    setState(1031);
                    match(SEMICOLON);
                }
                    break;
                case LEFT_BRACE: {
                    {
                        setState(1032);
                        match(LEFT_BRACE);
                        setState(1034);
                        _la = _input.LA(1);
                        if (_la == YIN_ELEMENT_KEYWORD) {
                            {
                                setState(1033);
                                yin_element_stmt();
                            }
                        }

                        setState(1036);
                        match(RIGHT_BRACE);
                    }
                }
                    break;
                default:
                    throw new NoViableAltException(this);
                }
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    public static class Extension_stmtContext extends ParserRuleContext {
        public TerminalNode RIGHT_BRACE() {
            return getToken(YangParser.RIGHT_BRACE, 0);
        }

        public TerminalNode EXTENSION_KEYWORD() {
            return getToken(YangParser.EXTENSION_KEYWORD, 0);
        }

        public List<Reference_stmtContext> reference_stmt() {
            return getRuleContexts(Reference_stmtContext.class);
        }

        public Description_stmtContext description_stmt(int i) {
            return getRuleContext(Description_stmtContext.class, i);
        }

        public List<Argument_stmtContext> argument_stmt() {
            return getRuleContexts(Argument_stmtContext.class);
        }

        public TerminalNode LEFT_BRACE() {
            return getToken(YangParser.LEFT_BRACE, 0);
        }

        public Status_stmtContext status_stmt(int i) {
            return getRuleContext(Status_stmtContext.class, i);
        }

        public TerminalNode SEMICOLON() {
            return getToken(YangParser.SEMICOLON, 0);
        }

        public List<Status_stmtContext> status_stmt() {
            return getRuleContexts(Status_stmtContext.class);
        }

        public StringContext string() {
            return getRuleContext(StringContext.class, 0);
        }

        public List<Description_stmtContext> description_stmt() {
            return getRuleContexts(Description_stmtContext.class);
        }

        public Reference_stmtContext reference_stmt(int i) {
            return getRuleContext(Reference_stmtContext.class, i);
        }

        public Argument_stmtContext argument_stmt(int i) {
            return getRuleContext(Argument_stmtContext.class, i);
        }

        public Extension_stmtContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_extension_stmt;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).enterExtension_stmt(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).exitExtension_stmt(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof YangParserVisitor)
                return ((YangParserVisitor<? extends T>) visitor)
                        .visitExtension_stmt(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final Extension_stmtContext extension_stmt()
            throws RecognitionException {
        Extension_stmtContext _localctx = new Extension_stmtContext(_ctx,
                getState());
        enterRule(_localctx, 166, RULE_extension_stmt);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(1039);
                match(EXTENSION_KEYWORD);
                setState(1040);
                string();
                setState(1053);
                switch (_input.LA(1)) {
                case SEMICOLON: {
                    setState(1041);
                    match(SEMICOLON);
                }
                    break;
                case LEFT_BRACE: {
                    {
                        setState(1042);
                        match(LEFT_BRACE);
                        setState(1049);
                        _errHandler.sync(this);
                        _la = _input.LA(1);
                        while (((((_la - 18)) & ~0x3f) == 0 && ((1L << (_la - 18)) & ((1L << (STATUS_KEYWORD - 18))
                                | (1L << (REFERENCE_KEYWORD - 18))
                                | (1L << (DESCRIPTION_KEYWORD - 18)) | (1L << (ARGUMENT_KEYWORD - 18)))) != 0)) {
                            {
                                setState(1047);
                                switch (_input.LA(1)) {
                                case ARGUMENT_KEYWORD: {
                                    setState(1043);
                                    argument_stmt();
                                }
                                    break;
                                case STATUS_KEYWORD: {
                                    setState(1044);
                                    status_stmt();
                                }
                                    break;
                                case DESCRIPTION_KEYWORD: {
                                    setState(1045);
                                    description_stmt();
                                }
                                    break;
                                case REFERENCE_KEYWORD: {
                                    setState(1046);
                                    reference_stmt();
                                }
                                    break;
                                default:
                                    throw new NoViableAltException(this);
                                }
                            }
                            setState(1051);
                            _errHandler.sync(this);
                            _la = _input.LA(1);
                        }
                        setState(1052);
                        match(RIGHT_BRACE);
                    }
                }
                    break;
                default:
                    throw new NoViableAltException(this);
                }
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    public static class Revision_date_stmtContext extends ParserRuleContext {
        public TerminalNode REVISION_DATE_KEYWORD() {
            return getToken(YangParser.REVISION_DATE_KEYWORD, 0);
        }

        public StringContext string() {
            return getRuleContext(StringContext.class, 0);
        }

        public StmtendContext stmtend() {
            return getRuleContext(StmtendContext.class, 0);
        }

        public Revision_date_stmtContext(ParserRuleContext parent,
                int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_revision_date_stmt;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).enterRevision_date_stmt(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).exitRevision_date_stmt(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof YangParserVisitor)
                return ((YangParserVisitor<? extends T>) visitor)
                        .visitRevision_date_stmt(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final Revision_date_stmtContext revision_date_stmt()
            throws RecognitionException {
        Revision_date_stmtContext _localctx = new Revision_date_stmtContext(
                _ctx, getState());
        enterRule(_localctx, 168, RULE_revision_date_stmt);
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(1055);
                match(REVISION_DATE_KEYWORD);
                setState(1056);
                string();
                setState(1057);
                stmtend();
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    public static class Revision_stmtContext extends ParserRuleContext {
        public TerminalNode RIGHT_BRACE() {
            return getToken(YangParser.RIGHT_BRACE, 0);
        }

        public Reference_stmtContext reference_stmt() {
            return getRuleContext(Reference_stmtContext.class, 0);
        }

        public TerminalNode SEMICOLON() {
            return getToken(YangParser.SEMICOLON, 0);
        }

        public TerminalNode REVISION_KEYWORD() {
            return getToken(YangParser.REVISION_KEYWORD, 0);
        }

        public StringContext string() {
            return getRuleContext(StringContext.class, 0);
        }

        public Description_stmtContext description_stmt() {
            return getRuleContext(Description_stmtContext.class, 0);
        }

        public TerminalNode LEFT_BRACE() {
            return getToken(YangParser.LEFT_BRACE, 0);
        }

        public Revision_stmtContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_revision_stmt;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).enterRevision_stmt(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).exitRevision_stmt(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof YangParserVisitor)
                return ((YangParserVisitor<? extends T>) visitor)
                        .visitRevision_stmt(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final Revision_stmtContext revision_stmt()
            throws RecognitionException {
        Revision_stmtContext _localctx = new Revision_stmtContext(_ctx,
                getState());
        enterRule(_localctx, 170, RULE_revision_stmt);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(1059);
                match(REVISION_KEYWORD);
                setState(1060);
                string();
                setState(1070);
                switch (_input.LA(1)) {
                case SEMICOLON: {
                    setState(1061);
                    match(SEMICOLON);
                }
                    break;
                case LEFT_BRACE: {
                    {
                        setState(1062);
                        match(LEFT_BRACE);
                        setState(1064);
                        _la = _input.LA(1);
                        if (_la == DESCRIPTION_KEYWORD) {
                            {
                                setState(1063);
                                description_stmt();
                            }
                        }

                        setState(1067);
                        _la = _input.LA(1);
                        if (_la == REFERENCE_KEYWORD) {
                            {
                                setState(1066);
                                reference_stmt();
                            }
                        }

                        setState(1069);
                        match(RIGHT_BRACE);
                    }
                }
                    break;
                default:
                    throw new NoViableAltException(this);
                }
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    public static class Units_stmtContext extends ParserRuleContext {
        public TerminalNode UNITS_KEYWORD() {
            return getToken(YangParser.UNITS_KEYWORD, 0);
        }

        public StringContext string() {
            return getRuleContext(StringContext.class, 0);
        }

        public StmtendContext stmtend() {
            return getRuleContext(StmtendContext.class, 0);
        }

        public Units_stmtContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_units_stmt;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).enterUnits_stmt(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).exitUnits_stmt(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof YangParserVisitor)
                return ((YangParserVisitor<? extends T>) visitor)
                        .visitUnits_stmt(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final Units_stmtContext units_stmt() throws RecognitionException {
        Units_stmtContext _localctx = new Units_stmtContext(_ctx, getState());
        enterRule(_localctx, 172, RULE_units_stmt);
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(1072);
                match(UNITS_KEYWORD);
                setState(1073);
                string();
                setState(1074);
                stmtend();
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    public static class Reference_stmtContext extends ParserRuleContext {
        public StringContext string() {
            return getRuleContext(StringContext.class, 0);
        }

        public StmtendContext stmtend() {
            return getRuleContext(StmtendContext.class, 0);
        }

        public TerminalNode REFERENCE_KEYWORD() {
            return getToken(YangParser.REFERENCE_KEYWORD, 0);
        }

        public Reference_stmtContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_reference_stmt;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).enterReference_stmt(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).exitReference_stmt(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof YangParserVisitor)
                return ((YangParserVisitor<? extends T>) visitor)
                        .visitReference_stmt(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final Reference_stmtContext reference_stmt()
            throws RecognitionException {
        Reference_stmtContext _localctx = new Reference_stmtContext(_ctx,
                getState());
        enterRule(_localctx, 174, RULE_reference_stmt);
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(1076);
                match(REFERENCE_KEYWORD);
                setState(1077);
                string();
                setState(1078);
                stmtend();
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    public static class Description_stmtContext extends ParserRuleContext {
        public StringContext string() {
            return getRuleContext(StringContext.class, 0);
        }

        public TerminalNode DESCRIPTION_KEYWORD() {
            return getToken(YangParser.DESCRIPTION_KEYWORD, 0);
        }

        public StmtendContext stmtend() {
            return getRuleContext(StmtendContext.class, 0);
        }

        public Description_stmtContext(ParserRuleContext parent,
                int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_description_stmt;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).enterDescription_stmt(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).exitDescription_stmt(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof YangParserVisitor)
                return ((YangParserVisitor<? extends T>) visitor)
                        .visitDescription_stmt(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final Description_stmtContext description_stmt()
            throws RecognitionException {
        Description_stmtContext _localctx = new Description_stmtContext(_ctx,
                getState());
        enterRule(_localctx, 176, RULE_description_stmt);
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(1080);
                match(DESCRIPTION_KEYWORD);
                setState(1081);
                string();
                setState(1082);
                stmtend();
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    public static class Contact_stmtContext extends ParserRuleContext {
        public TerminalNode CONTACT_KEYWORD() {
            return getToken(YangParser.CONTACT_KEYWORD, 0);
        }

        public StringContext string() {
            return getRuleContext(StringContext.class, 0);
        }

        public StmtendContext stmtend() {
            return getRuleContext(StmtendContext.class, 0);
        }

        public Contact_stmtContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_contact_stmt;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).enterContact_stmt(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).exitContact_stmt(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof YangParserVisitor)
                return ((YangParserVisitor<? extends T>) visitor)
                        .visitContact_stmt(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final Contact_stmtContext contact_stmt() throws RecognitionException {
        Contact_stmtContext _localctx = new Contact_stmtContext(_ctx,
                getState());
        enterRule(_localctx, 178, RULE_contact_stmt);
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(1084);
                match(CONTACT_KEYWORD);
                setState(1085);
                string();
                setState(1086);
                stmtend();
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    public static class Organization_stmtContext extends ParserRuleContext {
        public StringContext string() {
            return getRuleContext(StringContext.class, 0);
        }

        public StmtendContext stmtend() {
            return getRuleContext(StmtendContext.class, 0);
        }

        public TerminalNode ORGANIZATION_KEYWORD() {
            return getToken(YangParser.ORGANIZATION_KEYWORD, 0);
        }

        public Organization_stmtContext(ParserRuleContext parent,
                int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_organization_stmt;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).enterOrganization_stmt(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).exitOrganization_stmt(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof YangParserVisitor)
                return ((YangParserVisitor<? extends T>) visitor)
                        .visitOrganization_stmt(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final Organization_stmtContext organization_stmt()
            throws RecognitionException {
        Organization_stmtContext _localctx = new Organization_stmtContext(_ctx,
                getState());
        enterRule(_localctx, 180, RULE_organization_stmt);
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(1088);
                match(ORGANIZATION_KEYWORD);
                setState(1089);
                string();
                setState(1090);
                stmtend();
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    public static class Belongs_to_stmtContext extends ParserRuleContext {
        public TerminalNode RIGHT_BRACE() {
            return getToken(YangParser.RIGHT_BRACE, 0);
        }

        public StringContext string() {
            return getRuleContext(StringContext.class, 0);
        }

        public TerminalNode BELONGS_TO_KEYWORD() {
            return getToken(YangParser.BELONGS_TO_KEYWORD, 0);
        }

        public TerminalNode LEFT_BRACE() {
            return getToken(YangParser.LEFT_BRACE, 0);
        }

        public Prefix_stmtContext prefix_stmt() {
            return getRuleContext(Prefix_stmtContext.class, 0);
        }

        public Belongs_to_stmtContext(ParserRuleContext parent,
                int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_belongs_to_stmt;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).enterBelongs_to_stmt(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).exitBelongs_to_stmt(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof YangParserVisitor)
                return ((YangParserVisitor<? extends T>) visitor)
                        .visitBelongs_to_stmt(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final Belongs_to_stmtContext belongs_to_stmt()
            throws RecognitionException {
        Belongs_to_stmtContext _localctx = new Belongs_to_stmtContext(_ctx,
                getState());
        enterRule(_localctx, 182, RULE_belongs_to_stmt);
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(1092);
                match(BELONGS_TO_KEYWORD);
                setState(1093);
                string();
                setState(1094);
                match(LEFT_BRACE);
                setState(1095);
                prefix_stmt();
                setState(1096);
                match(RIGHT_BRACE);
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    public static class Prefix_stmtContext extends ParserRuleContext {
        public StringContext string() {
            return getRuleContext(StringContext.class, 0);
        }

        public StmtendContext stmtend() {
            return getRuleContext(StmtendContext.class, 0);
        }

        public TerminalNode PREFIX_KEYWORD() {
            return getToken(YangParser.PREFIX_KEYWORD, 0);
        }

        public Prefix_stmtContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_prefix_stmt;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).enterPrefix_stmt(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).exitPrefix_stmt(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof YangParserVisitor)
                return ((YangParserVisitor<? extends T>) visitor)
                        .visitPrefix_stmt(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final Prefix_stmtContext prefix_stmt() throws RecognitionException {
        Prefix_stmtContext _localctx = new Prefix_stmtContext(_ctx, getState());
        enterRule(_localctx, 184, RULE_prefix_stmt);
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(1098);
                match(PREFIX_KEYWORD);
                setState(1099);
                string();
                setState(1100);
                stmtend();
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    public static class Namespace_stmtContext extends ParserRuleContext {
        public StringContext string() {
            return getRuleContext(StringContext.class, 0);
        }

        public TerminalNode NAMESPACE_KEYWORD() {
            return getToken(YangParser.NAMESPACE_KEYWORD, 0);
        }

        public StmtendContext stmtend() {
            return getRuleContext(StmtendContext.class, 0);
        }

        public Namespace_stmtContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_namespace_stmt;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).enterNamespace_stmt(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).exitNamespace_stmt(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof YangParserVisitor)
                return ((YangParserVisitor<? extends T>) visitor)
                        .visitNamespace_stmt(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final Namespace_stmtContext namespace_stmt()
            throws RecognitionException {
        Namespace_stmtContext _localctx = new Namespace_stmtContext(_ctx,
                getState());
        enterRule(_localctx, 186, RULE_namespace_stmt);
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(1102);
                match(NAMESPACE_KEYWORD);
                setState(1103);
                string();
                setState(1104);
                stmtend();
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    public static class Include_stmtContext extends ParserRuleContext {
        public TerminalNode RIGHT_BRACE() {
            return getToken(YangParser.RIGHT_BRACE, 0);
        }

        public TerminalNode SEMICOLON() {
            return getToken(YangParser.SEMICOLON, 0);
        }

        public StringContext string() {
            return getRuleContext(StringContext.class, 0);
        }

        public TerminalNode INCLUDE_KEYWORD() {
            return getToken(YangParser.INCLUDE_KEYWORD, 0);
        }

        public TerminalNode LEFT_BRACE() {
            return getToken(YangParser.LEFT_BRACE, 0);
        }

        public Revision_date_stmtContext revision_date_stmt() {
            return getRuleContext(Revision_date_stmtContext.class, 0);
        }

        public Include_stmtContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_include_stmt;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).enterInclude_stmt(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).exitInclude_stmt(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof YangParserVisitor)
                return ((YangParserVisitor<? extends T>) visitor)
                        .visitInclude_stmt(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final Include_stmtContext include_stmt() throws RecognitionException {
        Include_stmtContext _localctx = new Include_stmtContext(_ctx,
                getState());
        enterRule(_localctx, 188, RULE_include_stmt);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(1106);
                match(INCLUDE_KEYWORD);
                setState(1107);
                string();
                setState(1114);
                switch (_input.LA(1)) {
                case SEMICOLON: {
                    setState(1108);
                    match(SEMICOLON);
                }
                    break;
                case LEFT_BRACE: {
                    {
                        setState(1109);
                        match(LEFT_BRACE);
                        setState(1111);
                        _la = _input.LA(1);
                        if (_la == REVISION_DATE_KEYWORD) {
                            {
                                setState(1110);
                                revision_date_stmt();
                            }
                        }

                        setState(1113);
                        match(RIGHT_BRACE);
                    }
                }
                    break;
                default:
                    throw new NoViableAltException(this);
                }
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    public static class Import_stmtContext extends ParserRuleContext {
        public TerminalNode RIGHT_BRACE() {
            return getToken(YangParser.RIGHT_BRACE, 0);
        }

        public StringContext string() {
            return getRuleContext(StringContext.class, 0);
        }

        public TerminalNode LEFT_BRACE() {
            return getToken(YangParser.LEFT_BRACE, 0);
        }

        public Prefix_stmtContext prefix_stmt() {
            return getRuleContext(Prefix_stmtContext.class, 0);
        }

        public Revision_date_stmtContext revision_date_stmt() {
            return getRuleContext(Revision_date_stmtContext.class, 0);
        }

        public TerminalNode IMPORT_KEYWORD() {
            return getToken(YangParser.IMPORT_KEYWORD, 0);
        }

        public Import_stmtContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_import_stmt;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).enterImport_stmt(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).exitImport_stmt(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof YangParserVisitor)
                return ((YangParserVisitor<? extends T>) visitor)
                        .visitImport_stmt(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final Import_stmtContext import_stmt() throws RecognitionException {
        Import_stmtContext _localctx = new Import_stmtContext(_ctx, getState());
        enterRule(_localctx, 190, RULE_import_stmt);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(1116);
                match(IMPORT_KEYWORD);
                setState(1117);
                string();
                setState(1118);
                match(LEFT_BRACE);
                setState(1119);
                prefix_stmt();
                setState(1121);
                _la = _input.LA(1);
                if (_la == REVISION_DATE_KEYWORD) {
                    {
                        setState(1120);
                        revision_date_stmt();
                    }
                }

                setState(1123);
                match(RIGHT_BRACE);
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    public static class Yang_version_stmtContext extends ParserRuleContext {
        public StringContext string() {
            return getRuleContext(StringContext.class, 0);
        }

        public StmtendContext stmtend() {
            return getRuleContext(StmtendContext.class, 0);
        }

        public TerminalNode YANG_VERSION_KEYWORD() {
            return getToken(YangParser.YANG_VERSION_KEYWORD, 0);
        }

        public Yang_version_stmtContext(ParserRuleContext parent,
                int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_yang_version_stmt;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).enterYang_version_stmt(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).exitYang_version_stmt(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof YangParserVisitor)
                return ((YangParserVisitor<? extends T>) visitor)
                        .visitYang_version_stmt(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final Yang_version_stmtContext yang_version_stmt()
            throws RecognitionException {
        Yang_version_stmtContext _localctx = new Yang_version_stmtContext(_ctx,
                getState());
        enterRule(_localctx, 192, RULE_yang_version_stmt);
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(1125);
                match(YANG_VERSION_KEYWORD);
                setState(1126);
                string();
                setState(1127);
                stmtend();
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    public static class Data_def_stmtContext extends ParserRuleContext {
        public Uses_stmtContext uses_stmt() {
            return getRuleContext(Uses_stmtContext.class, 0);
        }

        public Anyxml_stmtContext anyxml_stmt() {
            return getRuleContext(Anyxml_stmtContext.class, 0);
        }

        public List_stmtContext list_stmt() {
            return getRuleContext(List_stmtContext.class, 0);
        }

        public Leaf_stmtContext leaf_stmt() {
            return getRuleContext(Leaf_stmtContext.class, 0);
        }

        public Container_stmtContext container_stmt() {
            return getRuleContext(Container_stmtContext.class, 0);
        }

        public Choice_stmtContext choice_stmt() {
            return getRuleContext(Choice_stmtContext.class, 0);
        }

        public Leaf_list_stmtContext leaf_list_stmt() {
            return getRuleContext(Leaf_list_stmtContext.class, 0);
        }

        public Data_def_stmtContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_data_def_stmt;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).enterData_def_stmt(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).exitData_def_stmt(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof YangParserVisitor)
                return ((YangParserVisitor<? extends T>) visitor)
                        .visitData_def_stmt(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final Data_def_stmtContext data_def_stmt()
            throws RecognitionException {
        Data_def_stmtContext _localctx = new Data_def_stmtContext(_ctx,
                getState());
        enterRule(_localctx, 194, RULE_data_def_stmt);
        try {
            setState(1136);
            switch (_input.LA(1)) {
            case CONTAINER_KEYWORD:
                enterOuterAlt(_localctx, 1);
                {
                    setState(1129);
                    container_stmt();
                }
                break;
            case LEAF_KEYWORD:
                enterOuterAlt(_localctx, 2);
                {
                    setState(1130);
                    leaf_stmt();
                }
                break;
            case LEAF_LIST_KEYWORD:
                enterOuterAlt(_localctx, 3);
                {
                    setState(1131);
                    leaf_list_stmt();
                }
                break;
            case LIST_KEYWORD:
                enterOuterAlt(_localctx, 4);
                {
                    setState(1132);
                    list_stmt();
                }
                break;
            case CHOICE_KEYWORD:
                enterOuterAlt(_localctx, 5);
                {
                    setState(1133);
                    choice_stmt();
                }
                break;
            case ANYXML_KEYWORD:
                enterOuterAlt(_localctx, 6);
                {
                    setState(1134);
                    anyxml_stmt();
                }
                break;
            case USES_KEYWORD:
                enterOuterAlt(_localctx, 7);
                {
                    setState(1135);
                    uses_stmt();
                }
                break;
            default:
                throw new NoViableAltException(this);
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    public static class Body_stmtsContext extends ParserRuleContext {
        public List<Grouping_stmtContext> grouping_stmt() {
            return getRuleContexts(Grouping_stmtContext.class);
        }

        public List<Feature_stmtContext> feature_stmt() {
            return getRuleContexts(Feature_stmtContext.class);
        }

        public Identity_stmtContext identity_stmt(int i) {
            return getRuleContext(Identity_stmtContext.class, i);
        }

        public Typedef_stmtContext typedef_stmt(int i) {
            return getRuleContext(Typedef_stmtContext.class, i);
        }

        public List<Notification_stmtContext> notification_stmt() {
            return getRuleContexts(Notification_stmtContext.class);
        }

        public Grouping_stmtContext grouping_stmt(int i) {
            return getRuleContext(Grouping_stmtContext.class, i);
        }

        public Deviation_stmtContext deviation_stmt(int i) {
            return getRuleContext(Deviation_stmtContext.class, i);
        }

        public Rpc_stmtContext rpc_stmt(int i) {
            return getRuleContext(Rpc_stmtContext.class, i);
        }

        public Feature_stmtContext feature_stmt(int i) {
            return getRuleContext(Feature_stmtContext.class, i);
        }

        public List<Augment_stmtContext> augment_stmt() {
            return getRuleContexts(Augment_stmtContext.class);
        }

        public List<Rpc_stmtContext> rpc_stmt() {
            return getRuleContexts(Rpc_stmtContext.class);
        }

        public List<Typedef_stmtContext> typedef_stmt() {
            return getRuleContexts(Typedef_stmtContext.class);
        }

        public Data_def_stmtContext data_def_stmt(int i) {
            return getRuleContext(Data_def_stmtContext.class, i);
        }

        public List<Extension_stmtContext> extension_stmt() {
            return getRuleContexts(Extension_stmtContext.class);
        }

        public Extension_stmtContext extension_stmt(int i) {
            return getRuleContext(Extension_stmtContext.class, i);
        }

        public List<Data_def_stmtContext> data_def_stmt() {
            return getRuleContexts(Data_def_stmtContext.class);
        }

        public List<Identity_stmtContext> identity_stmt() {
            return getRuleContexts(Identity_stmtContext.class);
        }

        public List<Deviation_stmtContext> deviation_stmt() {
            return getRuleContexts(Deviation_stmtContext.class);
        }

        public Augment_stmtContext augment_stmt(int i) {
            return getRuleContext(Augment_stmtContext.class, i);
        }

        public Notification_stmtContext notification_stmt(int i) {
            return getRuleContext(Notification_stmtContext.class, i);
        }

        public Body_stmtsContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_body_stmts;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).enterBody_stmts(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).exitBody_stmts(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof YangParserVisitor)
                return ((YangParserVisitor<? extends T>) visitor)
                        .visitBody_stmts(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final Body_stmtsContext body_stmts() throws RecognitionException {
        Body_stmtsContext _localctx = new Body_stmtsContext(_ctx, getState());
        enterRule(_localctx, 196, RULE_body_stmts);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(1152);
                _errHandler.sync(this);
                _la = _input.LA(1);
                while (((((_la - 12)) & ~0x3f) == 0 && ((1L << (_la - 12)) & ((1L << (USES_KEYWORD - 12))
                        | (1L << (TYPEDEF_KEYWORD - 12))
                        | (1L << (RPC_KEYWORD - 12))
                        | (1L << (NOTIFICATION_KEYWORD - 12))
                        | (1L << (LIST_KEYWORD - 12))
                        | (1L << (LEAF_LIST_KEYWORD - 12))
                        | (1L << (LEAF_KEYWORD - 12))
                        | (1L << (IDENTITY_KEYWORD - 12))
                        | (1L << (GROUPING_KEYWORD - 12))
                        | (1L << (FEATURE_KEYWORD - 12))
                        | (1L << (DEVIATION_KEYWORD - 12))
                        | (1L << (EXTENSION_KEYWORD - 12))
                        | (1L << (CONTAINER_KEYWORD - 12))
                        | (1L << (CHOICE_KEYWORD - 12))
                        | (1L << (AUGMENT_KEYWORD - 12)) | (1L << (ANYXML_KEYWORD - 12)))) != 0)) {
                    {
                        {
                            setState(1148);
                            switch (_input.LA(1)) {
                            case EXTENSION_KEYWORD: {
                                setState(1138);
                                extension_stmt();
                            }
                                break;
                            case FEATURE_KEYWORD: {
                                setState(1139);
                                feature_stmt();
                            }
                                break;
                            case IDENTITY_KEYWORD: {
                                setState(1140);
                                identity_stmt();
                            }
                                break;
                            case TYPEDEF_KEYWORD: {
                                setState(1141);
                                typedef_stmt();
                            }
                                break;
                            case GROUPING_KEYWORD: {
                                setState(1142);
                                grouping_stmt();
                            }
                                break;
                            case USES_KEYWORD:
                            case LIST_KEYWORD:
                            case LEAF_LIST_KEYWORD:
                            case LEAF_KEYWORD:
                            case CONTAINER_KEYWORD:
                            case CHOICE_KEYWORD:
                            case ANYXML_KEYWORD: {
                                setState(1143);
                                data_def_stmt();
                            }
                                break;
                            case AUGMENT_KEYWORD: {
                                setState(1144);
                                augment_stmt();
                            }
                                break;
                            case RPC_KEYWORD: {
                                setState(1145);
                                rpc_stmt();
                            }
                                break;
                            case NOTIFICATION_KEYWORD: {
                                setState(1146);
                                notification_stmt();
                            }
                                break;
                            case DEVIATION_KEYWORD: {
                                setState(1147);
                                deviation_stmt();
                            }
                                break;
                            default:
                                throw new NoViableAltException(this);
                            }
                        }
                    }
                    setState(1154);
                    _errHandler.sync(this);
                    _la = _input.LA(1);
                }
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    public static class Revision_stmtsContext extends ParserRuleContext {
        public List<Revision_stmtContext> revision_stmt() {
            return getRuleContexts(Revision_stmtContext.class);
        }

        public Revision_stmtContext revision_stmt(int i) {
            return getRuleContext(Revision_stmtContext.class, i);
        }

        public Revision_stmtsContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_revision_stmts;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).enterRevision_stmts(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).exitRevision_stmts(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof YangParserVisitor)
                return ((YangParserVisitor<? extends T>) visitor)
                        .visitRevision_stmts(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final Revision_stmtsContext revision_stmts()
            throws RecognitionException {
        Revision_stmtsContext _localctx = new Revision_stmtsContext(_ctx,
                getState());
        enterRule(_localctx, 198, RULE_revision_stmts);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(1158);
                _errHandler.sync(this);
                _la = _input.LA(1);
                while (_la == REVISION_KEYWORD) {
                    {
                        {
                            setState(1155);
                            revision_stmt();
                        }
                    }
                    setState(1160);
                    _errHandler.sync(this);
                    _la = _input.LA(1);
                }
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    public static class Linkage_stmtsContext extends ParserRuleContext {
        public List<Include_stmtContext> include_stmt() {
            return getRuleContexts(Include_stmtContext.class);
        }

        public Import_stmtContext import_stmt(int i) {
            return getRuleContext(Import_stmtContext.class, i);
        }

        public Include_stmtContext include_stmt(int i) {
            return getRuleContext(Include_stmtContext.class, i);
        }

        public List<Import_stmtContext> import_stmt() {
            return getRuleContexts(Import_stmtContext.class);
        }

        public Linkage_stmtsContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_linkage_stmts;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).enterLinkage_stmts(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).exitLinkage_stmts(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof YangParserVisitor)
                return ((YangParserVisitor<? extends T>) visitor)
                        .visitLinkage_stmts(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final Linkage_stmtsContext linkage_stmts()
            throws RecognitionException {
        Linkage_stmtsContext _localctx = new Linkage_stmtsContext(_ctx,
                getState());
        enterRule(_localctx, 200, RULE_linkage_stmts);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(1165);
                _errHandler.sync(this);
                _la = _input.LA(1);
                while (_la == INCLUDE_KEYWORD || _la == IMPORT_KEYWORD) {
                    {
                        setState(1163);
                        switch (_input.LA(1)) {
                        case IMPORT_KEYWORD: {
                            setState(1161);
                            import_stmt();
                        }
                            break;
                        case INCLUDE_KEYWORD: {
                            setState(1162);
                            include_stmt();
                        }
                            break;
                        default:
                            throw new NoViableAltException(this);
                        }
                    }
                    setState(1167);
                    _errHandler.sync(this);
                    _la = _input.LA(1);
                }
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    public static class Meta_stmtsContext extends ParserRuleContext {
        public List<Reference_stmtContext> reference_stmt() {
            return getRuleContexts(Reference_stmtContext.class);
        }

        public Description_stmtContext description_stmt(int i) {
            return getRuleContext(Description_stmtContext.class, i);
        }

        public Organization_stmtContext organization_stmt(int i) {
            return getRuleContext(Organization_stmtContext.class, i);
        }

        public Contact_stmtContext contact_stmt(int i) {
            return getRuleContext(Contact_stmtContext.class, i);
        }

        public List<Contact_stmtContext> contact_stmt() {
            return getRuleContexts(Contact_stmtContext.class);
        }

        public List<Organization_stmtContext> organization_stmt() {
            return getRuleContexts(Organization_stmtContext.class);
        }

        public List<Description_stmtContext> description_stmt() {
            return getRuleContexts(Description_stmtContext.class);
        }

        public Reference_stmtContext reference_stmt(int i) {
            return getRuleContext(Reference_stmtContext.class, i);
        }

        public Meta_stmtsContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_meta_stmts;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).enterMeta_stmts(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).exitMeta_stmts(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof YangParserVisitor)
                return ((YangParserVisitor<? extends T>) visitor)
                        .visitMeta_stmts(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final Meta_stmtsContext meta_stmts() throws RecognitionException {
        Meta_stmtsContext _localctx = new Meta_stmtsContext(_ctx, getState());
        enterRule(_localctx, 202, RULE_meta_stmts);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(1174);
                _errHandler.sync(this);
                _la = _input.LA(1);
                while ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << REFERENCE_KEYWORD)
                        | (1L << ORGANIZATION_KEYWORD)
                        | (1L << DESCRIPTION_KEYWORD) | (1L << CONTACT_KEYWORD))) != 0)) {
                    {
                        setState(1172);
                        switch (_input.LA(1)) {
                        case ORGANIZATION_KEYWORD: {
                            setState(1168);
                            organization_stmt();
                        }
                            break;
                        case CONTACT_KEYWORD: {
                            setState(1169);
                            contact_stmt();
                        }
                            break;
                        case DESCRIPTION_KEYWORD: {
                            setState(1170);
                            description_stmt();
                        }
                            break;
                        case REFERENCE_KEYWORD: {
                            setState(1171);
                            reference_stmt();
                        }
                            break;
                        default:
                            throw new NoViableAltException(this);
                        }
                    }
                    setState(1176);
                    _errHandler.sync(this);
                    _la = _input.LA(1);
                }
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    public static class Submodule_header_stmtsContext extends ParserRuleContext {
        public Belongs_to_stmtContext belongs_to_stmt(int i) {
            return getRuleContext(Belongs_to_stmtContext.class, i);
        }

        public List<Belongs_to_stmtContext> belongs_to_stmt() {
            return getRuleContexts(Belongs_to_stmtContext.class);
        }

        public Yang_version_stmtContext yang_version_stmt(int i) {
            return getRuleContext(Yang_version_stmtContext.class, i);
        }

        public List<Yang_version_stmtContext> yang_version_stmt() {
            return getRuleContexts(Yang_version_stmtContext.class);
        }

        public Submodule_header_stmtsContext(ParserRuleContext parent,
                int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_submodule_header_stmts;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener)
                        .enterSubmodule_header_stmts(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener)
                        .exitSubmodule_header_stmts(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof YangParserVisitor)
                return ((YangParserVisitor<? extends T>) visitor)
                        .visitSubmodule_header_stmts(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final Submodule_header_stmtsContext submodule_header_stmts()
            throws RecognitionException {
        Submodule_header_stmtsContext _localctx = new Submodule_header_stmtsContext(
                _ctx, getState());
        enterRule(_localctx, 204, RULE_submodule_header_stmts);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(1179);
                _errHandler.sync(this);
                _la = _input.LA(1);
                do {
                    {
                        setState(1179);
                        switch (_input.LA(1)) {
                        case YANG_VERSION_KEYWORD: {
                            setState(1177);
                            yang_version_stmt();
                        }
                            break;
                        case BELONGS_TO_KEYWORD: {
                            setState(1178);
                            belongs_to_stmt();
                        }
                            break;
                        default:
                            throw new NoViableAltException(this);
                        }
                    }
                    setState(1181);
                    _errHandler.sync(this);
                    _la = _input.LA(1);
                } while (_la == YANG_VERSION_KEYWORD
                        || _la == BELONGS_TO_KEYWORD);
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    public static class Module_header_stmtsContext extends ParserRuleContext {
        public List<Namespace_stmtContext> namespace_stmt() {
            return getRuleContexts(Namespace_stmtContext.class);
        }

        public Namespace_stmtContext namespace_stmt(int i) {
            return getRuleContext(Namespace_stmtContext.class, i);
        }

        public Yang_version_stmtContext yang_version_stmt(int i) {
            return getRuleContext(Yang_version_stmtContext.class, i);
        }

        public Prefix_stmtContext prefix_stmt(int i) {
            return getRuleContext(Prefix_stmtContext.class, i);
        }

        public List<Prefix_stmtContext> prefix_stmt() {
            return getRuleContexts(Prefix_stmtContext.class);
        }

        public List<Yang_version_stmtContext> yang_version_stmt() {
            return getRuleContexts(Yang_version_stmtContext.class);
        }

        public Module_header_stmtsContext(ParserRuleContext parent,
                int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_module_header_stmts;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).enterModule_header_stmts(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).exitModule_header_stmts(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof YangParserVisitor)
                return ((YangParserVisitor<? extends T>) visitor)
                        .visitModule_header_stmts(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final Module_header_stmtsContext module_header_stmts()
            throws RecognitionException {
        Module_header_stmtsContext _localctx = new Module_header_stmtsContext(
                _ctx, getState());
        enterRule(_localctx, 206, RULE_module_header_stmts);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(1186);
                _errHandler.sync(this);
                _la = _input.LA(1);
                do {
                    {
                        setState(1186);
                        switch (_input.LA(1)) {
                        case YANG_VERSION_KEYWORD: {
                            setState(1183);
                            yang_version_stmt();
                        }
                            break;
                        case NAMESPACE_KEYWORD: {
                            setState(1184);
                            namespace_stmt();
                        }
                            break;
                        case PREFIX_KEYWORD: {
                            setState(1185);
                            prefix_stmt();
                        }
                            break;
                        default:
                            throw new NoViableAltException(this);
                        }
                    }
                    setState(1188);
                    _errHandler.sync(this);
                    _la = _input.LA(1);
                } while ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << YANG_VERSION_KEYWORD)
                        | (1L << PREFIX_KEYWORD) | (1L << NAMESPACE_KEYWORD))) != 0));
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    public static class Submodule_stmtContext extends ParserRuleContext {
        public TerminalNode RIGHT_BRACE() {
            return getToken(YangParser.RIGHT_BRACE, 0);
        }

        public Linkage_stmtsContext linkage_stmts() {
            return getRuleContext(Linkage_stmtsContext.class, 0);
        }

        public Revision_stmtsContext revision_stmts() {
            return getRuleContext(Revision_stmtsContext.class, 0);
        }

        public StringContext string() {
            return getRuleContext(StringContext.class, 0);
        }

        public Body_stmtsContext body_stmts() {
            return getRuleContext(Body_stmtsContext.class, 0);
        }

        public TerminalNode LEFT_BRACE() {
            return getToken(YangParser.LEFT_BRACE, 0);
        }

        public TerminalNode SUBMODULE_KEYWORD() {
            return getToken(YangParser.SUBMODULE_KEYWORD, 0);
        }

        public Submodule_header_stmtsContext submodule_header_stmts() {
            return getRuleContext(Submodule_header_stmtsContext.class, 0);
        }

        public Meta_stmtsContext meta_stmts() {
            return getRuleContext(Meta_stmtsContext.class, 0);
        }

        public Submodule_stmtContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_submodule_stmt;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).enterSubmodule_stmt(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).exitSubmodule_stmt(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof YangParserVisitor)
                return ((YangParserVisitor<? extends T>) visitor)
                        .visitSubmodule_stmt(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final Submodule_stmtContext submodule_stmt()
            throws RecognitionException {
        Submodule_stmtContext _localctx = new Submodule_stmtContext(_ctx,
                getState());
        enterRule(_localctx, 208, RULE_submodule_stmt);
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(1190);
                match(SUBMODULE_KEYWORD);
                setState(1191);
                string();
                setState(1192);
                match(LEFT_BRACE);
                setState(1193);
                submodule_header_stmts();
                setState(1194);
                linkage_stmts();
                setState(1195);
                meta_stmts();
                setState(1196);
                revision_stmts();
                setState(1197);
                body_stmts();
                setState(1198);
                match(RIGHT_BRACE);
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    public static class Module_stmtContext extends ParserRuleContext {
        public TerminalNode RIGHT_BRACE() {
            return getToken(YangParser.RIGHT_BRACE, 0);
        }

        public Linkage_stmtsContext linkage_stmts() {
            return getRuleContext(Linkage_stmtsContext.class, 0);
        }

        public Revision_stmtsContext revision_stmts() {
            return getRuleContext(Revision_stmtsContext.class, 0);
        }

        public StringContext string() {
            return getRuleContext(StringContext.class, 0);
        }

        public Body_stmtsContext body_stmts() {
            return getRuleContext(Body_stmtsContext.class, 0);
        }

        public Module_header_stmtsContext module_header_stmts() {
            return getRuleContext(Module_header_stmtsContext.class, 0);
        }

        public TerminalNode MODULE_KEYWORD() {
            return getToken(YangParser.MODULE_KEYWORD, 0);
        }

        public TerminalNode LEFT_BRACE() {
            return getToken(YangParser.LEFT_BRACE, 0);
        }

        public Meta_stmtsContext meta_stmts() {
            return getRuleContext(Meta_stmtsContext.class, 0);
        }

        public Module_stmtContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_module_stmt;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).enterModule_stmt(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).exitModule_stmt(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof YangParserVisitor)
                return ((YangParserVisitor<? extends T>) visitor)
                        .visitModule_stmt(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final Module_stmtContext module_stmt() throws RecognitionException {
        Module_stmtContext _localctx = new Module_stmtContext(_ctx, getState());
        enterRule(_localctx, 210, RULE_module_stmt);
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(1200);
                match(MODULE_KEYWORD);
                setState(1201);
                string();
                setState(1202);
                match(LEFT_BRACE);
                setState(1203);
                module_header_stmts();
                setState(1204);
                linkage_stmts();
                setState(1205);
                meta_stmts();
                setState(1206);
                revision_stmts();
                setState(1207);
                body_stmts();
                setState(1208);
                match(RIGHT_BRACE);
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    public static final String _serializedATN = "\2\3N\u04bd\4\2\t\2\4\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4"
            + "\t\t\t\4\n\t\n\4\13\t\13\4\f\t\f\4\r\t\r\4\16\t\16\4\17\t\17\4\20\t\20"
            + "\4\21\t\21\4\22\t\22\4\23\t\23\4\24\t\24\4\25\t\25\4\26\t\26\4\27\t\27"
            + "\4\30\t\30\4\31\t\31\4\32\t\32\4\33\t\33\4\34\t\34\4\35\t\35\4\36\t\36"
            + "\4\37\t\37\4 \t \4!\t!\4\"\t\"\4#\t#\4$\t$\4%\t%\4&\t&\4\'\t\'\4(\t(\4"
            + ")\t)\4*\t*\4+\t+\4,\t,\4-\t-\4.\t.\4/\t/\4\60\t\60\4\61\t\61\4\62\t\62"
            + "\4\63\t\63\4\64\t\64\4\65\t\65\4\66\t\66\4\67\t\67\48\t8\49\t9\4:\t:\4"
            + ";\t;\4<\t<\4=\t=\4>\t>\4?\t?\4@\t@\4A\tA\4B\tB\4C\tC\4D\tD\4E\tE\4F\t"
            + "F\4G\tG\4H\tH\4I\tI\4J\tJ\4K\tK\4L\tL\4M\tM\4N\tN\4O\tO\4P\tP\4Q\tQ\4"
            + "R\tR\4S\tS\4T\tT\4U\tU\4V\tV\4W\tW\4X\tX\4Y\tY\4Z\tZ\4[\t[\4\\\t\\\4]"
            + "\t]\4^\t^\4_\t_\4`\t`\4a\ta\4b\tb\4c\tc\4d\td\4e\te\4f\tf\4g\tg\4h\th"
            + "\4i\ti\4j\tj\4k\tk\3\2\3\2\5\2\u00d9\n\2\3\3\3\3\3\3\7\3\u00de\n\3\f\3"
            + "\16\3\u00e1\13\3\3\4\3\4\5\4\u00e5\n\4\3\4\3\4\3\5\3\5\5\5\u00eb\n\5\3"
            + "\5\3\5\5\5\u00ef\n\5\3\5\5\5\u00f2\n\5\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3\6"
            + "\3\6\3\6\3\6\7\6\u00ff\n\6\f\6\16\6\u0102\13\6\3\6\5\6\u0105\n\6\3\7\3"
            + "\7\3\7\3\7\3\7\3\7\3\7\3\7\7\7\u010f\n\7\f\7\16\7\u0112\13\7\3\7\5\7\u0115"
            + "\n\7\3\b\3\b\3\b\3\b\3\b\3\b\3\b\3\b\3\b\3\b\3\b\3\b\7\b\u0123\n\b\f\b"
            + "\16\b\u0126\13\b\3\b\5\b\u0129\n\b\3\t\3\t\3\t\3\t\3\t\5\t\u0130\n\t\3"
            + "\t\5\t\u0133\n\t\3\n\3\n\3\n\3\n\3\n\3\n\3\n\3\n\3\n\6\n\u013e\n\n\r\n"
            + "\16\n\u013f\3\n\3\n\3\13\3\13\3\13\3\13\3\13\3\13\3\13\3\13\3\13\3\13"
            + "\3\13\7\13\u014f\n\13\f\13\16\13\u0152\13\13\3\13\5\13\u0155\n\13\3\f"
            + "\3\f\3\f\3\f\3\f\6\f\u015c\n\f\r\f\16\f\u015d\3\f\3\f\3\r\3\r\3\r\3\r"
            + "\3\r\6\r\u0167\n\r\r\r\16\r\u0168\3\r\3\r\3\16\3\16\3\16\3\16\3\16\3\16"
            + "\3\16\3\16\3\16\3\16\3\16\3\16\7\16\u0179\n\16\f\16\16\16\u017c\13\16"
            + "\3\16\5\16\u017f\n\16\3\17\3\17\3\17\3\17\3\17\3\17\7\17\u0187\n\17\f"
            + "\17\16\17\u018a\13\17\3\17\5\17\u018d\n\17\3\20\3\20\3\20\3\20\3\20\3"
            + "\20\3\20\3\20\3\20\3\20\3\20\6\20\u019a\n\20\r\20\16\20\u019b\3\20\3\20"
            + "\3\21\3\21\3\21\3\21\3\21\3\21\3\21\3\21\3\21\3\21\3\21\6\21\u01ab\n\21"
            + "\r\21\16\21\u01ac\3\21\3\21\3\22\3\22\3\22\3\22\3\22\7\22\u01b6\n\22\f"
            + "\22\16\22\u01b9\13\22\3\23\3\23\7\23\u01bd\n\23\f\23\16\23\u01c0\13\23"
            + "\3\24\3\24\3\24\3\24\3\24\7\24\u01c7\n\24\f\24\16\24\u01ca\13\24\3\25"
            + "\3\25\3\25\3\25\3\25\3\25\7\25\u01d2\n\25\f\25\16\25\u01d5\13\25\3\26"
            + "\3\26\3\26\3\26\3\26\3\26\7\26\u01dd\n\26\f\26\16\26\u01e0\13\26\3\27"
            + "\3\27\3\27\3\27\3\27\3\27\7\27\u01e8\n\27\f\27\16\27\u01eb\13\27\3\30"
            + "\3\30\3\30\3\30\3\30\7\30\u01f2\n\30\f\30\16\30\u01f5\13\30\3\31\3\31"
            + "\3\31\3\31\3\31\3\31\3\31\5\31\u01fe\n\31\3\32\3\32\3\32\3\32\3\32\6\32"
            + "\u0205\n\32\r\32\16\32\u0206\3\32\3\32\5\32\u020b\n\32\3\33\3\33\3\33"
            + "\3\33\3\33\3\33\3\33\3\33\3\33\3\33\3\33\3\33\7\33\u0219\n\33\f\33\16"
            + "\33\u021c\13\33\3\33\5\33\u021f\n\33\3\34\3\34\3\34\3\34\3\34\3\34\3\34"
            + "\3\34\3\34\3\34\3\34\3\34\3\34\7\34\u022e\n\34\f\34\16\34\u0231\13\34"
            + "\3\34\5\34\u0234\n\34\3\35\3\35\3\35\3\35\3\35\3\35\3\35\3\35\3\35\3\35"
            + "\3\35\7\35\u0241\n\35\f\35\16\35\u0244\13\35\3\35\5\35\u0247\n\35\3\36"
            + "\3\36\3\36\3\36\3\36\5\36\u024e\n\36\3\37\3\37\3\37\3\37\3\37\3\37\3\37"
            + "\3\37\3\37\3\37\3\37\3\37\3\37\3\37\3\37\7\37\u025f\n\37\f\37\16\37\u0262"
            + "\13\37\3\37\5\37\u0265\n\37\3 \3 \3 \3 \3!\3!\3!\3!\3\"\3\"\3\"\3\"\3"
            + "\"\3\"\3\"\3\"\3\"\3\"\3\"\3\"\3\"\3\"\3\"\3\"\3\"\3\"\3\"\6\"\u0282\n"
            + "\"\r\"\16\"\u0283\3\"\3\"\3#\3#\3#\3#\3#\3#\3#\3#\3#\3#\3#\3#\3#\3#\3"
            + "#\3#\7#\u0298\n#\f#\16#\u029b\13#\3#\3#\3$\3$\3$\3$\3$\3$\3$\3$\3$\3$"
            + "\3$\3$\3$\3$\3$\7$\u02ae\n$\f$\16$\u02b1\13$\3$\3$\3%\3%\3%\3%\3%\3%\3"
            + "%\3%\3%\3%\3%\3%\3%\3%\3%\3%\7%\u02c5\n%\f%\16%\u02c8\13%\3%\5%\u02cb"
            + "\n%\3&\3&\3&\3&\3&\3&\3&\3&\3&\3&\3&\7&\u02d8\n&\f&\16&\u02db\13&\3&\5"
            + "&\u02de\n&\3\'\3\'\3\'\3\'\3(\3(\3)\3)\3)\3)\3*\3*\3*\3*\3+\3+\3+\3+\3"
            + ",\3,\3,\3,\3-\3-\3-\3-\3-\3-\3-\3-\3-\7-\u02ff\n-\f-\16-\u0302\13-\3-"
            + "\5-\u0305\n-\3.\3.\3/\3/\3/\3/\3\60\3\60\3\60\3\60\3\61\3\61\3\62\3\62"
            + "\3\62\3\62\3\63\3\63\3\64\3\64\3\64\3\64\3\65\3\65\3\66\3\66\3\66\3\66"
            + "\3\67\3\67\3\67\3\67\38\38\38\38\38\38\38\38\38\78\u0330\n8\f8\168\u0333"
            + "\138\38\58\u0336\n8\39\69\u0339\n9\r9\169\u033a\3:\6:\u033e\n:\r:\16:"
            + "\u033f\3;\3;\3<\5<\u0345\n<\3=\3=\3>\3>\3>\3>\3?\3?\3?\3?\3@\3@\3A\3A"
            + "\3A\3A\3A\3A\3A\3A\3A\7A\u035c\nA\fA\16A\u035f\13A\3A\5A\u0362\nA\3B\6"
            + "B\u0365\nB\rB\16B\u0366\3C\3C\3C\3C\3D\3D\3D\3D\3D\3D\3D\3D\3D\7D\u0376"
            + "\nD\fD\16D\u0379\13D\3D\5D\u037c\nD\3E\3E\3E\3E\3E\3E\3E\3E\3E\7E\u0387"
            + "\nE\fE\16E\u038a\13E\3E\5E\u038d\nE\3F\3F\7F\u0391\nF\fF\16F\u0394\13"
            + "F\3G\3G\3G\3G\3H\5H\u039b\nH\3H\3H\3H\5H\u03a0\nH\5H\u03a2\nH\3I\3I\3"
            + "I\3I\3I\3I\3I\3I\3I\7I\u03ad\nI\fI\16I\u03b0\13I\3I\5I\u03b3\nI\3J\3J"
            + "\3K\3K\3K\3K\3K\3K\3K\3K\3K\5K\u03c0\nK\3L\3L\3L\3L\3L\3L\3L\5L\u03c9"
            + "\nL\3M\3M\3M\3M\3M\3M\3M\3M\3M\6M\u03d4\nM\rM\16M\u03d5\3M\3M\3N\3N\3"
            + "N\3N\3O\3O\3O\3O\3O\3O\3O\3O\7O\u03e6\nO\fO\16O\u03e9\13O\3O\5O\u03ec"
            + "\nO\3P\3P\3P\3P\3Q\3Q\3Q\3Q\3Q\3Q\3Q\3Q\7Q\u03fa\nQ\fQ\16Q\u03fd\13Q\3"
            + "Q\5Q\u0400\nQ\3R\3R\3S\3S\3S\3S\3T\3T\3T\3T\3T\5T\u040d\nT\3T\5T\u0410"
            + "\nT\3U\3U\3U\3U\3U\3U\3U\3U\7U\u041a\nU\fU\16U\u041d\13U\3U\5U\u0420\n"
            + "U\3V\3V\3V\3V\3W\3W\3W\3W\3W\5W\u042b\nW\3W\5W\u042e\nW\3W\5W\u0431\n"
            + "W\3X\3X\3X\3X\3Y\3Y\3Y\3Y\3Z\3Z\3Z\3Z\3[\3[\3[\3[\3\\\3\\\3\\\3\\\3]\3"
            + "]\3]\3]\3]\3]\3^\3^\3^\3^\3_\3_\3_\3_\3`\3`\3`\3`\3`\5`\u045a\n`\3`\5"
            + "`\u045d\n`\3a\3a\3a\3a\3a\5a\u0464\na\3a\3a\3b\3b\3b\3b\3c\3c\3c\3c\3"
            + "c\3c\3c\5c\u0473\nc\3d\3d\3d\3d\3d\3d\3d\3d\3d\3d\5d\u047f\nd\7d\u0481"
            + "\nd\fd\16d\u0484\13d\3e\7e\u0487\ne\fe\16e\u048a\13e\3f\3f\7f\u048e\n"
            + "f\ff\16f\u0491\13f\3g\3g\3g\3g\7g\u0497\ng\fg\16g\u049a\13g\3h\3h\6h\u049e"
            + "\nh\rh\16h\u049f\3i\3i\3i\6i\u04a5\ni\ri\16i\u04a6\3j\3j\3j\3j\3j\3j\3"
            + "j\3j\3j\3j\3k\3k\3k\3k\3k\3k\3k\3k\3k\3k\3k\2l\2\4\6\b\n\f\16\20\22\24"
            + "\26\30\32\34\36 \"$&(*,.\60\62\64\668:<>@BDFHJLNPRTVXZ\\^`bdfhjlnprtv"
            + "xz|~\u0080\u0082\u0084\u0086\u0088\u008a\u008c\u008e\u0090\u0092\u0094"
            + "\u0096\u0098\u009a\u009c\u009e\u00a0\u00a2\u00a4\u00a6\u00a8\u00aa\u00ac"
            + "\u00ae\u00b0\u00b2\u00b4\u00b6\u00b8\u00ba\u00bc\u00be\u00c0\u00c2\u00c4"
            + "\u00c6\u00c8\u00ca\u00cc\u00ce\u00d0\u00d2\u00d4\2\2\u05a3\2\u00d8\3\2"
            + "\2\2\4\u00da\3\2\2\2\6\u00e2\3\2\2\2\b\u00f1\3\2\2\2\n\u00f3\3\2\2\2\f"
            + "\u0106\3\2\2\2\16\u0116\3\2\2\2\20\u012a\3\2\2\2\22\u0134\3\2\2\2\24\u0143"
            + "\3\2\2\2\26\u0156\3\2\2\2\30\u0161\3\2\2\2\32\u016c\3\2\2\2\34\u0180\3"
            + "\2\2\2\36\u018e\3\2\2\2 \u019f\3\2\2\2\"\u01b7\3\2\2\2$\u01be\3\2\2\2"
            + "&\u01c8\3\2\2\2(\u01d3\3\2\2\2*\u01de\3\2\2\2,\u01e9\3\2\2\2.\u01f3\3"
            + "\2\2\2\60\u01fd\3\2\2\2\62\u01ff\3\2\2\2\64\u020c\3\2\2\2\66\u0220\3\2"
            + "\2\28\u0235\3\2\2\2:\u024d\3\2\2\2<\u024f\3\2\2\2>\u0266\3\2\2\2@\u026a"
            + "\3\2\2\2B\u026e\3\2\2\2D\u0287\3\2\2\2F\u029e\3\2\2\2H\u02b4\3\2\2\2J"
            + "\u02cc\3\2\2\2L\u02df\3\2\2\2N\u02e3\3\2\2\2P\u02e5\3\2\2\2R\u02e9\3\2"
            + "\2\2T\u02ed\3\2\2\2V\u02f1\3\2\2\2X\u02f5\3\2\2\2Z\u0306\3\2\2\2\\\u0308"
            + "\3\2\2\2^\u030c\3\2\2\2`\u0310\3\2\2\2b\u0312\3\2\2\2d\u0316\3\2\2\2f"
            + "\u0318\3\2\2\2h\u031c\3\2\2\2j\u031e\3\2\2\2l\u0322\3\2\2\2n\u0326\3\2"
            + "\2\2p\u0338\3\2\2\2r\u033d\3\2\2\2t\u0341\3\2\2\2v\u0344\3\2\2\2x\u0346"
            + "\3\2\2\2z\u0348\3\2\2\2|\u034c\3\2\2\2~\u0350\3\2\2\2\u0080\u0352\3\2"
            + "\2\2\u0082\u0364\3\2\2\2\u0084\u0368\3\2\2\2\u0086\u036c\3\2\2\2\u0088"
            + "\u037d\3\2\2\2\u008a\u0392\3\2\2\2\u008c\u0395\3\2\2\2\u008e\u03a1\3\2"
            + "\2\2\u0090\u03a3\3\2\2\2\u0092\u03b4\3\2\2\2\u0094\u03bf\3\2\2\2\u0096"
            + "\u03c1\3\2\2\2\u0098\u03ca\3\2\2\2\u009a\u03d9\3\2\2\2\u009c\u03dd\3\2"
            + "\2\2\u009e\u03ed\3\2\2\2\u00a0\u03f1\3\2\2\2\u00a2\u0401\3\2\2\2\u00a4"
            + "\u0403\3\2\2\2\u00a6\u0407\3\2\2\2\u00a8\u0411\3\2\2\2\u00aa\u0421\3\2"
            + "\2\2\u00ac\u0425\3\2\2\2\u00ae\u0432\3\2\2\2\u00b0\u0436\3\2\2\2\u00b2"
            + "\u043a\3\2\2\2\u00b4\u043e\3\2\2\2\u00b6\u0442\3\2\2\2\u00b8\u0446\3\2"
            + "\2\2\u00ba\u044c\3\2\2\2\u00bc\u0450\3\2\2\2\u00be\u0454\3\2\2\2\u00c0"
            + "\u045e\3\2\2\2\u00c2\u0467\3\2\2\2\u00c4\u0472\3\2\2\2\u00c6\u0482\3\2"
            + "\2\2\u00c8\u0488\3\2\2\2\u00ca\u048f\3\2\2\2\u00cc\u0498\3\2\2\2\u00ce"
            + "\u049d\3\2\2\2\u00d0\u04a4\3\2\2\2\u00d2\u04a8\3\2\2\2\u00d4\u04b2\3\2"
            + "\2\2\u00d6\u00d9\5\u00d4k\2\u00d7\u00d9\5\u00d2j\2\u00d8\u00d6\3\2\2\2"
            + "\u00d8\u00d7\3\2\2\2\u00d9\3\3\2\2\2\u00da\u00df\7L\2\2\u00db\u00dc\7"
            + "\6\2\2\u00dc\u00de\7L\2\2\u00dd\u00db\3\2\2\2\u00de\u00e1\3\2\2\2\u00df"
            + "\u00dd\3\2\2\2\u00df\u00e0\3\2\2\2\u00e0\5\3\2\2\2\u00e1\u00df\3\2\2\2"
            + "\u00e2\u00e4\7K\2\2\u00e3\u00e5\5\4\3\2\u00e4\u00e3\3\2\2\2\u00e4\u00e5"
            + "\3\2\2\2\u00e5\u00e6\3\2\2\2\u00e6\u00e7\5\b\5\2\u00e7\7\3\2\2\2\u00e8"
            + "\u00ea\7\3\2\2\u00e9\u00eb\5\6\4\2\u00ea\u00e9\3\2\2\2\u00ea\u00eb\3\2"
            + "\2\2\u00eb\u00f2\3\2\2\2\u00ec\u00ee\7\4\2\2\u00ed\u00ef\5\6\4\2\u00ee"
            + "\u00ed\3\2\2\2\u00ee\u00ef\3\2\2\2\u00ef\u00f0\3\2\2\2\u00f0\u00f2\7\5"
            + "\2\2\u00f1\u00e8\3\2\2\2\u00f1\u00ec\3\2\2\2\u00f2\t\3\2\2\2\u00f3\u00f4"
            + "\78\2\2\u00f4\u0104\5\4\3\2\u00f5\u0105\7\3\2\2\u00f6\u0100\7\4\2\2\u00f7"
            + "\u00ff\5\u0096L\2\u00f8\u00ff\5\u00aeX\2\u00f9\u00ff\5\u0084C\2\u00fa"
            + "\u00ff\5f\64\2\u00fb\u00ff\5b\62\2\u00fc\u00ff\5R*\2\u00fd\u00ff\5P)\2"
            + "\u00fe\u00f7\3\2\2\2\u00fe\u00f8\3\2\2\2\u00fe\u00f9\3\2\2\2\u00fe\u00fa"
            + "\3\2\2\2\u00fe\u00fb\3\2\2\2\u00fe\u00fc\3\2\2\2\u00fe\u00fd\3\2\2\2\u00ff"
            + "\u0102\3\2\2\2\u0100\u00fe\3\2\2\2\u0100\u0101\3\2\2\2\u0101\u0103\3\2"
            + "\2\2\u0102\u0100\3\2\2\2\u0103\u0105\7\5\2\2\u0104\u00f5\3\2\2\2\u0104"
            + "\u00f6\3\2\2\2\u0105\13\3\2\2\2\u0106\u0107\78\2\2\u0107\u0114\5\4\3\2"
            + "\u0108\u0115\7\3\2\2\u0109\u0110\7\4\2\2\u010a\u010f\5\u00aeX\2\u010b"
            + "\u010f\5X-\2\u010c\u010f\5> \2\u010d\u010f\5\u0084C\2\u010e\u010a\3\2"
            + "\2\2\u010e\u010b\3\2\2\2\u010e\u010c\3\2\2\2\u010e\u010d\3\2\2\2\u010f"
            + "\u0112\3\2\2\2\u0110\u010e\3\2\2\2\u0110\u0111\3\2\2\2\u0111\u0113\3\2"
            + "\2\2\u0112\u0110\3\2\2\2\u0113\u0115\7\5\2\2\u0114\u0108\3\2\2\2\u0114"
            + "\u0109\3\2\2\2\u0115\r\3\2\2\2\u0116\u0117\78\2\2\u0117\u0128\5\4\3\2"
            + "\u0118\u0129\7\3\2\2\u0119\u0124\7\4\2\2\u011a\u0123\5\u00aeX\2\u011b"
            + "\u0123\5X-\2\u011c\u0123\5> \2\u011d\u0123\5\u0084C\2\u011e\u0123\5f\64"
            + "\2\u011f\u0123\5b\62\2\u0120\u0123\5R*\2\u0121\u0123\5P)\2\u0122\u011a"
            + "\3\2\2\2\u0122\u011b\3\2\2\2\u0122\u011c\3\2\2\2\u0122\u011d\3\2\2\2\u0122"
            + "\u011e\3\2\2\2\u0122\u011f\3\2\2\2\u0122\u0120\3\2\2\2\u0122\u0121\3\2"
            + "\2\2\u0123\u0126\3\2\2\2\u0124\u0122\3\2\2\2\u0124\u0125\3\2\2\2\u0125"
            + "\u0127\3\2\2\2\u0126\u0124\3\2\2\2\u0127\u0129\7\5\2\2\u0128\u0118\3\2"
            + "\2\2\u0128\u0119\3\2\2\2\u0129\17\3\2\2\2\u012a\u012b\78\2\2\u012b\u0132"
            + "\5\4\3\2\u012c\u0133\7\3\2\2\u012d\u012f\7\4\2\2\u012e\u0130\5\6\4\2\u012f"
            + "\u012e\3\2\2\2\u012f\u0130\3\2\2\2\u0130\u0131\3\2\2\2\u0131\u0133\7\5"
            + "\2\2\u0132\u012c\3\2\2\2\u0132\u012d\3\2\2\2\u0133\21\3\2\2\2\u0134\u0135"
            + "\79\2\2\u0135\u0136\5\4\3\2\u0136\u013d\7\4\2\2\u0137\u013e\5\u00b2Z\2"
            + "\u0138\u013e\5\u00b0Y\2\u0139\u013e\5\20\t\2\u013a\u013e\5\16\b\2\u013b"
            + "\u013e\5\n\6\2\u013c\u013e\5\f\7\2\u013d\u0137\3\2\2\2\u013d\u0138\3\2"
            + "\2\2\u013d\u0139\3\2\2\2\u013d\u013a\3\2\2\2\u013d\u013b\3\2\2\2\u013d"
            + "\u013c\3\2\2\2\u013e\u013f\3\2\2\2\u013f\u013d\3\2\2\2\u013f\u0140\3\2"
            + "\2\2\u0140\u0141\3\2\2\2\u0141\u0142\7\5\2\2\u0142\23\3\2\2\2\u0143\u0144"
            + "\7$\2\2\u0144\u0154\5\4\3\2\u0145\u0155\7\3\2\2\u0146\u0150\7\4\2\2\u0147"
            + "\u014f\5\u009aN\2\u0148\u014f\5j\66\2\u0149\u014f\5\u00b2Z\2\u014a\u014f"
            + "\5\u00b0Y\2\u014b\u014f\5\u0098M\2\u014c\u014f\5J&\2\u014d\u014f\5\u00c4"
            + "c\2\u014e\u0147\3\2\2\2\u014e\u0148\3\2\2\2\u014e\u0149\3\2\2\2\u014e"
            + "\u014a\3\2\2\2\u014e\u014b\3\2\2\2\u014e\u014c\3\2\2\2\u014e\u014d\3\2"
            + "\2\2\u014f\u0152\3\2\2\2\u0150\u014e\3\2\2\2\u0150\u0151\3\2\2\2\u0151"
            + "\u0153\3\2\2\2\u0152\u0150\3\2\2\2\u0153\u0155\7\5\2\2\u0154\u0145\3\2"
            + "\2\2\u0154\u0146\3\2\2\2\u0155\25\3\2\2\2\u0156\u0157\7!\2\2\u0157\u015b"
            + "\7\4\2\2\u0158\u015c\5\u0098M\2\u0159\u015c\5J&\2\u015a\u015c\5\u00c4"
            + "c\2\u015b\u0158\3\2\2\2\u015b\u0159\3\2\2\2\u015b\u015a\3\2\2\2\u015c"
            + "\u015d\3\2\2\2\u015d\u015b\3\2\2\2\u015d\u015e\3\2\2\2\u015e\u015f\3\2"
            + "\2\2\u015f\u0160\7\5\2\2\u0160\27\3\2\2\2\u0161\u0162\7\60\2\2\u0162\u0166"
            + "\7\4\2\2\u0163\u0167\5\u0098M\2\u0164\u0167\5J&\2\u0165\u0167\5\u00c4"
            + "c\2\u0166\u0163\3\2\2\2\u0166\u0164\3\2\2\2\u0166\u0165\3\2\2\2\u0167"
            + "\u0168\3\2\2\2\u0168\u0166\3\2\2\2\u0168\u0169\3\2\2\2\u0169\u016a\3\2"
            + "\2\2\u016a\u016b\7\5\2\2\u016b\31\3\2\2\2\u016c\u016d\7\25\2\2\u016d\u017e"
            + "\5\4\3\2\u016e\u017f\7\3\2\2\u016f\u017a\7\4\2\2\u0170\u0179\5\u009aN"
            + "\2\u0171\u0179\5j\66\2\u0172\u0179\5\u00b2Z\2\u0173\u0179\5\u00b0Y\2\u0174"
            + "\u0179\5\u0098M\2\u0175\u0179\5J&\2\u0176\u0179\5\30\r\2\u0177\u0179\5"
            + "\26\f\2\u0178\u0170\3\2\2\2\u0178\u0171\3\2\2\2\u0178\u0172\3\2\2\2\u0178"
            + "\u0173\3\2\2\2\u0178\u0174\3\2\2\2\u0178\u0175\3\2\2\2\u0178\u0176\3\2"
            + "\2\2\u0178\u0177\3\2\2\2\u0179\u017c\3\2\2\2\u017a\u0178\3\2\2\2\u017a"
            + "\u017b\3\2\2\2\u017b\u017d\3\2\2\2\u017c\u017a\3\2\2\2\u017d\u017f\7\5"
            + "\2\2\u017e\u016e\3\2\2\2\u017e\u016f\3\2\2\2\u017f\33\3\2\2\2\u0180\u0181"
            + "\7\f\2\2\u0181\u018c\5\4\3\2\u0182\u018d\7\3\2\2\u0183\u0188\7\4\2\2\u0184"
            + "\u0187\5\u00b2Z\2\u0185\u0187\5\u00b0Y\2\u0186\u0184\3\2\2\2\u0186\u0185"
            + "\3\2\2\2\u0187\u018a\3\2\2\2\u0188\u0186\3\2\2\2\u0188\u0189\3\2\2\2\u0189"
            + "\u018b\3\2\2\2\u018a\u0188\3\2\2\2\u018b\u018d\7\5\2\2\u018c\u0182\3\2"
            + "\2\2\u018c\u0183\3\2\2\2\u018d\35\3\2\2\2\u018e\u018f\7H\2\2\u018f\u0190"
            + "\5\4\3\2\u0190\u0199\7\4\2\2\u0191\u019a\5\6\4\2\u0192\u019a\5\34\17\2"
            + "\u0193\u019a\5\u009aN\2\u0194\u019a\5j\66\2\u0195\u019a\5\u00b2Z\2\u0196"
            + "\u019a\5\u00b0Y\2\u0197\u019a\5\u00c4c\2\u0198\u019a\58\35\2\u0199\u0191"
            + "\3\2\2\2\u0199\u0192\3\2\2\2\u0199\u0193\3\2\2\2\u0199\u0194\3\2\2\2\u0199"
            + "\u0195\3\2\2\2\u0199\u0196\3\2\2\2\u0199\u0197\3\2\2\2\u0199\u0198\3\2"
            + "\2\2\u019a\u019b\3\2\2\2\u019b\u0199\3\2\2\2\u019b\u019c\3\2\2\2\u019c"
            + "\u019d\3\2\2\2\u019d\u019e\7\5\2\2\u019e\37\3\2\2\2\u019f\u01a0\7H\2\2"
            + "\u01a0\u01a1\5\4\3\2\u01a1\u01aa\7\4\2\2\u01a2\u01ab\5\6\4\2\u01a3\u01ab"
            + "\5\34\17\2\u01a4\u01ab\5\u009aN\2\u01a5\u01ab\5j\66\2\u01a6\u01ab\5\u00b2"
            + "Z\2\u01a7\u01ab\5\u00b0Y\2\u01a8\u01ab\5\u00c4c\2\u01a9\u01ab\58\35\2"
            + "\u01aa\u01a2\3\2\2\2\u01aa\u01a3\3\2\2\2\u01aa\u01a4\3\2\2\2\u01aa\u01a5"
            + "\3\2\2\2\u01aa\u01a6\3\2\2\2\u01aa\u01a7\3\2\2\2\u01aa\u01a8\3\2\2\2\u01aa"
            + "\u01a9\3\2\2\2\u01ab\u01ac\3\2\2\2\u01ac\u01aa\3\2\2\2\u01ac\u01ad\3\2"
            + "\2\2\u01ad\u01ae\3\2\2\2\u01ae\u01af\7\5\2\2\u01af!\3\2\2\2\u01b0\u01b6"
            + "\5X-\2\u01b1\u01b6\5f\64\2\u01b2\u01b6\5b\62\2\u01b3\u01b6\5\u00b2Z\2"
            + "\u01b4\u01b6\5\u00b0Y\2\u01b5\u01b0\3\2\2\2\u01b5\u01b1\3\2\2\2\u01b5"
            + "\u01b2\3\2\2\2\u01b5\u01b3\3\2\2\2\u01b5\u01b4\3\2\2\2\u01b6\u01b9\3\2"
            + "\2\2\u01b7\u01b5\3\2\2\2\u01b7\u01b8\3\2\2\2\u01b8#\3\2\2\2\u01b9\u01b7"
            + "\3\2\2\2\u01ba\u01bd\5\u00b2Z\2\u01bb\u01bd\5\u00b0Y\2\u01bc\u01ba\3\2"
            + "\2\2\u01bc\u01bb\3\2\2\2\u01bd\u01c0\3\2\2\2\u01be\u01bc\3\2\2\2\u01be"
            + "\u01bf\3\2\2\2\u01bf%\3\2\2\2\u01c0\u01be\3\2\2\2\u01c1\u01c7\5\u0084"
            + "C\2\u01c2\u01c7\5f\64\2\u01c3\u01c7\5b\62\2\u01c4\u01c7\5\u00b2Z\2\u01c5"
            + "\u01c7\5\u00b0Y\2\u01c6\u01c1\3\2\2\2\u01c6\u01c2\3\2\2\2\u01c6\u01c3"
            + "\3\2\2\2\u01c6\u01c4\3\2\2\2\u01c6\u01c5\3\2\2\2\u01c7\u01ca\3\2\2\2\u01c8"
            + "\u01c6\3\2\2\2\u01c8\u01c9\3\2\2\2\u01c9\'\3\2\2\2\u01ca\u01c8\3\2\2\2"
            + "\u01cb\u01d2\5X-\2\u01cc\u01d2\5f\64\2\u01cd\u01d2\5R*\2\u01ce\u01d2\5"
            + "P)\2\u01cf\u01d2\5\u00b2Z\2\u01d0\u01d2\5\u00b0Y\2\u01d1\u01cb\3\2\2\2"
            + "\u01d1\u01cc\3\2\2\2\u01d1\u01cd\3\2\2\2\u01d1\u01ce\3\2\2\2\u01d1\u01cf"
            + "\3\2\2\2\u01d1\u01d0\3\2\2\2\u01d2\u01d5\3\2\2\2\u01d3\u01d1\3\2\2\2\u01d3"
            + "\u01d4\3\2\2\2\u01d4)\3\2\2\2\u01d5\u01d3\3\2\2\2\u01d6\u01dd\5X-\2\u01d7"
            + "\u01dd\5f\64\2\u01d8\u01dd\5R*\2\u01d9\u01dd\5P)\2\u01da\u01dd\5\u00b2"
            + "Z\2\u01db\u01dd\5\u00b0Y\2\u01dc\u01d6\3\2\2\2\u01dc\u01d7\3\2\2\2\u01dc"
            + "\u01d8\3\2\2\2\u01dc\u01d9\3\2\2\2\u01dc\u01da\3\2\2\2\u01dc\u01db\3\2"
            + "\2\2\u01dd\u01e0\3\2\2\2\u01de\u01dc\3\2\2\2\u01de\u01df\3\2\2\2\u01df"
            + "+\3\2\2\2\u01e0\u01de\3\2\2\2\u01e1\u01e8\5X-\2\u01e2\u01e8\5\u0084C\2"
            + "\u01e3\u01e8\5f\64\2\u01e4\u01e8\5b\62\2\u01e5\u01e8\5\u00b2Z\2\u01e6"
            + "\u01e8\5\u00b0Y\2\u01e7\u01e1\3\2\2\2\u01e7\u01e2\3\2\2\2\u01e7\u01e3"
            + "\3\2\2\2\u01e7\u01e4\3\2\2\2\u01e7\u01e5\3\2\2\2\u01e7\u01e6\3\2\2\2\u01e8"
            + "\u01eb\3\2\2\2\u01e9\u01e7\3\2\2\2\u01e9\u01ea\3\2\2\2\u01ea-\3\2\2\2"
            + "\u01eb\u01e9\3\2\2\2\u01ec\u01f2\5X-\2\u01ed\u01f2\5^\60\2\u01ee\u01f2"
            + "\5f\64\2\u01ef\u01f2\5\u00b2Z\2\u01f0\u01f2\5\u00b0Y\2\u01f1\u01ec\3\2"
            + "\2\2\u01f1\u01ed\3\2\2\2\u01f1\u01ee\3\2\2\2\u01f1\u01ef\3\2\2\2\u01f1"
            + "\u01f0\3\2\2\2\u01f2\u01f5\3\2\2\2\u01f3\u01f1\3\2\2\2\u01f3\u01f4\3\2"
            + "\2\2\u01f4/\3\2\2\2\u01f5\u01f3\3\2\2\2\u01f6\u01fe\5.\30\2\u01f7\u01fe"
            + "\5,\27\2\u01f8\u01fe\5*\26\2\u01f9\u01fe\5(\25\2\u01fa\u01fe\5&\24\2\u01fb"
            + "\u01fe\5$\23\2\u01fc\u01fe\5\"\22\2\u01fd\u01f6\3\2\2\2\u01fd\u01f7\3"
            + "\2\2\2\u01fd\u01f8\3\2\2\2\u01fd\u01f9\3\2\2\2\u01fd\u01fa\3\2\2\2\u01fd"
            + "\u01fb\3\2\2\2\u01fd\u01fc\3\2\2\2\u01fe\61\3\2\2\2\u01ff\u0200\7\31\2"
            + "\2\u0200\u020a\5\4\3\2\u0201\u020b\7\3\2\2\u0202\u0204\7\4\2\2\u0203\u0205"
            + "\5\60\31\2\u0204\u0203\3\2\2\2\u0205\u0206\3\2\2\2\u0206\u0204\3\2\2\2"
            + "\u0206\u0207\3\2\2\2\u0207\u0208\3\2\2\2\u0208\u0209\7\5\2\2\u0209\u020b"
            + "\3\2\2\2\u020a\u0201\3\2\2\2\u020a\u0202\3\2\2\2\u020b\63\3\2\2\2\u020c"
            + "\u020d\7\16\2\2\u020d\u021e\5\4\3\2\u020e\u021f\7\3\2\2\u020f\u021a\7"
            + "\4\2\2\u0210\u0219\5\6\4\2\u0211\u0219\5\34\17\2\u0212\u0219\5\u009aN"
            + "\2\u0213\u0219\5j\66\2\u0214\u0219\5\u00b2Z\2\u0215\u0219\5\u00b0Y\2\u0216"
            + "\u0219\5\62\32\2\u0217\u0219\5 \21\2\u0218\u0210\3\2\2\2\u0218\u0211\3"
            + "\2\2\2\u0218\u0212\3\2\2\2\u0218\u0213\3\2\2\2\u0218\u0214\3\2\2\2\u0218"
            + "\u0215\3\2\2\2\u0218\u0216\3\2\2\2\u0218\u0217\3\2\2\2\u0219\u021c\3\2"
            + "\2\2\u021a\u0218\3\2\2\2\u021a\u021b\3\2\2\2\u021b\u021d\3\2\2\2\u021c"
            + "\u021a\3\2\2\2\u021d\u021f\7\5\2\2\u021e\u020e\3\2\2\2\u021e\u020f\3\2"
            + "\2\2\u021f\65\3\2\2\2\u0220\u0221\7J\2\2\u0221\u0233\5\4\3\2\u0222\u0234"
            + "\7\3\2\2\u0223\u022f\7\4\2\2\u0224\u022e\5\6\4\2\u0225\u022e\5\34\17\2"
            + "\u0226\u022e\5\u009aN\2\u0227\u022e\5X-\2\u0228\u022e\5f\64\2\u0229\u022e"
            + "\5b\62\2\u022a\u022e\5j\66\2\u022b\u022e\5\u00b2Z\2\u022c\u022e\5\u00b0"
            + "Y\2\u022d\u0224\3\2\2\2\u022d\u0225\3\2\2\2\u022d\u0226\3\2\2\2\u022d"
            + "\u0227\3\2\2\2\u022d\u0228\3\2\2\2\u022d\u0229\3\2\2\2\u022d\u022a\3\2"
            + "\2\2\u022d\u022b\3\2\2\2\u022d\u022c\3\2\2\2\u022e\u0231\3\2\2\2\u022f"
            + "\u022d\3\2\2\2\u022f\u0230\3\2\2\2\u0230\u0232\3\2\2\2\u0231\u022f\3\2"
            + "\2\2\u0232\u0234\7\5\2\2\u0233\u0222\3\2\2\2\u0233\u0223\3\2\2\2\u0234"
            + "\67\3\2\2\2\u0235\u0236\7D\2\2\u0236\u0246\5\4\3\2\u0237\u0247\7\3\2\2"
            + "\u0238\u0242\7\4\2\2\u0239\u0241\5\6\4\2\u023a\u0241\5\34\17\2\u023b\u0241"
            + "\5\u009aN\2\u023c\u0241\5j\66\2\u023d\u0241\5\u00b2Z\2\u023e\u0241\5\u00b0"
            + "Y\2\u023f\u0241\5\u00c4c\2\u0240\u0239\3\2\2\2\u0240\u023a\3\2\2\2\u0240"
            + "\u023b\3\2\2\2\u0240\u023c\3\2\2\2\u0240\u023d\3\2\2\2\u0240\u023e\3\2"
            + "\2\2\u0240\u023f\3\2\2\2\u0241\u0244\3\2\2\2\u0242\u0240\3\2\2\2\u0242"
            + "\u0243\3\2\2\2\u0243\u0245\3\2\2\2\u0244\u0242\3\2\2\2\u0245\u0247\7\5"
            + "\2\2\u0246\u0237\3\2\2\2\u0246\u0238\3\2\2\2\u02479\3\2\2\2\u0248\u024e"
            + "\5H%\2\u0249\u024e\5F$\2\u024a\u024e\5D#\2\u024b\u024e\5B\"\2\u024c\u024e"
            + "\5\66\34\2\u024d\u0248\3\2\2\2\u024d\u0249\3\2\2\2\u024d\u024a\3\2\2\2"
            + "\u024d\u024b\3\2\2\2\u024d\u024c\3\2\2\2\u024e;\3\2\2\2\u024f\u0250\7"
            + "C\2\2\u0250\u0264\5\4\3\2\u0251\u0265\7\3\2\2\u0252\u0260\7\4\2\2\u0253"
            + "\u025f\5\6\4\2\u0254\u025f\5\34\17\2\u0255\u025f\5\u009aN\2\u0256\u025f"
            + "\5\u0084C\2\u0257\u025f\5f\64\2\u0258\u025f\5b\62\2\u0259\u025f\5j\66"
            + "\2\u025a\u025f\5\u00b2Z\2\u025b\u025f\5\u00b0Y\2\u025c\u025f\5:\36\2\u025d"
            + "\u025f\58\35\2\u025e\u0253\3\2\2\2\u025e\u0254\3\2\2\2\u025e\u0255\3\2"
            + "\2\2\u025e\u0256\3\2\2\2\u025e\u0257\3\2\2\2\u025e\u0258\3\2\2\2\u025e"
            + "\u0259\3\2\2\2\u025e\u025a\3\2\2\2\u025e\u025b\3\2\2\2\u025e\u025c\3\2"
            + "\2\2\u025e\u025d\3\2\2\2\u025f\u0262\3\2\2\2\u0260\u025e\3\2\2\2\u0260"
            + "\u0261\3\2\2\2\u0261\u0263\3\2\2\2\u0262\u0260\3\2\2\2\u0263\u0265\7\5"
            + "\2\2\u0264\u0251\3\2\2\2\u0264\u0252\3\2\2\2\u0265=\3\2\2\2\u0266\u0267"
            + "\7\20\2\2\u0267\u0268\5\4\3\2\u0268\u0269\5\b\5\2\u0269?\3\2\2\2\u026a"
            + "\u026b\7/\2\2\u026b\u026c\5\4\3\2\u026c\u026d\5\b\5\2\u026dA\3\2\2\2\u026e"
            + "\u026f\7+\2\2\u026f\u0270\5\4\3\2\u0270\u0281\7\4\2\2\u0271\u0282\5\6"
            + "\4\2\u0272\u0282\5\34\17\2\u0273\u0282\5\u009aN\2\u0274\u0282\5X-\2\u0275"
            + "\u0282\5@!\2\u0276\u0282\5> \2\u0277\u0282\5f\64\2\u0278\u0282\5R*\2\u0279"
            + "\u0282\5P)\2\u027a\u0282\5\\/\2\u027b\u0282\5j\66\2\u027c\u0282\5\u00b2"
            + "Z\2\u027d\u0282\5\u00b0Y\2\u027e\u0282\5\u0098M\2\u027f\u0282\5J&\2\u0280"
            + "\u0282\5\u00c4c\2\u0281\u0271\3\2\2\2\u0281\u0272\3\2\2\2\u0281\u0273"
            + "\3\2\2\2\u0281\u0274\3\2\2\2\u0281\u0275\3\2\2\2\u0281\u0276\3\2\2\2\u0281"
            + "\u0277\3\2\2\2\u0281\u0278\3\2\2\2\u0281\u0279\3\2\2\2\u0281\u027a\3\2"
            + "\2\2\u0281\u027b\3\2\2\2\u0281\u027c\3\2\2\2\u0281\u027d\3\2\2\2\u0281"
            + "\u027e\3\2\2\2\u0281\u027f\3\2\2\2\u0281\u0280\3\2\2\2\u0282\u0283\3\2"
            + "\2\2\u0283\u0281\3\2\2\2\u0283\u0284\3\2\2\2\u0284\u0285\3\2\2\2\u0285"
            + "\u0286\7\5\2\2\u0286C\3\2\2\2\u0287\u0288\7-\2\2\u0288\u0289\5\4\3\2\u0289"
            + "\u0299\7\4\2\2\u028a\u0298\5\6\4\2\u028b\u0298\5\34\17\2\u028c\u0298\5"
            + "\u009aN\2\u028d\u0298\5\u0096L\2\u028e\u0298\5\u00aeX\2\u028f\u0298\5"
            + "X-\2\u0290\u0298\5f\64\2\u0291\u0298\5R*\2\u0292\u0298\5P)\2\u0293\u0298"
            + "\5\\/\2\u0294\u0298\5j\66\2\u0295\u0298\5\u00b2Z\2\u0296\u0298\5\u00b0"
            + "Y\2\u0297\u028a\3\2\2\2\u0297\u028b\3\2\2\2\u0297\u028c\3\2\2\2\u0297"
            + "\u028d\3\2\2\2\u0297\u028e\3\2\2\2\u0297\u028f\3\2\2\2\u0297\u0290\3\2"
            + "\2\2\u0297\u0291\3\2\2\2\u0297\u0292\3\2\2\2\u0297\u0293\3\2\2\2\u0297"
            + "\u0294\3\2\2\2\u0297\u0295\3\2\2\2\u0297\u0296\3\2\2\2\u0298\u029b\3\2"
            + "\2\2\u0299\u0297\3\2\2\2\u0299\u029a\3\2\2\2\u029a\u029c\3\2\2\2\u029b"
            + "\u0299\3\2\2\2\u029c\u029d\7\5\2\2\u029dE\3\2\2\2\u029e\u029f\7.\2\2\u029f"
            + "\u02a0\5\4\3\2\u02a0\u02af\7\4\2\2\u02a1\u02ae\5\6\4\2\u02a2\u02ae\5\34"
            + "\17\2\u02a3\u02ae\5\u009aN\2\u02a4\u02ae\5\u0096L\2\u02a5\u02ae\5\u00ae"
            + "X\2\u02a6\u02ae\5X-\2\u02a7\u02ae\5\u0084C\2\u02a8\u02ae\5f\64\2\u02a9"
            + "\u02ae\5b\62\2\u02aa\u02ae\5j\66\2\u02ab\u02ae\5\u00b2Z\2\u02ac\u02ae"
            + "\5\u00b0Y\2\u02ad\u02a1\3\2\2\2\u02ad\u02a2\3\2\2\2\u02ad\u02a3\3\2\2"
            + "\2\u02ad\u02a4\3\2\2\2\u02ad\u02a5\3\2\2\2\u02ad\u02a6\3\2\2\2\u02ad\u02a7"
            + "\3\2\2\2\u02ad\u02a8\3\2\2\2\u02ad\u02a9\3\2\2\2\u02ad\u02aa\3\2\2\2\u02ad"
            + "\u02ab\3\2\2\2\u02ad\u02ac\3\2\2\2\u02ae\u02b1\3\2\2\2\u02af\u02ad\3\2"
            + "\2\2\u02af\u02b0\3\2\2\2\u02b0\u02b2\3\2\2\2\u02b1\u02af\3\2\2\2\u02b2"
            + "\u02b3\7\5\2\2\u02b3G\3\2\2\2\u02b4\u02b5\7@\2\2\u02b5\u02ca\5\4\3\2\u02b6"
            + "\u02cb\7\3\2\2\u02b7\u02c6\7\4\2\2\u02b8\u02c5\5\6\4\2\u02b9\u02c5\5\34"
            + "\17\2\u02ba\u02c5\5\u009aN\2\u02bb\u02c5\5X-\2\u02bc\u02c5\5^\60\2\u02bd"
            + "\u02c5\5f\64\2\u02be\u02c5\5j\66\2\u02bf\u02c5\5\u00b2Z\2\u02c0\u02c5"
            + "\5\u00b0Y\2\u02c1\u02c5\5\u0098M\2\u02c2\u02c5\5J&\2\u02c3\u02c5\5\u00c4"
            + "c\2\u02c4\u02b8\3\2\2\2\u02c4\u02b9\3\2\2\2\u02c4\u02ba\3\2\2\2\u02c4"
            + "\u02bb\3\2\2\2\u02c4\u02bc\3\2\2\2\u02c4\u02bd\3\2\2\2\u02c4\u02be\3\2"
            + "\2\2\u02c4\u02bf\3\2\2\2\u02c4\u02c0\3\2\2\2\u02c4\u02c1\3\2\2\2\u02c4"
            + "\u02c2\3\2\2\2\u02c4\u02c3\3\2\2\2\u02c5\u02c8\3\2\2\2\u02c6\u02c4\3\2"
            + "\2\2\u02c6\u02c7\3\2\2\2\u02c7\u02c9\3\2\2\2\u02c8\u02c6\3\2\2\2\u02c9"
            + "\u02cb\7\5\2\2\u02ca\u02b6\3\2\2\2\u02ca\u02b7\3\2\2\2\u02cbI\3\2\2\2"
            + "\u02cc\u02cd\7\65\2\2\u02cd\u02dd\5\4\3\2\u02ce\u02de\7\3\2\2\u02cf\u02d9"
            + "\7\4\2\2\u02d0\u02d8\5\6\4\2\u02d1\u02d8\5j\66\2\u02d2\u02d8\5\u00b2Z"
            + "\2\u02d3\u02d8\5\u00b0Y\2\u02d4\u02d8\5\u0098M\2\u02d5\u02d8\5J&\2\u02d6"
            + "\u02d8\5\u00c4c\2\u02d7\u02d0\3\2\2\2\u02d7\u02d1\3\2\2\2\u02d7\u02d2"
            + "\3\2\2\2\u02d7\u02d3\3\2\2\2\u02d7\u02d4\3\2\2\2\u02d7\u02d5\3\2\2\2\u02d7"
            + "\u02d6\3\2\2\2\u02d8\u02db\3\2\2\2\u02d9\u02d7\3\2\2\2\u02d9\u02da\3\2"
            + "\2\2\u02da\u02dc\3\2\2\2\u02db\u02d9\3\2\2\2\u02dc\u02de\7\5\2\2\u02dd"
            + "\u02ce\3\2\2\2\u02dd\u02cf\3\2\2\2\u02deK\3\2\2\2\u02df\u02e0\7\r\2\2"
            + "\u02e0\u02e1\5\4\3\2\u02e1\u02e2\5\b\5\2\u02e2M\3\2\2\2\u02e3\u02e4\5"
            + "\4\3\2\u02e4O\3\2\2\2\u02e5\u02e6\7)\2\2\u02e6\u02e7\5N(\2\u02e7\u02e8"
            + "\5\b\5\2\u02e8Q\3\2\2\2\u02e9\u02ea\7(\2\2\u02ea\u02eb\5\4\3\2\u02eb\u02ec"
            + "\5\b\5\2\u02ecS\3\2\2\2\u02ed\u02ee\7<\2\2\u02ee\u02ef\5\4\3\2\u02ef\u02f0"
            + "\5\b\5\2\u02f0U\3\2\2\2\u02f1\u02f2\7;\2\2\u02f2\u02f3\5\4\3\2\u02f3\u02f4"
            + "\5\b\5\2\u02f4W\3\2\2\2\u02f5\u02f6\7&\2\2\u02f6\u0304\5\4\3\2\u02f7\u0305"
            + "\7\3\2\2\u02f8\u0300\7\4\2\2\u02f9\u02ff\5\6\4\2\u02fa\u02ff\5V,\2\u02fb"
            + "\u02ff\5T+\2\u02fc\u02ff\5\u00b2Z\2\u02fd\u02ff\5\u00b0Y\2\u02fe\u02f9"
            + "\3\2\2\2\u02fe\u02fa\3\2\2\2\u02fe\u02fb\3\2\2\2\u02fe\u02fc\3\2\2\2\u02fe"
            + "\u02fd\3\2\2\2\u02ff\u0302\3\2\2\2\u0300\u02fe\3\2\2\2\u0300\u0301\3\2"
            + "\2\2\u0301\u0303\3\2\2\2\u0302\u0300\3\2\2\2\u0303\u0305\7\5\2\2\u0304"
            + "\u02f7\3\2\2\2\u0304\u02f8\3\2\2\2\u0305Y\3\2\2\2\u0306\u0307\5\4\3\2"
            + "\u0307[\3\2\2\2\u0308\u0309\7#\2\2\u0309\u030a\5Z.\2\u030a\u030b\5\b\5"
            + "\2\u030b]\3\2\2\2\u030c\u030d\7\34\2\2\u030d\u030e\5\4\3\2\u030e\u030f"
            + "\5\b\5\2\u030f_\3\2\2\2\u0310\u0311\5\4\3\2\u0311a\3\2\2\2\u0312\u0313"
            + "\7*\2\2\u0313\u0314\5`\61\2\u0314\u0315\5\b\5\2\u0315c\3\2\2\2\u0316\u0317"
            + "\5\4\3\2\u0317e\3\2\2\2\u0318\u0319\7B\2\2\u0319\u031a\5d\63\2\u031a\u031b"
            + "\5\b\5\2\u031bg\3\2\2\2\u031c\u031d\5\4\3\2\u031di\3\2\2\2\u031e\u031f"
            + "\7\24\2\2\u031f\u0320\5h\65\2\u0320\u0321\5\b\5\2\u0321k\3\2\2\2\u0322"
            + "\u0323\7\36\2\2\u0323\u0324\5\4\3\2\u0324\u0325\5\b\5\2\u0325m\3\2\2\2"
            + "\u0326\u0327\7E\2\2\u0327\u0335\5\4\3\2\u0328\u0336\7\3\2\2\u0329\u0331"
            + "\7\4\2\2\u032a\u0330\5\6\4\2\u032b\u0330\5l\67\2\u032c\u0330\5j\66\2\u032d"
            + "\u0330\5\u00b2Z\2\u032e\u0330\5\u00b0Y\2\u032f\u032a\3\2\2\2\u032f\u032b"
            + "\3\2\2\2\u032f\u032c\3\2\2\2\u032f\u032d\3\2\2\2\u032f\u032e\3\2\2\2\u0330"
            + "\u0333\3\2\2\2\u0331\u032f\3\2\2\2\u0331\u0332\3\2\2\2\u0332\u0334\3\2"
            + "\2\2\u0333\u0331\3\2\2\2\u0334\u0336\7\5\2\2\u0335\u0328\3\2\2\2\u0335"
            + "\u0329\3\2\2\2\u0336o\3\2\2\2\u0337\u0339\5n8\2\u0338\u0337\3\2\2\2\u0339"
            + "\u033a\3\2\2\2\u033a\u0338\3\2\2\2\u033a\u033b\3\2\2\2\u033bq\3\2\2\2"
            + "\u033c\u033e\5\u0096L\2\u033d\u033c\3\2\2\2\u033e\u033f\3\2\2\2\u033f"
            + "\u033d\3\2\2\2\u033f\u0340\3\2\2\2\u0340s\3\2\2\2\u0341\u0342\5\u009e"
            + "P\2\u0342u\3\2\2\2\u0343\u0345\5z>\2\u0344\u0343\3\2\2\2\u0344\u0345\3"
            + "\2\2\2\u0345w\3\2\2\2\u0346\u0347\5\4\3\2\u0347y\3\2\2\2\u0348\u0349\7"
            + "\30\2\2\u0349\u034a\5x=\2\u034a\u034b\5\b\5\2\u034b{\3\2\2\2\u034c\u034d"
            + "\7 \2\2\u034d\u034e\5\4\3\2\u034e\u034f\5\b\5\2\u034f}\3\2\2\2\u0350\u0351"
            + "\5|?\2\u0351\177\3\2\2\2\u0352\u0353\7=\2\2\u0353\u0361\5\4\3\2\u0354"
            + "\u0362\7\3\2\2\u0355\u035d\7\4\2\2\u0356\u035c\5\6\4\2\u0357\u035c\5L"
            + "\'\2\u0358\u035c\5j\66\2\u0359\u035c\5\u00b2Z\2\u035a\u035c\5\u00b0Y\2"
            + "\u035b\u0356\3\2\2\2\u035b\u0357\3\2\2\2\u035b\u0358\3\2\2\2\u035b\u0359"
            + "\3\2\2\2\u035b\u035a\3\2\2\2\u035c\u035f\3\2\2\2\u035d\u035b\3\2\2\2\u035d"
            + "\u035e\3\2\2\2\u035e\u0360\3\2\2\2\u035f\u035d\3\2\2\2\u0360\u0362\7\5"
            + "\2\2\u0361\u0354\3\2\2\2\u0361\u0355\3\2\2\2\u0362\u0081\3\2\2\2\u0363"
            + "\u0365\5\u0080A\2\u0364\u0363\3\2\2\2\u0365\u0366\3\2\2\2\u0366\u0364"
            + "\3\2\2\2\u0366\u0367\3\2\2\2\u0367\u0083\3\2\2\2\u0368\u0369\7?\2\2\u0369"
            + "\u036a\5\4\3\2\u036a\u036b\5\b\5\2\u036b\u0085\3\2\2\2\u036c\u036d\7\37"
            + "\2\2\u036d\u037b\5\4\3\2\u036e\u037c\7\3\2\2\u036f\u0377\7\4\2\2\u0370"
            + "\u0376\5\6\4\2\u0371\u0376\5V,\2\u0372\u0376\5T+\2\u0373\u0376\5\u00b2"
            + "Z\2\u0374\u0376\5\u00b0Y\2\u0375\u0370\3\2\2\2\u0375\u0371\3\2\2\2\u0375"
            + "\u0372\3\2\2\2\u0375\u0373\3\2\2\2\u0375\u0374\3\2\2\2\u0376\u0379\3\2"
            + "\2\2\u0377\u0375\3\2\2\2\u0377\u0378\3\2\2\2\u0378\u037a\3\2\2\2\u0379"
            + "\u0377\3\2\2\2\u037a\u037c\7\5\2\2\u037b\u036e\3\2\2\2\u037b\u036f\3\2"
            + "\2\2\u037c\u0087\3\2\2\2\u037d\u037e\7,\2\2\u037e\u038c\5\4\3\2\u037f"
            + "\u038d\7\3\2\2\u0380\u0388\7\4\2\2\u0381\u0387\5\6\4\2\u0382\u0387\5V"
            + ",\2\u0383\u0387\5T+\2\u0384\u0387\5\u00b2Z\2\u0385\u0387\5\u00b0Y\2\u0386"
            + "\u0381\3\2\2\2\u0386\u0382\3\2\2\2\u0386\u0383\3\2\2\2\u0386\u0384\3\2"
            + "\2\2\u0386\u0385\3\2\2\2\u0387\u038a\3\2\2\2\u0388\u0386\3\2\2\2\u0388"
            + "\u0389\3\2\2\2\u0389\u038b\3\2\2\2\u038a\u0388\3\2\2\2\u038b\u038d\7\5"
            + "\2\2\u038c\u037f\3\2\2\2\u038c\u0380\3\2\2\2\u038d\u0089\3\2\2\2\u038e"
            + "\u0391\5\u0088E\2\u038f\u0391\5\u0086D\2\u0390\u038e\3\2\2\2\u0390\u038f"
            + "\3\2\2\2\u0391\u0394\3\2\2\2\u0392\u0390\3\2\2\2\u0392\u0393\3\2\2\2\u0393"
            + "\u008b\3\2\2\2\u0394\u0392\3\2\2\2\u0395\u0396\7\66\2\2\u0396\u0397\5"
            + "\4\3\2\u0397\u0398\5\b\5\2\u0398\u008d\3\2\2\2\u0399\u039b\5\u0092J\2"
            + "\u039a\u0399\3\2\2\2\u039a\u039b\3\2\2\2\u039b\u039c\3\2\2\2\u039c\u03a2"
            + "\5\u008cG\2\u039d\u039f\5\u008cG\2\u039e\u03a0\5\u0092J\2\u039f\u039e"
            + "\3\2\2\2\u039f\u03a0\3\2\2\2\u03a0\u03a2\3\2\2\2\u03a1\u039a\3\2\2\2\u03a1"
            + "\u039d\3\2\2\2\u03a2\u008f\3\2\2\2\u03a3\u03a4\7\33\2\2\u03a4\u03b2\5"
            + "\4\3\2\u03a5\u03b3\7\3\2\2\u03a6\u03ae\7\4\2\2\u03a7\u03ad\5\6\4\2\u03a8"
            + "\u03ad\5V,\2\u03a9\u03ad\5T+\2\u03aa\u03ad\5\u00b2Z\2\u03ab\u03ad\5\u00b0"
            + "Y\2\u03ac\u03a7\3\2\2\2\u03ac\u03a8\3\2\2\2\u03ac\u03a9\3\2\2\2\u03ac"
            + "\u03aa\3\2\2\2\u03ac\u03ab\3\2\2\2\u03ad\u03b0\3\2\2\2\u03ae\u03ac\3\2"
            + "\2\2\u03ae\u03af\3\2\2\2\u03af\u03b1\3\2\2\2\u03b0\u03ae\3\2\2\2\u03b1"
            + "\u03b3\7\5\2\2\u03b2\u03a5\3\2\2\2\u03b2\u03a6\3\2\2\2\u03b3\u0091\3\2"
            + "\2\2\u03b4\u03b5\5\u0090I\2\u03b5\u0093\3\2\2\2\u03b6\u03c0\5\u0092J\2"
            + "\u03b7\u03c0\5\u008eH\2\u03b8\u03c0\5\u008aF\2\u03b9\u03c0\5\u0082B\2"
            + "\u03ba\u03c0\5~@\2\u03bb\u03c0\5t;\2\u03bc\u03c0\5v<\2\u03bd\u03c0\5p"
            + "9\2\u03be\u03c0\5r:\2\u03bf\u03b6\3\2\2\2\u03bf\u03b7\3\2\2\2\u03bf\u03b8"
            + "\3\2\2\2\u03bf\u03b9\3\2\2\2\u03bf\u03ba\3\2\2\2\u03bf\u03bb\3\2\2\2\u03bf"
            + "\u03bc\3\2\2\2\u03bf\u03bd\3\2\2\2\u03bf\u03be\3\2\2\2\u03c0\u0095\3\2"
            + "\2\2\u03c1\u03c2\7\22\2\2\u03c2\u03c8\5\4\3\2\u03c3\u03c9\7\3\2\2\u03c4"
            + "\u03c5\7\4\2\2\u03c5\u03c6\5\u0094K\2\u03c6\u03c7\7\5\2\2\u03c7\u03c9"
            + "\3\2\2\2\u03c8\u03c3\3\2\2\2\u03c8\u03c4\3\2\2\2\u03c9\u0097\3\2\2\2\u03ca"
            + "\u03cb\7\21\2\2\u03cb\u03cc\5\4\3\2\u03cc\u03d3\7\4\2\2\u03cd\u03d4\5"
            + "\u0096L\2\u03ce\u03d4\5\u00aeX\2\u03cf\u03d4\5\u0084C\2\u03d0\u03d4\5"
            + "j\66\2\u03d1\u03d4\5\u00b2Z\2\u03d2\u03d4\5\u00b0Y\2\u03d3\u03cd\3\2\2"
            + "\2\u03d3\u03ce\3\2\2\2\u03d3\u03cf\3\2\2\2\u03d3\u03d0\3\2\2\2\u03d3\u03d1"
            + "\3\2\2\2\u03d3\u03d2\3\2\2\2\u03d4\u03d5\3\2\2\2\u03d5\u03d3\3\2\2\2\u03d5"
            + "\u03d6\3\2\2\2\u03d6\u03d7\3\2\2\2\u03d7\u03d8\7\5\2\2\u03d8\u0099\3\2"
            + "\2\2\u03d9\u03da\7\63\2\2\u03da\u03db\5\4\3\2\u03db\u03dc\5\b\5\2\u03dc"
            + "\u009b\3\2\2\2\u03dd\u03de\7\67\2\2\u03de\u03eb\5\4\3\2\u03df\u03ec\7"
            + "\3\2\2\u03e0\u03e7\7\4\2\2\u03e1\u03e6\5\u009aN\2\u03e2\u03e6\5j\66\2"
            + "\u03e3\u03e6\5\u00b2Z\2\u03e4\u03e6\5\u00b0Y\2\u03e5\u03e1\3\2\2\2\u03e5"
            + "\u03e2\3\2\2\2\u03e5\u03e3\3\2\2\2\u03e5\u03e4\3\2\2\2\u03e6\u03e9\3\2"
            + "\2\2\u03e7\u03e5\3\2\2\2\u03e7\u03e8\3\2\2\2\u03e8\u03ea\3\2\2\2\u03e9"
            + "\u03e7\3\2\2\2\u03ea\u03ec\7\5\2\2\u03eb\u03df\3\2\2\2\u03eb\u03e0\3\2"
            + "\2\2\u03ec\u009d\3\2\2\2\u03ed\u03ee\7G\2\2\u03ee\u03ef\5\4\3\2\u03ef"
            + "\u03f0\5\b\5\2\u03f0\u009f\3\2\2\2\u03f1\u03f2\7\64\2\2\u03f2\u03ff\5"
            + "\4\3\2\u03f3\u0400\7\3\2\2\u03f4\u03fb\7\4\2\2\u03f5\u03fa\5\u009eP\2"
            + "\u03f6\u03fa\5j\66\2\u03f7\u03fa\5\u00b2Z\2\u03f8\u03fa\5\u00b0Y\2\u03f9"
            + "\u03f5\3\2\2\2\u03f9\u03f6\3\2\2\2\u03f9\u03f7\3\2\2\2\u03f9\u03f8\3\2"
            + "\2\2\u03fa\u03fd\3\2\2\2\u03fb\u03f9\3\2\2\2\u03fb\u03fc\3\2\2\2\u03fc"
            + "\u03fe\3\2\2\2\u03fd\u03fb\3\2\2\2\u03fe\u0400\7\5\2\2\u03ff\u03f3\3\2"
            + "\2\2\u03ff\u03f4\3\2\2\2\u0400\u00a1\3\2\2\2\u0401\u0402\5\4\3\2\u0402"
            + "\u00a3\3\2\2\2\u0403\u0404\7\n\2\2\u0404\u0405\5\u00a2R\2\u0405\u0406"
            + "\5\b\5\2\u0406\u00a5\3\2\2\2\u0407\u0408\7I\2\2\u0408\u040f\5\4\3\2\u0409"
            + "\u0410\7\3\2\2\u040a\u040c\7\4\2\2\u040b\u040d\5\u00a4S\2\u040c\u040b"
            + "\3\2\2\2\u040c\u040d\3\2\2\2\u040d\u040e\3\2\2\2\u040e\u0410\7\5\2\2\u040f"
            + "\u0409\3\2\2\2\u040f\u040a\3\2\2\2\u0410\u00a7\3\2\2\2\u0411\u0412\7:"
            + "\2\2\u0412\u041f\5\4\3\2\u0413\u0420\7\3\2\2\u0414\u041b\7\4\2\2\u0415"
            + "\u041a\5\u00a6T\2\u0416\u041a\5j\66\2\u0417\u041a\5\u00b2Z\2\u0418\u041a"
            + "\5\u00b0Y\2\u0419\u0415\3\2\2\2\u0419\u0416\3\2\2\2\u0419\u0417\3\2\2"
            + "\2\u0419\u0418\3\2\2\2\u041a\u041d\3\2\2\2\u041b\u0419\3\2\2\2\u041b\u041c"
            + "\3\2\2\2\u041c\u041e\3\2\2\2\u041d\u041b\3\2\2\2\u041e\u0420\7\5\2\2\u041f"
            + "\u0413\3\2\2\2\u041f\u0414\3\2\2\2\u0420\u00a9\3\2\2\2\u0421\u0422\7\26"
            + "\2\2\u0422\u0423\5\4\3\2\u0423\u0424\5\b\5\2\u0424\u00ab\3\2\2\2\u0425"
            + "\u0426\7\27\2\2\u0426\u0430\5\4\3\2\u0427\u0431\7\3\2\2\u0428\u042a\7"
            + "\4\2\2\u0429\u042b\5\u00b2Z\2\u042a\u0429\3\2\2\2\u042a\u042b\3\2\2\2"
            + "\u042b\u042d\3\2\2\2\u042c\u042e\5\u00b0Y\2\u042d\u042c\3\2\2\2\u042d"
            + "\u042e\3\2\2\2\u042e\u042f\3\2\2\2\u042f\u0431\7\5\2\2\u0430\u0427\3\2"
            + "\2\2\u0430\u0428\3\2\2\2\u0431\u00ad\3\2\2\2\u0432\u0433\7\17\2\2\u0433"
            + "\u0434\5\4\3\2\u0434\u0435\5\b\5\2\u0435\u00af\3\2\2\2\u0436\u0437\7\32"
            + "\2\2\u0437\u0438\5\4\3\2\u0438\u0439\5\b\5\2\u0439\u00b1\3\2\2\2\u043a"
            + "\u043b\7>\2\2\u043b\u043c\5\4\3\2\u043c\u043d\5\b\5\2\u043d\u00b3\3\2"
            + "\2\2\u043e\u043f\7A\2\2\u043f\u0440\5\4\3\2\u0440\u0441\5\b\5\2\u0441"
            + "\u00b5\3\2\2\2\u0442\u0443\7\"\2\2\u0443\u0444\5\4\3\2\u0444\u0445\5\b"
            + "\5\2\u0445\u00b7\3\2\2\2\u0446\u0447\7F\2\2\u0447\u0448\5\4\3\2\u0448"
            + "\u0449\7\4\2\2\u0449\u044a\5\u00ba^\2\u044a\u044b\7\5\2\2\u044b\u00b9"
            + "\3\2\2\2\u044c\u044d\7\35\2\2\u044d\u044e\5\4\3\2\u044e\u044f\5\b\5\2"
            + "\u044f\u00bb\3\2\2\2\u0450\u0451\7%\2\2\u0451\u0452\5\4\3\2\u0452\u0453"
            + "\5\b\5\2\u0453\u00bd\3\2\2\2\u0454\u0455\7\61\2\2\u0455\u045c\5\4\3\2"
            + "\u0456\u045d\7\3\2\2\u0457\u0459\7\4\2\2\u0458\u045a\5\u00aaV\2\u0459"
            + "\u0458\3\2\2\2\u0459\u045a\3\2\2\2\u045a\u045b\3\2\2\2\u045b\u045d\7\5"
            + "\2\2\u045c\u0456\3\2\2\2\u045c\u0457\3\2\2\2\u045d\u00bf\3\2\2\2\u045e"
            + "\u045f\7\62\2\2\u045f\u0460\5\4\3\2\u0460\u0461\7\4\2\2\u0461\u0463\5"
            + "\u00ba^\2\u0462\u0464\5\u00aaV\2\u0463\u0462\3\2\2\2\u0463\u0464\3\2\2"
            + "\2\u0464\u0465\3\2\2\2\u0465\u0466\7\5\2\2\u0466\u00c1\3\2\2\2\u0467\u0468"
            + "\7\13\2\2\u0468\u0469\5\4\3\2\u0469\u046a\5\b\5\2\u046a\u00c3\3\2\2\2"
            + "\u046b\u0473\5H%\2\u046c\u0473\5F$\2\u046d\u0473\5D#\2\u046e\u0473\5B"
            + "\"\2\u046f\u0473\5<\37\2\u0470\u0473\5\66\34\2\u0471\u0473\5\64\33\2\u0472"
            + "\u046b\3\2\2\2\u0472\u046c\3\2\2\2\u0472\u046d\3\2\2\2\u0472\u046e\3\2"
            + "\2\2\u0472\u046f\3\2\2\2\u0472\u0470\3\2\2\2\u0472\u0471\3\2\2\2\u0473"
            + "\u00c5\3\2\2\2\u0474\u047f\5\u00a8U\2\u0475\u047f\5\u009cO\2\u0476\u047f"
            + "\5\u00a0Q\2\u0477\u047f\5\u0098M\2\u0478\u047f\5J&\2\u0479\u047f\5\u00c4"
            + "c\2\u047a\u047f\5\36\20\2\u047b\u047f\5\32\16\2\u047c\u047f\5\24\13\2"
            + "\u047d\u047f\5\22\n\2\u047e\u0474\3\2\2\2\u047e\u0475\3\2\2\2\u047e\u0476"
            + "\3\2\2\2\u047e\u0477\3\2\2\2\u047e\u0478\3\2\2\2\u047e\u0479\3\2\2\2\u047e"
            + "\u047a\3\2\2\2\u047e\u047b\3\2\2\2\u047e\u047c\3\2\2\2\u047e\u047d\3\2"
            + "\2\2\u047f\u0481\3\2\2\2\u0480\u047e\3\2\2\2\u0481\u0484\3\2\2\2\u0482"
            + "\u0480\3\2\2\2\u0482\u0483\3\2\2\2\u0483\u00c7\3\2\2\2\u0484\u0482\3\2"
            + "\2\2\u0485\u0487\5\u00acW\2\u0486\u0485\3\2\2\2\u0487\u048a\3\2\2\2\u0488"
            + "\u0486\3\2\2\2\u0488\u0489\3\2\2\2\u0489\u00c9\3\2\2\2\u048a\u0488\3\2"
            + "\2\2\u048b\u048e\5\u00c0a\2\u048c\u048e\5\u00be`\2\u048d\u048b\3\2\2\2"
            + "\u048d\u048c\3\2\2\2\u048e\u0491\3\2\2\2\u048f\u048d\3\2\2\2\u048f\u0490"
            + "\3\2\2\2\u0490\u00cb\3\2\2\2\u0491\u048f\3\2\2\2\u0492\u0497\5\u00b6\\"
            + "\2\u0493\u0497\5\u00b4[\2\u0494\u0497\5\u00b2Z\2\u0495\u0497\5\u00b0Y"
            + "\2\u0496\u0492\3\2\2\2\u0496\u0493\3\2\2\2\u0496\u0494\3\2\2\2\u0496\u0495"
            + "\3\2\2\2\u0497\u049a\3\2\2\2\u0498\u0496\3\2\2\2\u0498\u0499\3\2\2\2\u0499"
            + "\u00cd\3\2\2\2\u049a\u0498\3\2\2\2\u049b\u049e\5\u00c2b\2\u049c\u049e"
            + "\5\u00b8]\2\u049d\u049b\3\2\2\2\u049d\u049c\3\2\2\2\u049e\u049f\3\2\2"
            + "\2\u049f\u049d\3\2\2\2\u049f\u04a0\3\2\2\2\u04a0\u00cf\3\2\2\2\u04a1\u04a5"
            + "\5\u00c2b\2\u04a2\u04a5\5\u00bc_\2\u04a3\u04a5\5\u00ba^\2\u04a4\u04a1"
            + "\3\2\2\2\u04a4\u04a2\3\2\2\2\u04a4\u04a3\3\2\2\2\u04a5\u04a6\3\2\2\2\u04a6"
            + "\u04a4\3\2\2\2\u04a6\u04a7\3\2\2\2\u04a7\u00d1\3\2\2\2\u04a8\u04a9\7\23"
            + "\2\2\u04a9\u04aa\5\4\3\2\u04aa\u04ab\7\4\2\2\u04ab\u04ac\5\u00ceh\2\u04ac"
            + "\u04ad\5\u00caf\2\u04ad\u04ae\5\u00ccg\2\u04ae\u04af\5\u00c8e\2\u04af"
            + "\u04b0\5\u00c6d\2\u04b0\u04b1\7\5\2\2\u04b1\u00d3\3\2\2\2\u04b2\u04b3"
            + "\7\'\2\2\u04b3\u04b4\5\4\3\2\u04b4\u04b5\7\4\2\2\u04b5\u04b6\5\u00d0i"
            + "\2\u04b6\u04b7\5\u00caf\2\u04b7\u04b8\5\u00ccg\2\u04b8\u04b9\5\u00c8e"
            + "\2\u04b9\u04ba\5\u00c6d\2\u04ba\u04bb\7\5\2\2\u04bb\u00d5\3\2\2\2\u008c"
            + "\u00d8\u00df\u00e4\u00ea\u00ee\u00f1\u00fe\u0100\u0104\u010e\u0110\u0114"
            + "\u0122\u0124\u0128\u012f\u0132\u013d\u013f\u014e\u0150\u0154\u015b\u015d"
            + "\u0166\u0168\u0178\u017a\u017e\u0186\u0188\u018c\u0199\u019b\u01aa\u01ac"
            + "\u01b5\u01b7\u01bc\u01be\u01c6\u01c8\u01d1\u01d3\u01dc\u01de\u01e7\u01e9"
            + "\u01f1\u01f3\u01fd\u0206\u020a\u0218\u021a\u021e\u022d\u022f\u0233\u0240"
            + "\u0242\u0246\u024d\u025e\u0260\u0264\u0281\u0283\u0297\u0299\u02ad\u02af"
            + "\u02c4\u02c6\u02ca\u02d7\u02d9\u02dd\u02fe\u0300\u0304\u032f\u0331\u0335"
            + "\u033a\u033f\u0344\u035b\u035d\u0361\u0366\u0375\u0377\u037b\u0386\u0388"
            + "\u038c\u0390\u0392\u039a\u039f\u03a1\u03ac\u03ae\u03b2\u03bf\u03c8\u03d3"
            + "\u03d5\u03e5\u03e7\u03eb\u03f9\u03fb\u03ff\u040c\u040f\u0419\u041b\u041f"
            + "\u042a\u042d\u0430\u0459\u045c\u0463\u0472\u047e\u0482\u0488\u048d\u048f"
            + "\u0496\u0498\u049d\u049f\u04a4\u04a6";
    public static final ATN _ATN = ATNSimulator.deserialize(_serializedATN
            .toCharArray());
    static {
        _decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
    }
}