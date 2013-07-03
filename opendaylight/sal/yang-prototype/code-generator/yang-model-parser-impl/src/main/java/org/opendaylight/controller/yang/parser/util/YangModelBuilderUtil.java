/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/eplv10.html
 */
package org.opendaylight.controller.yang.parser.util;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Stack;

import org.antlr.v4.runtime.tree.ParseTree;
import org.opendaylight.controller.antlrv4.code.gen.*;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Argument_stmtContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Base_stmtContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Bit_stmtContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Bits_specificationContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Config_argContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Config_stmtContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Decimal64_specificationContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Default_stmtContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Description_stmtContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Enum_specificationContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Enum_stmtContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Error_app_tag_stmtContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Error_message_stmtContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Fraction_digits_stmtContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Identityref_specificationContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Leafref_specificationContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Length_stmtContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Mandatory_argContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Mandatory_stmtContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Max_elements_stmtContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Max_value_argContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Min_elements_stmtContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Min_value_argContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Must_stmtContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Numerical_restrictionsContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Ordered_by_argContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Ordered_by_stmtContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Path_stmtContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Pattern_stmtContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Position_stmtContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Presence_stmtContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Range_stmtContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Reference_stmtContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Refine_anyxml_stmtsContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Refine_choice_stmtsContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Refine_container_stmtsContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Refine_leaf_list_stmtsContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Refine_leaf_stmtsContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Refine_list_stmtsContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Refine_pomContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Refine_stmtContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Require_instance_argContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Require_instance_stmtContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Status_argContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Status_stmtContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.StringContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.String_restrictionsContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Type_body_stmtsContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Units_stmtContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Value_stmtContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.When_stmtContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Yin_element_argContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Yin_element_stmtContext;
import org.opendaylight.controller.yang.common.QName;
import org.opendaylight.controller.yang.model.api.MustDefinition;
import org.opendaylight.controller.yang.model.api.RevisionAwareXPath;
import org.opendaylight.controller.yang.model.api.SchemaPath;
import org.opendaylight.controller.yang.model.api.Status;
import org.opendaylight.controller.yang.model.api.TypeDefinition;
import org.opendaylight.controller.yang.model.api.UnknownSchemaNode;
import org.opendaylight.controller.yang.model.api.type.BinaryTypeDefinition;
import org.opendaylight.controller.yang.model.api.type.BitsTypeDefinition;
import org.opendaylight.controller.yang.model.api.type.BitsTypeDefinition.Bit;
import org.opendaylight.controller.yang.model.api.type.EnumTypeDefinition;
import org.opendaylight.controller.yang.model.api.type.EnumTypeDefinition.EnumPair;
import org.opendaylight.controller.yang.model.api.type.IntegerTypeDefinition;
import org.opendaylight.controller.yang.model.api.type.LengthConstraint;
import org.opendaylight.controller.yang.model.api.type.PatternConstraint;
import org.opendaylight.controller.yang.model.api.type.RangeConstraint;
import org.opendaylight.controller.yang.model.api.type.StringTypeDefinition;
import org.opendaylight.controller.yang.model.api.type.UnsignedIntegerTypeDefinition;
import org.opendaylight.controller.yang.model.util.BaseConstraints;
import org.opendaylight.controller.yang.model.util.BaseTypes;
import org.opendaylight.controller.yang.model.util.BinaryType;
import org.opendaylight.controller.yang.model.util.BitsType;
import org.opendaylight.controller.yang.model.util.Decimal64;
import org.opendaylight.controller.yang.model.util.EnumerationType;
import org.opendaylight.controller.yang.model.util.ExtendedType;
import org.opendaylight.controller.yang.model.util.InstanceIdentifier;
import org.opendaylight.controller.yang.model.util.Int16;
import org.opendaylight.controller.yang.model.util.Int32;
import org.opendaylight.controller.yang.model.util.Int64;
import org.opendaylight.controller.yang.model.util.Int8;
import org.opendaylight.controller.yang.model.util.Leafref;
import org.opendaylight.controller.yang.model.util.RevisionAwareXPathImpl;
import org.opendaylight.controller.yang.model.util.StringType;
import org.opendaylight.controller.yang.model.util.Uint16;
import org.opendaylight.controller.yang.model.util.Uint32;
import org.opendaylight.controller.yang.model.util.Uint64;
import org.opendaylight.controller.yang.model.util.Uint8;
import org.opendaylight.controller.yang.model.util.UnknownType;
import org.opendaylight.controller.yang.parser.builder.api.Builder;
import org.opendaylight.controller.yang.parser.builder.api.ConfigNode;
import org.opendaylight.controller.yang.parser.builder.api.SchemaNodeBuilder;
import org.opendaylight.controller.yang.parser.builder.api.TypeDefinitionBuilder;
import org.opendaylight.controller.yang.parser.builder.impl.ChoiceBuilder;
import org.opendaylight.controller.yang.parser.builder.impl.ChoiceCaseBuilder;
import org.opendaylight.controller.yang.parser.builder.impl.ConstraintsBuilder;
import org.opendaylight.controller.yang.parser.builder.impl.UnionTypeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class YangModelBuilderUtil {
    private static final Logger logger = LoggerFactory.getLogger(YangModelBuilderUtil.class);

    private YangModelBuilderUtil() {
    }

    /**
     * Parse given tree and get first string value.
     *
     * @param treeNode
     *            tree to parse
     * @return first string value from given tree
     */
    public static String stringFromNode(final ParseTree treeNode) {
        final String result = "";
        for (int i = 0; i < treeNode.getChildCount(); ++i) {
            if (treeNode.getChild(i) instanceof StringContext) {
                final StringContext context = (StringContext) treeNode.getChild(i);
                if (context != null) {
                    return context.getChild(0).getText().replace("\"", "");
                }
            }
        }
        return result;
    }

    /**
     * Parse 'description', 'reference' and 'status' statements and fill in
     * given builder.
     *
     * @param ctx
     *            context to parse
     * @param builder
     *            builder to fill in with parsed statements
     */
    public static void parseSchemaNodeArgs(final ParseTree ctx, final SchemaNodeBuilder builder) {
        for (int i = 0; i < ctx.getChildCount(); i++) {
            final ParseTree child = ctx.getChild(i);
            if (child instanceof Description_stmtContext) {
                final String desc = stringFromNode(child);
                builder.setDescription(desc);
            } else if (child instanceof Reference_stmtContext) {
                final String ref = stringFromNode(child);
                builder.setReference(ref);
            } else if (child instanceof Status_stmtContext) {
                final Status status = parseStatus((Status_stmtContext) child);
                builder.setStatus(status);
            }
        }
    }

    /**
     * Parse given context and return its value;
     *
     * @param ctx
     *            status context
     * @return value parsed from context
     */
    public static Status parseStatus(final Status_stmtContext ctx) {
        Status result = null;
        for (int i = 0; i < ctx.getChildCount(); i++) {
            ParseTree statusArg = ctx.getChild(i);
            if (statusArg instanceof Status_argContext) {
                String statusArgStr = stringFromNode(statusArg);
                if ("current".equals(statusArgStr)) {
                    result = Status.CURRENT;
                } else if ("deprecated".equals(statusArgStr)) {
                    result = Status.DEPRECATED;
                } else if ("obsolete".equals(statusArgStr)) {
                    result = Status.OBSOLETE;
                } else {
                    logger.warn("Invalid 'status' statement: " + statusArgStr);
                }
            }
        }
        return result;
    }

    /**
     * Parse given tree and returns units statement as string.
     *
     * @param ctx
     *            context to parse
     * @return value of units statement as string or null if there is no units
     *         statement
     */
    public static String parseUnits(final ParseTree ctx) {
        String units = null;
        for (int i = 0; i < ctx.getChildCount(); i++) {
            ParseTree child = ctx.getChild(i);
            if (child instanceof Units_stmtContext) {
                units = stringFromNode(child);
                break;
            }
        }
        return units;
    }

    /**
     * Create SchemaPath from actualPath and names.
     *
     * @param actualPath
     *            current position in model
     * @param namespace
     * @param revision
     * @param prefix
     * @param names
     * @return SchemaPath object.
     */
    public static SchemaPath createActualSchemaPath(final List<String> actualPath, final URI namespace,
            final Date revision, final String prefix, final String... names) {
        final List<QName> path = new ArrayList<QName>();
        QName qname;
        // start from index 1 - module name omited
        for (int i = 1; i < actualPath.size(); i++) {
            qname = new QName(namespace, revision, prefix, actualPath.get(i));
            path.add(qname);
        }
        for (String name : names) {
            qname = new QName(namespace, revision, prefix, name);
            path.add(qname);
        }
        return new SchemaPath(path, true);
    }

    /**
     * Create SchemaPath from given string.
     *
     * @param augmentPath
     *            string representation of path
     * @return SchemaPath object
     */
    public static SchemaPath parseAugmentPath(final String augmentPath) {
        final boolean absolute = augmentPath.startsWith("/");
        final String[] splittedPath = augmentPath.split("/");
        List<QName> path = new ArrayList<QName>();
        QName name;
        for (String pathElement : splittedPath) {
            if (pathElement.length() > 0) {
                String[] splittedElement = pathElement.split(":");
                if (splittedElement.length == 1) {
                    name = new QName(null, null, null, splittedElement[0]);
                } else {
                    name = new QName(null, null, splittedElement[0], splittedElement[1]);
                }
                path.add(name);
            }
        }
        return new SchemaPath(path, absolute);
    }

    /**
     * Create java.util.List of QName objects from given key definition as
     * string.
     *
     * @param keyDefinition
     *            key definition as string
     * @param namespace
     *            current namespace
     * @param revision
     *            current revision
     * @param prefix
     *            current prefix
     * @return YANG list key as java.util.List of QName objects
     */
    public static List<QName> createListKey(final String keyDefinition, final URI namespace, final Date revision,
            final String prefix) {
        List<QName> key = new ArrayList<QName>();
        String[] splittedKey = keyDefinition.split(" ");

        QName qname = null;
        for (String keyElement : splittedKey) {
            if (keyElement.length() != 0) {
                qname = new QName(namespace, revision, prefix, keyElement);
                key.add(qname);
            }
        }
        return key;
    }

    /**
     * Parse given type body of enumeration statement.
     *
     * @param ctx
     *            type body context to parse
     * @param path
     *            actual position in YANG model
     * @param namespace
     * @param revision
     * @param prefix
     * @return List of EnumPair object parsed from given context
     */
    private static List<EnumTypeDefinition.EnumPair> getEnumConstants(final Type_body_stmtsContext ctx,
            final List<String> path, final URI namespace, final Date revision, final String prefix) {
        List<EnumTypeDefinition.EnumPair> enumConstants = new ArrayList<EnumTypeDefinition.EnumPair>();

        for (int j = 0; j < ctx.getChildCount(); j++) {
            ParseTree enumSpecChild = ctx.getChild(j);
            if (enumSpecChild instanceof Enum_specificationContext) {
                int highestValue = -1;
                for (int k = 0; k < enumSpecChild.getChildCount(); k++) {
                    ParseTree enumChild = enumSpecChild.getChild(k);
                    if (enumChild instanceof Enum_stmtContext) {
                        EnumPair enumPair = createEnumPair((Enum_stmtContext) enumChild, highestValue, path, namespace,
                                revision, prefix);
                        if (enumPair.getValue() > highestValue) {
                            highestValue = enumPair.getValue();
                        }
                        enumConstants.add(enumPair);
                    }
                }
            }
        }
        return enumConstants;
    }

    /**
     * Parse enum statement context
     *
     * @param ctx
     *            enum statement context
     * @param highestValue
     *            current highest value in enumeration
     * @param path
     *            actual position in YANG model
     * @param namespace
     * @param revision
     * @param prefix
     * @return EnumPair object parsed from given context
     */
    private static EnumTypeDefinition.EnumPair createEnumPair(final Enum_stmtContext ctx, final int highestValue,
            final List<String> path, final URI namespace, final Date revision, final String prefix) {
        final String name = stringFromNode(ctx);
        final QName qname = new QName(namespace, revision, prefix, name);
        Integer value = null;

        String description = null;
        String reference = null;
        Status status = null;

        List<String> enumPairPath = new ArrayList<String>(path);
        enumPairPath.add(name);

        for (int i = 0; i < ctx.getChildCount(); i++) {
            ParseTree child = ctx.getChild(i);
            if (child instanceof Value_stmtContext) {
                String valueStr = stringFromNode(child);
                value = Integer.valueOf(valueStr);
            } else if (child instanceof Description_stmtContext) {
                description = stringFromNode(child);
            } else if (child instanceof Reference_stmtContext) {
                reference = stringFromNode(child);
            } else if (child instanceof Status_stmtContext) {
                status = parseStatus((Status_stmtContext) child);
            }
        }

        if (value == null) {
            value = highestValue + 1;
        }
        if (value < -2147483648 || value > 2147483647) {
            throw new YangParseException(ctx.getStart().getLine(), "Error on enum '" + name
                    + "': the enum value MUST be in the range from -2147483648 to 2147483647, but was: " + value);
        }

        EnumPairImpl result = new EnumPairImpl();
        result.qname = qname;
        result.path = createActualSchemaPath(enumPairPath, namespace, revision, prefix);
        result.description = description;
        result.reference = reference;
        result.status = status;
        result.name = name;
        result.value = value;
        return result;
    }

    /**
     * Internal implementation of EnumPair.
     */
    private static class EnumPairImpl implements EnumTypeDefinition.EnumPair {
        private QName qname;
        private SchemaPath path;
        private String description;
        private String reference;
        private Status status;
        private List<UnknownSchemaNode> extensionSchemaNodes = Collections.emptyList();
        private String name;
        private Integer value;

        @Override
        public QName getQName() {
            return qname;
        }

        @Override
        public SchemaPath getPath() {
            return path;
        }

        @Override
        public String getDescription() {
            return description;
        }

        @Override
        public String getReference() {
            return reference;
        }

        @Override
        public Status getStatus() {
            return status;
        }

        @Override
        public List<UnknownSchemaNode> getUnknownSchemaNodes() {
            return extensionSchemaNodes;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public Integer getValue() {
            return value;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((qname == null) ? 0 : qname.hashCode());
            result = prime * result + ((path == null) ? 0 : path.hashCode());
            result = prime * result + ((extensionSchemaNodes == null) ? 0 : extensionSchemaNodes.hashCode());
            result = prime * result + ((name == null) ? 0 : name.hashCode());
            result = prime * result + ((value == null) ? 0 : value.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            EnumPairImpl other = (EnumPairImpl) obj;
            if (qname == null) {
                if (other.qname != null) {
                    return false;
                }
            } else if (!qname.equals(other.qname)) {
                return false;
            }
            if (path == null) {
                if (other.path != null) {
                    return false;
                }
            } else if (!path.equals(other.path)) {
                return false;
            }
            if (extensionSchemaNodes == null) {
                if (other.extensionSchemaNodes != null) {
                    return false;
                }
            } else if (!extensionSchemaNodes.equals(other.extensionSchemaNodes)) {
                return false;
            }
            if (name == null) {
                if (other.name != null) {
                    return false;
                }
            } else if (!name.equals(other.name)) {
                return false;
            }
            if (value == null) {
                if (other.value != null) {
                    return false;
                }
            } else if (!value.equals(other.value)) {
                return false;
            }
            return true;
        }

        @Override
        public String toString() {
            return EnumTypeDefinition.EnumPair.class.getSimpleName() + "[name=" + name + ", value=" + value + "]";
        }
    }

    /**
     * Get and parse range from given type body context.
     *
     * @param ctx
     *            type body context to parse
     * @return List of RangeConstraint created from this context
     */
    private static List<RangeConstraint> getRangeConstraints(final Type_body_stmtsContext ctx) {
        List<RangeConstraint> rangeConstraints = Collections.emptyList();
        outer: for (int j = 0; j < ctx.getChildCount(); j++) {
            ParseTree numRestrChild = ctx.getChild(j);
            if (numRestrChild instanceof Numerical_restrictionsContext) {
                for (int k = 0; k < numRestrChild.getChildCount(); k++) {
                    ParseTree rangeChild = numRestrChild.getChild(k);
                    if (rangeChild instanceof Range_stmtContext) {
                        rangeConstraints = parseRangeConstraints((Range_stmtContext) rangeChild);
                        break outer;
                    }
                }
            }
        }
        return rangeConstraints;
    }

    /**
     * Parse given range context.
     *
     * @param ctx
     *            range context to parse
     * @return List of RangeConstraints parsed from this context
     */
    private static List<RangeConstraint> parseRangeConstraints(final Range_stmtContext ctx) {
        final int line = ctx.getStart().getLine();
        List<RangeConstraint> rangeConstraints = new ArrayList<RangeConstraint>();
        String description = null;
        String reference = null;

        for (int i = 0; i < ctx.getChildCount(); i++) {
            ParseTree child = ctx.getChild(i);
            if (child instanceof Description_stmtContext) {
                description = stringFromNode(child);
            } else if (child instanceof Reference_stmtContext) {
                reference = stringFromNode(child);
            }
        }

        String rangeStr = stringFromNode(ctx);
        String trimmed = rangeStr.replace(" ", "");
        String[] splittedRange = trimmed.split("\\|");
        for (String rangeDef : splittedRange) {
            String[] splittedRangeDef = rangeDef.split("\\.\\.");
            Number min;
            Number max;
            if (splittedRangeDef.length == 1) {
                min = max = parseNumberConstraintValue(splittedRangeDef[0], line);
            } else {
                min = parseNumberConstraintValue(splittedRangeDef[0], line);
                max = parseNumberConstraintValue(splittedRangeDef[1], line);
            }
            RangeConstraint range = BaseConstraints.rangeConstraint(min, max, description, reference);
            rangeConstraints.add(range);
        }

        return rangeConstraints;
    }

    /**
     * Get and parse length from given type body context.
     *
     * @param ctx
     *            type body context to parse
     * @return List of LengthConstraint created from this context
     */
    private static List<LengthConstraint> getLengthConstraints(final Type_body_stmtsContext ctx) {
        List<LengthConstraint> lengthConstraints = Collections.emptyList();
        outer: for (int j = 0; j < ctx.getChildCount(); j++) {
            ParseTree stringRestrChild = ctx.getChild(j);
            if (stringRestrChild instanceof String_restrictionsContext) {
                for (int k = 0; k < stringRestrChild.getChildCount(); k++) {
                    ParseTree lengthChild = stringRestrChild.getChild(k);
                    if (lengthChild instanceof Length_stmtContext) {
                        lengthConstraints = parseLengthConstraints((Length_stmtContext) lengthChild);
                        break outer;
                    }
                }
            }
        }
        return lengthConstraints;
    }

    /**
     * Parse given length context.
     *
     * @param ctx
     *            length context to parse
     * @return List of LengthConstraints parsed from this context
     */
    private static List<LengthConstraint> parseLengthConstraints(final Length_stmtContext ctx) {
        final int line = ctx.getStart().getLine();
        List<LengthConstraint> lengthConstraints = new ArrayList<LengthConstraint>();
        String description = null;
        String reference = null;

        for (int i = 0; i < ctx.getChildCount(); i++) {
            ParseTree child = ctx.getChild(i);
            if (child instanceof Description_stmtContext) {
                description = stringFromNode(child);
            } else if (child instanceof Reference_stmtContext) {
                reference = stringFromNode(child);
            }
        }

        String lengthStr = stringFromNode(ctx);
        String trimmed = lengthStr.replace(" ", "");
        String[] splittedRange = trimmed.split("\\|");
        for (String rangeDef : splittedRange) {
            String[] splittedRangeDef = rangeDef.split("\\.\\.");
            Number min;
            Number max;
            if (splittedRangeDef.length == 1) {
                min = max = parseNumberConstraintValue(splittedRangeDef[0], line);
            } else {
                min = parseNumberConstraintValue(splittedRangeDef[0], line);
                max = parseNumberConstraintValue(splittedRangeDef[1], line);
            }
            LengthConstraint range = BaseConstraints.lengthConstraint(min, max, description, reference);
            lengthConstraints.add(range);
        }

        return lengthConstraints;
    }

    /**
     * @param value
     *            value to parse
     * @return wrapper object of primitive java type or UnknownBoundaryNumber if
     *         type is one of special YANG values 'min' or 'max'
     */
    private static Number parseNumberConstraintValue(final String value, final int line) {
        Number result = null;
        if ("min".equals(value) || "max".equals(value)) {
            result = new UnknownBoundaryNumber(value);
        } else {
            try {
                result = Long.valueOf(value);
            } catch (NumberFormatException e) {
                throw new YangParseException(line, "Unable to parse range value '" + value + "'.", e);
            }
        }
        return result;
    }

    /**
     * Parse type body and return pattern constraints.
     *
     * @param ctx
     *            type body
     * @return list of pattern constraints
     */
    private static List<PatternConstraint> getPatternConstraint(final Type_body_stmtsContext ctx) {
        List<PatternConstraint> patterns = new ArrayList<PatternConstraint>();

        for (int j = 0; j < ctx.getChildCount(); j++) {
            ParseTree stringRestrChild = ctx.getChild(j);
            if (stringRestrChild instanceof String_restrictionsContext) {
                for (int k = 0; k < stringRestrChild.getChildCount(); k++) {
                    ParseTree lengthChild = stringRestrChild.getChild(k);
                    if (lengthChild instanceof Pattern_stmtContext) {
                        patterns.add(parsePatternConstraint((Pattern_stmtContext) lengthChild));
                    }
                }
            }
        }
        return patterns;
    }

    /**
     * Internal helper method.
     *
     * @param ctx
     *            pattern context
     * @return PatternConstraint object
     */
    private static PatternConstraint parsePatternConstraint(final Pattern_stmtContext ctx) {
        String description = null;
        String reference = null;
        for (int i = 0; i < ctx.getChildCount(); i++) {
            ParseTree child = ctx.getChild(i);
            if (child instanceof Description_stmtContext) {
                description = stringFromNode(child);
            } else if (child instanceof Reference_stmtContext) {
                reference = stringFromNode(child);
            }
        }
        String pattern = patternStringFromNode(ctx);
        return BaseConstraints.patternConstraint(pattern, description, reference);
    }

    /**
     * Parse given context and return pattern value.
     *
     * @param ctx
     *            context to parse
     * @return pattern value as String
     */
    public static String patternStringFromNode(final Pattern_stmtContext ctx) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < ctx.getChildCount(); ++i) {
            ParseTree child = ctx.getChild(i);
            if (child instanceof StringContext) {
                for (int j = 0; j < child.getChildCount(); j++) {
                    if (j % 2 == 0) {
                        String patternToken = child.getChild(j).getText();
                        result.append(patternToken.substring(1, patternToken.length() - 1));
                    }
                }
            }
        }
        return result.toString();
    }

    /**
     * Get fraction digits value from type body.
     *
     * @param ctx
     *            type body context to parse
     * @return 'fraction-digits' value if present in given context, null
     *         otherwise
     */
    private static Integer getFractionDigits(Type_body_stmtsContext ctx) {
        Integer result = null;
        for (int j = 0; j < ctx.getChildCount(); j++) {
            ParseTree dec64specChild = ctx.getChild(j);
            if (dec64specChild instanceof Decimal64_specificationContext) {
                result = parseFractionDigits((Decimal64_specificationContext) dec64specChild);
            }
        }
        return result;
    }

    /**
     * Parse decimal64 fraction-digits value.
     *
     * @param ctx
     *            decimal64 context
     * @return fraction-digits value as Integer
     */
    private static Integer parseFractionDigits(Decimal64_specificationContext ctx) {
        Integer result = null;
        for (int k = 0; k < ctx.getChildCount(); k++) {
            ParseTree fdChild = ctx.getChild(k);
            if (fdChild instanceof Fraction_digits_stmtContext) {
                String value = stringFromNode(fdChild);
                try {
                    result = Integer.valueOf(value);
                } catch (NumberFormatException e) {
                    throw new YangParseException(ctx.getStart().getLine(), "Unable to parse fraction digits value '"
                            + value + "'.", e);
                }
            }
        }
        return result;
    }

    /**
     * Internal helper method for parsing bit statements from given type body
     * context.
     *
     * @param ctx
     *            type body context to parse
     * @param actualPath
     *            current position in YANG model
     * @param namespace
     * @param revision
     * @param prefix
     * @return List of Bit objects created from this context
     */
    private static List<BitsTypeDefinition.Bit> getBits(Type_body_stmtsContext ctx, List<String> actualPath,
            URI namespace, Date revision, String prefix) {
        final List<BitsTypeDefinition.Bit> bits = new ArrayList<BitsTypeDefinition.Bit>();
        for (int j = 0; j < ctx.getChildCount(); j++) {
            ParseTree bitsSpecChild = ctx.getChild(j);
            if (bitsSpecChild instanceof Bits_specificationContext) {
                long highestPosition = -1;
                for (int k = 0; k < bitsSpecChild.getChildCount(); k++) {
                    ParseTree bitChild = bitsSpecChild.getChild(k);
                    if (bitChild instanceof Bit_stmtContext) {
                        Bit bit = parseBit((Bit_stmtContext) bitChild, highestPosition, actualPath, namespace,
                                revision, prefix);
                        if (bit.getPosition() > highestPosition) {
                            highestPosition = bit.getPosition();
                        }
                        bits.add(bit);
                    }
                }
            }
        }
        return bits;
    }

    /**
     * Internal helper method for parsing bit context.
     *
     * @param ctx
     *            bit statement context to parse
     * @param highestPosition
     *            current highest position in bits type
     * @param actualPath
     *            current position in YANG model
     * @param namespace
     * @param revision
     * @param prefix
     * @return Bit object parsed from this context
     */
    private static BitsTypeDefinition.Bit parseBit(final Bit_stmtContext ctx, long highestPosition,
            List<String> actualPath, final URI namespace, final Date revision, final String prefix) {
        String name = stringFromNode(ctx);
        final QName qname = new QName(namespace, revision, prefix, name);
        Long position = null;

        String description = null;
        String reference = null;
        Status status = Status.CURRENT;

        Stack<String> bitPath = new Stack<String>();
        bitPath.addAll(actualPath);
        bitPath.add(name);

        SchemaPath schemaPath = createActualSchemaPath(bitPath, namespace, revision, prefix);

        for (int i = 0; i < ctx.getChildCount(); i++) {
            ParseTree child = ctx.getChild(i);
            if (child instanceof Position_stmtContext) {
                String positionStr = stringFromNode(child);
                position = Long.valueOf(positionStr);
            } else if (child instanceof Description_stmtContext) {
                description = stringFromNode(child);
            } else if (child instanceof Reference_stmtContext) {
                reference = stringFromNode(child);
            } else if (child instanceof Status_stmtContext) {
                status = parseStatus((Status_stmtContext) child);
            }
        }

        if (position == null) {
            position = highestPosition + 1;
        }
        if (position < 0 || position > 4294967295L) {
            throw new YangParseException(ctx.getStart().getLine(), "Error on bit '" + name
                    + "': the position value MUST be in the range 0 to 4294967295");
        }

        final List<UnknownSchemaNode> unknownNodes = Collections.emptyList();
        return new BitImpl(position, qname, schemaPath, description, reference, status, unknownNodes);
    }

    /**
     * Parse 'ordered-by' statement.
     *
     * The 'ordered-by' statement defines whether the order of entries within a
     * list are determined by the user or the system. The argument is one of the
     * strings "system" or "user". If not present, order defaults to "system".
     *
     * @param childNode
     *            Ordered_by_stmtContext
     * @return true, if ordered-by contains value 'user', false otherwise
     */
    public static boolean parseUserOrdered(Ordered_by_stmtContext childNode) {
        boolean result = false;
        for (int j = 0; j < childNode.getChildCount(); j++) {
            ParseTree orderArg = childNode.getChild(j);
            if (orderArg instanceof Ordered_by_argContext) {
                String orderStr = stringFromNode(orderArg);
                if ("system".equals(orderStr)) {
                    result = false;
                } else if ("user".equals(orderStr)) {
                    result = true;
                } else {
                    logger.warn("Invalid 'orderedby' statement.");
                }
            }
        }
        return result;
    }

    public static Boolean getConfig(final ParseTree ctx, final Builder parent, final String moduleName, final int line) {
        Boolean result = null;
        // parse configuration statement
        Boolean configuration = null;
        for (int i = 0; i < ctx.getChildCount(); i++) {
            ParseTree child = ctx.getChild(i);
            if (child instanceof Config_stmtContext) {
                configuration = parseConfig((Config_stmtContext) child);
                break;
            }
        }

        // If 'config' is not specified, the default is the same as the parent
        // schema node's 'config' value
        if (configuration == null) {
            if (parent instanceof ConfigNode) {
                Boolean parentConfig = ((ConfigNode) parent).isConfiguration();
                // If the parent node is a rpc input or output, it can has
                // config set to null
                result = parentConfig == null ? true : parentConfig;
            } else if (parent instanceof ChoiceCaseBuilder) {
                // If the parent node is a 'case' node, the value is the same as
                // the 'case' node's parent 'choice' node
                ChoiceCaseBuilder choiceCase = (ChoiceCaseBuilder) parent;
                Builder choice = choiceCase.getParent();
                Boolean parentConfig = null;
                if(choice instanceof ChoiceBuilder) {
                    parentConfig = ((ChoiceBuilder)choice).isConfiguration();
                } else {
                    parentConfig = true;
                }
                result = parentConfig;
            } else {
                result = true;
            }
        } else {
            // Check first: if a node has 'config' set to 'false', no node
            // underneath it can have 'config' set to 'true'
            if (parent instanceof ConfigNode) {
                Boolean parentConfig = ((ConfigNode) parent).isConfiguration();
                if (parentConfig == false && configuration == true) {
                    throw new YangParseException(moduleName, line,
                            "Can not set 'config' to 'true' if parent node has 'config' set to 'false'");
                }
            }
            result = configuration;
        }

        return result;
    }

    /**
     * Parse config statement.
     *
     * @param ctx
     *            config context to parse.
     * @return true if given context contains string 'true', false otherwise
     */
    private static Boolean parseConfig(final Config_stmtContext ctx) {
        Boolean result = null;
        if (ctx != null) {
            for (int i = 0; i < ctx.getChildCount(); ++i) {
                final ParseTree configContext = ctx.getChild(i);
                if (configContext instanceof Config_argContext) {
                    final String value = stringFromNode(configContext);
                    if ("true".equals(value)) {
                        result = true;
                        break;
                    } else if ("false".equals(value)) {
                        result = false;
                        break;
                    } else {
                        throw new YangParseException(ctx.getStart().getLine(),
                                "Failed to parse 'config' statement value: '" + value + "'.");
                    }
                }
            }
        }
        return result;
    }

    /**
     * Parse type body and create UnknownType definition.
     *
     * @param typedefQName
     *            qname of current type
     * @param ctx
     *            type body
     * @param actualPath
     * @param namespace
     * @param revision
     * @param prefix
     * @param parent
     * @return UnknownType object with constraints from parsed type body
     */
    public static TypeDefinition<?> parseUnknownTypeWithBody(final QName typedefQName,
            final Type_body_stmtsContext ctx, final List<String> actualPath, final URI namespace, final Date revision,
            final String prefix, final Builder parent) {
        String typeName = typedefQName.getLocalName();

        UnknownType.Builder unknownType = new UnknownType.Builder(typedefQName);

        if (ctx != null) {
            List<RangeConstraint> rangeStatements = getRangeConstraints(ctx);
            List<LengthConstraint> lengthStatements = getLengthConstraints(ctx);
            List<PatternConstraint> patternStatements = getPatternConstraint(ctx);
            Integer fractionDigits = getFractionDigits(ctx);

            if (parent instanceof TypeDefinitionBuilder) {
                TypeDefinitionBuilder typedef = (TypeDefinitionBuilder) parent;
                typedef.setRanges(rangeStatements);
                typedef.setLengths(lengthStatements);
                typedef.setPatterns(patternStatements);
                typedef.setFractionDigits(fractionDigits);
                return unknownType.build();
            } else {
                TypeDefinition<?> baseType = unknownType.build();
                TypeDefinition<?> result = null;
                QName qname = new QName(namespace, revision, prefix, typeName);
                ExtendedType.Builder typeBuilder = null;

                SchemaPath schemaPath = createTypeSchemaPath(actualPath, namespace, revision, prefix, typeName, false,
                        false);
                typeBuilder = new ExtendedType.Builder(qname, baseType, "", "", schemaPath);

                typeBuilder.ranges(rangeStatements);
                typeBuilder.lengths(lengthStatements);
                typeBuilder.patterns(patternStatements);
                typeBuilder.fractionDigits(fractionDigits);

                result = typeBuilder.build();

                return result;
            }
        }

        return unknownType.build();
    }

    /**
     * Create TypeDefinition object based on given type name and type body.
     *
     * @param moduleName
     *            current module name
     * @param typeName
     *            name of type
     * @param typeBody
     *            type body context
     * @param actualPath
     *            current path in schema
     * @param namespace
     *            current namespace
     * @param revision
     *            current revision
     * @param prefix
     *            current prefix
     * @param parent
     *            parent builder
     * @return TypeDefinition object based on parsed values.
     */
    public static TypeDefinition<?> parseTypeWithBody(final String moduleName, final String typeName,
            final Type_body_stmtsContext typeBody, final List<String> actualPath, final URI namespace,
            final Date revision, final String prefix, final Builder parent) {
        TypeDefinition<?> baseType = null;

        Integer fractionDigits = getFractionDigits(typeBody);
        List<LengthConstraint> lengthStatements = getLengthConstraints(typeBody);
        List<PatternConstraint> patternStatements = getPatternConstraint(typeBody);
        List<RangeConstraint> rangeStatements = getRangeConstraints(typeBody);

        TypeConstraints constraints = new TypeConstraints(moduleName, typeBody.getStart().getLine());
        constraints.addFractionDigits(fractionDigits);
        constraints.addLengths(lengthStatements);
        constraints.addPatterns(patternStatements);
        constraints.addRanges(rangeStatements);

        SchemaPath baseTypePathFinal = createTypeSchemaPath(actualPath, namespace, revision, prefix, typeName, true,
                true);
        SchemaPath baseTypePath = createTypeSchemaPath(actualPath, namespace, revision, prefix, typeName, true, false);

        if ("decimal64".equals(typeName)) {
            if (rangeStatements.isEmpty()) {
                return new Decimal64(baseTypePathFinal, fractionDigits);
            }
            Decimal64 decimalType = new Decimal64(baseTypePath, fractionDigits);
            constraints.addRanges(decimalType.getRangeStatements());
            baseType = decimalType;
        } else if (typeName.startsWith("int")) {
            IntegerTypeDefinition intType = null;
            if ("int8".equals(typeName)) {
                intType = new Int8(baseTypePath);
            } else if ("int16".equals(typeName)) {
                intType = new Int16(baseTypePath);
            } else if ("int32".equals(typeName)) {
                intType = new Int32(baseTypePath);
            } else if ("int64".equals(typeName)) {
                intType = new Int64(baseTypePath);
            }
            constraints.addRanges(intType.getRangeStatements());
            baseType = intType;
        } else if (typeName.startsWith("uint")) {
            UnsignedIntegerTypeDefinition uintType = null;
            if ("uint8".equals(typeName)) {
                uintType = new Uint8(baseTypePath);
            } else if ("uint16".equals(typeName)) {
                uintType = new Uint16(baseTypePath);
            } else if ("uint32".equals(typeName)) {
                uintType = new Uint32(baseTypePath);
            } else if ("uint64".equals(typeName)) {
                uintType = new Uint64(baseTypePath);
            }
            constraints.addRanges(uintType.getRangeStatements());
            baseType = uintType;
        } else if ("enumeration".equals(typeName)) {
            List<EnumTypeDefinition.EnumPair> enumConstants = getEnumConstants(typeBody, actualPath, namespace,
                    revision, prefix);
            return new EnumerationType(baseTypePathFinal, enumConstants);
        } else if ("string".equals(typeName)) {
            StringTypeDefinition stringType = new StringType(baseTypePath);
            constraints.addLengths(stringType.getLengthStatements());
            baseType = stringType;
        } else if ("bits".equals(typeName)) {
            return new BitsType(baseTypePathFinal, getBits(typeBody, actualPath, namespace, revision, prefix));
        } else if ("leafref".equals(typeName)) {
            final String path = parseLeafrefPath(typeBody);
            final boolean absolute = path.startsWith("/");
            RevisionAwareXPath xpath = new RevisionAwareXPathImpl(path, absolute);
            return new Leafref(baseTypePathFinal, xpath);
        } else if ("binary".equals(typeName)) {
            BinaryTypeDefinition binaryType = new BinaryType(baseTypePath);
            constraints.addLengths(binaryType.getLengthConstraints());
            baseType = binaryType;
        } else if ("instance-identifier".equals(typeName)) {
            boolean requireInstance = isRequireInstance(typeBody);
            baseType = new InstanceIdentifier(baseTypePath, null, requireInstance);
        }

        if (parent instanceof TypeDefinitionBuilder && !(parent instanceof UnionTypeBuilder)) {
            TypeDefinitionBuilder typedef = (TypeDefinitionBuilder) parent;
            typedef.setRanges(constraints.getRange());
            typedef.setLengths(constraints.getLength());
            typedef.setPatterns(constraints.getPatterns());
            typedef.setFractionDigits(constraints.getFractionDigits());
            return baseType;
        }

        TypeDefinition<?> result = null;
        QName qname = new QName(namespace, revision, prefix, typeName);
        ExtendedType.Builder typeBuilder = null;

        SchemaPath schemaPath = createTypeSchemaPath(actualPath, namespace, revision, prefix, typeName, false, false);
        typeBuilder = new ExtendedType.Builder(qname, baseType, "", "", schemaPath);

        typeBuilder.ranges(constraints.getRange());
        typeBuilder.lengths(constraints.getLength());
        typeBuilder.patterns(constraints.getPatterns());
        typeBuilder.fractionDigits(constraints.getFractionDigits());

        result = typeBuilder.build();
        return result;
    }

    /**
     * Create SchemaPath object from given path list with namespace, revision
     * and prefix based on given values.
     *
     * @param actualPath
     *            current position in model
     * @param namespace
     * @param revision
     * @param prefix
     * @param typeName
     * @param isBaseYangType
     *            if this is base yang type
     * @param isBaseYangTypeFinal
     *            if this is base yang type without restrictions
     * @return SchemaPath object.
     */
    private static SchemaPath createTypeSchemaPath(final List<String> actualPath, final URI namespace,
            final Date revision, final String prefix, final String typeName, final boolean isBaseYangType,
            final boolean isBaseYangTypeFinal) {
        List<String> typePath = new ArrayList<String>(actualPath);
        if (isBaseYangType && !isBaseYangTypeFinal) {
            typePath.add(typeName);
        }

        final List<QName> path = new ArrayList<QName>();
        QName qname;
        // start from index 1 -> module name omited
        for (int i = 1; i < typePath.size(); i++) {
            qname = new QName(namespace, revision, prefix, typePath.get(i));
            path.add(qname);
        }
        QName typeQName;
        if (isBaseYangType) {
            typeQName = new QName(BaseTypes.BaseTypesNamespace, typeName);
        } else {
            typeQName = new QName(namespace, revision, prefix, typeName);
        }
        path.add(typeQName);
        return new SchemaPath(path, true);
    }

    /**
     * Parse given context and find identityref base value.
     *
     * @param ctx
     *            type body
     * @return identityref base value as String
     */
    public static String getIdentityrefBase(Type_body_stmtsContext ctx) {
        String result = null;
        outer: for (int i = 0; i < ctx.getChildCount(); i++) {
            ParseTree child = ctx.getChild(i);
            if (child instanceof Identityref_specificationContext) {
                for (int j = 0; j < child.getChildCount(); j++) {
                    ParseTree baseArg = child.getChild(j);
                    if (baseArg instanceof Base_stmtContext) {
                        result = stringFromNode(baseArg);
                        break outer;
                    }
                }
            }
        }
        return result;
    }

    /**
     * Parse type body statement and find require-instance value.
     *
     * @param ctx
     *            type body context
     * @return require-instance value
     */
    private static boolean isRequireInstance(Type_body_stmtsContext ctx) {
        for (int i = 0; i < ctx.getChildCount(); i++) {
            ParseTree child = ctx.getChild(i);
            if (child instanceof Require_instance_stmtContext) {
                for (int j = 0; j < child.getChildCount(); j++) {
                    ParseTree reqArg = child.getChild(j);
                    if (reqArg instanceof Require_instance_argContext) {
                        return Boolean.valueOf(stringFromNode(reqArg));
                    }
                }
            }
        }
        return false;
    }

    /**
     * Parse type body statement and find leafref path.
     *
     * @param ctx
     *            type body context
     * @return leafref path as String
     */
    private static String parseLeafrefPath(Type_body_stmtsContext ctx) {
        for (int i = 0; i < ctx.getChildCount(); i++) {
            ParseTree child = ctx.getChild(i);
            if (child instanceof Leafref_specificationContext) {
                for (int j = 0; j < child.getChildCount(); j++) {
                    ParseTree leafRefSpec = child.getChild(j);
                    if (leafRefSpec instanceof Path_stmtContext) {
                        return stringFromNode(leafRefSpec);
                    }
                }
            }
        }
        return null;
    }

    /**
     * Internal helper method for parsing must statement.
     *
     * @param ctx
     *            Must_stmtContext
     * @return MustDefinition object based on parsed context
     */
    public static MustDefinition parseMust(final YangParser.Must_stmtContext ctx) {
        StringBuilder mustText = new StringBuilder();
        String description = null;
        String reference = null;
        String errorAppTag = null;
        String errorMessage = null;
        for (int i = 0; i < ctx.getChildCount(); ++i) {
            ParseTree child = ctx.getChild(i);
            if (child instanceof StringContext) {
                final StringContext context = (StringContext) child;
                if (context.getChildCount() == 1) {
                    String mustPart = context.getChild(0).getText();
                    // trim start and end quotation
                    mustText.append(mustPart.substring(1, mustPart.length() - 1));
                } else {
                    for (int j = 0; j < context.getChildCount(); j++) {
                        String mustPart = context.getChild(j).getText();
                        if (j == 0) {
                            mustText.append(mustPart.substring(0, mustPart.length() - 1));
                            continue;
                        }
                        if (j % 2 == 0) {
                            mustText.append(mustPart.substring(1));
                        }
                    }
                }
            } else if (child instanceof Description_stmtContext) {
                description = stringFromNode(child);
            } else if (child instanceof Reference_stmtContext) {
                reference = stringFromNode(child);
            } else if (child instanceof Error_app_tag_stmtContext) {
                errorAppTag = stringFromNode(child);
            } else if (child instanceof Error_message_stmtContext) {
                errorMessage = stringFromNode(child);
            }
        }

        MustDefinition must = new MustDefinitionImpl(mustText.toString(), description, reference, errorAppTag,
                errorMessage);
        return must;
    }

    /**
     * Parse given context and set constraints to constraints builder.
     *
     * @param ctx
     *            context to parse
     * @param constraints
     *            ConstraintsBuilder to fill
     */
    public static void parseConstraints(final ParseTree ctx, final ConstraintsBuilder constraints) {
        for (int i = 0; i < ctx.getChildCount(); ++i) {
            final ParseTree childNode = ctx.getChild(i);
            if (childNode instanceof Max_elements_stmtContext) {
                Integer max = parseMaxElements((Max_elements_stmtContext) childNode);
                constraints.setMaxElements(max);
            } else if (childNode instanceof Min_elements_stmtContext) {
                Integer min = parseMinElements((Min_elements_stmtContext) childNode);
                constraints.setMinElements(min);
            } else if (childNode instanceof Must_stmtContext) {
                MustDefinition must = parseMust((Must_stmtContext) childNode);
                constraints.addMustDefinition(must);
            } else if (childNode instanceof Mandatory_stmtContext) {
                for (int j = 0; j < childNode.getChildCount(); j++) {
                    ParseTree mandatoryTree = ctx.getChild(j);
                    if (mandatoryTree instanceof Mandatory_argContext) {
                        Boolean mandatory = Boolean.valueOf(stringFromNode(mandatoryTree));
                        constraints.setMandatory(mandatory);
                    }
                }
            } else if (childNode instanceof When_stmtContext) {
                constraints.addWhenCondition(stringFromNode(childNode));
            }
        }
    }

    private static Integer parseMinElements(Min_elements_stmtContext ctx) {
        Integer result = null;
        try {
            for (int j = 0; j < ctx.getChildCount(); j++) {
                ParseTree minArg = ctx.getChild(j);
                if (minArg instanceof Min_value_argContext) {
                    result = Integer.valueOf(stringFromNode(minArg));
                }
            }
            if (result == null) {
                throw new IllegalArgumentException();
            }
            return result;
        } catch (Exception e) {
            throw new YangParseException(ctx.getStart().getLine(), "Failed to parse min-elements.", e);
        }
    }

    private static Integer parseMaxElements(Max_elements_stmtContext ctx) {
        Integer result = null;
        try {
            for (int j = 0; j < ctx.getChildCount(); j++) {
                ParseTree maxArg = ctx.getChild(j);
                if (maxArg instanceof Max_value_argContext) {
                    result = Integer.valueOf(stringFromNode(maxArg));
                }
            }
            if (result == null) {
                throw new IllegalArgumentException();
            }
            return result;
        } catch (Exception e) {
            throw new YangParseException(ctx.getStart().getLine(), "Failed to parse max-elements.", e);
        }
    }

    /**
     * Parse given context and return yin value.
     *
     * @param ctx
     *            context to parse
     * @return true if value is 'true', false otherwise
     */
    public static boolean parseYinValue(Argument_stmtContext ctx) {
        boolean yinValue = false;
        outer: for (int j = 0; j < ctx.getChildCount(); j++) {
            ParseTree yin = ctx.getChild(j);
            if (yin instanceof Yin_element_stmtContext) {
                for (int k = 0; k < yin.getChildCount(); k++) {
                    ParseTree yinArg = yin.getChild(k);
                    if (yinArg instanceof Yin_element_argContext) {
                        String yinString = stringFromNode(yinArg);
                        if ("true".equals(yinString)) {
                            yinValue = true;
                            break outer;
                        }
                    }
                }
            }
        }
        return yinValue;
    }

    /**
     * Check this base type.
     *
     * @param typeName
     *            base YANG type name
     * @param moduleName
     *            name of current module
     * @param line
     *            line in module
     * @throws YangParseException
     *             if this is one of YANG type which MUST contain additional
     *             informations in its body
     */
    public static void checkMissingBody(final String typeName, final String moduleName, final int line)
            throws YangParseException {
        if ("decimal64".equals(typeName)) {
            throw new YangParseException(moduleName, line,
                    "The 'fraction-digits' statement MUST be present if the type is 'decimal64'.");
        } else if ("identityref".equals(typeName)) {
            throw new YangParseException(moduleName, line,
                    "The 'base' statement MUST be present if the type is 'identityref'.");
        } else if ("leafref".equals(typeName)) {
            throw new YangParseException(moduleName, line,
                    "The 'path' statement MUST be present if the type is 'leafref'.");
        } else if ("bits".equals(typeName)) {
            throw new YangParseException(moduleName, line, "The 'bit' statement MUST be present if the type is 'bits'.");
        } else if ("enumeration".equals(typeName)) {
            throw new YangParseException(moduleName, line,
                    "The 'enum' statement MUST be present if the type is 'enumeration'.");
        }
    }

    /**
     * Parse refine statement.
     *
     * @param refineCtx
     *            refine statement
     * @param line
     *            current line in yang model
     * @return RefineHolder object representing this refine statement
     */
    public static RefineHolder parseRefine(Refine_stmtContext refineCtx) {
        final String refineTarget = stringFromNode(refineCtx);
        final RefineHolder refine = new RefineHolder(refineCtx.getStart().getLine(), refineTarget);
        for (int j = 0; j < refineCtx.getChildCount(); j++) {
            ParseTree refinePom = refineCtx.getChild(j);
            if (refinePom instanceof Refine_pomContext) {
                for (int k = 0; k < refinePom.getChildCount(); k++) {
                    ParseTree refineStmt = refinePom.getChild(k);
                    parseRefineDefault(refine, refineStmt);

                    if (refineStmt instanceof Refine_leaf_stmtsContext) {
                        parseRefine(refine, (Refine_leaf_stmtsContext) refineStmt);
                    } else if (refineStmt instanceof Refine_container_stmtsContext) {
                        parseRefine(refine, (Refine_container_stmtsContext) refineStmt);
                    } else if (refineStmt instanceof Refine_list_stmtsContext) {
                        parseRefine(refine, (Refine_list_stmtsContext) refineStmt);
                    } else if (refineStmt instanceof Refine_leaf_list_stmtsContext) {
                        parseRefine(refine, (Refine_leaf_list_stmtsContext) refineStmt);
                    } else if (refineStmt instanceof Refine_choice_stmtsContext) {
                        parseRefine(refine, (Refine_choice_stmtsContext) refineStmt);
                    } else if (refineStmt instanceof Refine_anyxml_stmtsContext) {
                        parseRefine(refine, (Refine_anyxml_stmtsContext) refineStmt);
                    }
                }
            }
        }
        return refine;
    }

    private static void parseRefineDefault(RefineHolder refine, ParseTree refineStmt) {
        for (int i = 0; i < refineStmt.getChildCount(); i++) {
            ParseTree refineArg = refineStmt.getChild(i);
            if (refineArg instanceof Description_stmtContext) {
                String description = stringFromNode(refineArg);
                refine.setDescription(description);
            } else if (refineArg instanceof Reference_stmtContext) {
                String reference = stringFromNode(refineArg);
                refine.setReference(reference);
            } else if (refineArg instanceof Config_stmtContext) {
                Boolean config = parseConfig((Config_stmtContext) refineArg);
                refine.setConfiguration(config);
            }
        }
    }

    private static RefineHolder parseRefine(RefineHolder refine, Refine_leaf_stmtsContext refineStmt) {
        for (int i = 0; i < refineStmt.getChildCount(); i++) {
            ParseTree refineArg = refineStmt.getChild(i);
            if (refineArg instanceof Default_stmtContext) {
                String defaultStr = stringFromNode(refineArg);
                refine.setDefaultStr(defaultStr);
            } else if (refineArg instanceof Mandatory_stmtContext) {
                for (int j = 0; j < refineArg.getChildCount(); j++) {
                    ParseTree mandatoryTree = refineArg.getChild(j);
                    if (mandatoryTree instanceof Mandatory_argContext) {
                        Boolean mandatory = Boolean.valueOf(stringFromNode(mandatoryTree));
                        refine.setMandatory(mandatory);
                    }
                }
            } else if (refineArg instanceof Must_stmtContext) {
                MustDefinition must = parseMust((Must_stmtContext) refineArg);
                refine.setMust(must);

            }
        }
        return refine;
    }

    private static RefineHolder parseRefine(RefineHolder refine, Refine_container_stmtsContext refineStmt) {
        for (int m = 0; m < refineStmt.getChildCount(); m++) {
            ParseTree refineArg = refineStmt.getChild(m);
            if (refineArg instanceof Must_stmtContext) {
                MustDefinition must = parseMust((Must_stmtContext) refineArg);
                refine.setMust(must);
            } else if (refineArg instanceof Presence_stmtContext) {
                refine.setPresence(true);
            }
        }
        return refine;
    }

    private static RefineHolder parseRefine(RefineHolder refine, Refine_list_stmtsContext refineStmt) {
        for (int m = 0; m < refineStmt.getChildCount(); m++) {
            ParseTree refineArg = refineStmt.getChild(m);
            if (refineArg instanceof Must_stmtContext) {
                MustDefinition must = parseMust((Must_stmtContext) refineArg);
                refine.setMust(must);
            } else if (refineArg instanceof Max_elements_stmtContext) {
                Integer max = parseMaxElements((Max_elements_stmtContext) refineArg);
                refine.setMaxElements(max);
            } else if (refineArg instanceof Min_elements_stmtContext) {
                Integer min = parseMinElements((Min_elements_stmtContext) refineArg);
                refine.setMinElements(min);
            }
        }
        return refine;
    }

    private static RefineHolder parseRefine(RefineHolder refine, Refine_leaf_list_stmtsContext refineStmt) {
        for (int m = 0; m < refineStmt.getChildCount(); m++) {
            ParseTree refineArg = refineStmt.getChild(m);
            if (refineArg instanceof Must_stmtContext) {
                MustDefinition must = parseMust((Must_stmtContext) refineArg);
                refine.setMust(must);
            } else if (refineArg instanceof Max_elements_stmtContext) {
                Integer max = parseMaxElements((Max_elements_stmtContext) refineArg);
                refine.setMaxElements(max);
            } else if (refineArg instanceof Min_elements_stmtContext) {
                Integer min = parseMinElements((Min_elements_stmtContext) refineArg);
                refine.setMinElements(min);
            }
        }
        return refine;
    }

    private static RefineHolder parseRefine(RefineHolder refine, Refine_choice_stmtsContext refineStmt) {
        for (int i = 0; i < refineStmt.getChildCount(); i++) {
            ParseTree refineArg = refineStmt.getChild(i);
            if (refineArg instanceof Default_stmtContext) {
                String defaultStr = stringFromNode(refineArg);
                refine.setDefaultStr(defaultStr);
            } else if (refineArg instanceof Mandatory_stmtContext) {
                for (int j = 0; j < refineArg.getChildCount(); j++) {
                    ParseTree mandatoryTree = refineArg.getChild(j);
                    if (mandatoryTree instanceof Mandatory_argContext) {
                        Boolean mandatory = Boolean.valueOf(stringFromNode(mandatoryTree));
                        refine.setMandatory(mandatory);
                    }
                }
            }
        }
        return refine;
    }

    private static RefineHolder parseRefine(RefineHolder refine, Refine_anyxml_stmtsContext refineStmt) {
        for (int i = 0; i < refineStmt.getChildCount(); i++) {
            ParseTree refineArg = refineStmt.getChild(i);
            if (refineArg instanceof Must_stmtContext) {
                MustDefinition must = parseMust((Must_stmtContext) refineArg);
                refine.setMust(must);
            } else if (refineArg instanceof Mandatory_stmtContext) {
                for (int j = 0; j < refineArg.getChildCount(); j++) {
                    ParseTree mandatoryTree = refineArg.getChild(j);
                    if (mandatoryTree instanceof Mandatory_argContext) {
                        Boolean mandatory = Boolean.valueOf(stringFromNode(mandatoryTree));
                        refine.setMandatory(mandatory);
                    }
                }
            }
        }
        return refine;
    }

}
