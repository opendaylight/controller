/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.yang.parser.impl;

import static org.opendaylight.controller.yang.parser.util.ParserUtils.*;

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
import java.util.LinkedHashSet;
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
import org.opendaylight.controller.yang.model.api.GroupingDefinition;
import org.opendaylight.controller.yang.model.api.IdentitySchemaNode;
import org.opendaylight.controller.yang.model.api.Module;
import org.opendaylight.controller.yang.model.api.SchemaContext;
import org.opendaylight.controller.yang.model.api.SchemaPath;
import org.opendaylight.controller.yang.model.api.TypeDefinition;
import org.opendaylight.controller.yang.model.parser.api.YangModelParser;
import org.opendaylight.controller.yang.model.util.ExtendedType;
import org.opendaylight.controller.yang.model.util.IdentityrefType;
import org.opendaylight.controller.yang.model.util.UnknownType;
import org.opendaylight.controller.yang.parser.builder.api.AugmentationSchemaBuilder;
import org.opendaylight.controller.yang.parser.builder.api.Builder;
import org.opendaylight.controller.yang.parser.builder.api.DataNodeContainerBuilder;
import org.opendaylight.controller.yang.parser.builder.api.GroupingBuilder;
import org.opendaylight.controller.yang.parser.builder.api.SchemaNodeBuilder;
import org.opendaylight.controller.yang.parser.builder.api.TypeAwareBuilder;
import org.opendaylight.controller.yang.parser.builder.api.TypeDefinitionBuilder;
import org.opendaylight.controller.yang.parser.builder.api.UsesNodeBuilder;
import org.opendaylight.controller.yang.parser.builder.impl.IdentitySchemaNodeBuilder;
import org.opendaylight.controller.yang.parser.builder.impl.IdentityrefTypeBuilder;
import org.opendaylight.controller.yang.parser.builder.impl.ModuleBuilder;
import org.opendaylight.controller.yang.parser.builder.impl.RpcDefinitionBuilder;
import org.opendaylight.controller.yang.parser.builder.impl.UnionTypeBuilder;
import org.opendaylight.controller.yang.parser.builder.impl.UnknownSchemaNodeBuilder;
import org.opendaylight.controller.yang.parser.util.ModuleDependencySort;
import org.opendaylight.controller.yang.parser.util.RefineHolder;
import org.opendaylight.controller.yang.parser.util.RefineUtils;
import org.opendaylight.controller.yang.parser.util.TypeConstraints;
import org.opendaylight.controller.yang.parser.util.YangParseException;
import org.opendaylight.controller.yang.validator.YangModelBasicValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public final class YangParserImpl implements YangModelParser {

    private static final Logger logger = LoggerFactory.getLogger(YangParserImpl.class);

    @Override
    public Set<Module> parseYangModels(final List<File> yangFiles) {
        return Sets.newLinkedHashSet(parseYangModelsMapped(yangFiles).values());
    }

    @Override
    public Set<Module> parseYangModels(final List<File> yangFiles, final SchemaContext context) {
        if (yangFiles != null) {
            final Map<InputStream, File> inputStreams = Maps.newHashMap();

            for (final File yangFile : yangFiles) {
                try {
                    inputStreams.put(new FileInputStream(yangFile), yangFile);
                } catch (FileNotFoundException e) {
                    logger.warn("Exception while reading yang file: " + yangFile.getName(), e);
                }
            }

            Map<ModuleBuilder, InputStream> builderToStreamMap = Maps.newHashMap();

            final Map<String, TreeMap<Date, ModuleBuilder>> modules = resolveModuleBuilders(
                    Lists.newArrayList(inputStreams.keySet()), builderToStreamMap);

            for (InputStream is : inputStreams.keySet()) {
                try {
                    is.close();
                } catch (IOException e) {
                    logger.debug("Failed to close stream.");
                }
            }

            return new LinkedHashSet<Module>(buildWithContext(modules, context).values());
        }
        return Collections.emptySet();
    }

    @Override
    public Set<Module> parseYangModelsFromStreams(final List<InputStream> yangModelStreams) {
        return Sets.newHashSet(parseYangModelsFromStreamsMapped(yangModelStreams).values());
    }

    @Override
    public Set<Module> parseYangModelsFromStreams(final List<InputStream> yangModelStreams, SchemaContext context) {
        if (yangModelStreams != null) {
            Map<ModuleBuilder, InputStream> builderToStreamMap = Maps.newHashMap();
            final Map<String, TreeMap<Date, ModuleBuilder>> modules = resolveModuleBuildersWithContext(
                    yangModelStreams, builderToStreamMap, context);
            return new LinkedHashSet<Module>(buildWithContext(modules, context).values());
        }
        return Collections.emptySet();
    }

    @Override
    public Map<File, Module> parseYangModelsMapped(List<File> yangFiles) {
        if (yangFiles != null) {
            final Map<InputStream, File> inputStreams = Maps.newHashMap();

            for (final File yangFile : yangFiles) {
                try {
                    inputStreams.put(new FileInputStream(yangFile), yangFile);
                } catch (FileNotFoundException e) {
                    logger.warn("Exception while reading yang file: " + yangFile.getName(), e);
                }
            }

            Map<ModuleBuilder, InputStream> builderToStreamMap = Maps.newHashMap();
            final Map<String, TreeMap<Date, ModuleBuilder>> modules = resolveModuleBuilders(
                    Lists.newArrayList(inputStreams.keySet()), builderToStreamMap);

            for (InputStream is : inputStreams.keySet()) {
                try {
                    is.close();
                } catch (IOException e) {
                    logger.debug("Failed to close stream.");
                }
            }

            Map<File, Module> retVal = Maps.newLinkedHashMap();
            Map<ModuleBuilder, Module> builderToModuleMap = build(modules);

            for (Entry<ModuleBuilder, Module> builderToModule : builderToModuleMap.entrySet()) {
                retVal.put(inputStreams.get(builderToStreamMap.get(builderToModule.getKey())),
                        builderToModule.getValue());
            }

            return retVal;
        }
        return Collections.emptyMap();
    }

    @Override
    public Map<InputStream, Module> parseYangModelsFromStreamsMapped(final List<InputStream> yangModelStreams) {
        Map<ModuleBuilder, InputStream> builderToStreamMap = Maps.newHashMap();

        final Map<String, TreeMap<Date, ModuleBuilder>> modules = resolveModuleBuilders(yangModelStreams,
                builderToStreamMap);
        Map<InputStream, Module> retVal = Maps.newLinkedHashMap();
        Map<ModuleBuilder, Module> builderToModuleMap = build(modules);

        for (Entry<ModuleBuilder, Module> builderToModule : builderToModuleMap.entrySet()) {
            retVal.put(builderToStreamMap.get(builderToModule.getKey()), builderToModule.getValue());
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

    private Map<String, TreeMap<Date, ModuleBuilder>> resolveModuleBuilders(final List<InputStream> yangFileStreams,
            Map<ModuleBuilder, InputStream> streamToBuilderMap) {
        return resolveModuleBuildersWithContext(yangFileStreams, streamToBuilderMap, null);
    }

    private Map<String, TreeMap<Date, ModuleBuilder>> resolveModuleBuildersWithContext(
            final List<InputStream> yangFileStreams, final Map<ModuleBuilder, InputStream> streamToBuilderMap,
            final SchemaContext context) {
        final ModuleBuilder[] builders = parseModuleBuilders(yangFileStreams, streamToBuilderMap);

        // Linked Hash Map MUST be used because Linked Hash Map preserves ORDER
        // of items stored in map.
        final LinkedHashMap<String, TreeMap<Date, ModuleBuilder>> modules = new LinkedHashMap<String, TreeMap<Date, ModuleBuilder>>();

        // module dependency graph sorted
        List<ModuleBuilder> sorted = null;
        if (context == null) {
            sorted = ModuleDependencySort.sort(builders);
        } else {
            sorted = ModuleDependencySort.sortWithContext(context, builders);
        }

        for (final ModuleBuilder builder : sorted) {
            if (builder == null) {
                continue;
            }
            final String builderName = builder.getName();
            Date builderRevision = builder.getRevision();
            if (builderRevision == null) {
                builderRevision = new Date(0L);
            }
            TreeMap<Date, ModuleBuilder> builderByRevision = modules.get(builderName);
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
            parser.removeErrorListeners();
            parser.addErrorListener(new YangErrorListener());

            result = parser.yang();
        } catch (IOException e) {
            logger.warn("Exception while reading yang file: " + yangStream, e);
        }
        return result;
    }

    private Map<ModuleBuilder, Module> build(final Map<String, TreeMap<Date, ModuleBuilder>> modules) {
        // fix unresolved nodes
        for (Map.Entry<String, TreeMap<Date, ModuleBuilder>> entry : modules.entrySet()) {
            for (Map.Entry<Date, ModuleBuilder> childEntry : entry.getValue().entrySet()) {
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
        for (Map.Entry<String, TreeMap<Date, ModuleBuilder>> entry : modules.entrySet()) {
            final Map<Date, Module> modulesByRevision = new HashMap<Date, Module>();
            for (Map.Entry<Date, ModuleBuilder> childEntry : entry.getValue().entrySet()) {
                final ModuleBuilder moduleBuilder = childEntry.getValue();
                final Module module = moduleBuilder.build();
                modulesByRevision.put(childEntry.getKey(), module);
                result.put(moduleBuilder, module);
            }
        }
        return result;
    }

    private Map<ModuleBuilder, Module> buildWithContext(final Map<String, TreeMap<Date, ModuleBuilder>> modules,
            SchemaContext context) {
        // fix unresolved nodes
        for (Map.Entry<String, TreeMap<Date, ModuleBuilder>> entry : modules.entrySet()) {
            for (Map.Entry<Date, ModuleBuilder> childEntry : entry.getValue().entrySet()) {
                final ModuleBuilder moduleBuilder = childEntry.getValue();
                fixUnresolvedNodesWithContext(modules, moduleBuilder, context);
            }
        }
        resolveAugmentsWithContext(modules, context);

        // build
        // LinkedHashMap MUST be used otherwise the values will not maintain
        // order!
        // http://docs.oracle.com/javase/6/docs/api/java/util/LinkedHashMap.html
        final Map<ModuleBuilder, Module> result = new LinkedHashMap<ModuleBuilder, Module>();
        for (Map.Entry<String, TreeMap<Date, ModuleBuilder>> entry : modules.entrySet()) {
            final Map<Date, Module> modulesByRevision = new HashMap<Date, Module>();
            for (Map.Entry<Date, ModuleBuilder> childEntry : entry.getValue().entrySet()) {
                final ModuleBuilder moduleBuilder = childEntry.getValue();
                final Module module = moduleBuilder.build();
                modulesByRevision.put(childEntry.getKey(), module);
                result.put(moduleBuilder, module);
            }
        }
        return result;
    }

    private void fixUnresolvedNodes(final Map<String, TreeMap<Date, ModuleBuilder>> modules, final ModuleBuilder builder) {
        resolveDirtyNodes(modules, builder);
        resolveIdentities(modules, builder);
        resolveUsesRefine(modules, builder);
        resolveUnknownNodes(modules, builder);
    }

    private void fixUnresolvedNodesWithContext(final Map<String, TreeMap<Date, ModuleBuilder>> modules,
            final ModuleBuilder builder, final SchemaContext context) {
        resolveDirtyNodesWithContext(modules, builder, context);
        resolveIdentitiesWithContext(modules, builder, context);
        resolveUsesRefineWithContext(modules, builder, context);
        resolveUnknownNodesWithContext(modules, builder, context);
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
    private void resolveDirtyNodes(final Map<String, TreeMap<Date, ModuleBuilder>> modules, final ModuleBuilder module) {
        final Map<List<String>, TypeAwareBuilder> dirtyNodes = module.getDirtyNodes();
        if (!dirtyNodes.isEmpty()) {
            for (Map.Entry<List<String>, TypeAwareBuilder> entry : dirtyNodes.entrySet()) {
                final TypeAwareBuilder nodeToResolve = entry.getValue();

                if (nodeToResolve instanceof UnionTypeBuilder) {
                    // special handling for union types
                    resolveTypeUnion((UnionTypeBuilder) nodeToResolve, modules, module);
                } else if (nodeToResolve.getTypedef() instanceof IdentityrefTypeBuilder) {
                    // special handling for identityref types
                    IdentityrefTypeBuilder idref = (IdentityrefTypeBuilder) nodeToResolve.getTypedef();
                    nodeToResolve.setType(new IdentityrefType(findFullQName(modules, module, idref), idref.getPath()));
                } else {
                    resolveType(nodeToResolve, modules, module);
                }
            }
        }
    }

    private void resolveDirtyNodesWithContext(final Map<String, TreeMap<Date, ModuleBuilder>> modules,
            final ModuleBuilder module, SchemaContext context) {
        final Map<List<String>, TypeAwareBuilder> dirtyNodes = module.getDirtyNodes();
        if (!dirtyNodes.isEmpty()) {
            for (Map.Entry<List<String>, TypeAwareBuilder> entry : dirtyNodes.entrySet()) {
                final TypeAwareBuilder nodeToResolve = entry.getValue();

                if (nodeToResolve instanceof UnionTypeBuilder) {
                    // special handling for union types
                    resolveTypeUnionWithContext((UnionTypeBuilder) nodeToResolve, modules, module, context);
                } else if (nodeToResolve.getTypedef() instanceof IdentityrefTypeBuilder) {
                    // special handling for identityref types
                    IdentityrefTypeBuilder idref = (IdentityrefTypeBuilder) nodeToResolve.getTypedef();
                    nodeToResolve.setType(new IdentityrefType(findFullQName(modules, module, idref), idref.getPath()));
                } else {
                    resolveTypeWithContext(nodeToResolve, modules, module, context);
                }
            }
        }
    }

    /**
     * Resolve unknown type of node. It is assumed that type of node is either
     * UnknownType or ExtendedType with UnknownType as base type.
     *
     * @param nodeToResolve
     *            node with type to resolve
     * @param modules
     *            all loaded modules
     * @param module
     *            current module
     */
    private void resolveType(final TypeAwareBuilder nodeToResolve,
            final Map<String, TreeMap<Date, ModuleBuilder>> modules, final ModuleBuilder module) {
        TypeDefinitionBuilder resolvedType = null;
        final int line = nodeToResolve.getLine();
        final TypeDefinition<?> nodeToResolveType = nodeToResolve.getType();
        final QName unknownTypeQName = nodeToResolveType.getBaseType().getQName();
        final ModuleBuilder dependentModule = findDependentModuleBuilder(modules, module, unknownTypeQName.getPrefix(),
                line);

        final TypeDefinitionBuilder targetTypeBuilder = findTypeDefinitionBuilder(nodeToResolve.getPath(),
                dependentModule, unknownTypeQName.getLocalName(), module.getName(), line);

        if (nodeToResolveType instanceof ExtendedType) {
            final ExtendedType extType = (ExtendedType) nodeToResolveType;
            final TypeDefinitionBuilder newType = extendedTypeWithNewBaseTypeBuilder(targetTypeBuilder, extType,
                    modules, module, nodeToResolve.getLine());
            resolvedType = newType;
        } else {
            resolvedType = targetTypeBuilder;
        }

        // validate constraints
        final TypeConstraints constraints = findConstraintsFromTypeBuilder(nodeToResolve,
                new TypeConstraints(module.getName(), nodeToResolve.getLine()), modules, module, null);
        constraints.validateConstraints();

        nodeToResolve.setTypedef(resolvedType);
    }

    /**
     * Resolve unknown type of node. It is assumed that type of node is either
     * UnknownType or ExtendedType with UnknownType as base type.
     *
     * @param nodeToResolve
     *            node with type to resolve
     * @param modules
     *            all loaded modules
     * @param module
     *            current module
     * @param context
     *            SchemaContext containing already resolved modules
     */
    private void resolveTypeWithContext(final TypeAwareBuilder nodeToResolve,
            final Map<String, TreeMap<Date, ModuleBuilder>> modules, final ModuleBuilder module,
            final SchemaContext context) {
        TypeDefinitionBuilder resolvedType = null;
        final int line = nodeToResolve.getLine();
        final TypeDefinition<?> nodeToResolveType = nodeToResolve.getType();
        final QName unknownTypeQName = nodeToResolveType.getBaseType().getQName();
        final ModuleBuilder dependentModuleBuilder = findDependentModuleBuilder(modules, module,
                unknownTypeQName.getPrefix(), line);

        if (dependentModuleBuilder == null) {
            final Module dependentModule = findModuleFromContext(context, module, unknownTypeQName.getPrefix(), line);
            final Set<TypeDefinition<?>> types = dependentModule.getTypeDefinitions();
            final TypeDefinition<?> type = findTypeByName(types, unknownTypeQName.getLocalName());

            if (nodeToResolveType instanceof ExtendedType) {
                final ExtendedType extType = (ExtendedType) nodeToResolveType;
                final TypeDefinitionBuilder newType = extendedTypeWithNewBaseType(type, extType, module,
                        nodeToResolve.getLine());

                nodeToResolve.setTypedef(newType);
            } else {
                if(nodeToResolve instanceof TypeDefinitionBuilder) {
                    TypeDefinitionBuilder tdb = (TypeDefinitionBuilder)nodeToResolve;
                    TypeConstraints tc = findConstraintsFromTypeBuilder(nodeToResolve, new TypeConstraints(module.getName(), nodeToResolve.getLine()), modules, module, context);
                    tdb.setLengths(tc.getLength());
                    tdb.setPatterns(tc.getPatterns());
                    tdb.setRanges(tc.getRange());
                    tdb.setFractionDigits(tc.getFractionDigits());
                }
                nodeToResolve.setType(type);
            }

        } else {
            final TypeDefinitionBuilder targetTypeBuilder = findTypeDefinitionBuilder(nodeToResolve.getPath(),
                    dependentModuleBuilder, unknownTypeQName.getLocalName(), module.getName(), line);

            if (nodeToResolveType instanceof ExtendedType) {
                final ExtendedType extType = (ExtendedType) nodeToResolveType;
                final TypeDefinitionBuilder newType = extendedTypeWithNewBaseTypeBuilder(targetTypeBuilder, extType,
                        modules, module, nodeToResolve.getLine());
                resolvedType = newType;
            } else {
                resolvedType = targetTypeBuilder;
            }

            // validate constraints
            final TypeConstraints constraints = findConstraintsFromTypeBuilder(nodeToResolve, new TypeConstraints(
                    module.getName(), nodeToResolve.getLine()), modules, module, context);
            constraints.validateConstraints();

            nodeToResolve.setTypedef(resolvedType);
        }
    }

    private void resolveTypeUnion(final UnionTypeBuilder union,
            final Map<String, TreeMap<Date, ModuleBuilder>> modules, final ModuleBuilder builder) {

        final List<TypeDefinition<?>> unionTypes = union.getTypes();
        final List<TypeDefinition<?>> toRemove = new ArrayList<TypeDefinition<?>>();
        for (TypeDefinition<?> unionType : unionTypes) {
            if (unionType instanceof UnknownType) {
                final UnknownType ut = (UnknownType) unionType;
                final ModuleBuilder dependentModule = findDependentModuleBuilder(modules, builder, ut.getQName()
                        .getPrefix(), union.getLine());
                final TypeDefinitionBuilder resolvedType = findTypeDefinitionBuilder(union.getPath(), dependentModule,
                        ut.getQName().getLocalName(), builder.getName(), union.getLine());
                union.setTypedef(resolvedType);
                toRemove.add(ut);
            } else if (unionType instanceof ExtendedType) {
                final ExtendedType extType = (ExtendedType) unionType;
                final TypeDefinition<?> extTypeBase = extType.getBaseType();
                if (extTypeBase instanceof UnknownType) {
                    final UnknownType ut = (UnknownType) extTypeBase;
                    final ModuleBuilder dependentModule = findDependentModuleBuilder(modules, builder, ut.getQName()
                            .getPrefix(), union.getLine());
                    final TypeDefinitionBuilder targetTypeBuilder = findTypeDefinitionBuilder(union.getPath(),
                            dependentModule, ut.getQName().getLocalName(), builder.getName(), union.getLine());

                    final TypeDefinitionBuilder newType = extendedTypeWithNewBaseTypeBuilder(targetTypeBuilder,
                            extType, modules, builder, union.getLine());

                    union.setTypedef(newType);
                    toRemove.add(extType);
                }
            }
        }
        unionTypes.removeAll(toRemove);
    }

    private void resolveTypeUnionWithContext(final UnionTypeBuilder union,
            final Map<String, TreeMap<Date, ModuleBuilder>> modules, final ModuleBuilder builder,
            final SchemaContext context) {

        final List<TypeDefinition<?>> unionTypes = union.getTypes();
        final List<TypeDefinition<?>> toRemove = new ArrayList<TypeDefinition<?>>();
        for (TypeDefinition<?> unionType : unionTypes) {
            if (unionType instanceof UnknownType) {
                final UnknownType ut = (UnknownType) unionType;
                final QName utQName = ut.getQName();
                final ModuleBuilder dependentModuleBuilder = findDependentModuleBuilder(modules, builder,
                        utQName.getPrefix(), union.getLine());

                if (dependentModuleBuilder == null) {
                    Module dependentModule = findModuleFromContext(context, builder, utQName.getPrefix(),
                            union.getLine());
                    Set<TypeDefinition<?>> types = dependentModule.getTypeDefinitions();
                    TypeDefinition<?> type = findTypeByName(types, utQName.getLocalName());
                    union.setType(type);
                    toRemove.add(ut);
                } else {
                    final TypeDefinitionBuilder resolvedType = findTypeDefinitionBuilder(union.getPath(),
                            dependentModuleBuilder, utQName.getLocalName(), builder.getName(), union.getLine());
                    union.setTypedef(resolvedType);
                    toRemove.add(ut);
                }

            } else if (unionType instanceof ExtendedType) {
                final ExtendedType extType = (ExtendedType) unionType;
                TypeDefinition<?> extTypeBase = extType.getBaseType();
                if (extTypeBase instanceof UnknownType) {
                    final UnknownType ut = (UnknownType) extTypeBase;
                    final QName utQName = ut.getQName();
                    final ModuleBuilder dependentModuleBuilder = findDependentModuleBuilder(modules, builder,
                            utQName.getPrefix(), union.getLine());

                    if (dependentModuleBuilder == null) {
                        final Module dependentModule = findModuleFromContext(context, builder, utQName.getPrefix(),
                                union.getLine());
                        Set<TypeDefinition<?>> types = dependentModule.getTypeDefinitions();
                        TypeDefinition<?> type = findTypeByName(types, utQName.getLocalName());
                        final TypeDefinitionBuilder newType = extendedTypeWithNewBaseType(type, extType, builder, 0);

                        union.setTypedef(newType);
                        toRemove.add(extType);
                    } else {
                        final TypeDefinitionBuilder targetTypeBuilder = findTypeDefinitionBuilder(union.getPath(),
                                dependentModuleBuilder, utQName.getLocalName(), builder.getName(), union.getLine());

                        final TypeDefinitionBuilder newType = extendedTypeWithNewBaseTypeBuilder(targetTypeBuilder,
                                extType, modules, builder, union.getLine());

                        union.setTypedef(newType);
                        toRemove.add(extType);
                    }
                }
            }
        }
        unionTypes.removeAll(toRemove);
    }

    /**
     * Go through all augment definitions and resolve them. It is expected that
     * modules are already sorted by their dependencies. This method also finds
     * augment target node and add child nodes to it.
     *
     * @param modules
     *            all available modules
     */
    private void resolveAugments(final Map<String, TreeMap<Date, ModuleBuilder>> modules) {
        final List<ModuleBuilder> allModulesList = new ArrayList<ModuleBuilder>();
        final Set<ModuleBuilder> allModulesSet = new HashSet<ModuleBuilder>();
        for (Map.Entry<String, TreeMap<Date, ModuleBuilder>> entry : modules.entrySet()) {
            for (Map.Entry<Date, ModuleBuilder> inner : entry.getValue().entrySet()) {
                allModulesList.add(inner.getValue());
                allModulesSet.add(inner.getValue());
            }
        }

        for (int i = 0; i < allModulesList.size(); i++) {
            final ModuleBuilder module = allModulesList.get(i);
            // try to resolve augments in module
            resolveAugment(modules, module);
            // while all augments are not resolved
            final Iterator<ModuleBuilder> allModulesIterator = allModulesSet.iterator();
            while (!(module.getAugmentsResolved() == module.getAugments().size())) {
                ModuleBuilder nextModule = null;
                // try resolve other module augments
                try {
                    nextModule = allModulesIterator.next();
                    resolveAugment(modules, nextModule);
                } catch (NoSuchElementException e) {
                    throw new YangParseException("Failed to resolve augments in module '" + module.getName() + "'.", e);
                }
                // then try to resolve first module again
                resolveAugment(modules, module);
            }
        }
    }

    /**
     * Tries to resolve augments in given module. If augment target node is not
     * found, do nothing.
     *
     * @param modules
     *            all available modules
     * @param module
     *            current module
     */
    private void resolveAugment(final Map<String, TreeMap<Date, ModuleBuilder>> modules, final ModuleBuilder module) {
        if (module.getAugmentsResolved() < module.getAugments().size()) {
            for (AugmentationSchemaBuilder augmentBuilder : module.getAugments()) {

                if (!augmentBuilder.isResolved()) {
                    final SchemaPath augmentTargetSchemaPath = augmentBuilder.getTargetPath();
                    final List<QName> path = augmentTargetSchemaPath.getPath();

                    final QName qname = path.get(0);
                    String prefix = qname.getPrefix();
                    if (prefix == null) {
                        prefix = module.getPrefix();
                    }

                    final ModuleBuilder dependentModule = findDependentModuleBuilder(modules, module, prefix,
                            augmentBuilder.getLine());
                    processAugmentation(augmentBuilder, path, module, qname, dependentModule);
                }

            }
        }
    }

    /**
     * Go through all augment definitions and resolve them. This method works in
     * same way as {@link #resolveAugments(Map)} except that if target node is not
     * found in loaded modules, it search for target node in given context.
     *
     * @param modules
     *            all loaded modules
     * @param context
     *            SchemaContext containing already resolved modules
     */
    private void resolveAugmentsWithContext(final Map<String, TreeMap<Date, ModuleBuilder>> modules,
            final SchemaContext context) {
        final List<ModuleBuilder> allModulesList = new ArrayList<ModuleBuilder>();
        final Set<ModuleBuilder> allModulesSet = new HashSet<ModuleBuilder>();
        for (Map.Entry<String, TreeMap<Date, ModuleBuilder>> entry : modules.entrySet()) {
            for (Map.Entry<Date, ModuleBuilder> inner : entry.getValue().entrySet()) {
                allModulesList.add(inner.getValue());
                allModulesSet.add(inner.getValue());
            }
        }

        for (int i = 0; i < allModulesList.size(); i++) {
            final ModuleBuilder module = allModulesList.get(i);
            // try to resolve augments in module
            resolveAugmentWithContext(modules, module, context);
            // while all augments are not resolved
            final Iterator<ModuleBuilder> allModulesIterator = allModulesSet.iterator();
            while (!(module.getAugmentsResolved() == module.getAugments().size())) {
                ModuleBuilder nextModule = null;
                // try resolve other module augments
                try {
                    nextModule = allModulesIterator.next();
                    resolveAugmentWithContext(modules, nextModule, context);
                } catch (NoSuchElementException e) {
                    throw new YangParseException("Failed to resolve augments in module '" + module.getName() + "'.", e);
                }
                // then try to resolve first module again
                resolveAugmentWithContext(modules, module, context);
            }
        }
    }

    /**
     * Tries to resolve augments in given module. If augment target node is not
     * found, do nothing.
     *
     * @param modules
     *            all available modules
     * @param module
     *            current module
     */
    private void resolveAugmentWithContext(final Map<String, TreeMap<Date, ModuleBuilder>> modules,
            final ModuleBuilder module, final SchemaContext context) {
        if (module.getAugmentsResolved() < module.getAugments().size()) {

            for (AugmentationSchemaBuilder augmentBuilder : module.getAugments()) {
                final int line = augmentBuilder.getLine();

                if (!augmentBuilder.isResolved()) {
                    final List<QName> path = augmentBuilder.getTargetPath().getPath();
                    final QName qname = path.get(0);
                    String prefix = qname.getPrefix();
                    if (prefix == null) {
                        prefix = module.getPrefix();
                    }

                    // try to find augment target module in loaded modules...
                    final ModuleBuilder dependentModuleBuilder = findDependentModuleBuilder(modules, module, prefix,
                            line);
                    if (dependentModuleBuilder == null) {
                        // perform augmentation on module from context and
                        // continue to next augment
                        processAugmentationOnContext(augmentBuilder, path, module, prefix, line, context);
                        continue;
                    } else {
                        processAugmentation(augmentBuilder, path, module, qname, dependentModuleBuilder);
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
    private void resolveIdentities(final Map<String, TreeMap<Date, ModuleBuilder>> modules, final ModuleBuilder module) {
        final Set<IdentitySchemaNodeBuilder> identities = module.getIdentities();
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
                final ModuleBuilder dependentModule = findDependentModuleBuilder(modules, module, baseIdentityPrefix,
                        identity.getLine());

                final Set<IdentitySchemaNodeBuilder> dependentModuleIdentities = dependentModule.getIdentities();
                for (IdentitySchemaNodeBuilder idBuilder : dependentModuleIdentities) {
                    if (idBuilder.getQName().getLocalName().equals(baseIdentityLocalName)) {
                        identity.setBaseIdentity(idBuilder);
                    }
                }
            }
        }
    }

    /**
     * Go through identity statements defined in current module and resolve
     * their 'base' statement. Method tries to find base identity in given
     * modules. If base identity is not found, method will search it in context.
     *
     * @param modules
     *            all loaded modules
     * @param module
     *            current module
     * @param context
     *            SchemaContext containing already resolved modules
     */
    private void resolveIdentitiesWithContext(final Map<String, TreeMap<Date, ModuleBuilder>> modules,
            final ModuleBuilder module, SchemaContext context) {
        final Set<IdentitySchemaNodeBuilder> identities = module.getIdentities();
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
                final ModuleBuilder dependentModuleBuilder = findDependentModuleBuilder(modules, module,
                        baseIdentityPrefix, identity.getLine());

                if (dependentModuleBuilder == null) {
                    final Module dependentModule = findModuleFromContext(context, module, baseIdentityPrefix,
                            identity.getLine());
                    final Set<IdentitySchemaNode> dependentModuleIdentities = dependentModule.getIdentities();
                    for (IdentitySchemaNode idNode : dependentModuleIdentities) {
                        if (idNode.getQName().getLocalName().equals(baseIdentityLocalName)) {
                            identity.setBaseIdentity(idNode);
                        }
                    }
                } else {
                    final Set<IdentitySchemaNodeBuilder> dependentModuleIdentities = dependentModuleBuilder
                            .getIdentities();
                    for (IdentitySchemaNodeBuilder idBuilder : dependentModuleIdentities) {
                        if (idBuilder.getQName().getLocalName().equals(baseIdentityLocalName)) {
                            identity.setBaseIdentity(idBuilder);
                        }
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
    private void resolveUsesRefine(final Map<String, TreeMap<Date, ModuleBuilder>> modules, final ModuleBuilder module) {
        final Map<List<String>, UsesNodeBuilder> moduleUses = module.getUsesNodes();
        for (Map.Entry<List<String>, UsesNodeBuilder> entry : moduleUses.entrySet()) {
            final UsesNodeBuilder usesNode = entry.getValue();
            final int line = usesNode.getLine();
            final GroupingBuilder targetGrouping = getTargetGroupingFromModules(usesNode, modules, module);
            usesNode.setGroupingPath(targetGrouping.getPath());
            for (RefineHolder refine : usesNode.getRefines()) {
                final SchemaNodeBuilder nodeToRefine = RefineUtils.getRefineNodeFromGroupingBuilder(targetGrouping,
                        refine, module.getName());
                RefineUtils.performRefine(nodeToRefine, refine, line);
                usesNode.addRefineNode(nodeToRefine);
            }
        }
    }

    /**
     * Tries to search target grouping in given modules and resolve refine
     * nodes. If grouping is not found in modules, method tries to find it in
     * modules from context.
     *
     * @param modules
     *            all loaded modules
     * @param module
     *            current module
     * @param context
     *            SchemaContext containing already resolved modules
     */
    private void resolveUsesRefineWithContext(final Map<String, TreeMap<Date, ModuleBuilder>> modules,
            final ModuleBuilder module, SchemaContext context) {
        final Map<List<String>, UsesNodeBuilder> moduleUses = module.getUsesNodes();
        for (Map.Entry<List<String>, UsesNodeBuilder> entry : moduleUses.entrySet()) {
            final UsesNodeBuilder usesNode = entry.getValue();
            final int line = usesNode.getLine();

            final GroupingBuilder targetGroupingBuilder = getTargetGroupingFromModules(usesNode, modules, module);
            if (targetGroupingBuilder == null) {
                final GroupingDefinition targetGrouping = getTargetGroupingFromContext(usesNode, module, context);
                usesNode.setGroupingPath(targetGrouping.getPath());
                for (RefineHolder refine : usesNode.getRefines()) {
                    final SchemaNodeBuilder nodeToRefine = RefineUtils.getRefineNodeFromGroupingDefinition(
                            targetGrouping, refine, module.getName());
                    RefineUtils.performRefine(nodeToRefine, refine, line);
                    usesNode.addRefineNode(nodeToRefine);
                }
            } else {
                usesNode.setGroupingPath(targetGroupingBuilder.getPath());
                for (RefineHolder refine : usesNode.getRefines()) {
                    final SchemaNodeBuilder nodeToRefine = RefineUtils.getRefineNodeFromGroupingBuilder(
                            targetGroupingBuilder, refine, module.getName());
                    RefineUtils.performRefine(nodeToRefine, refine, line);
                    usesNode.addRefineNode(nodeToRefine);
                }
            }
        }
    }

    /**
     * Search given modules for grouping by name defined in uses node.
     *
     * @param usesBuilder
     *            builder of uses statement
     * @param modules
     *            all loaded modules
     * @param module
     *            current module
     * @return grouping with given name if found, null otherwise
     */
    private GroupingBuilder getTargetGroupingFromModules(final UsesNodeBuilder usesBuilder,
            final Map<String, TreeMap<Date, ModuleBuilder>> modules, final ModuleBuilder module) {
        final int line = usesBuilder.getLine();
        final String groupingString = usesBuilder.getGroupingName();
        String groupingPrefix;
        String groupingName;

        if (groupingString.contains(":")) {
            String[] splitted = groupingString.split(":");
            if (splitted.length != 2 || groupingString.contains("/")) {
                throw new YangParseException(module.getName(), line, "Invalid name of target grouping");
            }
            groupingPrefix = splitted[0];
            groupingName = splitted[1];
        } else {
            groupingPrefix = module.getPrefix();
            groupingName = groupingString;
        }

        ModuleBuilder dependentModule = null;
        if (groupingPrefix.equals(module.getPrefix())) {
            dependentModule = module;
        } else {
            dependentModule = findDependentModuleBuilder(modules, module, groupingPrefix, line);
        }

        if (dependentModule == null) {
            return null;
        }

        List<QName> path = usesBuilder.getPath().getPath();
        GroupingBuilder result = null;
        Set<GroupingBuilder> groupings = dependentModule.getModuleGroupings();
        result = findGroupingBuilder(groupings, groupingName);

        if (result == null) {
            Builder currentNode = null;
            final List<String> currentPath = new ArrayList<String>();
            currentPath.add(dependentModule.getName());

            for (int i = 0; i < path.size(); i++) {
                QName qname = path.get(i);
                currentPath.add(qname.getLocalName());
                currentNode = dependentModule.getModuleNode(currentPath);

                if (currentNode instanceof RpcDefinitionBuilder) {
                    groupings = ((RpcDefinitionBuilder) currentNode).getGroupings();
                } else if (currentNode instanceof DataNodeContainerBuilder) {
                    groupings = ((DataNodeContainerBuilder) currentNode).getGroupings();
                } else {
                    groupings = Collections.emptySet();
                }

                result = findGroupingBuilder(groupings, groupingName);
                if (result != null) {
                    break;
                }
            }
        }

        return result;
    }

    /**
     * Search context for grouping by name defined in uses node.
     *
     * @param usesBuilder
     *            builder of uses statement
     * @param module
     *            current module
     * @param context
     *            SchemaContext containing already resolved modules
     * @return grouping with given name if found, null otherwise
     */
    private GroupingDefinition getTargetGroupingFromContext(final UsesNodeBuilder usesBuilder,
            final ModuleBuilder module, SchemaContext context) {
        final int line = usesBuilder.getLine();
        String groupingString = usesBuilder.getGroupingName();
        String groupingPrefix;
        String groupingName;

        if (groupingString.contains(":")) {
            String[] splitted = groupingString.split(":");
            if (splitted.length != 2 || groupingString.contains("/")) {
                throw new YangParseException(module.getName(), line, "Invalid name of target grouping");
            }
            groupingPrefix = splitted[0];
            groupingName = splitted[1];
        } else {
            groupingPrefix = module.getPrefix();
            groupingName = groupingString;
        }

        Module dependentModule = findModuleFromContext(context, module, groupingPrefix, line);
        return findGroupingDefinition(dependentModule.getGroupings(), groupingName);
    }

    private QName findFullQName(final Map<String, TreeMap<Date, ModuleBuilder>> modules, final ModuleBuilder module,
            final IdentityrefTypeBuilder idref) {
        QName result = null;
        String baseString = idref.getBaseString();
        if (baseString.contains(":")) {
            String[] splittedBase = baseString.split(":");
            if (splittedBase.length > 2) {
                throw new YangParseException(module.getName(), idref.getLine(), "Failed to parse identityref base: "
                        + baseString);
            }
            String prefix = splittedBase[0];
            String name = splittedBase[1];
            ModuleBuilder dependentModule = findDependentModuleBuilder(modules, module, prefix, idref.getLine());
            result = new QName(dependentModule.getNamespace(), dependentModule.getRevision(), prefix, name);
        } else {
            result = new QName(module.getNamespace(), module.getRevision(), module.getPrefix(), baseString);
        }
        return result;
    }

    private void resolveUnknownNodes(final Map<String, TreeMap<Date, ModuleBuilder>> modules, final ModuleBuilder module) {
        for (UnknownSchemaNodeBuilder usnb : module.getUnknownNodes()) {
            QName nodeType = usnb.getNodeType();
            if (nodeType.getNamespace() == null || nodeType.getRevision() == null) {
                try {
                    ModuleBuilder dependentModule = findDependentModuleBuilder(modules, module, nodeType.getPrefix(),
                            usnb.getLine());
                    QName newNodeType = new QName(dependentModule.getNamespace(), dependentModule.getRevision(),
                            nodeType.getPrefix(), nodeType.getLocalName());
                    usnb.setNodeType(newNodeType);
                } catch (YangParseException e) {
                    logger.debug(module.getName(), usnb.getLine(), "Failed to find unknown node type: " + nodeType);
                }
            }
        }
    }

    private void resolveUnknownNodesWithContext(final Map<String, TreeMap<Date, ModuleBuilder>> modules,
            final ModuleBuilder module, SchemaContext context) {
        for (UnknownSchemaNodeBuilder unknownNodeBuilder : module.getUnknownNodes()) {
            QName nodeType = unknownNodeBuilder.getNodeType();
            if (nodeType.getNamespace() == null || nodeType.getRevision() == null) {
                try {
                    ModuleBuilder dependentModuleBuilder = findDependentModuleBuilder(modules, module,
                            nodeType.getPrefix(), unknownNodeBuilder.getLine());

                    QName newNodeType = null;
                    if (dependentModuleBuilder == null) {
                        Module dependentModule = findModuleFromContext(context, module, nodeType.getPrefix(),
                                unknownNodeBuilder.getLine());
                        newNodeType = new QName(dependentModule.getNamespace(), dependentModule.getRevision(),
                                nodeType.getPrefix(), nodeType.getLocalName());
                    } else {
                        newNodeType = new QName(dependentModuleBuilder.getNamespace(),
                                dependentModuleBuilder.getRevision(), nodeType.getPrefix(), nodeType.getLocalName());
                    }

                    unknownNodeBuilder.setNodeType(newNodeType);
                } catch (YangParseException e) {
                    logger.debug(module.getName(), unknownNodeBuilder.getLine(), "Failed to find unknown node type: "
                            + nodeType);
                }
            }
        }
    }

}
