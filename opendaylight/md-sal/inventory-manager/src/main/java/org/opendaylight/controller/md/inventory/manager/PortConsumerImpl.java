package org.opendaylight.controller.md.inventory.manager;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.opendaylight.controller.clustering.services.CacheConfigException;
import org.opendaylight.controller.clustering.services.CacheExistException;
import org.opendaylight.controller.clustering.services.IClusterContainerServices;
import org.opendaylight.controller.clustering.services.IClusterServices;
import org.opendaylight.controller.md.sal.common.api.data.DataCommitHandler;
import org.opendaylight.controller.md.sal.common.api.data.DataCommitHandler.DataCommitTransaction;
import org.opendaylight.controller.md.sal.common.api.data.DataModification;
import org.opendaylight.controller.sal.common.util.Rpcs;
import org.opendaylight.controller.sal.core.IContainer;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.utils.GlobalConstants;
import org.opendaylight.controller.sal.utils.Status;
import org.opendaylight.controller.sal.utils.StatusCode;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.port.rev130925.port.mod.port.Port;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.port.rev130925.port.mod.port.PortKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.port.config.rev131024.Ports;
import org.opendaylight.yang.gen.v1.urn.opendaylight.port.service.rev131107.SalPortService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.port.service.rev131107.UpdatePortInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.port.service.rev131107.port.update.UpdatedPortBuilder;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.NotificationListener;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PortConsumerImpl implements IPortUpdateImpl {

    protected static final Logger logger = LoggerFactory.getLogger(PortConsumerImpl.class);

    // private PortEventListener portEventListener = new PortEventListener();

    private Registration<NotificationListener> portListener;

    private SalPortService portService;

    private PortDataCommitHandler commitHandler;

    private ConcurrentMap<PortKey, Port> originalSwPortView;

    private ConcurrentMap<PortKey, Port> installedSwPortView;

    private ConcurrentMap<Node, List<Port>> nodePorts;

    private ConcurrentMap<PortKey, Port> inactivePorts;

    private IClusterContainerServices clusterPortContainerService = null;

    private IContainer container;

    public PortConsumerImpl() {

        InstanceIdentifier<? extends DataObject> path = InstanceIdentifier.builder().node(Ports.class).node(Port.class)
                .toInstance();

        portService = InventoryConsumerImpl.getProviderSession().getRpcService(SalPortService.class);

        clusterPortContainerService = InventoryConsumerImpl.getClusterContainerService();

        container = InventoryConsumerImpl.getContainer();

        if (!(cacheStartup())) {

            logger.error("Unable to allocate/retrieve port cache");

        }

        if (null == portService) {

            logger.error("Consumer SAL Port Service is down or NULL. Inventory may not function as intended");

            return;

        }

        // For switch/plugin events

        // portListener =
        // InventoryConsumerImpl.getNotificationService().registerNotificationListener(portEventListener);

        if (null == portListener) {

            logger.error("Listener to listen on port data modifcation events");

            return;

        }

        commitHandler = new PortDataCommitHandler();

        InventoryConsumerImpl.getDataProviderService().registerCommitHandler(path, commitHandler);

    }

    private boolean allocatePortCaches() {

        if (this.clusterPortContainerService == null) {
            logger.warn("Port: Un-initialized clusterPortContainerService, can't create cache");
            return false;
        }

        try {
            clusterPortContainerService.createCache("Inventory.originalSwPortView",
                    EnumSet.of(IClusterServices.cacheMode.TRANSACTIONAL));

            clusterPortContainerService.createCache("Inventory.installedSwPortView",
                    EnumSet.of(IClusterServices.cacheMode.TRANSACTIONAL));

            clusterPortContainerService.createCache("Inventory.inactivePorts",
                    EnumSet.of(IClusterServices.cacheMode.TRANSACTIONAL));

            clusterPortContainerService.createCache("Inventory.nodePorts",
                    EnumSet.of(IClusterServices.cacheMode.TRANSACTIONAL));

            // TODO for cluster mode
            /*
             * clusterPortContainerService.createCache(WORK_STATUS_CACHE,
             * EnumSet.of(IClusterServices.cacheMode.NON_TRANSACTIONAL,
             * IClusterServices.cacheMode.ASYNC));
             * clusterPortContainerService.createCache(WORK_ORDER_CACHE,
             * EnumSet.of(IClusterServices.cacheMode.NON_TRANSACTIONAL,
             * IClusterServices.cacheMode.ASYNC));
             */

        } catch (CacheConfigException cce) {
            logger.error("Port CacheConfigException");
            return false;

        } catch (CacheExistException cce) {
            logger.error("Port CacheExistException");
        }

        return true;

    }

    private void nonClusterPortObjectCreate() {
        originalSwPortView = new ConcurrentHashMap<PortKey, Port>();
        installedSwPortView = new ConcurrentHashMap<PortKey, Port>();
        nodePorts = new ConcurrentHashMap<Node, List<Port>>();
        inactivePorts = new ConcurrentHashMap<PortKey, Port>();
    }

    private boolean retrievePortCaches() {

        ConcurrentMap<?, ?> map;

        if (this.clusterPortContainerService == null) {
            logger.warn("Port: un-initialized clusterPortContainerService, can't retrieve cache");
            nonClusterPortObjectCreate();
            return false;
        }

        map = clusterPortContainerService.getCache("Inventory.originalSwPortView");
        if (map != null) {
            originalSwPortView = (ConcurrentMap<PortKey, Port>) map;
        } else {
            logger.error("Retrieval of cache(originalSwPortView) failed");
            return false;
        }

        map = clusterPortContainerService.getCache("Inventory.installedSwPortView");
        if (map != null) {
            installedSwPortView = (ConcurrentMap<PortKey, Port>) map;
        } else {
            logger.error("Retrieval of cache(installedSwPortView) failed");
            return false;
        }

        map = clusterPortContainerService.getCache("Inventory.inactivePorts");
        if (map != null) {
            inactivePorts = (ConcurrentMap<PortKey, Port>) map;
        } else {
            logger.error("Retrieval of cache(inactivePort) failed");
            return false;
        }

        map = clusterPortContainerService.getCache("Inventory.nodePorts");
        if (map != null) {
            nodePorts = (ConcurrentMap<Node, List<Port>>) map;
        } else {
            logger.error("Retrieval of cache(nodePort) failed");
            return false;
        }

        return true;

    }

    private boolean cacheStartup() {

        if (allocatePortCaches()) {

            if (retrievePortCaches()) {
                return true;
            }
        }
        return false;

    }

    /*
     * Update Port to the southbound plugin and our internal database
     *
     * @param path
     *
     * @param dataObject
     */
    private Status updatePort(InstanceIdentifier<?> path, Port portUpdateDataObject) {
        PortKey portKey = portUpdateDataObject.getKey();

        if (null != portKey && validatePort(portUpdateDataObject, InventoryConsumerImpl.operation.ADD).isSuccess()) {

            UpdatePortInputBuilder updatePortBuilder = new UpdatePortInputBuilder();
            UpdatedPortBuilder updatedport = new UpdatedPortBuilder();
            updatedport.fieldsFrom(portUpdateDataObject);

            updatePortBuilder.setUpdatedPort(updatedport.build());
            portService.updatePort(updatePortBuilder.build());

            originalSwPortView.put(portKey, portUpdateDataObject);
        } else {
            return new Status(StatusCode.BADREQUEST, "Port Key or attribute validation failed");
        }

        return new Status(StatusCode.SUCCESS);
    }

    public Status validatePort(Port port, InventoryConsumerImpl.operation add) {
        String containerName;
        Long portNumber;
        String portname;
        Status returnStatus = null;
        boolean returnResult;

        if (null != port) {

            containerName = port.getContainerName();

            if (null == containerName) {
                containerName = GlobalConstants.DEFAULT.toString();
            } else if (!InventoryConsumerImpl.isNameValid(containerName)) {
                logger.error("Container Name is invalid %s" + containerName);
                returnStatus = new Status(StatusCode.BADREQUEST, "Container Name is invalid");
                return returnStatus;
            }

            portname = port.getPortName();
            if (!InventoryConsumerImpl.isNameValid(portname)) {
                logger.error("Port Name is invalid %s" + portname);
                returnStatus = new Status(StatusCode.BADREQUEST, "Port Name is invalid");
                return returnStatus;
            }

            returnResult = doesPortEntryExists(port.getKey(), portname, containerName);

            if (InventoryConsumerImpl.operation.ADD == add && returnResult) {
                logger.error("Record with same Port exists");
                returnStatus = new Status(StatusCode.BADREQUEST, "Port record exists");
                return returnStatus;
            } else if (!returnResult) {
                logger.error("Port record does not exist");
                returnStatus = new Status(StatusCode.BADREQUEST, "Port record does not exist");
                return returnStatus;
            }

            MacAddress portAddress = port.getHardwareAddress();
            if (portAddress != null && InventoryConsumerImpl.isL2AddressValid(portAddress.getValue().toString())) {
                logger.error("Hardware address %s is not valid. Example: 00:05:b9:7c:81:5f", portAddress);
                returnStatus = new Status(StatusCode.BADREQUEST, "Hardware Address not valid");
                return returnStatus;
            }

        }
        return new Status(StatusCode.SUCCESS);
    }

    private boolean doesPortEntryExists(PortKey key, String portname, String containername) {
        if (!originalSwPortView.containsKey(key)) {
            return false;
        }

        for (Entry<PortKey, Port> entry : originalSwPortView.entrySet()) {
            if (entry.getValue().getPortName().equals(portname)) {
                if (entry.getValue().getContainerName().equals(containername)) {
                    return true;
                }
            }
        }
        return false;
    }

    private RpcResult<Void> commitToPlugin(internalTransaction transaction) {

        for (@SuppressWarnings("unused")
        Entry<InstanceIdentifier<?>, Port> entry : transaction.updates.entrySet()) {

            if (!updatePort(entry.getKey(), entry.getValue()).isSuccess()) {
                return Rpcs.getRpcResult(false, null, null);
            }
        }

        return Rpcs.getRpcResult(true, null, null);
    }

    private final class internalTransaction implements DataCommitTransaction<InstanceIdentifier<?>, DataObject> {

        private final DataModification<InstanceIdentifier<?>, DataObject> modification;

        @Override
        public DataModification<InstanceIdentifier<?>, DataObject> getModification() {

            return modification;
        }

        public internalTransaction(DataModification<InstanceIdentifier<?>, DataObject> modification) {
            this.modification = modification;
        }

        Map<InstanceIdentifier<?>, Port> updates = new HashMap<>();

        /**
         * We create a plan which flows will be added, which will be updated and
         * which will be removed based on our internal state.
         */

        void prepareUpdate() {

            Set<Entry<InstanceIdentifier<?>, DataObject>> puts = modification.getUpdatedConfigurationData().entrySet();
            for (Entry<InstanceIdentifier<?>, DataObject> entry : puts) {
                if (entry.getValue() instanceof Port) {
                    Port port = (Port) entry.getValue();
                    preparePutEntry(entry.getKey(), port);
                }

            }
        }

        private void preparePutEntry(InstanceIdentifier<?> key, Port port) {

            Port original = originalSwPortView.get(key);
            if (original != null) {
                // It is update for us

                updates.put(key, port);
            }
        }

        @Override
        public RpcResult<Void> finish() throws IllegalStateException {

            RpcResult<Void> rpcStatus = commitToPlugin(this);
            // We return true if internal transaction is successful.
            // return Rpcs.getRpcResult(true, null, Collections.emptySet());
            return rpcStatus;

        }

        @Override
        public RpcResult<Void> rollback() throws IllegalStateException {

            // NOOP - we did not modified any internal state during
            // requestCommit phase
            // return Rpcs.getRpcResult(true, null, Collections.emptySet());
            return Rpcs.getRpcResult(true, null, null);

        }

    }

    private final class PortDataCommitHandler implements DataCommitHandler<InstanceIdentifier<?>, DataObject> {

        @Override
        public org.opendaylight.controller.md.sal.common.api.data.DataCommitHandler.DataCommitTransaction<InstanceIdentifier<?>, DataObject> requestCommit(

        DataModification<InstanceIdentifier<?>, DataObject> modification) {

            // We should verify transaction

            System.out.println("Coming in PortDataCommitHandler");

            internalTransaction transaction = new internalTransaction(modification);

            transaction.prepareUpdate();

            return transaction;

        }

    }

    @Override
    public void updatePortDb(Port portUpdateDataObject) {

        PortKey portkey = portUpdateDataObject.getKey();

        if (null != portkey && validatePort(portUpdateDataObject, InventoryConsumerImpl.operation.ADD).isSuccess()) {

            originalSwPortView.put(portkey, portUpdateDataObject);
        }
    }

}
