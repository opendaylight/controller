/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.model.parser.util;

import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Stack;

import org.antlr.v4.runtime.tree.ParseTree;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Bit_stmtContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Bits_specificationContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Decimal64_specificationContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Description_stmtContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Enum_specificationContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Enum_stmtContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Fraction_digits_stmtContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Length_stmtContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Numerical_restrictionsContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Pattern_stmtContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Position_stmtContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Range_stmtContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Reference_stmtContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Status_argContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Status_stmtContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.StringContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.String_restrictionsContext;
import org.opendaylight.controller.antlrv4.code.gen.YangParser.Type_body_stmtsContext;
import org.opendaylight.controller.model.api.type.BitsTypeDefinition;
import org.opendaylight.controller.model.api.type.EnumTypeDefinition;
import org.opendaylight.controller.model.api.type.LengthConstraint;
import org.opendaylight.controller.model.api.type.PatternConstraint;
import org.opendaylight.controller.model.api.type.RangeConstraint;
import org.opendaylight.controller.model.api.type.BitsTypeDefinition.Bit;
import org.opendaylight.controller.model.parser.api.SchemaNodeBuilder;
import org.opendaylight.controller.model.util.BaseConstraints;
import org.opendaylight.controller.model.util.UnknownType;
import org.opendaylight.controller.yang.common.QName;
import org.opendaylight.controller.yang.model.api.ExtensionDefinition;
import org.opendaylight.controller.yang.model.api.SchemaPath;
import org.opendaylight.controller.yang.model.api.Status;
import org.opendaylight.controller.yang.model.api.TypeDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class YangModelBuilderHelper {

    private static final Logger logger = LoggerFactory
            .getLogger(YangModelBuilderHelper.class);

    /**
     * Get 'description', 'reference' and 'status' statements and fill in
     * builder.
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
                Status status = getStatus((Status_stmtContext) child);
                builder.setStatus(status);
            }
        }
    }

    public static SchemaPath getActualSchemaPath(Stack<String> actualPath,
            URI namespace, Date revision, String prefix) {
        final List<QName> path = new ArrayList<QName>();
        QName qname;
        for (String pathElement : actualPath) {
            qname = new QName(namespace, revision, prefix, pathElement);
            path.add(qname);
        }
        return new SchemaPath(path, true);
    }

    public static Status getStatus(Status_stmtContext ctx) {
        for (int i = 0; i < ctx.getChildCount(); i++) {
            ParseTree statusArg = ctx.getChild(i);
            if (statusArg instanceof Status_argContext) {
                String statusArgStr = stringFromNode(statusArg);
                if (statusArgStr.equals("current")) {
                    return Status.CURRENT;
                } else if (statusArgStr.equals("deprecated")) {
                    return Status.DEPRECATED;
                } else if (statusArgStr.equals("obsolete")) {
                    return Status.OBSOLOTE;
                } else {
                    logger.warn("Invalid 'status' statement: " + statusArgStr);
                }
            }
        }
        return null;
    }

    public static String stringFromNode(final ParseTree treeNode) {
        final String result = "";
        for (int j = 0; j < treeNode.getChildCount(); ++j) {
            if (treeNode.getChild(j) instanceof StringContext) {
                final StringContext context = (StringContext) treeNode
                        .getChild(j);

                if (context != null) {
                    return context.getChild(0).getText().replace("\"", "");
                }
            }
        }
        return result;
    }

    public static SchemaPath parsePath(String augmentPath) {
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

    public static TypeDefinition<?> parseUnknownType(QName typedefQName,
            ParseTree ctx) {
        UnknownType.Builder ut = new UnknownType.Builder(typedefQName);
        for (int i = 0; i < ctx.getChildCount(); i++) {
            ParseTree child = ctx.getChild(i);
            if (child instanceof Type_body_stmtsContext) {
                for (int j = 0; j < child.getChildCount(); j++) {
                    ParseTree typeBodyChild = child.getChild(j);
                    // NUMERICAL RESTRICTIONS
                    if (typeBodyChild instanceof Numerical_restrictionsContext) {
                        for (int k = 0; k < typeBodyChild.getChildCount(); k++) {
                            ParseTree numRestrictionsChild = typeBodyChild
                                    .getChild(k);
                            if (numRestrictionsChild instanceof Range_stmtContext) {
                                List<RangeConstraint> ranges = parseRangeConstraints((Range_stmtContext) numRestrictionsChild);
                                ut.rangeStatements(ranges);
                            }
                        }
                        // STRING RESTRICTIONS
                    } else if (typeBodyChild instanceof String_restrictionsContext) {
                        List<PatternConstraint> patterns = new ArrayList<PatternConstraint>();
                        List<LengthConstraint> lengths = new ArrayList<LengthConstraint>();
                        for (int k = 0; k < typeBodyChild.getChildCount(); k++) {
                            ParseTree stringRestrictionsChild = typeBodyChild
                                    .getChild(k);
                            if (stringRestrictionsChild instanceof Pattern_stmtContext) {
                                patterns.add(parsePatternConstraint((Pattern_stmtContext) stringRestrictionsChild));
                            } else if (stringRestrictionsChild instanceof Length_stmtContext) {
                                lengths = parseLengthConstraints((Length_stmtContext) stringRestrictionsChild);
                            }
                        }
                        ut.patterns(patterns);
                        ut.lengthStatements(lengths);
                        // DECIMAL64
                    } else if (typeBodyChild instanceof Decimal64_specificationContext) {
                        for (int k = 0; k < typeBodyChild.getChildCount(); k++) {
                            ParseTree fdChild = typeBodyChild.getChild(k);
                            if (fdChild instanceof Fraction_digits_stmtContext) {
                                // TODO: implement fraction digits
                                // return
                                // Integer.valueOf(stringFromNode(fdChild));
                            }
                        }
                    }

                }
            }
        }
        return ut.build();
    }

    public static List<RangeConstraint> getRangeConstraints(
            Type_body_stmtsContext ctx) {
        List<RangeConstraint> rangeConstraints = new ArrayList<RangeConstraint>();
        for (int j = 0; j < ctx.getChildCount(); j++) {
            ParseTree numRestrChild = ctx.getChild(j);
            if (numRestrChild instanceof Numerical_restrictionsContext) {
                for (int k = 0; k < numRestrChild.getChildCount(); k++) {
                    ParseTree rangeChild = numRestrChild.getChild(k);
                    if (rangeChild instanceof Range_stmtContext) {
                        rangeConstraints
                                .addAll(parseRangeConstraints((Range_stmtContext) rangeChild));
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
            // TODO: this needs to be refactored, because valid range can be
            // also defined as "1..max"
            String[] splittedRangeDef = rangeDef.split("\\.\\.");
            Long min = Long.valueOf(splittedRangeDef[0]);
            Long max = Long.valueOf(splittedRangeDef[1]);
            RangeConstraint range = BaseConstraints.rangeConstraint(min, max,
                    description, reference);
            rangeConstraints.add(range);
        }

        return rangeConstraints;
    }

    public static Integer getFractionDigits(Type_body_stmtsContext ctx) {
        for (int j = 0; j < ctx.getChildCount(); j++) {
            ParseTree dec64specChild = ctx.getChild(j);
            if (dec64specChild instanceof Decimal64_specificationContext) {
                for (int k = 0; k < dec64specChild.getChildCount(); k++) {
                    ParseTree fdChild = dec64specChild.getChild(k);
                    if (fdChild instanceof Fraction_digits_stmtContext) {
                        return Integer.valueOf(stringFromNode(fdChild));
                    }
                }
            }
        }
        return null;
    }

    public static List<EnumTypeDefinition.EnumPair> getEnumConstants(
            Type_body_stmtsContext ctx) {
        List<EnumTypeDefinition.EnumPair> enumConstants = new ArrayList<EnumTypeDefinition.EnumPair>();

        out: for (int j = 0; j < ctx.getChildCount(); j++) {
            ParseTree enumSpecChild = ctx.getChild(j);
            if (enumSpecChild instanceof Enum_specificationContext) {
                for (int k = 0; k < enumSpecChild.getChildCount(); k++) {
                    ParseTree enumChild = enumSpecChild.getChild(k);
                    if (enumChild instanceof Enum_stmtContext) {
                        enumConstants.add(createEnumPair(
                                (Enum_stmtContext) enumChild, k));
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
            Enum_stmtContext ctx, final int value) {
        final String name = stringFromNode(ctx);
        return new EnumTypeDefinition.EnumPair() {

            @Override
            public QName getQName() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public SchemaPath getPath() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public String getDescription() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public String getReference() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public Status getStatus() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public List<ExtensionDefinition> getExtensionSchemaNodes() {
                // TODO Auto-generated method stub
                return null;
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
            public String toString() {
                return EnumTypeDefinition.EnumPair.class.getSimpleName()
                        + "[name=" + name + ", value=" + value + "]";
            }
        };
    }

    public static List<LengthConstraint> getLengthConstraints(
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
            // TODO: this needs to be refactored, because valid length can be
            // also defined as "1"
            String[] splittedRangeDef = rangeDef.split("\\.\\.");
            Long min = Long.valueOf(splittedRangeDef[0]);
            Long max = Long.valueOf(splittedRangeDef[1]);
            LengthConstraint range = BaseConstraints.lengthConstraint(min, max,
                    description, reference);
            lengthConstraints.add(range);
        }

        return lengthConstraints;
    }

    public static List<PatternConstraint> getPatternConstraint(
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
        String pattern = stringFromNode(ctx);
        return BaseConstraints.patternConstraint(pattern, description,
                reference);
    }

    public static List<BitsTypeDefinition.Bit> getBits(
            Type_body_stmtsContext ctx, Stack<String> actualPath,
            URI namespace, Date revision, String prefix) {
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

    private static BitsTypeDefinition.Bit parseBit(final Bit_stmtContext ctx,
            Stack<String> actualPath, final URI namespace, final Date revision,
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

        SchemaPath schemaPath = getActualSchemaPath(bitPath, namespace,
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
                status = getStatus((Status_stmtContext) child);
            }
        }

        // TODO: extensionDefinitions
        return createBit(qname, schemaPath, description, reference, status,
                null, position);
    }

    private static BitsTypeDefinition.Bit createBit(final QName qname,
            final SchemaPath schemaPath, final String description,
            final String reference, final Status status,
            final List<ExtensionDefinition> extensionDefinitions,
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
            public List<ExtensionDefinition> getExtensionSchemaNodes() {
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
                    if (other.getExtensionSchemaNodes() != null) {
                        return false;
                    }
                } else if (!extensionDefinitions.equals(other
                        .getExtensionSchemaNodes())) {
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

}
