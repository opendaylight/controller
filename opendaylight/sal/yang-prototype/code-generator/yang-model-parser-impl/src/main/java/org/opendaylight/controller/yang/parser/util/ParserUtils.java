/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.yang.parser.util;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.opendaylight.controller.yang.common.QName;
import org.opendaylight.controller.yang.model.api.ModuleImport;
import org.opendaylight.controller.yang.model.api.MustDefinition;
import org.opendaylight.controller.yang.model.api.SchemaPath;
import org.opendaylight.controller.yang.model.api.TypeDefinition;
import org.opendaylight.controller.yang.model.api.type.BinaryTypeDefinition;
import org.opendaylight.controller.yang.model.api.type.BitsTypeDefinition;
import org.opendaylight.controller.yang.model.api.type.BooleanTypeDefinition;
import org.opendaylight.controller.yang.model.api.type.DecimalTypeDefinition;
import org.opendaylight.controller.yang.model.api.type.EmptyTypeDefinition;
import org.opendaylight.controller.yang.model.api.type.EnumTypeDefinition;
import org.opendaylight.controller.yang.model.api.type.EnumTypeDefinition.EnumPair;
import org.opendaylight.controller.yang.model.api.type.IdentityrefTypeDefinition;
import org.opendaylight.controller.yang.model.api.type.InstanceIdentifierTypeDefinition;
import org.opendaylight.controller.yang.model.api.type.IntegerTypeDefinition;
import org.opendaylight.controller.yang.model.api.type.LeafrefTypeDefinition;
import org.opendaylight.controller.yang.model.api.type.LengthConstraint;
import org.opendaylight.controller.yang.model.api.type.PatternConstraint;
import org.opendaylight.controller.yang.model.api.type.RangeConstraint;
import org.opendaylight.controller.yang.model.api.type.StringTypeDefinition;
import org.opendaylight.controller.yang.model.api.type.UnionTypeDefinition;
import org.opendaylight.controller.yang.model.api.type.UnsignedIntegerTypeDefinition;
import org.opendaylight.controller.yang.model.util.BinaryType;
import org.opendaylight.controller.yang.model.util.BitsType;
import org.opendaylight.controller.yang.model.util.BooleanType;
import org.opendaylight.controller.yang.model.util.Decimal64;
import org.opendaylight.controller.yang.model.util.EmptyType;
import org.opendaylight.controller.yang.model.util.EnumerationType;
import org.opendaylight.controller.yang.model.util.ExtendedType;
import org.opendaylight.controller.yang.model.util.IdentityrefType;
import org.opendaylight.controller.yang.model.util.InstanceIdentifier;
import org.opendaylight.controller.yang.model.util.Int16;
import org.opendaylight.controller.yang.model.util.Int32;
import org.opendaylight.controller.yang.model.util.Int64;
import org.opendaylight.controller.yang.model.util.Int8;
import org.opendaylight.controller.yang.model.util.Leafref;
import org.opendaylight.controller.yang.model.util.StringType;
import org.opendaylight.controller.yang.model.util.UnionType;
import org.opendaylight.controller.yang.parser.builder.api.AugmentationSchemaBuilder;
import org.opendaylight.controller.yang.parser.builder.api.Builder;
import org.opendaylight.controller.yang.parser.builder.api.DataNodeContainerBuilder;
import org.opendaylight.controller.yang.parser.builder.api.DataSchemaNodeBuilder;
import org.opendaylight.controller.yang.parser.builder.api.GroupingBuilder;
import org.opendaylight.controller.yang.parser.builder.api.SchemaNodeBuilder;
import org.opendaylight.controller.yang.parser.builder.api.TypeAwareBuilder;
import org.opendaylight.controller.yang.parser.builder.api.TypeDefinitionBuilder;
import org.opendaylight.controller.yang.parser.builder.api.UsesNodeBuilder;
import org.opendaylight.controller.yang.parser.builder.impl.AnyXmlBuilder;
import org.opendaylight.controller.yang.parser.builder.impl.ChoiceBuilder;
import org.opendaylight.controller.yang.parser.builder.impl.ChoiceCaseBuilder;
import org.opendaylight.controller.yang.parser.builder.impl.ConstraintsBuilder;
import org.opendaylight.controller.yang.parser.builder.impl.ContainerSchemaNodeBuilder;
import org.opendaylight.controller.yang.parser.builder.impl.GroupingBuilderImpl;
import org.opendaylight.controller.yang.parser.builder.impl.LeafListSchemaNodeBuilder;
import org.opendaylight.controller.yang.parser.builder.impl.LeafSchemaNodeBuilder;
import org.opendaylight.controller.yang.parser.builder.impl.ListSchemaNodeBuilder;
import org.opendaylight.controller.yang.parser.builder.impl.ModuleBuilder;
import org.opendaylight.controller.yang.parser.builder.impl.TypeDefinitionBuilderImpl;
import org.opendaylight.controller.yang.parser.builder.impl.UnknownSchemaNodeBuilder;
import org.opendaylight.controller.yang.parser.builder.impl.UsesNodeBuilderImpl;

public final class ParserUtils {

    private ParserUtils() {
    }

    /**
     * Get module import referenced by given prefix.
     * 
     * @param builder
     *            module to search
     * @param prefix
     *            prefix associated with import
     * @return ModuleImport based on given prefix
     */
    public static ModuleImport getModuleImport(final ModuleBuilder builder,
            final String prefix) {
        ModuleImport moduleImport = null;
        for (ModuleImport mi : builder.getModuleImports()) {
            if (mi.getPrefix().equals(prefix)) {
                moduleImport = mi;
                break;
            }
        }
        return moduleImport;
    }

    /**
     * Parse uses path.
     * 
     * @param usesPath
     *            as String
     * @return SchemaPath from given String
     */
    public static SchemaPath parseUsesPath(final String usesPath) {
        final boolean absolute = usesPath.startsWith("/");
        final String[] splittedPath = usesPath.split("/");
        final List<QName> path = new ArrayList<QName>();
        QName name;
        for (String pathElement : splittedPath) {
            if (pathElement.length() > 0) {
                final String[] splittedElement = pathElement.split(":");
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
     * Add all augment's child nodes to given target.
     * 
     * @param augment
     * @param target
     */
    public static void fillAugmentTarget(
            final AugmentationSchemaBuilder augment,
            final DataNodeContainerBuilder target) {
        for (DataSchemaNodeBuilder builder : augment.getChildNodes()) {
            builder.setAugmenting(true);
            correctAugmentChildPath(builder, target.getPath());
            target.addChildNode(builder);
        }
    }

    public static void fillAugmentTarget(
            final AugmentationSchemaBuilder augment, final ChoiceBuilder target) {
        for (DataSchemaNodeBuilder builder : augment.getChildNodes()) {
            builder.setAugmenting(true);
            correctAugmentChildPath(builder, target.getPath());
            target.addChildNode(builder);
        }
    }

    private static void correctAugmentChildPath(
            final DataSchemaNodeBuilder childNode,
            final SchemaPath parentSchemaPath) {

        // set correct path
        List<QName> targetNodePath = new ArrayList<QName>(
                parentSchemaPath.getPath());
        targetNodePath.add(childNode.getQName());
        childNode.setPath(new SchemaPath(targetNodePath, true));

        // set correct path for all child nodes
        if (childNode instanceof DataNodeContainerBuilder) {
            DataNodeContainerBuilder dataNodeContainer = (DataNodeContainerBuilder) childNode;
            for (DataSchemaNodeBuilder child : dataNodeContainer
                    .getChildNodes()) {
                correctAugmentChildPath(child, childNode.getPath());
            }
        }

        // if node can contains type, correct path for this type too
        if (childNode instanceof TypeAwareBuilder) {
            TypeAwareBuilder nodeBuilder = (TypeAwareBuilder) childNode;
            correctTypeAwareNodePath(nodeBuilder, parentSchemaPath);
        }
    }

    /**
     * Repair schema path of node type.
     * 
     * @param node
     *            node which contains type statement
     * @param parentSchemaPath
     *            schema path of parent node
     */
    private static void correctTypeAwareNodePath(
            TypeAwareBuilder node, SchemaPath parentSchemaPath) {
        final QName nodeBuilderQName = node.getQName();
        final TypeDefinition<?> nodeType = node.getType();

        Integer fd = null;
        List<LengthConstraint> lengths = null;
        List<PatternConstraint> patterns = null;
        List<RangeConstraint> ranges = null;

        if (nodeType != null) {
            if (nodeType instanceof ExtendedType) {
                ExtendedType et = (ExtendedType) nodeType;
                if (nodeType
                        .getQName()
                        .getLocalName()
                        .equals(nodeType.getBaseType().getQName()
                                .getLocalName())) {
                    fd = et.getFractionDigits();
                    lengths = et.getLengths();
                    patterns = et.getPatterns();
                    ranges = et.getRanges();
                    if (!hasConstraints(fd, lengths, patterns, ranges)) {
                        return;
                    }
                }
            }
            TypeDefinition<?> newType = createCorrectTypeDefinition(
                    parentSchemaPath, nodeBuilderQName, nodeType);
            node.setType(newType);
        } else {
            TypeDefinitionBuilder nodeBuilderTypedef = node.getTypedef();

            fd = nodeBuilderTypedef.getFractionDigits();
            lengths = nodeBuilderTypedef.getLengths();
            patterns = nodeBuilderTypedef.getPatterns();
            ranges = nodeBuilderTypedef.getRanges();

            String tdbTypeName = nodeBuilderTypedef.getQName().getLocalName();
            String baseTypeName = null;
            if (nodeBuilderTypedef.getType() == null) {
                baseTypeName = nodeBuilderTypedef.getTypedef().getQName()
                        .getLocalName();
            } else {
                baseTypeName = nodeBuilderTypedef.getType().getQName()
                        .getLocalName();
            }
            if (!(tdbTypeName.equals(baseTypeName))) {
                return;
            }

            if (!hasConstraints(fd, lengths, patterns, ranges)) {
                return;
            }

            SchemaPath newSchemaPath = createNewSchemaPath(
                    nodeBuilderTypedef.getPath(), nodeBuilderQName,
                    nodeBuilderTypedef.getQName());
            nodeBuilderTypedef.setPath(newSchemaPath);
        }
    }

    /**
     * Check if there are some constraints.
     * 
     * @param fd
     *            fraction digits
     * @param lengths
     *            length constraints
     * @param patterns
     *            pattern constraints
     * @param ranges
     *            range constraints
     * @return true, if any of constraints are present, false otherwise
     */
    private static boolean hasConstraints(final Integer fd,
            final List<LengthConstraint> lengths,
            final List<PatternConstraint> patterns,
            final List<RangeConstraint> ranges) {
        if (fd == null && (lengths == null || lengths.isEmpty())
                && (patterns == null || patterns.isEmpty())
                && (ranges == null || ranges.isEmpty())) {
            return false;
        } else {
            return true;
        }
    }

    private static TypeDefinition<?> createCorrectTypeDefinition(
            SchemaPath parentSchemaPath, QName nodeQName,
            TypeDefinition<?> nodeType) {
        TypeDefinition<?> result = null;
        SchemaPath newSchemaPath = null;
        if (nodeType != null) {
            if (nodeType instanceof BinaryTypeDefinition) {
                BinaryTypeDefinition binType = (BinaryTypeDefinition) nodeType;
                newSchemaPath = createNewSchemaPath(parentSchemaPath,
                        nodeQName, binType.getQName());
                List<Byte> bytes = (List<Byte>) binType.getDefaultValue();
                result = new BinaryType(newSchemaPath, bytes);
            } else if (nodeType instanceof BitsTypeDefinition) {
                BitsTypeDefinition bitsType = (BitsTypeDefinition) nodeType;
                newSchemaPath = createNewSchemaPath(parentSchemaPath,
                        nodeQName, nodeType.getQName());
                result = new BitsType(newSchemaPath, bitsType.getBits());
            } else if (nodeType instanceof BooleanTypeDefinition) {
                BooleanTypeDefinition booleanType = (BooleanTypeDefinition) nodeType;
                newSchemaPath = createNewSchemaPath(parentSchemaPath,
                        nodeQName, booleanType.getQName());
                result = new BooleanType(newSchemaPath);
            } else if (nodeType instanceof DecimalTypeDefinition) {
                DecimalTypeDefinition decimalType = (DecimalTypeDefinition) nodeType;
                newSchemaPath = createNewSchemaPath(parentSchemaPath,
                        nodeQName, decimalType.getQName());
                result = new Decimal64(newSchemaPath,
                        decimalType.getFractionDigits());
            } else if (nodeType instanceof EmptyTypeDefinition) {
                newSchemaPath = createNewSchemaPath(parentSchemaPath,
                        nodeQName, nodeType.getQName());
                result = new EmptyType(newSchemaPath);
            } else if (nodeType instanceof EnumTypeDefinition) {
                EnumTypeDefinition enumType = (EnumTypeDefinition) nodeType;
                newSchemaPath = createNewSchemaPath(parentSchemaPath,
                        nodeQName, enumType.getQName());
                result = new EnumerationType(newSchemaPath,
                        (EnumPair) enumType.getDefaultValue(),
                        enumType.getValues());
            } else if (nodeType instanceof IdentityrefTypeDefinition) {
                IdentityrefTypeDefinition idrefType = (IdentityrefTypeDefinition) nodeType;
                newSchemaPath = createNewSchemaPath(parentSchemaPath,
                        nodeQName, idrefType.getQName());
                result = new IdentityrefType(idrefType.getIdentity(),
                        newSchemaPath);
            } else if (nodeType instanceof InstanceIdentifierTypeDefinition) {
                InstanceIdentifierTypeDefinition instIdType = (InstanceIdentifierTypeDefinition) nodeType;
                newSchemaPath = createNewSchemaPath(parentSchemaPath,
                        nodeQName, instIdType.getQName());
                return new InstanceIdentifier(newSchemaPath,
                        instIdType.getPathStatement(),
                        instIdType.requireInstance());
            } else if (nodeType instanceof StringTypeDefinition) {
                result = createNewStringType(parentSchemaPath, nodeQName,
                        (StringTypeDefinition) nodeType);
            } else if (nodeType instanceof IntegerTypeDefinition) {
                result = createNewIntType(parentSchemaPath, nodeQName,
                        (IntegerTypeDefinition) nodeType);
            } else if (nodeType instanceof UnsignedIntegerTypeDefinition) {
                result = createNewUintType(parentSchemaPath, nodeQName,
                        (UnsignedIntegerTypeDefinition) nodeType);
            } else if (nodeType instanceof LeafrefTypeDefinition) {
                newSchemaPath = createNewSchemaPath(parentSchemaPath,
                        nodeQName, nodeType.getQName());
                result = new Leafref(newSchemaPath,
                        ((LeafrefTypeDefinition) nodeType).getPathStatement());
            } else if (nodeType instanceof UnionTypeDefinition) {
                UnionTypeDefinition unionType = (UnionTypeDefinition) nodeType;
                newSchemaPath = createNewSchemaPath(parentSchemaPath,
                        nodeQName, unionType.getQName());
                return new UnionType(newSchemaPath, unionType.getTypes());
            } else if (nodeType instanceof ExtendedType) {
                ExtendedType extType = (ExtendedType) nodeType;
                newSchemaPath = createNewSchemaPath(parentSchemaPath,
                        nodeQName, extType.getQName());
                result = createNewExtendedType(newSchemaPath, extType);
            }
        }
        return result;
    }

    private static TypeDefinition<?> createNewExtendedType(
            SchemaPath newSchemaPath, ExtendedType oldExtendedType) {
        QName qname = oldExtendedType.getQName();
        TypeDefinition<?> baseType = oldExtendedType.getBaseType();
        String desc = oldExtendedType.getDescription();
        String ref = oldExtendedType.getReference();
        ExtendedType.Builder builder = new ExtendedType.Builder(qname,
                baseType, desc, ref, newSchemaPath);
        builder.status(oldExtendedType.getStatus());
        builder.lengths(oldExtendedType.getLengths());
        builder.patterns(oldExtendedType.getPatterns());
        builder.ranges(oldExtendedType.getRanges());
        builder.fractionDigits(oldExtendedType.getFractionDigits());
        builder.unknownSchemaNodes(oldExtendedType.getUnknownSchemaNodes());
        return builder.build();
    }

    private static TypeDefinition<?> createNewStringType(SchemaPath schemaPath,
            QName nodeQName, StringTypeDefinition nodeType) {
        List<QName> path = schemaPath.getPath();
        List<QName> newPath = new ArrayList<QName>(path);
        newPath.add(nodeQName);
        newPath.add(nodeType.getQName());
        SchemaPath newSchemaPath = new SchemaPath(newPath,
                schemaPath.isAbsolute());

        return new StringType(newSchemaPath);
    }

    private static TypeDefinition<?> createNewIntType(SchemaPath schemaPath,
            QName nodeQName, IntegerTypeDefinition type) {
        QName typeQName = type.getQName();
        SchemaPath newSchemaPath = createNewSchemaPath(schemaPath, nodeQName,
                typeQName);
        String localName = typeQName.getLocalName();

        if ("int8".equals(localName)) {
            return new Int8(newSchemaPath);
        } else if ("int16".equals(localName)) {
            return new Int16(newSchemaPath);
        } else if ("int32".equals(localName)) {
            return new Int32(newSchemaPath);
        } else if ("int64".equals(localName)) {
            return new Int64(newSchemaPath);
        } else {
            return null;
        }
    }

    private static TypeDefinition<?> createNewUintType(SchemaPath schemaPath,
            QName nodeQName, UnsignedIntegerTypeDefinition type) {
        QName typeQName = type.getQName();
        SchemaPath newSchemaPath = createNewSchemaPath(schemaPath, nodeQName,
                typeQName);
        String localName = typeQName.getLocalName();

        if ("uint8".equals(localName)) {
            return new Int8(newSchemaPath);
        } else if ("uint16".equals(localName)) {
            return new Int16(newSchemaPath);
        } else if ("uint32".equals(localName)) {
            return new Int32(newSchemaPath);
        } else if ("uint64".equals(localName)) {
            return new Int64(newSchemaPath);
        } else {
            return null;
        }
    }

    private static SchemaPath createNewSchemaPath(SchemaPath schemaPath,
            QName currentQName, QName qname) {
        List<QName> newPath = new ArrayList<QName>(schemaPath.getPath());
        newPath.add(currentQName);
        newPath.add(qname);
        return new SchemaPath(newPath, schemaPath.isAbsolute());
    }

    public static void refineLeaf(LeafSchemaNodeBuilder leaf,
            RefineHolder refine, int line) {
        String defaultStr = refine.getDefaultStr();
        Boolean mandatory = refine.isMandatory();
        MustDefinition must = refine.getMust();
        List<UnknownSchemaNodeBuilder> unknownNodes = refine.getUnknownNodes();

        if (defaultStr != null && !("".equals(defaultStr))) {
            leaf.setDefaultStr(defaultStr);
        }
        if (mandatory != null) {
            leaf.getConstraints().setMandatory(mandatory);
        }
        if (must != null) {
            leaf.getConstraints().addMustDefinition(must);
        }
        if (unknownNodes != null) {
            for (UnknownSchemaNodeBuilder unknown : unknownNodes) {
                leaf.addUnknownSchemaNode(unknown);
            }
        }
    }

    public static void refineContainer(ContainerSchemaNodeBuilder container,
            RefineHolder refine, int line) {
        Boolean presence = refine.isPresence();
        MustDefinition must = refine.getMust();
        List<UnknownSchemaNodeBuilder> unknownNodes = refine.getUnknownNodes();

        if (presence != null) {
            container.setPresence(presence);
        }
        if (must != null) {
            container.getConstraints().addMustDefinition(must);
        }
        if (unknownNodes != null) {
            for (UnknownSchemaNodeBuilder unknown : unknownNodes) {
                container.addUnknownSchemaNode(unknown);
            }
        }
    }

    public static void refineList(ListSchemaNodeBuilder list,
            RefineHolder refine, int line) {
        MustDefinition must = refine.getMust();
        Integer min = refine.getMinElements();
        Integer max = refine.getMaxElements();
        List<UnknownSchemaNodeBuilder> unknownNodes = refine.getUnknownNodes();

        if (must != null) {
            list.getConstraints().addMustDefinition(must);
        }
        if (min != null) {
            list.getConstraints().setMinElements(min);
        }
        if (max != null) {
            list.getConstraints().setMaxElements(max);
        }
        if (unknownNodes != null) {
            for (UnknownSchemaNodeBuilder unknown : unknownNodes) {
                list.addUnknownSchemaNode(unknown);
            }
        }
    }

    public static void refineLeafList(LeafListSchemaNodeBuilder leafList,
            RefineHolder refine, int line) {
        MustDefinition must = refine.getMust();
        Integer min = refine.getMinElements();
        Integer max = refine.getMaxElements();
        List<UnknownSchemaNodeBuilder> unknownNodes = refine.getUnknownNodes();

        if (must != null) {
            leafList.getConstraints().addMustDefinition(must);
        }
        if (min != null) {
            leafList.getConstraints().setMinElements(min);
        }
        if (max != null) {
            leafList.getConstraints().setMaxElements(max);
        }
        if (unknownNodes != null) {
            for (UnknownSchemaNodeBuilder unknown : unknownNodes) {
                leafList.addUnknownSchemaNode(unknown);
            }
        }
    }

    public static void refineChoice(ChoiceBuilder choice, RefineHolder refine,
            int line) {
        String defaultStr = refine.getDefaultStr();
        Boolean mandatory = refine.isMandatory();
        List<UnknownSchemaNodeBuilder> unknownNodes = refine.getUnknownNodes();

        if (defaultStr != null) {
            choice.setDefaultCase(defaultStr);
        }
        if (mandatory != null) {
            choice.getConstraints().setMandatory(mandatory);
        }
        if (unknownNodes != null) {
            for (UnknownSchemaNodeBuilder unknown : unknownNodes) {
                choice.addUnknownSchemaNode(unknown);
            }
        }
    }

    public static void refineAnyxml(AnyXmlBuilder anyXml, RefineHolder refine,
            int line) {
        Boolean mandatory = refine.isMandatory();
        MustDefinition must = refine.getMust();
        List<UnknownSchemaNodeBuilder> unknownNodes = refine.getUnknownNodes();

        if (mandatory != null) {
            anyXml.getConstraints().setMandatory(mandatory);
        }
        if (must != null) {
            anyXml.getConstraints().addMustDefinition(must);
        }
        if (unknownNodes != null) {
            for (UnknownSchemaNodeBuilder unknown : unknownNodes) {
                anyXml.addUnknownSchemaNode(unknown);
            }
        }
    }

    public static void checkRefine(SchemaNodeBuilder node, RefineHolder refine) {
        String name = node.getQName().getLocalName();
        int line = refine.getLine();

        String defaultStr = refine.getDefaultStr();
        Boolean mandatory = refine.isMandatory();
        Boolean presence = refine.isPresence();
        MustDefinition must = refine.getMust();
        Integer min = refine.getMinElements();
        Integer max = refine.getMaxElements();

        if (node instanceof AnyXmlBuilder) {
            checkRefineDefault(node, defaultStr, line);
            checkRefinePresence(node, presence, line);
            checkRefineMinMax(name, line, min, max);
        } else if (node instanceof ChoiceBuilder) {
            checkRefinePresence(node, presence, line);
            checkRefineMust(node, must, line);
            checkRefineMinMax(name, line, min, max);
        } else if (node instanceof ContainerSchemaNodeBuilder) {
            checkRefineDefault(node, defaultStr, line);
            checkRefineMandatory(node, mandatory, line);
            checkRefineMust(node, must, line);
            checkRefineMinMax(name, line, min, max);
        } else if (node instanceof LeafSchemaNodeBuilder) {
            checkRefinePresence(node, presence, line);
            checkRefineMinMax(name, line, min, max);
        } else if (node instanceof LeafListSchemaNodeBuilder
                || node instanceof ListSchemaNodeBuilder) {
            checkRefineDefault(node, defaultStr, line);
            checkRefinePresence(node, presence, line);
            checkRefineMandatory(node, mandatory, line);
        } else if (node instanceof GroupingBuilder
                || node instanceof TypeDefinitionBuilder
                || node instanceof UsesNodeBuilder) {
            checkRefineDefault(node, defaultStr, line);
            checkRefinePresence(node, presence, line);
            checkRefineMandatory(node, mandatory, line);
            checkRefineMust(node, must, line);
            checkRefineMinMax(name, line, min, max);
        }
    }

    private static void checkRefineDefault(SchemaNodeBuilder node,
            String defaultStr, int line) {
        if (defaultStr != null) {
            throw new YangParseException(line, "Can not refine 'default' for '"
                    + node.getQName().getLocalName() + "'.");
        }
    }

    private static void checkRefineMandatory(SchemaNodeBuilder node,
            Boolean mandatory, int line) {
        if (mandatory != null) {
            throw new YangParseException(line,
                    "Can not refine 'mandatory' for '"
                            + node.getQName().getLocalName() + "'.");
        }
    }

    private static void checkRefinePresence(SchemaNodeBuilder node,
            Boolean presence, int line) {
        if (presence != null) {
            throw new YangParseException(line,
                    "Can not refine 'presence' for '"
                            + node.getQName().getLocalName() + "'.");
        }
    }

    private static void checkRefineMust(SchemaNodeBuilder node,
            MustDefinition must, int line) {
        if (must != null) {
            throw new YangParseException(line, "Can not refine 'must' for '"
                    + node.getQName().getLocalName() + "'.");
        }
    }

    private static void checkRefineMinMax(String refineTargetName,
            int refineLine, Integer min, Integer max) {
        if (min != null || max != null) {
            throw new YangParseException(refineLine,
                    "Can not refine 'min-elements' or 'max-elements' for '"
                            + refineTargetName + "'.");
        }
    }

    /**
     * Perform refine operation of following parameters:
     * <ul>
     * <li>description</li>
     * <li>reference</li>
     * <li>config</li>
     * </ul>
     * 
     * These parameters may be refined for any node.
     * 
     * @param node
     *            node to refine
     * @param refine
     *            refine holder containing values to refine
     * @param line
     *            current line in yang model
     */
    public static void refineDefault(Builder node, RefineHolder refine, int line) {
        Class<? extends Builder> cls = node.getClass();

        String description = refine.getDescription();
        if (description != null) {
            try {
                Method method = cls.getDeclaredMethod("setDescription",
                        String.class);
                method.invoke(node, description);
            } catch (Exception e) {
                throw new YangParseException(line,
                        "Cannot refine description in " + cls.getName(), e);
            }
        }

        String reference = refine.getReference();
        if (reference != null) {
            try {
                Method method = cls.getDeclaredMethod("setReference",
                        String.class);
                method.invoke(node, reference);
            } catch (Exception e) {
                throw new YangParseException(line,
                        "Cannot refine reference in " + cls.getName(), e);
            }
        }

        Boolean config = refine.isConfig();
        if (config != null) {
            try {
                Method method = cls.getDeclaredMethod("setConfiguration",
                        Boolean.TYPE);
                method.invoke(node, config);
            } catch (Exception e) {
                throw new YangParseException(line, "Cannot refine config in "
                        + cls.getName(), e);
            }
        }
    }

    public static LeafSchemaNodeBuilder copyLeafBuilder(
            final LeafSchemaNodeBuilder old) {
        final LeafSchemaNodeBuilder copy = new LeafSchemaNodeBuilder(
                old.getQName(), old.getLine());
        final TypeDefinition<?> type = old.getType();

        if (type == null) {
            copy.setTypedef(old.getTypedef());
        } else {
            copy.setType(type);
        }
        copy.setPath(old.getPath());
        copyConstraints(old, copy);
        for (UnknownSchemaNodeBuilder unknown : old.getUnknownNodes()) {
            copy.addUnknownSchemaNode(unknown);
        }
        copy.setDescription(old.getDescription());
        copy.setReference(old.getReference());
        copy.setStatus(old.getStatus());
        copy.setAugmenting(old.isAugmenting());
        copy.setConfiguration(old.isConfiguration());
        copy.setDefaultStr(old.getDefaultStr());
        copy.setUnits(old.getUnits());
        return copy;
    }

    public static ContainerSchemaNodeBuilder copyContainerBuilder(
            final ContainerSchemaNodeBuilder old) {
        final ContainerSchemaNodeBuilder copy = new ContainerSchemaNodeBuilder(
                old.getQName(), old.getLine());
        copy.setPath(old.getPath());
        copyConstraints(old, copy);
        for (UnknownSchemaNodeBuilder unknown : old.getUnknownNodes()) {
            copy.addUnknownSchemaNode(unknown);
        }
        for (DataSchemaNodeBuilder child : old.getChildNodes()) {
            copy.addChildNode(child);
        }
        for (GroupingBuilder grouping : old.getGroupings()) {
            copy.addGrouping(grouping);
        }
        for (TypeDefinitionBuilder typedef : old.getTypeDefinitions()) {
            copy.addTypedef(typedef);
        }
        for (AugmentationSchemaBuilder augment : old.getAugmentations()) {
            copy.addAugmentation(augment);
        }
        for (UsesNodeBuilder use : old.getUsesNodes()) {
            copy.addUsesNode(use);
        }
        copy.setDescription(old.getDescription());
        copy.setReference(old.getReference());
        copy.setStatus(old.getStatus());
        copy.setAugmenting(old.isAugmenting());
        copy.setConfiguration(old.isConfiguration());
        copy.setPresence(old.isPresence());
        return copy;
    }

    public static ListSchemaNodeBuilder copyListBuilder(
            final ListSchemaNodeBuilder old) {
        final ListSchemaNodeBuilder copy = new ListSchemaNodeBuilder(
                old.getQName(), old.getLine());
        copy.setPath(old.getPath());
        copyConstraints(old, copy);
        for (UnknownSchemaNodeBuilder unknown : old.getUnknownNodes()) {
            copy.addUnknownSchemaNode(unknown);
        }
        for (DataSchemaNodeBuilder child : old.getChildNodes()) {
            copy.addChildNode(child);
        }
        for (GroupingBuilder grouping : old.getGroupings()) {
            copy.addGrouping(grouping);
        }
        for (TypeDefinitionBuilder typedef : old.getTypeDefinitions()) {
            copy.addTypedef(typedef);
        }
        for (AugmentationSchemaBuilder augment : old.getAugmentations()) {
            copy.addAugmentation(augment);
        }
        for (UsesNodeBuilder use : old.getUsesNodes()) {
            copy.addUsesNode(use);
        }
        copy.setDescription(old.getDescription());
        copy.setReference(old.getReference());
        copy.setStatus(old.getStatus());
        copy.setAugmenting(old.isAugmenting());
        copy.setConfiguration(old.isConfiguration());
        copy.setUserOrdered(old.isUserOrdered());
        return copy;
    }

    public static LeafListSchemaNodeBuilder copyLeafListBuilder(
            final LeafListSchemaNodeBuilder old) {
        final LeafListSchemaNodeBuilder copy = new LeafListSchemaNodeBuilder(
                old.getQName(), old.getLine());
        copy.setPath(old.getPath());
        copyConstraints(old, copy);
        final TypeDefinition<?> type = old.getType();
        if (type == null) {
            copy.setTypedef(old.getTypedef());
        } else {
            copy.setType(type);
        }
        for (UnknownSchemaNodeBuilder unknown : old.getUnknownNodes()) {
            copy.addUnknownSchemaNode(unknown);
        }
        copy.setDescription(old.getDescription());
        copy.setReference(old.getReference());
        copy.setStatus(old.getStatus());
        copy.setAugmenting(old.isAugmenting());
        copy.setConfiguration(old.isConfiguration());
        copy.setUserOrdered(old.isUserOrdered());
        return copy;
    }

    public static ChoiceBuilder copyChoiceBuilder(final ChoiceBuilder old) {
        final ChoiceBuilder copy = new ChoiceBuilder(old.getQName(),
                old.getLine());
        copy.setPath(old.getPath());
        copyConstraints(old, copy);
        for (ChoiceCaseBuilder caseBuilder : old.getCases()) {
            copy.addChildNode(caseBuilder);
        }
        for (UnknownSchemaNodeBuilder unknown : old.getUnknownNodes()) {
            copy.addUnknownSchemaNode(unknown);
        }
        copy.setDefaultCase(old.getDefaultCase());
        copy.setDescription(old.getDescription());
        copy.setReference(old.getReference());
        copy.setStatus(old.getStatus());
        copy.setAugmenting(old.isAugmenting());
        copy.setConfiguration(old.isConfiguration());
        return copy;
    }

    public static AnyXmlBuilder copyAnyXmlBuilder(final AnyXmlBuilder old) {
        final AnyXmlBuilder copy = new AnyXmlBuilder(old.getQName(),
                old.getLine());
        copy.setPath(old.getPath());
        copyConstraints(old, copy);
        for (UnknownSchemaNodeBuilder unknown : old.getUnknownNodes()) {
            copy.addUnknownSchemaNode(unknown);
        }
        copy.setDescription(old.getDescription());
        copy.setReference(old.getReference());
        copy.setStatus(old.getStatus());
        copy.setConfiguration(old.isConfiguration());
        return copy;
    }

    public static GroupingBuilder copyGroupingBuilder(final GroupingBuilder old) {
        final GroupingBuilder copy = new GroupingBuilderImpl(old.getQName(),
                old.getLine());
        copy.setPath(old.getPath());
        for (DataSchemaNodeBuilder child : old.getChildNodes()) {
            copy.addChildNode(child);
        }
        for (GroupingBuilder grouping : old.getGroupings()) {
            copy.addGrouping(grouping);
        }
        for (TypeDefinitionBuilder typedef : old.getTypeDefinitions()) {
            copy.addTypedef(typedef);
        }
        for (UsesNodeBuilder use : old.getUses()) {
            copy.addUsesNode(use);
        }
        for (UnknownSchemaNodeBuilder unknown : old.getUnknownNodes()) {
            copy.addUnknownSchemaNode(unknown);
        }
        copy.setDescription(old.getDescription());
        copy.setReference(old.getReference());
        copy.setStatus(old.getStatus());
        return copy;
    }

    public static TypeDefinitionBuilderImpl copyTypedefBuilder(
            TypeDefinitionBuilderImpl old) {
        final TypeDefinitionBuilderImpl copy = new TypeDefinitionBuilderImpl(
                old.getQName(), old.getLine());
        copy.setPath(old.getPath());
        copy.setDefaultValue(old.getDefaultValue());
        copy.setUnits(old.getUnits());
        copy.setDescription(old.getDescription());
        copy.setReference(old.getReference());
        copy.setStatus(old.getStatus());

        copy.setRanges(old.getRanges());
        copy.setLengths(old.getLengths());
        copy.setPatterns(old.getPatterns());
        copy.setFractionDigits(old.getFractionDigits());

        TypeDefinition<?> type = old.getType();
        if (type == null) {
            copy.setTypedef(old.getTypedef());
        } else {
            copy.setType(old.getType());
        }
        copy.setUnits(old.getUnits());
        for (UnknownSchemaNodeBuilder unknown : old.getUnknownNodes()) {
            copy.addUnknownSchemaNode(unknown);
        }
        return copy;
    }

    public static UsesNodeBuilder copyUsesNodeBuilder(UsesNodeBuilder old) {
        final UsesNodeBuilder copy = new UsesNodeBuilderImpl(
                old.getGroupingName(), old.getLine());
        for (AugmentationSchemaBuilder augment : old.getAugmentations()) {
            copy.addAugment(augment);
        }
        copy.setAugmenting(old.isAugmenting());
        for (SchemaNodeBuilder refineNode : old.getRefineNodes()) {
            copy.addRefineNode(refineNode);
        }
        return copy;
    }

    private static void copyConstraints(final DataSchemaNodeBuilder oldBuilder,
            final DataSchemaNodeBuilder newBuilder) {
        final ConstraintsBuilder oldConstraints = oldBuilder.getConstraints();
        final ConstraintsBuilder newConstraints = newBuilder.getConstraints();
        newConstraints.addWhenCondition(oldConstraints.getWhenCondition());
        for (MustDefinition must : oldConstraints.getMustDefinitions()) {
            newConstraints.addMustDefinition(must);
        }
        newConstraints.setMandatory(oldConstraints.isMandatory());
        newConstraints.setMinElements(oldConstraints.getMinElements());
        newConstraints.setMaxElements(oldConstraints.getMaxElements());
    }

}
