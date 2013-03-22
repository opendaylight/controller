/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.model.parser.builder;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.opendaylight.controller.antlrv4.code.gen.YangLexer;
import org.opendaylight.controller.antlrv4.code.gen.YangParser;
import org.opendaylight.controller.model.parser.api.AugmentationSchemaBuilder;
import org.opendaylight.controller.model.parser.api.AugmentationTargetBuilder;
import org.opendaylight.controller.model.parser.api.Builder;
import org.opendaylight.controller.model.parser.api.TypeAwareBuilder;
import org.opendaylight.controller.model.parser.api.TypeDefinitionBuilder;
import org.opendaylight.controller.model.parser.impl.YangModelParserImpl;
import org.opendaylight.controller.model.util.UnknownType;
import org.opendaylight.controller.yang.common.QName;
import org.opendaylight.controller.yang.model.api.AugmentationSchema;
import org.opendaylight.controller.yang.model.api.Module;
import org.opendaylight.controller.yang.model.api.ModuleImport;
import org.opendaylight.controller.yang.model.api.SchemaPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class YangModelBuilder implements Builder {

    private static final Logger logger = LoggerFactory
            .getLogger(YangModelBuilder.class);

    private final Map<String, ModuleBuilder> modules = new HashMap<String, ModuleBuilder>();

    public YangModelBuilder(String... yangFiles) {
        final YangModelParserImpl yangModelParser = new YangModelParserImpl();
        final ParseTreeWalker walker = new ParseTreeWalker();

        List<ParseTree> trees = parseYangFiles(yangFiles);

        ModuleBuilder[] builders = new ModuleBuilder[trees.size()];

        for (int i = 0; i < trees.size(); i++) {
            walker.walk(yangModelParser, trees.get(i));
            builders[i] = yangModelParser.getModuleBuilder();
        }

        for (ModuleBuilder builder : builders) {
            final String builderName = builder.getName();
            modules.put(builderName, builder);
        }
    }

    @Override
    public Map<String, Module> build() {
        Map<String, Module> builtModules = new HashMap<String, Module>();
        for (ModuleBuilder builder : modules.values()) {
            validateBuilder(builder);
            builtModules.put(builder.getName(), builder.build());
        }
        return builtModules;
    }

    private void validateBuilder(ModuleBuilder builder) {
        resolveTypedefs(builder);
        resolveAugments(builder);
    }

    private void resolveTypedefs(ModuleBuilder builder) {
        Map<List<String>, TypeAwareBuilder> dirtyNodes = builder
                .getDirtyNodes();
        if (dirtyNodes.size() == 0) {
            return;
        } else {
            for (Map.Entry<List<String>, TypeAwareBuilder> entry : dirtyNodes
                    .entrySet()) {
                TypeAwareBuilder tab = entry.getValue();
                TypeDefinitionBuilder tdb = findTypeAwareBuilder(
                        entry.getValue(), builder);
                tab.setType(tdb.build());
            }
        }
    }

    private TypeDefinitionBuilder findTypeAwareBuilder(
            TypeAwareBuilder typeBuilder, ModuleBuilder builder) {
        UnknownType type = (UnknownType) typeBuilder.getType();
        QName typeQName = type.getQName();
        String typeName = type.getQName().getLocalName();
        String prefix = typeQName.getPrefix();

        ModuleBuilder dependentModuleBuilder;
        if (prefix.equals(builder.getPrefix())) {
            dependentModuleBuilder = builder;
        } else {
            String dependentModuleName = getDependentModuleName(builder, prefix);
            dependentModuleBuilder = modules.get(dependentModuleName);
        }

        Set<TypeDefinitionBuilder> typedefs = dependentModuleBuilder
                .getModuleTypedefs();

        TypeDefinitionBuilder lookedUpBuilder = null;
        for (TypeDefinitionBuilder tdb : typedefs) {
            QName qname = tdb.getQName();
            if (qname.getLocalName().equals(typeName)) {
                lookedUpBuilder = tdb;
                break;
            }
        }

        if (lookedUpBuilder.getBaseType() instanceof UnknownType) {
            return findTypeAwareBuilder((TypeAwareBuilder) lookedUpBuilder,
                    dependentModuleBuilder);
        } else {
            return lookedUpBuilder;
        }
    }

    private void resolveAugments(ModuleBuilder builder) {
        Set<AugmentationSchemaBuilder> augmentBuilders = builder
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
            String dependentModuleName = getDependentModuleName(builder, prefix);
            augmentTargetPath.add(0, dependentModuleName);

            ModuleBuilder dependentModule = modules.get(dependentModuleName);
            AugmentationTargetBuilder augmentTarget = (AugmentationTargetBuilder) dependentModule
                    .getNode(augmentTargetPath);
            AugmentationSchema result = augmentBuilder.build();
            augmentTarget.addAugmentation(result);
            augments.add(result);
        }
        builder.setAugmentations(augments);
    }

    private List<ParseTree> parseYangFiles(String... yangFiles) {
        List<ParseTree> trees = new ArrayList<ParseTree>();
        File yangFile;
        for (String fileName : yangFiles) {
            try {
                yangFile = new File(fileName);
                FileInputStream inStream = new FileInputStream(yangFile);
                ANTLRInputStream input = new ANTLRInputStream(inStream);
                final YangLexer lexer = new YangLexer(input);
                final CommonTokenStream tokens = new CommonTokenStream(lexer);
                final YangParser parser = new YangParser(tokens);
                trees.add(parser.yang());
            } catch (IOException e) {
                logger.warn("Exception while reading yang file: " + fileName, e);
            }
        }
        return trees;
    }

    /**
     * Returns name of dependent module based on given prefix.
     * 
     * @param builder
     *            current builder which contains import
     * @param prefix
     *            prefix of dependent module used in current builder
     * @return name of dependent module
     */
    private String getDependentModuleName(ModuleBuilder builder, String prefix) {
        ModuleImport moduleImport = null;
        for (ModuleImport mi : builder.getModuleImports()) {
            if (mi.getPrefix().equals(prefix)) {
                moduleImport = mi;
                break;
            }
        }
        return moduleImport.getModuleName();
    }

}
