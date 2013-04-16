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
            RULE_refine_pom = 23, RULE_refine_stmt = 24, RULE_uses_stmt = 25,
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
            "refine_container_stmts", "refine_pom", "refine_stmt", "uses_stmt",
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
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(437);
                _errHandler.sync(this);
                _la = _input.LA(1);
                while (((((_la - 24)) & ~0x3f) == 0 && ((1L << (_la - 24)) & ((1L << (REFERENCE_KEYWORD - 24))
                        | (1L << (MUST_KEYWORD - 24))
                        | (1L << (MANDATORY_KEYWORD - 24))
                        | (1L << (DESCRIPTION_KEYWORD - 24)) | (1L << (CONFIG_KEYWORD - 24)))) != 0)) {
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
                    setState(439);
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
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(444);
                _errHandler.sync(this);
                _la = _input.LA(1);
                while (_la == REFERENCE_KEYWORD || _la == DESCRIPTION_KEYWORD) {
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
                    setState(446);
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
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(454);
                _errHandler.sync(this);
                _la = _input.LA(1);
                while (((((_la - 24)) & ~0x3f) == 0 && ((1L << (_la - 24)) & ((1L << (REFERENCE_KEYWORD - 24))
                        | (1L << (MANDATORY_KEYWORD - 24))
                        | (1L << (DESCRIPTION_KEYWORD - 24))
                        | (1L << (DEFAULT_KEYWORD - 24)) | (1L << (CONFIG_KEYWORD - 24)))) != 0)) {
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
                    setState(456);
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
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(465);
                _errHandler.sync(this);
                _la = _input.LA(1);
                while (((((_la - 24)) & ~0x3f) == 0 && ((1L << (_la - 24)) & ((1L << (REFERENCE_KEYWORD - 24))
                        | (1L << (MUST_KEYWORD - 24))
                        | (1L << (MIN_ELEMENTS_KEYWORD - 24))
                        | (1L << (MAX_ELEMENTS_KEYWORD - 24))
                        | (1L << (DESCRIPTION_KEYWORD - 24)) | (1L << (CONFIG_KEYWORD - 24)))) != 0)) {
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
                    setState(467);
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
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(476);
                _errHandler.sync(this);
                _la = _input.LA(1);
                while (((((_la - 24)) & ~0x3f) == 0 && ((1L << (_la - 24)) & ((1L << (REFERENCE_KEYWORD - 24))
                        | (1L << (MUST_KEYWORD - 24))
                        | (1L << (MIN_ELEMENTS_KEYWORD - 24))
                        | (1L << (MAX_ELEMENTS_KEYWORD - 24))
                        | (1L << (DESCRIPTION_KEYWORD - 24)) | (1L << (CONFIG_KEYWORD - 24)))) != 0)) {
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
                    setState(478);
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
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(487);
                _errHandler.sync(this);
                _la = _input.LA(1);
                while (((((_la - 24)) & ~0x3f) == 0 && ((1L << (_la - 24)) & ((1L << (REFERENCE_KEYWORD - 24))
                        | (1L << (MUST_KEYWORD - 24))
                        | (1L << (MANDATORY_KEYWORD - 24))
                        | (1L << (DESCRIPTION_KEYWORD - 24))
                        | (1L << (DEFAULT_KEYWORD - 24)) | (1L << (CONFIG_KEYWORD - 24)))) != 0)) {
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
                    setState(489);
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
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(497);
                _errHandler.sync(this);
                _la = _input.LA(1);
                while (((((_la - 24)) & ~0x3f) == 0 && ((1L << (_la - 24)) & ((1L << (REFERENCE_KEYWORD - 24))
                        | (1L << (PRESENCE_KEYWORD - 24))
                        | (1L << (MUST_KEYWORD - 24))
                        | (1L << (DESCRIPTION_KEYWORD - 24)) | (1L << (CONFIG_KEYWORD - 24)))) != 0)) {
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
                    setState(499);
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

    public static class Refine_pomContext extends ParserRuleContext {
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

        public Refine_pomContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_refine_pom;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).enterRefine_pom(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof YangParserListener)
                ((YangParserListener) listener).exitRefine_pom(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof YangParserVisitor)
                return ((YangParserVisitor<? extends T>) visitor)
                        .visitRefine_pom(this);
            else
                return visitor.visitChildren(this);
        }
    }

    public final Refine_pomContext refine_pom() throws RecognitionException {
        Refine_pomContext _localctx = new Refine_pomContext(_ctx, getState());
        enterRule(_localctx, 46, RULE_refine_pom);
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
        public TerminalNode RIGHT_BRACE() {
            return getToken(YangParser.RIGHT_BRACE, 0);
        }

        public Refine_pomContext refine_pom() {
            return getRuleContext(Refine_pomContext.class, 0);
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
            enterOuterAlt(_localctx, 1);
            {
                setState(509);
                match(REFINE_KEYWORD);
                setState(510);
                string();
                setState(516);
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
                        {
                            setState(513);
                            refine_pom();
                        }
                        setState(514);
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
                setState(518);
                match(USES_KEYWORD);
                setState(519);
                string();
                setState(536);
                switch (_input.LA(1)) {
                case SEMICOLON: {
                    setState(520);
                    match(SEMICOLON);
                }
                    break;
                case LEFT_BRACE: {
                    {
                        setState(521);
                        match(LEFT_BRACE);
                        setState(532);
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
                                setState(530);
                                switch (_input.LA(1)) {
                                case IDENTIFIER: {
                                    setState(522);
                                    identifier_stmt();
                                }
                                    break;
                                case WHEN_KEYWORD: {
                                    setState(523);
                                    when_stmt();
                                }
                                    break;
                                case IF_FEATURE_KEYWORD: {
                                    setState(524);
                                    if_feature_stmt();
                                }
                                    break;
                                case STATUS_KEYWORD: {
                                    setState(525);
                                    status_stmt();
                                }
                                    break;
                                case DESCRIPTION_KEYWORD: {
                                    setState(526);
                                    description_stmt();
                                }
                                    break;
                                case REFERENCE_KEYWORD: {
                                    setState(527);
                                    reference_stmt();
                                }
                                    break;
                                case REFINE_KEYWORD: {
                                    setState(528);
                                    refine_stmt();
                                }
                                    break;
                                case AUGMENT_KEYWORD: {
                                    setState(529);
                                    uses_augment_stmt();
                                }
                                    break;
                                default:
                                    throw new NoViableAltException(this);
                                }
                            }
                            setState(534);
                            _errHandler.sync(this);
                            _la = _input.LA(1);
                        }
                        setState(535);
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
                setState(538);
                match(ANYXML_KEYWORD);
                setState(539);
                string();
                setState(557);
                switch (_input.LA(1)) {
                case SEMICOLON: {
                    setState(540);
                    match(SEMICOLON);
                }
                    break;
                case LEFT_BRACE: {
                    {
                        setState(541);
                        match(LEFT_BRACE);
                        setState(553);
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
                                setState(551);
                                switch (_input.LA(1)) {
                                case IDENTIFIER: {
                                    setState(542);
                                    identifier_stmt();
                                }
                                    break;
                                case WHEN_KEYWORD: {
                                    setState(543);
                                    when_stmt();
                                }
                                    break;
                                case IF_FEATURE_KEYWORD: {
                                    setState(544);
                                    if_feature_stmt();
                                }
                                    break;
                                case MUST_KEYWORD: {
                                    setState(545);
                                    must_stmt();
                                }
                                    break;
                                case CONFIG_KEYWORD: {
                                    setState(546);
                                    config_stmt();
                                }
                                    break;
                                case MANDATORY_KEYWORD: {
                                    setState(547);
                                    mandatory_stmt();
                                }
                                    break;
                                case STATUS_KEYWORD: {
                                    setState(548);
                                    status_stmt();
                                }
                                    break;
                                case DESCRIPTION_KEYWORD: {
                                    setState(549);
                                    description_stmt();
                                }
                                    break;
                                case REFERENCE_KEYWORD: {
                                    setState(550);
                                    reference_stmt();
                                }
                                    break;
                                default:
                                    throw new NoViableAltException(this);
                                }
                            }
                            setState(555);
                            _errHandler.sync(this);
                            _la = _input.LA(1);
                        }
                        setState(556);
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
                setState(559);
                match(CASE_KEYWORD);
                setState(560);
                string();
                setState(576);
                switch (_input.LA(1)) {
                case SEMICOLON: {
                    setState(561);
                    match(SEMICOLON);
                }
                    break;
                case LEFT_BRACE: {
                    {
                        setState(562);
                        match(LEFT_BRACE);
                        setState(572);
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
                                setState(570);
                                switch (_input.LA(1)) {
                                case IDENTIFIER: {
                                    setState(563);
                                    identifier_stmt();
                                }
                                    break;
                                case WHEN_KEYWORD: {
                                    setState(564);
                                    when_stmt();
                                }
                                    break;
                                case IF_FEATURE_KEYWORD: {
                                    setState(565);
                                    if_feature_stmt();
                                }
                                    break;
                                case STATUS_KEYWORD: {
                                    setState(566);
                                    status_stmt();
                                }
                                    break;
                                case DESCRIPTION_KEYWORD: {
                                    setState(567);
                                    description_stmt();
                                }
                                    break;
                                case REFERENCE_KEYWORD: {
                                    setState(568);
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
                                    setState(569);
                                    data_def_stmt();
                                }
                                    break;
                                default:
                                    throw new NoViableAltException(this);
                                }
                            }
                            setState(574);
                            _errHandler.sync(this);
                            _la = _input.LA(1);
                        }
                        setState(575);
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
            setState(583);
            switch (_input.LA(1)) {
            case CONTAINER_KEYWORD:
                enterOuterAlt(_localctx, 1);
                {
                    setState(578);
                    container_stmt();
                }
                break;
            case LEAF_KEYWORD:
                enterOuterAlt(_localctx, 2);
                {
                    setState(579);
                    leaf_stmt();
                }
                break;
            case LEAF_LIST_KEYWORD:
                enterOuterAlt(_localctx, 3);
                {
                    setState(580);
                    leaf_list_stmt();
                }
                break;
            case LIST_KEYWORD:
                enterOuterAlt(_localctx, 4);
                {
                    setState(581);
                    list_stmt();
                }
                break;
            case ANYXML_KEYWORD:
                enterOuterAlt(_localctx, 5);
                {
                    setState(582);
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
                setState(585);
                match(CHOICE_KEYWORD);
                setState(586);
                string();
                setState(606);
                switch (_input.LA(1)) {
                case SEMICOLON: {
                    setState(587);
                    match(SEMICOLON);
                }
                    break;
                case LEFT_BRACE: {
                    {
                        setState(588);
                        match(LEFT_BRACE);
                        setState(602);
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
                                setState(600);
                                switch (_input.LA(1)) {
                                case IDENTIFIER: {
                                    setState(589);
                                    identifier_stmt();
                                }
                                    break;
                                case WHEN_KEYWORD: {
                                    setState(590);
                                    when_stmt();
                                }
                                    break;
                                case IF_FEATURE_KEYWORD: {
                                    setState(591);
                                    if_feature_stmt();
                                }
                                    break;
                                case DEFAULT_KEYWORD: {
                                    setState(592);
                                    default_stmt();
                                }
                                    break;
                                case CONFIG_KEYWORD: {
                                    setState(593);
                                    config_stmt();
                                }
                                    break;
                                case MANDATORY_KEYWORD: {
                                    setState(594);
                                    mandatory_stmt();
                                }
                                    break;
                                case STATUS_KEYWORD: {
                                    setState(595);
                                    status_stmt();
                                }
                                    break;
                                case DESCRIPTION_KEYWORD: {
                                    setState(596);
                                    description_stmt();
                                }
                                    break;
                                case REFERENCE_KEYWORD: {
                                    setState(597);
                                    reference_stmt();
                                }
                                    break;
                                case LIST_KEYWORD:
                                case LEAF_LIST_KEYWORD:
                                case LEAF_KEYWORD:
                                case CONTAINER_KEYWORD:
                                case ANYXML_KEYWORD: {
                                    setState(598);
                                    short_case_stmt();
                                }
                                    break;
                                case CASE_KEYWORD: {
                                    setState(599);
                                    case_stmt();
                                }
                                    break;
                                default:
                                    throw new NoViableAltException(this);
                                }
                            }
                            setState(604);
                            _errHandler.sync(this);
                            _la = _input.LA(1);
                        }
                        setState(605);
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
                setState(608);
                match(UNIQUE_KEYWORD);
                setState(609);
                string();
                setState(610);
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
                setState(612);
                match(KEY_KEYWORD);
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
                setState(616);
                match(LIST_KEYWORD);
                setState(617);
                string();
                setState(618);
                match(LEFT_BRACE);
                setState(635);
                _errHandler.sync(this);
                _la = _input.LA(1);
                do {
                    {
                        setState(635);
                        switch (_input.LA(1)) {
                        case IDENTIFIER: {
                            setState(619);
                            identifier_stmt();
                        }
                            break;
                        case WHEN_KEYWORD: {
                            setState(620);
                            when_stmt();
                        }
                            break;
                        case IF_FEATURE_KEYWORD: {
                            setState(621);
                            if_feature_stmt();
                        }
                            break;
                        case MUST_KEYWORD: {
                            setState(622);
                            must_stmt();
                        }
                            break;
                        case KEY_KEYWORD: {
                            setState(623);
                            key_stmt();
                        }
                            break;
                        case UNIQUE_KEYWORD: {
                            setState(624);
                            unique_stmt();
                        }
                            break;
                        case CONFIG_KEYWORD: {
                            setState(625);
                            config_stmt();
                        }
                            break;
                        case MIN_ELEMENTS_KEYWORD: {
                            setState(626);
                            min_elements_stmt();
                        }
                            break;
                        case MAX_ELEMENTS_KEYWORD: {
                            setState(627);
                            max_elements_stmt();
                        }
                            break;
                        case ORDERED_BY_KEYWORD: {
                            setState(628);
                            ordered_by_stmt();
                        }
                            break;
                        case STATUS_KEYWORD: {
                            setState(629);
                            status_stmt();
                        }
                            break;
                        case DESCRIPTION_KEYWORD: {
                            setState(630);
                            description_stmt();
                        }
                            break;
                        case REFERENCE_KEYWORD: {
                            setState(631);
                            reference_stmt();
                        }
                            break;
                        case TYPEDEF_KEYWORD: {
                            setState(632);
                            typedef_stmt();
                        }
                            break;
                        case GROUPING_KEYWORD: {
                            setState(633);
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
                            setState(634);
                            data_def_stmt();
                        }
                            break;
                        default:
                            throw new NoViableAltException(this);
                        }
                    }
                    setState(637);
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
                setState(639);
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
                setState(641);
                match(LEAF_LIST_KEYWORD);
                setState(642);
                string();
                setState(643);
                match(LEFT_BRACE);
                setState(659);
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
                        setState(657);
                        switch (_input.LA(1)) {
                        case IDENTIFIER: {
                            setState(644);
                            identifier_stmt();
                        }
                            break;
                        case WHEN_KEYWORD: {
                            setState(645);
                            when_stmt();
                        }
                            break;
                        case IF_FEATURE_KEYWORD: {
                            setState(646);
                            if_feature_stmt();
                        }
                            break;
                        case TYPE_KEYWORD: {
                            setState(647);
                            type_stmt();
                        }
                            break;
                        case UNITS_KEYWORD: {
                            setState(648);
                            units_stmt();
                        }
                            break;
                        case MUST_KEYWORD: {
                            setState(649);
                            must_stmt();
                        }
                            break;
                        case CONFIG_KEYWORD: {
                            setState(650);
                            config_stmt();
                        }
                            break;
                        case MIN_ELEMENTS_KEYWORD: {
                            setState(651);
                            min_elements_stmt();
                        }
                            break;
                        case MAX_ELEMENTS_KEYWORD: {
                            setState(652);
                            max_elements_stmt();
                        }
                            break;
                        case ORDERED_BY_KEYWORD: {
                            setState(653);
                            ordered_by_stmt();
                        }
                            break;
                        case STATUS_KEYWORD: {
                            setState(654);
                            status_stmt();
                        }
                            break;
                        case DESCRIPTION_KEYWORD: {
                            setState(655);
                            description_stmt();
                        }
                            break;
                        case REFERENCE_KEYWORD: {
                            setState(656);
                            reference_stmt();
                        }
                            break;
                        default:
                            throw new NoViableAltException(this);
                        }
                    }
                    setState(661);
                    _errHandler.sync(this);
                    _la = _input.LA(1);
                }
                setState(662);
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
                setState(664);
                match(LEAF_KEYWORD);
                setState(665);
                string();
                setState(666);
                match(LEFT_BRACE);
                setState(681);
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
                        setState(679);
                        switch (_input.LA(1)) {
                        case IDENTIFIER: {
                            setState(667);
                            identifier_stmt();
                        }
                            break;
                        case WHEN_KEYWORD: {
                            setState(668);
                            when_stmt();
                        }
                            break;
                        case IF_FEATURE_KEYWORD: {
                            setState(669);
                            if_feature_stmt();
                        }
                            break;
                        case TYPE_KEYWORD: {
                            setState(670);
                            type_stmt();
                        }
                            break;
                        case UNITS_KEYWORD: {
                            setState(671);
                            units_stmt();
                        }
                            break;
                        case MUST_KEYWORD: {
                            setState(672);
                            must_stmt();
                        }
                            break;
                        case DEFAULT_KEYWORD: {
                            setState(673);
                            default_stmt();
                        }
                            break;
                        case CONFIG_KEYWORD: {
                            setState(674);
                            config_stmt();
                        }
                            break;
                        case MANDATORY_KEYWORD: {
                            setState(675);
                            mandatory_stmt();
                        }
                            break;
                        case STATUS_KEYWORD: {
                            setState(676);
                            status_stmt();
                        }
                            break;
                        case DESCRIPTION_KEYWORD: {
                            setState(677);
                            description_stmt();
                        }
                            break;
                        case REFERENCE_KEYWORD: {
                            setState(678);
                            reference_stmt();
                        }
                            break;
                        default:
                            throw new NoViableAltException(this);
                        }
                    }
                    setState(683);
                    _errHandler.sync(this);
                    _la = _input.LA(1);
                }
                setState(684);
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
                setState(686);
                match(CONTAINER_KEYWORD);
                setState(687);
                string();
                setState(708);
                switch (_input.LA(1)) {
                case SEMICOLON: {
                    setState(688);
                    match(SEMICOLON);
                }
                    break;
                case LEFT_BRACE: {
                    {
                        setState(689);
                        match(LEFT_BRACE);
                        setState(704);
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
                                setState(702);
                                switch (_input.LA(1)) {
                                case IDENTIFIER: {
                                    setState(690);
                                    identifier_stmt();
                                }
                                    break;
                                case WHEN_KEYWORD: {
                                    setState(691);
                                    when_stmt();
                                }
                                    break;
                                case IF_FEATURE_KEYWORD: {
                                    setState(692);
                                    if_feature_stmt();
                                }
                                    break;
                                case MUST_KEYWORD: {
                                    setState(693);
                                    must_stmt();
                                }
                                    break;
                                case PRESENCE_KEYWORD: {
                                    setState(694);
                                    presence_stmt();
                                }
                                    break;
                                case CONFIG_KEYWORD: {
                                    setState(695);
                                    config_stmt();
                                }
                                    break;
                                case STATUS_KEYWORD: {
                                    setState(696);
                                    status_stmt();
                                }
                                    break;
                                case DESCRIPTION_KEYWORD: {
                                    setState(697);
                                    description_stmt();
                                }
                                    break;
                                case REFERENCE_KEYWORD: {
                                    setState(698);
                                    reference_stmt();
                                }
                                    break;
                                case TYPEDEF_KEYWORD: {
                                    setState(699);
                                    typedef_stmt();
                                }
                                    break;
                                case GROUPING_KEYWORD: {
                                    setState(700);
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
                                    setState(701);
                                    data_def_stmt();
                                }
                                    break;
                                default:
                                    throw new NoViableAltException(this);
                                }
                            }
                            setState(706);
                            _errHandler.sync(this);
                            _la = _input.LA(1);
                        }
                        setState(707);
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
                setState(710);
                match(GROUPING_KEYWORD);
                setState(711);
                string();
                setState(727);
                switch (_input.LA(1)) {
                case SEMICOLON: {
                    setState(712);
                    match(SEMICOLON);
                }
                    break;
                case LEFT_BRACE: {
                    {
                        setState(713);
                        match(LEFT_BRACE);
                        setState(723);
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
                                setState(721);
                                switch (_input.LA(1)) {
                                case IDENTIFIER: {
                                    setState(714);
                                    identifier_stmt();
                                }
                                    break;
                                case STATUS_KEYWORD: {
                                    setState(715);
                                    status_stmt();
                                }
                                    break;
                                case DESCRIPTION_KEYWORD: {
                                    setState(716);
                                    description_stmt();
                                }
                                    break;
                                case REFERENCE_KEYWORD: {
                                    setState(717);
                                    reference_stmt();
                                }
                                    break;
                                case TYPEDEF_KEYWORD: {
                                    setState(718);
                                    typedef_stmt();
                                }
                                    break;
                                case GROUPING_KEYWORD: {
                                    setState(719);
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
                                    setState(720);
                                    data_def_stmt();
                                }
                                    break;
                                default:
                                    throw new NoViableAltException(this);
                                }
                            }
                            setState(725);
                            _errHandler.sync(this);
                            _la = _input.LA(1);
                        }
                        setState(726);
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
                setState(729);
                match(VALUE_KEYWORD);
                setState(730);
                string();
                setState(731);
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
                setState(733);
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
                setState(735);
                match(MAX_ELEMENTS_KEYWORD);
                setState(736);
                max_value_arg();
                setState(737);
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
                setState(739);
                match(MIN_ELEMENTS_KEYWORD);
                setState(740);
                string();
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
                setState(743);
                match(ERROR_APP_TAG_KEYWORD);
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
                setState(747);
                match(ERROR_MESSAGE_KEYWORD);
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
                setState(751);
                match(MUST_KEYWORD);
                setState(752);
                string();
                setState(766);
                switch (_input.LA(1)) {
                case SEMICOLON: {
                    setState(753);
                    match(SEMICOLON);
                }
                    break;
                case LEFT_BRACE: {
                    {
                        setState(754);
                        match(LEFT_BRACE);
                        setState(762);
                        _errHandler.sync(this);
                        _la = _input.LA(1);
                        while (((((_la - 24)) & ~0x3f) == 0 && ((1L << (_la - 24)) & ((1L << (REFERENCE_KEYWORD - 24))
                                | (1L << (ERROR_MESSAGE_KEYWORD - 24))
                                | (1L << (ERROR_APP_TAG_KEYWORD - 24))
                                | (1L << (DESCRIPTION_KEYWORD - 24)) | (1L << (IDENTIFIER - 24)))) != 0)) {
                            {
                                setState(760);
                                switch (_input.LA(1)) {
                                case IDENTIFIER: {
                                    setState(755);
                                    identifier_stmt();
                                }
                                    break;
                                case ERROR_MESSAGE_KEYWORD: {
                                    setState(756);
                                    error_message_stmt();
                                }
                                    break;
                                case ERROR_APP_TAG_KEYWORD: {
                                    setState(757);
                                    error_app_tag_stmt();
                                }
                                    break;
                                case DESCRIPTION_KEYWORD: {
                                    setState(758);
                                    description_stmt();
                                }
                                    break;
                                case REFERENCE_KEYWORD: {
                                    setState(759);
                                    reference_stmt();
                                }
                                    break;
                                default:
                                    throw new NoViableAltException(this);
                                }
                            }
                            setState(764);
                            _errHandler.sync(this);
                            _la = _input.LA(1);
                        }
                        setState(765);
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
                setState(768);
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
                setState(770);
                match(ORDERED_BY_KEYWORD);
                setState(771);
                ordered_by_arg();
                setState(772);
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
                setState(774);
                match(PRESENCE_KEYWORD);
                setState(775);
                string();
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
                setState(778);
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
                setState(780);
                match(MANDATORY_KEYWORD);
                setState(781);
                mandatory_arg();
                setState(782);
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
                setState(784);
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
                setState(786);
                match(CONFIG_KEYWORD);
                setState(787);
                config_arg();
                setState(788);
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
                setState(790);
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
                setState(792);
                match(STATUS_KEYWORD);
                setState(793);
                status_arg();
                setState(794);
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
                setState(796);
                match(POSITION_KEYWORD);
                setState(797);
                string();
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
                setState(800);
                match(BIT_KEYWORD);
                setState(801);
                string();
                setState(815);
                switch (_input.LA(1)) {
                case SEMICOLON: {
                    setState(802);
                    match(SEMICOLON);
                }
                    break;
                case LEFT_BRACE: {
                    {
                        setState(803);
                        match(LEFT_BRACE);
                        setState(811);
                        _errHandler.sync(this);
                        _la = _input.LA(1);
                        while (((((_la - 18)) & ~0x3f) == 0 && ((1L << (_la - 18)) & ((1L << (STATUS_KEYWORD - 18))
                                | (1L << (REFERENCE_KEYWORD - 18))
                                | (1L << (POSITION_KEYWORD - 18))
                                | (1L << (DESCRIPTION_KEYWORD - 18)) | (1L << (IDENTIFIER - 18)))) != 0)) {
                            {
                                setState(809);
                                switch (_input.LA(1)) {
                                case IDENTIFIER: {
                                    setState(804);
                                    identifier_stmt();
                                }
                                    break;
                                case POSITION_KEYWORD: {
                                    setState(805);
                                    position_stmt();
                                }
                                    break;
                                case STATUS_KEYWORD: {
                                    setState(806);
                                    status_stmt();
                                }
                                    break;
                                case DESCRIPTION_KEYWORD: {
                                    setState(807);
                                    description_stmt();
                                }
                                    break;
                                case REFERENCE_KEYWORD: {
                                    setState(808);
                                    reference_stmt();
                                }
                                    break;
                                default:
                                    throw new NoViableAltException(this);
                                }
                            }
                            setState(813);
                            _errHandler.sync(this);
                            _la = _input.LA(1);
                        }
                        setState(814);
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
                setState(818);
                _errHandler.sync(this);
                _la = _input.LA(1);
                do {
                    {
                        {
                            setState(817);
                            bit_stmt();
                        }
                    }
                    setState(820);
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
                setState(823);
                _errHandler.sync(this);
                _la = _input.LA(1);
                do {
                    {
                        {
                            setState(822);
                            type_stmt();
                        }
                    }
                    setState(825);
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
                setState(827);
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
                setState(830);
                _la = _input.LA(1);
                if (_la == REQUIRE_INSTANCE_KEYWORD) {
                    {
                        setState(829);
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
                setState(832);
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
                setState(834);
                match(REQUIRE_INSTANCE_KEYWORD);
                setState(835);
                require_instance_arg();
                setState(836);
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
                setState(838);
                match(PATH_KEYWORD);
                setState(839);
                string();
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
                setState(842);
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
                setState(844);
                match(ENUM_KEYWORD);
                setState(845);
                string();
                setState(859);
                switch (_input.LA(1)) {
                case SEMICOLON: {
                    setState(846);
                    match(SEMICOLON);
                }
                    break;
                case LEFT_BRACE: {
                    {
                        setState(847);
                        match(LEFT_BRACE);
                        setState(855);
                        _errHandler.sync(this);
                        _la = _input.LA(1);
                        while (((((_la - 11)) & ~0x3f) == 0 && ((1L << (_la - 11)) & ((1L << (VALUE_KEYWORD - 11))
                                | (1L << (STATUS_KEYWORD - 11))
                                | (1L << (REFERENCE_KEYWORD - 11))
                                | (1L << (DESCRIPTION_KEYWORD - 11)) | (1L << (IDENTIFIER - 11)))) != 0)) {
                            {
                                setState(853);
                                switch (_input.LA(1)) {
                                case IDENTIFIER: {
                                    setState(848);
                                    identifier_stmt();
                                }
                                    break;
                                case VALUE_KEYWORD: {
                                    setState(849);
                                    value_stmt();
                                }
                                    break;
                                case STATUS_KEYWORD: {
                                    setState(850);
                                    status_stmt();
                                }
                                    break;
                                case DESCRIPTION_KEYWORD: {
                                    setState(851);
                                    description_stmt();
                                }
                                    break;
                                case REFERENCE_KEYWORD: {
                                    setState(852);
                                    reference_stmt();
                                }
                                    break;
                                default:
                                    throw new NoViableAltException(this);
                                }
                            }
                            setState(857);
                            _errHandler.sync(this);
                            _la = _input.LA(1);
                        }
                        setState(858);
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
                setState(862);
                _errHandler.sync(this);
                _la = _input.LA(1);
                do {
                    {
                        {
                            setState(861);
                            enum_stmt();
                        }
                    }
                    setState(864);
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
                setState(866);
                match(DEFAULT_KEYWORD);
                setState(867);
                string();
                setState(868);
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
                setState(870);
                match(PATTERN_KEYWORD);
                setState(871);
                string();
                setState(885);
                switch (_input.LA(1)) {
                case SEMICOLON: {
                    setState(872);
                    match(SEMICOLON);
                }
                    break;
                case LEFT_BRACE: {
                    {
                        setState(873);
                        match(LEFT_BRACE);
                        setState(881);
                        _errHandler.sync(this);
                        _la = _input.LA(1);
                        while (((((_la - 24)) & ~0x3f) == 0 && ((1L << (_la - 24)) & ((1L << (REFERENCE_KEYWORD - 24))
                                | (1L << (ERROR_MESSAGE_KEYWORD - 24))
                                | (1L << (ERROR_APP_TAG_KEYWORD - 24))
                                | (1L << (DESCRIPTION_KEYWORD - 24)) | (1L << (IDENTIFIER - 24)))) != 0)) {
                            {
                                setState(879);
                                switch (_input.LA(1)) {
                                case IDENTIFIER: {
                                    setState(874);
                                    identifier_stmt();
                                }
                                    break;
                                case ERROR_MESSAGE_KEYWORD: {
                                    setState(875);
                                    error_message_stmt();
                                }
                                    break;
                                case ERROR_APP_TAG_KEYWORD: {
                                    setState(876);
                                    error_app_tag_stmt();
                                }
                                    break;
                                case DESCRIPTION_KEYWORD: {
                                    setState(877);
                                    description_stmt();
                                }
                                    break;
                                case REFERENCE_KEYWORD: {
                                    setState(878);
                                    reference_stmt();
                                }
                                    break;
                                default:
                                    throw new NoViableAltException(this);
                                }
                            }
                            setState(883);
                            _errHandler.sync(this);
                            _la = _input.LA(1);
                        }
                        setState(884);
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
                setState(887);
                match(LENGTH_KEYWORD);
                setState(888);
                string();
                setState(902);
                switch (_input.LA(1)) {
                case SEMICOLON: {
                    setState(889);
                    match(SEMICOLON);
                }
                    break;
                case LEFT_BRACE: {
                    {
                        setState(890);
                        match(LEFT_BRACE);
                        setState(898);
                        _errHandler.sync(this);
                        _la = _input.LA(1);
                        while (((((_la - 24)) & ~0x3f) == 0 && ((1L << (_la - 24)) & ((1L << (REFERENCE_KEYWORD - 24))
                                | (1L << (ERROR_MESSAGE_KEYWORD - 24))
                                | (1L << (ERROR_APP_TAG_KEYWORD - 24))
                                | (1L << (DESCRIPTION_KEYWORD - 24)) | (1L << (IDENTIFIER - 24)))) != 0)) {
                            {
                                setState(896);
                                switch (_input.LA(1)) {
                                case IDENTIFIER: {
                                    setState(891);
                                    identifier_stmt();
                                }
                                    break;
                                case ERROR_MESSAGE_KEYWORD: {
                                    setState(892);
                                    error_message_stmt();
                                }
                                    break;
                                case ERROR_APP_TAG_KEYWORD: {
                                    setState(893);
                                    error_app_tag_stmt();
                                }
                                    break;
                                case DESCRIPTION_KEYWORD: {
                                    setState(894);
                                    description_stmt();
                                }
                                    break;
                                case REFERENCE_KEYWORD: {
                                    setState(895);
                                    reference_stmt();
                                }
                                    break;
                                default:
                                    throw new NoViableAltException(this);
                                }
                            }
                            setState(900);
                            _errHandler.sync(this);
                            _la = _input.LA(1);
                        }
                        setState(901);
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
                setState(908);
                _errHandler.sync(this);
                _la = _input.LA(1);
                while (_la == PATTERN_KEYWORD || _la == LENGTH_KEYWORD) {
                    {
                        setState(906);
                        switch (_input.LA(1)) {
                        case LENGTH_KEYWORD: {
                            setState(904);
                            length_stmt();
                        }
                            break;
                        case PATTERN_KEYWORD: {
                            setState(905);
                            pattern_stmt();
                        }
                            break;
                        default:
                            throw new NoViableAltException(this);
                        }
                    }
                    setState(910);
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
                setState(911);
                match(FRACTION_DIGITS_KEYWORD);
                setState(912);
                string();
                setState(913);
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
            setState(923);
            switch (getInterpreter().adaptivePredict(_input, 100, _ctx)) {
            case 1:
                enterOuterAlt(_localctx, 1);
                {
                    setState(916);
                    _la = _input.LA(1);
                    if (_la == RANGE_KEYWORD) {
                        {
                            setState(915);
                            numerical_restrictions();
                        }
                    }

                    setState(918);
                    fraction_digits_stmt();
                }
                break;

            case 2:
                enterOuterAlt(_localctx, 2);
                {
                    setState(919);
                    fraction_digits_stmt();
                    setState(921);
                    _la = _input.LA(1);
                    if (_la == RANGE_KEYWORD) {
                        {
                            setState(920);
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
                setState(925);
                match(RANGE_KEYWORD);
                setState(926);
                string();
                setState(940);
                switch (_input.LA(1)) {
                case SEMICOLON: {
                    setState(927);
                    match(SEMICOLON);
                }
                    break;
                case LEFT_BRACE: {
                    {
                        setState(928);
                        match(LEFT_BRACE);
                        setState(936);
                        _errHandler.sync(this);
                        _la = _input.LA(1);
                        while (((((_la - 24)) & ~0x3f) == 0 && ((1L << (_la - 24)) & ((1L << (REFERENCE_KEYWORD - 24))
                                | (1L << (ERROR_MESSAGE_KEYWORD - 24))
                                | (1L << (ERROR_APP_TAG_KEYWORD - 24))
                                | (1L << (DESCRIPTION_KEYWORD - 24)) | (1L << (IDENTIFIER - 24)))) != 0)) {
                            {
                                setState(934);
                                switch (_input.LA(1)) {
                                case IDENTIFIER: {
                                    setState(929);
                                    identifier_stmt();
                                }
                                    break;
                                case ERROR_MESSAGE_KEYWORD: {
                                    setState(930);
                                    error_message_stmt();
                                }
                                    break;
                                case ERROR_APP_TAG_KEYWORD: {
                                    setState(931);
                                    error_app_tag_stmt();
                                }
                                    break;
                                case DESCRIPTION_KEYWORD: {
                                    setState(932);
                                    description_stmt();
                                }
                                    break;
                                case REFERENCE_KEYWORD: {
                                    setState(933);
                                    reference_stmt();
                                }
                                    break;
                                default:
                                    throw new NoViableAltException(this);
                                }
                            }
                            setState(938);
                            _errHandler.sync(this);
                            _la = _input.LA(1);
                        }
                        setState(939);
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
                setState(942);
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
            setState(953);
            switch (getInterpreter().adaptivePredict(_input, 104, _ctx)) {
            case 1:
                enterOuterAlt(_localctx, 1);
                {
                    setState(944);
                    numerical_restrictions();
                }
                break;

            case 2:
                enterOuterAlt(_localctx, 2);
                {
                    setState(945);
                    decimal64_specification();
                }
                break;

            case 3:
                enterOuterAlt(_localctx, 3);
                {
                    setState(946);
                    string_restrictions();
                }
                break;

            case 4:
                enterOuterAlt(_localctx, 4);
                {
                    setState(947);
                    enum_specification();
                }
                break;

            case 5:
                enterOuterAlt(_localctx, 5);
                {
                    setState(948);
                    leafref_specification();
                }
                break;

            case 6:
                enterOuterAlt(_localctx, 6);
                {
                    setState(949);
                    identityref_specification();
                }
                break;

            case 7:
                enterOuterAlt(_localctx, 7);
                {
                    setState(950);
                    instance_identifier_specification();
                }
                break;

            case 8:
                enterOuterAlt(_localctx, 8);
                {
                    setState(951);
                    bits_specification();
                }
                break;

            case 9:
                enterOuterAlt(_localctx, 9);
                {
                    setState(952);
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
                setState(955);
                match(TYPE_KEYWORD);
                setState(956);
                string();
                setState(962);
                switch (_input.LA(1)) {
                case SEMICOLON: {
                    setState(957);
                    match(SEMICOLON);
                }
                    break;
                case LEFT_BRACE: {
                    {
                        setState(958);
                        match(LEFT_BRACE);
                        setState(959);
                        type_body_stmts();
                        setState(960);
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
                setState(964);
                match(TYPEDEF_KEYWORD);
                setState(965);
                string();
                setState(966);
                match(LEFT_BRACE);
                setState(973);
                _errHandler.sync(this);
                _la = _input.LA(1);
                do {
                    {
                        setState(973);
                        switch (_input.LA(1)) {
                        case TYPE_KEYWORD: {
                            setState(967);
                            type_stmt();
                        }
                            break;
                        case UNITS_KEYWORD: {
                            setState(968);
                            units_stmt();
                        }
                            break;
                        case DEFAULT_KEYWORD: {
                            setState(969);
                            default_stmt();
                        }
                            break;
                        case STATUS_KEYWORD: {
                            setState(970);
                            status_stmt();
                        }
                            break;
                        case DESCRIPTION_KEYWORD: {
                            setState(971);
                            description_stmt();
                        }
                            break;
                        case REFERENCE_KEYWORD: {
                            setState(972);
                            reference_stmt();
                        }
                            break;
                        default:
                            throw new NoViableAltException(this);
                        }
                    }
                    setState(975);
                    _errHandler.sync(this);
                    _la = _input.LA(1);
                } while ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << UNITS_KEYWORD)
                        | (1L << TYPE_KEYWORD)
                        | (1L << STATUS_KEYWORD)
                        | (1L << REFERENCE_KEYWORD)
                        | (1L << DESCRIPTION_KEYWORD) | (1L << DEFAULT_KEYWORD))) != 0));
                setState(977);
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
                setState(979);
                match(IF_FEATURE_KEYWORD);
                setState(980);
                string();
                setState(981);
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
                setState(983);
                match(FEATURE_KEYWORD);
                setState(984);
                string();
                setState(997);
                switch (_input.LA(1)) {
                case SEMICOLON: {
                    setState(985);
                    match(SEMICOLON);
                }
                    break;
                case LEFT_BRACE: {
                    {
                        setState(986);
                        match(LEFT_BRACE);
                        setState(993);
                        _errHandler.sync(this);
                        _la = _input.LA(1);
                        while ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << STATUS_KEYWORD)
                                | (1L << REFERENCE_KEYWORD)
                                | (1L << IF_FEATURE_KEYWORD) | (1L << DESCRIPTION_KEYWORD))) != 0)) {
                            {
                                setState(991);
                                switch (_input.LA(1)) {
                                case IF_FEATURE_KEYWORD: {
                                    setState(987);
                                    if_feature_stmt();
                                }
                                    break;
                                case STATUS_KEYWORD: {
                                    setState(988);
                                    status_stmt();
                                }
                                    break;
                                case DESCRIPTION_KEYWORD: {
                                    setState(989);
                                    description_stmt();
                                }
                                    break;
                                case REFERENCE_KEYWORD: {
                                    setState(990);
                                    reference_stmt();
                                }
                                    break;
                                default:
                                    throw new NoViableAltException(this);
                                }
                            }
                            setState(995);
                            _errHandler.sync(this);
                            _la = _input.LA(1);
                        }
                        setState(996);
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
                setState(999);
                match(BASE_KEYWORD);
                setState(1000);
                string();
                setState(1001);
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
                setState(1003);
                match(IDENTITY_KEYWORD);
                setState(1004);
                string();
                setState(1017);
                switch (_input.LA(1)) {
                case SEMICOLON: {
                    setState(1005);
                    match(SEMICOLON);
                }
                    break;
                case LEFT_BRACE: {
                    {
                        setState(1006);
                        match(LEFT_BRACE);
                        setState(1013);
                        _errHandler.sync(this);
                        _la = _input.LA(1);
                        while (((((_la - 18)) & ~0x3f) == 0 && ((1L << (_la - 18)) & ((1L << (STATUS_KEYWORD - 18))
                                | (1L << (REFERENCE_KEYWORD - 18))
                                | (1L << (DESCRIPTION_KEYWORD - 18)) | (1L << (BASE_KEYWORD - 18)))) != 0)) {
                            {
                                setState(1011);
                                switch (_input.LA(1)) {
                                case BASE_KEYWORD: {
                                    setState(1007);
                                    base_stmt();
                                }
                                    break;
                                case STATUS_KEYWORD: {
                                    setState(1008);
                                    status_stmt();
                                }
                                    break;
                                case DESCRIPTION_KEYWORD: {
                                    setState(1009);
                                    description_stmt();
                                }
                                    break;
                                case REFERENCE_KEYWORD: {
                                    setState(1010);
                                    reference_stmt();
                                }
                                    break;
                                default:
                                    throw new NoViableAltException(this);
                                }
                            }
                            setState(1015);
                            _errHandler.sync(this);
                            _la = _input.LA(1);
                        }
                        setState(1016);
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
                setState(1019);
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
                setState(1021);
                match(YIN_ELEMENT_KEYWORD);
                setState(1022);
                yin_element_arg();
                setState(1023);
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
                setState(1025);
                match(ARGUMENT_KEYWORD);
                setState(1026);
                string();
                setState(1033);
                switch (_input.LA(1)) {
                case SEMICOLON: {
                    setState(1027);
                    match(SEMICOLON);
                }
                    break;
                case LEFT_BRACE: {
                    {
                        setState(1028);
                        match(LEFT_BRACE);
                        setState(1030);
                        _la = _input.LA(1);
                        if (_la == YIN_ELEMENT_KEYWORD) {
                            {
                                setState(1029);
                                yin_element_stmt();
                            }
                        }

                        setState(1032);
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
                setState(1035);
                match(EXTENSION_KEYWORD);
                setState(1036);
                string();
                setState(1049);
                switch (_input.LA(1)) {
                case SEMICOLON: {
                    setState(1037);
                    match(SEMICOLON);
                }
                    break;
                case LEFT_BRACE: {
                    {
                        setState(1038);
                        match(LEFT_BRACE);
                        setState(1045);
                        _errHandler.sync(this);
                        _la = _input.LA(1);
                        while (((((_la - 18)) & ~0x3f) == 0 && ((1L << (_la - 18)) & ((1L << (STATUS_KEYWORD - 18))
                                | (1L << (REFERENCE_KEYWORD - 18))
                                | (1L << (DESCRIPTION_KEYWORD - 18)) | (1L << (ARGUMENT_KEYWORD - 18)))) != 0)) {
                            {
                                setState(1043);
                                switch (_input.LA(1)) {
                                case ARGUMENT_KEYWORD: {
                                    setState(1039);
                                    argument_stmt();
                                }
                                    break;
                                case STATUS_KEYWORD: {
                                    setState(1040);
                                    status_stmt();
                                }
                                    break;
                                case DESCRIPTION_KEYWORD: {
                                    setState(1041);
                                    description_stmt();
                                }
                                    break;
                                case REFERENCE_KEYWORD: {
                                    setState(1042);
                                    reference_stmt();
                                }
                                    break;
                                default:
                                    throw new NoViableAltException(this);
                                }
                            }
                            setState(1047);
                            _errHandler.sync(this);
                            _la = _input.LA(1);
                        }
                        setState(1048);
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
                setState(1051);
                match(REVISION_DATE_KEYWORD);
                setState(1052);
                string();
                setState(1053);
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
                setState(1055);
                match(REVISION_KEYWORD);
                setState(1056);
                string();
                setState(1066);
                switch (_input.LA(1)) {
                case SEMICOLON: {
                    setState(1057);
                    match(SEMICOLON);
                }
                    break;
                case LEFT_BRACE: {
                    {
                        setState(1058);
                        match(LEFT_BRACE);
                        setState(1060);
                        _la = _input.LA(1);
                        if (_la == DESCRIPTION_KEYWORD) {
                            {
                                setState(1059);
                                description_stmt();
                            }
                        }

                        setState(1063);
                        _la = _input.LA(1);
                        if (_la == REFERENCE_KEYWORD) {
                            {
                                setState(1062);
                                reference_stmt();
                            }
                        }

                        setState(1065);
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
                setState(1068);
                match(UNITS_KEYWORD);
                setState(1069);
                string();
                setState(1070);
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
                setState(1072);
                match(REFERENCE_KEYWORD);
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
                setState(1076);
                match(DESCRIPTION_KEYWORD);
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
                setState(1080);
                match(CONTACT_KEYWORD);
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
                setState(1084);
                match(ORGANIZATION_KEYWORD);
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
                setState(1088);
                match(BELONGS_TO_KEYWORD);
                setState(1089);
                string();
                setState(1090);
                match(LEFT_BRACE);
                setState(1091);
                prefix_stmt();
                setState(1092);
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
                setState(1094);
                match(PREFIX_KEYWORD);
                setState(1095);
                string();
                setState(1096);
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
                setState(1098);
                match(NAMESPACE_KEYWORD);
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
                setState(1102);
                match(INCLUDE_KEYWORD);
                setState(1103);
                string();
                setState(1110);
                switch (_input.LA(1)) {
                case SEMICOLON: {
                    setState(1104);
                    match(SEMICOLON);
                }
                    break;
                case LEFT_BRACE: {
                    {
                        setState(1105);
                        match(LEFT_BRACE);
                        setState(1107);
                        _la = _input.LA(1);
                        if (_la == REVISION_DATE_KEYWORD) {
                            {
                                setState(1106);
                                revision_date_stmt();
                            }
                        }

                        setState(1109);
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
                setState(1112);
                match(IMPORT_KEYWORD);
                setState(1113);
                string();
                setState(1114);
                match(LEFT_BRACE);
                setState(1115);
                prefix_stmt();
                setState(1117);
                _la = _input.LA(1);
                if (_la == REVISION_DATE_KEYWORD) {
                    {
                        setState(1116);
                        revision_date_stmt();
                    }
                }

                setState(1119);
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
                setState(1121);
                match(YANG_VERSION_KEYWORD);
                setState(1122);
                string();
                setState(1123);
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
            setState(1132);
            switch (_input.LA(1)) {
            case CONTAINER_KEYWORD:
                enterOuterAlt(_localctx, 1);
                {
                    setState(1125);
                    container_stmt();
                }
                break;
            case LEAF_KEYWORD:
                enterOuterAlt(_localctx, 2);
                {
                    setState(1126);
                    leaf_stmt();
                }
                break;
            case LEAF_LIST_KEYWORD:
                enterOuterAlt(_localctx, 3);
                {
                    setState(1127);
                    leaf_list_stmt();
                }
                break;
            case LIST_KEYWORD:
                enterOuterAlt(_localctx, 4);
                {
                    setState(1128);
                    list_stmt();
                }
                break;
            case CHOICE_KEYWORD:
                enterOuterAlt(_localctx, 5);
                {
                    setState(1129);
                    choice_stmt();
                }
                break;
            case ANYXML_KEYWORD:
                enterOuterAlt(_localctx, 6);
                {
                    setState(1130);
                    anyxml_stmt();
                }
                break;
            case USES_KEYWORD:
                enterOuterAlt(_localctx, 7);
                {
                    setState(1131);
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
                setState(1148);
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
                            setState(1144);
                            switch (_input.LA(1)) {
                            case EXTENSION_KEYWORD: {
                                setState(1134);
                                extension_stmt();
                            }
                                break;
                            case FEATURE_KEYWORD: {
                                setState(1135);
                                feature_stmt();
                            }
                                break;
                            case IDENTITY_KEYWORD: {
                                setState(1136);
                                identity_stmt();
                            }
                                break;
                            case TYPEDEF_KEYWORD: {
                                setState(1137);
                                typedef_stmt();
                            }
                                break;
                            case GROUPING_KEYWORD: {
                                setState(1138);
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
                                setState(1139);
                                data_def_stmt();
                            }
                                break;
                            case AUGMENT_KEYWORD: {
                                setState(1140);
                                augment_stmt();
                            }
                                break;
                            case RPC_KEYWORD: {
                                setState(1141);
                                rpc_stmt();
                            }
                                break;
                            case NOTIFICATION_KEYWORD: {
                                setState(1142);
                                notification_stmt();
                            }
                                break;
                            case DEVIATION_KEYWORD: {
                                setState(1143);
                                deviation_stmt();
                            }
                                break;
                            default:
                                throw new NoViableAltException(this);
                            }
                        }
                    }
                    setState(1150);
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
                setState(1154);
                _errHandler.sync(this);
                _la = _input.LA(1);
                while (_la == REVISION_KEYWORD) {
                    {
                        {
                            setState(1151);
                            revision_stmt();
                        }
                    }
                    setState(1156);
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
                setState(1161);
                _errHandler.sync(this);
                _la = _input.LA(1);
                while (_la == INCLUDE_KEYWORD || _la == IMPORT_KEYWORD) {
                    {
                        setState(1159);
                        switch (_input.LA(1)) {
                        case IMPORT_KEYWORD: {
                            setState(1157);
                            import_stmt();
                        }
                            break;
                        case INCLUDE_KEYWORD: {
                            setState(1158);
                            include_stmt();
                        }
                            break;
                        default:
                            throw new NoViableAltException(this);
                        }
                    }
                    setState(1163);
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
                setState(1170);
                _errHandler.sync(this);
                _la = _input.LA(1);
                while ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << REFERENCE_KEYWORD)
                        | (1L << ORGANIZATION_KEYWORD)
                        | (1L << DESCRIPTION_KEYWORD) | (1L << CONTACT_KEYWORD))) != 0)) {
                    {
                        setState(1168);
                        switch (_input.LA(1)) {
                        case ORGANIZATION_KEYWORD: {
                            setState(1164);
                            organization_stmt();
                        }
                            break;
                        case CONTACT_KEYWORD: {
                            setState(1165);
                            contact_stmt();
                        }
                            break;
                        case DESCRIPTION_KEYWORD: {
                            setState(1166);
                            description_stmt();
                        }
                            break;
                        case REFERENCE_KEYWORD: {
                            setState(1167);
                            reference_stmt();
                        }
                            break;
                        default:
                            throw new NoViableAltException(this);
                        }
                    }
                    setState(1172);
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
                setState(1175);
                _errHandler.sync(this);
                _la = _input.LA(1);
                do {
                    {
                        setState(1175);
                        switch (_input.LA(1)) {
                        case YANG_VERSION_KEYWORD: {
                            setState(1173);
                            yang_version_stmt();
                        }
                            break;
                        case BELONGS_TO_KEYWORD: {
                            setState(1174);
                            belongs_to_stmt();
                        }
                            break;
                        default:
                            throw new NoViableAltException(this);
                        }
                    }
                    setState(1177);
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
                setState(1182);
                _errHandler.sync(this);
                _la = _input.LA(1);
                do {
                    {
                        setState(1182);
                        switch (_input.LA(1)) {
                        case YANG_VERSION_KEYWORD: {
                            setState(1179);
                            yang_version_stmt();
                        }
                            break;
                        case NAMESPACE_KEYWORD: {
                            setState(1180);
                            namespace_stmt();
                        }
                            break;
                        case PREFIX_KEYWORD: {
                            setState(1181);
                            prefix_stmt();
                        }
                            break;
                        default:
                            throw new NoViableAltException(this);
                        }
                    }
                    setState(1184);
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
                setState(1186);
                match(SUBMODULE_KEYWORD);
                setState(1187);
                string();
                setState(1188);
                match(LEFT_BRACE);
                setState(1189);
                submodule_header_stmts();
                setState(1190);
                linkage_stmts();
                setState(1191);
                meta_stmts();
                setState(1192);
                revision_stmts();
                setState(1193);
                body_stmts();
                setState(1194);
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
                setState(1196);
                match(MODULE_KEYWORD);
                setState(1197);
                string();
                setState(1198);
                match(LEFT_BRACE);
                setState(1199);
                module_header_stmts();
                setState(1200);
                linkage_stmts();
                setState(1201);
                meta_stmts();
                setState(1202);
                revision_stmts();
                setState(1203);
                body_stmts();
                setState(1204);
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

    public static final String _serializedATN = "\2\3N\u04b9\4\2\t\2\4\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4"
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
            + "\3\31\3\31\3\31\3\31\3\31\5\31\u01fe\n\31\3\32\3\32\3\32\3\32\3\32\3\32"
            + "\3\32\5\32\u0207\n\32\3\33\3\33\3\33\3\33\3\33\3\33\3\33\3\33\3\33\3\33"
            + "\3\33\3\33\7\33\u0215\n\33\f\33\16\33\u0218\13\33\3\33\5\33\u021b\n\33"
            + "\3\34\3\34\3\34\3\34\3\34\3\34\3\34\3\34\3\34\3\34\3\34\3\34\3\34\7\34"
            + "\u022a\n\34\f\34\16\34\u022d\13\34\3\34\5\34\u0230\n\34\3\35\3\35\3\35"
            + "\3\35\3\35\3\35\3\35\3\35\3\35\3\35\3\35\7\35\u023d\n\35\f\35\16\35\u0240"
            + "\13\35\3\35\5\35\u0243\n\35\3\36\3\36\3\36\3\36\3\36\5\36\u024a\n\36\3"
            + "\37\3\37\3\37\3\37\3\37\3\37\3\37\3\37\3\37\3\37\3\37\3\37\3\37\3\37\3"
            + "\37\7\37\u025b\n\37\f\37\16\37\u025e\13\37\3\37\5\37\u0261\n\37\3 \3 "
            + "\3 \3 \3!\3!\3!\3!\3\"\3\"\3\"\3\"\3\"\3\"\3\"\3\"\3\"\3\"\3\"\3\"\3\""
            + "\3\"\3\"\3\"\3\"\3\"\3\"\6\"\u027e\n\"\r\"\16\"\u027f\3\"\3\"\3#\3#\3"
            + "#\3#\3#\3#\3#\3#\3#\3#\3#\3#\3#\3#\3#\3#\7#\u0294\n#\f#\16#\u0297\13#"
            + "\3#\3#\3$\3$\3$\3$\3$\3$\3$\3$\3$\3$\3$\3$\3$\3$\3$\7$\u02aa\n$\f$\16"
            + "$\u02ad\13$\3$\3$\3%\3%\3%\3%\3%\3%\3%\3%\3%\3%\3%\3%\3%\3%\3%\3%\7%\u02c1"
            + "\n%\f%\16%\u02c4\13%\3%\5%\u02c7\n%\3&\3&\3&\3&\3&\3&\3&\3&\3&\3&\3&\7"
            + "&\u02d4\n&\f&\16&\u02d7\13&\3&\5&\u02da\n&\3\'\3\'\3\'\3\'\3(\3(\3)\3"
            + ")\3)\3)\3*\3*\3*\3*\3+\3+\3+\3+\3,\3,\3,\3,\3-\3-\3-\3-\3-\3-\3-\3-\3"
            + "-\7-\u02fb\n-\f-\16-\u02fe\13-\3-\5-\u0301\n-\3.\3.\3/\3/\3/\3/\3\60\3"
            + "\60\3\60\3\60\3\61\3\61\3\62\3\62\3\62\3\62\3\63\3\63\3\64\3\64\3\64\3"
            + "\64\3\65\3\65\3\66\3\66\3\66\3\66\3\67\3\67\3\67\3\67\38\38\38\38\38\3"
            + "8\38\38\38\78\u032c\n8\f8\168\u032f\138\38\58\u0332\n8\39\69\u0335\n9"
            + "\r9\169\u0336\3:\6:\u033a\n:\r:\16:\u033b\3;\3;\3<\5<\u0341\n<\3=\3=\3"
            + ">\3>\3>\3>\3?\3?\3?\3?\3@\3@\3A\3A\3A\3A\3A\3A\3A\3A\3A\7A\u0358\nA\f"
            + "A\16A\u035b\13A\3A\5A\u035e\nA\3B\6B\u0361\nB\rB\16B\u0362\3C\3C\3C\3"
            + "C\3D\3D\3D\3D\3D\3D\3D\3D\3D\7D\u0372\nD\fD\16D\u0375\13D\3D\5D\u0378"
            + "\nD\3E\3E\3E\3E\3E\3E\3E\3E\3E\7E\u0383\nE\fE\16E\u0386\13E\3E\5E\u0389"
            + "\nE\3F\3F\7F\u038d\nF\fF\16F\u0390\13F\3G\3G\3G\3G\3H\5H\u0397\nH\3H\3"
            + "H\3H\5H\u039c\nH\5H\u039e\nH\3I\3I\3I\3I\3I\3I\3I\3I\3I\7I\u03a9\nI\f"
            + "I\16I\u03ac\13I\3I\5I\u03af\nI\3J\3J\3K\3K\3K\3K\3K\3K\3K\3K\3K\5K\u03bc"
            + "\nK\3L\3L\3L\3L\3L\3L\3L\5L\u03c5\nL\3M\3M\3M\3M\3M\3M\3M\3M\3M\6M\u03d0"
            + "\nM\rM\16M\u03d1\3M\3M\3N\3N\3N\3N\3O\3O\3O\3O\3O\3O\3O\3O\7O\u03e2\n"
            + "O\fO\16O\u03e5\13O\3O\5O\u03e8\nO\3P\3P\3P\3P\3Q\3Q\3Q\3Q\3Q\3Q\3Q\3Q"
            + "\7Q\u03f6\nQ\fQ\16Q\u03f9\13Q\3Q\5Q\u03fc\nQ\3R\3R\3S\3S\3S\3S\3T\3T\3"
            + "T\3T\3T\5T\u0409\nT\3T\5T\u040c\nT\3U\3U\3U\3U\3U\3U\3U\3U\7U\u0416\n"
            + "U\fU\16U\u0419\13U\3U\5U\u041c\nU\3V\3V\3V\3V\3W\3W\3W\3W\3W\5W\u0427"
            + "\nW\3W\5W\u042a\nW\3W\5W\u042d\nW\3X\3X\3X\3X\3Y\3Y\3Y\3Y\3Z\3Z\3Z\3Z"
            + "\3[\3[\3[\3[\3\\\3\\\3\\\3\\\3]\3]\3]\3]\3]\3]\3^\3^\3^\3^\3_\3_\3_\3"
            + "_\3`\3`\3`\3`\3`\5`\u0456\n`\3`\5`\u0459\n`\3a\3a\3a\3a\3a\5a\u0460\n"
            + "a\3a\3a\3b\3b\3b\3b\3c\3c\3c\3c\3c\3c\3c\5c\u046f\nc\3d\3d\3d\3d\3d\3"
            + "d\3d\3d\3d\3d\5d\u047b\nd\7d\u047d\nd\fd\16d\u0480\13d\3e\7e\u0483\ne"
            + "\fe\16e\u0486\13e\3f\3f\7f\u048a\nf\ff\16f\u048d\13f\3g\3g\3g\3g\7g\u0493"
            + "\ng\fg\16g\u0496\13g\3h\3h\6h\u049a\nh\rh\16h\u049b\3i\3i\3i\6i\u04a1"
            + "\ni\ri\16i\u04a2\3j\3j\3j\3j\3j\3j\3j\3j\3j\3j\3k\3k\3k\3k\3k\3k\3k\3"
            + "k\3k\3k\3k\2l\2\4\6\b\n\f\16\20\22\24\26\30\32\34\36 \"$&(*,.\60\62\64"
            + "\668:<>@BDFHJLNPRTVXZ\\^`bdfhjlnprtvxz|~\u0080\u0082\u0084\u0086\u0088"
            + "\u008a\u008c\u008e\u0090\u0092\u0094\u0096\u0098\u009a\u009c\u009e\u00a0"
            + "\u00a2\u00a4\u00a6\u00a8\u00aa\u00ac\u00ae\u00b0\u00b2\u00b4\u00b6\u00b8"
            + "\u00ba\u00bc\u00be\u00c0\u00c2\u00c4\u00c6\u00c8\u00ca\u00cc\u00ce\u00d0"
            + "\u00d2\u00d4\2\2\u059e\2\u00d8\3\2\2\2\4\u00da\3\2\2\2\6\u00e2\3\2\2\2"
            + "\b\u00f1\3\2\2\2\n\u00f3\3\2\2\2\f\u0106\3\2\2\2\16\u0116\3\2\2\2\20\u012a"
            + "\3\2\2\2\22\u0134\3\2\2\2\24\u0143\3\2\2\2\26\u0156\3\2\2\2\30\u0161\3"
            + "\2\2\2\32\u016c\3\2\2\2\34\u0180\3\2\2\2\36\u018e\3\2\2\2 \u019f\3\2\2"
            + "\2\"\u01b7\3\2\2\2$\u01be\3\2\2\2&\u01c8\3\2\2\2(\u01d3\3\2\2\2*\u01de"
            + "\3\2\2\2,\u01e9\3\2\2\2.\u01f3\3\2\2\2\60\u01fd\3\2\2\2\62\u01ff\3\2\2"
            + "\2\64\u0208\3\2\2\2\66\u021c\3\2\2\28\u0231\3\2\2\2:\u0249\3\2\2\2<\u024b"
            + "\3\2\2\2>\u0262\3\2\2\2@\u0266\3\2\2\2B\u026a\3\2\2\2D\u0283\3\2\2\2F"
            + "\u029a\3\2\2\2H\u02b0\3\2\2\2J\u02c8\3\2\2\2L\u02db\3\2\2\2N\u02df\3\2"
            + "\2\2P\u02e1\3\2\2\2R\u02e5\3\2\2\2T\u02e9\3\2\2\2V\u02ed\3\2\2\2X\u02f1"
            + "\3\2\2\2Z\u0302\3\2\2\2\\\u0304\3\2\2\2^\u0308\3\2\2\2`\u030c\3\2\2\2"
            + "b\u030e\3\2\2\2d\u0312\3\2\2\2f\u0314\3\2\2\2h\u0318\3\2\2\2j\u031a\3"
            + "\2\2\2l\u031e\3\2\2\2n\u0322\3\2\2\2p\u0334\3\2\2\2r\u0339\3\2\2\2t\u033d"
            + "\3\2\2\2v\u0340\3\2\2\2x\u0342\3\2\2\2z\u0344\3\2\2\2|\u0348\3\2\2\2~"
            + "\u034c\3\2\2\2\u0080\u034e\3\2\2\2\u0082\u0360\3\2\2\2\u0084\u0364\3\2"
            + "\2\2\u0086\u0368\3\2\2\2\u0088\u0379\3\2\2\2\u008a\u038e\3\2\2\2\u008c"
            + "\u0391\3\2\2\2\u008e\u039d\3\2\2\2\u0090\u039f\3\2\2\2\u0092\u03b0\3\2"
            + "\2\2\u0094\u03bb\3\2\2\2\u0096\u03bd\3\2\2\2\u0098\u03c6\3\2\2\2\u009a"
            + "\u03d5\3\2\2\2\u009c\u03d9\3\2\2\2\u009e\u03e9\3\2\2\2\u00a0\u03ed\3\2"
            + "\2\2\u00a2\u03fd\3\2\2\2\u00a4\u03ff\3\2\2\2\u00a6\u0403\3\2\2\2\u00a8"
            + "\u040d\3\2\2\2\u00aa\u041d\3\2\2\2\u00ac\u0421\3\2\2\2\u00ae\u042e\3\2"
            + "\2\2\u00b0\u0432\3\2\2\2\u00b2\u0436\3\2\2\2\u00b4\u043a\3\2\2\2\u00b6"
            + "\u043e\3\2\2\2\u00b8\u0442\3\2\2\2\u00ba\u0448\3\2\2\2\u00bc\u044c\3\2"
            + "\2\2\u00be\u0450\3\2\2\2\u00c0\u045a\3\2\2\2\u00c2\u0463\3\2\2\2\u00c4"
            + "\u046e\3\2\2\2\u00c6\u047e\3\2\2\2\u00c8\u0484\3\2\2\2\u00ca\u048b\3\2"
            + "\2\2\u00cc\u0494\3\2\2\2\u00ce\u0499\3\2\2\2\u00d0\u04a0\3\2\2\2\u00d2"
            + "\u04a4\3\2\2\2\u00d4\u04ae\3\2\2\2\u00d6\u00d9\5\u00d4k\2\u00d7\u00d9"
            + "\5\u00d2j\2\u00d8\u00d6\3\2\2\2\u00d8\u00d7\3\2\2\2\u00d9\3\3\2\2\2\u00da"
            + "\u00df\7L\2\2\u00db\u00dc\7\6\2\2\u00dc\u00de\7L\2\2\u00dd\u00db\3\2\2"
            + "\2\u00de\u00e1\3\2\2\2\u00df\u00dd\3\2\2\2\u00df\u00e0\3\2\2\2\u00e0\5"
            + "\3\2\2\2\u00e1\u00df\3\2\2\2\u00e2\u00e4\7K\2\2\u00e3\u00e5\5\4\3\2\u00e4"
            + "\u00e3\3\2\2\2\u00e4\u00e5\3\2\2\2\u00e5\u00e6\3\2\2\2\u00e6\u00e7\5\b"
            + "\5\2\u00e7\7\3\2\2\2\u00e8\u00ea\7\3\2\2\u00e9\u00eb\5\6\4\2\u00ea\u00e9"
            + "\3\2\2\2\u00ea\u00eb\3\2\2\2\u00eb\u00f2\3\2\2\2\u00ec\u00ee\7\4\2\2\u00ed"
            + "\u00ef\5\6\4\2\u00ee\u00ed\3\2\2\2\u00ee\u00ef\3\2\2\2\u00ef\u00f0\3\2"
            + "\2\2\u00f0\u00f2\7\5\2\2\u00f1\u00e8\3\2\2\2\u00f1\u00ec\3\2\2\2\u00f2"
            + "\t\3\2\2\2\u00f3\u00f4\78\2\2\u00f4\u0104\5\4\3\2\u00f5\u0105\7\3\2\2"
            + "\u00f6\u0100\7\4\2\2\u00f7\u00ff\5\u0096L\2\u00f8\u00ff\5\u00aeX\2\u00f9"
            + "\u00ff\5\u0084C\2\u00fa\u00ff\5f\64\2\u00fb\u00ff\5b\62\2\u00fc\u00ff"
            + "\5R*\2\u00fd\u00ff\5P)\2\u00fe\u00f7\3\2\2\2\u00fe\u00f8\3\2\2\2\u00fe"
            + "\u00f9\3\2\2\2\u00fe\u00fa\3\2\2\2\u00fe\u00fb\3\2\2\2\u00fe\u00fc\3\2"
            + "\2\2\u00fe\u00fd\3\2\2\2\u00ff\u0102\3\2\2\2\u0100\u00fe\3\2\2\2\u0100"
            + "\u0101\3\2\2\2\u0101\u0103\3\2\2\2\u0102\u0100\3\2\2\2\u0103\u0105\7\5"
            + "\2\2\u0104\u00f5\3\2\2\2\u0104\u00f6\3\2\2\2\u0105\13\3\2\2\2\u0106\u0107"
            + "\78\2\2\u0107\u0114\5\4\3\2\u0108\u0115\7\3\2\2\u0109\u0110\7\4\2\2\u010a"
            + "\u010f\5\u00aeX\2\u010b\u010f\5X-\2\u010c\u010f\5> \2\u010d\u010f\5\u0084"
            + "C\2\u010e\u010a\3\2\2\2\u010e\u010b\3\2\2\2\u010e\u010c\3\2\2\2\u010e"
            + "\u010d\3\2\2\2\u010f\u0112\3\2\2\2\u0110\u010e\3\2\2\2\u0110\u0111\3\2"
            + "\2\2\u0111\u0113\3\2\2\2\u0112\u0110\3\2\2\2\u0113\u0115\7\5\2\2\u0114"
            + "\u0108\3\2\2\2\u0114\u0109\3\2\2\2\u0115\r\3\2\2\2\u0116\u0117\78\2\2"
            + "\u0117\u0128\5\4\3\2\u0118\u0129\7\3\2\2\u0119\u0124\7\4\2\2\u011a\u0123"
            + "\5\u00aeX\2\u011b\u0123\5X-\2\u011c\u0123\5> \2\u011d\u0123\5\u0084C\2"
            + "\u011e\u0123\5f\64\2\u011f\u0123\5b\62\2\u0120\u0123\5R*\2\u0121\u0123"
            + "\5P)\2\u0122\u011a\3\2\2\2\u0122\u011b\3\2\2\2\u0122\u011c\3\2\2\2\u0122"
            + "\u011d\3\2\2\2\u0122\u011e\3\2\2\2\u0122\u011f\3\2\2\2\u0122\u0120\3\2"
            + "\2\2\u0122\u0121\3\2\2\2\u0123\u0126\3\2\2\2\u0124\u0122\3\2\2\2\u0124"
            + "\u0125\3\2\2\2\u0125\u0127\3\2\2\2\u0126\u0124\3\2\2\2\u0127\u0129\7\5"
            + "\2\2\u0128\u0118\3\2\2\2\u0128\u0119\3\2\2\2\u0129\17\3\2\2\2\u012a\u012b"
            + "\78\2\2\u012b\u0132\5\4\3\2\u012c\u0133\7\3\2\2\u012d\u012f\7\4\2\2\u012e"
            + "\u0130\5\6\4\2\u012f\u012e\3\2\2\2\u012f\u0130\3\2\2\2\u0130\u0131\3\2"
            + "\2\2\u0131\u0133\7\5\2\2\u0132\u012c\3\2\2\2\u0132\u012d\3\2\2\2\u0133"
            + "\21\3\2\2\2\u0134\u0135\79\2\2\u0135\u0136\5\4\3\2\u0136\u013d\7\4\2\2"
            + "\u0137\u013e\5\u00b2Z\2\u0138\u013e\5\u00b0Y\2\u0139\u013e\5\20\t\2\u013a"
            + "\u013e\5\16\b\2\u013b\u013e\5\n\6\2\u013c\u013e\5\f\7\2\u013d\u0137\3"
            + "\2\2\2\u013d\u0138\3\2\2\2\u013d\u0139\3\2\2\2\u013d\u013a\3\2\2\2\u013d"
            + "\u013b\3\2\2\2\u013d\u013c\3\2\2\2\u013e\u013f\3\2\2\2\u013f\u013d\3\2"
            + "\2\2\u013f\u0140\3\2\2\2\u0140\u0141\3\2\2\2\u0141\u0142\7\5\2\2\u0142"
            + "\23\3\2\2\2\u0143\u0144\7$\2\2\u0144\u0154\5\4\3\2\u0145\u0155\7\3\2\2"
            + "\u0146\u0150\7\4\2\2\u0147\u014f\5\u009aN\2\u0148\u014f\5j\66\2\u0149"
            + "\u014f\5\u00b2Z\2\u014a\u014f\5\u00b0Y\2\u014b\u014f\5\u0098M\2\u014c"
            + "\u014f\5J&\2\u014d\u014f\5\u00c4c\2\u014e\u0147\3\2\2\2\u014e\u0148\3"
            + "\2\2\2\u014e\u0149\3\2\2\2\u014e\u014a\3\2\2\2\u014e\u014b\3\2\2\2\u014e"
            + "\u014c\3\2\2\2\u014e\u014d\3\2\2\2\u014f\u0152\3\2\2\2\u0150\u014e\3\2"
            + "\2\2\u0150\u0151\3\2\2\2\u0151\u0153\3\2\2\2\u0152\u0150\3\2\2\2\u0153"
            + "\u0155\7\5\2\2\u0154\u0145\3\2\2\2\u0154\u0146\3\2\2\2\u0155\25\3\2\2"
            + "\2\u0156\u0157\7!\2\2\u0157\u015b\7\4\2\2\u0158\u015c\5\u0098M\2\u0159"
            + "\u015c\5J&\2\u015a\u015c\5\u00c4c\2\u015b\u0158\3\2\2\2\u015b\u0159\3"
            + "\2\2\2\u015b\u015a\3\2\2\2\u015c\u015d\3\2\2\2\u015d\u015b\3\2\2\2\u015d"
            + "\u015e\3\2\2\2\u015e\u015f\3\2\2\2\u015f\u0160\7\5\2\2\u0160\27\3\2\2"
            + "\2\u0161\u0162\7\60\2\2\u0162\u0166\7\4\2\2\u0163\u0167\5\u0098M\2\u0164"
            + "\u0167\5J&\2\u0165\u0167\5\u00c4c\2\u0166\u0163\3\2\2\2\u0166\u0164\3"
            + "\2\2\2\u0166\u0165\3\2\2\2\u0167\u0168\3\2\2\2\u0168\u0166\3\2\2\2\u0168"
            + "\u0169\3\2\2\2\u0169\u016a\3\2\2\2\u016a\u016b\7\5\2\2\u016b\31\3\2\2"
            + "\2\u016c\u016d\7\25\2\2\u016d\u017e\5\4\3\2\u016e\u017f\7\3\2\2\u016f"
            + "\u017a\7\4\2\2\u0170\u0179\5\u009aN\2\u0171\u0179\5j\66\2\u0172\u0179"
            + "\5\u00b2Z\2\u0173\u0179\5\u00b0Y\2\u0174\u0179\5\u0098M\2\u0175\u0179"
            + "\5J&\2\u0176\u0179\5\30\r\2\u0177\u0179\5\26\f\2\u0178\u0170\3\2\2\2\u0178"
            + "\u0171\3\2\2\2\u0178\u0172\3\2\2\2\u0178\u0173\3\2\2\2\u0178\u0174\3\2"
            + "\2\2\u0178\u0175\3\2\2\2\u0178\u0176\3\2\2\2\u0178\u0177\3\2\2\2\u0179"
            + "\u017c\3\2\2\2\u017a\u0178\3\2\2\2\u017a\u017b\3\2\2\2\u017b\u017d\3\2"
            + "\2\2\u017c\u017a\3\2\2\2\u017d\u017f\7\5\2\2\u017e\u016e\3\2\2\2\u017e"
            + "\u016f\3\2\2\2\u017f\33\3\2\2\2\u0180\u0181\7\f\2\2\u0181\u018c\5\4\3"
            + "\2\u0182\u018d\7\3\2\2\u0183\u0188\7\4\2\2\u0184\u0187\5\u00b2Z\2\u0185"
            + "\u0187\5\u00b0Y\2\u0186\u0184\3\2\2\2\u0186\u0185\3\2\2\2\u0187\u018a"
            + "\3\2\2\2\u0188\u0186\3\2\2\2\u0188\u0189\3\2\2\2\u0189\u018b\3\2\2\2\u018a"
            + "\u0188\3\2\2\2\u018b\u018d\7\5\2\2\u018c\u0182\3\2\2\2\u018c\u0183\3\2"
            + "\2\2\u018d\35\3\2\2\2\u018e\u018f\7H\2\2\u018f\u0190\5\4\3\2\u0190\u0199"
            + "\7\4\2\2\u0191\u019a\5\6\4\2\u0192\u019a\5\34\17\2\u0193\u019a\5\u009a"
            + "N\2\u0194\u019a\5j\66\2\u0195\u019a\5\u00b2Z\2\u0196\u019a\5\u00b0Y\2"
            + "\u0197\u019a\5\u00c4c\2\u0198\u019a\58\35\2\u0199\u0191\3\2\2\2\u0199"
            + "\u0192\3\2\2\2\u0199\u0193\3\2\2\2\u0199\u0194\3\2\2\2\u0199\u0195\3\2"
            + "\2\2\u0199\u0196\3\2\2\2\u0199\u0197\3\2\2\2\u0199\u0198\3\2\2\2\u019a"
            + "\u019b\3\2\2\2\u019b\u0199\3\2\2\2\u019b\u019c\3\2\2\2\u019c\u019d\3\2"
            + "\2\2\u019d\u019e\7\5\2\2\u019e\37\3\2\2\2\u019f\u01a0\7H\2\2\u01a0\u01a1"
            + "\5\4\3\2\u01a1\u01aa\7\4\2\2\u01a2\u01ab\5\6\4\2\u01a3\u01ab\5\34\17\2"
            + "\u01a4\u01ab\5\u009aN\2\u01a5\u01ab\5j\66\2\u01a6\u01ab\5\u00b2Z\2\u01a7"
            + "\u01ab\5\u00b0Y\2\u01a8\u01ab\5\u00c4c\2\u01a9\u01ab\58\35\2\u01aa\u01a2"
            + "\3\2\2\2\u01aa\u01a3\3\2\2\2\u01aa\u01a4\3\2\2\2\u01aa\u01a5\3\2\2\2\u01aa"
            + "\u01a6\3\2\2\2\u01aa\u01a7\3\2\2\2\u01aa\u01a8\3\2\2\2\u01aa\u01a9\3\2"
            + "\2\2\u01ab\u01ac\3\2\2\2\u01ac\u01aa\3\2\2\2\u01ac\u01ad\3\2\2\2\u01ad"
            + "\u01ae\3\2\2\2\u01ae\u01af\7\5\2\2\u01af!\3\2\2\2\u01b0\u01b6\5X-\2\u01b1"
            + "\u01b6\5f\64\2\u01b2\u01b6\5b\62\2\u01b3\u01b6\5\u00b2Z\2\u01b4\u01b6"
            + "\5\u00b0Y\2\u01b5\u01b0\3\2\2\2\u01b5\u01b1\3\2\2\2\u01b5\u01b2\3\2\2"
            + "\2\u01b5\u01b3\3\2\2\2\u01b5\u01b4\3\2\2\2\u01b6\u01b9\3\2\2\2\u01b7\u01b5"
            + "\3\2\2\2\u01b7\u01b8\3\2\2\2\u01b8#\3\2\2\2\u01b9\u01b7\3\2\2\2\u01ba"
            + "\u01bd\5\u00b2Z\2\u01bb\u01bd\5\u00b0Y\2\u01bc\u01ba\3\2\2\2\u01bc\u01bb"
            + "\3\2\2\2\u01bd\u01c0\3\2\2\2\u01be\u01bc\3\2\2\2\u01be\u01bf\3\2\2\2\u01bf"
            + "%\3\2\2\2\u01c0\u01be\3\2\2\2\u01c1\u01c7\5\u0084C\2\u01c2\u01c7\5f\64"
            + "\2\u01c3\u01c7\5b\62\2\u01c4\u01c7\5\u00b2Z\2\u01c5\u01c7\5\u00b0Y\2\u01c6"
            + "\u01c1\3\2\2\2\u01c6\u01c2\3\2\2\2\u01c6\u01c3\3\2\2\2\u01c6\u01c4\3\2"
            + "\2\2\u01c6\u01c5\3\2\2\2\u01c7\u01ca\3\2\2\2\u01c8\u01c6\3\2\2\2\u01c8"
            + "\u01c9\3\2\2\2\u01c9\'\3\2\2\2\u01ca\u01c8\3\2\2\2\u01cb\u01d2\5X-\2\u01cc"
            + "\u01d2\5f\64\2\u01cd\u01d2\5R*\2\u01ce\u01d2\5P)\2\u01cf\u01d2\5\u00b2"
            + "Z\2\u01d0\u01d2\5\u00b0Y\2\u01d1\u01cb\3\2\2\2\u01d1\u01cc\3\2\2\2\u01d1"
            + "\u01cd\3\2\2\2\u01d1\u01ce\3\2\2\2\u01d1\u01cf\3\2\2\2\u01d1\u01d0\3\2"
            + "\2\2\u01d2\u01d5\3\2\2\2\u01d3\u01d1\3\2\2\2\u01d3\u01d4\3\2\2\2\u01d4"
            + ")\3\2\2\2\u01d5\u01d3\3\2\2\2\u01d6\u01dd\5X-\2\u01d7\u01dd\5f\64\2\u01d8"
            + "\u01dd\5R*\2\u01d9\u01dd\5P)\2\u01da\u01dd\5\u00b2Z\2\u01db\u01dd\5\u00b0"
            + "Y\2\u01dc\u01d6\3\2\2\2\u01dc\u01d7\3\2\2\2\u01dc\u01d8\3\2\2\2\u01dc"
            + "\u01d9\3\2\2\2\u01dc\u01da\3\2\2\2\u01dc\u01db\3\2\2\2\u01dd\u01e0\3\2"
            + "\2\2\u01de\u01dc\3\2\2\2\u01de\u01df\3\2\2\2\u01df+\3\2\2\2\u01e0\u01de"
            + "\3\2\2\2\u01e1\u01e8\5X-\2\u01e2\u01e8\5\u0084C\2\u01e3\u01e8\5f\64\2"
            + "\u01e4\u01e8\5b\62\2\u01e5\u01e8\5\u00b2Z\2\u01e6\u01e8\5\u00b0Y\2\u01e7"
            + "\u01e1\3\2\2\2\u01e7\u01e2\3\2\2\2\u01e7\u01e3\3\2\2\2\u01e7\u01e4\3\2"
            + "\2\2\u01e7\u01e5\3\2\2\2\u01e7\u01e6\3\2\2\2\u01e8\u01eb\3\2\2\2\u01e9"
            + "\u01e7\3\2\2\2\u01e9\u01ea\3\2\2\2\u01ea-\3\2\2\2\u01eb\u01e9\3\2\2\2"
            + "\u01ec\u01f2\5X-\2\u01ed\u01f2\5^\60\2\u01ee\u01f2\5f\64\2\u01ef\u01f2"
            + "\5\u00b2Z\2\u01f0\u01f2\5\u00b0Y\2\u01f1\u01ec\3\2\2\2\u01f1\u01ed\3\2"
            + "\2\2\u01f1\u01ee\3\2\2\2\u01f1\u01ef\3\2\2\2\u01f1\u01f0\3\2\2\2\u01f2"
            + "\u01f5\3\2\2\2\u01f3\u01f1\3\2\2\2\u01f3\u01f4\3\2\2\2\u01f4/\3\2\2\2"
            + "\u01f5\u01f3\3\2\2\2\u01f6\u01fe\5.\30\2\u01f7\u01fe\5,\27\2\u01f8\u01fe"
            + "\5*\26\2\u01f9\u01fe\5(\25\2\u01fa\u01fe\5&\24\2\u01fb\u01fe\5$\23\2\u01fc"
            + "\u01fe\5\"\22\2\u01fd\u01f6\3\2\2\2\u01fd\u01f7\3\2\2\2\u01fd\u01f8\3"
            + "\2\2\2\u01fd\u01f9\3\2\2\2\u01fd\u01fa\3\2\2\2\u01fd\u01fb\3\2\2\2\u01fd"
            + "\u01fc\3\2\2\2\u01fe\61\3\2\2\2\u01ff\u0200\7\31\2\2\u0200\u0206\5\4\3"
            + "\2\u0201\u0207\7\3\2\2\u0202\u0203\7\4\2\2\u0203\u0204\5\60\31\2\u0204"
            + "\u0205\7\5\2\2\u0205\u0207\3\2\2\2\u0206\u0201\3\2\2\2\u0206\u0202\3\2"
            + "\2\2\u0207\63\3\2\2\2\u0208\u0209\7\16\2\2\u0209\u021a\5\4\3\2\u020a\u021b"
            + "\7\3\2\2\u020b\u0216\7\4\2\2\u020c\u0215\5\6\4\2\u020d\u0215\5\34\17\2"
            + "\u020e\u0215\5\u009aN\2\u020f\u0215\5j\66\2\u0210\u0215\5\u00b2Z\2\u0211"
            + "\u0215\5\u00b0Y\2\u0212\u0215\5\62\32\2\u0213\u0215\5 \21\2\u0214\u020c"
            + "\3\2\2\2\u0214\u020d\3\2\2\2\u0214\u020e\3\2\2\2\u0214\u020f\3\2\2\2\u0214"
            + "\u0210\3\2\2\2\u0214\u0211\3\2\2\2\u0214\u0212\3\2\2\2\u0214\u0213\3\2"
            + "\2\2\u0215\u0218\3\2\2\2\u0216\u0214\3\2\2\2\u0216\u0217\3\2\2\2\u0217"
            + "\u0219\3\2\2\2\u0218\u0216\3\2\2\2\u0219\u021b\7\5\2\2\u021a\u020a\3\2"
            + "\2\2\u021a\u020b\3\2\2\2\u021b\65\3\2\2\2\u021c\u021d\7J\2\2\u021d\u022f"
            + "\5\4\3\2\u021e\u0230\7\3\2\2\u021f\u022b\7\4\2\2\u0220\u022a\5\6\4\2\u0221"
            + "\u022a\5\34\17\2\u0222\u022a\5\u009aN\2\u0223\u022a\5X-\2\u0224\u022a"
            + "\5f\64\2\u0225\u022a\5b\62\2\u0226\u022a\5j\66\2\u0227\u022a\5\u00b2Z"
            + "\2\u0228\u022a\5\u00b0Y\2\u0229\u0220\3\2\2\2\u0229\u0221\3\2\2\2\u0229"
            + "\u0222\3\2\2\2\u0229\u0223\3\2\2\2\u0229\u0224\3\2\2\2\u0229\u0225\3\2"
            + "\2\2\u0229\u0226\3\2\2\2\u0229\u0227\3\2\2\2\u0229\u0228\3\2\2\2\u022a"
            + "\u022d\3\2\2\2\u022b\u0229\3\2\2\2\u022b\u022c\3\2\2\2\u022c\u022e\3\2"
            + "\2\2\u022d\u022b\3\2\2\2\u022e\u0230\7\5\2\2\u022f\u021e\3\2\2\2\u022f"
            + "\u021f\3\2\2\2\u0230\67\3\2\2\2\u0231\u0232\7D\2\2\u0232\u0242\5\4\3\2"
            + "\u0233\u0243\7\3\2\2\u0234\u023e\7\4\2\2\u0235\u023d\5\6\4\2\u0236\u023d"
            + "\5\34\17\2\u0237\u023d\5\u009aN\2\u0238\u023d\5j\66\2\u0239\u023d\5\u00b2"
            + "Z\2\u023a\u023d\5\u00b0Y\2\u023b\u023d\5\u00c4c\2\u023c\u0235\3\2\2\2"
            + "\u023c\u0236\3\2\2\2\u023c\u0237\3\2\2\2\u023c\u0238\3\2\2\2\u023c\u0239"
            + "\3\2\2\2\u023c\u023a\3\2\2\2\u023c\u023b\3\2\2\2\u023d\u0240\3\2\2\2\u023e"
            + "\u023c\3\2\2\2\u023e\u023f\3\2\2\2\u023f\u0241\3\2\2\2\u0240\u023e\3\2"
            + "\2\2\u0241\u0243\7\5\2\2\u0242\u0233\3\2\2\2\u0242\u0234\3\2\2\2\u0243"
            + "9\3\2\2\2\u0244\u024a\5H%\2\u0245\u024a\5F$\2\u0246\u024a\5D#\2\u0247"
            + "\u024a\5B\"\2\u0248\u024a\5\66\34\2\u0249\u0244\3\2\2\2\u0249\u0245\3"
            + "\2\2\2\u0249\u0246\3\2\2\2\u0249\u0247\3\2\2\2\u0249\u0248\3\2\2\2\u024a"
            + ";\3\2\2\2\u024b\u024c\7C\2\2\u024c\u0260\5\4\3\2\u024d\u0261\7\3\2\2\u024e"
            + "\u025c\7\4\2\2\u024f\u025b\5\6\4\2\u0250\u025b\5\34\17\2\u0251\u025b\5"
            + "\u009aN\2\u0252\u025b\5\u0084C\2\u0253\u025b\5f\64\2\u0254\u025b\5b\62"
            + "\2\u0255\u025b\5j\66\2\u0256\u025b\5\u00b2Z\2\u0257\u025b\5\u00b0Y\2\u0258"
            + "\u025b\5:\36\2\u0259\u025b\58\35\2\u025a\u024f\3\2\2\2\u025a\u0250\3\2"
            + "\2\2\u025a\u0251\3\2\2\2\u025a\u0252\3\2\2\2\u025a\u0253\3\2\2\2\u025a"
            + "\u0254\3\2\2\2\u025a\u0255\3\2\2\2\u025a\u0256\3\2\2\2\u025a\u0257\3\2"
            + "\2\2\u025a\u0258\3\2\2\2\u025a\u0259\3\2\2\2\u025b\u025e\3\2\2\2\u025c"
            + "\u025a\3\2\2\2\u025c\u025d\3\2\2\2\u025d\u025f\3\2\2\2\u025e\u025c\3\2"
            + "\2\2\u025f\u0261\7\5\2\2\u0260\u024d\3\2\2\2\u0260\u024e\3\2\2\2\u0261"
            + "=\3\2\2\2\u0262\u0263\7\20\2\2\u0263\u0264\5\4\3\2\u0264\u0265\5\b\5\2"
            + "\u0265?\3\2\2\2\u0266\u0267\7/\2\2\u0267\u0268\5\4\3\2\u0268\u0269\5\b"
            + "\5\2\u0269A\3\2\2\2\u026a\u026b\7+\2\2\u026b\u026c\5\4\3\2\u026c\u027d"
            + "\7\4\2\2\u026d\u027e\5\6\4\2\u026e\u027e\5\34\17\2\u026f\u027e\5\u009a"
            + "N\2\u0270\u027e\5X-\2\u0271\u027e\5@!\2\u0272\u027e\5> \2\u0273\u027e"
            + "\5f\64\2\u0274\u027e\5R*\2\u0275\u027e\5P)\2\u0276\u027e\5\\/\2\u0277"
            + "\u027e\5j\66\2\u0278\u027e\5\u00b2Z\2\u0279\u027e\5\u00b0Y\2\u027a\u027e"
            + "\5\u0098M\2\u027b\u027e\5J&\2\u027c\u027e\5\u00c4c\2\u027d\u026d\3\2\2"
            + "\2\u027d\u026e\3\2\2\2\u027d\u026f\3\2\2\2\u027d\u0270\3\2\2\2\u027d\u0271"
            + "\3\2\2\2\u027d\u0272\3\2\2\2\u027d\u0273\3\2\2\2\u027d\u0274\3\2\2\2\u027d"
            + "\u0275\3\2\2\2\u027d\u0276\3\2\2\2\u027d\u0277\3\2\2\2\u027d\u0278\3\2"
            + "\2\2\u027d\u0279\3\2\2\2\u027d\u027a\3\2\2\2\u027d\u027b\3\2\2\2\u027d"
            + "\u027c\3\2\2\2\u027e\u027f\3\2\2\2\u027f\u027d\3\2\2\2\u027f\u0280\3\2"
            + "\2\2\u0280\u0281\3\2\2\2\u0281\u0282\7\5\2\2\u0282C\3\2\2\2\u0283\u0284"
            + "\7-\2\2\u0284\u0285\5\4\3\2\u0285\u0295\7\4\2\2\u0286\u0294\5\6\4\2\u0287"
            + "\u0294\5\34\17\2\u0288\u0294\5\u009aN\2\u0289\u0294\5\u0096L\2\u028a\u0294"
            + "\5\u00aeX\2\u028b\u0294\5X-\2\u028c\u0294\5f\64\2\u028d\u0294\5R*\2\u028e"
            + "\u0294\5P)\2\u028f\u0294\5\\/\2\u0290\u0294\5j\66\2\u0291\u0294\5\u00b2"
            + "Z\2\u0292\u0294\5\u00b0Y\2\u0293\u0286\3\2\2\2\u0293\u0287\3\2\2\2\u0293"
            + "\u0288\3\2\2\2\u0293\u0289\3\2\2\2\u0293\u028a\3\2\2\2\u0293\u028b\3\2"
            + "\2\2\u0293\u028c\3\2\2\2\u0293\u028d\3\2\2\2\u0293\u028e\3\2\2\2\u0293"
            + "\u028f\3\2\2\2\u0293\u0290\3\2\2\2\u0293\u0291\3\2\2\2\u0293\u0292\3\2"
            + "\2\2\u0294\u0297\3\2\2\2\u0295\u0293\3\2\2\2\u0295\u0296\3\2\2\2\u0296"
            + "\u0298\3\2\2\2\u0297\u0295\3\2\2\2\u0298\u0299\7\5\2\2\u0299E\3\2\2\2"
            + "\u029a\u029b\7.\2\2\u029b\u029c\5\4\3\2\u029c\u02ab\7\4\2\2\u029d\u02aa"
            + "\5\6\4\2\u029e\u02aa\5\34\17\2\u029f\u02aa\5\u009aN\2\u02a0\u02aa\5\u0096"
            + "L\2\u02a1\u02aa\5\u00aeX\2\u02a2\u02aa\5X-\2\u02a3\u02aa\5\u0084C\2\u02a4"
            + "\u02aa\5f\64\2\u02a5\u02aa\5b\62\2\u02a6\u02aa\5j\66\2\u02a7\u02aa\5\u00b2"
            + "Z\2\u02a8\u02aa\5\u00b0Y\2\u02a9\u029d\3\2\2\2\u02a9\u029e\3\2\2\2\u02a9"
            + "\u029f\3\2\2\2\u02a9\u02a0\3\2\2\2\u02a9\u02a1\3\2\2\2\u02a9\u02a2\3\2"
            + "\2\2\u02a9\u02a3\3\2\2\2\u02a9\u02a4\3\2\2\2\u02a9\u02a5\3\2\2\2\u02a9"
            + "\u02a6\3\2\2\2\u02a9\u02a7\3\2\2\2\u02a9\u02a8\3\2\2\2\u02aa\u02ad\3\2"
            + "\2\2\u02ab\u02a9\3\2\2\2\u02ab\u02ac\3\2\2\2\u02ac\u02ae\3\2\2\2\u02ad"
            + "\u02ab\3\2\2\2\u02ae\u02af\7\5\2\2\u02afG\3\2\2\2\u02b0\u02b1\7@\2\2\u02b1"
            + "\u02c6\5\4\3\2\u02b2\u02c7\7\3\2\2\u02b3\u02c2\7\4\2\2\u02b4\u02c1\5\6"
            + "\4\2\u02b5\u02c1\5\34\17\2\u02b6\u02c1\5\u009aN\2\u02b7\u02c1\5X-\2\u02b8"
            + "\u02c1\5^\60\2\u02b9\u02c1\5f\64\2\u02ba\u02c1\5j\66\2\u02bb\u02c1\5\u00b2"
            + "Z\2\u02bc\u02c1\5\u00b0Y\2\u02bd\u02c1\5\u0098M\2\u02be\u02c1\5J&\2\u02bf"
            + "\u02c1\5\u00c4c\2\u02c0\u02b4\3\2\2\2\u02c0\u02b5\3\2\2\2\u02c0\u02b6"
            + "\3\2\2\2\u02c0\u02b7\3\2\2\2\u02c0\u02b8\3\2\2\2\u02c0\u02b9\3\2\2\2\u02c0"
            + "\u02ba\3\2\2\2\u02c0\u02bb\3\2\2\2\u02c0\u02bc\3\2\2\2\u02c0\u02bd\3\2"
            + "\2\2\u02c0\u02be\3\2\2\2\u02c0\u02bf\3\2\2\2\u02c1\u02c4\3\2\2\2\u02c2"
            + "\u02c0\3\2\2\2\u02c2\u02c3\3\2\2\2\u02c3\u02c5\3\2\2\2\u02c4\u02c2\3\2"
            + "\2\2\u02c5\u02c7\7\5\2\2\u02c6\u02b2\3\2\2\2\u02c6\u02b3\3\2\2\2\u02c7"
            + "I\3\2\2\2\u02c8\u02c9\7\65\2\2\u02c9\u02d9\5\4\3\2\u02ca\u02da\7\3\2\2"
            + "\u02cb\u02d5\7\4\2\2\u02cc\u02d4\5\6\4\2\u02cd\u02d4\5j\66\2\u02ce\u02d4"
            + "\5\u00b2Z\2\u02cf\u02d4\5\u00b0Y\2\u02d0\u02d4\5\u0098M\2\u02d1\u02d4"
            + "\5J&\2\u02d2\u02d4\5\u00c4c\2\u02d3\u02cc\3\2\2\2\u02d3\u02cd\3\2\2\2"
            + "\u02d3\u02ce\3\2\2\2\u02d3\u02cf\3\2\2\2\u02d3\u02d0\3\2\2\2\u02d3\u02d1"
            + "\3\2\2\2\u02d3\u02d2\3\2\2\2\u02d4\u02d7\3\2\2\2\u02d5\u02d3\3\2\2\2\u02d5"
            + "\u02d6\3\2\2\2\u02d6\u02d8\3\2\2\2\u02d7\u02d5\3\2\2\2\u02d8\u02da\7\5"
            + "\2\2\u02d9\u02ca\3\2\2\2\u02d9\u02cb\3\2\2\2\u02daK\3\2\2\2\u02db\u02dc"
            + "\7\r\2\2\u02dc\u02dd\5\4\3\2\u02dd\u02de\5\b\5\2\u02deM\3\2\2\2\u02df"
            + "\u02e0\5\4\3\2\u02e0O\3\2\2\2\u02e1\u02e2\7)\2\2\u02e2\u02e3\5N(\2\u02e3"
            + "\u02e4\5\b\5\2\u02e4Q\3\2\2\2\u02e5\u02e6\7(\2\2\u02e6\u02e7\5\4\3\2\u02e7"
            + "\u02e8\5\b\5\2\u02e8S\3\2\2\2\u02e9\u02ea\7<\2\2\u02ea\u02eb\5\4\3\2\u02eb"
            + "\u02ec\5\b\5\2\u02ecU\3\2\2\2\u02ed\u02ee\7;\2\2\u02ee\u02ef\5\4\3\2\u02ef"
            + "\u02f0\5\b\5\2\u02f0W\3\2\2\2\u02f1\u02f2\7&\2\2\u02f2\u0300\5\4\3\2\u02f3"
            + "\u0301\7\3\2\2\u02f4\u02fc\7\4\2\2\u02f5\u02fb\5\6\4\2\u02f6\u02fb\5V"
            + ",\2\u02f7\u02fb\5T+\2\u02f8\u02fb\5\u00b2Z\2\u02f9\u02fb\5\u00b0Y\2\u02fa"
            + "\u02f5\3\2\2\2\u02fa\u02f6\3\2\2\2\u02fa\u02f7\3\2\2\2\u02fa\u02f8\3\2"
            + "\2\2\u02fa\u02f9\3\2\2\2\u02fb\u02fe\3\2\2\2\u02fc\u02fa\3\2\2\2\u02fc"
            + "\u02fd\3\2\2\2\u02fd\u02ff\3\2\2\2\u02fe\u02fc\3\2\2\2\u02ff\u0301\7\5"
            + "\2\2\u0300\u02f3\3\2\2\2\u0300\u02f4\3\2\2\2\u0301Y\3\2\2\2\u0302\u0303"
            + "\5\4\3\2\u0303[\3\2\2\2\u0304\u0305\7#\2\2\u0305\u0306\5Z.\2\u0306\u0307"
            + "\5\b\5\2\u0307]\3\2\2\2\u0308\u0309\7\34\2\2\u0309\u030a\5\4\3\2\u030a"
            + "\u030b\5\b\5\2\u030b_\3\2\2\2\u030c\u030d\5\4\3\2\u030da\3\2\2\2\u030e"
            + "\u030f\7*\2\2\u030f\u0310\5`\61\2\u0310\u0311\5\b\5\2\u0311c\3\2\2\2\u0312"
            + "\u0313\5\4\3\2\u0313e\3\2\2\2\u0314\u0315\7B\2\2\u0315\u0316\5d\63\2\u0316"
            + "\u0317\5\b\5\2\u0317g\3\2\2\2\u0318\u0319\5\4\3\2\u0319i\3\2\2\2\u031a"
            + "\u031b\7\24\2\2\u031b\u031c\5h\65\2\u031c\u031d\5\b\5\2\u031dk\3\2\2\2"
            + "\u031e\u031f\7\36\2\2\u031f\u0320\5\4\3\2\u0320\u0321\5\b\5\2\u0321m\3"
            + "\2\2\2\u0322\u0323\7E\2\2\u0323\u0331\5\4\3\2\u0324\u0332\7\3\2\2\u0325"
            + "\u032d\7\4\2\2\u0326\u032c\5\6\4\2\u0327\u032c\5l\67\2\u0328\u032c\5j"
            + "\66\2\u0329\u032c\5\u00b2Z\2\u032a\u032c\5\u00b0Y\2\u032b\u0326\3\2\2"
            + "\2\u032b\u0327\3\2\2\2\u032b\u0328\3\2\2\2\u032b\u0329\3\2\2\2\u032b\u032a"
            + "\3\2\2\2\u032c\u032f\3\2\2\2\u032d\u032b\3\2\2\2\u032d\u032e\3\2\2\2\u032e"
            + "\u0330\3\2\2\2\u032f\u032d\3\2\2\2\u0330\u0332\7\5\2\2\u0331\u0324\3\2"
            + "\2\2\u0331\u0325\3\2\2\2\u0332o\3\2\2\2\u0333\u0335\5n8\2\u0334\u0333"
            + "\3\2\2\2\u0335\u0336\3\2\2\2\u0336\u0334\3\2\2\2\u0336\u0337\3\2\2\2\u0337"
            + "q\3\2\2\2\u0338\u033a\5\u0096L\2\u0339\u0338\3\2\2\2\u033a\u033b\3\2\2"
            + "\2\u033b\u0339\3\2\2\2\u033b\u033c\3\2\2\2\u033cs\3\2\2\2\u033d\u033e"
            + "\5\u009eP\2\u033eu\3\2\2\2\u033f\u0341\5z>\2\u0340\u033f\3\2\2\2\u0340"
            + "\u0341\3\2\2\2\u0341w\3\2\2\2\u0342\u0343\5\4\3\2\u0343y\3\2\2\2\u0344"
            + "\u0345\7\30\2\2\u0345\u0346\5x=\2\u0346\u0347\5\b\5\2\u0347{\3\2\2\2\u0348"
            + "\u0349\7 \2\2\u0349\u034a\5\4\3\2\u034a\u034b\5\b\5\2\u034b}\3\2\2\2\u034c"
            + "\u034d\5|?\2\u034d\177\3\2\2\2\u034e\u034f\7=\2\2\u034f\u035d\5\4\3\2"
            + "\u0350\u035e\7\3\2\2\u0351\u0359\7\4\2\2\u0352\u0358\5\6\4\2\u0353\u0358"
            + "\5L\'\2\u0354\u0358\5j\66\2\u0355\u0358\5\u00b2Z\2\u0356\u0358\5\u00b0"
            + "Y\2\u0357\u0352\3\2\2\2\u0357\u0353\3\2\2\2\u0357\u0354\3\2\2\2\u0357"
            + "\u0355\3\2\2\2\u0357\u0356\3\2\2\2\u0358\u035b\3\2\2\2\u0359\u0357\3\2"
            + "\2\2\u0359\u035a\3\2\2\2\u035a\u035c\3\2\2\2\u035b\u0359\3\2\2\2\u035c"
            + "\u035e\7\5\2\2\u035d\u0350\3\2\2\2\u035d\u0351\3\2\2\2\u035e\u0081\3\2"
            + "\2\2\u035f\u0361\5\u0080A\2\u0360\u035f\3\2\2\2\u0361\u0362\3\2\2\2\u0362"
            + "\u0360\3\2\2\2\u0362\u0363\3\2\2\2\u0363\u0083\3\2\2\2\u0364\u0365\7?"
            + "\2\2\u0365\u0366\5\4\3\2\u0366\u0367\5\b\5\2\u0367\u0085\3\2\2\2\u0368"
            + "\u0369\7\37\2\2\u0369\u0377\5\4\3\2\u036a\u0378\7\3\2\2\u036b\u0373\7"
            + "\4\2\2\u036c\u0372\5\6\4\2\u036d\u0372\5V,\2\u036e\u0372\5T+\2\u036f\u0372"
            + "\5\u00b2Z\2\u0370\u0372\5\u00b0Y\2\u0371\u036c\3\2\2\2\u0371\u036d\3\2"
            + "\2\2\u0371\u036e\3\2\2\2\u0371\u036f\3\2\2\2\u0371\u0370\3\2\2\2\u0372"
            + "\u0375\3\2\2\2\u0373\u0371\3\2\2\2\u0373\u0374\3\2\2\2\u0374\u0376\3\2"
            + "\2\2\u0375\u0373\3\2\2\2\u0376\u0378\7\5\2\2\u0377\u036a\3\2\2\2\u0377"
            + "\u036b\3\2\2\2\u0378\u0087\3\2\2\2\u0379\u037a\7,\2\2\u037a\u0388\5\4"
            + "\3\2\u037b\u0389\7\3\2\2\u037c\u0384\7\4\2\2\u037d\u0383\5\6\4\2\u037e"
            + "\u0383\5V,\2\u037f\u0383\5T+\2\u0380\u0383\5\u00b2Z\2\u0381\u0383\5\u00b0"
            + "Y\2\u0382\u037d\3\2\2\2\u0382\u037e\3\2\2\2\u0382\u037f\3\2\2\2\u0382"
            + "\u0380\3\2\2\2\u0382\u0381\3\2\2\2\u0383\u0386\3\2\2\2\u0384\u0382\3\2"
            + "\2\2\u0384\u0385\3\2\2\2\u0385\u0387\3\2\2\2\u0386\u0384\3\2\2\2\u0387"
            + "\u0389\7\5\2\2\u0388\u037b\3\2\2\2\u0388\u037c\3\2\2\2\u0389\u0089\3\2"
            + "\2\2\u038a\u038d\5\u0088E\2\u038b\u038d\5\u0086D\2\u038c\u038a\3\2\2\2"
            + "\u038c\u038b\3\2\2\2\u038d\u0390\3\2\2\2\u038e\u038c\3\2\2\2\u038e\u038f"
            + "\3\2\2\2\u038f\u008b\3\2\2\2\u0390\u038e\3\2\2\2\u0391\u0392\7\66\2\2"
            + "\u0392\u0393\5\4\3\2\u0393\u0394\5\b\5\2\u0394\u008d\3\2\2\2\u0395\u0397"
            + "\5\u0092J\2\u0396\u0395\3\2\2\2\u0396\u0397\3\2\2\2\u0397\u0398\3\2\2"
            + "\2\u0398\u039e\5\u008cG\2\u0399\u039b\5\u008cG\2\u039a\u039c\5\u0092J"
            + "\2\u039b\u039a\3\2\2\2\u039b\u039c\3\2\2\2\u039c\u039e\3\2\2\2\u039d\u0396"
            + "\3\2\2\2\u039d\u0399\3\2\2\2\u039e\u008f\3\2\2\2\u039f\u03a0\7\33\2\2"
            + "\u03a0\u03ae\5\4\3\2\u03a1\u03af\7\3\2\2\u03a2\u03aa\7\4\2\2\u03a3\u03a9"
            + "\5\6\4\2\u03a4\u03a9\5V,\2\u03a5\u03a9\5T+\2\u03a6\u03a9\5\u00b2Z\2\u03a7"
            + "\u03a9\5\u00b0Y\2\u03a8\u03a3\3\2\2\2\u03a8\u03a4\3\2\2\2\u03a8\u03a5"
            + "\3\2\2\2\u03a8\u03a6\3\2\2\2\u03a8\u03a7\3\2\2\2\u03a9\u03ac\3\2\2\2\u03aa"
            + "\u03a8\3\2\2\2\u03aa\u03ab\3\2\2\2\u03ab\u03ad\3\2\2\2\u03ac\u03aa\3\2"
            + "\2\2\u03ad\u03af\7\5\2\2\u03ae\u03a1\3\2\2\2\u03ae\u03a2\3\2\2\2\u03af"
            + "\u0091\3\2\2\2\u03b0\u03b1\5\u0090I\2\u03b1\u0093\3\2\2\2\u03b2\u03bc"
            + "\5\u0092J\2\u03b3\u03bc\5\u008eH\2\u03b4\u03bc\5\u008aF\2\u03b5\u03bc"
            + "\5\u0082B\2\u03b6\u03bc\5~@\2\u03b7\u03bc\5t;\2\u03b8\u03bc\5v<\2\u03b9"
            + "\u03bc\5p9\2\u03ba\u03bc\5r:\2\u03bb\u03b2\3\2\2\2\u03bb\u03b3\3\2\2\2"
            + "\u03bb\u03b4\3\2\2\2\u03bb\u03b5\3\2\2\2\u03bb\u03b6\3\2\2\2\u03bb\u03b7"
            + "\3\2\2\2\u03bb\u03b8\3\2\2\2\u03bb\u03b9\3\2\2\2\u03bb\u03ba\3\2\2\2\u03bc"
            + "\u0095\3\2\2\2\u03bd\u03be\7\22\2\2\u03be\u03c4\5\4\3\2\u03bf\u03c5\7"
            + "\3\2\2\u03c0\u03c1\7\4\2\2\u03c1\u03c2\5\u0094K\2\u03c2\u03c3\7\5\2\2"
            + "\u03c3\u03c5\3\2\2\2\u03c4\u03bf\3\2\2\2\u03c4\u03c0\3\2\2\2\u03c5\u0097"
            + "\3\2\2\2\u03c6\u03c7\7\21\2\2\u03c7\u03c8\5\4\3\2\u03c8\u03cf\7\4\2\2"
            + "\u03c9\u03d0\5\u0096L\2\u03ca\u03d0\5\u00aeX\2\u03cb\u03d0\5\u0084C\2"
            + "\u03cc\u03d0\5j\66\2\u03cd\u03d0\5\u00b2Z\2\u03ce\u03d0\5\u00b0Y\2\u03cf"
            + "\u03c9\3\2\2\2\u03cf\u03ca\3\2\2\2\u03cf\u03cb\3\2\2\2\u03cf\u03cc\3\2"
            + "\2\2\u03cf\u03cd\3\2\2\2\u03cf\u03ce\3\2\2\2\u03d0\u03d1\3\2\2\2\u03d1"
            + "\u03cf\3\2\2\2\u03d1\u03d2\3\2\2\2\u03d2\u03d3\3\2\2\2\u03d3\u03d4\7\5"
            + "\2\2\u03d4\u0099\3\2\2\2\u03d5\u03d6\7\63\2\2\u03d6\u03d7\5\4\3\2\u03d7"
            + "\u03d8\5\b\5\2\u03d8\u009b\3\2\2\2\u03d9\u03da\7\67\2\2\u03da\u03e7\5"
            + "\4\3\2\u03db\u03e8\7\3\2\2\u03dc\u03e3\7\4\2\2\u03dd\u03e2\5\u009aN\2"
            + "\u03de\u03e2\5j\66\2\u03df\u03e2\5\u00b2Z\2\u03e0\u03e2\5\u00b0Y\2\u03e1"
            + "\u03dd\3\2\2\2\u03e1\u03de\3\2\2\2\u03e1\u03df\3\2\2\2\u03e1\u03e0\3\2"
            + "\2\2\u03e2\u03e5\3\2\2\2\u03e3\u03e1\3\2\2\2\u03e3\u03e4\3\2\2\2\u03e4"
            + "\u03e6\3\2\2\2\u03e5\u03e3\3\2\2\2\u03e6\u03e8\7\5\2\2\u03e7\u03db\3\2"
            + "\2\2\u03e7\u03dc\3\2\2\2\u03e8\u009d\3\2\2\2\u03e9\u03ea\7G\2\2\u03ea"
            + "\u03eb\5\4\3\2\u03eb\u03ec\5\b\5\2\u03ec\u009f\3\2\2\2\u03ed\u03ee\7\64"
            + "\2\2\u03ee\u03fb\5\4\3\2\u03ef\u03fc\7\3\2\2\u03f0\u03f7\7\4\2\2\u03f1"
            + "\u03f6\5\u009eP\2\u03f2\u03f6\5j\66\2\u03f3\u03f6\5\u00b2Z\2\u03f4\u03f6"
            + "\5\u00b0Y\2\u03f5\u03f1\3\2\2\2\u03f5\u03f2\3\2\2\2\u03f5\u03f3\3\2\2"
            + "\2\u03f5\u03f4\3\2\2\2\u03f6\u03f9\3\2\2\2\u03f7\u03f5\3\2\2\2\u03f7\u03f8"
            + "\3\2\2\2\u03f8\u03fa\3\2\2\2\u03f9\u03f7\3\2\2\2\u03fa\u03fc\7\5\2\2\u03fb"
            + "\u03ef\3\2\2\2\u03fb\u03f0\3\2\2\2\u03fc\u00a1\3\2\2\2\u03fd\u03fe\5\4"
            + "\3\2\u03fe\u00a3\3\2\2\2\u03ff\u0400\7\n\2\2\u0400\u0401\5\u00a2R\2\u0401"
            + "\u0402\5\b\5\2\u0402\u00a5\3\2\2\2\u0403\u0404\7I\2\2\u0404\u040b\5\4"
            + "\3\2\u0405\u040c\7\3\2\2\u0406\u0408\7\4\2\2\u0407\u0409\5\u00a4S\2\u0408"
            + "\u0407\3\2\2\2\u0408\u0409\3\2\2\2\u0409\u040a\3\2\2\2\u040a\u040c\7\5"
            + "\2\2\u040b\u0405\3\2\2\2\u040b\u0406\3\2\2\2\u040c\u00a7\3\2\2\2\u040d"
            + "\u040e\7:\2\2\u040e\u041b\5\4\3\2\u040f\u041c\7\3\2\2\u0410\u0417\7\4"
            + "\2\2\u0411\u0416\5\u00a6T\2\u0412\u0416\5j\66\2\u0413\u0416\5\u00b2Z\2"
            + "\u0414\u0416\5\u00b0Y\2\u0415\u0411\3\2\2\2\u0415\u0412\3\2\2\2\u0415"
            + "\u0413\3\2\2\2\u0415\u0414\3\2\2\2\u0416\u0419\3\2\2\2\u0417\u0415\3\2"
            + "\2\2\u0417\u0418\3\2\2\2\u0418\u041a\3\2\2\2\u0419\u0417\3\2\2\2\u041a"
            + "\u041c\7\5\2\2\u041b\u040f\3\2\2\2\u041b\u0410\3\2\2\2\u041c\u00a9\3\2"
            + "\2\2\u041d\u041e\7\26\2\2\u041e\u041f\5\4\3\2\u041f\u0420\5\b\5\2\u0420"
            + "\u00ab\3\2\2\2\u0421\u0422\7\27\2\2\u0422\u042c\5\4\3\2\u0423\u042d\7"
            + "\3\2\2\u0424\u0426\7\4\2\2\u0425\u0427\5\u00b2Z\2\u0426\u0425\3\2\2\2"
            + "\u0426\u0427\3\2\2\2\u0427\u0429\3\2\2\2\u0428\u042a\5\u00b0Y\2\u0429"
            + "\u0428\3\2\2\2\u0429\u042a\3\2\2\2\u042a\u042b\3\2\2\2\u042b\u042d\7\5"
            + "\2\2\u042c\u0423\3\2\2\2\u042c\u0424\3\2\2\2\u042d\u00ad\3\2\2\2\u042e"
            + "\u042f\7\17\2\2\u042f\u0430\5\4\3\2\u0430\u0431\5\b\5\2\u0431\u00af\3"
            + "\2\2\2\u0432\u0433\7\32\2\2\u0433\u0434\5\4\3\2\u0434\u0435\5\b\5\2\u0435"
            + "\u00b1\3\2\2\2\u0436\u0437\7>\2\2\u0437\u0438\5\4\3\2\u0438\u0439\5\b"
            + "\5\2\u0439\u00b3\3\2\2\2\u043a\u043b\7A\2\2\u043b\u043c\5\4\3\2\u043c"
            + "\u043d\5\b\5\2\u043d\u00b5\3\2\2\2\u043e\u043f\7\"\2\2\u043f\u0440\5\4"
            + "\3\2\u0440\u0441\5\b\5\2\u0441\u00b7\3\2\2\2\u0442\u0443\7F\2\2\u0443"
            + "\u0444\5\4\3\2\u0444\u0445\7\4\2\2\u0445\u0446\5\u00ba^\2\u0446\u0447"
            + "\7\5\2\2\u0447\u00b9\3\2\2\2\u0448\u0449\7\35\2\2\u0449\u044a\5\4\3\2"
            + "\u044a\u044b\5\b\5\2\u044b\u00bb\3\2\2\2\u044c\u044d\7%\2\2\u044d\u044e"
            + "\5\4\3\2\u044e\u044f\5\b\5\2\u044f\u00bd\3\2\2\2\u0450\u0451\7\61\2\2"
            + "\u0451\u0458\5\4\3\2\u0452\u0459\7\3\2\2\u0453\u0455\7\4\2\2\u0454\u0456"
            + "\5\u00aaV\2\u0455\u0454\3\2\2\2\u0455\u0456\3\2\2\2\u0456\u0457\3\2\2"
            + "\2\u0457\u0459\7\5\2\2\u0458\u0452\3\2\2\2\u0458\u0453\3\2\2\2\u0459\u00bf"
            + "\3\2\2\2\u045a\u045b\7\62\2\2\u045b\u045c\5\4\3\2\u045c\u045d\7\4\2\2"
            + "\u045d\u045f\5\u00ba^\2\u045e\u0460\5\u00aaV\2\u045f\u045e\3\2\2\2\u045f"
            + "\u0460\3\2\2\2\u0460\u0461\3\2\2\2\u0461\u0462\7\5\2\2\u0462\u00c1\3\2"
            + "\2\2\u0463\u0464\7\13\2\2\u0464\u0465\5\4\3\2\u0465\u0466\5\b\5\2\u0466"
            + "\u00c3\3\2\2\2\u0467\u046f\5H%\2\u0468\u046f\5F$\2\u0469\u046f\5D#\2\u046a"
            + "\u046f\5B\"\2\u046b\u046f\5<\37\2\u046c\u046f\5\66\34\2\u046d\u046f\5"
            + "\64\33\2\u046e\u0467\3\2\2\2\u046e\u0468\3\2\2\2\u046e\u0469\3\2\2\2\u046e"
            + "\u046a\3\2\2\2\u046e\u046b\3\2\2\2\u046e\u046c\3\2\2\2\u046e\u046d\3\2"
            + "\2\2\u046f\u00c5\3\2\2\2\u0470\u047b\5\u00a8U\2\u0471\u047b\5\u009cO\2"
            + "\u0472\u047b\5\u00a0Q\2\u0473\u047b\5\u0098M\2\u0474\u047b\5J&\2\u0475"
            + "\u047b\5\u00c4c\2\u0476\u047b\5\36\20\2\u0477\u047b\5\32\16\2\u0478\u047b"
            + "\5\24\13\2\u0479\u047b\5\22\n\2\u047a\u0470\3\2\2\2\u047a\u0471\3\2\2"
            + "\2\u047a\u0472\3\2\2\2\u047a\u0473\3\2\2\2\u047a\u0474\3\2\2\2\u047a\u0475"
            + "\3\2\2\2\u047a\u0476\3\2\2\2\u047a\u0477\3\2\2\2\u047a\u0478\3\2\2\2\u047a"
            + "\u0479\3\2\2\2\u047b\u047d\3\2\2\2\u047c\u047a\3\2\2\2\u047d\u0480\3\2"
            + "\2\2\u047e\u047c\3\2\2\2\u047e\u047f\3\2\2\2\u047f\u00c7\3\2\2\2\u0480"
            + "\u047e\3\2\2\2\u0481\u0483\5\u00acW\2\u0482\u0481\3\2\2\2\u0483\u0486"
            + "\3\2\2\2\u0484\u0482\3\2\2\2\u0484\u0485\3\2\2\2\u0485\u00c9\3\2\2\2\u0486"
            + "\u0484\3\2\2\2\u0487\u048a\5\u00c0a\2\u0488\u048a\5\u00be`\2\u0489\u0487"
            + "\3\2\2\2\u0489\u0488\3\2\2\2\u048a\u048d\3\2\2\2\u048b\u0489\3\2\2\2\u048b"
            + "\u048c\3\2\2\2\u048c\u00cb\3\2\2\2\u048d\u048b\3\2\2\2\u048e\u0493\5\u00b6"
            + "\\\2\u048f\u0493\5\u00b4[\2\u0490\u0493\5\u00b2Z\2\u0491\u0493\5\u00b0"
            + "Y\2\u0492\u048e\3\2\2\2\u0492\u048f\3\2\2\2\u0492\u0490\3\2\2\2\u0492"
            + "\u0491\3\2\2\2\u0493\u0496\3\2\2\2\u0494\u0492\3\2\2\2\u0494\u0495\3\2"
            + "\2\2\u0495\u00cd\3\2\2\2\u0496\u0494\3\2\2\2\u0497\u049a\5\u00c2b\2\u0498"
            + "\u049a\5\u00b8]\2\u0499\u0497\3\2\2\2\u0499\u0498\3\2\2\2\u049a\u049b"
            + "\3\2\2\2\u049b\u0499\3\2\2\2\u049b\u049c\3\2\2\2\u049c\u00cf\3\2\2\2\u049d"
            + "\u04a1\5\u00c2b\2\u049e\u04a1\5\u00bc_\2\u049f\u04a1\5\u00ba^\2\u04a0"
            + "\u049d\3\2\2\2\u04a0\u049e\3\2\2\2\u04a0\u049f\3\2\2\2\u04a1\u04a2\3\2"
            + "\2\2\u04a2\u04a0\3\2\2\2\u04a2\u04a3\3\2\2\2\u04a3\u00d1\3\2\2\2\u04a4"
            + "\u04a5\7\23\2\2\u04a5\u04a6\5\4\3\2\u04a6\u04a7\7\4\2\2\u04a7\u04a8\5"
            + "\u00ceh\2\u04a8\u04a9\5\u00caf\2\u04a9\u04aa\5\u00ccg\2\u04aa\u04ab\5"
            + "\u00c8e\2\u04ab\u04ac\5\u00c6d\2\u04ac\u04ad\7\5\2\2\u04ad\u00d3\3\2\2"
            + "\2\u04ae\u04af\7\'\2\2\u04af\u04b0\5\4\3\2\u04b0\u04b1\7\4\2\2\u04b1\u04b2"
            + "\5\u00d0i\2\u04b2\u04b3\5\u00caf\2\u04b3\u04b4\5\u00ccg\2\u04b4\u04b5"
            + "\5\u00c8e\2\u04b5\u04b6\5\u00c6d\2\u04b6\u04b7\7\5\2\2\u04b7\u00d5\3\2"
            + "\2\2\u008b\u00d8\u00df\u00e4\u00ea\u00ee\u00f1\u00fe\u0100\u0104\u010e"
            + "\u0110\u0114\u0122\u0124\u0128\u012f\u0132\u013d\u013f\u014e\u0150\u0154"
            + "\u015b\u015d\u0166\u0168\u0178\u017a\u017e\u0186\u0188\u018c\u0199\u019b"
            + "\u01aa\u01ac\u01b5\u01b7\u01bc\u01be\u01c6\u01c8\u01d1\u01d3\u01dc\u01de"
            + "\u01e7\u01e9\u01f1\u01f3\u01fd\u0206\u0214\u0216\u021a\u0229\u022b\u022f"
            + "\u023c\u023e\u0242\u0249\u025a\u025c\u0260\u027d\u027f\u0293\u0295\u02a9"
            + "\u02ab\u02c0\u02c2\u02c6\u02d3\u02d5\u02d9\u02fa\u02fc\u0300\u032b\u032d"
            + "\u0331\u0336\u033b\u0340\u0357\u0359\u035d\u0362\u0371\u0373\u0377\u0382"
            + "\u0384\u0388\u038c\u038e\u0396\u039b\u039d\u03a8\u03aa\u03ae\u03bb\u03c4"
            + "\u03cf\u03d1\u03e1\u03e3\u03e7\u03f5\u03f7\u03fb\u0408\u040b\u0415\u0417"
            + "\u041b\u0426\u0429\u042c\u0455\u0458\u045f\u046e\u047a\u047e\u0484\u0489"
            + "\u048b\u0492\u0494\u0499\u049b\u04a0\u04a2";
    public static final ATN _ATN = ATNSimulator.deserialize(_serializedATN
            .toCharArray());
    static {
        _decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
    }
}