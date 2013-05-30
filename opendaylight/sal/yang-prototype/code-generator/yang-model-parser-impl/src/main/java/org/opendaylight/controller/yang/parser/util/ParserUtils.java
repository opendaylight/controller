/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.yang.parser.util;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

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
import org.opendaylight.controller.yang.parser.builder.api.ChildNodeBuilder;
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
import org.opendaylight.controller.yang.parser.builder.impl.TypedefBuilder;
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
            final ChildNodeBuilder target) {
        for (DataSchemaNodeBuilder builder : augment.getChildNodes()) {
            builder.setAugmenting(true);
            correctAugmentChildPath(augment, target.getPath());
            target.addChildNode(builder);
        }
    }

    private static void correctAugmentChildPath(final ChildNodeBuilder node,
            final SchemaPath parentSchemaPath) {
        for (DataSchemaNodeBuilder builder : node.getChildNodes()) {

            // add correct path
            List<QName> targetNodePath = new ArrayList<QName>(
                    parentSchemaPath.getPath());
            targetNodePath.add(builder.getQName());
            builder.setPath(new SchemaPath(targetNodePath, true));

            if (builder instanceof ChildNodeBuilder) {
                ChildNodeBuilder cnb = (ChildNodeBuilder) builder;
                correctAugmentChildPath(cnb, builder.getPath());
            }

            // if child can contains type, correct path for this type too
            if(builder instanceof TypeAwareBuilder) {
                TypeAwareBuilder nodeBuilder = (TypeAwareBuilder)builder;
                QName nodeBuilderQName = nodeBuilder.getQName();
                TypeDefinition<?> nodeBuilderType = nodeBuilder.getType();
                if(nodeBuilderType != null) {
                    TypeDefinition<?> newType = createCorrectTypeDefinition(parentSchemaPath, nodeBuilderQName, nodeBuilderType);
                    nodeBuilder.setType(newType);
                } else {
                    TypeDefinitionBuilder nodeBuilderTypedef = nodeBuilder.getTypedef();
                    SchemaPath newSchemaPath = createNewSchemaPath(nodeBuilderTypedef.getPath(), nodeBuilderQName, nodeBuilderTypedef.getQName());
                    nodeBuilderTypedef.setPath(newSchemaPath);
                }
            }
        }
    }

    private static TypeDefinition<?> createCorrectTypeDefinition(SchemaPath parentSchemaPath, QName nodeQName, TypeDefinition<?> nodeType) {
        TypeDefinition<?> result = null;
        SchemaPath newSchemaPath = null;
        if(nodeType != null) {
            if(nodeType instanceof BinaryTypeDefinition) {
                BinaryTypeDefinition binType = (BinaryTypeDefinition)nodeType;
                newSchemaPath = createNewSchemaPath(parentSchemaPath, nodeQName, binType.getQName());
                List<Byte> bytes = (List<Byte>)binType.getDefaultValue();
                result = new BinaryType(newSchemaPath, bytes, binType.getLengthConstraints(), binType.getUnits());
            } else if(nodeType instanceof BitsTypeDefinition) {
                BitsTypeDefinition bitsType = (BitsTypeDefinition)nodeType;
                newSchemaPath = createNewSchemaPath(parentSchemaPath, nodeQName, nodeType.getQName());
                result = new BitsType(newSchemaPath, bitsType.getBits(), bitsType.getUnits());
            } else if(nodeType instanceof BooleanTypeDefinition) {
                BooleanTypeDefinition booleanType = (BooleanTypeDefinition)nodeType;
                newSchemaPath = createNewSchemaPath(parentSchemaPath, nodeQName, booleanType.getQName());
                result = new BooleanType(newSchemaPath, (Boolean)booleanType.getDefaultValue(), booleanType.getUnits());
            } else if(nodeType instanceof DecimalTypeDefinition) {
                DecimalTypeDefinition decimalType = (DecimalTypeDefinition)nodeType;
                newSchemaPath = createNewSchemaPath(parentSchemaPath, nodeQName, decimalType.getQName());
                BigDecimal defaultValue = (BigDecimal)decimalType.getDefaultValue();
                result = new Decimal64(newSchemaPath, decimalType.getUnits(), defaultValue, decimalType.getRangeStatements(), decimalType.getFractionDigits());
            } else if(nodeType instanceof EmptyTypeDefinition) {
                newSchemaPath = createNewSchemaPath(parentSchemaPath, nodeQName, nodeType.getQName());
                result = new EmptyType(newSchemaPath);
            } else if(nodeType instanceof EnumTypeDefinition) {
                EnumTypeDefinition enumType = (EnumTypeDefinition)nodeType;
                newSchemaPath = createNewSchemaPath(parentSchemaPath, nodeQName, enumType.getQName());
                result = new EnumerationType(newSchemaPath, (EnumPair)enumType.getDefaultValue(), enumType.getValues(), enumType.getUnits());
            } else if(nodeType instanceof IdentityrefTypeDefinition) {
                IdentityrefTypeDefinition idrefType = (IdentityrefTypeDefinition)nodeType;
                newSchemaPath = createNewSchemaPath(parentSchemaPath, nodeQName, idrefType.getQName());
                result = new IdentityrefType(idrefType.getIdentity(), newSchemaPath);
            } else if(nodeType instanceof InstanceIdentifierTypeDefinition) {
                InstanceIdentifierTypeDefinition instIdType = (InstanceIdentifierTypeDefinition)nodeType;
                newSchemaPath = createNewSchemaPath(parentSchemaPath, nodeQName, instIdType.getQName());
                return new InstanceIdentifier(newSchemaPath, instIdType.getPathStatement(), instIdType.requireInstance());
            } else if(nodeType instanceof StringTypeDefinition) {
                result = copyStringType(parentSchemaPath, nodeQName, (StringTypeDefinition)nodeType);
            } else if(nodeType instanceof IntegerTypeDefinition) {
                result = copyIntType(parentSchemaPath, nodeQName, (IntegerTypeDefinition)nodeType);
            } else if(nodeType instanceof UnsignedIntegerTypeDefinition) {
                result = copyUIntType(parentSchemaPath, nodeQName, (UnsignedIntegerTypeDefinition)nodeType);
            } else if(nodeType instanceof LeafrefTypeDefinition) {
                newSchemaPath = createNewSchemaPath(parentSchemaPath, nodeQName, nodeType.getQName());
                result = new Leafref(newSchemaPath, ((LeafrefTypeDefinition)nodeType).getPathStatement());
            } else if(nodeType instanceof UnionTypeDefinition) {
                UnionTypeDefinition unionType = (UnionTypeDefinition)nodeType;
                newSchemaPath = createNewSchemaPath(parentSchemaPath, nodeQName, unionType.getQName());
                return new UnionType(newSchemaPath, unionType.getTypes());
            }
        }
        return result;
    }

    private static TypeDefinition<?> copyStringType(SchemaPath schemaPath, QName nodeQName, StringTypeDefinition nodeType) {
        List<QName> path = schemaPath.getPath();
        List<QName> newPath = new ArrayList<QName>(path);
        newPath.add(nodeQName);
        newPath.add(nodeType.getQName());
        SchemaPath newSchemaPath = new SchemaPath(newPath, schemaPath.isAbsolute());

        String newDefault = nodeType.getDefaultValue().toString();
        String newUnits = nodeType.getUnits();
        List<LengthConstraint> lengths = nodeType.getLengthStatements();
        List<PatternConstraint> patterns = nodeType.getPatterns();

        return new StringType(newSchemaPath, newDefault, lengths, patterns, newUnits);
    }

    private static TypeDefinition<?> copyIntType(SchemaPath schemaPath, QName nodeQName, IntegerTypeDefinition type) {
        QName typeQName = type.getQName();
        SchemaPath newSchemaPath = createNewSchemaPath(schemaPath, nodeQName, typeQName);

        String localName = typeQName.getLocalName();
        List<RangeConstraint> ranges = type.getRangeStatements();
        String units = type.getUnits();

        if("int8".equals(localName)) {
            Byte defaultValue = (Byte)type.getDefaultValue();
            return new Int8(newSchemaPath, ranges, units, defaultValue);
        } else if("int16".equals(localName)) {
            Short defaultValue = (Short)type.getDefaultValue();
            return new Int16(newSchemaPath, ranges, units, defaultValue);
        } else if("int32".equals(localName)) {
            Integer defaultValue = (Integer)type.getDefaultValue();
            return new Int32(newSchemaPath, ranges, units, defaultValue);
        } else if("int64".equals(localName)) {
            Long defaultValue = (Long)type.getDefaultValue();
            return new Int64(newSchemaPath, ranges, units, defaultValue);
        } else {
            return null;
        }
    }

    private static TypeDefinition<?> copyUIntType(SchemaPath schemaPath, QName nodeQName, UnsignedIntegerTypeDefinition type) {
        QName typeQName = type.getQName();
        SchemaPath newSchemaPath = createNewSchemaPath(schemaPath, nodeQName, typeQName);

        String localName = typeQName.getLocalName();
        List<RangeConstraint> ranges = type.getRangeStatements();
        String units = type.getUnits();

        if("uint8".equals(localName)) {
            Byte defaultValue = (Byte)type.getDefaultValue();
            return new Int8(newSchemaPath, ranges, units, defaultValue);
        } else if("uint16".equals(localName)) {
            Short defaultValue = (Short)type.getDefaultValue();
            return new Int16(newSchemaPath, ranges, units, defaultValue);
        } else if("uint32".equals(localName)) {
            Integer defaultValue = (Integer)type.getDefaultValue();
            return new Int32(newSchemaPath, ranges, units, defaultValue);
        } else if("uint64".equals(localName)) {
            Long defaultValue = (Long)type.getDefaultValue();
            return new Int64(newSchemaPath, ranges, units, defaultValue);
        } else {
            return null;
        }
    }

    private static SchemaPath createNewSchemaPath(SchemaPath schemaPath, QName currentQName, QName qname) {
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
            copy.setType(old.getTypedef());
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
        for (TypeDefinitionBuilder typedef : old.getTypedefs()) {
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
        for (TypeDefinitionBuilder typedef : old.getTypedefs()) {
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
            copy.setType(old.getTypedef());
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
        for (TypeDefinitionBuilder typedef : old.getTypedefs()) {
            copy.addTypedef(typedef);
        }
        for (UsesNodeBuilder use : old.getUsesNodes()) {
            copy.addUsesNode(use);
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
        for (TypeDefinitionBuilder typedef : old.getTypedefs()) {
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

    public static TypedefBuilder copyTypedefBuilder(TypedefBuilder old) {
        final TypedefBuilder copy = new TypedefBuilder(old.getQName(),
                old.getLine());
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
        if(type == null) {
            copy.setType(old.getTypedef());
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
                old.getGroupingPathString(), old.getLine());
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
