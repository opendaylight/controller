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
import org.opendaylight.controller.yang.model.api.type.BitsTypeDefinition;
import org.opendaylight.controller.yang.model.api.type.BitsTypeDefinition.Bit;
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
import org.opendaylight.controller.yang.model.parser.builder.impl.UnionTypeBuilder;
import org.opendaylight.controller.yang.model.util.BaseConstraints;
import org.opendaylight.controller.yang.model.util.BinaryType;
import org.opendaylight.controller.yang.model.util.BitsType;
import org.opendaylight.controller.yang.model.util.StringType;
import org.opendaylight.controller.yang.model.util.UnknownType;
import org.opendaylight.controller.yang.model.util.YangTypesConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class YangModelParserImpl implements YangModelParser {

    private static final Logger logger = LoggerFactory
            .getLogger(YangModelParserImpl.class);

    @Override
    public Module parseYangModel(String yangFile) {
        final Map<String, TreeMap<Date, ModuleBuilder>> modules = resolveModuleBuildersFromStreams(yangFile);
        Set<Module> result = build(modules);
        return result.iterator().next();
    }

    @Override
    public Set<Module> parseYangModels(String... yangFiles) {
        final Map<String, TreeMap<Date, ModuleBuilder>> modules = resolveModuleBuildersFromStreams(yangFiles);
        Set<Module> result = build(modules);
        return result;
    }

    @Override
    public Set<Module> parseYangModelsFromStreams(
            InputStream... yangModelStreams) {
        final Map<String, TreeMap<Date, ModuleBuilder>> modules = resolveModuleBuildersFromStreams(yangModelStreams);
        Set<Module> result = build(modules);
        return result;
    }

    @Override
    public SchemaContext resolveSchemaContext(Set<Module> modules) {
        return new SchemaContextImpl(modules);
    }

    private Map<String, TreeMap<Date, ModuleBuilder>> resolveModuleBuildersFromStreams(
            String... yangFiles) {
        InputStream[] streams = new InputStream[yangFiles.length];
        for (int i = 0; i < yangFiles.length; i++) {
            final String yangFileName = yangFiles[i];
            final File yangFile = new File(yangFileName);
            FileInputStream inStream = null;
            try {
                inStream = new FileInputStream(yangFile);
            } catch (FileNotFoundException e) {
                logger.warn("Exception while reading yang stream: " + inStream,
                        e);
            }
            streams[i] = inStream;
        }
        return resolveModuleBuildersFromStreams(streams);
    }

    private Map<String, TreeMap<Date, ModuleBuilder>> resolveModuleBuildersFromStreams(
            InputStream... yangFiles) {
        final Map<String, TreeMap<Date, ModuleBuilder>> modules = new HashMap<String, TreeMap<Date, ModuleBuilder>>();
        final ParseTreeWalker walker = new ParseTreeWalker();
        final List<ParseTree> trees = parseStreams(yangFiles);
        final ModuleBuilder[] builders = new ModuleBuilder[trees.size()];

        for (int i = 0; i < trees.size(); i++) {
            final YangModelParserListenerImpl yangModelParser = new YangModelParserListenerImpl();
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
        List<ParseTree> trees = new ArrayList<ParseTree>();
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
        // first validate
        for (Map.Entry<String, TreeMap<Date, ModuleBuilder>> entry : modules
                .entrySet()) {
            for (Map.Entry<Date, ModuleBuilder> childEntry : entry.getValue()
                    .entrySet()) {
                ModuleBuilder moduleBuilder = childEntry.getValue();
                validateBuilder(modules, moduleBuilder);
            }
        }
        // then build
        final Set<Module> result = new HashSet<Module>();
        for (Map.Entry<String, TreeMap<Date, ModuleBuilder>> entry : modules
                .entrySet()) {
            final Map<Date, Module> modulesByRevision = new HashMap<Date, Module>();
            for (Map.Entry<Date, ModuleBuilder> childEntry : entry.getValue()
                    .entrySet()) {
                ModuleBuilder moduleBuilder = childEntry.getValue();
                modulesByRevision.put(childEntry.getKey(),
                        moduleBuilder.build());
                result.add(moduleBuilder.build());
            }
        }

        return result;
    }

    private void validateBuilder(
            Map<String, TreeMap<Date, ModuleBuilder>> modules,
            ModuleBuilder builder) {
        resolveTypedefs(modules, builder);
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
    private void resolveTypedefs(
            Map<String, TreeMap<Date, ModuleBuilder>> modules,
            ModuleBuilder module) {
        Map<List<String>, TypeAwareBuilder> dirtyNodes = module.getDirtyNodes();
        if (dirtyNodes.size() == 0) {
            return;
        } else {
            for (Map.Entry<List<String>, TypeAwareBuilder> entry : dirtyNodes
                    .entrySet()) {
                TypeAwareBuilder typeToResolve = entry.getValue();

                if (typeToResolve instanceof UnionTypeBuilder) {
                    resolveUnionTypeBuilder(modules, module,
                            (UnionTypeBuilder) typeToResolve);
                } else {
                    UnknownType ut = (UnknownType) typeToResolve.getType();
                    TypeDefinition<?> resolvedType = findTargetType(ut,
                            modules, module);
                    typeToResolve.setType(resolvedType);
                }
            }
        }
    }

    private UnionTypeBuilder resolveUnionTypeBuilder(
            Map<String, TreeMap<Date, ModuleBuilder>> modules,
            ModuleBuilder builder, UnionTypeBuilder unionTypeBuilderToResolve) {
        List<TypeDefinition<?>> resolvedTypes = new ArrayList<TypeDefinition<?>>();
        List<TypeDefinition<?>> typesToRemove = new ArrayList<TypeDefinition<?>>();

        for (TypeDefinition<?> td : unionTypeBuilderToResolve.getTypes()) {
            if (td instanceof UnknownType) {
                TypeDefinition<?> resolvedType = findTargetType(
                        (UnknownType) td, modules, builder);
                resolvedTypes.add(resolvedType);
                typesToRemove.add(td);
            }
        }

        List<TypeDefinition<?>> unionTypeBuilderTypes = unionTypeBuilderToResolve
                .getTypes();
        unionTypeBuilderTypes.addAll(resolvedTypes);
        unionTypeBuilderTypes.removeAll(typesToRemove);

        return unionTypeBuilderToResolve;
    }

    private TypeDefinition<?> findTargetType(UnknownType ut,
            Map<String, TreeMap<Date, ModuleBuilder>> modules,
            ModuleBuilder builder) {

        Map<TypeDefinitionBuilder, TypeConstraints> foundedTypeDefinitionBuilder = findTypeDefinitionBuilderWithConstraints(
                modules, ut, builder);
        TypeDefinitionBuilder targetType = foundedTypeDefinitionBuilder
                .entrySet().iterator().next().getKey();
        TypeConstraints constraints = foundedTypeDefinitionBuilder.entrySet()
                .iterator().next().getValue();

        TypeDefinition<?> targetTypeBaseType = targetType.getBaseType();

        // RANGE
        List<RangeConstraint> ranges = ut.getRangeStatements();
        resolveRanges(ranges, targetType, modules, builder);
        // LENGTH
        List<LengthConstraint> lengths = ut.getLengthStatements();
        resolveLengths(lengths, targetType, modules, builder);
        // PATTERN
        List<PatternConstraint> patterns = ut.getPatterns();
        // Fraction Digits
        Integer fractionDigits = ut.getFractionDigits();

        targetTypeBaseType = mergeConstraints(targetTypeBaseType, constraints, ranges, lengths,
                patterns, fractionDigits);

        return targetTypeBaseType;
    }

    /**
     * Merge curent constraints with founded type constraints
     *
     * @param targetTypeBaseType
     * @param constraints
     * @param ranges
     * @param lengths
     * @param patterns
     * @param fractionDigits
     */
    private TypeDefinition<?> mergeConstraints(TypeDefinition<?> targetTypeBaseType,
            TypeConstraints constraints, List<RangeConstraint> ranges,
            List<LengthConstraint> lengths, List<PatternConstraint> patterns,
            Integer fractionDigits) {
        String targetTypeBaseTypeName = targetTypeBaseType.getQName()
                .getLocalName();
        // enumeration, leafref and identityref omitted because they have no
        // restrictions
        if (targetTypeBaseType instanceof DecimalTypeDefinition) {
            List<RangeConstraint> fullRanges = new ArrayList<RangeConstraint>();
            fullRanges.addAll(constraints.getRanges());
            fullRanges.addAll(ranges);
            Integer fd = fractionDigits == null ? constraints
                    .getFractionDigits() : fractionDigits;
            targetTypeBaseType = YangTypesConverter
                    .javaTypeForBaseYangDecimal64Type(fullRanges, fd);
        } else if (targetTypeBaseType instanceof IntegerTypeDefinition) {
            List<RangeConstraint> fullRanges = new ArrayList<RangeConstraint>();
            fullRanges.addAll(constraints.getRanges());
            fullRanges.addAll(ranges);
            if (targetTypeBaseTypeName.startsWith("int")) {
                targetTypeBaseType = YangTypesConverter
                        .javaTypeForBaseYangSignedIntegerType(
                                targetTypeBaseTypeName, fullRanges);
            } else {
                targetTypeBaseType = YangTypesConverter
                        .javaTypeForBaseYangUnsignedIntegerType(
                                targetTypeBaseTypeName, fullRanges);
            }
        } else if (targetTypeBaseType instanceof StringTypeDefinition) {
            List<LengthConstraint> fullLengths = new ArrayList<LengthConstraint>();
            fullLengths.addAll(constraints.getLengths());
            fullLengths.addAll(lengths);
            List<PatternConstraint> fullPatterns = new ArrayList<PatternConstraint>();
            fullPatterns.addAll(constraints.getPatterns());
            fullPatterns.addAll(patterns);
            targetTypeBaseType = new StringType(fullLengths, fullPatterns);
        } else if (targetTypeBaseType instanceof BitsTypeDefinition) {
            BitsTypeDefinition bitsType = (BitsTypeDefinition) targetTypeBaseType;
            List<Bit> bits = bitsType.getBits();
            targetTypeBaseType = new BitsType(bits);
        } else if (targetTypeBaseType instanceof BinaryTypeDefinition) {
            targetTypeBaseType = new BinaryType(null, lengths, null);
        }
        return targetTypeBaseType;
    }

    private TypeDefinitionBuilder findTypeDefinitionBuilder(
            Map<String, TreeMap<Date, ModuleBuilder>> modules,
            UnknownType unknownType, ModuleBuilder builder) {
        Map<TypeDefinitionBuilder, TypeConstraints> result = findTypeDefinitionBuilderWithConstraints(
                modules, unknownType, builder);
        return result.entrySet().iterator().next().getKey();
    }

    private Map<TypeDefinitionBuilder, TypeConstraints> findTypeDefinitionBuilderWithConstraints(
            Map<String, TreeMap<Date, ModuleBuilder>> modules,
            UnknownType unknownType, ModuleBuilder builder) {
        return findTypeDefinitionBuilderWithConstraints(new TypeConstraints(),
                modules, unknownType, builder);
    }

    /**
     * Traverse through all referenced types chain until base YANG type is
     * founded.
     *
     * @param constraints
     *            current type constraints
     * @param modules
     *            all available modules
     * @param unknownType
     *            unknown type
     * @param builder
     *            current module
     * @return map, where key is type referenced and value is its constraints
     */
    private Map<TypeDefinitionBuilder, TypeConstraints> findTypeDefinitionBuilderWithConstraints(
            TypeConstraints constraints,
            Map<String, TreeMap<Date, ModuleBuilder>> modules,
            UnknownType unknownType, ModuleBuilder builder) {
        Map<TypeDefinitionBuilder, TypeConstraints> result = new HashMap<TypeDefinitionBuilder, TypeConstraints>();
        QName unknownTypeQName = unknownType.getQName();
        String unknownTypeName = unknownTypeQName.getLocalName();
        String unknownTypePrefix = unknownTypeQName.getPrefix();

        // search for module which contains referenced typedef
        ModuleBuilder dependentModuleBuilder;
        if (unknownTypePrefix.equals(builder.getPrefix())) {
            dependentModuleBuilder = builder;
        } else {
            dependentModuleBuilder = findDependentModule(modules, builder,
                    unknownTypePrefix);
        }

        // pull all typedef statements from dependent module...
        final Set<TypeDefinitionBuilder> typedefs = dependentModuleBuilder
                .getModuleTypedefs();
        // and search for referenced typedef
        TypeDefinitionBuilder lookedUpBuilder = null;
        for (TypeDefinitionBuilder tdb : typedefs) {
            QName qname = tdb.getQName();
            if (qname.getLocalName().equals(unknownTypeName)) {
                lookedUpBuilder = tdb;
                break;
            }
        }

        // if referenced type is UnknownType again, search recursively with
        // current constraints
        TypeDefinition<?> referencedType = lookedUpBuilder.getBaseType();
        if (referencedType instanceof UnknownType) {
            UnknownType unknown = (UnknownType) lookedUpBuilder.getBaseType();

            final List<RangeConstraint> ranges = unknown.getRangeStatements();
            constraints.addRanges(ranges);
            final List<LengthConstraint> lengths = unknown
                    .getLengthStatements();
            constraints.addLengths(lengths);
            final List<PatternConstraint> patterns = unknown.getPatterns();
            constraints.addPatterns(patterns);
            return findTypeDefinitionBuilderWithConstraints(constraints,
                    modules, unknown, dependentModuleBuilder);
        } else {
            // pull restriction from this base type and add them to
            // 'constraints'
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
            } else if (referencedType instanceof BinaryTypeDefinition) {
                constraints.addLengths(((BinaryTypeDefinition) referencedType)
                        .getLengthConstraints());
            }
            result.put(lookedUpBuilder, constraints);
            return result;
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
            //
            augmentTargetPath.add(0, dependentModule.getName());
            //


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
                ModuleBuilder dependentModule;
                if (baseIdentityPrefix.equals(module.getPrefix())) {
                    dependentModule = module;
                } else {
                    dependentModule = findDependentModule(modules, module,
                            baseIdentityPrefix);
                }

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
     * @return dependent module builder
     */
    private ModuleBuilder findDependentModule(
            Map<String, TreeMap<Date, ModuleBuilder>> modules,
            ModuleBuilder module, String prefix) {
        ModuleImport dependentModuleImport = getModuleImport(module, prefix);
        String dependentModuleName = dependentModuleImport.getModuleName();
        Date dependentModuleRevision = dependentModuleImport.getRevision();

        TreeMap<Date, ModuleBuilder> moduleBuildersByRevision = modules
                .get(dependentModuleName);
        ModuleBuilder dependentModule;
        if (dependentModuleRevision == null) {
            dependentModule = moduleBuildersByRevision.lastEntry().getValue();
        } else {
            dependentModule = moduleBuildersByRevision
                    .get(dependentModuleRevision);
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

    /**
     * Helper method for resolving special 'min' or 'max' values in range
     * constraint
     *
     * @param ranges
     *            ranges to resolve
     * @param targetType
     *            target type
     * @param modules
     *            all available modules
     * @param builder
     *            current module
     */
    private void resolveRanges(List<RangeConstraint> ranges,
            TypeDefinitionBuilder targetType,
            Map<String, TreeMap<Date, ModuleBuilder>> modules,
            ModuleBuilder builder) {
        if (ranges != null && ranges.size() > 0) {
            Long min = (Long) ranges.get(0).getMin();
            Long max = (Long) ranges.get(ranges.size() - 1).getMax();
            // if range contains one of the special values 'min' or 'max'
            if (min.equals(Long.MIN_VALUE) || max.equals(Long.MAX_VALUE)) {
                Long[] values = parseRangeConstraint(targetType, modules,
                        builder);
                if (min.equals(Long.MIN_VALUE)) {
                    min = values[0];
                    RangeConstraint oldFirst = ranges.get(0);
                    RangeConstraint newFirst = BaseConstraints.rangeConstraint(
                            min, oldFirst.getMax(), oldFirst.getDescription(),
                            oldFirst.getReference());
                    ranges.set(0, newFirst);
                }
                if (max.equals(Long.MAX_VALUE)) {
                    max = values[1];
                    RangeConstraint oldLast = ranges.get(ranges.size() - 1);
                    RangeConstraint newLast = BaseConstraints.rangeConstraint(
                            oldLast.getMin(), max, oldLast.getDescription(),
                            oldLast.getReference());
                    ranges.set(ranges.size() - 1, newLast);
                }
            }
        }
    }

    /**
     * Helper method for resolving special 'min' or 'max' values in length
     * constraint
     *
     * @param lengths
     *            lengths to resolve
     * @param targetType
     *            target type
     * @param modules
     *            all available modules
     * @param builder
     *            current module
     */
    private void resolveLengths(List<LengthConstraint> lengths,
            TypeDefinitionBuilder targetType,
            Map<String, TreeMap<Date, ModuleBuilder>> modules,
            ModuleBuilder builder) {
        if (lengths != null && lengths.size() > 0) {
            Long min = lengths.get(0).getMin().longValue();
            Long max = lengths.get(lengths.size() - 1).getMax().longValue();
            // if length contains one of the special values 'min' or 'max'
            if (min.equals(Long.MIN_VALUE) || max.equals(Long.MAX_VALUE)) {
                Long[] values = parseRangeConstraint(targetType, modules,
                        builder);
                if (min.equals(Long.MIN_VALUE)) {
                    min = values[0];
                    LengthConstraint oldFirst = lengths.get(0);
                    LengthConstraint newFirst = BaseConstraints
                            .lengthConstraint(min, oldFirst.getMax(),
                                    oldFirst.getDescription(),
                                    oldFirst.getReference());
                    lengths.set(0, newFirst);
                }
                if (max.equals(Long.MAX_VALUE)) {
                    max = values[1];
                    LengthConstraint oldLast = lengths.get(lengths.size() - 1);
                    LengthConstraint newLast = BaseConstraints
                            .lengthConstraint(oldLast.getMin(), max,
                                    oldLast.getDescription(),
                                    oldLast.getReference());
                    lengths.set(lengths.size() - 1, newLast);
                }
            }
        }
    }

    private Long[] parseRangeConstraint(TypeDefinitionBuilder targetType,
            Map<String, TreeMap<Date, ModuleBuilder>> modules,
            ModuleBuilder builder) {
        TypeDefinition<?> targetBaseType = targetType.getBaseType();

        if (targetBaseType instanceof IntegerTypeDefinition) {
            IntegerTypeDefinition itd = (IntegerTypeDefinition) targetBaseType;
            List<RangeConstraint> ranges = itd.getRangeStatements();
            Long min = (Long) ranges.get(0).getMin();
            Long max = (Long) ranges.get(ranges.size() - 1).getMax();
            return new Long[] { min, max };
        } else if (targetBaseType instanceof DecimalTypeDefinition) {
            DecimalTypeDefinition dtd = (DecimalTypeDefinition) targetBaseType;
            List<RangeConstraint> ranges = dtd.getRangeStatements();
            Long min = (Long) ranges.get(0).getMin();
            Long max = (Long) ranges.get(ranges.size() - 1).getMax();
            return new Long[] { min, max };
        } else {
            return parseRangeConstraint(
                    findTypeDefinitionBuilder(modules,
                            (UnknownType) targetBaseType, builder), modules,
                    builder);
        }
    }

    private Date createEpochTime() {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(0);
        return c.getTime();
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
    }

    private static class TypeConstraints {
        private final List<RangeConstraint> ranges = new ArrayList<RangeConstraint>();
        private final List<LengthConstraint> lengths = new ArrayList<LengthConstraint>();
        private final List<PatternConstraint> patterns = new ArrayList<PatternConstraint>();
        private Integer fractionDigits;

        public List<RangeConstraint> getRanges() {
            return ranges;
        }

        public void addRanges(List<RangeConstraint> ranges) {
            this.ranges.addAll(0, ranges);
        }

        public List<LengthConstraint> getLengths() {
            return lengths;
        }

        public void addLengths(List<LengthConstraint> lengths) {
            this.lengths.addAll(0, lengths);
        }

        public List<PatternConstraint> getPatterns() {
            return patterns;
        }

        public void addPatterns(List<PatternConstraint> patterns) {
            this.patterns.addAll(0, patterns);
        }

        public Integer getFractionDigits() {
            return fractionDigits;
        }

        public void setFractionDigits(Integer fractionDigits) {
            if (fractionDigits != null) {
                this.fractionDigits = fractionDigits;
            }
        }
    }

}
