/**
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.mdsal.ioc;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMMountPointService;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.controller.sal.binding.api.BindingAwareService;
import org.opendaylight.controller.sal.binding.api.mount.MountInstance;
import org.opendaylight.controller.sal.binding.api.mount.MountProviderService;
import org.opendaylight.controller.sal.core.api.Broker;
import org.opendaylight.controller.sal.core.api.BrokerService;
import org.opendaylight.controller.sal.core.api.notify.NotificationListener;
import org.opendaylight.controller.sal.core.api.notify.NotificationPublishService;
import org.opendaylight.controller.sal.core.api.notify.NotificationService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.RpcService;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MdSAL {
    private static final Logger LOGGER = LoggerFactory.getLogger(MdSAL.class);

    private BindingAwareBroker.ProviderContext bindingAwareContext;
    private Broker.ProviderSession bindingIndependentContext;

    // -----------------------------
    // ----- FRAMEWORK METHODS -----
    // -----------------------------
    // TODO: Solve reinit issue
    public void setBindingAwareContext(BindingAwareBroker.ProviderContext bindingAwareContext) {
        this.bindingAwareContext = bindingAwareContext;
    }

    public void setBindingIndependentContext(Broker.ProviderSession bindingIndependentContext) {
        this.bindingIndependentContext = bindingIndependentContext;
    }

    //TODO: We should hide brokers and expose functionalities instead
    public DataBroker getDataBroker() {
        return getBaSalService(DataBroker.class);
    }

    public synchronized boolean isReady() {
        return (bindingAwareContext != null && bindingIndependentContext != null);
    }

    // -----------------------
    // ----- API METHODS -----
    // -----------------------
    // TODO: Factor out API methods to interface
    // method does not return registration object. Rather will hold references internally and manipulate using node id and API
    public <T extends RpcService> void addRpcImplementation(Class<T> serviceInterface,
                                                            T implementation)
            throws IllegalStateException {
        bindingAwareContext.addRpcImplementation(serviceInterface, implementation);
    }

    // method does not return registration object. Rather will hold references internally and manipulate using node id and API
    public <T extends RpcService> void addRpcImplementation(Node node,
                                                            Class<T> serviceInterface,
                                                            T implementation)
            throws IllegalStateException {
        BindingAwareBroker.RoutedRpcRegistration<T> registration
                = addRoutedRpcImplementation(serviceInterface, implementation);

        NodeRef nodeRef = createNodeRef(node.getId());
        registration.registerPath(NodeContext.class, nodeRef.getValue());
    }

    public ListenerRegistration<NotificationListener> addNotificationListener(String nodeId,
                                                                              QName notification,
                                                                              NotificationListener listener) {
        YangInstanceIdentifier yii = inventoryNodeBIIdentifier(nodeId);

        NotificationService notificationService =
                getBiSalService(DOMMountPointService.class)
                        .getMountPoint(yii)
                        .get()
                        .getService(NotificationPublishService.class)
                        .get();

        ListenerRegistration<NotificationListener> registration =
                notificationService.addNotificationListener(notification, listener);

        LOGGER.info("Notification listener registered for {}, at node {}", notification, nodeId);

        return registration;
    }

    public ListenerRegistration<NotificationListener> addNotificationListener(QName notification,
                                                                              NotificationListener listener) {
        NotificationService notificationService =
                getBiSalService(NotificationPublishService.class);

        ListenerRegistration<NotificationListener> registration =
                notificationService.addNotificationListener(notification, listener);

        LOGGER.info("Notification listener registered for {}.", notification);

        return registration;
    }

    public <T extends RpcService> T getRpcService(Class<T> serviceInterface) {
        return bindingAwareContext.getRpcService(serviceInterface);
    }

    public <T extends RpcService> T getRpcService(String nodeId, Class<T> serviceInterface) {
        MountProviderService mountProviderService = getBaSalService(MountProviderService.class);

        InstanceIdentifier<Node> key = InstanceIdentifier.create(Nodes.class)
                                                         .child(Node.class,
                                                                 new NodeKey(new NodeId(nodeId)));

        MountInstance mountPoint = mountProviderService.getMountPoint(key);
        return mountPoint.getRpcService(serviceInterface);
    }

    public void publishNotification(CompositeNode notification) {
        getBiSalService(NotificationPublishService.class).publish(notification);
    }

    public SchemaContext getSchemaContext(String nodeId) {
        YangInstanceIdentifier yii = inventoryNodeBIIdentifier(nodeId);

        SchemaContext schemaContext =
                getBiSalService(DOMMountPointService.class)
                        .getMountPoint(yii)
                        .get().getSchemaContext();

        return schemaContext;
    }

    // ---------------------------
    // ----- UTILITY METHODS -----
    // ---------------------------
    private <T extends BindingAwareService> T getBaSalService(Class<T> service) {
        return bindingAwareContext.getSALService(service);
    }

    private <T extends BrokerService> T getBiSalService(Class<T> service) {
        return bindingIndependentContext.getService(service);
    }

    private static final String NODE_ID_NAME = "id";

    public static YangInstanceIdentifier inventoryNodeBIIdentifier(String nodeId) {
        return YangInstanceIdentifier.builder()
                .node(Nodes.QNAME)
                .nodeWithKey(Node.QNAME,
                             QName.create(Node.QNAME.getNamespace(),
                                          Node.QNAME.getRevision(),
                                          NODE_ID_NAME),
                             nodeId)
                .build();
    }

    private <T extends RpcService> BindingAwareBroker.RoutedRpcRegistration<T> addRoutedRpcImplementation(Class<T> serviceInterface,
                                                                                                          T implementation)
            throws IllegalStateException {
        return bindingAwareContext.addRoutedRpcImplementation(serviceInterface, implementation);
    }

    public static NodeRef createNodeRef(NodeId nodeId) {
        NodeKey nodeKey = new NodeKey(nodeId);
        InstanceIdentifier<Node> path = InstanceIdentifier
                .builder(Nodes.class)
                .child(Node.class, nodeKey)
                .build();
        return new NodeRef(path);
    }
}
