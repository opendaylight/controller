/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.yang.parser.util;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.opendaylight.controller.yang.common.QName;
import org.opendaylight.controller.yang.model.api.AnyXmlSchemaNode;
import org.opendaylight.controller.yang.model.api.ChoiceNode;
import org.opendaylight.controller.yang.model.api.ConstraintDefinition;
import org.opendaylight.controller.yang.model.api.ContainerSchemaNode;
import org.opendaylight.controller.yang.model.api.DataNodeContainer;
import org.opendaylight.controller.yang.model.api.DataSchemaNode;
import org.opendaylight.controller.yang.model.api.GroupingDefinition;
import org.opendaylight.controller.yang.model.api.LeafListSchemaNode;
import org.opendaylight.controller.yang.model.api.LeafSchemaNode;
import org.opendaylight.controller.yang.model.api.ListSchemaNode;
import org.opendaylight.controller.yang.model.api.Module;
import org.opendaylight.controller.yang.model.api.ModuleImport;
import org.opendaylight.controller.yang.model.api.MustDefinition;
import org.opendaylight.controller.yang.model.api.NotificationDefinition;
import org.opendaylight.controller.yang.model.api.RevisionAwareXPath;
import org.opendaylight.controller.yang.model.api.SchemaContext;
import org.opendaylight.controller.yang.model.api.SchemaNode;
import org.opendaylight.controller.yang.model.api.SchemaPath;
import org.opendaylight.controller.yang.model.api.TypeDefinition;
import org.opendaylight.controller.yang.model.api.UnknownSchemaNode;
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
import org.opendaylight.controller.yang.model.util.Uint16;
import org.opendaylight.controller.yang.model.util.Uint32;
import org.opendaylight.controller.yang.model.util.Uint64;
import org.opendaylight.controller.yang.model.util.Uint8;
import org.opendaylight.controller.yang.model.util.UnionType;
import org.opendaylight.controller.yang.model.util.UnknownType;
import org.opendaylight.controller.yang.parser.builder.api.AugmentationSchemaBuilder;
import org.opendaylight.controller.yang.parser.builder.api.AugmentationTargetBuilder;
import org.opendaylight.controller.yang.parser.builder.api.Builder;
import org.opendaylight.controller.yang.parser.builder.api.DataNodeContainerBuilder;
import org.opendaylight.controller.yang.parser.builder.api.DataSchemaNodeBuilder;
import org.opendaylight.controller.yang.parser.builder.api.GroupingBuilder;
import org.opendaylight.controller.yang.parser.builder.api.GroupingMember;
import org.opendaylight.controller.yang.parser.builder.api.SchemaNodeBuilder;
import org.opendaylight.controller.yang.parser.builder.api.TypeAwareBuilder;
import org.opendaylight.controller.yang.parser.builder.api.TypeDefinitionBuilder;
import org.opendaylight.controller.yang.parser.builder.api.UsesNodeBuilder;
import org.opendaylight.controller.yang.parser.builder.impl.AnyXmlBuilder;
import org.opendaylight.controller.yang.parser.builder.impl.ChoiceBuilder;
import org.opendaylight.controller.yang.parser.builder.impl.ChoiceBuilder.ChoiceNodeImpl;
import org.opendaylight.controller.yang.parser.builder.impl.ChoiceCaseBuilder;
import org.opendaylight.controller.yang.parser.builder.impl.ChoiceCaseBuilder.ChoiceCaseNodeImpl;
import org.opendaylight.controller.yang.parser.builder.impl.ConstraintsBuilder;
import org.opendaylight.controller.yang.parser.builder.impl.ContainerSchemaNodeBuilder;
import org.opendaylight.controller.yang.parser.builder.impl.ContainerSchemaNodeBuilder.ContainerSchemaNodeImpl;
import org.opendaylight.controller.yang.parser.builder.impl.GroupingBuilderImpl;
import org.opendaylight.controller.yang.parser.builder.impl.LeafListSchemaNodeBuilder;
import org.opendaylight.controller.yang.parser.builder.impl.LeafSchemaNodeBuilder;
import org.opendaylight.controller.yang.parser.builder.impl.ListSchemaNodeBuilder;
import org.opendaylight.controller.yang.parser.builder.impl.ListSchemaNodeBuilder.ListSchemaNodeImpl;
import org.opendaylight.controller.yang.parser.builder.impl.ModuleBuilder;
import org.opendaylight.controller.yang.parser.builder.impl.NotificationBuilder;
import org.opendaylight.controller.yang.parser.builder.impl.NotificationBuilder.NotificationDefinitionImpl;
import org.opendaylight.controller.yang.parser.builder.impl.RpcDefinitionBuilder;
import org.opendaylight.controller.yang.parser.builder.impl.TypeDefinitionBuilderImpl;
import org.opendaylight.controller.yang.parser.builder.impl.UnionTypeBuilder;
import org.opendaylight.controller.yang.parser.builder.impl.UnknownSchemaNodeBuilder;

public final class ParserUtils {

    private ParserUtils() {
    }

    /**
     * Create new SchemaPath from given path and name.
     *
     * Append new qname to schema path created from name argument. New QName
     * gets namespace, revision and prefix same as last qname in current schema
     * path.
     *
     * @param schemaPath
     * @param name
     * @return
     */
    public static SchemaPath createSchemaPath(SchemaPath schemaPath, String name) {
        List<QName> path = new ArrayList<QName>(schemaPath.getPath());
        QName last = path.get(path.size() - 1);
        QName newQName = new QName(last.getNamespace(), last.getRevision(), last.getPrefix(), name);
        path.add(newQName);
        return new SchemaPath(path, schemaPath.isAbsolute());
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
    public static ModuleImport getModuleImport(final ModuleBuilder builder, final String prefix) {
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
     * Find dependent module based on given prefix
     *
     * @param modules
     *            all available modules
     * @param module
     *            current module
     * @param prefix
     *            target module prefix
     * @param line
     *            current line in yang model
     * @return
     */
    public static ModuleBuilder findDependentModuleBuilder(final Map<String, TreeMap<Date, ModuleBuilder>> modules,
            final ModuleBuilder module, final String prefix, final int line) {
        ModuleBuilder dependentModule = null;
        Date dependentModuleRevision = null;

        if (prefix.equals(module.getPrefix())) {
            dependentModule = module;
        } else {
            final ModuleImport dependentModuleImport = getModuleImport(module, prefix);
            if (dependentModuleImport == null) {
                throw new YangParseException(module.getName(), line, "No import found with prefix '" + prefix + "'.");
            }
            final String dependentModuleName = dependentModuleImport.getModuleName();
            dependentModuleRevision = dependentModuleImport.getRevision();

            final TreeMap<Date, ModuleBuilder> moduleBuildersByRevision = modules.get(dependentModuleName);
            if (moduleBuildersByRevision == null) {
                return null;
            }
            if (dependentModuleRevision == null) {
                dependentModule = moduleBuildersByRevision.lastEntry().getValue();
            } else {
                dependentModule = moduleBuildersByRevision.get(dependentModuleRevision);
            }
        }
        return dependentModule;
    }

    /**
     * Find module from context based on prefix.
     *
     * @param context
     *            schema context
     * @param currentModule
     *            current module
     * @param prefix
     *            current prefix used to reference dependent module
     * @param line
     *            current line in yang model
     * @return module based on given prefix if found in context, null otherwise
     */
    public static Module findModuleFromContext(final SchemaContext context, final ModuleBuilder currentModule,
            final String prefix, final int line) {
        TreeMap<Date, Module> modulesByRevision = new TreeMap<Date, Module>();

        Date dependentModuleRevision = null;

        final ModuleImport dependentModuleImport = ParserUtils.getModuleImport(currentModule, prefix);
        if (dependentModuleImport == null) {
            throw new YangParseException(currentModule.getName(), line, "No import found with prefix '" + prefix + "'.");
        }
        final String dependentModuleName = dependentModuleImport.getModuleName();
        dependentModuleRevision = dependentModuleImport.getRevision();

        for (Module contextModule : context.getModules()) {
            if (contextModule.getName().equals(dependentModuleName)) {
                Date revision = contextModule.getRevision();
                if (revision == null) {
                    revision = new Date(0L);
                }
                modulesByRevision.put(revision, contextModule);
                break;
            }
        }

        Module result = null;
        if (dependentModuleRevision == null) {
            result = modulesByRevision.get(modulesByRevision.firstKey());
        } else {
            result = modulesByRevision.get(dependentModuleRevision);
        }

        return result;
    }

    /**
     * Find grouping by name.
     *
     * @param groupings
     *            collection of grouping builders to search
     * @param name
     *            name of grouping
     * @return grouping with given name if present in collection, null otherwise
     */
    public static GroupingBuilder findGroupingBuilder(Set<GroupingBuilder> groupings, String name) {
        for (GroupingBuilder grouping : groupings) {
            if (grouping.getQName().getLocalName().equals(name)) {
                return grouping;
            }
        }
        return null;
    }

    /**
     * Find grouping by name.
     *
     * @param groupings
     *            collection of grouping definitions to search
     * @param name
     *            name of grouping
     * @return grouping with given name if present in collection, null otherwise
     */
    public static GroupingDefinition findGroupingDefinition(Set<GroupingDefinition> groupings, String name) {
        for (GroupingDefinition grouping : groupings) {
            if (grouping.getQName().getLocalName().equals(name)) {
                return grouping;
            }
        }
        return null;
    }

    /**
     * Search types for type with given name.
     *
     * @param types
     *            types to search
     * @param name
     *            name of type
     * @return type with given name if present in collection, null otherwise
     */
    public static TypeDefinitionBuilder findTypedefBuilderByName(Set<TypeDefinitionBuilder> types, String name) {
        for (TypeDefinitionBuilder td : types) {
            if (td.getQName().getLocalName().equals(name)) {
                return td;
            }
        }
        return null;
    }

    /**
     * Find type by name.
     *
     * @param types
     *            collection of types
     * @param typeName
     *            type name
     * @return type with given name if it is present in collection, null
     *         otherwise
     */
    public static TypeDefinition<?> findTypeByName(Set<TypeDefinition<?>> types, String typeName) {
        for (TypeDefinition<?> type : types) {
            if (type.getQName().getLocalName().equals(typeName)) {
                return type;
            }
        }
        return null;
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
                    name = new QName(null, null, splittedElement[0], splittedElement[1]);
                }
                path.add(name);
            }
        }
        return new SchemaPath(path, absolute);
    }

    /**
     * Check if node is present in refine nodes.
     *
     * @param nodeQName
     *            qname of node
     * @param refineNodes
     *            collections of refined nodes
     * @return true, if node with given qname was found, false otherwise
     */
    public static SchemaNodeBuilder getRefined(QName nodeQName, List<SchemaNodeBuilder> refineNodes) {
        for (SchemaNodeBuilder rn : refineNodes) {
            if (rn.getQName().equals(nodeQName)) {
                return rn;
            }
        }
        return null;
    }

    /**
     * Pull restriction from type and add them to constraints.
     *
     * @param type
     * @param constraints
     */
    public static void mergeConstraints(final TypeDefinition<?> type, final TypeConstraints constraints) {
        if (type instanceof DecimalTypeDefinition) {
            constraints.addRanges(((DecimalTypeDefinition) type).getRangeStatements());
            constraints.addFractionDigits(((DecimalTypeDefinition) type).getFractionDigits());
        } else if (type instanceof IntegerTypeDefinition) {
            constraints.addRanges(((IntegerTypeDefinition) type).getRangeStatements());
        } else if (type instanceof StringTypeDefinition) {
            constraints.addPatterns(((StringTypeDefinition) type).getPatterns());
            constraints.addLengths(((StringTypeDefinition) type).getLengthStatements());
        } else if (type instanceof BinaryTypeDefinition) {
            constraints.addLengths(((BinaryTypeDefinition) type).getLengthConstraints());
        }
    }

    /**
     * Find node in grouping by name.
     *
     * @param grouping
     *            grouping to search
     * @param refineNodeName
     *            name of node
     * @return builder of node with given name if present in grouping, null
     *         otherwise
     */
    public static Builder findRefineTargetBuilder(final GroupingBuilder grouping, final String refineNodeName) {
        // search child nodes
        Builder result = grouping.getChildNode(refineNodeName);
        // search groupings
        if (result == null) {
            Set<GroupingBuilder> grps = grouping.getGroupingBuilders();
            for (GroupingBuilder gr : grps) {
                if (gr.getQName().getLocalName().equals(refineNodeName)) {
                    result = gr;
                    break;
                }
            }
        }
        // search typedefs
        if (result == null) {
            Set<TypeDefinitionBuilder> typedefs = grouping.getTypeDefinitionBuilders();
            for (TypeDefinitionBuilder typedef : typedefs) {
                if (typedef.getQName().getLocalName().equals(refineNodeName)) {
                    result = typedef;
                    break;
                }
            }
        }
        return result;
    }

    /**
     * Find node in grouping by name.
     *
     * @param builder
     *            grouping to search
     * @param refineNodeName
     *            name of node
     * @return node with given name if present in grouping, null otherwise
     */
    public static Object findRefineTargetNode(final GroupingDefinition builder, final String refineNodeName) {
        Object result = builder.getDataChildByName(refineNodeName);
        if (result == null) {
            Set<GroupingDefinition> grps = builder.getGroupings();
            for (GroupingDefinition gr : grps) {
                if (gr.getQName().getLocalName().equals(refineNodeName)) {
                    result = gr;
                    break;
                }
            }
        }
        if (result == null) {
            Set<TypeDefinition<?>> typedefs = builder.getTypeDefinitions();
            for (TypeDefinition<?> typedef : typedefs) {
                if (typedef.getQName().getLocalName().equals(refineNodeName)) {
                    result = typedef;
                    break;
                }
            }
        }
        return result;
    }

    /**
     * Add all augment's child nodes to given target.
     *
     * @param augment
     *            builder of augment statement
     * @param target
     *            augmentation target node
     */
    public static void fillAugmentTarget(final AugmentationSchemaBuilder augment, final DataNodeContainerBuilder target) {
        boolean usesAugment = augment.getParent() instanceof UsesNodeBuilder;
        for (DataSchemaNodeBuilder builder : augment.getChildNodeBuilders()) {
            builder.setAugmenting(true);
            if (usesAugment) {
                if (builder instanceof GroupingMember) {
                    ((GroupingMember) builder).setAddedByUses(true);
                }
            }
            correctAugmentChildPath(builder, target.getPath());
            target.addChildNode(builder);
        }
    }

    /**
     * Add all augment's child nodes to given target.
     *
     * @param augment
     *            builder of augment statement
     * @param target
     *            augmentation target choice node
     */
    public static void fillAugmentTarget(final AugmentationSchemaBuilder augment, final ChoiceBuilder target) {
        boolean usesAugment = augment.getParent() instanceof UsesNodeBuilder;
        for (DataSchemaNodeBuilder builder : augment.getChildNodeBuilders()) {
            builder.setAugmenting(true);
            if (usesAugment) {
                if (builder instanceof GroupingMember) {
                    ((GroupingMember) builder).setAddedByUses(true);
                }
            }
            correctAugmentChildPath(builder, target.getPath());
            target.addChildNode(builder);
        }
    }

    private static void correctAugmentChildPath(final DataSchemaNodeBuilder childNode, final SchemaPath parentSchemaPath) {
        // set correct path
        List<QName> targetNodePath = new ArrayList<QName>(parentSchemaPath.getPath());
        targetNodePath.add(childNode.getQName());
        childNode.setPath(new SchemaPath(targetNodePath, true));

        // set correct path for all child nodes
        if (childNode instanceof DataNodeContainerBuilder) {
            DataNodeContainerBuilder dataNodeContainer = (DataNodeContainerBuilder) childNode;
            for (DataSchemaNodeBuilder child : dataNodeContainer.getChildNodeBuilders()) {
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
    private static void correctTypeAwareNodePath(final TypeAwareBuilder node, final SchemaPath parentSchemaPath) {
        final QName nodeBuilderQName = node.getQName();
        final TypeDefinition<?> nodeType = node.getType();

        Integer fd = null;
        List<LengthConstraint> lengths = null;
        List<PatternConstraint> patterns = null;
        List<RangeConstraint> ranges = null;

        if (nodeType != null) {
            if (nodeType instanceof ExtendedType) {
                ExtendedType et = (ExtendedType) nodeType;
                if (nodeType.getQName().getLocalName().equals(nodeType.getBaseType().getQName().getLocalName())) {
                    fd = et.getFractionDigits();
                    lengths = et.getLengths();
                    patterns = et.getPatterns();
                    ranges = et.getRanges();
                    if (!hasConstraints(fd, lengths, patterns, ranges)) {
                        return;
                    }
                }
            }
            TypeDefinition<?> newType = createCorrectTypeDefinition(parentSchemaPath, nodeBuilderQName, nodeType);
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
                baseTypeName = nodeBuilderTypedef.getTypedef().getQName().getLocalName();
            } else {
                baseTypeName = nodeBuilderTypedef.getType().getQName().getLocalName();
            }
            if (!(tdbTypeName.equals(baseTypeName))) {
                return;
            }

            if (!hasConstraints(fd, lengths, patterns, ranges)) {
                return;
            }

            SchemaPath newSchemaPath = createNewSchemaPath(nodeBuilderTypedef.getPath(), nodeBuilderQName,
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
    private static boolean hasConstraints(final Integer fd, final List<LengthConstraint> lengths,
            final List<PatternConstraint> patterns, final List<RangeConstraint> ranges) {
        if (fd == null && (lengths == null || lengths.isEmpty()) && (patterns == null || patterns.isEmpty())
                && (ranges == null || ranges.isEmpty())) {
            return false;
        } else {
            return true;
        }
    }

    private static TypeDefinition<?> createCorrectTypeDefinition(SchemaPath parentSchemaPath, QName nodeQName,
            TypeDefinition<?> nodeType) {
        QName nodeTypeQName = nodeType.getQName();
        SchemaPath newSchemaPath = createNewSchemaPath(parentSchemaPath, nodeQName, nodeTypeQName);
        TypeDefinition<?> result = null;

        if (nodeType != null) {
            if (nodeType instanceof BinaryTypeDefinition) {
                BinaryTypeDefinition binType = (BinaryTypeDefinition) nodeType;

                // List<Byte> bytes = (List<Byte>) binType.getDefaultValue();
                // workaround to get rid of 'Unchecked cast' warning
                List<Byte> bytes = new ArrayList<Byte>();
                Object defaultValue = binType.getDefaultValue();
                if (defaultValue instanceof List) {
                    for (Object o : List.class.cast(defaultValue)) {
                        if (o instanceof Byte) {
                            bytes.add((Byte) o);
                        }
                    }
                }
                result = new BinaryType(newSchemaPath, bytes);
            } else if (nodeType instanceof BitsTypeDefinition) {
                BitsTypeDefinition bitsType = (BitsTypeDefinition) nodeType;
                result = new BitsType(newSchemaPath, bitsType.getBits());
            } else if (nodeType instanceof BooleanTypeDefinition) {
                result = new BooleanType(newSchemaPath);
            } else if (nodeType instanceof DecimalTypeDefinition) {
                DecimalTypeDefinition decimalType = (DecimalTypeDefinition) nodeType;
                result = new Decimal64(newSchemaPath, decimalType.getFractionDigits());
            } else if (nodeType instanceof EmptyTypeDefinition) {
                result = new EmptyType(newSchemaPath);
            } else if (nodeType instanceof EnumTypeDefinition) {
                EnumTypeDefinition enumType = (EnumTypeDefinition) nodeType;
                result = new EnumerationType(newSchemaPath, (EnumPair) enumType.getDefaultValue(), enumType.getValues());
            } else if (nodeType instanceof IdentityrefTypeDefinition) {
                IdentityrefTypeDefinition idrefType = (IdentityrefTypeDefinition) nodeType;
                result = new IdentityrefType(idrefType.getIdentity(), newSchemaPath);
            } else if (nodeType instanceof InstanceIdentifierTypeDefinition) {
                InstanceIdentifierTypeDefinition instIdType = (InstanceIdentifierTypeDefinition) nodeType;
                return new InstanceIdentifier(newSchemaPath, instIdType.getPathStatement(),
                        instIdType.requireInstance());
            } else if (nodeType instanceof StringTypeDefinition) {
                result = createNewStringType(parentSchemaPath, nodeQName, (StringTypeDefinition) nodeType);
            } else if (nodeType instanceof IntegerTypeDefinition) {
                result = createNewIntType(parentSchemaPath, nodeQName, (IntegerTypeDefinition) nodeType);
            } else if (nodeType instanceof UnsignedIntegerTypeDefinition) {
                result = createNewUintType(parentSchemaPath, nodeQName, (UnsignedIntegerTypeDefinition) nodeType);
            } else if (nodeType instanceof LeafrefTypeDefinition) {
                result = new Leafref(newSchemaPath, ((LeafrefTypeDefinition) nodeType).getPathStatement());
            } else if (nodeType instanceof UnionTypeDefinition) {
                UnionTypeDefinition unionType = (UnionTypeDefinition) nodeType;
                return new UnionType(newSchemaPath, unionType.getTypes());
            } else if (nodeType instanceof ExtendedType) {
                ExtendedType extType = (ExtendedType) nodeType;
                result = createNewExtendedType(extType, newSchemaPath);
            }
        }
        return result;
    }

    /**
     * Create new ExtendedType based on given type and with schema path.
     *
     * @param newPath
     *            schema path for new type
     * @param oldType
     *            type based
     * @return
     */
    private static ExtendedType createNewExtendedType(final ExtendedType oldType, final SchemaPath newPath) {
        QName qname = oldType.getQName();
        TypeDefinition<?> baseType = oldType.getBaseType();
        String desc = oldType.getDescription();
        String ref = oldType.getReference();
        ExtendedType.Builder builder = new ExtendedType.Builder(qname, baseType, desc, ref, newPath);
        builder.status(oldType.getStatus());
        builder.lengths(oldType.getLengths());
        builder.patterns(oldType.getPatterns());
        builder.ranges(oldType.getRanges());
        builder.fractionDigits(oldType.getFractionDigits());
        builder.unknownSchemaNodes(oldType.getUnknownSchemaNodes());
        return builder.build();
    }

    private static StringTypeDefinition createNewStringType(final SchemaPath schemaPath, final QName nodeQName,
            final StringTypeDefinition nodeType) {
        final List<QName> path = schemaPath.getPath();
        final List<QName> newPath = new ArrayList<QName>(path);
        newPath.add(nodeQName);
        newPath.add(nodeType.getQName());
        final SchemaPath newSchemaPath = new SchemaPath(newPath, schemaPath.isAbsolute());
        return new StringType(newSchemaPath);
    }

    private static IntegerTypeDefinition createNewIntType(final SchemaPath schemaPath, final QName nodeQName,
            final IntegerTypeDefinition type) {
        final QName typeQName = type.getQName();
        final SchemaPath newSchemaPath = createNewSchemaPath(schemaPath, nodeQName, typeQName);
        final String localName = typeQName.getLocalName();

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

    private static UnsignedIntegerTypeDefinition createNewUintType(final SchemaPath schemaPath, final QName nodeQName,
            final UnsignedIntegerTypeDefinition type) {
        final QName typeQName = type.getQName();
        final SchemaPath newSchemaPath = createNewSchemaPath(schemaPath, nodeQName, typeQName);
        final String localName = typeQName.getLocalName();

        if ("uint8".equals(localName)) {
            return new Uint8(newSchemaPath);
        } else if ("uint16".equals(localName)) {
            return new Uint16(newSchemaPath);
        } else if ("uint32".equals(localName)) {
            return new Uint32(newSchemaPath);
        } else if ("uint64".equals(localName)) {
            return new Uint64(newSchemaPath);
        } else {
            return null;
        }
    }

    private static SchemaPath createNewSchemaPath(final SchemaPath schemaPath, final QName currentQName,
            final QName qname) {
        List<QName> newPath = new ArrayList<QName>(schemaPath.getPath());
        newPath.add(currentQName);
        newPath.add(qname);
        return new SchemaPath(newPath, schemaPath.isAbsolute());
    }

    /**
     * Create LeafSchemaNodeBuilder from given LeafSchemaNode.
     *
     * @param leaf
     *            leaf from which to create builder
     * @param line
     *            line in module
     * @return builder object from leaf
     */
    public static LeafSchemaNodeBuilder createLeafBuilder(LeafSchemaNode leaf, int line) {
        final LeafSchemaNodeBuilder builder = new LeafSchemaNodeBuilder(leaf.getQName(), leaf.getPath(), line);
        convertDataSchemaNode(leaf, builder);
        builder.setConfiguration(leaf.isConfiguration());
        final TypeDefinition<?> type = leaf.getType();
        builder.setType(type);
        builder.setPath(leaf.getPath());
        builder.setUnknownNodes(leaf.getUnknownSchemaNodes());
        builder.setDefaultStr(leaf.getDefault());
        builder.setUnits(leaf.getUnits());
        return builder;
    }

    public static ContainerSchemaNodeBuilder createContainer(ContainerSchemaNode container, int line) {
        final ContainerSchemaNodeBuilder builder = new ContainerSchemaNodeBuilder(line, container.getQName(),
                container.getPath());
        convertDataSchemaNode(container, builder);
        builder.setConfiguration(container.isConfiguration());
        builder.setUnknownNodes(container.getUnknownSchemaNodes());
        builder.setChildNodes(container.getChildNodes());
        builder.setGroupings(container.getGroupings());
        builder.setTypedefs(container.getTypeDefinitions());
        builder.setAugmentations(container.getAvailableAugmentations());
        builder.setUsesnodes(container.getUses());
        builder.setPresence(container.isPresenceContainer());
        return builder;
    }

    public static ListSchemaNodeBuilder createList(ListSchemaNode list, int line) {
        ListSchemaNodeBuilder builder = new ListSchemaNodeBuilder(line, list.getQName(), list.getPath());
        convertDataSchemaNode(list, builder);
        builder.setConfiguration(list.isConfiguration());
        builder.setUnknownNodes(list.getUnknownSchemaNodes());
        builder.setTypedefs(list.getTypeDefinitions());
        builder.setChildNodes(list.getChildNodes());
        builder.setGroupings(list.getGroupings());
        builder.setAugmentations(list.getAvailableAugmentations());
        builder.setUsesnodes(list.getUses());
        builder.setUserOrdered(builder.isUserOrdered());
        return builder;
    }

    public static LeafListSchemaNodeBuilder createLeafList(LeafListSchemaNode leafList, int line) {
        final LeafListSchemaNodeBuilder builder = new LeafListSchemaNodeBuilder(line, leafList.getQName(),
                leafList.getPath());
        convertDataSchemaNode(leafList, builder);
        builder.setConfiguration(leafList.isConfiguration());
        builder.setType(leafList.getType());
        builder.setUnknownNodes(leafList.getUnknownSchemaNodes());
        builder.setUserOrdered(leafList.isUserOrdered());
        return builder;
    }

    public static ChoiceBuilder createChoice(ChoiceNode choice, int line) {
        final ChoiceBuilder builder = new ChoiceBuilder(line, choice.getQName());
        convertDataSchemaNode(choice, builder);
        builder.setConfiguration(choice.isConfiguration());
        builder.setCases(choice.getCases());
        builder.setUnknownNodes(choice.getUnknownSchemaNodes());
        builder.setDefaultCase(choice.getDefaultCase());
        return builder;
    }

    public static AnyXmlBuilder createAnyXml(AnyXmlSchemaNode anyxml, int line) {
        final AnyXmlBuilder builder = new AnyXmlBuilder(line, anyxml.getQName(), anyxml.getPath());
        convertDataSchemaNode(anyxml, builder);
        builder.setConfiguration(anyxml.isConfiguration());
        builder.setUnknownNodes(anyxml.getUnknownSchemaNodes());
        return builder;
    }

    public static GroupingBuilder createGrouping(GroupingDefinition grouping, int line) {
        final GroupingBuilderImpl builder = new GroupingBuilderImpl(grouping.getQName(), line);
        builder.setPath(grouping.getPath());
        builder.setChildNodes(grouping.getChildNodes());
        builder.setGroupings(grouping.getGroupings());
        builder.setTypedefs(grouping.getTypeDefinitions());
        builder.setUsesnodes(grouping.getUses());
        builder.setUnknownNodes(grouping.getUnknownSchemaNodes());
        builder.setDescription(grouping.getDescription());
        builder.setReference(grouping.getReference());
        builder.setStatus(grouping.getStatus());
        return builder;
    }

    public static TypeDefinitionBuilder createTypedef(ExtendedType typedef, int line) {
        final TypeDefinitionBuilderImpl builder = new TypeDefinitionBuilderImpl(typedef.getQName(), line);
        builder.setPath(typedef.getPath());
        builder.setDefaultValue(typedef.getDefaultValue());
        builder.setUnits(typedef.getUnits());
        builder.setDescription(typedef.getDescription());
        builder.setReference(typedef.getReference());
        builder.setStatus(typedef.getStatus());
        builder.setRanges(typedef.getRanges());
        builder.setLengths(typedef.getLengths());
        builder.setPatterns(typedef.getPatterns());
        builder.setFractionDigits(typedef.getFractionDigits());
        final TypeDefinition<?> type = typedef.getBaseType();
        builder.setType(type);
        builder.setUnits(typedef.getUnits());
        builder.setUnknownNodes(typedef.getUnknownSchemaNodes());
        return builder;
    }

    public static UnknownSchemaNodeBuilder createUnknownSchemaNode(UnknownSchemaNode unknownNode, int line) {
        final UnknownSchemaNodeBuilder builder = new UnknownSchemaNodeBuilder(line, unknownNode.getQName());
        builder.setPath(unknownNode.getPath());
        builder.setUnknownNodes(unknownNode.getUnknownSchemaNodes());
        builder.setDescription(unknownNode.getDescription());
        builder.setReference(unknownNode.getReference());
        builder.setStatus(unknownNode.getStatus());
        builder.setAddedByUses(unknownNode.isAddedByUses());
        builder.setNodeType(unknownNode.getNodeType());
        builder.setNodeParameter(unknownNode.getNodeParameter());
        return builder;
    }

    /**
     * Set DataSchemaNode arguments to builder object
     *
     * @param node
     *            node from which arguments should be read
     * @param builder
     *            builder to which arguments should be set
     */
    private static void convertDataSchemaNode(DataSchemaNode node, DataSchemaNodeBuilder builder) {
        builder.setPath(node.getPath());
        builder.setDescription(node.getDescription());
        builder.setReference(node.getReference());
        builder.setStatus(node.getStatus());
        builder.setAugmenting(node.isAugmenting());
        copyConstraintsFromDefinition(node.getConstraints(), builder.getConstraints());
    }

    /**
     * Copy constraints from constraints definition to constraints builder.
     *
     * @param nodeConstraints
     *            definition from which constraints will be copied
     * @param constraints
     *            builder to which constraints will be added
     */
    private static void copyConstraintsFromDefinition(final ConstraintDefinition nodeConstraints,
            final ConstraintsBuilder constraints) {
        final RevisionAwareXPath when = nodeConstraints.getWhenCondition();
        final Set<MustDefinition> must = nodeConstraints.getMustConstraints();

        if (when != null) {
            constraints.addWhenCondition(when.toString());
        }
        if (must != null) {
            for (MustDefinition md : must) {
                constraints.addMustDefinition(md);
            }
        }
        constraints.setMandatory(nodeConstraints.isMandatory());
        constraints.setMinElements(nodeConstraints.getMinElements());
        constraints.setMaxElements(nodeConstraints.getMaxElements());
    }

    public static void processAugmentationOnContext(final AugmentationSchemaBuilder augmentBuilder,
            final List<QName> path, final ModuleBuilder module, final String prefix, final int line,
            final SchemaContext context) {
        final Module dependentModule = findModuleFromContext(context, module, prefix, line);
        if (dependentModule == null) {
            throw new YangParseException(module.getName(), line, "Failed to find referenced module with prefix "
                    + prefix + ".");
        }
        SchemaNode node = dependentModule.getDataChildByName(path.get(0).getLocalName());
        if (node == null) {
            Set<NotificationDefinition> notifications = dependentModule.getNotifications();
            for (NotificationDefinition ntf : notifications) {
                if (ntf.getQName().getLocalName().equals(path.get(0).getLocalName())) {
                    node = ntf;
                    break;
                }
            }
        }
        if (node == null) {
            return;
        }

        for (int i = 1; i < path.size(); i++) {
            if (node instanceof DataNodeContainer) {
                DataNodeContainer ref = (DataNodeContainer) node;
                node = ref.getDataChildByName(path.get(i).getLocalName());
            }
        }
        if (node == null) {
            return;
        }

        if (node instanceof ContainerSchemaNodeImpl) {
            // includes container, input and output statement
            ContainerSchemaNodeImpl c = (ContainerSchemaNodeImpl) node;
            ContainerSchemaNodeBuilder cb = c.toBuilder();
            fillAugmentTarget(augmentBuilder, cb);
            ((AugmentationTargetBuilder) cb).addAugmentation(augmentBuilder);
            SchemaPath oldPath = cb.getPath();
            cb.rebuild();
            augmentBuilder.setTargetPath(new SchemaPath(oldPath.getPath(), oldPath.isAbsolute()));
            augmentBuilder.setResolved(true);
            module.augmentResolved();
        } else if (node instanceof ListSchemaNodeImpl) {
            ListSchemaNodeImpl l = (ListSchemaNodeImpl) node;
            ListSchemaNodeBuilder lb = l.toBuilder();
            fillAugmentTarget(augmentBuilder, lb);
            ((AugmentationTargetBuilder) lb).addAugmentation(augmentBuilder);
            SchemaPath oldPath = lb.getPath();
            lb.rebuild();
            augmentBuilder.setTargetPath(new SchemaPath(oldPath.getPath(), oldPath.isAbsolute()));
            augmentBuilder.setResolved(true);
            module.augmentResolved();
        } else if (node instanceof ChoiceNodeImpl) {
            ChoiceNodeImpl ch = (ChoiceNodeImpl) node;
            ChoiceBuilder chb = ch.toBuilder();
            fillAugmentTarget(augmentBuilder, chb);
            ((AugmentationTargetBuilder) chb).addAugmentation(augmentBuilder);
            SchemaPath oldPath = chb.getPath();
            chb.rebuild();
            augmentBuilder.setTargetPath(new SchemaPath(oldPath.getPath(), oldPath.isAbsolute()));
            augmentBuilder.setResolved(true);
            module.augmentResolved();
        } else if (node instanceof ChoiceCaseNodeImpl) {
            ChoiceCaseNodeImpl chc = (ChoiceCaseNodeImpl) node;
            ChoiceCaseBuilder chcb = chc.toBuilder();
            fillAugmentTarget(augmentBuilder, chcb);
            ((AugmentationTargetBuilder) chcb).addAugmentation(augmentBuilder);
            SchemaPath oldPath = chcb.getPath();
            chcb.rebuild();
            augmentBuilder.setTargetPath(new SchemaPath(oldPath.getPath(), oldPath.isAbsolute()));
            augmentBuilder.setResolved(true);
            module.augmentResolved();
        } else if (node instanceof NotificationDefinitionImpl) {
            NotificationDefinitionImpl nd = (NotificationDefinitionImpl) node;
            NotificationBuilder nb = nd.toBuilder();
            fillAugmentTarget(augmentBuilder, nb);
            ((AugmentationTargetBuilder) nb).addAugmentation(augmentBuilder);
            SchemaPath oldPath = nb.getPath();
            nb.rebuild();
            augmentBuilder.setTargetPath(new SchemaPath(oldPath.getPath(), oldPath.isAbsolute()));
            augmentBuilder.setResolved(true);
            module.augmentResolved();
        } else {
            throw new YangParseException(module.getName(), line, "Target of type " + node.getClass()
                    + " can not be augmented.");
        }
    }

    public static void processAugmentation(final AugmentationSchemaBuilder augmentBuilder, final List<QName> path,
            final ModuleBuilder module, final ModuleBuilder dependentModuleBuilder) {
        DataSchemaNodeBuilder currentParent = null;
        for (DataSchemaNodeBuilder child : dependentModuleBuilder.getChildNodeBuilders()) {
            final QName childQName = child.getQName();
            if (childQName.getLocalName().equals(path.get(0).getLocalName())) {
                currentParent = child;
                break;
            }
        }

        if (currentParent == null) {
            return;
        }

        for (int i = 1; i < path.size(); i++) {
            final QName currentQName = path.get(i);
            DataSchemaNodeBuilder newParent = null;
            if (currentParent instanceof DataNodeContainerBuilder) {
                for (DataSchemaNodeBuilder child : ((DataNodeContainerBuilder) currentParent).getChildNodeBuilders()) {
                    final QName childQName = child.getQName();
                    if (childQName.getLocalName().equals(currentQName.getLocalName())) {
                        newParent = child;
                        break;
                    }
                }
            } else if (currentParent instanceof ChoiceBuilder) {
                for (ChoiceCaseBuilder caseBuilder : ((ChoiceBuilder) currentParent).getCases()) {
                    final QName caseQName = caseBuilder.getQName();
                    if (caseQName.getLocalName().equals(currentQName.getLocalName())) {
                        newParent = caseBuilder;
                        break;
                    }
                }
            }

            if (newParent == null) {
                break; // node not found, quit search
            } else {
                currentParent = newParent;
            }
        }

        final String currentName = currentParent.getQName().getLocalName();
        final String lastAugmentPathElementName = path.get(path.size() - 1).getLocalName();
        if (currentName.equals(lastAugmentPathElementName)) {

            if (currentParent instanceof ChoiceBuilder) {
                fillAugmentTarget(augmentBuilder, (ChoiceBuilder) currentParent);
            } else {
                fillAugmentTarget(augmentBuilder, (DataNodeContainerBuilder) currentParent);
            }
            ((AugmentationTargetBuilder) currentParent).addAugmentation(augmentBuilder);
            SchemaPath oldPath = currentParent.getPath();
            augmentBuilder.setTargetPath(new SchemaPath(oldPath.getPath(), oldPath.isAbsolute()));
            augmentBuilder.setResolved(true);
            module.augmentResolved();
        }
    }

    /**
     * Create new type builder based on old type with new base type.
     *
     * @param newBaseType
     *            new base type builder
     * @param oldExtendedType
     *            old type
     * @param modules
     *            all loaded modules
     * @param module
     *            current module
     * @param line
     *            current line in module
     * @return new type builder based on old type with new base type
     */
    public static TypeDefinitionBuilder extendedTypeWithNewBaseTypeBuilder(final TypeDefinitionBuilder newBaseType,
            final ExtendedType oldExtendedType, final Map<String, TreeMap<Date, ModuleBuilder>> modules,
            final ModuleBuilder module, final int line) {
        final TypeConstraints tc = new TypeConstraints(module.getName(), line);
        tc.addFractionDigits(oldExtendedType.getFractionDigits());
        tc.addLengths(oldExtendedType.getLengths());
        tc.addPatterns(oldExtendedType.getPatterns());
        tc.addRanges(oldExtendedType.getRanges());

        final TypeConstraints constraints = findConstraintsFromTypeBuilder(newBaseType, tc, modules, module, null);
        final TypeDefinitionBuilderImpl newType = new TypeDefinitionBuilderImpl(oldExtendedType.getQName(), line);
        newType.setTypedef(newBaseType);
        newType.setPath(oldExtendedType.getPath());
        newType.setDescription(oldExtendedType.getDescription());
        newType.setReference(oldExtendedType.getReference());
        newType.setStatus(oldExtendedType.getStatus());
        newType.setLengths(constraints.getLength());
        newType.setPatterns(constraints.getPatterns());
        newType.setRanges(constraints.getRange());
        newType.setFractionDigits(constraints.getFractionDigits());
        newType.setUnits(oldExtendedType.getUnits());
        newType.setDefaultValue(oldExtendedType.getDefaultValue());
        newType.setUnknownNodes(oldExtendedType.getUnknownSchemaNodes());
        return newType;
    }

    /**
     * Create new type builder based on old type with new base type.
     *
     * @param newBaseType
     *            new base type
     * @param oldExtendedType
     *            old type
     * @param modules
     *            all loaded modules
     * @param module
     *            current module
     * @param line
     *            current line in module
     * @return new type builder based on old type with new base type
     */
    public static TypeDefinitionBuilder extendedTypeWithNewBaseType(final TypeDefinition<?> newBaseType,
            final ExtendedType oldExtendedType, final ModuleBuilder module, final int line) {
        final TypeConstraints tc = new TypeConstraints(module.getName(), line);

        final TypeConstraints constraints = findConstraintsFromTypeDefinition(newBaseType, tc);
        final TypeDefinitionBuilderImpl newType = new TypeDefinitionBuilderImpl(oldExtendedType.getQName(), line);
        newType.setType(newBaseType);
        newType.setPath(oldExtendedType.getPath());
        newType.setDescription(oldExtendedType.getDescription());
        newType.setReference(oldExtendedType.getReference());
        newType.setStatus(oldExtendedType.getStatus());
        newType.setLengths(constraints.getLength());
        newType.setPatterns(constraints.getPatterns());
        newType.setRanges(constraints.getRange());
        newType.setFractionDigits(constraints.getFractionDigits());
        newType.setUnits(oldExtendedType.getUnits());
        newType.setDefaultValue(oldExtendedType.getDefaultValue());
        newType.setUnknownNodes(oldExtendedType.getUnknownSchemaNodes());
        return newType;
    }

    /**
     * Pull restrictions from type and add them to constraints.
     *
     * @param typeToResolve
     *            type from which constraints will be read
     * @param constraints
     *            constraints object to which constraints will be added
     * @return constraints contstraints object containing constraints from given
     *         type
     */
    private static TypeConstraints findConstraintsFromTypeDefinition(final TypeDefinition<?> typeToResolve,
            final TypeConstraints constraints) {
        // union type cannot be restricted
        if (typeToResolve instanceof UnionTypeDefinition) {
            return constraints;
        }
        if (typeToResolve instanceof ExtendedType) {
            ExtendedType extType = (ExtendedType) typeToResolve;
            constraints.addFractionDigits(extType.getFractionDigits());
            constraints.addLengths(extType.getLengths());
            constraints.addPatterns(extType.getPatterns());
            constraints.addRanges(extType.getRanges());
            return findConstraintsFromTypeDefinition(extType.getBaseType(), constraints);
        } else {
            mergeConstraints(typeToResolve, constraints);
            return constraints;
        }
    }

    public static TypeConstraints findConstraintsFromTypeBuilder(final TypeAwareBuilder nodeToResolve,
            final TypeConstraints constraints, final Map<String, TreeMap<Date, ModuleBuilder>> modules,
            final ModuleBuilder builder, final SchemaContext context) {

        // union type cannot be restricted
        if (nodeToResolve instanceof UnionTypeBuilder) {
            return constraints;
        }

        if (nodeToResolve instanceof TypeDefinitionBuilder) {
            TypeDefinitionBuilder typedefToResolve = (TypeDefinitionBuilder) nodeToResolve;
            constraints.addFractionDigits(typedefToResolve.getFractionDigits());
            constraints.addLengths(typedefToResolve.getLengths());
            constraints.addPatterns(typedefToResolve.getPatterns());
            constraints.addRanges(typedefToResolve.getRanges());
        }

        TypeDefinition<?> type = nodeToResolve.getType();
        if (type == null) {
            return findConstraintsFromTypeBuilder(nodeToResolve.getTypedef(), constraints, modules, builder, context);
        } else {
            QName qname = type.getQName();
            if (type instanceof UnknownType) {
                ModuleBuilder dependentModuleBuilder = findDependentModuleBuilder(modules, builder, qname.getPrefix(),
                        nodeToResolve.getLine());
                if (dependentModuleBuilder == null) {
                    if (context == null) {
                        throw new YangParseException(builder.getName(), nodeToResolve.getLine(),
                                "Failed to resolved type constraints.");
                    }
                    Module dm = findModuleFromContext(context, builder, qname.getPrefix(), nodeToResolve.getLine());
                    TypeDefinition<?> t = findTypeByName(dm.getTypeDefinitions(), qname.getLocalName());
                    if (t instanceof ExtendedType) {
                        ExtendedType extType = (ExtendedType) t;
                        constraints.addFractionDigits(extType.getFractionDigits());
                        constraints.addLengths(extType.getLengths());
                        constraints.addPatterns(extType.getPatterns());
                        constraints.addRanges(extType.getRanges());
                        return constraints;
                    } else {
                        mergeConstraints(t, constraints);
                        return constraints;
                    }
                } else {
                    TypeDefinitionBuilder tdb = findTypeDefinitionBuilder(nodeToResolve, dependentModuleBuilder,
                            qname.getLocalName(), builder.getName(), nodeToResolve.getLine());
                    return findConstraintsFromTypeBuilder(tdb, constraints, modules, dependentModuleBuilder, context);
                }
            } else if (type instanceof ExtendedType) {
                ExtendedType extType = (ExtendedType) type;
                constraints.addFractionDigits(extType.getFractionDigits());
                constraints.addLengths(extType.getLengths());
                constraints.addPatterns(extType.getPatterns());
                constraints.addRanges(extType.getRanges());

                TypeDefinition<?> base = extType.getBaseType();
                if (base instanceof UnknownType) {
                    ModuleBuilder dependentModule = findDependentModuleBuilder(modules, builder, base.getQName()
                            .getPrefix(), nodeToResolve.getLine());
                    TypeDefinitionBuilder tdb = findTypeDefinitionBuilder(nodeToResolve, dependentModule, base
                            .getQName().getLocalName(), builder.getName(), nodeToResolve.getLine());
                    return findConstraintsFromTypeBuilder(tdb, constraints, modules, dependentModule, context);
                } else {
                    // it has to be base yang type
                    mergeConstraints(type, constraints);
                    return constraints;
                }
            } else {
                // it is base yang type
                mergeConstraints(type, constraints);
                return constraints;
            }
        }
    }

    /**
     * Search for type definition builder by name.
     *
     * @param dirtyNodeSchemaPath
     *            schema path of node which contains unresolved type
     * @param dependentModule
     *            module which should contains referenced type
     * @param typeName
     *            name of type definition
     * @param currentModuleName
     *            name of current module
     * @param line
     *            current line in yang model
     * @return
     */
    public static TypeDefinitionBuilder findTypeDefinitionBuilder(final TypeAwareBuilder nodeToResolve,
            final ModuleBuilder dependentModule, final String typeName, final String currentModuleName, final int line) {

        TypeDefinitionBuilder result = null;

        Set<TypeDefinitionBuilder> typedefs = dependentModule.getTypeDefinitionBuilders();
        result = findTypedefBuilderByName(typedefs, typeName);
        if (result != null) {
            return result;
        }

        Builder parent = nodeToResolve.getParent();
        while (parent != null) {
            if (parent instanceof DataNodeContainerBuilder) {
                typedefs = ((DataNodeContainerBuilder) parent).getTypeDefinitionBuilders();
            } else if (parent instanceof RpcDefinitionBuilder) {
                typedefs = ((RpcDefinitionBuilder) parent).getTypeDefinitions();
            }
            result = findTypedefBuilderByName(typedefs, typeName);
            if (result == null) {
                parent = parent.getParent();
            } else {
                break;
            }
        }

        if (result == null) {
            throw new YangParseException(currentModuleName, line, "Referenced type '" + typeName + "' not found.");
        }
        return result;
    }

}
