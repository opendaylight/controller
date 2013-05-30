/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.yang.parser.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeMap;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.opendaylight.controller.antlrv4.code.gen.YangLexer;
import org.opendaylight.controller.antlrv4.code.gen.YangParser;
import org.opendaylight.controller.yang.common.QName;
import org.opendaylight.controller.yang.model.api.Module;
import org.opendaylight.controller.yang.model.api.ModuleImport;
import org.opendaylight.controller.yang.model.api.SchemaContext;
import org.opendaylight.controller.yang.model.api.SchemaPath;
import org.opendaylight.controller.yang.model.api.TypeDefinition;
import org.opendaylight.controller.yang.model.api.type.BinaryTypeDefinition;
import org.opendaylight.controller.yang.model.api.type.DecimalTypeDefinition;
import org.opendaylight.controller.yang.model.api.type.IntegerTypeDefinition;
import org.opendaylight.controller.yang.model.api.type.LengthConstraint;
import org.opendaylight.controller.yang.model.api.type.PatternConstraint;
import org.opendaylight.controller.yang.model.api.type.RangeConstraint;
import org.opendaylight.controller.yang.model.api.type.StringTypeDefinition;
import org.opendaylight.controller.yang.model.parser.api.YangModelParser;
import org.opendaylight.controller.yang.model.util.ExtendedType;
import org.opendaylight.controller.yang.model.util.IdentityrefType;
import org.opendaylight.controller.yang.model.util.UnknownType;
import org.opendaylight.controller.yang.parser.builder.api.AugmentationSchemaBuilder;
import org.opendaylight.controller.yang.parser.builder.api.AugmentationTargetBuilder;
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
import org.opendaylight.controller.yang.parser.builder.impl.ContainerSchemaNodeBuilder;
import org.opendaylight.controller.yang.parser.builder.impl.IdentitySchemaNodeBuilder;
import org.opendaylight.controller.yang.parser.builder.impl.IdentityrefTypeBuilder;
import org.opendaylight.controller.yang.parser.builder.impl.LeafListSchemaNodeBuilder;
import org.opendaylight.controller.yang.parser.builder.impl.LeafSchemaNodeBuilder;
import org.opendaylight.controller.yang.parser.builder.impl.ListSchemaNodeBuilder;
import org.opendaylight.controller.yang.parser.builder.impl.ModuleBuilder;
import org.opendaylight.controller.yang.parser.builder.impl.RpcDefinitionBuilder;
import org.opendaylight.controller.yang.parser.builder.impl.TypeDefinitionBuilderImpl;
import org.opendaylight.controller.yang.parser.builder.impl.UnionTypeBuilder;
import org.opendaylight.controller.yang.parser.builder.impl.UnknownSchemaNodeBuilder;
import org.opendaylight.controller.yang.parser.util.ModuleDependencySort;
import org.opendaylight.controller.yang.parser.util.ParserUtils;
import org.opendaylight.controller.yang.parser.util.RefineHolder;
import org.opendaylight.controller.yang.parser.util.TypeConstraints;
import org.opendaylight.controller.yang.parser.util.YangParseException;
import org.opendaylight.controller.yang.validator.YangModelBasicValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public final class YangParserImpl implements YangModelParser {

    private static final Logger logger = LoggerFactory
            .getLogger(YangParserImpl.class);

    @Override
    public Map<File, Module> parseYangModelsMapped(List<File> yangFiles) {
        if (yangFiles != null) {
            final Map<InputStream, File> inputStreams = Maps.newHashMap();

            for (final File yangFile : yangFiles) {
                try {
                    inputStreams.put(new FileInputStream(yangFile), yangFile);
                } catch (FileNotFoundException e) {
                    logger.warn("Exception while reading yang file: "
                            + yangFile.getName(), e);
                }
            }

            Map<ModuleBuilder, InputStream> builderToStreamMap = Maps
                    .newHashMap();

            final Map<String, TreeMap<Date, ModuleBuilder>> modules = resolveModuleBuilders(
                    Lists.newArrayList(inputStreams.keySet()),
                    builderToStreamMap);
            // return new LinkedHashSet<Module>(build(modules).values());

            Map<File, Module> retVal = Maps.newLinkedHashMap();
            Map<ModuleBuilder, Module> builderToModuleMap = build(modules);

            for (Entry<ModuleBuilder, Module> builderToModule : builderToModuleMap
                    .entrySet()) {
                retVal.put(inputStreams.get(builderToStreamMap
                        .get(builderToModule.getKey())), builderToModule
                        .getValue());
            }

            return retVal;
        }
        return Collections.emptyMap();
    }

    @Override
    public Set<Module> parseYangModels(final List<File> yangFiles) {
        return Sets.newLinkedHashSet(parseYangModelsMapped(yangFiles).values());
    }

    @Override
    public Set<Module> parseYangModelsFromStreams(
            final List<InputStream> yangModelStreams) {
        return Sets.newHashSet(parseYangModelsFromStreamsMapped(
                yangModelStreams).values());
    }

    @Override
    public Map<InputStream, Module> parseYangModelsFromStreamsMapped(
            final List<InputStream> yangModelStreams) {
        Map<ModuleBuilder, InputStream> builderToStreamMap = Maps.newHashMap();

        final Map<String, TreeMap<Date, ModuleBuilder>> modules = resolveModuleBuilders(
                yangModelStreams, builderToStreamMap);
        Map<InputStream, Module> retVal = Maps.newLinkedHashMap();
        Map<ModuleBuilder, Module> builderToModuleMap = build(modules);

        for (Entry<ModuleBuilder, Module> builderToModule : builderToModuleMap
                .entrySet()) {
            retVal.put(builderToStreamMap.get(builderToModule.getKey()),
                    builderToModule.getValue());
        }
        return retVal;
    }

    @Override
    public SchemaContext resolveSchemaContext(final Set<Module> modules) {
        return new SchemaContextImpl(modules);
    }

    private ModuleBuilder[] parseModuleBuilders(List<InputStream> inputStreams,
            Map<ModuleBuilder, InputStream> streamToBuilderMap) {

        final ParseTreeWalker walker = new ParseTreeWalker();
        final List<ParseTree> trees = parseStreams(inputStreams);
        final ModuleBuilder[] builders = new ModuleBuilder[trees.size()];

        // validate yang
        new YangModelBasicValidator(walker).validate(trees);

        YangParserListenerImpl yangModelParser = null;
        for (int i = 0; i < trees.size(); i++) {
            yangModelParser = new YangParserListenerImpl();
            walker.walk(yangModelParser, trees.get(i));
            ModuleBuilder moduleBuilder = yangModelParser.getModuleBuilder();

            // We expect the order of trees and streams has to be the same
            streamToBuilderMap.put(moduleBuilder, inputStreams.get(i));
            builders[i] = moduleBuilder;
        }
        return builders;
    }

    private Map<String, TreeMap<Date, ModuleBuilder>> resolveModuleBuilders(
            final List<InputStream> yangFileStreams,
            Map<ModuleBuilder, InputStream> streamToBuilderMap) {

        final ModuleBuilder[] builders = parseModuleBuilders(yangFileStreams,
                streamToBuilderMap);

        // Linked Hash Map MUST be used because Linked Hash Map preserves ORDER
        // of items stored in map.
        final LinkedHashMap<String, TreeMap<Date, ModuleBuilder>> modules = new LinkedHashMap<String, TreeMap<Date, ModuleBuilder>>();

        // module dependency graph sorted
        List<ModuleBuilder> sorted = ModuleDependencySort.sort(builders);

        for (ModuleBuilder builder : sorted) {
            final String builderName = builder.getName();
            Date builderRevision = builder.getRevision();
            if (builderRevision == null) {
                builderRevision = new Date(0L);
            }
            TreeMap<Date, ModuleBuilder> builderByRevision = modules
                    .get(builderName);
            if (builderByRevision == null) {
                builderByRevision = new TreeMap<Date, ModuleBuilder>();
            }
            builderByRevision.put(builderRevision, builder);
            modules.put(builderName, builderByRevision);
        }
        return modules;
    }

    private List<ParseTree> parseStreams(final List<InputStream> yangStreams) {
        final List<ParseTree> trees = new ArrayList<ParseTree>();
        for (InputStream yangStream : yangStreams) {
            trees.add(parseStream(yangStream));
        }
        return trees;
    }

    private ParseTree parseStream(final InputStream yangStream) {
        ParseTree result = null;
        try {
            final ANTLRInputStream input = new ANTLRInputStream(yangStream);
            final YangLexer lexer = new YangLexer(input);
            final CommonTokenStream tokens = new CommonTokenStream(lexer);
            final YangParser parser = new YangParser(tokens);
            result = parser.yang();
        } catch (IOException e) {
            logger.warn("Exception while reading yang file: " + yangStream, e);
        }
        return result;
    }

    private Map<ModuleBuilder, Module> build(
            final Map<String, TreeMap<Date, ModuleBuilder>> modules) {
        // fix unresolved nodes
        for (Map.Entry<String, TreeMap<Date, ModuleBuilder>> entry : modules
                .entrySet()) {
            for (Map.Entry<Date, ModuleBuilder> childEntry : entry.getValue()
                    .entrySet()) {
                final ModuleBuilder moduleBuilder = childEntry.getValue();
                fixUnresolvedNodes(modules, moduleBuilder);
            }
        }
        resolveAugments(modules);

        // build
        // LinkedHashMap MUST be used otherwise the values will not maintain
        // order!
        // http://docs.oracle.com/javase/6/docs/api/java/util/LinkedHashMap.html
        final Map<ModuleBuilder, Module> result = new LinkedHashMap<ModuleBuilder, Module>();
        for (Map.Entry<String, TreeMap<Date, ModuleBuilder>> entry : modules
                .entrySet()) {
            final Map<Date, Module> modulesByRevision = new HashMap<Date, Module>();
            for (Map.Entry<Date, ModuleBuilder> childEntry : entry.getValue()
                    .entrySet()) {
                final ModuleBuilder moduleBuilder = childEntry.getValue();
                final Module module = moduleBuilder.build();
                modulesByRevision.put(childEntry.getKey(), module);
                result.put(moduleBuilder, module);
            }
        }
        return result;
    }

    private void fixUnresolvedNodes(
            final Map<String, TreeMap<Date, ModuleBuilder>> modules,
            final ModuleBuilder builder) {
        resolveDirtyNodes(modules, builder);
        resolveIdentities(modules, builder);
        resolveUsesRefines(modules, builder);
        resolveUnknownNodes(modules, builder);
    }

    /**
     * Search for dirty nodes (node which contains UnknownType) and resolve
     * unknown types.
     *
     * @param modules
     *            all available modules
     * @param module
     *            current module
     */
    private void resolveDirtyNodes(
            final Map<String, TreeMap<Date, ModuleBuilder>> modules,
            final ModuleBuilder module) {
        final Map<List<String>, TypeAwareBuilder> dirtyNodes = module
                .getDirtyNodes();
        if (!dirtyNodes.isEmpty()) {
            for (Map.Entry<List<String>, TypeAwareBuilder> entry : dirtyNodes
                    .entrySet()) {

                final TypeAwareBuilder nodeToResolve = entry.getValue();
                // different handling for union types
                if (nodeToResolve instanceof UnionTypeBuilder) {
                    final UnionTypeBuilder union = (UnionTypeBuilder) nodeToResolve;
                    final List<TypeDefinition<?>> unionTypes = union.getTypes();
                    final List<UnknownType> toRemove = new ArrayList<UnknownType>();
                    for (TypeDefinition<?> td : unionTypes) {
                        if (td instanceof UnknownType) {
                            final UnknownType unknownType = (UnknownType) td;
                            final TypeDefinitionBuilder resolvedType = resolveTypeUnion(
                                    nodeToResolve, unknownType, modules, module);
                            union.setTypedef(resolvedType);
                            toRemove.add(unknownType);
                        }
                    }
                    unionTypes.removeAll(toRemove);
                } else if (nodeToResolve.getTypedef() instanceof IdentityrefTypeBuilder) {
                    // different handling for identityref types
                    IdentityrefTypeBuilder idref = (IdentityrefTypeBuilder) nodeToResolve
                            .getTypedef();
                    nodeToResolve.setType(new IdentityrefType(findFullQName(
                            modules, module, idref), idref.getPath()));
                } else {
                    final TypeDefinitionBuilder resolvedType = resolveType(
                            nodeToResolve, modules, module);
                    nodeToResolve.setTypedef(resolvedType);
                }
            }
        }
    }

    private TypeDefinitionBuilder resolveType(
            final TypeAwareBuilder nodeToResolve,
            final Map<String, TreeMap<Date, ModuleBuilder>> modules,
            final ModuleBuilder builder) {
        final TypeConstraints constraints = new TypeConstraints();

        final TypeDefinitionBuilder targetTypeBuilder = getTypeDefinitionBuilderFromDirtyNode(
                nodeToResolve, modules, builder);
        final TypeConstraints tConstraints = findConstraints(nodeToResolve,
                constraints, modules, builder);
        targetTypeBuilder.setRanges(tConstraints.getRange());
        targetTypeBuilder.setLengths(tConstraints.getLength());
        targetTypeBuilder.setPatterns(tConstraints.getPatterns());
        targetTypeBuilder.setFractionDigits(tConstraints.getFractionDigits());

        return targetTypeBuilder;
    }

    private TypeDefinitionBuilder resolveTypeUnion(
            final TypeAwareBuilder typeToResolve,
            final UnknownType unknownType,
            final Map<String, TreeMap<Date, ModuleBuilder>> modules,
            final ModuleBuilder builder) {
        final TypeConstraints constraints = new TypeConstraints();

        final TypeDefinitionBuilder targetTypeBuilder = getUnionBuilder(
                typeToResolve, unknownType, modules, builder);
        final TypeConstraints tConstraints = findConstraints(typeToResolve,
                constraints, modules, builder);
        targetTypeBuilder.setRanges(tConstraints.getRange());
        targetTypeBuilder.setLengths(tConstraints.getLength());
        targetTypeBuilder.setPatterns(tConstraints.getPatterns());
        targetTypeBuilder.setFractionDigits(tConstraints.getFractionDigits());

        return targetTypeBuilder;
    }

    private TypeDefinitionBuilder getTypeDefinitionBuilderFromDirtyNode(
            final TypeAwareBuilder nodeToResolve,
            final Map<String, TreeMap<Date, ModuleBuilder>> modules,
            final ModuleBuilder module) {

        final UnknownType unknownType = (UnknownType) nodeToResolve.getType();
        final QName unknownTypeQName = unknownType.getQName();

        // search for module which contains referenced typedef
        final ModuleBuilder dependentModule = findDependentModule(modules,
                module, unknownTypeQName.getPrefix(), nodeToResolve.getLine());

        final TypeDefinitionBuilder lookedUpBuilder = findTypeDefinitionBuilder(
                nodeToResolve.getPath(), dependentModule,
                unknownTypeQName.getLocalName(), module.getName(),
                nodeToResolve.getLine());

        final TypeDefinitionBuilder lookedUpBuilderCopy = copyTypedefBuilder(
                lookedUpBuilder, nodeToResolve instanceof TypeDefinitionBuilder);
        final TypeDefinitionBuilder resolvedCopy = resolveCopiedBuilder(
                lookedUpBuilderCopy, modules, dependentModule);
        return resolvedCopy;
    }

    private TypeDefinitionBuilder getUnionBuilder(
            final TypeAwareBuilder nodeToResolve,
            final UnknownType unknownType,
            final Map<String, TreeMap<Date, ModuleBuilder>> modules,
            final ModuleBuilder module) {

        final TypeDefinition<?> baseTypeToResolve = nodeToResolve.getType();
        if (baseTypeToResolve != null
                && !(baseTypeToResolve instanceof UnknownType)) {
            return (TypeDefinitionBuilder) nodeToResolve;
        }

        final QName unknownTypeQName = unknownType.getQName();
        // search for module which contains referenced typedef
        final ModuleBuilder dependentModule = findDependentModule(modules,
                module, unknownTypeQName.getPrefix(), nodeToResolve.getLine());
        final TypeDefinitionBuilder lookedUpBuilder = findTypeDefinitionBuilder(
                nodeToResolve.getPath(), dependentModule,
                unknownTypeQName.getLocalName(), module.getName(),
                nodeToResolve.getLine());

        final TypeDefinitionBuilder lookedUpBuilderCopy = copyTypedefBuilder(
                lookedUpBuilder, nodeToResolve instanceof TypeDefinitionBuilder);
        final TypeDefinitionBuilder resolvedCopy = resolveCopiedBuilder(
                lookedUpBuilderCopy, modules, dependentModule);
        return resolvedCopy;
    }

    private TypeDefinitionBuilder copyTypedefBuilder(
            final TypeDefinitionBuilder old, final boolean seekByTypedefBuilder) {
        if (old instanceof UnionTypeBuilder) {
            final UnionTypeBuilder oldUnion = (UnionTypeBuilder) old;
            final UnionTypeBuilder newUnion = new UnionTypeBuilder(
                    old.getLine());
            for (TypeDefinition<?> td : oldUnion.getTypes()) {
                newUnion.setType(td);
            }
            for (TypeDefinitionBuilder tdb : oldUnion.getTypedefs()) {
                newUnion.setTypedef(copyTypedefBuilder(tdb, true));
            }
            newUnion.setPath(old.getPath());
            return newUnion;
        }

        final QName oldName = old.getQName();
        final QName newName = new QName(oldName.getNamespace(),
                oldName.getRevision(), oldName.getPrefix(),
                oldName.getLocalName());
        final TypeDefinitionBuilder tdb = new TypeDefinitionBuilderImpl(
                newName, old.getLine());

        tdb.setRanges(old.getRanges());
        tdb.setLengths(old.getLengths());
        tdb.setPatterns(old.getPatterns());
        tdb.setFractionDigits(old.getFractionDigits());
        tdb.setPath(old.getPath());

        final TypeDefinition<?> oldType = old.getType();
        if (oldType == null) {
            tdb.setTypedef(old.getTypedef());
        } else {
            tdb.setType(oldType);
        }

        if (!seekByTypedefBuilder) {
            tdb.setDescription(old.getDescription());
            tdb.setReference(old.getReference());
            tdb.setStatus(old.getStatus());
            tdb.setDefaultValue(old.getDefaultValue());
            tdb.setUnits(old.getUnits());
        }
        return tdb;
    }

    private TypeDefinitionBuilder resolveCopiedBuilder(
            final TypeDefinitionBuilder copy,
            final Map<String, TreeMap<Date, ModuleBuilder>> modules,
            final ModuleBuilder builder) {

        if (copy instanceof UnionTypeBuilder) {
            final UnionTypeBuilder union = (UnionTypeBuilder) copy;
            final List<TypeDefinition<?>> unionTypes = union.getTypes();
            final List<UnknownType> toRemove = new ArrayList<UnknownType>();
            for (TypeDefinition<?> td : unionTypes) {
                if (td instanceof UnknownType) {
                    final UnknownType unknownType = (UnknownType) td;
                    final TypeDefinitionBuilder resolvedType = resolveTypeUnion(
                            union, unknownType, modules, builder);
                    union.setTypedef(resolvedType);
                    toRemove.add(unknownType);
                }
            }
            unionTypes.removeAll(toRemove);

            return union;
        }

        final TypeDefinition<?> base = copy.getType();
        final TypeDefinitionBuilder baseTdb = copy.getTypedef();
        if (base != null && !(base instanceof UnknownType)) {
            return copy;
        } else if (base instanceof UnknownType) {
            final UnknownType unknownType = (UnknownType) base;
            final QName unknownTypeQName = unknownType.getQName();
            final String unknownTypePrefix = unknownTypeQName.getPrefix();
            final ModuleBuilder dependentModule = findDependentModule(modules,
                    builder, unknownTypePrefix, copy.getLine());
            final TypeDefinitionBuilder utBuilder = getTypeDefinitionBuilderFromDirtyNode(
                    copy, modules, dependentModule);
            copy.setTypedef(utBuilder);
            return copy;
        } else if (base == null && baseTdb != null) {
            // make a copy of baseTypeDef and call again
            final TypeDefinitionBuilder baseTdbCopy = copyTypedefBuilder(
                    baseTdb, true);
            final TypeDefinitionBuilder baseTdbCopyResolved = resolveCopiedBuilder(
                    baseTdbCopy, modules, builder);
            copy.setTypedef(baseTdbCopyResolved);
            return copy;
        } else {
            throw new YangParseException(copy.getLine(),
                    "Failed to resolve type " + copy.getQName().getLocalName());
        }
    }

    private TypeConstraints findConstraints(
            final TypeAwareBuilder nodeToResolve,
            final TypeConstraints constraints,
            final Map<String, TreeMap<Date, ModuleBuilder>> modules,
            final ModuleBuilder builder) {
        // union type cannot be restricted
        if (nodeToResolve instanceof UnionTypeBuilder) {
            return constraints;
        }

        // if referenced type is UnknownType again, search recursively with
        // current constraints
        final TypeDefinition<?> referencedType = nodeToResolve.getType();
        List<RangeConstraint> ranges = Collections.emptyList();
        List<LengthConstraint> lengths = Collections.emptyList();
        List<PatternConstraint> patterns = Collections.emptyList();
        Integer fractionDigits = null;
        if (referencedType == null) {
            final TypeDefinitionBuilder tdb = nodeToResolve.getTypedef();
            ranges = tdb.getRanges();
            constraints.addRanges(ranges);
            lengths = tdb.getLengths();
            constraints.addLengths(lengths);
            patterns = tdb.getPatterns();
            constraints.addPatterns(patterns);
            fractionDigits = tdb.getFractionDigits();
            constraints.setFractionDigits(fractionDigits);
            return constraints;
        } else if (referencedType instanceof ExtendedType) {
            final ExtendedType ext = (ExtendedType) referencedType;
            ranges = ext.getRanges();
            constraints.addRanges(ranges);
            lengths = ext.getLengths();
            constraints.addLengths(lengths);
            patterns = ext.getPatterns();
            constraints.addPatterns(patterns);
            fractionDigits = ext.getFractionDigits();
            constraints.setFractionDigits(fractionDigits);
            return findConstraints(
                    findTypeDefinitionBuilder(nodeToResolve.getPath(), builder,
                            ext.getQName().getLocalName(), builder.getName(),
                            nodeToResolve.getLine()), constraints, modules,
                    builder);
        } else if (referencedType instanceof UnknownType) {
            final UnknownType unknown = (UnknownType) referencedType;
            ranges = unknown.getRangeStatements();
            constraints.addRanges(ranges);
            lengths = unknown.getLengthStatements();
            constraints.addLengths(lengths);
            patterns = unknown.getPatterns();
            constraints.addPatterns(patterns);
            fractionDigits = unknown.getFractionDigits();
            constraints.setFractionDigits(fractionDigits);

            String unknownTypePrefix = unknown.getQName().getPrefix();
            if (unknownTypePrefix == null || "".equals(unknownTypePrefix)) {
                unknownTypePrefix = builder.getPrefix();
            }
            final ModuleBuilder dependentModule = findDependentModule(modules,
                    builder, unknown.getQName().getPrefix(),
                    nodeToResolve.getLine());
            final TypeDefinitionBuilder utBuilder = findTypeDefinitionBuilder(
                    nodeToResolve.getPath(), dependentModule, unknown
                            .getQName().getLocalName(), builder.getName(),
                    nodeToResolve.getLine());
            return findConstraints(utBuilder, constraints, modules,
                    dependentModule);
        } else {
            // HANDLE BASE YANG TYPE
            mergeConstraints(referencedType, constraints);
            return constraints;
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
    private TypeDefinitionBuilder findTypeDefinitionBuilder(
            SchemaPath dirtyNodeSchemaPath,
            final ModuleBuilder dependentModule, final String typeName,
            final String currentModuleName, final int line) {
        final List<QName> path = dirtyNodeSchemaPath.getPath();
        TypeDefinitionBuilder result = null;

        Set<TypeDefinitionBuilder> typedefs = dependentModule
                .getModuleTypedefs();
        result = findTdb(typedefs, typeName);

        if (result == null) {
            Builder currentNode = null;
            final List<String> currentPath = new ArrayList<String>();
            currentPath.add(dependentModule.getName());

            for (int i = 0; i < path.size(); i++) {
                QName qname = path.get(i);
                currentPath.add(qname.getLocalName());
                currentNode = dependentModule.getModuleNode(currentPath);

                if (currentNode instanceof RpcDefinitionBuilder) {
                    typedefs = ((RpcDefinitionBuilder) currentNode)
                            .getTypeDefinitions();
                } else if (currentNode instanceof DataNodeContainerBuilder) {
                    typedefs = ((DataNodeContainerBuilder) currentNode)
                            .getTypeDefinitions();
                } else {
                    typedefs = Collections.emptySet();
                }

                result = findTdb(typedefs, typeName);
                if (result != null) {
                    break;
                }
            }
        }

        if (result != null) {
            return result;
        }
        throw new YangParseException(currentModuleName, line,
                "Referenced type '" + typeName + "' not found.");
    }

    private TypeDefinitionBuilder findTdb(Set<TypeDefinitionBuilder> types,
            String name) {
        for (TypeDefinitionBuilder td : types) {
            if (td.getQName().getLocalName().equals(name)) {
                return td;
            }
        }
        return null;
    }

    /**
     * Pull restriction from referenced type and add them to given constraints
     *
     * @param referencedType
     * @param constraints
     */
    private void mergeConstraints(final TypeDefinition<?> referencedType,
            final TypeConstraints constraints) {

        if (referencedType instanceof DecimalTypeDefinition) {
            constraints.addRanges(((DecimalTypeDefinition) referencedType)
                    .getRangeStatements());
            constraints
                    .setFractionDigits(((DecimalTypeDefinition) referencedType)
                            .getFractionDigits());
        } else if (referencedType instanceof IntegerTypeDefinition) {
            constraints.addRanges(((IntegerTypeDefinition) referencedType)
                    .getRangeStatements());
        } else if (referencedType instanceof StringTypeDefinition) {
            constraints.addPatterns(((StringTypeDefinition) referencedType)
                    .getPatterns());
            constraints.addLengths(((StringTypeDefinition) referencedType)
                    .getLengthStatements());
        } else if (referencedType instanceof BinaryTypeDefinition) {
            constraints.addLengths(((BinaryTypeDefinition) referencedType)
                    .getLengthConstraints());
        }
    }

    /**
     * Go through all augment definitions and resolve them. This method also
     * finds augment target node and add child nodes to it.
     *
     * @param modules
     *            all available modules
     */
    private void resolveAugments(
            final Map<String, TreeMap<Date, ModuleBuilder>> modules) {
        final List<ModuleBuilder> allModulesList = new ArrayList<ModuleBuilder>();
        final Set<ModuleBuilder> allModulesSet = new HashSet<ModuleBuilder>();
        for (Map.Entry<String, TreeMap<Date, ModuleBuilder>> entry : modules
                .entrySet()) {
            for (Map.Entry<Date, ModuleBuilder> inner : entry.getValue()
                    .entrySet()) {
                allModulesList.add(inner.getValue());
                allModulesSet.add(inner.getValue());
            }
        }

        for (int i = 0; i < allModulesList.size(); i++) {
            final ModuleBuilder module = allModulesList.get(i);
            // try to resolve augments in module
            resolveAugment(modules, module);
            // while all augments are not resolved
            final Iterator<ModuleBuilder> allModulesIterator = allModulesSet
                    .iterator();
            while (!(module.getAugmentsResolved() == module.getAugments()
                    .size())) {
                ModuleBuilder nextModule = null;
                // try resolve other module augments
                try {
                    nextModule = allModulesIterator.next();
                    resolveAugment(modules, nextModule);
                } catch (NoSuchElementException e) {
                    throw new YangParseException(
                            "Failed to resolve augments in module '"
                                    + module.getName() + "'.", e);
                }
                // then try to resolve first module again
                resolveAugment(modules, module);
            }
        }
    }

    /**
     *
     * @param modules
     *            all available modules
     * @param module
     *            current module
     */
    private void resolveAugment(
            final Map<String, TreeMap<Date, ModuleBuilder>> modules,
            final ModuleBuilder module) {
        if (module.getAugmentsResolved() < module.getAugments().size()) {
            for (AugmentationSchemaBuilder augmentBuilder : module
                    .getAugments()) {

                if (!augmentBuilder.isResolved()) {
                    final SchemaPath augmentTargetSchemaPath = augmentBuilder
                            .getTargetPath();
                    final List<QName> path = augmentTargetSchemaPath.getPath();

                    final QName qname = path.get(0);
                    String prefix = qname.getPrefix();
                    if (prefix == null) {
                        prefix = module.getPrefix();
                    }

                    DataSchemaNodeBuilder currentParent = null;
                    final ModuleBuilder dependentModule = findDependentModule(
                            modules, module, prefix, augmentBuilder.getLine());
                    for (DataSchemaNodeBuilder child : dependentModule
                            .getChildNodes()) {
                        final QName childQName = child.getQName();
                        if (childQName.getLocalName().equals(
                                qname.getLocalName())) {
                            currentParent = child;
                            break;
                        }
                    }

                    if (currentParent == null) {
                        continue;
                    }

                    for (int i = 1; i < path.size(); i++) {
                        final QName currentQName = path.get(i);
                        DataSchemaNodeBuilder newParent = null;
                        for (DataSchemaNodeBuilder child : ((DataNodeContainerBuilder) currentParent)
                                .getChildNodes()) {
                            final QName childQName = child.getQName();
                            if (childQName.getLocalName().equals(
                                    currentQName.getLocalName())) {
                                newParent = child;
                                break;
                            }
                        }
                        if (newParent == null) {
                            break; // node not found, quit search
                        } else {
                            currentParent = newParent;
                        }
                    }

                    final QName currentQName = currentParent.getQName();
                    final QName lastAugmentPathElement = path
                            .get(path.size() - 1);
                    if (currentQName.getLocalName().equals(
                            lastAugmentPathElement.getLocalName())) {

                        if (currentParent instanceof ChoiceBuilder) {
                            ParserUtils.fillAugmentTarget(augmentBuilder,
                                    (ChoiceBuilder) currentParent);
                        } else {
                            ParserUtils.fillAugmentTarget(augmentBuilder,
                                    (DataNodeContainerBuilder) currentParent);
                        }
                        ((AugmentationTargetBuilder) currentParent)
                                .addAugmentation(augmentBuilder);
                        SchemaPath oldPath = currentParent.getPath();
                        augmentBuilder.setTargetPath(new SchemaPath(oldPath
                                .getPath(), oldPath.isAbsolute()));
                        augmentBuilder.setResolved(true);
                        module.augmentResolved();
                    }
                }

            }
        }
    }

    /**
     * Go through identity statements defined in current module and resolve
     * their 'base' statement if present.
     *
     * @param modules
     *            all modules
     * @param module
     *            module being resolved
     */
    private void resolveIdentities(
            final Map<String, TreeMap<Date, ModuleBuilder>> modules,
            final ModuleBuilder module) {
        final Set<IdentitySchemaNodeBuilder> identities = module
                .getIdentities();
        for (IdentitySchemaNodeBuilder identity : identities) {
            final String baseIdentityName = identity.getBaseIdentityName();
            if (baseIdentityName != null) {
                String baseIdentityPrefix = null;
                String baseIdentityLocalName = null;
                if (baseIdentityName.contains(":")) {
                    final String[] splitted = baseIdentityName.split(":");
                    baseIdentityPrefix = splitted[0];
                    baseIdentityLocalName = splitted[1];
                } else {
                    baseIdentityPrefix = module.getPrefix();
                    baseIdentityLocalName = baseIdentityName;
                }
                final ModuleBuilder dependentModule = findDependentModule(
                        modules, module, baseIdentityPrefix, identity.getLine());

                final Set<IdentitySchemaNodeBuilder> dependentModuleIdentities = dependentModule
                        .getIdentities();
                for (IdentitySchemaNodeBuilder idBuilder : dependentModuleIdentities) {
                    if (idBuilder.getQName().getLocalName()
                            .equals(baseIdentityLocalName)) {
                        identity.setBaseIdentity(idBuilder);
                    }
                }
            }
        }
    }

    /**
     * Go through uses statements defined in current module and resolve their
     * refine statements.
     *
     * @param modules
     *            all modules
     * @param module
     *            module being resolved
     */
    private void resolveUsesRefines(
            final Map<String, TreeMap<Date, ModuleBuilder>> modules,
            final ModuleBuilder module) {
        final Map<List<String>, UsesNodeBuilder> moduleUses = module
                .getUsesNodes();
        for (Map.Entry<List<String>, UsesNodeBuilder> entry : moduleUses
                .entrySet()) {
            final List<String> key = entry.getKey();
            final UsesNodeBuilder usesNode = entry.getValue();
            final int line = usesNode.getLine();

            final String groupingName = key.get(key.size() - 1);

            for (RefineHolder refine : usesNode.getRefines()) {
                SchemaNodeBuilder refineTarget = getRefineNodeBuilderCopy(
                        groupingName, refine, modules, module);
                ParserUtils.checkRefine(refineTarget, refine);
                ParserUtils.refineDefault(refineTarget, refine, line);
                if (refineTarget instanceof LeafSchemaNodeBuilder) {
                    final LeafSchemaNodeBuilder leaf = (LeafSchemaNodeBuilder) refineTarget;
                    ParserUtils.refineLeaf(leaf, refine, line);
                    usesNode.addRefineNode(leaf);
                } else if (refineTarget instanceof ContainerSchemaNodeBuilder) {
                    final ContainerSchemaNodeBuilder container = (ContainerSchemaNodeBuilder) refineTarget;
                    ParserUtils.refineContainer(container, refine, line);
                    usesNode.addRefineNode(container);
                } else if (refineTarget instanceof ListSchemaNodeBuilder) {
                    final ListSchemaNodeBuilder list = (ListSchemaNodeBuilder) refineTarget;
                    ParserUtils.refineList(list, refine, line);
                    usesNode.addRefineNode(list);
                } else if (refineTarget instanceof LeafListSchemaNodeBuilder) {
                    final LeafListSchemaNodeBuilder leafList = (LeafListSchemaNodeBuilder) refineTarget;
                    ParserUtils.refineLeafList(leafList, refine, line);
                    usesNode.addRefineNode(leafList);
                } else if (refineTarget instanceof ChoiceBuilder) {
                    final ChoiceBuilder choice = (ChoiceBuilder) refineTarget;
                    ParserUtils.refineChoice(choice, refine, line);
                    usesNode.addRefineNode(choice);
                } else if (refineTarget instanceof AnyXmlBuilder) {
                    final AnyXmlBuilder anyXml = (AnyXmlBuilder) refineTarget;
                    ParserUtils.refineAnyxml(anyXml, refine, line);
                    usesNode.addRefineNode(anyXml);
                } else if (refineTarget instanceof GroupingBuilder) {
                    usesNode.addRefineNode(refineTarget);
                } else if (refineTarget instanceof TypeDefinitionBuilder) {
                    usesNode.addRefineNode(refineTarget);
                }
            }
        }
    }

    /**
     * Find original builder of node to refine and return copy of this builder.
     * <p>
     * We must create and use a copy of builder to preserve original builder
     * state, because this object will be refined (modified) and later added to
     * {@link UsesNodeBuilder}.
     * </p>
     *
     * @param groupingPath
     *            path to grouping which contains node to refine
     * @param refine
     *            refine object containing informations about refine
     * @param modules
     *            all loaded modules
     * @param module
     *            current module
     * @return copy of node to be refined if it is present in grouping, null
     *         otherwise
     */
    private SchemaNodeBuilder getRefineNodeBuilderCopy(
            final String groupingPath, final RefineHolder refine,
            final Map<String, TreeMap<Date, ModuleBuilder>> modules,
            final ModuleBuilder module) {
        Builder result = null;
        final Builder lookedUpBuilder = findRefineTargetBuilder(groupingPath,
                refine, modules, module);
        if (lookedUpBuilder instanceof LeafSchemaNodeBuilder) {
            result = ParserUtils
                    .copyLeafBuilder((LeafSchemaNodeBuilder) lookedUpBuilder);
        } else if (lookedUpBuilder instanceof ContainerSchemaNodeBuilder) {
            result = ParserUtils
                    .copyContainerBuilder((ContainerSchemaNodeBuilder) lookedUpBuilder);
        } else if (lookedUpBuilder instanceof ListSchemaNodeBuilder) {
            result = ParserUtils
                    .copyListBuilder((ListSchemaNodeBuilder) lookedUpBuilder);
        } else if (lookedUpBuilder instanceof LeafListSchemaNodeBuilder) {
            result = ParserUtils
                    .copyLeafListBuilder((LeafListSchemaNodeBuilder) lookedUpBuilder);
        } else if (lookedUpBuilder instanceof ChoiceBuilder) {
            result = ParserUtils
                    .copyChoiceBuilder((ChoiceBuilder) lookedUpBuilder);
        } else if (lookedUpBuilder instanceof AnyXmlBuilder) {
            result = ParserUtils
                    .copyAnyXmlBuilder((AnyXmlBuilder) lookedUpBuilder);
        } else if (lookedUpBuilder instanceof GroupingBuilder) {
            result = ParserUtils
                    .copyGroupingBuilder((GroupingBuilder) lookedUpBuilder);
        } else if (lookedUpBuilder instanceof TypeDefinitionBuilder) {
            result = ParserUtils
                    .copyTypedefBuilder((TypeDefinitionBuilderImpl) lookedUpBuilder);
        } else {
            throw new YangParseException(module.getName(), refine.getLine(),
                    "Target '" + refine.getName() + "' can not be refined");
        }
        return (SchemaNodeBuilder) result;
    }

    /**
     * Find builder of refine node.
     *
     * @param groupingPath
     *            path to grouping which contains node to refine
     * @param refine
     *            object containing refine information
     * @param modules
     *            all loaded modules
     * @param module
     *            current module
     * @return Builder object of refine node if it is present in grouping, null
     *         otherwise
     */
    private Builder findRefineTargetBuilder(final String groupingPath,
            final RefineHolder refine,
            final Map<String, TreeMap<Date, ModuleBuilder>> modules,
            final ModuleBuilder module) {
        final String refineNodeName = refine.getName();
        final SchemaPath path = ParserUtils.parseUsesPath(groupingPath);
        final List<String> builderPath = new ArrayList<String>();
        String prefix = null;
        for (QName qname : path.getPath()) {
            builderPath.add(qname.getLocalName());
            prefix = qname.getPrefix();
        }
        if (prefix == null) {
            prefix = module.getPrefix();
        }

        final ModuleBuilder dependentModule = findDependentModule(modules,
                module, prefix, refine.getLine());
        builderPath.add(0, dependentModule.getName());
        final GroupingBuilder builder = dependentModule
                .getGrouping(builderPath);

        Builder result = builder.getChildNode(refineNodeName);
        if (result == null) {
            Set<GroupingBuilder> grps = builder.getGroupings();
            for (GroupingBuilder gr : grps) {
                if (gr.getQName().getLocalName().equals(refineNodeName)) {
                    result = gr;
                    break;
                }
            }
        }
        if (result == null) {
            Set<TypeDefinitionBuilder> typedefs = builder.getTypeDefinitions();
            for (TypeDefinitionBuilder typedef : typedefs) {
                if (typedef.getQName().getLocalName().equals(refineNodeName)) {
                    result = typedef;
                    break;
                }
            }
        }
        return result;
    }

    private QName findFullQName(
            final Map<String, TreeMap<Date, ModuleBuilder>> modules,
            final ModuleBuilder module, final IdentityrefTypeBuilder idref) {
        QName result = null;
        String baseString = idref.getBaseString();
        if (baseString.contains(":")) {
            String[] splittedBase = baseString.split(":");
            if (splittedBase.length > 2) {
                throw new YangParseException(module.getName(), idref.getLine(),
                        "Failed to parse identityref base: " + baseString);
            }
            String prefix = splittedBase[0];
            String name = splittedBase[1];
            ModuleBuilder dependentModule = findDependentModule(modules,
                    module, prefix, idref.getLine());
            result = new QName(dependentModule.getNamespace(),
                    dependentModule.getRevision(), prefix, name);
        } else {
            result = new QName(module.getNamespace(), module.getRevision(),
                    module.getPrefix(), baseString);
        }
        return result;
    }

    private void resolveUnknownNodes(
            final Map<String, TreeMap<Date, ModuleBuilder>> modules,
            final ModuleBuilder module) {
        for (UnknownSchemaNodeBuilder usnb : module.getUnknownNodes()) {
            QName nodeType = usnb.getNodeType();
            if (nodeType.getNamespace() == null
                    || nodeType.getRevision() == null) {
                try {
                    ModuleBuilder dependentModule = findDependentModule(
                            modules, module, nodeType.getPrefix(),
                            usnb.getLine());
                    QName newNodeType = new QName(
                            dependentModule.getNamespace(),
                            dependentModule.getRevision(),
                            nodeType.getPrefix(), nodeType.getLocalName());
                    usnb.setNodeType(newNodeType);
                } catch (YangParseException e) {
                    logger.debug(module.getName(), usnb.getLine(),
                            "Failed to find unknown node type: " + nodeType);
                }
            }
        }
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
    private ModuleBuilder findDependentModule(
            final Map<String, TreeMap<Date, ModuleBuilder>> modules,
            final ModuleBuilder module, final String prefix, final int line) {
        ModuleBuilder dependentModule = null;
        Date dependentModuleRevision = null;

        if (prefix.equals(module.getPrefix())) {
            dependentModule = module;
        } else {
            final ModuleImport dependentModuleImport = ParserUtils
                    .getModuleImport(module, prefix);
            if (dependentModuleImport == null) {
                throw new YangParseException(module.getName(), line,
                        "No import found with prefix '" + prefix + "'.");
            }
            final String dependentModuleName = dependentModuleImport
                    .getModuleName();
            dependentModuleRevision = dependentModuleImport.getRevision();

            final TreeMap<Date, ModuleBuilder> moduleBuildersByRevision = modules
                    .get(dependentModuleName);
            if (moduleBuildersByRevision == null) {
                throw new YangParseException(module.getName(), line,
                        "Failed to find dependent module '"
                                + dependentModuleName + "'.");
            }
            if (dependentModuleRevision == null) {
                dependentModule = moduleBuildersByRevision.lastEntry()
                        .getValue();
            } else {
                dependentModule = moduleBuildersByRevision
                        .get(dependentModuleRevision);
            }
        }

        if (dependentModule == null) {
            throw new YangParseException(module.getName(), line,
                    "Failed to find dependent module with prefix '" + prefix
                            + "' and revision '" + dependentModuleRevision
                            + "'.");
        }
        return dependentModule;
    }

}
