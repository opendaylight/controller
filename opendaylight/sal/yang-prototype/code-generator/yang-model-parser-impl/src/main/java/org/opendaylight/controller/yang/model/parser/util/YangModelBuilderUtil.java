/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/eplv10.html
 */
package org.opendaylight.controller.yang.model.parser.util;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Stack;

import org.antlr.v4.runtime.tree.ParseTree;
import org.opendaylight.controller.antlrv4.code.gen.YangParser;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Bit_stmtContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Bits_specificationContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Config_argContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Config_stmtContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Decimal64_specificationContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Description_stmtContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Enum_specificationContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Enum_stmtContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Fraction_digits_stmtContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Leafref_specificationContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Length_stmtContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Mandatory_argContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Mandatory_stmtContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Max_elements_stmtContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Min_elements_stmtContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Must_stmtContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Numerical_restrictionsContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Ordered_by_argContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Ordered_by_stmtContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Path_stmtContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Pattern_stmtContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Position_stmtContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Range_stmtContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Reference_stmtContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Require_instance_argContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Require_instance_stmtContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Status_argContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Status_stmtContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.StringContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.String_restrictionsContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Type_body_stmtsContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Units_stmtContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.When_stmtContext;
import org.opendaylight.controller.yang.common.QName;
import org.opendaylight.controller.yang.model.api.RevisionAwareXPath;
import org.opendaylight.controller.yang.model.api.SchemaPath;
import org.opendaylight.controller.yang.model.api.Status;
import org.opendaylight.controller.yang.model.api.TypeDefinition;
import org.opendaylight.controller.yang.model.api.UnknownSchemaNode;
import org.opendaylight.controller.yang.model.api.type.BitsTypeDefinition;
import org.opendaylight.controller.yang.model.api.type.EnumTypeDefinition;
import org.opendaylight.controller.yang.model.api.type.LengthConstraint;
import org.opendaylight.controller.yang.model.api.type.PatternConstraint;
import org.opendaylight.controller.yang.model.api.type.RangeConstraint;
import org.opendaylight.controller.yang.model.api.type.BitsTypeDefinition.Bit;
import org.opendaylight.controller.yang.model.parser.builder.api.SchemaNodeBuilder;
import org.opendaylight.controller.yang.model.parser.builder.impl.ConstraintsBuilder;
import org.opendaylight.controller.yang.model.util.BaseConstraints;
import org.opendaylight.controller.yang.model.util.BinaryType;
import org.opendaylight.controller.yang.model.util.BitsType;
import org.opendaylight.controller.yang.model.util.EnumerationType;
import org.opendaylight.controller.yang.model.util.InstanceIdentifier;
import org.opendaylight.controller.yang.model.util.Leafref;
import org.opendaylight.controller.yang.model.util.RevisionAwareXPathImpl;
import org.opendaylight.controller.yang.model.util.StringType;
import org.opendaylight.controller.yang.model.util.UnknownType;
import org.opendaylight.controller.yang.model.util.YangTypesConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class YangModelBuilderUtil {

    private static final Logger logger = LoggerFactory
            .getLogger(YangModelBuilderUtil.class);

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
                final StringContext context = (StringContext) treeNode
                        .getChild(i);
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
    public static void parseSchemaNodeArgs(ParseTree ctx,
            SchemaNodeBuilder builder) {
        for (int i = 0; i < ctx.getChildCount(); i++) {
            ParseTree child = ctx.getChild(i);
            if (child instanceof Description_stmtContext) {
                String desc = stringFromNode(child);
                builder.setDescription(desc);
            } else if (child instanceof Reference_stmtContext) {
                String ref = stringFromNode(child);
                builder.setReference(ref);
            } else if (child instanceof Status_stmtContext) {
                Status status = parseStatus((Status_stmtContext) child);
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
    public static Status parseStatus(Status_stmtContext ctx) {
        for (int i = 0; i < ctx.getChildCount(); i++) {
            ParseTree statusArg = ctx.getChild(i);
            if (statusArg instanceof Status_argContext) {
                String statusArgStr = stringFromNode(statusArg);
                if (statusArgStr.equals("current")) {
                    return Status.CURRENT;
                } else if (statusArgStr.equals("deprecated")) {
                    return Status.DEPRECATED;
                } else if (statusArgStr.equals("obsolete")) {
                    return Status.OBSOLETE;
                } else {
                    logger.warn("Invalid 'status' statement: " + statusArgStr);
                }
            }
        }
        return null;
    }

    /**
     * Parse given tree and returns units statement as string.
     *
     * @param ctx
     *            context to parse
     * @return value of units statement as string or null if there is no units
     *         statement
     */
    public static String parseUnits(ParseTree ctx) {
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
     * Create SchemaPath object from given path list with namespace, revision
     * and prefix based on given values.
     *
     * @param actualPath
     * @param namespace
     * @param revision
     * @param prefix
     * @return SchemaPath object.
     */
    public static SchemaPath createActualSchemaPath(List<String> actualPath,
            URI namespace, Date revision, String prefix) {
        final List<QName> path = new ArrayList<QName>();
        QName qname;
        for (String pathElement : actualPath) {
            qname = new QName(namespace, revision, prefix, pathElement);
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
    public static SchemaPath parseAugmentPath(String augmentPath) {
        boolean absolute = augmentPath.startsWith("/");
        String[] splittedPath = augmentPath.split("/");
        List<QName> path = new ArrayList<QName>();
        QName name;
        for (String pathElement : splittedPath) {
            if (pathElement.length() > 0) {
                String[] splittedElement = pathElement.split(":");
                if (splittedElement.length == 1) {
                    name = new QName(null, null, null, splittedElement[0]);
                } else {
                    name = new QName(null, null, splittedElement[0],
                            splittedElement[1]);
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
    public static List<QName> createListKey(String keyDefinition,
            URI namespace, Date revision, String prefix) {
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

    private static List<EnumTypeDefinition.EnumPair> getEnumConstants(
            Type_body_stmtsContext ctx, List<String> path, URI namespace,
            Date revision, String prefix) {
        List<EnumTypeDefinition.EnumPair> enumConstants = new ArrayList<EnumTypeDefinition.EnumPair>();

        out: for (int j = 0; j < ctx.getChildCount(); j++) {
            ParseTree enumSpecChild = ctx.getChild(j);
            if (enumSpecChild instanceof Enum_specificationContext) {
                for (int k = 0; k < enumSpecChild.getChildCount(); k++) {
                    ParseTree enumChild = enumSpecChild.getChild(k);
                    if (enumChild instanceof Enum_stmtContext) {
                        enumConstants.add(createEnumPair(
                                (Enum_stmtContext) enumChild, k, path,
                                namespace, revision, prefix));
                        if (k == enumSpecChild.getChildCount() - 1) {
                            break out;
                        }
                    }
                }
            }
        }
        return enumConstants;
    }

    private static EnumTypeDefinition.EnumPair createEnumPair(
            Enum_stmtContext ctx, final int value, List<String> path,
            final URI namespace, final Date revision, final String prefix) {
        final String name = stringFromNode(ctx);
        final QName qname = new QName(namespace, revision, prefix, name);
        String description = null;
        String reference = null;
        Status status = null;
        List<String> enumPairPath = new ArrayList<String>(path);
        enumPairPath.add(name);

        for (int i = 0; i < ctx.getChildCount(); i++) {
            ParseTree child = ctx.getChild(i);
            if (child instanceof Description_stmtContext) {
                description = stringFromNode(child);
            } else if (child instanceof Reference_stmtContext) {
                reference = stringFromNode(child);
            } else if (child instanceof Status_stmtContext) {
                status = parseStatus((Status_stmtContext) child);
            }
        }

        EnumPairImpl result = new EnumPairImpl();
        result.qname = qname;
        result.path = createActualSchemaPath(enumPairPath, namespace, revision,
                prefix);
        result.description = description;
        result.reference = reference;
        result.status = status;
        // TODO: extensionSchemaNodes
        result.name = name;
        result.value = value;
        return result;
    }

    private static class EnumPairImpl implements EnumTypeDefinition.EnumPair {

        private QName qname;
        private SchemaPath path;
        private String description;
        private String reference;
        private Status status;
        private List<UnknownSchemaNode> extensionSchemaNodes = Collections
                .emptyList();
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
            result = prime * result
                    + ((description == null) ? 0 : description.hashCode());
            result = prime * result
                    + ((reference == null) ? 0 : reference.hashCode());
            result = prime * result
                    + ((status == null) ? 0 : status.hashCode());
            result = prime
                    * result
                    + ((extensionSchemaNodes == null) ? 0
                            : extensionSchemaNodes.hashCode());
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
            if (description == null) {
                if (other.description != null) {
                    return false;
                }
            } else if (!description.equals(other.description)) {
                return false;
            }
            if (reference == null) {
                if (other.reference != null) {
                    return false;
                }
            } else if (!reference.equals(other.reference)) {
                return false;
            }
            if (status == null) {
                if (other.status != null) {
                    return false;
                }
            } else if (!status.equals(other.status)) {
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
            return EnumTypeDefinition.EnumPair.class.getSimpleName() + "[name="
                    + name + ", value=" + value + "]";
        }
    };

    private static List<RangeConstraint> getRangeConstraints(
            Type_body_stmtsContext ctx) {
        final List<RangeConstraint> rangeConstraints = new ArrayList<RangeConstraint>();
        for (int j = 0; j < ctx.getChildCount(); j++) {
            ParseTree numRestrChild = ctx.getChild(j);
            if (numRestrChild instanceof Numerical_restrictionsContext) {
                for (int k = 0; k < numRestrChild.getChildCount(); k++) {
                    ParseTree rangeChild = numRestrChild.getChild(k);
                    if (rangeChild instanceof Range_stmtContext) {
                        rangeConstraints
                                .addAll(parseRangeConstraints((Range_stmtContext) rangeChild));
                        break;
                    }
                }
            }
        }
        return rangeConstraints;
    }

    private static List<RangeConstraint> parseRangeConstraints(
            Range_stmtContext ctx) {
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
            Long min;
            Long max;
            if (splittedRangeDef.length == 1) {
                min = max = parseRangeValue(splittedRangeDef[0]);
            } else {
                min = parseRangeValue(splittedRangeDef[0]);
                max = parseRangeValue(splittedRangeDef[1]);
            }
            RangeConstraint range = BaseConstraints.rangeConstraint(min, max,
                    description, reference);
            rangeConstraints.add(range);
        }

        return rangeConstraints;
    }

    private static List<LengthConstraint> getLengthConstraints(
            Type_body_stmtsContext ctx) {
        List<LengthConstraint> lengthConstraints = new ArrayList<LengthConstraint>();
        for (int j = 0; j < ctx.getChildCount(); j++) {
            ParseTree stringRestrChild = ctx.getChild(j);
            if (stringRestrChild instanceof String_restrictionsContext) {
                for (int k = 0; k < stringRestrChild.getChildCount(); k++) {
                    ParseTree lengthChild = stringRestrChild.getChild(k);
                    if (lengthChild instanceof Length_stmtContext) {
                        lengthConstraints
                                .addAll(parseLengthConstraints((Length_stmtContext) lengthChild));
                    }
                }
            }
        }
        return lengthConstraints;
    }

    private static List<LengthConstraint> parseLengthConstraints(
            Length_stmtContext ctx) {
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
            Long min;
            Long max;
            if (splittedRangeDef.length == 1) {
                min = max = parseRangeValue(splittedRangeDef[0]);
            } else {
                min = parseRangeValue(splittedRangeDef[0]);
                max = parseRangeValue(splittedRangeDef[1]);
            }
            LengthConstraint range = BaseConstraints.lengthConstraint(min, max,
                    description, reference);
            lengthConstraints.add(range);
        }

        return lengthConstraints;
    }

    private static Long parseRangeValue(String value) {
        Long result = null;
        if (value.equals("min")) {
            result = Long.MIN_VALUE;
        } else if (value.equals("max")) {
            result = Long.MAX_VALUE;
        } else {
            result = Long.valueOf(value);
        }
        return result;
    }

    private static List<PatternConstraint> getPatternConstraint(
            Type_body_stmtsContext ctx) {
        List<PatternConstraint> patterns = new ArrayList<PatternConstraint>();

        out: for (int j = 0; j < ctx.getChildCount(); j++) {
            ParseTree stringRestrChild = ctx.getChild(j);
            if (stringRestrChild instanceof String_restrictionsContext) {
                for (int k = 0; k < stringRestrChild.getChildCount(); k++) {
                    ParseTree lengthChild = stringRestrChild.getChild(k);
                    if (lengthChild instanceof Pattern_stmtContext) {
                        patterns.add(parsePatternConstraint((Pattern_stmtContext) lengthChild));
                        if (k == lengthChild.getChildCount() - 1) {
                            break out;
                        }
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
    private static PatternConstraint parsePatternConstraint(
            Pattern_stmtContext ctx) {
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
        return BaseConstraints.patternConstraint(pattern, description,
                reference);
    }

    public static String patternStringFromNode(final Pattern_stmtContext treeNode) {
        String result = "";
        for (int i = 0; i < treeNode.getChildCount(); ++i) {
            ParseTree child = treeNode.getChild(i);
            if (child instanceof StringContext) {
                for(int j = 0; j < child.getChildCount(); j++) {
                    if(j % 2 == 0) {
                        String patternToken = child.getChild(j).getText();
                        result += patternToken.substring(1, patternToken.length()-1);
                    }
                }
            }
        }
        return result;
    }

    private static Integer getFractionDigits(Type_body_stmtsContext ctx) {
        for (int j = 0; j < ctx.getChildCount(); j++) {
            ParseTree dec64specChild = ctx.getChild(j);
            if (dec64specChild instanceof Decimal64_specificationContext) {
                return parseFractionDigits((Decimal64_specificationContext) dec64specChild);
            }
        }
        return null;
    }

    private static Integer parseFractionDigits(
            Decimal64_specificationContext ctx) {
        for (int k = 0; k < ctx.getChildCount(); k++) {
            ParseTree fdChild = ctx.getChild(k);
            if (fdChild instanceof Fraction_digits_stmtContext) {
                return Integer.valueOf(stringFromNode(fdChild));
            }
        }
        return null;
    }

    private static List<BitsTypeDefinition.Bit> getBits(
            Type_body_stmtsContext ctx, List<String> actualPath, URI namespace,
            Date revision, String prefix) {
        List<BitsTypeDefinition.Bit> bits = new ArrayList<BitsTypeDefinition.Bit>();
        for (int j = 0; j < ctx.getChildCount(); j++) {
            ParseTree bitsSpecChild = ctx.getChild(j);
            if (bitsSpecChild instanceof Bits_specificationContext) {
                for (int k = 0; k < bitsSpecChild.getChildCount(); k++) {
                    ParseTree bitChild = bitsSpecChild.getChild(k);
                    if (bitChild instanceof Bit_stmtContext) {
                        bits.add(parseBit((Bit_stmtContext) bitChild,
                                actualPath, namespace, revision, prefix));
                    }
                }
            }
        }
        return bits;
    }

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

    private static BitsTypeDefinition.Bit parseBit(final Bit_stmtContext ctx,
            List<String> actualPath, final URI namespace, final Date revision,
            final String prefix) {
        String name = stringFromNode(ctx);
        final QName qname = new QName(namespace, revision, prefix, name);
        Long position = null;

        String description = null;
        String reference = null;
        Status status = Status.CURRENT;

        Stack<String> bitPath = new Stack<String>();
        bitPath.addAll(actualPath);
        bitPath.add(name);

        SchemaPath schemaPath = createActualSchemaPath(bitPath, namespace,
                revision, prefix);

        for (int i = 0; i < ctx.getChildCount(); i++) {
            ParseTree child = ctx.getChild(i);
            if (child instanceof Position_stmtContext) {
                String positionStr = stringFromNode(child);
                position = Long.valueOf(positionStr);
                if (position < 0 || position > 4294967295L) {
                    throw new IllegalArgumentException(
                            "position value MUST be in the range 0 to 4294967295, but was: "
                                    + position);
                }
            } else if (child instanceof Description_stmtContext) {
                description = stringFromNode(child);
            } else if (child instanceof Reference_stmtContext) {
                reference = stringFromNode(child);
            } else if (child instanceof Status_stmtContext) {
                status = parseStatus((Status_stmtContext) child);
            }
        }

        // TODO: extensionDefinitions
        return createBit(qname, schemaPath, description, reference, status,
                null, position);
    }

    private static BitsTypeDefinition.Bit createBit(final QName qname,
            final SchemaPath schemaPath, final String description,
            final String reference, final Status status,
            final List<UnknownSchemaNode> extensionDefinitions,
            final Long position) {
        return new BitsTypeDefinition.Bit() {

            @Override
            public QName getQName() {
                return qname;
            }

            @Override
            public SchemaPath getPath() {
                return schemaPath;
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
                return extensionDefinitions;
            }

            @Override
            public Long getPosition() {
                return position;
            }

            @Override
            public String getName() {
                return qname.getLocalName();
            }

            @Override
            public int hashCode() {
                final int prime = 31;
                int result = 1;
                result = prime * result
                        + ((qname == null) ? 0 : qname.hashCode());
                result = prime * result
                        + ((schemaPath == null) ? 0 : schemaPath.hashCode());
                result = prime * result
                        + ((description == null) ? 0 : description.hashCode());
                result = prime * result
                        + ((reference == null) ? 0 : reference.hashCode());
                result = prime * result
                        + ((status == null) ? 0 : status.hashCode());
                result = prime * result
                        + ((position == null) ? 0 : position.hashCode());
                result = prime
                        * result
                        + ((extensionDefinitions == null) ? 0
                                : extensionDefinitions.hashCode());
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
                Bit other = (Bit) obj;
                if (qname == null) {
                    if (other.getQName() != null) {
                        return false;
                    }
                } else if (!qname.equals(other.getQName())) {
                    return false;
                }
                if (schemaPath == null) {
                    if (other.getPath() != null) {
                        return false;
                    }
                } else if (!schemaPath.equals(other.getPath())) {
                    return false;
                }
                if (description == null) {
                    if (other.getDescription() != null) {
                        return false;
                    }
                } else if (!description.equals(other.getDescription())) {
                    return false;
                }
                if (reference == null) {
                    if (other.getReference() != null) {
                        return false;
                    }
                } else if (!reference.equals(other.getReference())) {
                    return false;
                }
                if (status == null) {
                    if (other.getStatus() != null) {
                        return false;
                    }
                } else if (!status.equals(other.getStatus())) {
                    return false;
                }
                if (extensionDefinitions == null) {
                    if (other.getUnknownSchemaNodes() != null) {
                        return false;
                    }
                } else if (!extensionDefinitions.equals(other
                        .getUnknownSchemaNodes())) {
                    return false;
                }
                if (position == null) {
                    if (other.getPosition() != null) {
                        return false;
                    }
                } else if (!position.equals(other.getPosition())) {
                    return false;
                }
                return true;
            }

            @Override
            public String toString() {
                return Bit.class.getSimpleName() + "[name="
                        + qname.getLocalName() + ", position=" + position + "]";
            }
        };
    }

    /**
     * Parse orderedby statement.
     *
     * @param childNode
     *            Ordered_by_stmtContext
     * @return true, if orderedby contains value 'user' or false otherwise
     */
    public static boolean parseUserOrdered(Ordered_by_stmtContext childNode) {
        boolean result = false;
        for (int j = 0; j < childNode.getChildCount(); j++) {
            ParseTree orderArg = childNode.getChild(j);
            if (orderArg instanceof Ordered_by_argContext) {
                String orderStr = stringFromNode(orderArg);
                if (orderStr.equals("system")) {
                    result = false;
                } else if (orderStr.equals("user")) {
                    result = true;
                } else {
                    logger.warn("Invalid 'orderedby' statement.");
                }
            }
        }
        return result;
    }

    /**
     * Parse given config context and return true if it contains string 'true',
     * false otherwise.
     *
     * @param ctx
     *            config context to parse.
     * @return true if given context contains string 'true', false otherwise
     */
    public static boolean parseConfig(final Config_stmtContext ctx) {
        if (ctx != null) {
            for (int i = 0; i < ctx.getChildCount(); ++i) {
                final ParseTree configContext = ctx.getChild(i);
                if (configContext instanceof Config_argContext) {
                    final String value = stringFromNode(configContext);
                    if (value.equals("true")) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Parse given type body and creates UnknownType definition.
     *
     * @param typedefQName
     *            qname of current type
     * @param ctx
     *            type body
     * @return UnknownType object with constraints from parsed type body
     */
    public static TypeDefinition<?> parseUnknownTypeBody(QName typedefQName,
            Type_body_stmtsContext ctx) {
        UnknownType.Builder ut = new UnknownType.Builder(typedefQName);

        if (ctx != null) {
            List<RangeConstraint> rangeStatements = getRangeConstraints(ctx);
            List<LengthConstraint> lengthStatements = getLengthConstraints(ctx);
            List<PatternConstraint> patternStatements = getPatternConstraint(ctx);
            Integer fractionDigits = getFractionDigits(ctx);

            ut.rangeStatements(rangeStatements);
            ut.lengthStatements(lengthStatements);
            ut.patterns(patternStatements);
            ut.fractionDigits(fractionDigits);
        }

        return ut.build();
    }

    /**
     * Create TypeDefinition object based on given type name and type body.
     *
     * @param typeName
     *            name of type
     * @param typeBody
     *            type body
     * @param actualPath
     *            current path in schema
     * @param namespace
     *            current namespace
     * @param revision
     *            current revision
     * @param prefix
     *            current prefix
     * @return TypeDefinition object based on parsed values.
     */
    public static TypeDefinition<?> parseTypeBody(String typeName,
            Type_body_stmtsContext typeBody, List<String> actualPath,
            URI namespace, Date revision, String prefix) {
        TypeDefinition<?> type = null;

        List<RangeConstraint> rangeStatements = getRangeConstraints(typeBody);
        Integer fractionDigits = getFractionDigits(typeBody);
        List<LengthConstraint> lengthStatements = getLengthConstraints(typeBody);
        List<PatternConstraint> patternStatements = getPatternConstraint(typeBody);
        List<EnumTypeDefinition.EnumPair> enumConstants = getEnumConstants(typeBody, actualPath, namespace, revision, prefix);

        if (typeName.equals("decimal64")) {
            type = YangTypesConverter.javaTypeForBaseYangDecimal64Type(
                    rangeStatements, fractionDigits);
        } else if (typeName.startsWith("int")) {
            type = YangTypesConverter.javaTypeForBaseYangSignedIntegerType(typeName,
                    rangeStatements);
        } else if(typeName.startsWith("uint")) {
            type = YangTypesConverter.javaTypeForBaseYangUnsignedIntegerType(typeName,
                    rangeStatements);
        } else if (typeName.equals("enumeration")) {
            type = new EnumerationType(enumConstants);
        } else if (typeName.equals("string")) {
            type = new StringType(lengthStatements, patternStatements);
        } else if (typeName.equals("bits")) {
            type = new BitsType(getBits(typeBody, actualPath, namespace,
                    revision, prefix));
        } else if (typeName.equals("leafref")) {
            final String path = parseLeafrefPath(typeBody);
            final boolean absolute = path.startsWith("/");
            RevisionAwareXPath xpath = new RevisionAwareXPathImpl(path,
                    absolute);
            type = new Leafref(actualPath, namespace, revision, xpath);
        } else if (typeName.equals("binary")) {
            type = new BinaryType(null, lengthStatements, null);
        } else if (typeName.equals("instance-identifier")) {
            boolean requireInstance = isRequireInstance(typeBody);
            type = new InstanceIdentifier(null, requireInstance);
        }
        return type;
    }

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
     * Internal helper method for parsing Must_stmtContext.
     *
     * @param ctx
     *            Must_stmtContext
     * @return an array of strings with following fields: [0] must text [1]
     *         description [2] reference
     */
    public static String[] parseMust(YangParser.Must_stmtContext ctx) {
        String[] params = new String[3];

        String mustText = "";
        String description = null;
        String reference = null;
        for (int i = 0; i < ctx.getChildCount(); ++i) {
            ParseTree child = ctx.getChild(i);
            if (child instanceof StringContext) {
                final StringContext context = (StringContext) child;
                for (int j = 0; j < context.getChildCount(); j++) {
                    String mustPart = context.getChild(j).getText();
                    if (j == 0) {
                        mustText += mustPart
                                .substring(0, mustPart.length() - 1);
                        continue;
                    }
                    if (j % 2 == 0) {
                        mustText += mustPart.substring(1);
                    }
                }
            } else if (child instanceof Description_stmtContext) {
                description = stringFromNode(child);
            } else if (child instanceof Reference_stmtContext) {
                reference = stringFromNode(child);
            }
        }
        params[0] = mustText;
        params[1] = description;
        params[2] = reference;

        return params;
    }

    /**
     * Parse given tree and set constraints to given builder.
     *
     * @param ctx
     *            Context to search.
     * @param constraintsBuilder
     *            ConstraintsBuilder to fill.
     */
    public static void parseConstraints(ParseTree ctx,
            ConstraintsBuilder constraintsBuilder) {
        for (int i = 0; i < ctx.getChildCount(); ++i) {
            final ParseTree childNode = ctx.getChild(i);
            if (childNode instanceof Max_elements_stmtContext) {
                Integer max = Integer.valueOf(stringFromNode(childNode));
                constraintsBuilder.setMinElements(max);
            } else if (childNode instanceof Min_elements_stmtContext) {
                Integer min = Integer.valueOf(stringFromNode(childNode));
                constraintsBuilder.setMinElements(min);
            } else if (childNode instanceof Must_stmtContext) {
                String[] mustParams = parseMust((Must_stmtContext) childNode);
                constraintsBuilder.addMustDefinition(mustParams[0],
                        mustParams[1], mustParams[2]);
            } else if (childNode instanceof Mandatory_stmtContext) {
                for (int j = 0; j < childNode.getChildCount(); j++) {
                    ParseTree mandatoryTree = ctx.getChild(j);
                    if (mandatoryTree instanceof Mandatory_argContext) {
                        Boolean mandatory = Boolean
                                .valueOf(stringFromNode(mandatoryTree));
                        constraintsBuilder.setMandatory(mandatory);
                    }
                }
            } else if (childNode instanceof When_stmtContext) {
                constraintsBuilder.addWhenCondition(stringFromNode(childNode));
            }
        }
    }

}
