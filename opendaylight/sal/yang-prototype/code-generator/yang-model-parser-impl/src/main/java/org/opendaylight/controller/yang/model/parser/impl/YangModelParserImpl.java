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
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.opendaylight.controller.antlrv4.code.gen.YangLexer;
import org.opendaylight.controller.antlrv4.code.gen.YangParser;
import org.opendaylight.controller.yang.common.QName;
import org.opendaylight.controller.yang.model.api.AugmentationSchema;
import org.opendaylight.controller.yang.model.api.DataSchemaNode;
import org.opendaylight.controller.yang.model.api.ExtensionDefinition;
import org.opendaylight.controller.yang.model.api.Module;
import org.opendaylight.controller.yang.model.api.ModuleImport;
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
import org.opendaylight.controller.yang.model.parser.builder.api.ChildNodeBuilder;
import org.opendaylight.controller.yang.model.parser.builder.api.DataSchemaNodeBuilder;
import org.opendaylight.controller.yang.model.parser.builder.api.TypeAwareBuilder;
import org.opendaylight.controller.yang.model.parser.builder.api.TypeDefinitionBuilder;
import org.opendaylight.controller.yang.model.parser.builder.impl.IdentitySchemaNodeBuilder;
import org.opendaylight.controller.yang.model.parser.builder.impl.ModuleBuilder;
import org.opendaylight.controller.yang.model.parser.builder.impl.TypedefBuilder;
import org.opendaylight.controller.yang.model.parser.builder.impl.UnionTypeBuilder;
import org.opendaylight.controller.yang.model.parser.util.TypeConstraints;
import org.opendaylight.controller.yang.model.parser.util.YangParseException;
import org.opendaylight.controller.yang.model.util.ExtendedType;
import org.opendaylight.controller.yang.model.util.UnknownType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class YangModelParserImpl implements YangModelParser {

    private static final Logger logger = LoggerFactory
            .getLogger(YangModelParserImpl.class);

    @Override
    public Module parseYangModel(final String yangFile) {
        final Map<String, TreeMap<Date, ModuleBuilder>> modules = resolveModuleBuildersFromStreams(yangFile);
        final Set<Module> result = build(modules);
        return result.iterator().next();
    }

    @Override
    public Set<Module> parseYangModels(final String... yangFiles) {
        final Map<String, TreeMap<Date, ModuleBuilder>> modules = resolveModuleBuildersFromStreams(yangFiles);
        return build(modules);
    }

    @Override
    public Set<Module> parseYangModelsFromStreams(
            final InputStream... yangModelStreams) {
        final Map<String, TreeMap<Date, ModuleBuilder>> modules = resolveModuleBuildersFromStreams(yangModelStreams);
        return build(modules);
    }

    @Override
    public SchemaContext resolveSchemaContext(final Set<Module> modules) {
        return new SchemaContextImpl(modules);
    }

    private Map<String, TreeMap<Date, ModuleBuilder>> resolveModuleBuildersFromStreams(
            String... yangFiles) {
        final InputStream[] streams = loadStreams(yangFiles);

        if (streams != null) {
            final Map<String, TreeMap<Date, ModuleBuilder>> result = resolveModuleBuildersFromStreams(streams);
            cloaseStreams(streams);

            if (result != null) {
                return result;
            }
        }
        return new HashMap<String, TreeMap<Date, ModuleBuilder>>();
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

    private void cloaseStreams(final InputStream[] streams) {
        if (streams != null) {
            for (int i = 0; i < streams.length; ++i) {
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

    private Map<String, TreeMap<Date, ModuleBuilder>> resolveModuleBuildersFromStreams(
            InputStream... yangFiles) {
        final Map<String, TreeMap<Date, ModuleBuilder>> modules = new HashMap<String, TreeMap<Date, ModuleBuilder>>();
        final ParseTreeWalker walker = new ParseTreeWalker();
        final List<ParseTree> trees = parseStreams(yangFiles);
        final ModuleBuilder[] builders = new ModuleBuilder[trees.size()];

	// validation
        // if validation fails with any file, do not continue and throw
        // exception
        for (int i = 0; i < trees.size(); i++) {
            try {
                final YangModelValidationListener yangModelParser = new YangModelValidationListener();
                walker.walk(yangModelParser, trees.get(i));
            } catch (IllegalStateException e) {
                // wrap exception to add information about which file failed
                throw new YangValidationException(
                        "Yang validation failed for file" + yangFiles[i], e);
            }
        }


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
                builderRevision = createEpochTime();
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

    private List<ParseTree> parseStreams(InputStream... yangStreams) {
        final List<ParseTree> trees = new ArrayList<ParseTree>();
        for (InputStream yangStream : yangStreams) {
            trees.add(parseStream(yangStream));
        }
        return trees;
    }

    private ParseTree parseStream(InputStream yangStream) {
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

    private Set<Module> build(Map<String, TreeMap<Date, ModuleBuilder>> modules) {
        // validate
        for (Map.Entry<String, TreeMap<Date, ModuleBuilder>> entry : modules
                .entrySet()) {
            for (Map.Entry<Date, ModuleBuilder> childEntry : entry.getValue()
                    .entrySet()) {
                ModuleBuilder moduleBuilder = childEntry.getValue();
                validateModule(modules, moduleBuilder);
            }
        }

        // build
        final Set<Module> result = new HashSet<Module>();
        for (Map.Entry<String, TreeMap<Date, ModuleBuilder>> entry : modules
                .entrySet()) {
            final Map<Date, Module> modulesByRevision = new HashMap<Date, Module>();
            for (Map.Entry<Date, ModuleBuilder> childEntry : entry.getValue()
                    .entrySet()) {
                ModuleBuilder moduleBuilder = childEntry.getValue();
                Module module = moduleBuilder.build();
                modulesByRevision.put(childEntry.getKey(), module);
                result.add(module);
            }
        }
        return result;
    }

    private void validateModule(
            Map<String, TreeMap<Date, ModuleBuilder>> modules,
            ModuleBuilder builder) {
        resolveDirtyNodes(modules, builder);
        resolveAugments(modules, builder);
        resolveIdentities(modules, builder);
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
            Map<String, TreeMap<Date, ModuleBuilder>> modules,
            ModuleBuilder module) {
        final Map<List<String>, TypeAwareBuilder> dirtyNodes = module
                .getDirtyNodes();
        if (!dirtyNodes.isEmpty()) {
            for (Map.Entry<List<String>, TypeAwareBuilder> entry : dirtyNodes
                    .entrySet()) {

                TypeAwareBuilder typeToResolve = entry.getValue();
                if (typeToResolve instanceof UnionTypeBuilder) {
                    UnionTypeBuilder union = (UnionTypeBuilder) typeToResolve;
                    List<TypeDefinition<?>> unionTypes = union.getTypes();
                    List<UnknownType> toRemove = new ArrayList<UnknownType>();
                    for (TypeDefinition<?> td : unionTypes) {
                        if (td instanceof UnknownType) {
                            UnknownType unknownType = (UnknownType) td;
                            TypeDefinitionBuilder resolvedType = findTargetTypeUnion(
                                    typeToResolve, unknownType, modules, module);
                            union.setType(resolvedType);
                            toRemove.add(unknownType);
                        }
                    }
                    unionTypes.removeAll(toRemove);
                } else {
                    TypeDefinitionBuilder resolvedType = findTargetType(
                            typeToResolve, modules, module);
                    typeToResolve.setType(resolvedType);
                }
            }
        }
    }

    private TypeDefinitionBuilder findTargetType(
            TypeAwareBuilder typeToResolve,
            Map<String, TreeMap<Date, ModuleBuilder>> modules,
            ModuleBuilder builder) {
        TypeConstraints constraints = new TypeConstraints();

        TypeDefinitionBuilder targetType = findTypedef(typeToResolve, modules,
                builder);
        TypeConstraints tConstraints = findConstraints(typeToResolve,
                constraints, modules, builder);
        targetType.setRanges(tConstraints.getRange());
        targetType.setLengths(tConstraints.getLength());
        targetType.setPatterns(tConstraints.getPatterns());
        targetType.setFractionDigits(tConstraints.getFractionDigits());

        return targetType;
    }

    private TypeDefinitionBuilder findTargetTypeUnion(
            TypeAwareBuilder typeToResolve, UnknownType unknownType,
            Map<String, TreeMap<Date, ModuleBuilder>> modules,
            ModuleBuilder builder) {
        TypeConstraints constraints = new TypeConstraints();

        TypeDefinitionBuilder targetType = findTypedefUnion(typeToResolve,
                unknownType, modules, builder);
        TypeConstraints tConstraints = findConstraints(typeToResolve,
                constraints, modules, builder);
        targetType.setRanges(tConstraints.getRange());
        targetType.setLengths(tConstraints.getLength());
        targetType.setPatterns(tConstraints.getPatterns());
        targetType.setFractionDigits(tConstraints.getFractionDigits());

        return targetType;
    }

    private TypeDefinitionBuilder findTypedef(TypeAwareBuilder typeToResolve,
            Map<String, TreeMap<Date, ModuleBuilder>> modules,
            ModuleBuilder builder) {

        TypeDefinition<?> baseTypeToResolve = typeToResolve.getType();
        if (baseTypeToResolve != null
                && !(baseTypeToResolve instanceof UnknownType)) {
            return (TypeDefinitionBuilder) typeToResolve;
        }

        UnknownType unknownType = (UnknownType) typeToResolve.getType();

        QName unknownTypeQName = unknownType.getQName();
        String unknownTypeName = unknownTypeQName.getLocalName();
        String unknownTypePrefix = unknownTypeQName.getPrefix();

        // search for module which contains referenced typedef
        ModuleBuilder dependentModule = findDependentModule(modules, builder,
                unknownTypePrefix);
        TypeDefinitionBuilder lookedUpBuilder = findTypedefBuilder(
                dependentModule.getModuleTypedefs(), unknownTypeName);

        TypeDefinitionBuilder lookedUpBuilderCopy = copyTypedefBuilder(
                lookedUpBuilder, typeToResolve instanceof TypeDefinitionBuilder);
        TypeDefinitionBuilder resolvedCopy = resolveCopiedBuilder(
                lookedUpBuilderCopy, modules, dependentModule);
        return resolvedCopy;
    }

    private TypeDefinitionBuilder findTypedefUnion(
            TypeAwareBuilder typeToResolve, UnknownType unknownType,
            Map<String, TreeMap<Date, ModuleBuilder>> modules,
            ModuleBuilder builder) {

        TypeDefinition<?> baseTypeToResolve = typeToResolve.getType();
        if (baseTypeToResolve != null
                && !(baseTypeToResolve instanceof UnknownType)) {
            return (TypeDefinitionBuilder) typeToResolve;
        }

        QName unknownTypeQName = unknownType.getQName();
        String unknownTypeName = unknownTypeQName.getLocalName();
        String unknownTypePrefix = unknownTypeQName.getPrefix();

        // search for module which contains referenced typedef
        ModuleBuilder dependentModule = findDependentModule(modules, builder,
                unknownTypePrefix);
        TypeDefinitionBuilder lookedUpBuilder = findTypedefBuilder(
                dependentModule.getModuleTypedefs(), unknownTypeName);

        TypeDefinitionBuilder lookedUpBuilderCopy = copyTypedefBuilder(
                lookedUpBuilder, typeToResolve instanceof TypeDefinitionBuilder);
        TypeDefinitionBuilder resolvedCopy = resolveCopiedBuilder(
                lookedUpBuilderCopy, modules, dependentModule);
        return resolvedCopy;
    }

    private TypeDefinitionBuilder copyTypedefBuilder(TypeDefinitionBuilder old,
            boolean seekByTypedefBuilder) {
        if (old instanceof UnionTypeBuilder) {
            UnionTypeBuilder oldUnion = (UnionTypeBuilder) old;
            UnionTypeBuilder newUnion = new UnionTypeBuilder();
            for (TypeDefinition<?> td : oldUnion.getTypes()) {
                newUnion.setType(td);
            }
            for (TypeDefinitionBuilder tdb : oldUnion.getTypedefs()) {
                newUnion.setType(copyTypedefBuilder(tdb, true));
            }
            return newUnion;
        }

        QName oldQName = old.getQName();
        QName newQName = new QName(oldQName.getNamespace(),
                oldQName.getRevision(), oldQName.getPrefix(),
                oldQName.getLocalName());
        TypeDefinitionBuilder tdb = new TypedefBuilder(newQName);

        tdb.setRanges(old.getRanges());
        tdb.setLengths(old.getLengths());
        tdb.setPatterns(old.getPatterns());

        TypeDefinition<?> oldType = old.getType();
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
            TypeDefinitionBuilder copied,
            Map<String, TreeMap<Date, ModuleBuilder>> modules,
            ModuleBuilder builder) {

        if (copied instanceof UnionTypeBuilder) {
            UnionTypeBuilder union = (UnionTypeBuilder) copied;
            List<TypeDefinition<?>> unionTypes = union.getTypes();
            List<UnknownType> toRemove = new ArrayList<UnknownType>();
            for (TypeDefinition<?> td : unionTypes) {
                if (td instanceof UnknownType) {
                    UnknownType unknownType = (UnknownType) td;
                    TypeDefinitionBuilder resolvedType = findTargetTypeUnion(
                            union, unknownType, modules, builder);
                    union.setType(resolvedType);
                    toRemove.add(unknownType);
                }
            }
            unionTypes.removeAll(toRemove);

            return union;
        }

        TypeDefinition<?> base = copied.getType();
        TypeDefinitionBuilder baseTdb = copied.getTypedef();
        if (base != null && !(base instanceof UnknownType)) {
            return copied;
        } else if (base instanceof UnknownType) {
            UnknownType unknownType = (UnknownType) base;
            QName unknownTypeQName = unknownType.getQName();
            String unknownTypePrefix = unknownTypeQName.getPrefix();
            ModuleBuilder dependentModule = findDependentModule(modules,
                    builder, unknownTypePrefix);
            TypeDefinitionBuilder unknownTypeBuilder = findTypedef(copied,
                    modules, dependentModule);
            copied.setType(unknownTypeBuilder);
            return copied;
        } else if (base == null && baseTdb != null) {
            // make a copy of baseTypeDef and call again
            TypeDefinitionBuilder baseTdbCopy = copyTypedefBuilder(baseTdb,
                    true);
            TypeDefinitionBuilder baseTdbCopyResolved = resolveCopiedBuilder(
                    baseTdbCopy, modules, builder);
            copied.setType(baseTdbCopyResolved);
            return copied;
        } else {
            throw new IllegalStateException(
                    "TypeDefinitionBuilder in unexpected state");
        }
    }

    private TypeDefinitionBuilder findTypedef(QName unknownTypeQName,
            Map<String, TreeMap<Date, ModuleBuilder>> modules,
            ModuleBuilder builder) {

        String unknownTypeName = unknownTypeQName.getLocalName();
        String unknownTypePrefix = unknownTypeQName.getPrefix();

        // search for module which contains referenced typedef
        ModuleBuilder dependentModule = findDependentModule(modules, builder,
                unknownTypePrefix);

        TypeDefinitionBuilder lookedUpBuilder = findTypedefBuilder(
                dependentModule.getModuleTypedefs(), unknownTypeName);

        TypeDefinitionBuilder copied = copyTypedefBuilder(lookedUpBuilder, true);
        return copied;
    }

    private TypeConstraints findConstraints(TypeAwareBuilder typeToResolve,
            TypeConstraints constraints,
            Map<String, TreeMap<Date, ModuleBuilder>> modules,
            ModuleBuilder builder) {

        // union type cannot be restricted
        if (typeToResolve instanceof UnionTypeBuilder) {
            return constraints;
        }

        // if referenced type is UnknownType again, search recursively with
        // current constraints
        TypeDefinition<?> referencedType = typeToResolve.getType();
        if (referencedType == null) {
            TypeDefinitionBuilder tdb = (TypeDefinitionBuilder) typeToResolve;
            final List<RangeConstraint> ranges = tdb.getRanges();
            constraints.addRanges(ranges);
            final List<LengthConstraint> lengths = tdb.getLengths();
            constraints.addLengths(lengths);
            final List<PatternConstraint> patterns = tdb.getPatterns();
            constraints.addPatterns(patterns);
            final Integer fractionDigits = tdb.getFractionDigits();
            constraints.setFractionDigits(fractionDigits);
            return constraints;
        } else if (referencedType instanceof ExtendedType) {
            ExtendedType ext = (ExtendedType) referencedType;
            final List<RangeConstraint> ranges = ext.getRanges();
            constraints.addRanges(ranges);
            final List<LengthConstraint> lengths = ext.getLengths();
            constraints.addLengths(lengths);
            final List<PatternConstraint> patterns = ext.getPatterns();
            constraints.addPatterns(patterns);
            final Integer fractionDigits = ext.getFractionDigits();
            constraints.setFractionDigits(fractionDigits);
            return findConstraints(
                    findTypedef(ext.getQName(), modules, builder), constraints,
                    modules, builder);
        } else if (referencedType instanceof UnknownType) {
            UnknownType unknown = (UnknownType) referencedType;

            final List<RangeConstraint> ranges = unknown.getRangeStatements();
            constraints.addRanges(ranges);
            final List<LengthConstraint> lengths = unknown
                    .getLengthStatements();
            constraints.addLengths(lengths);
            final List<PatternConstraint> patterns = unknown.getPatterns();
            constraints.addPatterns(patterns);
            final Integer fractionDigits = unknown.getFractionDigits();
            constraints.setFractionDigits(fractionDigits);

            String unknownTypePrefix = unknown.getQName().getPrefix();
            if (unknownTypePrefix == null || "".equals(unknownTypePrefix)) {
                unknownTypePrefix = builder.getPrefix();
            }
            ModuleBuilder dependentModule = findDependentModule(modules,
                    builder, unknown.getQName().getPrefix());
            TypeDefinitionBuilder unknownTypeBuilder = findTypedef(
                    unknown.getQName(), modules, builder);
            return findConstraints(unknownTypeBuilder, constraints, modules,
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
    private TypeDefinitionBuilder findTypedefBuilder(
            Set<TypeDefinitionBuilder> typedefs, String name) {
        TypeDefinitionBuilder result = null;
        for (TypeDefinitionBuilder td : typedefs) {
            if (td.getQName().getLocalName().equals(name)) {
                result = td;
                break;
            }
        }
        if (result == null) {
            throw new YangParseException(
                    "Target module does not contain typedef '" + name + "'.");
        }
        return result;
    }

    /**
     * Pull restriction from referenced type and add them to given constraints
     * 
     * @param referencedType
     * @param constraints
     */
    private void mergeConstraints(TypeDefinition<?> referencedType,
            TypeConstraints constraints) {

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
     * Go through all augmentation definitions and resolve them. This means find
     * referenced node and add child nodes to it.
     * 
     * @param modules
     *            all available modules
     * @param module
     *            current module
     */
    private void resolveAugments(
            Map<String, TreeMap<Date, ModuleBuilder>> modules,
            ModuleBuilder module) {
        Set<AugmentationSchemaBuilder> augmentBuilders = module
                .getAddedAugments();

        Set<AugmentationSchema> augments = new HashSet<AugmentationSchema>();
        for (AugmentationSchemaBuilder augmentBuilder : augmentBuilders) {
            SchemaPath augmentTargetSchemaPath = augmentBuilder.getTargetPath();
            String prefix = null;
            List<String> augmentTargetPath = new ArrayList<String>();

            for (QName pathPart : augmentTargetSchemaPath.getPath()) {
                prefix = pathPart.getPrefix();
                augmentTargetPath.add(pathPart.getLocalName());
            }
            ModuleBuilder dependentModule = findDependentModule(modules,
                    module, prefix);
            augmentTargetPath.add(0, dependentModule.getName());

            AugmentationTargetBuilder augmentTarget = (AugmentationTargetBuilder) dependentModule
                    .getNode(augmentTargetPath);
            AugmentationSchema result = augmentBuilder.build();
            augmentTarget.addAugmentation(result);
            fillAugmentTarget(augmentBuilder, (ChildNodeBuilder) augmentTarget);
            augments.add(result);
        }
        module.setAugmentations(augments);
    }

    /**
     * Add all augment's child nodes to given target.
     * 
     * @param augment
     * @param target
     */
    private void fillAugmentTarget(AugmentationSchemaBuilder augment,
            ChildNodeBuilder target) {
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
            Map<String, TreeMap<Date, ModuleBuilder>> modules,
            ModuleBuilder module) {
        Set<IdentitySchemaNodeBuilder> identities = module.getAddedIdentities();
        for (IdentitySchemaNodeBuilder identity : identities) {
            String baseIdentityName = identity.getBaseIdentityName();
            if (baseIdentityName != null) {
                String baseIdentityPrefix = null;
                String baseIdentityLocalName = null;
                if (baseIdentityName.contains(":")) {
                    String[] splitted = baseIdentityName.split(":");
                    baseIdentityPrefix = splitted[0];
                    baseIdentityLocalName = splitted[1];
                } else {
                    baseIdentityPrefix = module.getPrefix();
                    baseIdentityLocalName = baseIdentityName;
                }
                ModuleBuilder dependentModule = findDependentModule(modules,
                        module, baseIdentityPrefix);

                Set<IdentitySchemaNodeBuilder> dependentModuleIdentities = dependentModule
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
            Map<String, TreeMap<Date, ModuleBuilder>> modules,
            ModuleBuilder module, String prefix) {
        ModuleBuilder dependentModule = null;
        Date dependentModuleRevision = null;

        if (prefix.equals(module.getPrefix())) {
            dependentModule = module;
        } else {
            ModuleImport dependentModuleImport = getModuleImport(module, prefix);
            if (dependentModuleImport == null) {
                throw new YangParseException("No import found with prefix '"
                        + prefix + "' in module " + module.getName() + "'.");
            }
            String dependentModuleName = dependentModuleImport.getModuleName();
            dependentModuleRevision = dependentModuleImport.getRevision();

            TreeMap<Date, ModuleBuilder> moduleBuildersByRevision = modules
                    .get(dependentModuleName);
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
    private ModuleImport getModuleImport(ModuleBuilder builder, String prefix) {
        ModuleImport moduleImport = null;
        for (ModuleImport mi : builder.getModuleImports()) {
            if (mi.getPrefix().equals(prefix)) {
                moduleImport = mi;
                break;
            }
        }
        return moduleImport;
    }

    private Date createEpochTime() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(0);
        return calendar.getTime();
    }

    private static class SchemaContextImpl implements SchemaContext {
        private final Set<Module> modules;

        private SchemaContextImpl(Set<Module> modules) {
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
        public Module findModuleByNamespace(URI namespace) {
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
