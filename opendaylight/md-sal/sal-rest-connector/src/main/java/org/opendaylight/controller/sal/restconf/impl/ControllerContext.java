/**
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.restconf.impl;

import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import javax.ws.rs.core.Response.Status;
import org.opendaylight.controller.md.sal.common.impl.util.compat.DataNormalizationOperation;
import org.opendaylight.controller.md.sal.common.impl.util.compat.DataNormalizer;
import org.opendaylight.controller.md.sal.dom.api.DOMMountPoint;
import org.opendaylight.controller.md.sal.dom.api.DOMMountPointService;
import org.opendaylight.controller.sal.rest.api.Draft02;
import org.opendaylight.controller.sal.rest.impl.RestUtil;
import org.opendaylight.controller.sal.restconf.impl.RestconfError.ErrorTag;
import org.opendaylight.controller.sal.restconf.impl.RestconfError.ErrorType;
import org.opendaylight.yangtools.concepts.Codec;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.AugmentationIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.InstanceIdentifierBuilder;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.AnyXmlSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ChoiceCaseNode;
import org.opendaylight.yangtools.yang.model.api.ChoiceNode;
import org.opendaylight.yangtools.yang.model.api.ContainerSchemaNode;
import org.opendaylight.yangtools.yang.model.api.DataNodeContainer;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.GroupingDefinition;
import org.opendaylight.yangtools.yang.model.api.LeafListSchemaNode;
import org.opendaylight.yangtools.yang.model.api.LeafSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ListSchemaNode;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.RpcDefinition;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaContextListener;
import org.opendaylight.yangtools.yang.model.api.TypeDefinition;
import org.opendaylight.yangtools.yang.model.api.type.IdentityrefTypeDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ControllerContext implements SchemaContextListener {
    private final static Logger LOG = LoggerFactory.getLogger(ControllerContext.class);

    private final static ControllerContext INSTANCE = new ControllerContext();

    private final static String NULL_VALUE = "null";

    private final static String MOUNT_MODULE = "yang-ext";

    private final static String MOUNT_NODE = "mount";

    public final static String MOUNT = "yang-ext:mount";

    private final static String URI_ENCODING_CHAR_SET = "ISO-8859-1";

    private static final Splitter SLASH_SPLITTER = Splitter.on('/');

    private final AtomicReference<Map<QName, RpcDefinition>> qnameToRpc =
            new AtomicReference<>(Collections.<QName, RpcDefinition>emptyMap());

    private volatile SchemaContext globalSchema;
    private volatile DOMMountPointService mountService;

    private DataNormalizer dataNormalizer;

    public void setGlobalSchema(final SchemaContext globalSchema) {
        this.globalSchema = globalSchema;
        this.dataNormalizer = new DataNormalizer(globalSchema);
    }

    public void setMountService(final DOMMountPointService mountService) {
        this.mountService = mountService;
    }

    private ControllerContext() {
    }

    public static ControllerContext getInstance() {
        return ControllerContext.INSTANCE;
    }

    private void checkPreconditions() {
        if (globalSchema == null) {
            throw new RestconfDocumentedException(Status.SERVICE_UNAVAILABLE);
        }
    }

    public void setSchemas(final SchemaContext schemas) {
        this.onGlobalContextUpdated(schemas);
    }

    public InstanceIdentifierContext toInstanceIdentifier(final String restconfInstance) {
        return this.toIdentifier(restconfInstance, false);
    }

    public SchemaContext getGlobalSchema() {
        return globalSchema;
    }

    public InstanceIdentifierContext toMountPointIdentifier(final String restconfInstance) {
        return this.toIdentifier(restconfInstance, true);
    }

    private InstanceIdentifierContext toIdentifier(final String restconfInstance, final boolean toMountPointIdentifier) {
        this.checkPreconditions();

        final List<String> pathArgs = urlPathArgsDecode(SLASH_SPLITTER.split(restconfInstance));
        omitFirstAndLastEmptyString(pathArgs);
        if (pathArgs.isEmpty()) {
            return null;
        }

        String first = pathArgs.iterator().next();
        final String startModule = ControllerContext.toModuleName(first);
        if (startModule == null) {
            throw new RestconfDocumentedException("First node in URI has to be in format \"moduleName:nodeName\"",
                    ErrorType.PROTOCOL, ErrorTag.INVALID_VALUE);
        }

        InstanceIdentifierBuilder builder = YangInstanceIdentifier.builder();
        Module latestModule = globalSchema.findModuleByName(startModule, null);
        InstanceIdentifierContext iiWithSchemaNode = this.collectPathArguments(builder, pathArgs, latestModule, null,
                toMountPointIdentifier);

        if (iiWithSchemaNode == null) {
            throw new RestconfDocumentedException("URI has bad format", ErrorType.PROTOCOL, ErrorTag.INVALID_VALUE);
        }

        return iiWithSchemaNode;
    }

    private static List<String> omitFirstAndLastEmptyString(final List<String> list) {
        if (list.isEmpty()) {
            return list;
        }

        String head = list.iterator().next();
        if (head.isEmpty()) {
            list.remove(0);
        }

        if (list.isEmpty()) {
            return list;
        }

        String last = list.get(list.size() - 1);
        if (last.isEmpty()) {
            list.remove(list.size() - 1);
        }

        return list;
    }
    public Module findModuleByName(final String moduleName) {
        this.checkPreconditions();
        Preconditions.checkArgument(moduleName != null && !moduleName.isEmpty());
        return globalSchema.findModuleByName(moduleName, null);
    }

    public Module findModuleByName(final DOMMountPoint mountPoint, final String moduleName) {
        Preconditions.checkArgument(moduleName != null && mountPoint != null);

        final SchemaContext mountPointSchema = mountPoint.getSchemaContext();
        if (mountPointSchema == null) {
            return null;
        }

        return mountPointSchema.findModuleByName(moduleName, null);
    }

    public Module findModuleByNamespace(final URI namespace) {
        this.checkPreconditions();
        Preconditions.checkArgument(namespace != null);
        return globalSchema.findModuleByNamespaceAndRevision(namespace, null);
    }

    public Module findModuleByNamespace(final DOMMountPoint mountPoint, final URI namespace) {
        Preconditions.checkArgument(namespace != null && mountPoint != null);

        final SchemaContext mountPointSchema = mountPoint.getSchemaContext();
        if (mountPointSchema == null) {
            return null;
        }

        return mountPointSchema.findModuleByNamespaceAndRevision(namespace, null);
    }

    public Module findModuleByNameAndRevision(final QName module) {
        this.checkPreconditions();
        Preconditions.checkArgument(module != null && module.getLocalName() != null && module.getRevision() != null);

        return globalSchema.findModuleByName(module.getLocalName(), module.getRevision());
    }

    public Module findModuleByNameAndRevision(final DOMMountPoint mountPoint, final QName module) {
        this.checkPreconditions();
        Preconditions.checkArgument(module != null && module.getLocalName() != null && module.getRevision() != null
                && mountPoint != null);

        SchemaContext schemaContext = mountPoint.getSchemaContext();
        return schemaContext == null ? null : schemaContext.findModuleByName(module.getLocalName(),
                module.getRevision());
    }

    public DataNodeContainer getDataNodeContainerFor(final YangInstanceIdentifier path) {
        this.checkPreconditions();

        final Iterable<PathArgument> elements = path.getPathArguments();
        PathArgument head = elements.iterator().next();
        final QName startQName = head.getNodeType();
        final Module initialModule = globalSchema.findModuleByNamespaceAndRevision(startQName.getNamespace(),
                startQName.getRevision());
        DataNodeContainer node = initialModule;
        for (final PathArgument element : elements) {
            QName _nodeType = element.getNodeType();
            final DataSchemaNode potentialNode = ControllerContext.childByQName(node, _nodeType);
            if (potentialNode == null || !ControllerContext.isListOrContainer(potentialNode)) {
                return null;
            }
            node = (DataNodeContainer) potentialNode;
        }

        return node;
    }

    public String toFullRestconfIdentifier(final YangInstanceIdentifier path, final DOMMountPoint mount) {
        this.checkPreconditions();

        final Iterable<PathArgument> elements = path.getPathArguments();
        final StringBuilder builder = new StringBuilder();
        PathArgument head = elements.iterator().next();
        final QName startQName = head.getNodeType();
        final SchemaContext schemaContext;
        if (mount != null) {
            schemaContext = mount.getSchemaContext();
        } else {
            schemaContext = globalSchema;
        }
        final Module initialModule = schemaContext.findModuleByNamespaceAndRevision(startQName.getNamespace(),
                startQName.getRevision());
        DataNodeContainer node = initialModule;
        for (final PathArgument element : elements) {
            if (!(element instanceof AugmentationIdentifier)) {
                QName _nodeType = element.getNodeType();
                final DataSchemaNode potentialNode = ControllerContext.childByQName(node, _nodeType);
                if (!(element instanceof NodeIdentifier && potentialNode instanceof ListSchemaNode)) {
                    if (!ControllerContext.isListOrContainer(potentialNode)) {
                        return null;
                    }
                    builder.append(convertToRestconfIdentifier(element, (DataNodeContainer) potentialNode, mount));
                    node = (DataNodeContainer) potentialNode;
                }
            }
        }

        return builder.toString();
    }

    public String findModuleNameByNamespace(final URI namespace) {
        this.checkPreconditions();

        final Module module = this.findModuleByNamespace(namespace);
        return module == null ? null : module.getName();
    }

    public String findModuleNameByNamespace(final DOMMountPoint mountPoint, final URI namespace) {
        final Module module = this.findModuleByNamespace(mountPoint, namespace);
        return module == null ? null : module.getName();
    }

    public URI findNamespaceByModuleName(final String moduleName) {
        final Module module = this.findModuleByName(moduleName);
        return module == null ? null : module.getNamespace();
    }

    public URI findNamespaceByModuleName(final DOMMountPoint mountPoint, final String moduleName) {
        final Module module = this.findModuleByName(mountPoint, moduleName);
        return module == null ? null : module.getNamespace();
    }

    public Set<Module> getAllModules(final DOMMountPoint mountPoint) {
        this.checkPreconditions();

        SchemaContext schemaContext = mountPoint == null ? null : mountPoint.getSchemaContext();
        return schemaContext == null ? null : schemaContext.getModules();
    }

    public Set<Module> getAllModules() {
        this.checkPreconditions();
        return globalSchema.getModules();
    }

    private static final CharSequence toRestconfIdentifier(final SchemaContext context, final QName qname) {
        final Module schema = context.findModuleByNamespaceAndRevision(qname.getNamespace(), qname.getRevision());
        return schema == null ? null : schema.getName() + ':' + qname.getLocalName();
    }

    public CharSequence toRestconfIdentifier(final QName qname, final DOMMountPoint mount) {
        final SchemaContext schema;
        if (mount != null) {
            schema = mount.getSchemaContext();
        } else {
            checkPreconditions();
            schema = globalSchema;
        }

        return toRestconfIdentifier(schema, qname);
    }

    public CharSequence toRestconfIdentifier(final QName qname) {
        this.checkPreconditions();

        return toRestconfIdentifier(globalSchema, qname);
    }

    public CharSequence toRestconfIdentifier(final DOMMountPoint mountPoint, final QName qname) {
        if (mountPoint == null) {
            return null;
        }

        return toRestconfIdentifier(mountPoint.getSchemaContext(), qname);
    }

    public Module getRestconfModule() {
        return findModuleByNameAndRevision(Draft02.RestConfModule.IETF_RESTCONF_QNAME);
    }

    private static final Predicate<GroupingDefinition> ERRORS_GROUPING_FILTER = new Predicate<GroupingDefinition>() {
        @Override
        public boolean apply(final GroupingDefinition g) {
            return Draft02.RestConfModule.ERRORS_GROUPING_SCHEMA_NODE.equals(g.getQName().getLocalName());
        }
    };

    public DataSchemaNode getRestconfModuleErrorsSchemaNode() {
        Module restconfModule = getRestconfModule();
        if (restconfModule == null) {
            return null;
        }

        Set<GroupingDefinition> groupings = restconfModule.getGroupings();

        Iterable<GroupingDefinition> filteredGroups = Iterables.filter(groupings, ERRORS_GROUPING_FILTER);

        final GroupingDefinition restconfGrouping = Iterables.getFirst(filteredGroups, null);

        List<DataSchemaNode> instanceDataChildrenByName = findInstanceDataChildrenByName(restconfGrouping,
                Draft02.RestConfModule.ERRORS_CONTAINER_SCHEMA_NODE);
        return Iterables.getFirst(instanceDataChildrenByName, null);
    }

    private static final Predicate<GroupingDefinition> GROUPING_FILTER = new Predicate<GroupingDefinition>() {
        @Override
        public boolean apply(final GroupingDefinition g) {
            return Draft02.RestConfModule.RESTCONF_GROUPING_SCHEMA_NODE.equals(g.getQName().getLocalName());
        }
    };

    public DataSchemaNode getRestconfModuleRestConfSchemaNode(final Module inRestconfModule, final String schemaNodeName) {
        Module restconfModule = inRestconfModule;
        if (restconfModule == null) {
            restconfModule = getRestconfModule();
        }

        if (restconfModule == null) {
            return null;
        }

        Set<GroupingDefinition> groupings = restconfModule.getGroupings();
        Iterable<GroupingDefinition> filteredGroups = Iterables.filter(groupings, GROUPING_FILTER);
        final GroupingDefinition restconfGrouping = Iterables.getFirst(filteredGroups, null);

        List<DataSchemaNode> instanceDataChildrenByName = findInstanceDataChildrenByName(restconfGrouping,
                Draft02.RestConfModule.RESTCONF_CONTAINER_SCHEMA_NODE);
        final DataSchemaNode restconfContainer = Iterables.getFirst(instanceDataChildrenByName, null);

        if (Objects.equal(schemaNodeName, Draft02.RestConfModule.OPERATIONS_CONTAINER_SCHEMA_NODE)) {
            List<DataSchemaNode> instances = findInstanceDataChildrenByName(
                    ((DataNodeContainer) restconfContainer), Draft02.RestConfModule.OPERATIONS_CONTAINER_SCHEMA_NODE);
            return Iterables.getFirst(instances, null);
        } else if (Objects.equal(schemaNodeName, Draft02.RestConfModule.STREAMS_CONTAINER_SCHEMA_NODE)) {
            List<DataSchemaNode> instances = findInstanceDataChildrenByName(
                    ((DataNodeContainer) restconfContainer), Draft02.RestConfModule.STREAMS_CONTAINER_SCHEMA_NODE);
            return Iterables.getFirst(instances, null);
        } else if (Objects.equal(schemaNodeName, Draft02.RestConfModule.STREAM_LIST_SCHEMA_NODE)) {
            List<DataSchemaNode> instances = findInstanceDataChildrenByName(
                    ((DataNodeContainer) restconfContainer), Draft02.RestConfModule.STREAMS_CONTAINER_SCHEMA_NODE);
            final DataSchemaNode modules = Iterables.getFirst(instances, null);
            instances = findInstanceDataChildrenByName(((DataNodeContainer) modules),
                    Draft02.RestConfModule.STREAM_LIST_SCHEMA_NODE);
            return Iterables.getFirst(instances, null);
        } else if (Objects.equal(schemaNodeName, Draft02.RestConfModule.MODULES_CONTAINER_SCHEMA_NODE)) {
            List<DataSchemaNode> instances = findInstanceDataChildrenByName(
                    ((DataNodeContainer) restconfContainer), Draft02.RestConfModule.MODULES_CONTAINER_SCHEMA_NODE);
            return Iterables.getFirst(instances, null);
        } else if (Objects.equal(schemaNodeName, Draft02.RestConfModule.MODULE_LIST_SCHEMA_NODE)) {
            List<DataSchemaNode> instances = findInstanceDataChildrenByName(
                    ((DataNodeContainer) restconfContainer), Draft02.RestConfModule.MODULES_CONTAINER_SCHEMA_NODE);
            final DataSchemaNode modules = Iterables.getFirst(instances, null);
            instances = findInstanceDataChildrenByName(((DataNodeContainer) modules),
                    Draft02.RestConfModule.MODULE_LIST_SCHEMA_NODE);
            return Iterables.getFirst(instances, null);
        } else if (Objects.equal(schemaNodeName, Draft02.RestConfModule.STREAMS_CONTAINER_SCHEMA_NODE)) {
            List<DataSchemaNode> instances = findInstanceDataChildrenByName(
                    ((DataNodeContainer) restconfContainer), Draft02.RestConfModule.STREAMS_CONTAINER_SCHEMA_NODE);
            return Iterables.getFirst(instances, null);
        }

        return null;
    }

    private static DataSchemaNode childByQName(final ChoiceNode container, final QName name) {
        for (final ChoiceCaseNode caze : container.getCases()) {
            final DataSchemaNode ret = ControllerContext.childByQName(caze, name);
            if (ret != null) {
                return ret;
            }
        }

        return null;
    }

    private static DataSchemaNode childByQName(final ChoiceCaseNode container, final QName name) {
        return container.getDataChildByName(name);
    }

    private static DataSchemaNode childByQName(final ContainerSchemaNode container, final QName name) {
        return ControllerContext.dataNodeChildByQName(container, name);
    }

    private static DataSchemaNode childByQName(final ListSchemaNode container, final QName name) {
        return ControllerContext.dataNodeChildByQName(container, name);
    }

    private static DataSchemaNode childByQName(final Module container, final QName name) {
        return ControllerContext.dataNodeChildByQName(container, name);
    }

    private static DataSchemaNode childByQName(final DataSchemaNode container, final QName name) {
        return null;
    }

    private static DataSchemaNode dataNodeChildByQName(final DataNodeContainer container, final QName name) {
        DataSchemaNode ret = container.getDataChildByName(name);
        if (ret == null) {
            for (final DataSchemaNode node : container.getChildNodes()) {
                if ((node instanceof ChoiceNode)) {
                    final ChoiceNode choiceNode = ((ChoiceNode) node);
                    DataSchemaNode childByQName = ControllerContext.childByQName(choiceNode, name);
                    if (childByQName != null) {
                        return childByQName;
                    }
                }
            }
        }
        return ret;
    }

    private String toUriString(final Object object, final LeafSchemaNode leafNode, final DOMMountPoint mount) throws UnsupportedEncodingException {
        final Codec<Object, Object> codec = RestCodec.from(leafNode.getType(), mount);
        return object == null ? "" : URLEncoder.encode(codec.serialize(object).toString(), ControllerContext.URI_ENCODING_CHAR_SET);
    }

    private InstanceIdentifierContext collectPathArguments(final InstanceIdentifierBuilder builder,
            final List<String> strings, final DataNodeContainer parentNode, final DOMMountPoint mountPoint,
            final boolean returnJustMountPoint) {
        Preconditions.<List<String>> checkNotNull(strings);

        if (parentNode == null) {
            return null;
        }

        if (strings.isEmpty()) {
            return new InstanceIdentifierContext(builder.toInstance(), ((DataSchemaNode) parentNode), mountPoint,mountPoint != null ? mountPoint.getSchemaContext() : globalSchema);
        }

        String head = strings.iterator().next();
        final String nodeName = toNodeName(head);
        final String moduleName = ControllerContext.toModuleName(head);

        DataSchemaNode targetNode = null;
        if (!Strings.isNullOrEmpty(moduleName)) {
            if (Objects.equal(moduleName, ControllerContext.MOUNT_MODULE)
                    && Objects.equal(nodeName, ControllerContext.MOUNT_NODE)) {
                if (mountPoint != null) {
                    throw new RestconfDocumentedException("Restconf supports just one mount point in URI.",
                            ErrorType.APPLICATION, ErrorTag.OPERATION_NOT_SUPPORTED);
                }

                if (mountService == null) {
                    throw new RestconfDocumentedException(
                            "MountService was not found. Finding behind mount points does not work.",
                            ErrorType.APPLICATION, ErrorTag.OPERATION_NOT_SUPPORTED);
                }

                final YangInstanceIdentifier partialPath = builder.toInstance();
                final Optional<DOMMountPoint> mountOpt = mountService.getMountPoint(partialPath);
                if (!mountOpt.isPresent()) {
                    LOG.debug("Instance identifier to missing mount point: {}", partialPath);
                    throw new RestconfDocumentedException("Mount point does not exist.", ErrorType.PROTOCOL,
                            ErrorTag.UNKNOWN_ELEMENT);
                }
                DOMMountPoint mount = mountOpt.get();

                final SchemaContext mountPointSchema = mount.getSchemaContext();
                if (mountPointSchema == null) {
                    throw new RestconfDocumentedException("Mount point does not contain any schema with modules.",
                            ErrorType.APPLICATION, ErrorTag.UNKNOWN_ELEMENT);
                }

                if (returnJustMountPoint) {
                    YangInstanceIdentifier instance = YangInstanceIdentifier.builder().toInstance();
                    return new InstanceIdentifierContext(instance, mountPointSchema, mount,mountPointSchema);
                }

                if (strings.size() == 1) {
                    YangInstanceIdentifier instance = YangInstanceIdentifier.builder().toInstance();
                    return new InstanceIdentifierContext(instance, mountPointSchema, mount,mountPointSchema);
                }

                final String moduleNameBehindMountPoint = toModuleName(strings.get(1));
                if (moduleNameBehindMountPoint == null) {
                    throw new RestconfDocumentedException(
                            "First node after mount point in URI has to be in format \"moduleName:nodeName\"",
                            ErrorType.PROTOCOL, ErrorTag.INVALID_VALUE);
                }

                final Module moduleBehindMountPoint = mountPointSchema.findModuleByName(moduleNameBehindMountPoint, null);
                if (moduleBehindMountPoint == null) {
                    throw new RestconfDocumentedException("\"" + moduleName
                            + "\" module does not exist in mount point.", ErrorType.PROTOCOL, ErrorTag.UNKNOWN_ELEMENT);
                }

                List<String> subList = strings.subList(1, strings.size());
                return this.collectPathArguments(YangInstanceIdentifier.builder(), subList, moduleBehindMountPoint, mount,
                        returnJustMountPoint);
            }

            Module module = null;
            if (mountPoint == null) {
                module = globalSchema.findModuleByName(moduleName, null);
                if (module == null) {
                    throw new RestconfDocumentedException("\"" + moduleName + "\" module does not exist.",
                            ErrorType.PROTOCOL, ErrorTag.UNKNOWN_ELEMENT);
                }
            } else {
                SchemaContext schemaContext = mountPoint.getSchemaContext();
                if (schemaContext != null) {
                    module = schemaContext.findModuleByName(moduleName, null);
                } else {
                    module = null;
                }
                if (module == null) {
                    throw new RestconfDocumentedException("\"" + moduleName
                            + "\" module does not exist in mount point.", ErrorType.PROTOCOL, ErrorTag.UNKNOWN_ELEMENT);
                }
            }

            targetNode = findInstanceDataChildByNameAndNamespace(parentNode, nodeName, module.getNamespace());
            if (targetNode == null) {
                throw new RestconfDocumentedException("URI has bad format. Possible reasons:\n" + " 1. \"" + head
                        + "\" was not found in parent data node.\n" + " 2. \"" + head
                        + "\" is behind mount point. Then it should be in format \"/" + MOUNT + "/" + head + "\".",
                        ErrorType.PROTOCOL, ErrorTag.INVALID_VALUE);
            }
        } else {
            final List<DataSchemaNode> potentialSchemaNodes = findInstanceDataChildrenByName(parentNode, nodeName);
            if (potentialSchemaNodes.size() > 1) {
                final StringBuilder strBuilder = new StringBuilder();
                for (final DataSchemaNode potentialNodeSchema : potentialSchemaNodes) {
                    strBuilder.append("   ").append(potentialNodeSchema.getQName().getNamespace()).append("\n");
                }

                throw new RestconfDocumentedException(
                        "URI has bad format. Node \""
                                + nodeName
                                + "\" is added as augment from more than one module. "
                                + "Therefore the node must have module name and it has to be in format \"moduleName:nodeName\"."
                                + "\nThe node is added as augment from modules with namespaces:\n"
                                + strBuilder.toString(), ErrorType.PROTOCOL, ErrorTag.INVALID_VALUE);
            }

            if (potentialSchemaNodes.isEmpty()) {
                throw new RestconfDocumentedException("\"" + nodeName + "\" in URI was not found in parent data node",
                        ErrorType.PROTOCOL, ErrorTag.UNKNOWN_ELEMENT);
            }

            targetNode = potentialSchemaNodes.iterator().next();
        }

        if (!ControllerContext.isListOrContainer(targetNode)) {
            throw new RestconfDocumentedException("URI has bad format. Node \"" + head
                    + "\" must be Container or List yang type.", ErrorType.PROTOCOL, ErrorTag.INVALID_VALUE);
        }

        int consumed = 1;
        if ((targetNode instanceof ListSchemaNode)) {
            final ListSchemaNode listNode = ((ListSchemaNode) targetNode);
            final int keysSize = listNode.getKeyDefinition().size();
            if ((strings.size() - consumed) < keysSize) {
                throw new RestconfDocumentedException("Missing key for list \"" + listNode.getQName().getLocalName()
                        + "\".", ErrorType.PROTOCOL, ErrorTag.DATA_MISSING);
            }

            final List<String> uriKeyValues = strings.subList(consumed, consumed + keysSize);
            final HashMap<QName, Object> keyValues = new HashMap<QName, Object>();
            int i = 0;
            for (final QName key : listNode.getKeyDefinition()) {
                {
                    final String uriKeyValue = uriKeyValues.get(i);
                    if (uriKeyValue.equals(NULL_VALUE)) {
                        throw new RestconfDocumentedException("URI has bad format. List \""
                                + listNode.getQName().getLocalName() + "\" cannot contain \"null\" value as a key.",
                                ErrorType.PROTOCOL, ErrorTag.INVALID_VALUE);
                    }

                    this.addKeyValue(keyValues, listNode.getDataChildByName(key), uriKeyValue, mountPoint);
                    i++;
                }
            }

            consumed = consumed + i;
            builder.nodeWithKey(targetNode.getQName(), keyValues);
        } else {
            builder.node(targetNode.getQName());
        }

        if ((targetNode instanceof DataNodeContainer)) {
            final List<String> remaining = strings.subList(consumed, strings.size());
            return this.collectPathArguments(builder, remaining, ((DataNodeContainer) targetNode), mountPoint,
                    returnJustMountPoint);
        }

        return new InstanceIdentifierContext(builder.toInstance(), targetNode, mountPoint,mountPoint != null ? mountPoint.getSchemaContext() : globalSchema);
    }

    public static DataSchemaNode findInstanceDataChildByNameAndNamespace(final DataNodeContainer container, final String name,
            final URI namespace) {
        Preconditions.<URI> checkNotNull(namespace);

        final List<DataSchemaNode> potentialSchemaNodes = findInstanceDataChildrenByName(container, name);

        Predicate<DataSchemaNode> filter = new Predicate<DataSchemaNode>() {
            @Override
            public boolean apply(final DataSchemaNode node) {
                return Objects.equal(node.getQName().getNamespace(), namespace);
            }
        };

        Iterable<DataSchemaNode> result = Iterables.filter(potentialSchemaNodes, filter);
        return Iterables.getFirst(result, null);
    }

    public static List<DataSchemaNode> findInstanceDataChildrenByName(final DataNodeContainer container, final String name) {
        Preconditions.<DataNodeContainer> checkNotNull(container);
        Preconditions.<String> checkNotNull(name);

        List<DataSchemaNode> instantiatedDataNodeContainers = new ArrayList<DataSchemaNode>();
        collectInstanceDataNodeContainers(instantiatedDataNodeContainers, container, name);
        return instantiatedDataNodeContainers;
    }

    private static final Function<ChoiceNode, Set<ChoiceCaseNode>> CHOICE_FUNCTION = new Function<ChoiceNode, Set<ChoiceCaseNode>>() {
        @Override
        public Set<ChoiceCaseNode> apply(final ChoiceNode node) {
            return node.getCases();
        }
    };

    private static void collectInstanceDataNodeContainers(final List<DataSchemaNode> potentialSchemaNodes,
            final DataNodeContainer container, final String name) {

        Predicate<DataSchemaNode> filter = new Predicate<DataSchemaNode>() {
            @Override
            public boolean apply(final DataSchemaNode node) {
                return Objects.equal(node.getQName().getLocalName(), name);
            }
        };

        Iterable<DataSchemaNode> nodes = Iterables.filter(container.getChildNodes(), filter);

        // Can't combine this loop with the filter above because the filter is
        // lazily-applied by Iterables.filter.
        for (final DataSchemaNode potentialNode : nodes) {
            if (isInstantiatedDataSchema(potentialNode)) {
                potentialSchemaNodes.add(potentialNode);
            }
        }

        Iterable<ChoiceNode> choiceNodes = Iterables.filter(container.getChildNodes(), ChoiceNode.class);
        Iterable<Set<ChoiceCaseNode>> map = Iterables.transform(choiceNodes, CHOICE_FUNCTION);

        final Iterable<ChoiceCaseNode> allCases = Iterables.<ChoiceCaseNode> concat(map);
        for (final ChoiceCaseNode caze : allCases) {
            collectInstanceDataNodeContainers(potentialSchemaNodes, caze, name);
        }
    }

    public static boolean isInstantiatedDataSchema(final DataSchemaNode node) {
        return node instanceof LeafSchemaNode || node instanceof LeafListSchemaNode
                || node instanceof ContainerSchemaNode || node instanceof ListSchemaNode
                || node instanceof AnyXmlSchemaNode;
    }

    private void addKeyValue(final HashMap<QName, Object> map, final DataSchemaNode node, final String uriValue,
            final DOMMountPoint mountPoint) {
        Preconditions.<String> checkNotNull(uriValue);
        Preconditions.checkArgument((node instanceof LeafSchemaNode));

        final String urlDecoded = urlPathArgDecode(uriValue);
        final TypeDefinition<? extends Object> typedef = ((LeafSchemaNode) node).getType();
        Codec<Object, Object> codec = RestCodec.from(typedef, mountPoint);

        Object decoded = codec == null ? null : codec.deserialize(urlDecoded);
        String additionalInfo = "";
        if (decoded == null) {
            TypeDefinition<? extends Object> baseType = RestUtil.resolveBaseTypeFrom(typedef);
            if ((baseType instanceof IdentityrefTypeDefinition)) {
                decoded = this.toQName(urlDecoded);
                additionalInfo = "For key which is of type identityref it should be in format module_name:identity_name.";
            }
        }

        if (decoded == null) {
            throw new RestconfDocumentedException(uriValue + " from URI can't be resolved. " + additionalInfo,
                    ErrorType.PROTOCOL, ErrorTag.INVALID_VALUE);
        }

        map.put(node.getQName(), decoded);
    }

    private static String toModuleName(final String str) {
        final int idx = str.indexOf(':');
        if (idx == -1) {
            return null;
        }

        // Make sure there is only one occurrence
        if (str.indexOf(':', idx + 1) != -1) {
            return null;
        }

        return str.substring(0, idx);
    }

    private static String toNodeName(final String str) {
        final int idx = str.indexOf(':');
        if (idx == -1) {
            return str;
        }

        // Make sure there is only one occurrence
        if (str.indexOf(':', idx + 1) != -1) {
            return str;
        }

        return str.substring(idx + 1);
    }

    private QName toQName(final String name) {
        final String module = toModuleName(name);
        final String node = toNodeName(name);
        final Module m = globalSchema.findModuleByName(module, null);
        return m == null ? null : QName.create(m.getQNameModule(), node);
    }

    private static boolean isListOrContainer(final DataSchemaNode node) {
        return node instanceof ListSchemaNode || node instanceof ContainerSchemaNode;
    }

    public RpcDefinition getRpcDefinition(final String name) {
        final QName validName = this.toQName(name);
        return validName == null ? null : this.qnameToRpc.get().get(validName);
    }

    @Override
    public void onGlobalContextUpdated(final SchemaContext context) {
        if (context != null) {
            final Collection<RpcDefinition> defs = context.getOperations();
            final Map<QName, RpcDefinition> newMap = new HashMap<>(defs.size());

            for (final RpcDefinition operation : defs) {
                newMap.put(operation.getQName(), operation);
            }

            // FIXME: still not completely atomic
            this.qnameToRpc.set(ImmutableMap.copyOf(newMap));
            this.setGlobalSchema(context);
        }
    }

    public static List<String> urlPathArgsDecode(final Iterable<String> strings) {
        try {
            List<String> decodedPathArgs = new ArrayList<String>();
            for (final String pathArg : strings) {
                String _decode = URLDecoder.decode(pathArg, URI_ENCODING_CHAR_SET);
                decodedPathArgs.add(_decode);
            }
            return decodedPathArgs;
        } catch (UnsupportedEncodingException e) {
            throw new RestconfDocumentedException("Invalid URL path '" + strings + "': " + e.getMessage(),
                    ErrorType.PROTOCOL, ErrorTag.INVALID_VALUE);
        }
    }

    public String urlPathArgDecode(final String pathArg) {
        if (pathArg != null) {
            try {
                return URLDecoder.decode(pathArg, URI_ENCODING_CHAR_SET);
            } catch (UnsupportedEncodingException e) {
                throw new RestconfDocumentedException("Invalid URL path arg '" + pathArg + "': " + e.getMessage(),
                        ErrorType.PROTOCOL, ErrorTag.INVALID_VALUE);
            }
        }

        return null;
    }

    private CharSequence convertToRestconfIdentifier(final PathArgument argument, final DataNodeContainer node, final DOMMountPoint mount) {
        if (argument instanceof NodeIdentifier && node instanceof ContainerSchemaNode) {
            return convertToRestconfIdentifier((NodeIdentifier) argument, mount);
        } else if (argument instanceof NodeIdentifierWithPredicates && node instanceof ListSchemaNode) {
            return convertToRestconfIdentifier((NodeIdentifierWithPredicates) argument, (ListSchemaNode) node, mount);
        } else if (argument != null && node != null) {
            throw new IllegalArgumentException("Conversion of generic path argument is not supported");
        } else {
            throw new IllegalArgumentException("Unhandled parameter types: "
                    + Arrays.<Object> asList(argument, node).toString());
        }
    }

    private CharSequence convertToRestconfIdentifier(final NodeIdentifier argument, final DOMMountPoint mount) {
        return "/" + this.toRestconfIdentifier(argument.getNodeType(), mount);
    }

    private CharSequence convertToRestconfIdentifier(final NodeIdentifierWithPredicates argument,
            final ListSchemaNode node, final DOMMountPoint mount) {
        QName nodeType = argument.getNodeType();
        final CharSequence nodeIdentifier = this.toRestconfIdentifier(nodeType, mount);
        final Map<QName, Object> keyValues = argument.getKeyValues();

        StringBuilder builder = new StringBuilder();
        builder.append('/');
        builder.append(nodeIdentifier);
        builder.append('/');

        List<QName> keyDefinition = node.getKeyDefinition();
        boolean hasElements = false;
        for (final QName key : keyDefinition) {
            for (DataSchemaNode listChild : node.getChildNodes()) {
                if (listChild.getQName().equals(key)) {
                    if (!hasElements) {
                        hasElements = true;
                    } else {
                        builder.append('/');
                    }

                    try {
                        Preconditions.checkState(listChild instanceof LeafSchemaNode, "List key has to consist of leaves");
                        builder.append(this.toUriString(keyValues.get(key), (LeafSchemaNode)listChild, mount));
                    } catch (UnsupportedEncodingException e) {
                        LOG.error("Error parsing URI: {}", keyValues.get(key), e);
                        return null;
                    }
                    break;
                }
            }
        }

        return builder.toString();
    }

    private static DataSchemaNode childByQName(final Object container, final QName name) {
        if (container instanceof ChoiceCaseNode) {
            return childByQName((ChoiceCaseNode) container, name);
        } else if (container instanceof ChoiceNode) {
            return childByQName((ChoiceNode) container, name);
        } else if (container instanceof ContainerSchemaNode) {
            return childByQName((ContainerSchemaNode) container, name);
        } else if (container instanceof ListSchemaNode) {
            return childByQName((ListSchemaNode) container, name);
        } else if (container instanceof DataSchemaNode) {
            return childByQName((DataSchemaNode) container, name);
        } else if (container instanceof Module) {
            return childByQName((Module) container, name);
        } else {
            throw new IllegalArgumentException("Unhandled parameter types: "
                    + Arrays.<Object> asList(container, name).toString());
        }
    }

    public Entry<YangInstanceIdentifier, NormalizedNode<?, ?>> toNormalized(final YangInstanceIdentifier legacy,
            final CompositeNode compositeNode) {
        try {
            return dataNormalizer.toNormalized(legacy, compositeNode);
        } catch (NullPointerException e) {
            throw new RestconfDocumentedException("Data normalizer isn't set. Normalization isn't possible", e);
        }
    }

    public YangInstanceIdentifier toNormalized(final YangInstanceIdentifier legacy) {
        try {
            return dataNormalizer.toNormalized(legacy);
        } catch (NullPointerException e) {
            throw new RestconfDocumentedException("Data normalizer isn't set. Normalization isn't possible", e);
        }
    }

    public CompositeNode toLegacy(final YangInstanceIdentifier instanceIdentifier,
            final NormalizedNode<?,?> normalizedNode) {
        try {
            return dataNormalizer.toLegacy(instanceIdentifier, normalizedNode);
        } catch (NullPointerException e) {
            throw new RestconfDocumentedException("Data normalizer isn't set. Normalization isn't possible", e);
        }
    }

    public DataNormalizationOperation<?> getRootOperation() {
        return dataNormalizer.getRootOperation();
    }

}
