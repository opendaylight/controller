/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.yang.model.parser.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
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
import org.opendaylight.controller.yang.model.api.DataSchemaNode;
import org.opendaylight.controller.yang.model.api.ExtensionDefinition;
import org.opendaylight.controller.yang.model.api.Module;
import org.opendaylight.controller.yang.model.api.ModuleImport;
import org.opendaylight.controller.yang.model.api.MustDefinition;
import org.opendaylight.controller.yang.model.api.NotificationDefinition;
import org.opendaylight.controller.yang.model.api.RpcDefinition;
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
import org.opendaylight.controller.yang.model.parser.builder.api.AugmentationSchemaBuilder;
import org.opendaylight.controller.yang.model.parser.builder.api.AugmentationTargetBuilder;
import org.opendaylight.controller.yang.model.parser.builder.api.Builder;
import org.opendaylight.controller.yang.model.parser.builder.api.ChildNodeBuilder;
import org.opendaylight.controller.yang.model.parser.builder.api.DataSchemaNodeBuilder;
import org.opendaylight.controller.yang.model.parser.builder.api.GroupingBuilder;
import org.opendaylight.controller.yang.model.parser.builder.api.TypeAwareBuilder;
import org.opendaylight.controller.yang.model.parser.builder.api.TypeDefinitionBuilder;
import org.opendaylight.controller.yang.model.parser.builder.api.UsesNodeBuilder;
import org.opendaylight.controller.yang.model.parser.builder.impl.AnyXmlBuilder;
import org.opendaylight.controller.yang.model.parser.builder.impl.ChoiceBuilder;
import org.opendaylight.controller.yang.model.parser.builder.impl.ContainerSchemaNodeBuilder;
import org.opendaylight.controller.yang.model.parser.builder.impl.IdentitySchemaNodeBuilder;
import org.opendaylight.controller.yang.model.parser.builder.impl.IdentityrefTypeBuilder;
import org.opendaylight.controller.yang.model.parser.builder.impl.LeafListSchemaNodeBuilder;
import org.opendaylight.controller.yang.model.parser.builder.impl.LeafSchemaNodeBuilder;
import org.opendaylight.controller.yang.model.parser.builder.impl.ListSchemaNodeBuilder;
import org.opendaylight.controller.yang.model.parser.builder.impl.ModuleBuilder;
import org.opendaylight.controller.yang.model.parser.builder.impl.TypedefBuilder;
import org.opendaylight.controller.yang.model.parser.builder.impl.UnionTypeBuilder;
import org.opendaylight.controller.yang.model.parser.util.ParserUtils;
import org.opendaylight.controller.yang.model.parser.util.RefineHolder;
import org.opendaylight.controller.yang.model.parser.util.RefineHolder.Refine;
import org.opendaylight.controller.yang.model.parser.util.TypeConstraints;
import org.opendaylight.controller.yang.model.parser.util.YangParseException;
import org.opendaylight.controller.yang.model.util.ExtendedType;
import org.opendaylight.controller.yang.model.util.IdentityrefType;
import org.opendaylight.controller.yang.model.util.UnknownType;
import org.opendaylight.controller.yang.model.validator.YangModelBasicValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class YangModelParserImpl implements YangModelParser {

    private static final Logger logger = LoggerFactory
            .getLogger(YangModelParserImpl.class);

    @Override
    public Module parseYangModel(final String yangFile) {
        final Map<String, TreeMap<Date, ModuleBuilder>> modules = resolveModuleBuilders(yangFile);
        final Set<Module> result = build(modules);
        return result.iterator().next();
    }

    @Override
    public Set<Module> parseYangModels(final String... yangFiles) {
        final Map<String, TreeMap<Date, ModuleBuilder>> modules = resolveModuleBuilders(yangFiles);
        return build(modules);
    }

    @Override
    public Set<Module> parseYangModelsFromStreams(
            final InputStream... yangModelStreams) {
        final Map<String, TreeMap<Date, ModuleBuilder>> modules = resolveModuleBuilders(yangModelStreams);
        return build(modules);
    }

    @Override
    public SchemaContext resolveSchemaContext(final Set<Module> modules) {
        return new SchemaContextImpl(modules);
    }

    private Map<String, TreeMap<Date, ModuleBuilder>> resolveModuleBuilders(
            final String... yangFiles) {
        final InputStream[] streams = loadStreams(yangFiles);
        Map<String, TreeMap<Date, ModuleBuilder>> result = Collections
                .emptyMap();

        if (streams != null) {
            result = resolveModuleBuilders(streams);
            closeStreams(streams);
        }
        return result;
    }

    private InputStream[] loadStreams(final String... yangFiles) {
        final InputStream[] streams = new InputStream[yangFiles.length];
        for (int i = 0; i < yangFiles.length; i++) {
            final String yangFileName = yangFiles[i];
            final File yangFile = new File(yangFileName);
            try {
                streams[i] = new FileInputStream(yangFile);
            } catch (FileNotFoundException e) {
                logger.warn("Exception while reading yang stream: "
                        + streams[i], e);
            }
        }
        return streams;
    }

    private void closeStreams(final InputStream[] streams) {
        if (streams != null) {
            for (int i = 0; i < streams.length; i++) {
                try {
                    if (streams[i] != null) {
                        streams[i].close();
                    }
                } catch (IOException e) {
                    logger.warn("Exception while closing yang stream: "
                            + streams[i], e);
                }
            }
        }
    }

    private Map<String, TreeMap<Date, ModuleBuilder>> resolveModuleBuilders(
            final InputStream... yangFiles) {
        final Map<String, TreeMap<Date, ModuleBuilder>> modules = new HashMap<String, TreeMap<Date, ModuleBuilder>>();
        final ParseTreeWalker walker = new ParseTreeWalker();
        final List<ParseTree> trees = parseStreams(yangFiles);
        final ModuleBuilder[] builders = new ModuleBuilder[trees.size()];

        // validate yang
        new YangModelBasicValidator(walker).validate(trees);

        YangModelParserListenerImpl yangModelParser = null;
        for (int i = 0; i < trees.size(); i++) {
            yangModelParser = new YangModelParserListenerImpl();
            walker.walk(yangModelParser, trees.get(i));
            builders[i] = yangModelParser.getModuleBuilder();
        }

        for (ModuleBuilder builder : builders) {
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

    private List<ParseTree> parseStreams(final InputStream... yangStreams) {
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
        final Set<Module> result = new HashSet<Module>();
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
                } else if(nodeToResolve.getTypedef() instanceof IdentityrefTypeBuilder) {
                    IdentityrefTypeBuilder idref = (IdentityrefTypeBuilder)nodeToResolve.getTypedef();
                    nodeToResolve.setType(new IdentityrefType(findFullQName(modules, module, idref)));
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
                builder, unknownTypeQName.getPrefix());
        final TypeDefinitionBuilder lookedUpBuilder = findTypedefBuilderByName(
                dependentModule, unknownTypeQName.getLocalName());

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
                module, unknownTypeQName.getPrefix());
        final TypeDefinitionBuilder lookedUpBuilder = findTypedefBuilderByName(
                dependentModule, unknownTypeQName.getLocalName());

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
            final UnionTypeBuilder newUnion = new UnionTypeBuilder();
            for (TypeDefinition<?> td : oldUnion.getTypes()) {
                newUnion.setType(td);
            }
            for (TypeDefinitionBuilder tdb : oldUnion.getTypedefs()) {
                newUnion.setType(copyTypedefBuilder(tdb, true));
            }
            return newUnion;
        }

        final QName oldName = old.getQName();
        final QName newName = new QName(oldName.getNamespace(),
                oldName.getRevision(), oldName.getPrefix(),
                oldName.getLocalName());
        final TypeDefinitionBuilder tdb = new TypedefBuilder(newName);

        tdb.setRanges(old.getRanges());
        tdb.setLengths(old.getLengths());
        tdb.setPatterns(old.getPatterns());
        tdb.setFractionDigits(old.getFractionDigits());

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
                    builder, unknownTypePrefix);
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
            throw new IllegalStateException(
                    "TypeDefinitionBuilder in unexpected state");
        }
    }

    private TypeDefinitionBuilder findTypedefBuilder(
            final QName unknownTypeQName,
            final Map<String, TreeMap<Date, ModuleBuilder>> modules,
            final ModuleBuilder builder) {

        // search for module which contains referenced typedef
        final ModuleBuilder dependentModule = findDependentModule(modules,
                builder, unknownTypeQName.getPrefix());

        final TypeDefinitionBuilder lookedUpBuilder = findTypedefBuilderByName(
                dependentModule, unknownTypeQName.getLocalName());

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
            final TypeDefinitionBuilder tdb = (TypeDefinitionBuilder) nodeToResolve;
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
                    findTypedefBuilder(ext.getQName(), modules, builder),
                    constraints, modules, builder);
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
                    builder, unknown.getQName().getPrefix());
            final TypeDefinitionBuilder utBuilder = findTypedefBuilder(
                    unknown.getQName(), modules, builder);
            return findConstraints(utBuilder, constraints, modules,
                    dependentModule);
        } else {
            // HANDLE BASE YANG TYPE
            mergeConstraints(referencedType, constraints);
            return constraints;
        }

    }

    /**
     * Go through all typedef statements from given module and search for one
     * with given name
     *
     * @param typedefs
     *            typedef statements to search
     * @param name
     *            name of searched typedef
     * @return typedef with name equals to given name
     */
    private TypeDefinitionBuilder findTypedefBuilderByName(
            final ModuleBuilder dependentModule, final String name) {
        TypeDefinitionBuilder result = null;
        final Set<TypeDefinitionBuilder> typedefs = dependentModule
                .getModuleTypedefs();
        for (TypeDefinitionBuilder td : typedefs) {
            if (td.getQName().getLocalName().equals(name)) {
                result = td;
                break;
            }
        }
        if (result == null) {
            throw new YangParseException("Target module '"
                    + dependentModule.getName()
                    + "' does not contain typedef '" + name + "'.");
        }
        return result;
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
     * Go through all augmentation definitions and resolve them. This method
     * also finds referenced node and add child nodes to it.
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

    private void resolveAugment(
            final Map<String, TreeMap<Date, ModuleBuilder>> modules,
            final ModuleBuilder module) {
        if (module.getAugmentsResolved() < module.getAddedAugments().size()) {
            for (AugmentationSchemaBuilder augmentBuilder : module
                    .getAddedAugments()) {
                final SchemaPath augmentTargetSchemaPath = augmentBuilder
                        .getTargetPath();
                final List<QName> path = augmentTargetSchemaPath.getPath();

                int i = 0;
                final QName qname = path.get(i);
                String prefix = qname.getPrefix();
                if(prefix == null) {
                    prefix = module.getPrefix();
                }

                DataSchemaNodeBuilder currentParent = null;
                final ModuleBuilder dependentModule = findDependentModule(
                        modules, module, prefix);
                for (DataSchemaNodeBuilder child : dependentModule
                        .getChildNodes()) {
                    final QName childQName = child.getQName();
                    if (childQName.getLocalName().equals(qname.getLocalName())) {
                        currentParent = child;
                        i++;
                        break;
                    }
                }

                for (; i < path.size(); i++) {
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
                final QName lastAugmentPathElement = path.get(path.size() - 1);

                if (currentQName.getLocalName().equals(
                        lastAugmentPathElement.getLocalName())) {
                    fillAugmentTarget(augmentBuilder,
                            (ChildNodeBuilder) currentParent);
                    ((AugmentationTargetBuilder) currentParent)
                            .addAugmentation(augmentBuilder);
                    module.augmentResolved();
                }
            }
        }
    }

    /**
     * Add all augment's child nodes to given target.
     *
     * @param augment
     * @param target
     */
    private void fillAugmentTarget(final AugmentationSchemaBuilder augment,
            final ChildNodeBuilder target) {
        for (DataSchemaNodeBuilder builder : augment.getChildNodes()) {
            builder.setAugmenting(true);
            target.addChildNode(builder);
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
                        modules, module, baseIdentityPrefix);

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

            final String groupingName = key.get(key.size() - 1);

            final List<RefineHolder> refines = usesNode.getRefines();
            for (RefineHolder refine : refines) {
                final Refine refineType = refine.getType();
                // refine statements
                final String defaultStr = refine.getDefaultStr();
                final Boolean mandatory = refine.isMandatory();
                final MustDefinition must = refine.getMust();
                final Boolean presence = refine.isPresence();
                final Integer min = refine.getMinElements();
                final Integer max = refine.getMaxElements();

                switch (refineType) {
                case LEAF:
                    final LeafSchemaNodeBuilder leaf = (LeafSchemaNodeBuilder) getRefineTargetBuilder(
                            groupingName, refine, modules, module);
                    if (defaultStr != null && !("".equals(defaultStr))) {
                        leaf.setDefaultStr(defaultStr);
                    }
                    if (mandatory != null) {
                        leaf.getConstraints().setMandatory(mandatory);
                    }
                    if (must != null) {
                        leaf.getConstraints().addMustDefinition(must);
                    }
                    usesNode.addRefineNode(leaf);
                    break;
                case CONTAINER:
                    final ContainerSchemaNodeBuilder container = (ContainerSchemaNodeBuilder) getRefineTargetBuilder(
                            groupingName, refine, modules, module);
                    if (presence != null) {
                        container.setPresence(presence);
                    }
                    if (must != null) {
                        container.getConstraints().addMustDefinition(must);
                    }
                    usesNode.addRefineNode(container);
                    break;
                case LIST:
                    final ListSchemaNodeBuilder list = (ListSchemaNodeBuilder) getRefineTargetBuilder(
                            groupingName, refine, modules, module);
                    if (must != null) {
                        list.getConstraints().addMustDefinition(must);
                    }
                    if (min != null) {
                        list.getConstraints().setMinElements(min);
                    }
                    if (max != null) {
                        list.getConstraints().setMaxElements(max);
                    }
                    break;
                case LEAF_LIST:
                    final LeafListSchemaNodeBuilder leafList = (LeafListSchemaNodeBuilder) getRefineTargetBuilder(
                            groupingName, refine, modules, module);
                    if (must != null) {
                        leafList.getConstraints().addMustDefinition(must);
                    }
                    if (min != null) {
                        leafList.getConstraints().setMinElements(min);
                    }
                    if (max != null) {
                        leafList.getConstraints().setMaxElements(max);
                    }
                    break;
                case CHOICE:
                    final ChoiceBuilder choice = (ChoiceBuilder) getRefineTargetBuilder(
                            groupingName, refine, modules, module);
                    if (defaultStr != null) {
                        choice.setDefaultCase(defaultStr);
                    }
                    if (mandatory != null) {
                        choice.getConstraints().setMandatory(mandatory);
                    }
                    break;
                case ANYXML:
                    final AnyXmlBuilder anyXml = (AnyXmlBuilder) getRefineTargetBuilder(
                            groupingName, refine, modules, module);
                    if (mandatory != null) {
                        anyXml.getConstraints().setMandatory(mandatory);
                    }
                    if (must != null) {
                        anyXml.getConstraints().addMustDefinition(must);
                    }
                }
            }
        }

    }

    /**
     * Find original builder of refine node and return copy of this builder.
     *
     * @param groupingPath
     *            path to grouping which contains node to refine
     * @param refine
     *            refine object containing informations about refine
     * @param modules
     *            all loaded modules
     * @param module
     *            current module
     * @return copy of Builder object of node to be refined if it is present in
     *         grouping, null otherwise
     */
    private Builder getRefineTargetBuilder(final String groupingPath,
            final RefineHolder refine,
            final Map<String, TreeMap<Date, ModuleBuilder>> modules,
            final ModuleBuilder module) {
        Builder result = null;
        final Builder lookedUpBuilder = findRefineTargetBuilder(groupingPath,
                refine.getName(), modules, module);
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
        } else {
            throw new YangParseException("Target '" + refine.getName()
                    + "' can not be refined");
        }
        return result;
    }

    /**
     * Find builder of refine node.
     *
     * @param groupingPath
     *            path to grouping which contains node to refine
     * @param refineNodeName
     *            name of node to be refined
     * @param modules
     *            all loaded modules
     * @param module
     *            current module
     * @return Builder object of refine node if it is present in grouping, null
     *         otherwise
     */
    private Builder findRefineTargetBuilder(final String groupingPath,
            final String refineNodeName,
            final Map<String, TreeMap<Date, ModuleBuilder>> modules,
            final ModuleBuilder module) {
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
                module, prefix);
        builderPath.add(0, "grouping");
        builderPath.add(0, dependentModule.getName());
        final GroupingBuilder builder = (GroupingBuilder) dependentModule
                .getNode(builderPath);

        return builder.getChildNode(refineNodeName);
    }

    private QName findFullQName(final Map<String, TreeMap<Date, ModuleBuilder>> modules,
            final ModuleBuilder module, final IdentityrefTypeBuilder idref) {
        QName result = null;
        String baseString = idref.getBaseString();
        if(baseString.contains(":")) {
            String[] splittedBase = baseString.split(":");
            if(splittedBase.length > 2) {
                throw new YangParseException("Failed to parse identity base: "+ baseString);
            }
            String prefix = splittedBase[0];
            String name = splittedBase[1];
            ModuleBuilder dependentModule = findDependentModule(modules, module, prefix);
            result = new QName(dependentModule.getNamespace(), dependentModule.getRevision(), prefix, name);
        } else {
            result = new QName(module.getNamespace(), module.getRevision(), module.getPrefix(), baseString);
        }
        return result;
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
            final ModuleBuilder module, final String prefix) {
        ModuleBuilder dependentModule = null;
        Date dependentModuleRevision = null;

        if (prefix.equals(module.getPrefix())) {
            dependentModule = module;
        } else {
            final ModuleImport dependentModuleImport = getModuleImport(module,
                    prefix);
            if (dependentModuleImport == null) {
                throw new YangParseException("No import found with prefix '"
                        + prefix + "' in module " + module.getName() + "'.");
            }
            final String dependentModuleName = dependentModuleImport
                    .getModuleName();
            dependentModuleRevision = dependentModuleImport.getRevision();

            final TreeMap<Date, ModuleBuilder> moduleBuildersByRevision = modules
                    .get(dependentModuleName);
            if (moduleBuildersByRevision == null) {
                throw new YangParseException(
                        "Failed to find dependent module '"
                                + dependentModuleName + "' needed by module '"
                                + module.getName() + "'.");
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
            throw new YangParseException(
                    "Failed to find dependent module with prefix '" + prefix
                            + "' and revision '" + dependentModuleRevision
                            + "'.");
        }
        return dependentModule;
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
    private ModuleImport getModuleImport(final ModuleBuilder builder,
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

    private static class SchemaContextImpl implements SchemaContext {
        private final Set<Module> modules;

        private SchemaContextImpl(final Set<Module> modules) {
            this.modules = modules;
        }

        @Override
        public Set<DataSchemaNode> getDataDefinitions() {
            final Set<DataSchemaNode> dataDefs = new HashSet<DataSchemaNode>();
            for (Module m : modules) {
                dataDefs.addAll(m.getChildNodes());
            }
            return dataDefs;
        }

        @Override
        public Set<Module> getModules() {
            return modules;
        }

        @Override
        public Set<NotificationDefinition> getNotifications() {
            final Set<NotificationDefinition> notifications = new HashSet<NotificationDefinition>();
            for (Module m : modules) {
                notifications.addAll(m.getNotifications());
            }
            return notifications;
        }

        @Override
        public Set<RpcDefinition> getOperations() {
            final Set<RpcDefinition> rpcs = new HashSet<RpcDefinition>();
            for (Module m : modules) {
                rpcs.addAll(m.getRpcs());
            }
            return rpcs;
        }

        @Override
        public Set<ExtensionDefinition> getExtensions() {
            final Set<ExtensionDefinition> extensions = new HashSet<ExtensionDefinition>();
            for (Module m : modules) {
                extensions.addAll(m.getExtensionSchemaNodes());
            }
            return extensions;
        }

        @Override
        public Module findModuleByName(final String name, final Date revision) {
            if ((name != null) && (revision != null)) {
                for (final Module module : modules) {
                    if (module.getName().equals(name)
                            && module.getRevision().equals(revision)) {
                        return module;
                    }
                }
            }
            return null;
        }

        @Override
        public Module findModuleByNamespace(final URI namespace) {
            if (namespace != null) {
                for (final Module module : modules) {
                    if (module.getNamespace().equals(namespace)) {
                        return module;
                    }
                }
            }
            return null;
        }
    }

}
