lexer grammar YangLexer;

@header {
package org.opendaylight.controller.antlrv4.code.gen;
}

tokens{
    SEMICOLON,
    LEFT_BRACE,
    RIGHT_BRACE
}


PLUS : '+'-> pushMode(VALUE_MODE);
WS : [ \n\r\t] -> skip;
LINE_COMMENT :  ('//' (~( '\r' | '\n' )*)) -> skip;

START_BLOCK_COMMENT : '/*' ->pushMode(BLOCK_COMMENT_MODE), skip ;


SEMICOLON : ';' ->type(SEMICOLON);
LEFT_BRACE : '{' ->type(LEFT_BRACE);
RIGHT_BRACE : '}' ->type(RIGHT_BRACE);

YIN_ELEMENT_KEYWORD : 'yin-element'-> pushMode(VALUE_MODE);
YANG_VERSION_KEYWORD: 'yang-version'-> pushMode(VALUE_MODE);
WHEN_KEYWORD : 'when'-> pushMode(VALUE_MODE);
VALUE_KEYWORD : 'value'-> pushMode(VALUE_MODE);
USES_KEYWORD : 'uses'-> pushMode(VALUE_MODE);
UNITS_KEYWORD : 'units'-> pushMode(VALUE_MODE);
UNIQUE_KEYWORD : 'unique'-> pushMode(VALUE_MODE);
TYPEDEF_KEYWORD : 'typedef'-> pushMode(VALUE_MODE);
TYPE_KEYWORD : 'type'-> pushMode(VALUE_MODE);
SUBMODULE_KEYWORD : 'submodule'-> pushMode(VALUE_MODE);
STATUS_KEYWORD : 'status'-> pushMode(VALUE_MODE);
RPC_KEYWORD : 'rpc'-> pushMode(VALUE_MODE);
REVISION_DATE_KEYWORD : 'revision-date'-> pushMode(VALUE_MODE);
REVISION_KEYWORD : 'revision'-> pushMode(VALUE_MODE);
REQUIRE_INSTANCE_KEYWORD : 'require-instance'-> pushMode(VALUE_MODE);
REFINE_KEYWORD : 'refine'-> pushMode(VALUE_MODE);
REFERENCE_KEYWORD : 'reference'-> pushMode(VALUE_MODE);
RANGE_KEYWORD : 'range'-> pushMode(VALUE_MODE);
PRESENCE_KEYWORD : 'presence'-> pushMode(VALUE_MODE);
PREFIX_KEYWORD : 'prefix'-> pushMode(VALUE_MODE);
POSITION_KEYWORD : 'position'-> pushMode(VALUE_MODE);
PATTERN_KEYWORD : 'pattern'-> pushMode(VALUE_MODE);
PATH_KEYWORD : 'path'-> pushMode(VALUE_MODE);
OUTPUT_KEYWORD : 'output';
ORGANIZATION_KEYWORD: 'organization'-> pushMode(VALUE_MODE);
ORDERED_BY_KEYWORD : 'ordered-by'-> pushMode(VALUE_MODE);
NOTIFICATION_KEYWORD: 'notification'-> pushMode(VALUE_MODE);
NAMESPACE_KEYWORD : 'namespace'-> pushMode(VALUE_MODE);
MUST_KEYWORD : 'must'-> pushMode(VALUE_MODE);
MODULE_KEYWORD : 'module'-> pushMode(VALUE_MODE);
MIN_ELEMENTS_KEYWORD : 'min-elements'-> pushMode(VALUE_MODE);
MAX_ELEMENTS_KEYWORD : 'max-elements'-> pushMode(VALUE_MODE);
MANDATORY_KEYWORD : 'mandatory'-> pushMode(VALUE_MODE);
LIST_KEYWORD : 'list'-> pushMode(VALUE_MODE);
LENGTH_KEYWORD : 'length'-> pushMode(VALUE_MODE);
LEAF_LIST_KEYWORD : 'leaf-list'-> pushMode(VALUE_MODE);
LEAF_KEYWORD : 'leaf'-> pushMode(VALUE_MODE);
KEY_KEYWORD : 'key'-> pushMode(VALUE_MODE);
INPUT_KEYWORD : 'input';
INCLUDE_KEYWORD : 'include'-> pushMode(VALUE_MODE);
IMPORT_KEYWORD : 'import'-> pushMode(VALUE_MODE);
IF_FEATURE_KEYWORD : 'if-feature'-> pushMode(VALUE_MODE);
IDENTITY_KEYWORD : 'identity'-> pushMode(VALUE_MODE);
GROUPING_KEYWORD : 'grouping'-> pushMode(VALUE_MODE);
FRACTION_DIGITS_KEYWORD : 'fraction-digits'-> pushMode(VALUE_MODE);
FEATURE_KEYWORD : 'feature'-> pushMode(VALUE_MODE);
DEVIATE_KEYWORD : 'deviate'-> pushMode(VALUE_MODE);
DEVIATION_KEYWORD : 'deviation'-> pushMode(VALUE_MODE);
EXTENSION_KEYWORD : 'extension'-> pushMode(VALUE_MODE);
ERROR_MESSAGE_KEYWORD : 'error-message'-> pushMode(VALUE_MODE);
ERROR_APP_TAG_KEYWORD : 'error-app-tag'-> pushMode(VALUE_MODE);
ENUM_KEYWORD : 'enum'-> pushMode(VALUE_MODE);
DESCRIPTION_KEYWORD : 'description'-> pushMode(VALUE_MODE);
DEFAULT_KEYWORD : 'default'-> pushMode(VALUE_MODE);
CONTAINER_KEYWORD : 'container'-> pushMode(VALUE_MODE);
CONTACT_KEYWORD : 'contact'-> pushMode(VALUE_MODE);
CONFIG_KEYWORD : 'config'-> pushMode(VALUE_MODE);
CHOICE_KEYWORD: 'choice'-> pushMode(VALUE_MODE);
CASE_KEYWORD : 'case'-> pushMode(VALUE_MODE);
BIT_KEYWORD : 'bit'-> pushMode(VALUE_MODE);
BELONGS_TO_KEYWORD : 'belongs-to'-> pushMode(VALUE_MODE);
BASE_KEYWORD : 'base'-> pushMode(VALUE_MODE);
AUGMENT_KEYWORD : 'augment'-> pushMode(VALUE_MODE);
ARGUMENT_KEYWORD : 'argument'-> pushMode(VALUE_MODE);
ANYXML_KEYWORD : 'anyxml'-> pushMode(VALUE_MODE);

IDENTIFIER : [/.a-zA-Z_0-9\-][a-zA-Z0-9_\-.:]* -> pushMode(VALUE_MODE);

mode VALUE_MODE;

fragment ESC :  '\\' (["\\/bfnrt] | UNICODE) ;
fragment UNICODE : 'u' HEX HEX HEX HEX ;
fragment HEX : [0-9a-fA-F] ;
          
END_IDENTIFIER_SEMICOLON : ';' -> type(SEMICOLON),popMode;
END_IDENTIFIER_LEFT_BRACE : '{' ->type(LEFT_BRACE), popMode;
 
fragment SUB_STRING : ('"' (ESC | ~["])*'"') | ('\'' (ESC | ~['])*'\'') ;

STRING: (SUB_STRING |  (~( '\r' | '\n' | ' ' | ';' | '{' )+)) ->popMode;// IDENTIFIER ;
S : [ \n\r\t] -> skip;    

mode BLOCK_COMMENT_MODE;
END_BLOCK_COMMENT : '*/' -> popMode,skip;
BLOCK_COMMENT :  . ->more,skip;