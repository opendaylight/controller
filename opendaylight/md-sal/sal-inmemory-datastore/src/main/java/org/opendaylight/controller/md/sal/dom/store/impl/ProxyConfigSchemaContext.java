/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.store.impl;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.model.api.AugmentationSchema;
import org.opendaylight.yangtools.yang.model.api.ChoiceCaseNode;
import org.opendaylight.yangtools.yang.model.api.ChoiceNode;
import org.opendaylight.yangtools.yang.model.api.ConstraintDefinition;
import org.opendaylight.yangtools.yang.model.api.ContainerSchemaNode;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ExtensionDefinition;
import org.opendaylight.yangtools.yang.model.api.GroupingDefinition;
import org.opendaylight.yangtools.yang.model.api.ListSchemaNode;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.ModuleIdentifier;
import org.opendaylight.yangtools.yang.model.api.NotificationDefinition;
import org.opendaylight.yangtools.yang.model.api.RpcDefinition;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;
import org.opendaylight.yangtools.yang.model.api.Status;
import org.opendaylight.yangtools.yang.model.api.TypeDefinition;
import org.opendaylight.yangtools.yang.model.api.UnknownSchemaNode;
import org.opendaylight.yangtools.yang.model.api.UsesNode;
import org.opendaylight.yangtools.yang.parser.builder.impl.ChoiceBuilder;
import org.opendaylight.yangtools.yang.parser.builder.impl.ChoiceCaseBuilder;
import org.opendaylight.yangtools.yang.parser.builder.impl.ContainerSchemaNodeBuilder;
import org.opendaylight.yangtools.yang.parser.builder.impl.ListSchemaNodeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProxyConfigSchemaContext implements SchemaContext {
    private static final Logger LOG = LoggerFactory.getLogger(ProxyConfigSchemaContext.class);

    private SchemaContext operationalSchemaContext;

    // Cache of modified DataSchemaNode objects that contains only child nodes available in configuration data store context
    LoadingCache<QName, DataSchemaNode> configSchemaNodes = CacheBuilder.newBuilder().maximumSize(1000).build(
            new CacheLoader<QName, DataSchemaNode>() {
                @Override
                public DataSchemaNode load(QName qName) throws Exception {
                    DataSchemaNode dataChildByName = operationalSchemaContext.getDataChildByName(qName);
                    if (dataChildByName != null && dataChildByName.isConfiguration()) {
                        return makeConfigOnlyDataSchemaNode(dataChildByName);
                    }
                    return null;
                }
            });

    public ProxyConfigSchemaContext(SchemaContext operationalSchemaContext) {
        this.operationalSchemaContext = operationalSchemaContext;
    }

    @Override
    public boolean isPresenceContainer() {
        return operationalSchemaContext.isPresenceContainer();
    }

    @Override
    public Set<TypeDefinition<?>> getTypeDefinitions() {
        return operationalSchemaContext.getTypeDefinitions();
    }

    @Override
    public Collection<DataSchemaNode> getChildNodes() {
        Set<DataSchemaNode> configChildNodes = new LinkedHashSet<>();
        for (DataSchemaNode dataSchemaNode : operationalSchemaContext.getChildNodes()) {
            if (dataSchemaNode.isConfiguration()) {
                try {
                    DataSchemaNode cachedConfigSchemaNode;
                    cachedConfigSchemaNode = configSchemaNodes.get(dataSchemaNode.getQName());
                    configChildNodes.add(cachedConfigSchemaNode);
                } catch (ExecutionException e) {
                    LOG.error("Exception when trying to retrieve config DataSchemaNode for node '{'} : ",
                            dataSchemaNode.getQName(), e);
                }
            }
        }
        return Collections.unmodifiableSet(configChildNodes);
    }

    @Override
    public Set<GroupingDefinition> getGroupings() {
        return operationalSchemaContext.getGroupings();
    }

    @Override
    public DataSchemaNode getDataChildByName(QName name) {
        DataSchemaNode cachedConfigSchemaNode = null;
        try {
            cachedConfigSchemaNode = configSchemaNodes.get(name);
        } catch (ExecutionException e) {
            LOG.error("Exception when trying to retrieve config DataSchemaNode for node '{'} : ",
                    name, e);
        }
        return cachedConfigSchemaNode;
    }

    @Override
    public DataSchemaNode getDataChildByName(String name) {
        DataSchemaNode dataChildByName = operationalSchemaContext.getDataChildByName(name);
        DataSchemaNode cachedConfigSchemaNode = null;
        try {
            cachedConfigSchemaNode = configSchemaNodes.get(dataChildByName.getQName());
        } catch (ExecutionException e) {
            LOG.error("Exception when trying to retrieve config DataSchemaNode for node '{'} : ",
                    dataChildByName.getQName(), e);
        }
        return cachedConfigSchemaNode;
    }

    @Override
    public Set<UsesNode> getUses() {
        return operationalSchemaContext.getUses();
    }

    @Override
    public Set<AugmentationSchema> getAvailableAugmentations() {
        return operationalSchemaContext.getAvailableAugmentations();
    }

    @Override
    public boolean isAugmenting() {
        return operationalSchemaContext.isAugmenting();
    }

    @Override
    public boolean isAddedByUses() {
        return operationalSchemaContext.isAddedByUses();
    }

    @Override
    public boolean isConfiguration() {
        return operationalSchemaContext.isConfiguration();
    }

    @Override
    public ConstraintDefinition getConstraints() {
        return operationalSchemaContext.getConstraints();
    }

    @Override
    public QName getQName() {
        return operationalSchemaContext.getQName();
    }

    @Override
    public SchemaPath getPath() {
        return operationalSchemaContext.getPath();
    }

    @Override
    public List<UnknownSchemaNode> getUnknownSchemaNodes() {
        return operationalSchemaContext.getUnknownSchemaNodes();
    }

    @Override
    public String getDescription() {
        return operationalSchemaContext.getDescription();
    }

    @Override
    public String getReference() {
        return operationalSchemaContext.getReference();
    }

    @Override
    public Status getStatus() {
        return operationalSchemaContext.getStatus();
    }

    @Override
    public Set<DataSchemaNode> getDataDefinitions() {
        Set<DataSchemaNode> configDataDefinitions = new LinkedHashSet<>();
        for (DataSchemaNode dataSchemaNode : operationalSchemaContext.getDataDefinitions()) {
            if (dataSchemaNode.isConfiguration()) {
                configDataDefinitions.add(dataSchemaNode);
            }
        }
        return Collections.unmodifiableSet(configDataDefinitions);
    }

    @Override
    public Set<Module> getModules() {
        return operationalSchemaContext.getModules();
    }

    @Override
    public Set<NotificationDefinition> getNotifications() {
        return operationalSchemaContext.getNotifications();
    }

    @Override
    public Set<RpcDefinition> getOperations() {
        return operationalSchemaContext.getOperations();
    }

    @Override
    public Set<ExtensionDefinition> getExtensions() {
        return operationalSchemaContext.getExtensions();
    }

    @Override
    public Module findModuleByName(String name, Date revision) {
        return operationalSchemaContext.findModuleByName(name, revision);
    }

    @Override
    public Set<Module> findModuleByNamespace(URI namespace) {
        return operationalSchemaContext.findModuleByNamespace(namespace);
    }

    @Override
    public Module findModuleByNamespaceAndRevision(URI namespace, Date revision) {
        return operationalSchemaContext.findModuleByNamespaceAndRevision(namespace, revision);
    }

    @Override
    public Optional<String> getModuleSource(ModuleIdentifier moduleIdentifier) {
        return operationalSchemaContext.getModuleSource(moduleIdentifier);
    }

    @Override
    public Set<ModuleIdentifier> getAllModuleIdentifiers() {
        return operationalSchemaContext.getAllModuleIdentifiers();
    }

    /**
     * Method returns DataSchemaNode that contains only children that have "config true" defined in the parent schema
     *
     * @param node root DataShemaNode that will be modified
     * @return DataSchemaNode DataShemaNode with non configuration nodes removed.
     */
    private DataSchemaNode makeConfigOnlyDataSchemaNode(DataSchemaNode node) {
        Preconditions.checkNotNull(node);

        if (node instanceof ContainerSchemaNode) {
            ContainerSchemaNode containerSchemaNode = (ContainerSchemaNode) node;
            ContainerSchemaNodeBuilder csnBuilder = new ContainerSchemaNodeBuilder("", 0, containerSchemaNode.getQName(),
                    containerSchemaNode.getPath());
            csnBuilder.setAddedByUses(containerSchemaNode.isAddedByUses());
            csnBuilder.setAugmenting(containerSchemaNode.isAugmenting());
            csnBuilder.setConfiguration(containerSchemaNode.isConfiguration());
            csnBuilder.setPath(containerSchemaNode.getPath());
            csnBuilder.setPresence(containerSchemaNode.isPresenceContainer());
            csnBuilder.setDescription(containerSchemaNode.getDescription());
            csnBuilder.setStatus(containerSchemaNode.getStatus());
            csnBuilder.setReference(containerSchemaNode.getReference());
            for (DataSchemaNode childNode : containerSchemaNode.getChildNodes()) {
                if (childNode.isConfiguration()) {
                    DataSchemaNode modifiedChildNode = makeConfigOnlyDataSchemaNode(childNode);
                    csnBuilder.addChildNode(modifiedChildNode);
                }
            }
            return csnBuilder.build();
        } else if (node instanceof ListSchemaNode) {
            ListSchemaNode listSchemaNode = (ListSchemaNode) node;
            ListSchemaNodeBuilder lsnBuilder = new ListSchemaNodeBuilder("", 0, node.getQName(), node.getPath());
            lsnBuilder.setAddedByUses(listSchemaNode.isAddedByUses());
            lsnBuilder.setAugmenting(listSchemaNode.isAugmenting());
            lsnBuilder.setConfiguration(listSchemaNode.isConfiguration());
            lsnBuilder.setPath(listSchemaNode.getPath());
            lsnBuilder.setUserOrdered(listSchemaNode.isUserOrdered());
            lsnBuilder.setDescription(listSchemaNode.getDescription());
            lsnBuilder.setStatus(listSchemaNode.getStatus());
            lsnBuilder.setReference(listSchemaNode.getReference());
            Set<String> keys = new HashSet<>();
            for (QName keyQName : listSchemaNode.getKeyDefinition()) {
                keys.add(keyQName.getLocalName());
            }
            lsnBuilder.setKeys(keys);
            for (DataSchemaNode childNode : listSchemaNode.getChildNodes()) {
                if (childNode.isConfiguration()) {
                    DataSchemaNode modifiedChildNode = makeConfigOnlyDataSchemaNode(childNode);
                    lsnBuilder.addChildNode(modifiedChildNode);
                }
            }
            return lsnBuilder.build();
        } else if (node instanceof ChoiceNode) {
            ChoiceNode choiceNode = (ChoiceNode) node;
            ChoiceBuilder cBuilder = new ChoiceBuilder("", 0, node.getQName(), node.getPath());
            cBuilder.setReference(choiceNode.getReference());
            cBuilder.setStatus(choiceNode.getStatus());
            cBuilder.setConfiguration(choiceNode.isConfiguration());
            cBuilder.setAddedByUses(choiceNode.isAddedByUses());
            cBuilder.setAugmenting(choiceNode.isAugmenting());
            cBuilder.setDefaultCase(choiceNode.getDefaultCase());
            cBuilder.setDescription(choiceNode.getDescription());
            cBuilder.setPath(choiceNode.getPath());
            ChoiceCaseBuilder ccBuilder;
            for (ChoiceCaseNode caseNode : choiceNode.getCases()) {
                ChoiceCaseNode modifiedCaseNode = (ChoiceCaseNode) makeConfigOnlyDataSchemaNode(caseNode);
                ccBuilder = new ChoiceCaseBuilder("", 0, caseNode.getQName(), caseNode.getPath(), modifiedCaseNode);
                cBuilder.addCase(ccBuilder);
            }
            return cBuilder.build();
        } else if (node instanceof ChoiceCaseNode) {
            ChoiceCaseNode choiceCaseNode = (ChoiceCaseNode) node;
            ChoiceCaseBuilder ccBuilder = new ChoiceCaseBuilder("", 0, node.getQName(), node.getPath());
            ccBuilder.setAddedByUses(choiceCaseNode.isAddedByUses());
            ccBuilder.setAugmenting(choiceCaseNode.isAugmenting());
            ccBuilder.setPath(choiceCaseNode.getPath());
            ccBuilder.setDescription(choiceCaseNode.getDescription());
            ccBuilder.setStatus(choiceCaseNode.getStatus());
            ccBuilder.setReference(choiceCaseNode.getReference());
            for (DataSchemaNode childNode : choiceCaseNode.getChildNodes()) {
                if (childNode.isConfiguration()) {
                    DataSchemaNode modifiedChildNode = makeConfigOnlyDataSchemaNode(childNode);
                    ccBuilder.addChildNode(modifiedChildNode);
                }
            }
            return ccBuilder.build();
        } else {
            return node;
        }
    }
}
