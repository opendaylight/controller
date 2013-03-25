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
import java.io.IOException;
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
import org.opendaylight.controller.model.util.UnknownType;
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
import org.opendaylight.controller.yang.model.parser.api.YangModelParser;
import org.opendaylight.controller.yang.model.parser.builder.api.AugmentationSchemaBuilder;
import org.opendaylight.controller.yang.model.parser.builder.api.AugmentationTargetBuilder;
import org.opendaylight.controller.yang.model.parser.builder.api.ChildNodeBuilder;
import org.opendaylight.controller.yang.model.parser.builder.api.DataSchemaNodeBuilder;
import org.opendaylight.controller.yang.model.parser.builder.api.TypeAwareBuilder;
import org.opendaylight.controller.yang.model.parser.builder.api.TypeDefinitionBuilder;
import org.opendaylight.controller.yang.model.parser.builder.impl.ModuleBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class YangModelParserImpl implements YangModelParser {

    private static final Logger logger = LoggerFactory
            .getLogger(YangModelParserImpl.class);

    @Override
    public Module parseYangModel(String yangFile) {
        final Map<String, TreeMap<Date, ModuleBuilder>> modules = loadFiles(yangFile);
        Set<Module> result = build(modules);
        return result.iterator().next();
    }

    @Override
    public Set<Module> parseYangModels(String... yangFiles) {
        final Map<String, TreeMap<Date, ModuleBuilder>> modules = loadFiles(yangFiles);
        Set<Module> result = build(modules);
        return result;
    }

    @Override
    public SchemaContext resolveSchemaContext(Set<Module> modules) {
        return new SchemaContextImpl(modules);
    }

    private Set<Module> build(Map<String, TreeMap<Date, ModuleBuilder>> modules) {
        // first validate
        for (Map.Entry<String, TreeMap<Date, ModuleBuilder>> entry : modules.entrySet()) {
            for (Map.Entry<Date, ModuleBuilder> childEntry : entry.getValue().entrySet()) {
                ModuleBuilder moduleBuilder = childEntry.getValue();
                validateBuilder(modules, moduleBuilder);
            }
        }

        // then build
        final Set<Module> result = new HashSet<Module>();
        for (Map.Entry<String, TreeMap<Date, ModuleBuilder>> entry : modules.entrySet()) {
            final Map<Date, Module> modulesByRevision = new HashMap<Date, Module>();
            for (Map.Entry<Date, ModuleBuilder> childEntry : entry.getValue().entrySet()) {
                ModuleBuilder moduleBuilder = childEntry.getValue();
                modulesByRevision.put(childEntry.getKey(),moduleBuilder.build());
                result.add(moduleBuilder.build());
            }
        }

        return result;
    }

    private void validateBuilder(Map<String, TreeMap<Date, ModuleBuilder>> modules, ModuleBuilder builder) {
        resolveTypedefs(modules, builder);
        resolveAugments(modules, builder);
    }

    private void resolveTypedefs(Map<String, TreeMap<Date, ModuleBuilder>> modules, ModuleBuilder builder) {
        Map<List<String>, TypeAwareBuilder> dirtyNodes = builder.getDirtyNodes();
        if (dirtyNodes.size() == 0) {
            return;
        } else {
            for (Map.Entry<List<String>, TypeAwareBuilder> entry : dirtyNodes.entrySet()) {
                TypeAwareBuilder tab = entry.getValue();
                TypeDefinitionBuilder tdb = findTypeDefinitionBuilder(modules,entry.getValue(), builder);
                tab.setType(tdb.build());
            }
        }
    }

    private TypeDefinitionBuilder findTypeDefinitionBuilder(Map<String, TreeMap<Date, ModuleBuilder>> modules, TypeAwareBuilder typeBuilder, ModuleBuilder builder) {
        UnknownType type = (UnknownType) typeBuilder.getType();
        QName typeQName = type.getQName();
        String typeName = type.getQName().getLocalName();
        String prefix = typeQName.getPrefix();

        ModuleBuilder dependentModuleBuilder;
        if (prefix.equals(builder.getPrefix())) {
            dependentModuleBuilder = builder;
        } else {
            ModuleImport dependentModuleImport = getDependentModuleImport(builder, prefix);
            String dependentModuleName = dependentModuleImport.getModuleName();
            Date dependentModuleRevision = dependentModuleImport.getRevision();
            TreeMap<Date, ModuleBuilder> moduleBuildersByRevision = modules.get(dependentModuleName);
            if(dependentModuleRevision == null) {
                dependentModuleBuilder = moduleBuildersByRevision.lastEntry().getValue();
            } else {
                dependentModuleBuilder = moduleBuildersByRevision.get(dependentModuleRevision);
            }
        }

        final Set<TypeDefinitionBuilder> typedefs = dependentModuleBuilder.getModuleTypedefs();

        TypeDefinitionBuilder lookedUpBuilder = null;
        for (TypeDefinitionBuilder tdb : typedefs) {
            QName qname = tdb.getQName();
            if (qname.getLocalName().equals(typeName)) {
                lookedUpBuilder = tdb;
                break;
            }
        }

        if (lookedUpBuilder.getBaseType() instanceof UnknownType) {
            return findTypeDefinitionBuilder(modules, (TypeAwareBuilder) lookedUpBuilder, dependentModuleBuilder);
        } else {
            return lookedUpBuilder;
        }
    }

    private void resolveAugments(Map<String, TreeMap<Date, ModuleBuilder>> modules, ModuleBuilder builder) {
        Set<AugmentationSchemaBuilder> augmentBuilders = builder.getAddedAugments();

        Set<AugmentationSchema> augments = new HashSet<AugmentationSchema>();
        for (AugmentationSchemaBuilder augmentBuilder : augmentBuilders) {
            SchemaPath augmentTargetSchemaPath = augmentBuilder.getTargetPath();
            String prefix = null;
            List<String> augmentTargetPath = new ArrayList<String>();
            for (QName pathPart : augmentTargetSchemaPath.getPath()) {
                prefix = pathPart.getPrefix();
                augmentTargetPath.add(pathPart.getLocalName());
            }
            ModuleImport dependentModuleImport = getDependentModuleImport(builder, prefix);
            String dependentModuleName = dependentModuleImport.getModuleName();
            augmentTargetPath.add(0, dependentModuleName);

            Date dependentModuleRevision = dependentModuleImport.getRevision();

            TreeMap<Date, ModuleBuilder> moduleBuildersByRevision = modules.get(dependentModuleName);
            ModuleBuilder dependentModule;
            if(dependentModuleRevision == null) {
                dependentModule = moduleBuildersByRevision.lastEntry().getValue();
            } else {
                dependentModule = moduleBuildersByRevision.get(dependentModuleRevision);
            }

            AugmentationTargetBuilder augmentTarget = (AugmentationTargetBuilder) dependentModule.getNode(augmentTargetPath);
            AugmentationSchema result = augmentBuilder.build();
            augmentTarget.addAugmentation(result);
            fillAugmentTarget(augmentBuilder, (ChildNodeBuilder) augmentTarget);
            augments.add(result);
        }
        builder.setAugmentations(augments);
    }

    private void fillAugmentTarget(AugmentationSchemaBuilder augment,
            ChildNodeBuilder target) {
        for (DataSchemaNodeBuilder builder : augment.getChildNodes()) {
            builder.setAugmenting(true);
            target.addChildNode(builder);
        }
    }

    private Map<String, TreeMap<Date, ModuleBuilder>> loadFiles(String... yangFiles) {
        final Map<String, TreeMap<Date, ModuleBuilder>> modules = new HashMap<String, TreeMap<Date, ModuleBuilder>>();

        final YangModelParserListenerImpl yangModelParser = new YangModelParserListenerImpl();
        final ParseTreeWalker walker = new ParseTreeWalker();

        List<ParseTree> trees = parseFiles(yangFiles);

        ModuleBuilder[] builders = new ModuleBuilder[trees.size()];

        for (int i = 0; i < trees.size(); i++) {
            walker.walk(yangModelParser, trees.get(i));
            builders[i] = yangModelParser.getModuleBuilder();
        }

        for (ModuleBuilder builder : builders) {
            final String builderName = builder.getName();
            Date builderRevision = builder.getRevision();
            if(builderRevision == null) {
                builderRevision = createEpochTime();
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

    private List<ParseTree> parseFiles(String... yangFileNames) {
        List<ParseTree> trees = new ArrayList<ParseTree>();
        for (String fileName : yangFileNames) {
            trees.add(parseFile(fileName));
        }
        return trees;
    }

    private ParseTree parseFile(String yangFileName) {
        ParseTree result = null;
        try {
            final File yangFile = new File(yangFileName);
            final FileInputStream inStream = new FileInputStream(yangFile);
            final ANTLRInputStream input = new ANTLRInputStream(inStream);
            final YangLexer lexer = new YangLexer(input);
            final CommonTokenStream tokens = new CommonTokenStream(lexer);
            final YangParser parser = new YangParser(tokens);
            result = parser.yang();
        } catch (IOException e) {
            logger.warn("Exception while reading yang file: " + yangFileName, e);
        }
        return result;
    }

    private ModuleImport getDependentModuleImport(ModuleBuilder builder, String prefix) {
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

}
