package org.opendaylight.controller.md.sal.dom.store.impl;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.model.api.*;
import org.opendaylight.yangtools.yang.parser.builder.impl.ChoiceCaseBuilder;
import org.opendaylight.yangtools.yang.parser.builder.impl.ContainerSchemaNodeBuilder;
import org.opendaylight.yangtools.yang.parser.builder.impl.ListSchemaNodeBuilder;

import java.net.URI;
import java.util.*;
import java.util.concurrent.ExecutionException;

public class SchemaContextConfigProxy implements SchemaContext {

    private SchemaContext operationalSchemaContext;

    LoadingCache<QName, DataSchemaNode> configSchemaNodes = CacheBuilder.newBuilder().maximumSize(1000).build(
            new CacheLoader<QName, DataSchemaNode>() {
                @Override
                public DataSchemaNode load(QName qName) throws Exception {
                    DataSchemaNode dataChildByName = operationalSchemaContext.getDataChildByName(qName);
                    return makeConfigOnlyDataSchemaNode(dataChildByName);
                }
            });

    public SchemaContextConfigProxy(SchemaContext operationalSchemaContext) {
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
                configChildNodes.add(dataSchemaNode);
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
        DataSchemaNode dataSchemaNode = null;
        try {
            dataSchemaNode = configSchemaNodes.get(name);
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        return dataSchemaNode;
    }

    @Override
    public DataSchemaNode getDataChildByName(String name) {
        DataSchemaNode dataChildByName = operationalSchemaContext.getDataChildByName(name);
        if (dataChildByName.isConfiguration()) {
            return makeConfigOnlyDataSchemaNode(dataChildByName);
        }
        return null;
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
     * @param node root of the tree
     * @return DataSchemaNode representing tree without operational only nodes.
     */
    private DataSchemaNode makeConfigOnlyDataSchemaNode(DataSchemaNode node) {
        Preconditions.checkNotNull(node);

        if (node instanceof AnyXmlSchemaNode) {
//			new AnyXmlBuilder("", 0, node.getQName(), node.getPath(), ((AnyXmlSchemaNode) node));
            return node;
        } else if (node instanceof ChoiceNode) {
//			new ChoiceBuilder("", 0, node.getQName(), node.getPath(), ((ChoiceNode) node));
            return node;
        } else if (node instanceof ContainerSchemaNode) {
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
        } else if (node instanceof LeafSchemaNode) {
//			new LeafSchemaNodeBuilder("", 0, node.getQName(), node.getPath(), ((LeafSchemaNode) node));
            return node;
        } else if (node instanceof LeafListSchemaNode) {
//			new LeafListSchemaNodeBuilder("", 0, node.getQName(), node.getPath(), ((LeafListSchemaNode) node));
            return node;
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
        } else if (node instanceof ChoiceCaseNode) {
            ChoiceCaseNode choiceCaseNode = (ChoiceCaseNode) node;
            ChoiceCaseBuilder ccBuilder = new ChoiceCaseBuilder("", 0, node.getQName(), node.getPath());
            ccBuilder.setAddedByUses(choiceCaseNode.isAddedByUses());
            ccBuilder.setAugmenting(choiceCaseNode.isAugmenting());
            ccBuilder.setConfiguration(choiceCaseNode.isConfiguration());
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
        }
        throw new RuntimeException("Something went wrong!!!");
    }
}
