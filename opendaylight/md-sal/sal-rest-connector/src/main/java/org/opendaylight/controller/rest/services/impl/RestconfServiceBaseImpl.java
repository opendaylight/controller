/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.rest.services.impl;

import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import java.net.URI;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.api.DOMMountPoint;
import org.opendaylight.controller.rest.common.InstanceIdentifierContext;
import org.opendaylight.controller.rest.common.NormalizedNodeContext;
import org.opendaylight.controller.rest.common.QueryParametersParser;
import org.opendaylight.controller.rest.connector.RestBrokerFacade;
import org.opendaylight.controller.rest.connector.RestSchemaController;
import org.opendaylight.controller.rest.connector.impl.RestSchemaControllerImpl;
import org.opendaylight.controller.rest.errors.RestconfDocumentedException;
import org.opendaylight.controller.rest.errors.RestconfError.ErrorTag;
import org.opendaylight.controller.rest.errors.RestconfError.ErrorType;
import org.opendaylight.controller.rest.services.RestconfServiceBase;
import org.opendaylight.controller.rest.streams.listeners.ListenerAdapter;
import org.opendaylight.controller.rest.streams.listeners.Notificator;
import org.opendaylight.controller.rest.streams.websockets.WebSocketServer;
import org.opendaylight.controller.sal.rest.api.Draft02;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.LeafSetEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapNode;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.CollectionNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.DataContainerNodeAttrBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.ListNodeBuilder;
import org.opendaylight.yangtools.yang.model.api.ContainerSchemaNode;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.FeatureDefinition;
import org.opendaylight.yangtools.yang.model.api.LeafListSchemaNode;
import org.opendaylight.yangtools.yang.model.api.LeafSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ListSchemaNode;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RestconfServiceBaseImpl extends AbstractRestconfServiceImpl implements RestconfServiceBase {

    private static final Logger LOG = LoggerFactory.getLogger(RestconfServiceBaseImpl.class);

    private static final SimpleDateFormat REVISION_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    private static final String DATASTORE_PARAM_NAME = "datastore";
    private static final String SCOPE_PARAM_NAME = "scope";
    private static final int NOTIFICATION_PORT = 8181;

    public RestconfServiceBaseImpl(final RestBrokerFacade dataBroker, final RestSchemaController schemaCx) {
        super(dataBroker, schemaCx);
    }

    @Override
    public Object getRoot() {
        return null;
    }

    @Override
    public NormalizedNodeContext getModules(final UriInfo uriInfo) {
        final Set<Module> allModules = schemaController.getAllModules();
        final MapNode allModuleMap = makeModuleMapNode(allModules);

        final SchemaContext schemaContext = schemaController.getGlobalSchema();

        final Module restconfModule = getRestconfModule();
        final DataSchemaNode modulesSchemaNode = schemaController.getRestconfModuleRestConfSchemaNode(restconfModule,
                Draft02.RestConfModule.MODULES_CONTAINER_SCHEMA_NODE);
        Preconditions.checkState(modulesSchemaNode instanceof ContainerSchemaNode);

        final DataContainerNodeAttrBuilder<NodeIdentifier, ContainerNode> moduleContainerBuilder = Builders
                .containerBuilder((ContainerSchemaNode) modulesSchemaNode);
        moduleContainerBuilder.withChild(allModuleMap);

        return new NormalizedNodeContext(new InstanceIdentifierContext<>(null, modulesSchemaNode, null, schemaContext),
                moduleContainerBuilder.build(), QueryParametersParser.parseWriterParameters(uriInfo));
    }

    @Override
    public NormalizedNodeContext getModules(final String identifier, final UriInfo uriInfo) {
        Preconditions.checkNotNull(identifier);
        if (!identifier.contains(RestSchemaControllerImpl.MOUNT)) {
            final String errMsg = "URI has bad format. If modules behind mount point should be showed,"
                    + " URI has to end with " + RestSchemaControllerImpl.MOUNT;
            LOG.debug(errMsg + " for " + identifier);
            throw new RestconfDocumentedException(errMsg, ErrorType.PROTOCOL, ErrorTag.INVALID_VALUE);
        }

        final InstanceIdentifierContext<?> mountPointIdentifier = schemaController.toMountPointIdentifier(identifier);
        final DOMMountPoint mountPoint = mountPointIdentifier.getMountPoint();
        final Set<Module> modules = schemaController.getAllModules(mountPoint);
        final MapNode mountPointModulesMap = makeModuleMapNode(modules);

        final Module restconfModule = getRestconfModule();
        final DataSchemaNode modulesSchemaNode = schemaController.getRestconfModuleRestConfSchemaNode(restconfModule,
                Draft02.RestConfModule.MODULES_CONTAINER_SCHEMA_NODE);
        Preconditions.checkState(modulesSchemaNode instanceof ContainerSchemaNode);

        final DataContainerNodeAttrBuilder<NodeIdentifier, ContainerNode> moduleContainerBuilder = Builders
                .containerBuilder((ContainerSchemaNode) modulesSchemaNode);
        moduleContainerBuilder.withChild(mountPointModulesMap);

        return new NormalizedNodeContext(new InstanceIdentifierContext<>(null, modulesSchemaNode, mountPoint,
                schemaController.getGlobalSchema()), moduleContainerBuilder.build(),
                QueryParametersParser.parseWriterParameters(uriInfo));
    }

    @Override
    public NormalizedNodeContext getModule(final String identifier, final UriInfo uriInfo) {
        Preconditions.checkNotNull(identifier);
        final QName moduleNameAndRevision = getModuleNameAndRevision(identifier);
        Module module = null;
        DOMMountPoint mountPoint = null;
        final SchemaContext schemaContext;
        if (identifier.contains(RestSchemaControllerImpl.MOUNT)) {
            final InstanceIdentifierContext<?> mountPointIdentifier = schemaController
                    .toMountPointIdentifier(identifier);
            mountPoint = mountPointIdentifier.getMountPoint();
            module = schemaController.findModuleByNameAndRevision(mountPoint, moduleNameAndRevision);
            schemaContext = mountPoint.getSchemaContext();
        } else {
            module = schemaController.findModuleByNameAndRevision(moduleNameAndRevision);
            schemaContext = schemaController.getGlobalSchema();
        }

        if (module == null) {
            final String errMsg = "Module with name '" + moduleNameAndRevision.getLocalName() + "' and revision '"
                    + moduleNameAndRevision.getRevision() + "' was not found.";
            LOG.debug(errMsg);
            throw new RestconfDocumentedException(errMsg, ErrorType.PROTOCOL, ErrorTag.UNKNOWN_ELEMENT);
        }

        final Module restconfModule = getRestconfModule();
        final Set<Module> modules = Collections.singleton(module);
        final MapNode moduleMap = makeModuleMapNode(modules);

        final DataSchemaNode moduleSchemaNode = schemaController.getRestconfModuleRestConfSchemaNode(restconfModule,
                Draft02.RestConfModule.MODULE_LIST_SCHEMA_NODE);
        Preconditions.checkState(moduleSchemaNode instanceof ListSchemaNode);

        return new NormalizedNodeContext(new InstanceIdentifierContext<>(null, moduleSchemaNode, mountPoint,
                schemaContext), moduleMap, QueryParametersParser.parseWriterParameters(uriInfo));
    }

    /**
     * Subscribes to some path in schema context (stream) to listen on changes on this stream.
     *
     * Additional parameters for subscribing to stream are loaded via rpc input parameters:
     * <ul>
     * <li>datastore</li> - default CONFIGURATION (other values of {@link LogicalDatastoreType} enum type)
     * <li>scope</li> - default BASE (other values of {@link DataChangeScope})
     * </ul>
     */
    @Override
    public Response subscribeToStream(final String identifier, final UriInfo uriInfo) {
        final String streamName = Notificator.createStreamNameFromUri(identifier);
        if (Strings.isNullOrEmpty(streamName)) {
            throw new RestconfDocumentedException("Stream name is empty.", ErrorType.PROTOCOL, ErrorTag.INVALID_VALUE);
        }

        final ListenerAdapter listener = Notificator.getListenerFor(streamName);
        if (listener == null) {
            throw new RestconfDocumentedException("Stream was not found.", ErrorType.PROTOCOL, ErrorTag.UNKNOWN_ELEMENT);
        }

        final Map<String, String> paramToValues = resolveValuesFromUri(identifier);
        final LogicalDatastoreType datastore = parserURIEnumParameter(LogicalDatastoreType.class,
                paramToValues.get(DATASTORE_PARAM_NAME));
        if (datastore == null) {
            throw new RestconfDocumentedException("Stream name doesn't contains datastore value (pattern /datastore=)",
                    ErrorType.APPLICATION, ErrorTag.MISSING_ATTRIBUTE);
        }
        final DataChangeScope scope = parserURIEnumParameter(DataChangeScope.class, paramToValues.get(SCOPE_PARAM_NAME));
        if (scope == null) {
            throw new RestconfDocumentedException("Stream name doesn't contains datastore value (pattern /scope=)",
                    ErrorType.APPLICATION, ErrorTag.MISSING_ATTRIBUTE);
        }

        broker.registerToListenDataChanges(datastore, scope, listener);

        final UriBuilder uriBuilder = uriInfo.getAbsolutePathBuilder();
        int notificationPort = NOTIFICATION_PORT;
        try {
            final WebSocketServer webSocketServerInstance = WebSocketServer.getInstance();
            notificationPort = webSocketServerInstance.getPort();
        } catch (final NullPointerException e) {
            WebSocketServer.createInstance(NOTIFICATION_PORT);
        }
        final UriBuilder uriToWebsocketServerBuilder = uriBuilder.port(notificationPort).scheme("ws");
        final URI uriToWebsocketServer = uriToWebsocketServerBuilder.replacePath(streamName).build();

        return Response.status(Status.OK).location(uriToWebsocketServer).build();
    }

    @Override
    public NormalizedNodeContext getAvailableStreams(final UriInfo uriInfo) {
        final SchemaContext schemaContext = schemaController.getGlobalSchema();
        final Set<String> availableStreams = Notificator.getStreamNames();
        final Module restconfModule = getRestconfModule();
        final DataSchemaNode streamSchemaNode = schemaController.getRestconfModuleRestConfSchemaNode(restconfModule,
                Draft02.RestConfModule.STREAM_LIST_SCHEMA_NODE);
        Preconditions.checkState(streamSchemaNode instanceof ListSchemaNode);

        final CollectionNodeBuilder<MapEntryNode, MapNode> listStreamsBuilder = Builders
                .mapBuilder((ListSchemaNode) streamSchemaNode);

        for (final String streamName : availableStreams) {
            listStreamsBuilder.withChild(toStreamEntryNode(streamName, streamSchemaNode));
        }

        final DataSchemaNode streamsContainerSchemaNode = schemaController.getRestconfModuleRestConfSchemaNode(restconfModule,
                Draft02.RestConfModule.STREAMS_CONTAINER_SCHEMA_NODE);
        Preconditions.checkState(streamsContainerSchemaNode instanceof ContainerSchemaNode);

        final DataContainerNodeAttrBuilder<NodeIdentifier, ContainerNode> streamsContainerBuilder = Builders
                .containerBuilder((ContainerSchemaNode) streamsContainerSchemaNode);
        streamsContainerBuilder.withChild(listStreamsBuilder.build());

        return new NormalizedNodeContext(new InstanceIdentifierContext<>(null, streamsContainerSchemaNode, null,
                schemaContext), streamsContainerBuilder.build(), QueryParametersParser.parseWriterParameters(uriInfo));
    }

    private MapNode makeModuleMapNode(final Set<Module> modules) {
        Preconditions.checkNotNull(modules);
        final Module restconfModule = getRestconfModule();
        final DataSchemaNode moduleSchemaNode = schemaController.getRestconfModuleRestConfSchemaNode(restconfModule,
                Draft02.RestConfModule.MODULE_LIST_SCHEMA_NODE);
        Preconditions.checkState(moduleSchemaNode instanceof ListSchemaNode);

        final CollectionNodeBuilder<MapEntryNode, MapNode> listModuleBuilder = Builders
                .mapBuilder((ListSchemaNode) moduleSchemaNode);

        for (final Module module : modules) {
            listModuleBuilder.withChild(toModuleEntryNode(module, moduleSchemaNode));
        }
        return listModuleBuilder.build();
    }

    protected MapEntryNode toModuleEntryNode(final Module module, final DataSchemaNode moduleSchemaNode) {
        Preconditions.checkArgument(moduleSchemaNode instanceof ListSchemaNode,
                "moduleSchemaNode has to be of type ListSchemaNode");
        final ListSchemaNode listModuleSchemaNode = (ListSchemaNode) moduleSchemaNode;
        final DataContainerNodeAttrBuilder<NodeIdentifierWithPredicates, MapEntryNode> moduleNodeValues = Builders
                .mapEntryBuilder(listModuleSchemaNode);

        List<DataSchemaNode> instanceDataChildrenByName = RestSchemaControllerImpl.findInstanceDataChildrenByName(
                (listModuleSchemaNode), "name");
        final DataSchemaNode nameSchemaNode = Iterables.getFirst(instanceDataChildrenByName, null);
        Preconditions.checkState(nameSchemaNode instanceof LeafSchemaNode);
        moduleNodeValues.withChild(Builders.leafBuilder((LeafSchemaNode) nameSchemaNode).withValue(module.getName())
                .build());

        instanceDataChildrenByName = RestSchemaControllerImpl.findInstanceDataChildrenByName((listModuleSchemaNode),
                "revision");
        final DataSchemaNode revisionSchemaNode = Iterables.getFirst(instanceDataChildrenByName, null);
        Preconditions.checkState(revisionSchemaNode instanceof LeafSchemaNode);
        final String revision = REVISION_FORMAT.format(module.getRevision());
        moduleNodeValues.withChild(Builders.leafBuilder((LeafSchemaNode) revisionSchemaNode).withValue(revision)
                .build());

        instanceDataChildrenByName = RestSchemaControllerImpl.findInstanceDataChildrenByName((listModuleSchemaNode),
                "namespace");
        final DataSchemaNode namespaceSchemaNode = Iterables.getFirst(instanceDataChildrenByName, null);
        Preconditions.checkState(namespaceSchemaNode instanceof LeafSchemaNode);
        moduleNodeValues.withChild(Builders.leafBuilder((LeafSchemaNode) namespaceSchemaNode)
                .withValue(module.getNamespace().toString()).build());

        instanceDataChildrenByName = RestSchemaControllerImpl.findInstanceDataChildrenByName((listModuleSchemaNode),
                "feature");
        final DataSchemaNode featureSchemaNode = Iterables.getFirst(instanceDataChildrenByName, null);
        Preconditions.checkState(featureSchemaNode instanceof LeafListSchemaNode);
        final ListNodeBuilder<Object, LeafSetEntryNode<Object>> featuresBuilder = Builders
                .leafSetBuilder((LeafListSchemaNode) featureSchemaNode);
        for (final FeatureDefinition feature : module.getFeatures()) {
            featuresBuilder.withChild(Builders.leafSetEntryBuilder(((LeafListSchemaNode) featureSchemaNode))
                    .withValue(feature.getQName().getLocalName()).build());
        }
        moduleNodeValues.withChild(featuresBuilder.build());

        return moduleNodeValues.build();
    }

    private QName getModuleNameAndRevision(final String identifier) {
        final int mountIndex = identifier.indexOf(RestSchemaControllerImpl.MOUNT);
        String moduleNameAndRevision = "";
        if (mountIndex >= 0) {
            moduleNameAndRevision = identifier.substring(mountIndex + RestSchemaControllerImpl.MOUNT.length());
        } else {
            moduleNameAndRevision = identifier;
        }

        final Splitter splitter = Splitter.on("/").omitEmptyStrings();
        final Iterable<String> split = splitter.split(moduleNameAndRevision);
        final List<String> pathArgs = Lists.<String> newArrayList(split);
        if (pathArgs.size() < 2) {
            LOG.debug("URI has bad format. It should be \'moduleName/yyyy-MM-dd\' " + identifier);
            throw new RestconfDocumentedException(
                    "URI has bad format. End of URI should be in format \'moduleName/yyyy-MM-dd\'", ErrorType.PROTOCOL,
                    ErrorTag.INVALID_VALUE);
        }

        try {
            final String moduleName = pathArgs.get(0);
            final String revision = pathArgs.get(1);
            final Date moduleRevision = REVISION_FORMAT.parse(revision);
            return QName.create(null, moduleRevision, moduleName);
        } catch (final ParseException e) {
            LOG.debug("URI has bad format. It should be \'moduleName/yyyy-MM-dd\' " + identifier);
            throw new RestconfDocumentedException("URI has bad format. It should be \'moduleName/yyyy-MM-dd\'",
                    ErrorType.PROTOCOL, ErrorTag.INVALID_VALUE);
        }
    }

    protected MapEntryNode toStreamEntryNode(final String streamName, final DataSchemaNode streamSchemaNode) {
        Preconditions.checkArgument(streamSchemaNode instanceof ListSchemaNode,
                "streamSchemaNode has to be of type ListSchemaNode");
        final ListSchemaNode listStreamSchemaNode = (ListSchemaNode) streamSchemaNode;
        final DataContainerNodeAttrBuilder<NodeIdentifierWithPredicates, MapEntryNode> streamNodeValues = Builders
                .mapEntryBuilder(listStreamSchemaNode);

        List<DataSchemaNode> instanceDataChildrenByName = RestSchemaControllerImpl.findInstanceDataChildrenByName(
                (listStreamSchemaNode), "name");
        final DataSchemaNode nameSchemaNode = Iterables.getFirst(instanceDataChildrenByName, null);
        Preconditions.checkState(nameSchemaNode instanceof LeafSchemaNode);
        streamNodeValues.withChild(Builders.leafBuilder((LeafSchemaNode) nameSchemaNode).withValue(streamName).build());

        instanceDataChildrenByName = RestSchemaControllerImpl.findInstanceDataChildrenByName((listStreamSchemaNode),
                "description");
        final DataSchemaNode descriptionSchemaNode = Iterables.getFirst(instanceDataChildrenByName, null);
        Preconditions.checkState(descriptionSchemaNode instanceof LeafSchemaNode);
        streamNodeValues.withChild(Builders.leafBuilder((LeafSchemaNode) nameSchemaNode)
                .withValue("DESCRIPTION_PLACEHOLDER").build());

        instanceDataChildrenByName = RestSchemaControllerImpl.findInstanceDataChildrenByName((listStreamSchemaNode),
                "replay-support");
        final DataSchemaNode replaySupportSchemaNode = Iterables.getFirst(instanceDataChildrenByName, null);
        Preconditions.checkState(replaySupportSchemaNode instanceof LeafSchemaNode);
        streamNodeValues.withChild(Builders.leafBuilder((LeafSchemaNode) replaySupportSchemaNode)
                .withValue(Boolean.valueOf(true)).build());

        instanceDataChildrenByName = RestSchemaControllerImpl.findInstanceDataChildrenByName((listStreamSchemaNode),
                "replay-log-creation-time");
        final DataSchemaNode replayLogCreationTimeSchemaNode = Iterables.getFirst(instanceDataChildrenByName, null);
        Preconditions.checkState(replayLogCreationTimeSchemaNode instanceof LeafSchemaNode);
        streamNodeValues.withChild(Builders.leafBuilder((LeafSchemaNode) replayLogCreationTimeSchemaNode).withValue("")
                .build());

        instanceDataChildrenByName = RestSchemaControllerImpl.findInstanceDataChildrenByName((listStreamSchemaNode),
                "events");
        final DataSchemaNode eventsSchemaNode = Iterables.getFirst(instanceDataChildrenByName, null);
        Preconditions.checkState(eventsSchemaNode instanceof LeafSchemaNode);
        streamNodeValues.withChild(Builders.leafBuilder((LeafSchemaNode) eventsSchemaNode).withValue("").build());

        return streamNodeValues.build();
    }

    /**
     * Checks whether {@code value} is one of the string representation of enumeration {@code classDescriptor}
     *
     * @return enum object if string value of {@code classDescriptor} enumeration is equal to {@code value}. Other cases
     *         null.
     */
    private <T> T parserURIEnumParameter(final Class<T> classDescriptor, final String value) {
        if (Strings.isNullOrEmpty(value)) {
            return null;
        }
        return resolveAsEnum(classDescriptor, value);
    }

    private Map<String, String> resolveValuesFromUri(final String uri) {
        final Map<String, String> result = new HashMap<>();
        final String[] tokens = uri.split("/");
        for (int i = 1; i < tokens.length; i++) {
            final String[] parameterTokens = tokens[i].split("=");
            if (parameterTokens.length == 2) {
                result.put(parameterTokens[0], parameterTokens[1]);
            }
        }
        return result;
    }
}
