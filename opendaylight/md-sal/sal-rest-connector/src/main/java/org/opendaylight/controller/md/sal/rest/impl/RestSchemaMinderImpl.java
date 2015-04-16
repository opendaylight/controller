/**
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.md.sal.rest.impl;

import static org.opendaylight.controller.md.sal.rest.common.RestconfInternalConstants.MOUNT;
import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;
import org.opendaylight.controller.md.sal.dom.api.DOMMountPoint;
import org.opendaylight.controller.md.sal.dom.api.DOMMountPointService;
import org.opendaylight.controller.md.sal.rest.RestSchemaMinder;
import org.opendaylight.controller.md.sal.rest.common.RestconfInternalConstants;
import org.opendaylight.controller.md.sal.rest.common.RestconfParsingUtils;
import org.opendaylight.controller.md.sal.rest.common.RestconfSchemaNodeUtils;
import org.opendaylight.controller.md.sal.rest.common.RestconfValidationUtils;
import org.opendaylight.controller.sal.rest.api.Draft02;
import org.opendaylight.controller.sal.rest.impl.RestUtil;
import org.opendaylight.controller.sal.restconf.impl.InstanceIdentifierContext;
import org.opendaylight.controller.sal.restconf.impl.RestCodec;
import org.opendaylight.controller.sal.restconf.impl.RestconfDocumentedException;
import org.opendaylight.controller.sal.restconf.impl.RestconfError.ErrorTag;
import org.opendaylight.controller.sal.restconf.impl.RestconfError.ErrorType;
import org.opendaylight.yangtools.concepts.Codec;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.InstanceIdentifierBuilder;
import org.opendaylight.yangtools.yang.model.api.ContainerSchemaNode;
import org.opendaylight.yangtools.yang.model.api.DataNodeContainer;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.GroupingDefinition;
import org.opendaylight.yangtools.yang.model.api.LeafSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ListSchemaNode;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.RpcDefinition;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaNode;
import org.opendaylight.yangtools.yang.model.api.TypeDefinition;
import org.opendaylight.yangtools.yang.model.api.type.IdentityrefTypeDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * sal-rest-connector
 * org.opendaylight.controller.md.sal.rest
 *
 *
 *
 * @author <a href="mailto:vdemcak@cisco.com">Vaclav Demcak</a>
 *
 * Created: Mar 22, 2015
 */
public class RestSchemaMinderImpl implements RestSchemaMinder {
    private static final Logger LOG = LoggerFactory.getLogger(RestSchemaMinderImpl.class);

    private final DOMMountPointService mountService;
    private volatile SchemaContext globalSchema;
    private volatile Map<QName, RpcDefinition> qnameToRpc;

    public RestSchemaMinderImpl(@Nonnull final DOMMountPointService mountServ,
            @Nonnull final SchemaContext schemaContext) {
        mountService = Preconditions.checkNotNull(mountServ);
        tell(schemaContext);
    }

    @Override
    public synchronized void tell(@Nonnull final SchemaContext schemaContext) {
        final Set<RpcDefinition> defs = schemaContext.getOperations();
        final Map<QName, RpcDefinition> newMap = new HashMap<>(defs.size());
        for (final RpcDefinition oper : defs) {
            newMap.put(oper.getQName(), oper);
        }
        globalSchema = Preconditions.checkNotNull(schemaContext);
        qnameToRpc = ImmutableMap.copyOf(newMap);
    }

    public RpcDefinition getRpcDefinition(final String name) {
        final QName qname = RestconfParsingUtils.toQName(name);
        return qname != null ? qnameToRpc.get(qname) : null;
    }

    @Override
    public Module getRestconfModule() {
        final QName restQName = Draft02.RestConfModule.IETF_RESTCONF_QNAME;
        final Module restconfModule = globalSchema.findModuleByName(restQName.getLocalName(), restQName.getRevision());
        RestconfValidationUtils.checkDocumentedError(restconfModule != null, ErrorType.APPLICATION,
                ErrorTag.OPERATION_NOT_SUPPORTED, "ietf-restconf module was not found.");
        return restconfModule;
    }

    @Override
    public ListSchemaNode getStreamListSchemaNode() {
        final DataSchemaNode slsn = getRestconfModuleRestConfSchemaNode(Draft02.RestConfModule.STREAM_LIST_SCHEMA_NODE);
        RestconfValidationUtils.checkDocumentedError(slsn instanceof ListSchemaNode, ErrorType.APPLICATION,
                ErrorTag.OPERATION_NOT_SUPPORTED, "ietf-restconf module doesn't contain stream list schema node.");
        return (ListSchemaNode) slsn;
    }

    @Override
    public ContainerSchemaNode getStreamContainerSchemaNode() {
        final DataSchemaNode scsn = getRestconfModuleRestConfSchemaNode(Draft02.RestConfModule.STREAMS_CONTAINER_SCHEMA_NODE);
        RestconfValidationUtils.checkDocumentedError(scsn instanceof ContainerSchemaNode, ErrorType.APPLICATION,
                ErrorTag.OPERATION_NOT_SUPPORTED, "ietf-restconf module doesn't contain stream container schema node.");
        return (ContainerSchemaNode) scsn;
    }

    @Override
    public ListSchemaNode getModuleListSchemaNode() {
        final DataSchemaNode mlsn = getRestconfModuleRestConfSchemaNode(Draft02.RestConfModule.MODULE_LIST_SCHEMA_NODE);
        RestconfValidationUtils.checkDocumentedError(mlsn instanceof ListSchemaNode, ErrorType.APPLICATION,
                ErrorTag.OPERATION_NOT_SUPPORTED, "ietf-restconf module doesn't contain module list schema node.");
        return (ListSchemaNode) mlsn;
    }

    @Override
    public ContainerSchemaNode getModuleContainerSchemaNode() {
        final DataSchemaNode mcsn = getRestconfModuleRestConfSchemaNode(Draft02.RestConfModule.MODULES_CONTAINER_SCHEMA_NODE);
        RestconfValidationUtils.checkDocumentedError(mcsn instanceof ContainerSchemaNode, ErrorType.APPLICATION,
                ErrorTag.OPERATION_NOT_SUPPORTED, "ietf-restconf module doesn't contain module container schema node.");
        return (ContainerSchemaNode) mcsn;
    }

    @Override
    public DOMMountPoint parseUriRequestToMountPoint(final String requestUriIdentifier) {
        RestconfValidationUtils.checkDocumentedError(requestUriIdentifier != null, ErrorType.PROTOCOL, ErrorTag.INVALID_VALUE,
                "Nullable mountpoint navigation URL");
        RestconfValidationUtils.checkDocumentedError(( ! requestUriIdentifier.contains(RestconfInternalConstants.MOUNT)),
                ErrorType.PROTOCOL, ErrorTag.INVALID_VALUE, "Missing mountpoint marker in navigation URL");
        RestconfValidationUtils.checkDocumentedError(mountService != null, ErrorType.APPLICATION, ErrorTag.OPERATION_NOT_SUPPORTED,
                "MountService was not found. Finding behind mount points does not work.");

        final String uriToMountpoint = requestUriIdentifier.substring(
                0, requestUriIdentifier.indexOf(RestconfInternalConstants.MOUNT));
        final List<String> listPathArg = RestconfParsingUtils.urlPathArgsDecode(
                RestconfInternalConstants.SLASH_SPLITTER.split(uriToMountpoint));
        final InstanceIdentifierContext<?> identCx = parseUriRequest(listPathArg, null, globalSchema, YangInstanceIdentifier.builder());

        final Optional<DOMMountPoint> mountOpt = mountService.getMountPoint(identCx.getInstanceIdentifier());
        RestconfValidationUtils.checkDocumentedError(mountOpt.isPresent(), ErrorType.PROTOCOL,
                ErrorTag.UNKNOWN_ELEMENT, "Mount point does not exist.");

        return mountOpt.get();
    }

    @Override
    public InstanceIdentifierContext<?> parseUriRequest(final String requestUriIdentifier) {
        if (Strings.isNullOrEmpty(requestUriIdentifier)) {
            return new InstanceIdentifierContext<>(RestconfInternalConstants.ROOT, globalSchema, null, globalSchema);
        }

        DOMMountPoint mountPoint = null;
        SchemaContext schemaContext = globalSchema;
        final List<String> pathArgs;

        if (requestUriIdentifier.contains(RestconfInternalConstants.MOUNT)) {
            /* retrieve string without first mountPoint */
            final int mountEndIndex = requestUriIdentifier.indexOf(RestconfInternalConstants.MOUNT) + RestconfInternalConstants.MOUNT.length();
            final String postMountPointUri = requestUriIdentifier.substring(mountEndIndex, requestUriIdentifier.length());

            RestconfValidationUtils.checkDocumentedError(postMountPointUri.contains(RestconfInternalConstants.MOUNT),
                    ErrorType.APPLICATION, ErrorTag.OPERATION_NOT_SUPPORTED, "Restconf supports just one mount point in URI.");

            mountPoint = parseUriRequestToMountPoint(requestUriIdentifier);
            schemaContext = mountPoint.getSchemaContext();
            pathArgs = RestconfParsingUtils.urlPathArgsDecode(
                    RestconfInternalConstants.SLASH_SPLITTER.split(postMountPointUri));
            final String moduleNameBehindMountPoint = RestconfParsingUtils.toModuleName(pathArgs.get(0));
            RestconfValidationUtils.checkDocumentedError(moduleNameBehindMountPoint != null, ErrorType.PROTOCOL, ErrorTag.INVALID_VALUE,
                    "First node after mount point in URI has to be in format \"moduleName:nodeName\"");
        } else {
            pathArgs = RestconfParsingUtils.urlPathArgsDecode(
                    RestconfInternalConstants.SLASH_SPLITTER.split(requestUriIdentifier));
        }

        final String first = pathArgs.iterator().next();
        final String startModule = RestconfParsingUtils.toModuleName(first);
        RestconfValidationUtils.checkDocumentedError(startModule != null, ErrorType.PROTOCOL, ErrorTag.INVALID_VALUE,
                "First node in URI has to be in format \"moduleName:nodeName\"");

        final InstanceIdentifierContext<?> iiContext = parseUriRequest(pathArgs, mountPoint, schemaContext, YangInstanceIdentifier.builder());
        RestconfValidationUtils.checkDocumentedError(iiContext != null, ErrorType.PROTOCOL,
                ErrorTag.INVALID_VALUE, "URI has bad format");
        return iiContext;
    }

    private InstanceIdentifierContext<?> parseUriRequest(final List<String> pathArgs, final DOMMountPoint mountPoint,
            final DataNodeContainer parentNode, final InstanceIdentifierBuilder yiiBuilder) {

        Preconditions.checkArgument(pathArgs != null);
        Preconditions.checkArgument(parentNode != null);
        Preconditions.checkArgument(yiiBuilder != null);

        final SchemaContext schemaContext = mountPoint != null ? mountPoint.getSchemaContext() : globalSchema;

        if (pathArgs.isEmpty()) {
            return new InstanceIdentifierContext<>(yiiBuilder.build(), (DataSchemaNode)parentNode, mountPoint, schemaContext);
        }

        final String head = pathArgs.iterator().next();
        final String nodeName = RestconfParsingUtils.toNodeName(head);
        final String moduleName = RestconfParsingUtils.toModuleName(head);

        final DataSchemaNode targetNode;

        if (Strings.isNullOrEmpty(moduleName)) {
            final Module module = schemaContext.findModuleByName(moduleName, null);
            RestconfValidationUtils.checkDocumentedError(module != null, ErrorType.PROTOCOL, ErrorTag.UNKNOWN_ELEMENT,
                    "\"" + moduleName + "\" module does not exist.");

            targetNode = RestconfSchemaNodeUtils.findInstanceDataChildByNameAndNamespace(module, nodeName, module.getNamespace());
            if (targetNode == null) {
                final RpcDefinition rpc = getRpcDefinition(head);
                if (rpc != null) {
                    // TODO do we want to add head to yiiBuilder
//                    yiiBuilder.node(rpc.getQName());
                    // TODO do we need still YangInstanceIdentifier Normalization ?
//                  new DataNormalizer(schemaContext).toNormalized(yiiBuilder.build());
                    return new InstanceIdentifierContext<>(yiiBuilder.build(), rpc, mountPoint, schemaContext);
                }
            }

            RestconfValidationUtils.checkDocumentedError(targetNode != null, ErrorType.PROTOCOL, ErrorTag.INVALID_VALUE,
                    "URI has bad format. Possible reasons:\n" + " 1. \"" + head + "\" was not found in parent data node.\n"
                    + " 2. \"" + head + "\" is behind mount point. Then it should be in format \"/" + MOUNT + "/" + head + "\".");
        } else {

            final List<DataSchemaNode> potentialSchemaNodes = RestconfSchemaNodeUtils.findInstanceDataChildrenByName(parentNode, nodeName);
            RestconfValidationUtils.checkDocumentedError(( ! potentialSchemaNodes.isEmpty()), ErrorType.PROTOCOL,
                    ErrorTag.UNKNOWN_ELEMENT, "\"" + nodeName + "\" in URI was not found in parent data node");

            if (potentialSchemaNodes.size() > 1) {
                final StringBuilder strBuilder = new StringBuilder();
                for (final DataSchemaNode potentialNodeSchema : potentialSchemaNodes) {
                    strBuilder.append("   ").append(potentialNodeSchema.getQName().getNamespace()).append("\n");
                }

                throw new RestconfDocumentedException("URI has bad format. Node \"" + nodeName + "\" is added as augment from"
                        + " more than one module. Therefore the node must have module name and it has to be in format"
                        + " \"moduleName:nodeName\". \nThe node is added as augment from modules with namespaces:\n"
                        + strBuilder.toString(), ErrorType.PROTOCOL, ErrorTag.INVALID_VALUE);
            }

            targetNode = potentialSchemaNodes.get(0);
        }
        RestconfValidationUtils.checkDocumentedError(RestconfSchemaNodeUtils.isListOrContainer(targetNode), ErrorType.PROTOCOL,
                ErrorTag.INVALID_VALUE, "URI has bad format. Node \"" + head + "\" must be Container or List yang type.");

        int consumed = 1;
        if (targetNode instanceof ListSchemaNode) {
            final ListSchemaNode listNode = ((ListSchemaNode) targetNode);
            final int keysSize = listNode.getKeyDefinition().size();
            RestconfValidationUtils.checkDocumentedError(((pathArgs.size() - 1) < keysSize), ErrorType.PROTOCOL,
                    ErrorTag.DATA_MISSING, "Missing key for list \"" + listNode.getQName().getLocalName() + "\".");

            final List<String> uriKeyValues = pathArgs.subList(1, keysSize + 1);
            final HashMap<QName, Object> keyValues = new HashMap<>();
            int keyIndex = 0;
            for (final QName key : listNode.getKeyDefinition()) {
                final String uriKeyValue = uriKeyValues.get(keyIndex);
                RestconfValidationUtils.checkDocumentedError(RestconfInternalConstants.NULL_VALUE.equals(uriKeyValues),
                        ErrorType.PROTOCOL, ErrorTag.INVALID_VALUE, "URI has bad format. List \""
                                + listNode.getQName().getLocalName() + "\" cannot contain \"null\" value as a key.");
                addKeyValue(keyValues, listNode.getDataChildByName(key), uriKeyValue, mountPoint);
                keyIndex++;
            }

            consumed = consumed + keyIndex;
            yiiBuilder.nodeWithKey(targetNode.getQName(), keyValues);
        } else {
            yiiBuilder.node(targetNode.getQName());
        }

        if (targetNode instanceof DataNodeContainer) {
            final List<String> remaining = pathArgs.subList(consumed, pathArgs.size());
            return parseUriRequest(remaining, mountPoint, (DataNodeContainer) targetNode, yiiBuilder);
        }

        // TODO do we need still YangInstanceIdentifier Normalization ?
//        new DataNormalizer(schemaContext).toNormalized(yiiBuilder.build());

        return new InstanceIdentifierContext<SchemaNode>(yiiBuilder.build(), targetNode, mountPoint, schemaContext);
    }

    private static void addKeyValue(final HashMap<QName, Object> map, final DataSchemaNode node, final String uriValue,
            final DOMMountPoint mountPoint) {
        Preconditions.<String> checkNotNull(uriValue);
        Preconditions.checkArgument((node instanceof LeafSchemaNode));

        final String urlDecoded = RestconfParsingUtils.urlPathArgDecode(uriValue);
        final TypeDefinition<? extends Object> typedef = ((LeafSchemaNode) node).getType();
        final Codec<Object, Object> codec = RestCodec.from(typedef, mountPoint);

        Object decoded = codec == null ? null : codec.deserialize(urlDecoded);
        String additionalInfo = "";
        if (decoded == null) {
            final TypeDefinition<? extends Object> baseType = RestUtil.resolveBaseTypeFrom(typedef);
            if ((baseType instanceof IdentityrefTypeDefinition)) {
                decoded = RestconfParsingUtils.toQName(urlDecoded);
                additionalInfo = "For key which is of type identityref it should be in format module_name:identity_name.";
            }
        }

        RestconfValidationUtils.checkDocumentedError(decoded != null, ErrorType.PROTOCOL, ErrorTag.INVALID_VALUE,
                uriValue + " from URI can't be resolved. " + additionalInfo);

        map.put(node.getQName(), decoded);
    }

    private static final Predicate<GroupingDefinition> GROUPING_FILTER = new Predicate<GroupingDefinition>() {
        @Override
        public boolean apply(final GroupingDefinition g) {
            return Draft02.RestConfModule.RESTCONF_GROUPING_SCHEMA_NODE.equals(g.getQName().getLocalName());
        }
    };

    private DataSchemaNode getRestconfModuleRestConfSchemaNode(final String schemaNodeName) {
        final Module restconfModule = getRestconfModule();

        final Set<GroupingDefinition> groupings = restconfModule.getGroupings();
        final Iterable<GroupingDefinition> filteredGroups = Iterables.filter(groupings, GROUPING_FILTER);
        final GroupingDefinition restconfGrouping = Iterables.getFirst(filteredGroups, null);

        final DataSchemaNode restconfContainer = RestconfSchemaNodeUtils.findInstanceDataChildByName(restconfGrouping,
                Draft02.RestConfModule.RESTCONF_CONTAINER_SCHEMA_NODE);

        if (Objects.equal(schemaNodeName, Draft02.RestConfModule.OPERATIONS_CONTAINER_SCHEMA_NODE)) {
            return RestconfSchemaNodeUtils.findInstanceDataChildByName(((DataNodeContainer) restconfContainer),
                    Draft02.RestConfModule.OPERATIONS_CONTAINER_SCHEMA_NODE);
        } else if (Objects.equal(schemaNodeName, Draft02.RestConfModule.STREAMS_CONTAINER_SCHEMA_NODE)) {
            return RestconfSchemaNodeUtils.findInstanceDataChildByName(((DataNodeContainer) restconfContainer),
                    Draft02.RestConfModule.STREAMS_CONTAINER_SCHEMA_NODE);
        } else if (Objects.equal(schemaNodeName, Draft02.RestConfModule.STREAM_LIST_SCHEMA_NODE)) {

            final DataSchemaNode modules = RestconfSchemaNodeUtils.findInstanceDataChildByName(
                    ((DataNodeContainer) restconfContainer), Draft02.RestConfModule.STREAMS_CONTAINER_SCHEMA_NODE);
            return RestconfSchemaNodeUtils.findInstanceDataChildByName(((DataNodeContainer) modules),
                    Draft02.RestConfModule.STREAM_LIST_SCHEMA_NODE);
        } else if (Objects.equal(schemaNodeName, Draft02.RestConfModule.MODULES_CONTAINER_SCHEMA_NODE)) {
            return RestconfSchemaNodeUtils.findInstanceDataChildByName(((DataNodeContainer) restconfContainer),
                    Draft02.RestConfModule.MODULES_CONTAINER_SCHEMA_NODE);
        } else if (Objects.equal(schemaNodeName, Draft02.RestConfModule.MODULE_LIST_SCHEMA_NODE)) {
            final DataSchemaNode modules = RestconfSchemaNodeUtils.findInstanceDataChildByName(
                    ((DataNodeContainer) restconfContainer), Draft02.RestConfModule.MODULES_CONTAINER_SCHEMA_NODE);
            return RestconfSchemaNodeUtils.findInstanceDataChildByName(((DataNodeContainer) modules),
                    Draft02.RestConfModule.MODULE_LIST_SCHEMA_NODE);
        } else if (Objects.equal(schemaNodeName, Draft02.RestConfModule.STREAMS_CONTAINER_SCHEMA_NODE)) {
            return RestconfSchemaNodeUtils.findInstanceDataChildByName(((DataNodeContainer) restconfContainer),
                    Draft02.RestConfModule.STREAMS_CONTAINER_SCHEMA_NODE);
        }
        return null;
    }
}
