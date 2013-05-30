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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
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
import org.opendaylight.controller.yang.parser.builder.api.ChildNodeBuilder;
import org.opendaylight.controller.yang.parser.builder.api.DataSchemaNodeBuilder;
import org.opendaylight.controller.yang.parser.builder.api.GroupingBuilder;
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
import org.opendaylight.controller.yang.parser.builder.impl.TypedefBuilder;
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

public class YangParserImpl implements YangModelParser {

    private static final Logger logger = LoggerFactory
            .getLogger(YangParserImpl.class);

    @Override
    public Set<Module> parseYangModels(final List<File> yangFiles) {
        if (yangFiles != null) {
            final List<InputStream> inputStreams = new ArrayList<InputStream>();

            for (final File yangFile : yangFiles) {
                try {
                    inputStreams.add(new FileInputStream(yangFile));
                } catch (FileNotFoundException e) {
                    logger.warn("Exception while reading yang file: "
                            + yangFile.getName(), e);
                }
            }
            final Map<String, TreeMap<Date, ModuleBuilder>> modules = resolveModuleBuilders(inputStreams);
            return build(modules);
        }
        return Collections.emptySet();
    }

    @Override
    public Set<Module> parseYangModelsFromStreams(
            final List<InputStream> yangModelStreams) {
        final Map<String, TreeMap<Date, ModuleBuilder>> modules = resolveModuleBuilders(yangModelStreams);
        return build(modules);
    }

    @Override
    public SchemaContext resolveSchemaContext(final Set<Module> modules) {
        return new SchemaContextImpl(modules);
    }

    private Map<String, TreeMap<Date, ModuleBuilder>> resolveModuleBuilders(
            final List<InputStream> yangFileStreams) {
        // Linked Hash Map MUST be used because Linked Hash Map preserves ORDER
        // of items stored in map.
        final Map<String, TreeMap<Date, ModuleBuilder>> modules = new LinkedHashMap<String, TreeMap<Date, ModuleBuilder>>();
        final ParseTreeWalker walker = new ParseTreeWalker();
        final List<ParseTree> trees = parseStreams(yangFileStreams);
        final ModuleBuilder[] builders = new ModuleBuilder[trees.size()];

        // validate yang
        new YangModelBasicValidator(walker).validate(trees);

        YangParserListenerImpl yangModelParser = null;
        for (int i = 0; i < trees.size(); i++) {
            yangModelParser = new YangParserListenerImpl();
            walker.walk(yangModelParser, trees.get(i));
            builders[i] = yangModelParser.getModuleBuilder();
        }

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

    private Set<Module> build(
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
        // Linked Hash Set MUST be used otherwise the Set will not maintain
        // order!
        // http://docs.oracle.com/javase/6/docs/api/java/util/LinkedHashSet.html
        final Set<Module> result = new LinkedHashSet<Module>();
        for (Map.Entry<String, TreeMap<Date, ModuleBuilder>> entry : modules
                .entrySet()) {
            final Map<Date, Module> modulesByRevision = new HashMap<Date, Module>();
            for (Map.Entry<Date, ModuleBuilder> childEntry : entry.getValue()
                    .entrySet()) {
                final ModuleBuilder moduleBuilder = childEntry.getValue();
                final Module module = moduleBuilder.build();
                modulesByRevision.put(childEntry.getKey(), module);
                result.add(module);
            }
        }
        return result;
    }

    private void fixUnresolvedNodes(
            final Map<String, TreeMap<Date, ModuleBuilder>> modules,
            final ModuleBuilder builder) {
        resolveDirtyNodes(modules, builder);
        resolveIdentities(modules, builder);
        resolveUses(modules, builder);
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
                            union.setType(resolvedType);
                            toRemove.add(unknownType);
                        }
                    }
                    unionTypes.removeAll(toRemove);
                } else if (nodeToResolve.getTypedef() instanceof IdentityrefTypeBuilder) {
                    IdentityrefTypeBuilder idref = (IdentityrefTypeBuilder) nodeToResolve
                            .getTypedef();
                    nodeToResolve.setType(new IdentityrefType(findFullQName(
                            modules, module, idref), idref.getPath()));
                } else {
                    final TypeDefinitionBuilder resolvedType = resolveType(
                            nodeToResolve, modules, module);
                    nodeToResolve.setType(resolvedType);
                }
            }
        }
    }

    private TypeDefinitionBuilder resolveType(
            final TypeAwareBuilder typeToResolve,
            final Map<String, TreeMap<Date, ModuleBuilder>> modules,
            final ModuleBuilder builder) {
        final TypeConstraints constraints = new TypeConstraints();

        final TypeDefinitionBuilder targetType = getTypedefBuilder(
                typeToResolve, modules, builder);
        final TypeConstraints tConstraints = findConstraints(typeToResolve,
                constraints, modules, builder);
        targetType.setRanges(tConstraints.getRange());
        targetType.setLengths(tConstraints.getLength());
        targetType.setPatterns(tConstraints.getPatterns());
        targetType.setFractionDigits(tConstraints.getFractionDigits());

        return targetType;
    }

    private TypeDefinitionBuilder resolveTypeUnion(
            final TypeAwareBuilder typeToResolve,
            final UnknownType unknownType,
            final Map<String, TreeMap<Date, ModuleBuilder>> modules,
            final ModuleBuilder builder) {
        final TypeConstraints constraints = new TypeConstraints();

        final TypeDefinitionBuilder targetType = getUnionBuilder(typeToResolve,
                unknownType, modules, builder);
        final TypeConstraints tConstraints = findConstraints(typeToResolve,
                constraints, modules, builder);
        targetType.setRanges(tConstraints.getRange());
        targetType.setLengths(tConstraints.getLength());
        targetType.setPatterns(tConstraints.getPatterns());
        targetType.setFractionDigits(tConstraints.getFractionDigits());

        return targetType;
    }

    private TypeDefinitionBuilder getTypedefBuilder(
            final TypeAwareBuilder nodeToResolve,
            final Map<String, TreeMap<Date, ModuleBuilder>> modules,
            final ModuleBuilder builder) {

        final TypeDefinition<?> nodeToResolveBase = nodeToResolve.getType();
        if (nodeToResolveBase != null
                && !(nodeToResolveBase instanceof UnknownType)) {
            return (TypeDefinitionBuilder) nodeToResolve;
        }

        final UnknownType unknownType = (UnknownType) nodeToResolve.getType();
        final QName unknownTypeQName = unknownType.getQName();

        // search for module which contains referenced typedef
        final ModuleBuilder dependentModule = findDependentModule(modules,
                builder, unknownTypeQName.getPrefix(), nodeToResolve.getLine());
        final TypeDefinitionBuilder lookedUpBuilder = findTypedefBuilderByName(
                dependentModule, unknownTypeQName.getLocalName(),
                builder.getName(), nodeToResolve.getLine());

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
        final TypeDefinitionBuilder lookedUpBuilder = findTypedefBuilderByName(
                dependentModule, unknownTypeQName.getLocalName(),
                module.getName(), nodeToResolve.getLine());

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
            final UnionTypeBuilder newUnion = new UnionTypeBuilder(old.getLine());
            for (TypeDefinition<?> td : oldUnion.getTypes()) {
                newUnion.setType(td);
            }
            for (TypeDefinitionBuilder tdb : oldUnion.getTypedefs()) {
                newUnion.setType(copyTypedefBuilder(tdb, true));
            }
            newUnion.setPath(old.getPath());
            return newUnion;
        }

        final QName oldName = old.getQName();
        final QName newName = new QName(oldName.getNamespace(),
                oldName.getRevision(), oldName.getPrefix(),
                oldName.getLocalName());
        final TypeDefinitionBuilder tdb = new TypedefBuilder(newName,
                old.getLine());

        tdb.setRanges(old.getRanges());
        tdb.setLengths(old.getLengths());
        tdb.setPatterns(old.getPatterns());
        tdb.setFractionDigits(old.getFractionDigits());
        tdb.setPath(old.getPath());

        final TypeDefinition<?> oldType = old.getType();
        if (oldType == null) {
            tdb.setType(old.getTypedef());
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
                    union.setType(resolvedType);
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
            final TypeDefinitionBuilder utBuilder = getTypedefBuilder(copy,
                    modules, dependentModule);
            copy.setType(utBuilder);
            return copy;
        } else if (base == null && baseTdb != null) {
            // make a copy of baseTypeDef and call again
            final TypeDefinitionBuilder baseTdbCopy = copyTypedefBuilder(
                    baseTdb, true);
            final TypeDefinitionBuilder baseTdbCopyResolved = resolveCopiedBuilder(
                    baseTdbCopy, modules, builder);
            copy.setType(baseTdbCopyResolved);
            return copy;
        } else {
            throw new IllegalStateException("Failed to resolve type "
                    + copy.getQName().getLocalName());
        }
    }

    private TypeDefinitionBuilder findTypedefBuilder(
            final QName unknownTypeQName,
            final Map<String, TreeMap<Date, ModuleBuilder>> modules,
            final ModuleBuilder builder, int line) {
        // search for module which contains referenced typedef
        final ModuleBuilder dependentModule = findDependentModule(modules,
                builder, unknownTypeQName.getPrefix(), line);
        final TypeDefinitionBuilder lookedUpBuilder = findTypedefBuilderByName(
                dependentModule, unknownTypeQName.getLocalName(),
                builder.getName(), line);
        return copyTypedefBuilder(lookedUpBuilder, true);
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
                    findTypedefBuilder(ext.getQName(), modules, builder,
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
            final TypeDefinitionBuilder utBuilder = findTypedefBuilder(
                    unknown.getQName(), modules, builder,
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
     * @param dependentModule
     *            module to search
     * @param name
     *            name of type definition
     * @param currentModuleName
     *            current module name
     * @param line
     *            current line in yang model
     * @return
     */
    private TypeDefinitionBuilder findTypedefBuilderByName(
            final ModuleBuilder dependentModule, final String name,
            final String currentModuleName, final int line) {
        final Set<TypeDefinitionBuilder> typedefs = dependentModule
                .getModuleTypedefs();
        for (TypeDefinitionBuilder td : typedefs) {
            if (td.getQName().getLocalName().equals(name)) {
                return td;
            }
        }
        throw new YangParseException(currentModuleName, line, "Target module '"
                + dependentModule.getName() + "' does not contain typedef '"
                + name + "'.");
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
            while (!(module.getAugmentsResolved() == module.getAddedAugments()
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
        if (module.getAugmentsResolved() < module.getAddedAugments().size()) {
            for (AugmentationSchemaBuilder augmentBuilder : module
                    .getAddedAugments()) {

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

                    for (int i = 1; i < path.size(); i++) {
                        final QName currentQName = path.get(i);
                        DataSchemaNodeBuilder newParent = null;
                        for (DataSchemaNodeBuilder child : ((ChildNodeBuilder) currentParent)
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
                        ParserUtils.fillAugmentTarget(augmentBuilder,
                                (ChildNodeBuilder) currentParent);
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
                .getAddedIdentities();
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
                        .getAddedIdentities();
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
    private void resolveUses(
            final Map<String, TreeMap<Date, ModuleBuilder>> modules,
            final ModuleBuilder module) {
        final Map<List<String>, UsesNodeBuilder> moduleUses = module
                .getAddedUsesNodes();
        for (Map.Entry<List<String>, UsesNodeBuilder> entry : moduleUses
                .entrySet()) {
            final List<String> key = entry.getKey();
            final UsesNodeBuilder usesNode = entry.getValue();
            final int line = usesNode.getLine();

            final String groupingName = key.get(key.size() - 1);

            for (RefineHolder refine : usesNode.getRefines()) {
                Builder refineTarget = getRefineNodeBuilderCopy(groupingName,
                        refine, modules, module);
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
                } else if(refineTarget instanceof GroupingBuilder) {
                    usesNode.addRefineNode((GroupingBuilder)refineTarget);
                } else if(refineTarget instanceof TypedefBuilder) {
                    usesNode.addRefineNode((TypedefBuilder)refineTarget);
                }
            }
        }
    }

    /**
     * Find original builder of node to refine and return copy of this builder.
     * <p>
     * We must make a copy of builder to preserve original builder, because this
     * object will be refined (modified) and later added to
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
    private Builder getRefineNodeBuilderCopy(final String groupingPath,
            final RefineHolder refine,
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
                    .copyTypedefBuilder((TypedefBuilder) lookedUpBuilder);
        } else {
            throw new YangParseException(module.getName(), refine.getLine(),
                    "Target '" + refine.getName() + "' can not be refined");
        }
        return result;
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
        builderPath.add(0, "grouping");
        builderPath.add(0, dependentModule.getName());
        final GroupingBuilder builder = (GroupingBuilder) dependentModule
                .getNode(builderPath);

        Builder result = builder.getChildNode(refineNodeName);
        if(result == null) {
            Set<GroupingBuilder> grps = builder.getGroupings();
            for(GroupingBuilder gr : grps) {
                if(gr.getQName().getLocalName().equals(refineNodeName)) {
                    result = gr;
                    break;
                }
            }
        }
        if(result == null) {
            Set<TypeDefinitionBuilder> typedefs = builder.getTypedefs();
            for(TypeDefinitionBuilder typedef : typedefs) {
                if(typedef.getQName().getLocalName().equals(refineNodeName)) {
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
        for (UnknownSchemaNodeBuilder usnb : module.getAddedUnknownNodes()) {
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
