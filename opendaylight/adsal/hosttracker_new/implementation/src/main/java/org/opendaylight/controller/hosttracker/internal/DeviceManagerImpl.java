/*
 * Copyright (c) 2011,2012 Big Switch Networks, Inc.
 *
 * Licensed under the Eclipse Public License, Version 1.0 (the
 * "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 *      http://www.eclipse.org/legal/epl-v10.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * permissions and limitations under the License.
 *
 * This file incorporates work covered by the following copyright and
 * permission notice:
 *
 *    Originally created by David Erickson, Stanford University
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the
 *    License. You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing,
 *    software distributed under the License is distributed on an "AS
 *    IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 *    express or implied. See the License for the specific language
 *    governing permissions and limitations under the License.
 */

package org.opendaylight.controller.hosttracker.internal;

import static org.opendaylight.controller.hosttracker.internal.DeviceManagerImpl.DeviceUpdate.Change.ADD;
import static org.opendaylight.controller.hosttracker.internal.DeviceManagerImpl.DeviceUpdate.Change.CHANGE;
import static org.opendaylight.controller.hosttracker.internal.DeviceManagerImpl.DeviceUpdate.Change.DELETE;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.opendaylight.controller.hosttracker.Entity;
import org.opendaylight.controller.hosttracker.IDevice;
import org.opendaylight.controller.hosttracker.IDeviceListener;
import org.opendaylight.controller.hosttracker.IDeviceService;
import org.opendaylight.controller.hosttracker.IEntityClass;
import org.opendaylight.controller.hosttracker.IEntityClassListener;
import org.opendaylight.controller.hosttracker.IEntityClassifierService;
import org.opendaylight.controller.hosttracker.IfIptoHost;
import org.opendaylight.controller.hosttracker.IfNewHostNotify;
import org.opendaylight.controller.hosttracker.SwitchPort;
import org.opendaylight.controller.hosttracker.hostAware.HostNodeConnector;
import org.opendaylight.controller.sal.core.ConstructionException;
import org.opendaylight.controller.sal.core.Edge;
import org.opendaylight.controller.sal.core.Host;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.core.NodeConnector.NodeConnectorIDType;
import org.opendaylight.controller.sal.core.Property;
import org.opendaylight.controller.sal.core.State;
import org.opendaylight.controller.sal.core.Tier;
import org.opendaylight.controller.sal.core.UpdateType;
import org.opendaylight.controller.sal.packet.ARP;
import org.opendaylight.controller.sal.packet.Ethernet;
import org.opendaylight.controller.sal.packet.IDataPacketService;
import org.opendaylight.controller.sal.packet.IListenDataPacket;
import org.opendaylight.controller.sal.packet.Packet;
import org.opendaylight.controller.sal.packet.PacketResult;
import org.opendaylight.controller.sal.packet.RawPacket;
import org.opendaylight.controller.sal.packet.address.DataLinkAddress;
import org.opendaylight.controller.sal.packet.address.EthernetAddress;
import org.opendaylight.controller.sal.topology.TopoEdgeUpdate;
import org.opendaylight.controller.sal.utils.HexEncode;
import org.opendaylight.controller.sal.utils.ListenerDispatcher;
import org.opendaylight.controller.sal.utils.MultiIterator;
import org.opendaylight.controller.sal.utils.NetUtils;
import org.opendaylight.controller.sal.utils.SingletonTask;
import org.opendaylight.controller.sal.utils.Status;
import org.opendaylight.controller.sal.utils.StatusCode;
import org.opendaylight.controller.switchmanager.IInventoryListener;
import org.opendaylight.controller.switchmanager.ISwitchManager;
import org.opendaylight.controller.topologymanager.ITopologyManager;
import org.opendaylight.controller.topologymanager.ITopologyManagerAware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * DeviceManager creates Devices based upon MAC addresses seen in the network.
 * It tracks any network addresses mapped to the Device, and its location within
 * the network.
 *
 * @author readams
 */
public class DeviceManagerImpl implements IDeviceService, IEntityClassListener,
        IListenDataPacket, ITopologyManagerAware, IfIptoHost,
        IInventoryListener {
    protected static Logger logger = LoggerFactory
            .getLogger(DeviceManagerImpl.class);

    public static final String MODULE_NAME = "devicemanager";

    private ITopologyManager topology;
    private ISwitchManager switchManager = null;
    private IDataPacketService dataPacketService = null;

    public static final String CNT_INCOMING = MODULE_NAME + "-incoming";
    public static final String CNT_RECONCILE_REQUEST = MODULE_NAME
            + "-reconcileRequest";
    public static final String CNT_RECONCILE_NO_SOURCE = MODULE_NAME
            + "-reconcileNoSourceDevice";
    public static final String CNT_RECONCILE_NO_DEST = MODULE_NAME
            + "-reconcileNoDestDevice";
    public static final String CNT_BROADCAST_SOURCE = MODULE_NAME
            + "-broadcastSource";
    public static final String CNT_NO_SOURCE = MODULE_NAME + "-noSourceDevice";
    public static final String CNT_NO_DEST = MODULE_NAME + "-noDestDevice";
    public static final String CNT_DHCP_CLIENT_NAME_SNOOPED = MODULE_NAME
            + "-dhcpClientNameSnooped";
    public static final String CNT_DEVICE_ON_INTERAL_PORT_NOT_LEARNED = MODULE_NAME
            + "-deviceOnInternalPortNotLearned";
    public static final String CNT_PACKET_NOT_ALLOWED = MODULE_NAME
            + "-packetNotAllowed";
    public static final String CNT_NEW_DEVICE = MODULE_NAME + "-newDevice";
    public static final String CNT_PACKET_ON_INTERNAL_PORT_FOR_KNOWN_DEVICE = MODULE_NAME
            + "-packetOnInternalPortForKnownDevice";
    public static final String CNT_NEW_ENTITY = MODULE_NAME + "-newEntity";
    public static final String CNT_DEVICE_CHANGED = MODULE_NAME
            + "-deviceChanged";
    public static final String CNT_DEVICE_MOVED = MODULE_NAME + "-deviceMoved";
    public static final String CNT_CLEANUP_ENTITIES_RUNS = MODULE_NAME
            + "-cleanupEntitiesRuns";
    public static final String CNT_ENTITY_REMOVED_TIMEOUT = MODULE_NAME
            + "-entityRemovedTimeout";
    public static final String CNT_DEVICE_DELETED = MODULE_NAME
            + "-deviceDeleted";
    public static final String CNT_DEVICE_RECLASSIFY_DELETE = MODULE_NAME
            + "-deviceReclassifyDelete";
    public static final String CNT_DEVICE_STORED = MODULE_NAME
            + "-deviceStored";
    public static final String CNT_DEVICE_STORE_THROTTLED = MODULE_NAME
            + "-deviceStoreThrottled";
    public static final String CNT_DEVICE_REMOVED_FROM_STORE = MODULE_NAME
            + "-deviceRemovedFromStore";
    public static final String CNT_SYNC_EXCEPTION = MODULE_NAME
            + "-syncException";
    public static final String CNT_DEVICES_FROM_STORE = MODULE_NAME
            + "-devicesFromStore";
    public static final String CNT_CONSOLIDATE_STORE_RUNS = MODULE_NAME
            + "-consolidateStoreRuns";
    public static final String CNT_CONSOLIDATE_STORE_DEVICES_REMOVED = MODULE_NAME
            + "-consolidateStoreDevicesRemoved";

    static final String DEVICE_SYNC_STORE_NAME = DeviceManagerImpl.class
            .getCanonicalName() + ".stateStore";


    /**
     * Time in milliseconds before entities will expire
     */
    protected static final int ENTITY_TIMEOUT = 60 * 60 * 1000;

    /**
     * Time in seconds between cleaning up old entities/devices
     */
    protected static final int ENTITY_CLEANUP_INTERVAL = 60 * 60;

    /**
     * This is the master device map that maps device IDs to {@link Device}
     * objects.
     */
    protected ConcurrentHashMap<Long, Device> deviceMap;

    protected ConcurrentHashMap<NodeConnector, Entity> inactiveStaticDevices;
    /**
     * Counter used to generate device keys
     */
    protected long deviceKeyCounter = 0;

    /**
     * Lock for incrementing the device key counter
     */
    protected Object deviceKeyLock = new Object();

    /**
     * This is the primary entity index that contains all entities
     */
    protected DeviceUniqueIndex primaryIndex;

    /**
     * This stores secondary indices over the fields in the devices
     */
    protected Map<EnumSet<DeviceField>, DeviceIndex> secondaryIndexMap;

    /**
     * This map contains state for each of the {@ref IEntityClass} that exist
     */
    protected ConcurrentHashMap<String, ClassState> classStateMap;

    /**
     * This is the list of indices we want on a per-class basis
     */
    protected Set<EnumSet<DeviceField>> perClassIndices;

    /**
     * The entity classifier currently in use
     */
    protected IEntityClassifierService entityClassifier;

    /**
     * Used to cache state about specific entity classes
     */
    protected class ClassState {

        /**
         * The class index
         */
        protected DeviceUniqueIndex classIndex;

        /**
         * This stores secondary indices over the fields in the device for the
         * class
         */
        protected Map<EnumSet<DeviceField>, DeviceIndex> secondaryIndexMap;

        /**
         * Allocate a new {@link ClassState} object for the class
         *
         * @param clazz
         *            the class to use for the state
         */
        public ClassState(IEntityClass clazz) {
            EnumSet<DeviceField> keyFields = clazz.getKeyFields();
            EnumSet<DeviceField> primaryKeyFields = entityClassifier
                    .getKeyFields();
            boolean keyFieldsMatchPrimary = primaryKeyFields.equals(keyFields);

            if (!keyFieldsMatchPrimary)
                classIndex = new DeviceUniqueIndex(keyFields);

            secondaryIndexMap = new HashMap<EnumSet<DeviceField>, DeviceIndex>();
            for (EnumSet<DeviceField> fields : perClassIndices) {
                secondaryIndexMap.put(fields, new DeviceMultiIndex(fields));
            }
        }
    }

    /**
     * Device manager event listeners reclassifyDeviceListeners are notified
     * first before reconcileDeviceListeners. This is to make sure devices are
     * correctly reclassified before reconciliation.
     */
    protected ListenerDispatcher<String, IDeviceListener> deviceListeners;

    /**
     * Using the IfNewHostNotify to notify listeners of host changes.
     */
    private Set<IfNewHostNotify> newHostNotify = Collections
            .synchronizedSet(new HashSet<IfNewHostNotify>());

    /**
     * A device update event to be dispatched
     */
    protected static class DeviceUpdate {
        public enum Change {
            ADD, DELETE, CHANGE;
        }

        /**
         * The affected device
         */
        protected Device device;

        /**
         * The change that was made
         */
        protected Change change;

        /**
         * If not added, then this is the list of fields changed
         */
        protected EnumSet<DeviceField> fieldsChanged;

        public DeviceUpdate(Device device, Change change,
                EnumSet<DeviceField> fieldsChanged) {
            super();
            this.device = device;
            this.change = change;
            this.fieldsChanged = fieldsChanged;
        }

        @Override
        public String toString() {
            String devIdStr = device.getEntityClass().getName() + "::"
                    + device.getMACAddressString();
            return "DeviceUpdate [device=" + devIdStr + ", change=" + change
                    + ", fieldsChanged=" + fieldsChanged + "]";
        }

    }

    /**
     * AttachmentPointComparator
     *
     * Compares two attachment points and returns the latest one. It is assumed
     * that the two attachment points are in the same L2 domain.
     *
     * @author srini
     */
    protected class AttachmentPointComparator implements
            Comparator<AttachmentPoint> {
        public AttachmentPointComparator() {
            super();
        }

        @Override
        public int compare(AttachmentPoint oldAP, AttachmentPoint newAP) {
            // First compare based on L2 domain ID;

            // XXX - missing functionality -- need topology
            // long oldDomain = topology.getL2DomainId(oldSw);
            // boolean oldBD = topology.isBroadcastDomainPort(oldSw, oldPort);
            long oldDomain = 0;
            boolean oldBD = false;

            // XXX - missing functionality -- need topology
            // long newDomain = topology.getL2DomainId(newSw);
            // boolean newBD = topology.isBroadcastDomainPort(newSw, newPort);
            long newDomain = 0;
            boolean newBD = false;

            if (oldDomain < newDomain) {
               return -1;
            } else if (oldDomain > newDomain) {
                return 1;
            }
            // Give preference to OFPP_LOCAL always
            if (!oldAP.getPort().getType().equals(NodeConnectorIDType.SWSTACK)
                    && newAP.getPort().getType()
                            .equals(NodeConnectorIDType.SWSTACK)) {
                return -1;
            } else if (oldAP.getPort().getType()
                    .equals(NodeConnectorIDType.SWSTACK)
                    && !newAP.getPort().getType()
                            .equals(NodeConnectorIDType.SWSTACK)) {
                return 1;
            }

            // We expect that the last seen of the new AP is higher than
            // old AP, if it is not, just reverse and send the negative
            // of the result.
            if (oldAP.getActiveSince() > newAP.getActiveSince())
                return -compare(newAP, oldAP);

            long activeOffset = 0;

            if (!newBD && oldBD) {
                return -1;
            }
            if (newBD && oldBD) {
                activeOffset = AttachmentPoint.EXTERNAL_TO_EXTERNAL_TIMEOUT;
            } else if (newBD && !oldBD) {
                activeOffset = AttachmentPoint.OPENFLOW_TO_EXTERNAL_TIMEOUT;
            }


            if ((newAP.getActiveSince() > oldAP.getLastSeen() + activeOffset)
                    || (newAP.getLastSeen() > oldAP.getLastSeen()
                            + AttachmentPoint.INACTIVITY_INTERVAL)) {
                return -1;
            }
            return 1;
        }
    }

    /**
     * Comparator for sorting by cluster ID
     */
    public AttachmentPointComparator apComparator;

    /**
     * Switch ports where attachment points shouldn't be learned
     */
    private Set<SwitchPort> suppressAPs;

    /**
     * Periodic task to clean up expired entities
     */
    public SingletonTask entityCleanupTask;

    // ********************
    // Dependency injection
    // ********************

    void setNewHostNotify(IfNewHostNotify obj) {
        this.newHostNotify.add(obj);
    }

    void unsetNewHostNotify(IfNewHostNotify obj) {
        this.newHostNotify.remove(obj);
    }

    void setDataPacketService(IDataPacketService s) {
        this.dataPacketService = s;
    }

    void unsetDataPacketService(IDataPacketService s) {
        if (this.dataPacketService == s) {
            this.dataPacketService = null;
        }
    }

    public void setTopologyManager(ITopologyManager s) {
        this.topology = s;
    }

    public void unsetTopologyManager(ITopologyManager s) {
        if (this.topology == s) {
            logger.debug("Topology Manager Service removed!");
            this.topology = null;
        }
    }

    private volatile boolean stopped = true;
    private ScheduledExecutorService ses;

    public void stop() {
        stopped = true;
        if (ses != null)
            ses.shutdownNow();
    }

    public void start() {
        this.perClassIndices = new HashSet<EnumSet<DeviceField>>();

        // XXX - TODO need to make it possible to register a non-default
        // classifier
        entityClassifier = new DefaultEntityClassifier();
        this.deviceListeners = new ListenerDispatcher<String, IDeviceListener>();
        this.suppressAPs = Collections
                .newSetFromMap(new ConcurrentHashMap<SwitchPort, Boolean>());
        primaryIndex = new DeviceUniqueIndex(entityClassifier.getKeyFields());
        secondaryIndexMap = new HashMap<EnumSet<DeviceField>, DeviceIndex>();

        deviceMap = new ConcurrentHashMap<Long, Device>();
        inactiveStaticDevices = new ConcurrentHashMap<NodeConnector, Entity>();
        classStateMap = new ConcurrentHashMap<String, ClassState>();
        apComparator = new AttachmentPointComparator();

        addIndex(true, EnumSet.of(DeviceField.IPV4));

        stopped = false;
        // XXX - Should use a common threadpool but this doesn't currently exist
        ses = Executors.newScheduledThreadPool(1);
        Runnable ecr = new Runnable() {
            @Override
            public void run() {
                cleanupEntities();
                if (!stopped)
                    entityCleanupTask.reschedule(ENTITY_CLEANUP_INTERVAL,
                            TimeUnit.SECONDS);
            }
        };
        entityCleanupTask = new SingletonTask(ses, ecr);
        entityCleanupTask.reschedule(ENTITY_CLEANUP_INTERVAL, TimeUnit.SECONDS);

        registerDeviceManagerDebugCounters();
    }

    /**
     * Periodic task to consolidate entries in the store. I.e., delete entries
     * in the store that are not known to DeviceManager
     */
    // XXX - Missing functionality
    // private SingletonTask storeConsolidateTask;

    // *********************
    // IDeviceManagerService
    // *********************

    void setSwitchManager(ISwitchManager s) {
        logger.debug("SwitchManager set");
        this.switchManager = s;
    }

    void unsetSwitchManager(ISwitchManager s) {
        if (this.switchManager == s) {
            logger.debug("SwitchManager removed!");
            this.switchManager = null;
        }
    }

    @Override
    public IDevice getDevice(Long deviceKey) {
        return deviceMap.get(deviceKey);
    }

    @Override
    public IDevice findDevice(long macAddress, Short vlan, Integer ipv4Address,
            NodeConnector port) throws IllegalArgumentException {
        if (vlan != null && vlan.shortValue() <= 0)
            vlan = null;
        if (ipv4Address != null && ipv4Address == 0)
            ipv4Address = null;
        Entity e = new Entity(macAddress, vlan, ipv4Address, port, null);
        if (!allKeyFieldsPresent(e, entityClassifier.getKeyFields())) {
            throw new IllegalArgumentException("Not all key fields specified."
                    + " Required fields: " + entityClassifier.getKeyFields());
        }
        return findDeviceByEntity(e);
    }

    @Override
    public IDevice findClassDevice(IEntityClass entityClass, long macAddress,
            Short vlan, Integer ipv4Address) throws IllegalArgumentException {
        if (vlan != null && vlan.shortValue() <= 0)
            vlan = null;
        if (ipv4Address != null && ipv4Address == 0)
            ipv4Address = null;
        Entity e = new Entity(macAddress, vlan, ipv4Address, null, null);
        if (entityClass == null
                || !allKeyFieldsPresent(e, entityClass.getKeyFields())) {
            throw new IllegalArgumentException("Not all key fields and/or "
                    + " no source device specified. Required fields: "
                    + entityClassifier.getKeyFields());
        }
        return findDestByEntity(entityClass, e);
    }

    @Override
    public Collection<? extends IDevice> getAllDevices() {
        return Collections.unmodifiableCollection(deviceMap.values());
    }

    @Override
    public void addIndex(boolean perClass, EnumSet<DeviceField> keyFields) {
        if (perClass) {
            perClassIndices.add(keyFields);
        } else {
            secondaryIndexMap.put(keyFields, new DeviceMultiIndex(keyFields));
        }
    }

    @Override
    public Iterator<? extends IDevice> queryDevices(Long macAddress,
            Short vlan, Integer ipv4Address, NodeConnector port) {
        DeviceIndex index = null;
        if (secondaryIndexMap.size() > 0) {
            EnumSet<DeviceField> keys = getEntityKeys(macAddress, vlan,
                    ipv4Address, port);
            index = secondaryIndexMap.get(keys);
        }

        Iterator<Device> deviceIterator = null;
        if (index == null) {
            // Do a full table scan
            deviceIterator = deviceMap.values().iterator();
        } else {
            // index lookup
            Entity entity = new Entity((macAddress == null ? 0 : macAddress),
                    vlan, ipv4Address, port, null);
            deviceIterator = new DeviceIndexInterator(this,
                    index.queryByEntity(entity));
        }

        DeviceIterator di = new DeviceIterator(deviceIterator, null,
                macAddress, vlan, ipv4Address, port);
        return di;
    }

    @Override
    public Iterator<? extends IDevice> queryClassDevices(
            IEntityClass entityClass, Long macAddress, Short vlan,
            Integer ipv4Address, NodeConnector port) {
        ArrayList<Iterator<Device>> iterators = new ArrayList<Iterator<Device>>();
        ClassState classState = getClassState(entityClass);

        DeviceIndex index = null;
        if (classState.secondaryIndexMap.size() > 0) {
            EnumSet<DeviceField> keys = getEntityKeys(macAddress, vlan,
                    ipv4Address, port);
            index = classState.secondaryIndexMap.get(keys);
        }

        Iterator<Device> iter;
        if (index == null) {
            index = classState.classIndex;
            if (index == null) {
                // scan all devices
                return new DeviceIterator(deviceMap.values().iterator(),
                        new IEntityClass[] { entityClass }, macAddress, vlan,
                        ipv4Address, port);
            } else {
                // scan the entire class
                iter = new DeviceIndexInterator(this, index.getAll());
            }
        } else {
            // index lookup
            Entity entity = new Entity((macAddress == null ? 0 : macAddress),
                    vlan, ipv4Address, port, null);
            iter = new DeviceIndexInterator(this, index.queryByEntity(entity));
        }
        iterators.add(iter);

        return new MultiIterator<Device>(iterators.iterator());
    }

    protected Iterator<Device> getDeviceIteratorForQuery(Long macAddress,
            Short vlan, Integer ipv4Address, NodeConnector port) {
        DeviceIndex index = null;
        if (secondaryIndexMap.size() > 0) {
            EnumSet<DeviceField> keys = getEntityKeys(macAddress, vlan,
                    ipv4Address, port);
            index = secondaryIndexMap.get(keys);
        }

        Iterator<Device> deviceIterator = null;
        if (index == null) {
            // Do a full table scan
            deviceIterator = deviceMap.values().iterator();
        } else {
            // index lookup
            Entity entity = new Entity((macAddress == null ? 0 : macAddress),
                    vlan, ipv4Address, port, null);
            deviceIterator = new DeviceIndexInterator(this,
                    index.queryByEntity(entity));
        }

        DeviceIterator di = new DeviceIterator(deviceIterator, null,
                macAddress, vlan, ipv4Address, port);
        return di;
    }

    @Override
    public void addListener(IDeviceListener listener) {
        deviceListeners.addListener("device", listener);
        logListeners();
    }

    @Override
    public void addSuppressAPs(NodeConnector port) {
        suppressAPs.add(new SwitchPort(port));
    }

    @Override
    public void removeSuppressAPs(NodeConnector port) {
        suppressAPs.remove(new SwitchPort(port));
    }

    @Override
    public Set<SwitchPort> getSuppressAPs() {
        return Collections.unmodifiableSet(suppressAPs);
    }

    private void logListeners() {
        List<IDeviceListener> listeners = deviceListeners.getOrderedListeners();
        if (listeners != null) {
            StringBuffer sb = new StringBuffer();
            sb.append("DeviceListeners: ");
            for (IDeviceListener l : listeners) {
                sb.append(l.getName());
                sb.append(",");
            }
            logger.debug(sb.toString());
        }
    }


    // *****************
    // IListenDataPacket
    // *****************

    @Override
    public PacketResult receiveDataPacket(RawPacket inPkt) {
        // XXX - Can this really pass in null? Why would you ever want that?
        if (inPkt == null) {
            return PacketResult.IGNORED;
        }

        Packet formattedPak = this.dataPacketService.decodeDataPacket(inPkt);
        Ethernet eth;
        if (formattedPak instanceof Ethernet) {
            eth = (Ethernet) formattedPak;
        } else {
            return PacketResult.IGNORED;
        }

        // Extract source entity information
        NodeConnector inPort = inPkt.getIncomingNodeConnector();
        Entity srcEntity = getSourceEntityFromPacket(eth, inPort);
        if (srcEntity == null) {
            return PacketResult.CONSUME;
        }

        // Learn from ARP packet for special VRRP settings.
        // In VRRP settings, the source MAC address and sender MAC
        // addresses can be different. In such cases, we need to learn
        // the IP to MAC mapping of the VRRP IP address. The source
        // entity will not have that information. Hence, a separate call
        // to learn devices in such cases.
        learnDeviceFromArpResponseData(eth, inPort);

        // Learn/lookup device information
        Device srcDevice = learnDeviceByEntity(srcEntity);
        if (srcDevice == null) {
            return PacketResult.CONSUME;
        }
        logger.trace("Saw packet from device {}", srcDevice);

        return PacketResult.KEEP_PROCESSING;
    }

    // ****************
    // Internal methods
    // ****************


    /**
     * Check whether the given attachment point is valid given the current
     * topology
     *
     * @param switchDPID
     *            the DPID
     * @param switchPort
     *            the port
     * @return true if it's a valid attachment point
     */
    public boolean isValidAttachmentPoint(NodeConnector port) {
        // XXX - missing functionality -- need topology module
        // if (topology.isAttachmentPointPort(port) == false)
        // return false;
        if (topology.isInternal(port))
            return false;
        if (!switchManager.isNodeConnectorEnabled(port))
            return false;
        if (suppressAPs.contains(new SwitchPort(port)))
            return false;

        return true;
    }

    /**
     * Get sender IP address from packet if the packet is either an ARP packet.
     *
     * @param eth
     * @param dlAddr
     * @return
     */
    private int getSrcNwAddr(Ethernet eth, long dlAddr) {
        if (eth.getPayload() instanceof ARP) {
            ARP arp = (ARP) eth.getPayload();
            if ((arp.getProtocolType() == ARP.PROTO_TYPE_IP)
                    && (toLong(arp.getSenderHardwareAddress()) == dlAddr)) {
                return toIPv4Address(arp.getSenderProtocolAddress());
            }
        }
        return 0;
    }

    /**
     * Parse an entity from an {@link Ethernet} packet.
     *
     * @param eth
     *            the packet to parse
     * @param sw
     *            the switch on which the packet arrived
     * @param pi
     *            the original packetin
     * @return the entity from the packet
     */
    protected Entity getSourceEntityFromPacket(Ethernet eth, NodeConnector port) {
        byte[] dlAddrArr = eth.getSourceMACAddress();
        long dlAddr = toLong(dlAddrArr);

        // Ignore broadcast/multicast source
        if ((dlAddrArr[0] & 0x1) != 0)
            return null;

        // XXX missing functionality
        // short vlan = 0;
        int nwSrc = getSrcNwAddr(eth, dlAddr);
        return new Entity(dlAddr, null, ((nwSrc != 0) ? nwSrc : null), port,
                new Date());
    }

    /**
     * Learn device from ARP data in scenarios where the Ethernet source MAC is
     * different from the sender hardware address in ARP data.
     */
    protected void learnDeviceFromArpResponseData(Ethernet eth,
            NodeConnector port) {

        if (!(eth.getPayload() instanceof ARP))
            return;
        ARP arp = (ARP) eth.getPayload();

        byte[] dlAddrArr = eth.getSourceMACAddress();
        long dlAddr = toLong(dlAddrArr);

        byte[] senderHardwareAddr = arp.getSenderHardwareAddress();
        long senderAddr = toLong(senderHardwareAddr);

        if (dlAddr == senderAddr)
            return;

        // Ignore broadcast/multicast source
        if ((senderHardwareAddr[0] & 0x1) != 0)
            return;

        // short vlan = eth.getVlanID();
        int nwSrc = toIPv4Address(arp.getSenderProtocolAddress());

        Entity e = new Entity(senderAddr, null, ((nwSrc != 0) ? nwSrc : null),
                port, new Date());

        learnDeviceByEntity(e);
    }

    /**
     * Look up a {@link Device} based on the provided {@link Entity}. We first
     * check the primary index. If we do not find an entry there we classify the
     * device into its IEntityClass and query the classIndex. This implies that
     * all key field of the current IEntityClassifier must be present in the
     * entity for the lookup to succeed!
     *
     * @param entity
     *            the entity to search for
     * @return The {@link Device} object if found
     */
    protected Device findDeviceByEntity(Entity entity) {
        // Look up the fully-qualified entity to see if it already
        // exists in the primary entity index.
        Long deviceKey = primaryIndex.findByEntity(entity);
        IEntityClass entityClass = null;

        if (deviceKey == null) {
            // If the entity does not exist in the primary entity index,
            // use the entity classifier for find the classes for the
            // entity. Look up the entity in the returned class'
            // class entity index.
            entityClass = entityClassifier.classifyEntity(entity);
            if (entityClass == null) {
                return null;
            }
            ClassState classState = getClassState(entityClass);

            if (classState.classIndex != null) {
                deviceKey = classState.classIndex.findByEntity(entity);
            }
        }
        if (deviceKey == null)
            return null;
        return deviceMap.get(deviceKey);
    }

    /**
     * Get a destination device using entity fields that corresponds with the
     * given source device. The source device is important since there could be
     * ambiguity in the destination device without the attachment point
     * information.
     *
     * @param reference
     *            the source device's entity class. The returned destination
     *            will be in the same entity class as the source.
     * @param dstEntity
     *            the entity to look up
     * @return an {@link Device} or null if no device is found.
     */
    protected Device findDestByEntity(IEntityClass reference, Entity dstEntity) {

        // Look up the fully-qualified entity to see if it
        // exists in the primary entity index
        Long deviceKey = primaryIndex.findByEntity(dstEntity);

        if (deviceKey == null) {
            // This could happen because:
            // 1) no destination known, or a broadcast destination
            // 2) if we have attachment point key fields since
            // attachment point information isn't available for
            // destination devices.
            // For the second case, we'll need to match up the
            // destination device with the class of the source
            // device.
            ClassState classState = getClassState(reference);
            if (classState.classIndex == null) {
                return null;
            }
            deviceKey = classState.classIndex.findByEntity(dstEntity);
        }
        if (deviceKey == null)
            return null;
        return deviceMap.get(deviceKey);
    }

    /**
     * Look up a {@link Device} within a particular entity class based on the
     * provided {@link Entity}.
     *
     * @param clazz
     *            the entity class to search for the entity
     * @param entity
     *            the entity to search for
     * @return The {@link Device} object if found private Device
     *         findDeviceInClassByEntity(IEntityClass clazz, Entity entity) { //
     *         XXX - TODO throw new UnsupportedOperationException(); }
     */

    /**
     * Look up a {@link Device} based on the provided {@link Entity}. Also
     * learns based on the new entity, and will update existing devices as
     * required.
     *
     * @param entity
     *            the {@link Entity}
     * @return The {@link Device} object if found
     */
    protected Device learnDeviceByEntity(Entity entity) {
        logger.info("Primary index {}", primaryIndex);
        ArrayList<Long> deleteQueue = null;
        LinkedList<DeviceUpdate> deviceUpdates = null;
        Device oldDevice = null;
        Device device = null;

        // we may need to restart the learning process if we detect
        // concurrent modification. Note that we ensure that at least
        // one thread should always succeed so we don't get into infinite
        // starvation loops
        while (true) {
            deviceUpdates = null;

            // Look up the fully-qualified entity to see if it already
            // exists in the primary entity index.
            Long deviceKey = primaryIndex.findByEntity(entity);
            IEntityClass entityClass = null;

            if (deviceKey == null) {
                // If the entity does not exist in the primary entity index,
                // use the entity classifier for find the classes for the
                // entity. Look up the entity in the returned class'
                // class entity index.
                entityClass = entityClassifier.classifyEntity(entity);
                if (entityClass == null) {
                    // could not classify entity. No device
                    device = null;
                    break;
                }
                ClassState classState = getClassState(entityClass);

                if (classState.classIndex != null) {
                    deviceKey = classState.classIndex.findByEntity(entity);
                }
            }
            if (deviceKey != null) {
                // If the primary or secondary index contains the entity
                // use resulting device key to look up the device in the
                // device map, and use the referenced Device below.
                device = deviceMap.get(deviceKey);
                if (device == null) {
                    // This can happen due to concurrent modification
                    if (logger.isDebugEnabled()) {
                        logger.debug("No device for deviceKey {} while "
                                + "while processing entity {}", deviceKey,
                                entity);
                    }
                    // if so, then try again till we don't even get the device
                    // key
                    // and so we recreate the device
                    continue;
                }
            } else {
                // If the secondary index does not contain the entity,
                // create a new Device object containing the entity, and
                // generate a new device ID if the the entity is on an
                // attachment point port. Otherwise ignore.
                if (entity.hasSwitchPort()
                        && !isValidAttachmentPoint(entity.getPort())) {
                    // debugCounters.updateCounter(CNT_DEVICE_ON_INTERAL_PORT_NOT_LEARNED);
                    if (logger.isDebugEnabled()) {
                        logger.debug("Not learning new device on internal"
                                + " link: {}", entity);
                    }
                    device = null;
                    break;
                }
                // Before we create the new device also check if
                // the entity is allowed (e.g., for spoofing protection)
                if (!isEntityAllowed(entity, entityClass)) {
                    // debugCounters.updateCounter(CNT_PACKET_NOT_ALLOWED);
                    if (logger.isDebugEnabled()) {
                        logger.debug("PacketIn is not allowed {} {}",
                                entityClass.getName(), entity);
                    }
                    device = null;
                    break;
                }
                synchronized (deviceKeyLock) {
                    deviceKey = Long.valueOf(deviceKeyCounter++);
                }
                device = allocateDevice(deviceKey, entity, entityClass);

                // Add the new device to the primary map with a simple put
                deviceMap.put(deviceKey, device);

                // update indices
                if (!updateIndices(device, deviceKey)) {
                    if (deleteQueue == null)
                        deleteQueue = new ArrayList<Long>();
                    deleteQueue.add(deviceKey);
                    continue;
                }

                updateSecondaryIndices(entity, entityClass, deviceKey);

                // We need to count and log here. If we log earlier we could
                // hit a concurrent modification and restart the dev creation
                // and potentially count the device twice.
                // debugCounters.updateCounter(CNT_NEW_DEVICE);
                if (logger.isDebugEnabled()) {
                    logger.debug(
                            "New device created: {} deviceKey={}, entity={}",
                            new Object[] { device, deviceKey, entity });
                }
                // generate new device update
                deviceUpdates = updateUpdates(deviceUpdates, new DeviceUpdate(
                        device, ADD, null));

                break;
            }
            // if it gets here, we have a pre-existing Device for this Entity
            if (!isEntityAllowed(entity, device.getEntityClass())) {
                // debugCounters.updateCounter(CNT_PACKET_NOT_ALLOWED);
                if (logger.isDebugEnabled()) {
                    logger.info("PacketIn is not allowed {} {}", device
                            .getEntityClass().getName(), entity);
                }
                return null;
            }
            // If this is not an attachment point port we don't learn the new
            // entity
            // and don't update indexes. But we do allow the device to continue
            // up
            // the chain.
            if (entity.hasSwitchPort()
                    && !isValidAttachmentPoint(entity.getPort())) {
                // debugCounters.updateCounter(CNT_PACKET_ON_INTERNAL_PORT_FOR_KNOWN_DEVICE);
                break;
            }
            int entityindex = -1;
            if ((entityindex = device.entityIndex(entity)) >= 0) {
                // Entity already exists
                // update timestamp on the found entity
                Date lastSeen = entity.getLastSeenTimestamp();
                if (lastSeen == null) {
                    lastSeen = new Date();
                    entity.setLastSeenTimestamp(lastSeen);
                }
                device.entities[entityindex].setLastSeenTimestamp(lastSeen);
                // we break the loop after checking for changes to the AP
            } else {
                // New entity for this device
                // compute the insertion point for the entity.
                // see Arrays.binarySearch()
                entityindex = -(entityindex + 1);
                Device newDevice = allocateDevice(device, entity, entityindex);

                // generate updates
                EnumSet<DeviceField> changedFields = findChangedFields(device,
                        entity);

                // update the device map with a replace call
                boolean res = deviceMap.replace(deviceKey, device, newDevice);
                // If replace returns false, restart the process from the
                // beginning (this implies another thread concurrently
                // modified this Device).
                if (!res)
                    continue;
                oldDevice = device;
                device = newDevice;
                // update indices
                if (!updateIndices(device, deviceKey)) {
                    continue;
                }
                updateSecondaryIndices(entity, device.getEntityClass(),
                        deviceKey);

                // We need to count here after all the possible "continue"
                // statements in this branch
                // debugCounters.updateCounter(CNT_NEW_ENTITY);
                if (changedFields.size() > 0) {
                    // debugCounters.updateCounter(CNT_DEVICE_CHANGED);
                    deviceUpdates = updateUpdates(deviceUpdates,
                            new DeviceUpdate(newDevice, CHANGE, changedFields));
                }
                // we break the loop after checking for changed AP
            }
            // Update attachment point (will only be hit if the device
            // already existed and no concurrent modification)
            if (entity.hasSwitchPort()) {
                boolean moved = device.updateAttachmentPoint(entity.getPort(),
                        entity.getLastSeenTimestamp().getTime());
                // TODO: use update mechanism instead of sending the
                // notification directly
                if (moved) {
                    // we count device moved events in
                    // sendDeviceMovedNotification()
                    sendDeviceMovedNotification(device, oldDevice);
                    if (logger.isTraceEnabled()) {
                        logger.trace("Device moved: attachment points {},"
                                + "entities {}", device.attachmentPoints,
                                device.entities);
                    }
                } else {
                    if (logger.isTraceEnabled()) {
                        logger.trace("Device attachment point updated: "
                                + "attachment points {}," + "entities {}",
                                device.attachmentPoints, device.entities);
                    }
                }
            }
            break;
        }

        if (deleteQueue != null) {
            for (Long l : deleteQueue) {
                Device dev = deviceMap.get(l);
                this.deleteDevice(dev);
            }
        }

        processUpdates(deviceUpdates);
        // deviceSyncManager.storeDeviceThrottled(device);

        return device;
    }

    protected boolean isEntityAllowed(Entity entity, IEntityClass entityClass) {
        return true;
    }

    protected EnumSet<DeviceField> findChangedFields(Device device,
            Entity newEntity) {
        EnumSet<DeviceField> changedFields = EnumSet.of(DeviceField.IPV4,
                DeviceField.VLAN, DeviceField.SWITCHPORT);

        if (newEntity.getIpv4Address() == null)
            changedFields.remove(DeviceField.IPV4);
        if (newEntity.getVlan() == null)
            changedFields.remove(DeviceField.VLAN);
        if (newEntity.getPort() == null)
            changedFields.remove(DeviceField.SWITCHPORT);

        if (changedFields.size() == 0)
            return changedFields;

        for (Entity entity : device.getEntities()) {
            if (newEntity.getIpv4Address() == null
                    || (entity.getIpv4Address() != null && entity
                            .getIpv4Address()
                            .equals(newEntity.getIpv4Address())))
                changedFields.remove(DeviceField.IPV4);
            if (newEntity.getVlan() == null
                    || (entity.getVlan() != null && entity.getVlan().equals(
                            newEntity.getVlan())))
                changedFields.remove(DeviceField.VLAN);
            if (newEntity.getPort() == null
                    || (entity.getPort() != null && entity.getPort().equals(
                            newEntity.getPort())))
                changedFields.remove(DeviceField.SWITCHPORT);
        }

        return changedFields;
    }

    /**
     * Send update notifications to listeners
     *
     * @param updates
     *            the updates to process.
     */
    protected void processUpdates(Queue<DeviceUpdate> updates) {
        if (updates == null)
            return;
        DeviceUpdate update = null;
        while (null != (update = updates.poll())) {
            if (logger.isTraceEnabled()) {
                logger.trace("Dispatching device update: {}", update);
            }
            // if (update.change == DeviceUpdate.Change.DELETE)
            // deviceSyncManager.removeDevice(update.device);
            // else
            // deviceSyncManager.storeDevice(update.device);
            List<IDeviceListener> listeners = deviceListeners
                    .getOrderedListeners();
            notifyListeners(listeners, update);
        }
    }

    protected void notifyListeners(List<IDeviceListener> listeners,
            DeviceUpdate update) {
        // Topology update is for some reason outside of listeners registry
        // logic
        Entity[] ents = update.device.getEntities();
        Entity e = ents[ents.length - 1];

        NodeConnector p = e.getPort();
        Node node = p.getNode();
        Host h = null;
        try {

            byte[] mac = NetUtils.longToByteArray6(e.getMacAddress());
            DataLinkAddress dla = new EthernetAddress(
                    mac);
            e.getIpv4Address();
            InetAddress.getAllByName(e.getIpv4Address().toString());
            h = new org.opendaylight.controller.sal.core.Host(dla,
                    InetAddress.getByName(e.getIpv4Address().toString()));
        } catch (ConstructionException ce) {
            p = null;
            h = null;
        } catch (UnknownHostException ue) {
            p = null;
            h = null;
        }

        if (topology != null && p != null && h != null) {
            if (update.change.equals(DeviceUpdate.Change.ADD)) {
                Tier tier = new Tier(1);
                switchManager.setNodeProp(node, tier);
                topology.updateHostLink(p, h, UpdateType.ADDED, null);
            } else {
                // No need to reset the tiering if no other hosts are currently
                // connected
                // If this switch was discovered to be an access switch, it
                // still is even if the host is down
                Tier tier = new Tier(0);
                switchManager.setNodeProp(node, tier);
                topology.updateHostLink(p, h, UpdateType.REMOVED, null);
            }
        }

        if (listeners == null && newHostNotify.isEmpty()) {
            return;
        }
        /**
         * TODO: IfNewHostNotify is needed for current controller API. Adding
         * logic so that existing apps (like SimpleForwardingManager) work.
         * IDeviceListener adds additional methods and uses IListener's callback
         * ordering. The two interfaces need to be merged.
         */

        for (IfNewHostNotify notify : newHostNotify) {
            switch (update.change) {
            case ADD:
                notify.notifyHTClient(update.device.toHostNodeConnector());
                break;
            case DELETE:
                notify.notifyHTClientHostRemoved(update.device
                        .toHostNodeConnector());
                break;
            case CHANGE:
            }
        }

        /**
         * TODO: Remove this section as IDeviceListener functionality gets
         * merged with IfNewHostNotify
         */
        for (IDeviceListener listener : listeners) {
            switch (update.change) {
            case ADD:
                listener.deviceAdded(update.device);
                break;
            case DELETE:
                listener.deviceRemoved(update.device);
                break;
            case CHANGE:
                for (DeviceField field : update.fieldsChanged) {
                    switch (field) {
                    case IPV4:
                        listener.deviceIPV4AddrChanged(update.device);
                        break;
                    case SWITCHPORT:
                        // listener.deviceMoved(update.device);
                        break;
                    case VLAN:
                        listener.deviceVlanChanged(update.device);
                        break;
                    default:
                        logger.debug("Unknown device field changed {}",
                                update.fieldsChanged.toString());
                        break;
                    }
                }
                break;
            }
        }
    }

    /**
     * Check if the entity e has all the keyFields set. Returns false if not
     *
     * @param e
     *            entity to check
     * @param keyFields
     *            the key fields to check e against
     * @return
     */
    protected boolean allKeyFieldsPresent(Entity e,
            EnumSet<DeviceField> keyFields) {
        for (DeviceField f : keyFields) {
            switch (f) {
            case MAC:
                // MAC address is always present
                break;
            case IPV4:
                if (e.getIpv4Address() == null)
                    return false;
                break;
            case SWITCHPORT:
                if (e.getPort() == null)
                    return false;
                break;
            case VLAN:
                // FIXME: vlan==null is ambiguous: it can mean: not present
                // or untagged
                // if (e.vlan == null) return false;
                break;
            default:
                // we should never get here. unless somebody extended
                // DeviceFields
                throw new IllegalStateException();
            }
        }
        return true;
    }

    private LinkedList<DeviceUpdate> updateUpdates(
            LinkedList<DeviceUpdate> list, DeviceUpdate update) {
        if (update == null)
            return list;
        if (list == null)
            list = new LinkedList<DeviceUpdate>();
        list.add(update);

        return list;
    }

    /**
     * Get the secondary index for a class. Will return null if the secondary
     * index was created concurrently in another thread.
     *
     * @param clazz
     *            the class for the index
     * @return
     */
    private ClassState getClassState(IEntityClass clazz) {
        ClassState classState = classStateMap.get(clazz.getName());
        if (classState != null)
            return classState;

        classState = new ClassState(clazz);
        ClassState r = classStateMap.putIfAbsent(clazz.getName(), classState);
        if (r != null) {
            // concurrent add
            return r;
        }
        return classState;
    }

    /**
     * Update both the primary and class indices for the provided device. If the
     * update fails because of an concurrent update, will return false.
     *
     * @param device
     *            the device to update
     * @param deviceKey
     *            the device key for the device
     * @return true if the update succeeded, false otherwise.
     */
    private boolean updateIndices(Device device, Long deviceKey) {
        if (!primaryIndex.updateIndex(device, deviceKey)) {
            return false;
        }
        IEntityClass entityClass = device.getEntityClass();
        ClassState classState = getClassState(entityClass);

        if (classState.classIndex != null) {
            if (!classState.classIndex.updateIndex(device, deviceKey))
                return false;
        }
        return true;
    }

    /**
     * Update the secondary indices for the given entity and associated entity
     * classes
     *
     * @param entity
     *            the entity to update
     * @param entityClass
     *            the entity class for the entity
     * @param deviceKey
     *            the device key to set up
     */
    private void updateSecondaryIndices(Entity entity,
            IEntityClass entityClass, Long deviceKey) {
        for (DeviceIndex index : secondaryIndexMap.values()) {
            index.updateIndex(entity, deviceKey);
        }
        ClassState state = getClassState(entityClass);
        for (DeviceIndex index : state.secondaryIndexMap.values()) {
            index.updateIndex(entity, deviceKey);
        }
    }

    /**
     * Clean up expired entities/devices
     */
    protected void cleanupEntities() {
        // debugCounters.updateCounter(CNT_CLEANUP_ENTITIES_RUNS);

        Calendar c = Calendar.getInstance();
        c.add(Calendar.MILLISECOND, -ENTITY_TIMEOUT);
        Date cutoff = c.getTime();

        ArrayList<Entity> toRemove = new ArrayList<Entity>();
        ArrayList<Entity> toKeep = new ArrayList<Entity>();

        Iterator<Device> diter = deviceMap.values().iterator();
        LinkedList<DeviceUpdate> deviceUpdates = new LinkedList<DeviceUpdate>();

        while (diter.hasNext()) {
            Device d = diter.next();

            while (true) {
                deviceUpdates.clear();
                toRemove.clear();
                toKeep.clear();
                for (Entity e : d.getEntities()) {
                    if (e.getLastSeenTimestamp() != null
                            && 0 > e.getLastSeenTimestamp().compareTo(cutoff)) {
                        // individual entity needs to be removed
                        toRemove.add(e);
                    } else {
                        toKeep.add(e);
                    }
                }
                if (toRemove.size() == 0) {
                    break;
                }

                // debugCounters.updateCounter(CNT_ENTITY_REMOVED_TIMEOUT);
                for (Entity e : toRemove) {
                    removeEntity(e, d.getEntityClass(), d.getDeviceKey(),
                            toKeep);
                }

                if (toKeep.size() > 0) {
                    Device newDevice = allocateDevice(d.getDeviceKey(),
                            d.getDHCPClientName(), d.oldAPs,
                            d.attachmentPoints, toKeep, d.getEntityClass());

                    EnumSet<DeviceField> changedFields = EnumSet
                            .noneOf(DeviceField.class);
                    for (Entity e : toRemove) {
                        changedFields.addAll(findChangedFields(newDevice, e));
                    }
                    DeviceUpdate update = null;
                    if (changedFields.size() > 0) {
                        update = new DeviceUpdate(d, CHANGE, changedFields);
                    }

                    if (!deviceMap.replace(newDevice.getDeviceKey(), d,
                            newDevice)) {
                        // concurrent modification; try again
                        // need to use device that is the map now for the next
                        // iteration
                        d = deviceMap.get(d.getDeviceKey());
                        if (null != d)
                            continue;
                    }
                    if (update != null) {
                        // need to count after all possibly continue stmts in
                        // this branch
                        // debugCounters.updateCounter(CNT_DEVICE_CHANGED);
                        deviceUpdates.add(update);
                    }
                } else {
                    DeviceUpdate update = new DeviceUpdate(d, DELETE, null);
                    if (!deviceMap.remove(d.getDeviceKey(), d)) {
                        // concurrent modification; try again
                        // need to use device that is the map now for the next
                        // iteration
                        d = deviceMap.get(d.getDeviceKey());
                        if (null != d)
                            continue;
                        // debugCounters.updateCounter(CNT_DEVICE_DELETED);
                    }
                    deviceUpdates.add(update);
                }
                processUpdates(deviceUpdates);
                break;
            }
        }
    }

    protected void removeEntity(Entity removed, IEntityClass entityClass,
            Long deviceKey, Collection<Entity> others) {
        // Don't count in this method. This method CAN BE called to clean-up
        // after concurrent device adds/updates and thus counting here
        // is misleading
        for (DeviceIndex index : secondaryIndexMap.values()) {
            index.removeEntityIfNeeded(removed, deviceKey, others);
        }
        ClassState classState = getClassState(entityClass);
        for (DeviceIndex index : classState.secondaryIndexMap.values()) {
            index.removeEntityIfNeeded(removed, deviceKey, others);
        }

        primaryIndex.removeEntityIfNeeded(removed, deviceKey, others);

        if (classState.classIndex != null) {
            classState.classIndex.removeEntityIfNeeded(removed, deviceKey,
                    others);
        }
    }

    /**
     * method to delete a given device, remove all entities first and then
     * finally delete the device itself.
     *
     * @param device
     */
    protected void deleteDevice(Device device) {
        // Don't count in this method. This method CAN BE called to clean-up
        // after concurrent device adds/updates and thus counting here
        // is misleading
        ArrayList<Entity> emptyToKeep = new ArrayList<Entity>();
        for (Entity entity : device.getEntities()) {
            this.removeEntity(entity, device.getEntityClass(),
                    device.getDeviceKey(), emptyToKeep);
        }
        if (!deviceMap.remove(device.getDeviceKey(), device)) {
            if (logger.isDebugEnabled())
                logger.debug("device map does not have this device -"
                        + device.toString());
        }
    }

    private EnumSet<DeviceField> getEntityKeys(Long macAddress, Short vlan,
            Integer ipv4Address, NodeConnector port) {
        // FIXME: vlan==null is a valid search. Need to handle this
        // case correctly. Note that the code will still work correctly.
        // But we might do a full device search instead of using an index.
        EnumSet<DeviceField> keys = EnumSet.noneOf(DeviceField.class);
        if (macAddress != null)
            keys.add(DeviceField.MAC);
        if (vlan != null)
            keys.add(DeviceField.VLAN);
        if (ipv4Address != null)
            keys.add(DeviceField.IPV4);
        if (port != null)
            keys.add(DeviceField.SWITCHPORT);
        return keys;
    }

    protected Iterator<Device> queryClassByEntity(IEntityClass clazz,
            EnumSet<DeviceField> keyFields, Entity entity) {
        ClassState classState = getClassState(clazz);
        DeviceIndex index = classState.secondaryIndexMap.get(keyFields);
        if (index == null)
            return Collections.<Device> emptySet().iterator();
        return new DeviceIndexInterator(this, index.queryByEntity(entity));
    }

    protected Device allocateDevice(Long deviceKey, Entity entity,
            IEntityClass entityClass) {
        return new Device(this, deviceKey, entity, entityClass);
    }

    // TODO: FIX THIS.
    protected Device allocateDevice(Long deviceKey, String dhcpClientName,
            List<AttachmentPoint> aps, List<AttachmentPoint> trueAPs,
            Collection<Entity> entities, IEntityClass entityClass) {
        return new Device(this, deviceKey, dhcpClientName, aps, trueAPs,
                entities, entityClass);
    }

    protected Device allocateDevice(Device device, Entity entity,
            int insertionpoint) {
        return new Device(device, entity, insertionpoint);
    }

    // not used
    protected Device allocateDevice(Device device, Set<Entity> entities) {
        List<AttachmentPoint> newPossibleAPs = new ArrayList<AttachmentPoint>();
        List<AttachmentPoint> newAPs = new ArrayList<AttachmentPoint>();
        for (Entity entity : entities) {
            if (entity.getPort() != null) {
                AttachmentPoint aP = new AttachmentPoint(entity.getPort(), 0);
                newPossibleAPs.add(aP);
            }
        }
        if (device.attachmentPoints != null) {
            for (AttachmentPoint oldAP : device.attachmentPoints) {
                if (newPossibleAPs.contains(oldAP)) {
                    newAPs.add(oldAP);
                }
            }
        }
        if (newAPs.isEmpty())
            newAPs = null;
        Device d = new Device(this, device.getDeviceKey(),
                device.getDHCPClientName(), newAPs, null, entities,
                device.getEntityClass());
        d.updateAttachmentPoint();
        return d;
    }

    // *********************
    // ITopologyManagerAware
    // *********************

    @Override
    public void edgeUpdate(List<TopoEdgeUpdate> topoedgeupdateList) {
        Iterator<Device> diter = deviceMap.values().iterator();

        while (diter.hasNext()) {
            Device d = diter.next();
            if (d.updateAttachmentPoint()) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Attachment point changed for device: {}", d);
                }
                sendDeviceMovedNotification(d);
            }
        }
    }

    @Override
    public void edgeOverUtilized(Edge edge) {
        // nothing to do
    }

    @Override
    public void edgeUtilBackToNormal(Edge edge) {
        // nothing to do
    }

    // *********************
    // IEntityClassListener
    // *********************

    @Override
    public void entityClassChanged(Set<String> entityClassNames) {
        /*
         * iterate through the devices, reclassify the devices that belong to
         * these entity class names
         */
        Iterator<Device> diter = deviceMap.values().iterator();
        while (diter.hasNext()) {
            Device d = diter.next();
            if (d.getEntityClass() == null
                    || entityClassNames.contains(d.getEntityClass().getName()))
                reclassifyDevice(d);
        }
    }

    // *************
    // Local methods
    // *************
    /**
     * Send update notifications to listeners
     *
     * @param updates
     *            the updates to process.
     */
    protected void sendDeviceMovedNotification(Device d) {
        // debugCounters.updateCounter(CNT_DEVICE_MOVED);
        // deviceSyncManager.storeDevice(d);
        List<IDeviceListener> listeners = deviceListeners.getOrderedListeners();
        if (listeners != null) {
            for (IDeviceListener listener : listeners) {
                listener.deviceMoved(d);
            }
        }
    }

    /**
     * Send update notifications to listeners. IfNewHostNotify listeners need to
     * remove old device and add new device.
     *
     * @param device
     * @param oldDevice
     */
    protected void sendDeviceMovedNotification(Device device, Device oldDevice) {
        for (IfNewHostNotify notify : newHostNotify) {
            notify.notifyHTClientHostRemoved(oldDevice.toHostNodeConnector());
            notify.notifyHTClient(device.toHostNodeConnector());
        }
        sendDeviceMovedNotification(device);
    }

    /**
     * this method will reclassify and reconcile a device - possibilities are -
     * create new device(s), remove entities from this device. If the device
     * entity class did not change then it returns false else true.
     *
     * @param device
     */
    protected boolean reclassifyDevice(Device device) {
        // first classify all entities of this device
        if (device == null) {
            logger.debug("In reclassify for null device");
            return false;
        }
        boolean needToReclassify = false;
        for (Entity entity : device.entities) {
            IEntityClass entityClass = this.entityClassifier
                    .classifyEntity(entity);
            if (entityClass == null || device.getEntityClass() == null) {
                needToReclassify = true;
                break;
            }
            if (!entityClass.getName()
                    .equals(device.getEntityClass().getName())) {
                needToReclassify = true;
                break;
            }
        }
        if (needToReclassify == false) {
            return false;
        }

        // debugCounters.updateCounter(CNT_DEVICE_RECLASSIFY_DELETE);
        LinkedList<DeviceUpdate> deviceUpdates = new LinkedList<DeviceUpdate>();
        // delete this device and then re-learn all the entities
        this.deleteDevice(device);
        deviceUpdates.add(new DeviceUpdate(device, DeviceUpdate.Change.DELETE,
                null));
        if (!deviceUpdates.isEmpty())
            processUpdates(deviceUpdates);
        for (Entity entity : device.entities) {
            this.learnDeviceByEntity(entity);
        }
        return true;
    }


    private long toLong(byte[] address) {
        long mac = 0;
        for (int i = 0; i < 6; i++) {
            long t = (address[i] & 0xffL) << ((5 - i) * 8);
            mac |= t;
        }
        return mac;
    }

     /**
     * Accepts an IPv4 address in a byte array and returns the corresponding
     * 32-bit integer value.
     *
     * @param ipAddress
     * @return
     */
    private static int toIPv4Address(byte[] ipAddress) {
        int ip = 0;
        for (int i = 0; i < 4; i++) {
            int t = (ipAddress[i] & 0xff) << ((3 - i) * 8);
            ip |= t;
        }
        return ip;
    }

    private void registerDeviceManagerDebugCounters() {
        /*
         * XXX Missing functionality if (debugCounters == null) {
         * logger.error("Debug Counter Service not found."); debugCounters = new
         * NullDebugCounter(); return; }
         * debugCounters.registerCounter(CNT_INCOMING,
         * "All incoming packets seen by this module",
         * CounterType.ALWAYS_COUNT);
         * debugCounters.registerCounter(CNT_RECONCILE_REQUEST,
         * "Number of flows that have been received for reconciliation by " +
         * "this module", CounterType.ALWAYS_COUNT);
         * debugCounters.registerCounter(CNT_RECONCILE_NO_SOURCE,
         * "Number of flow reconcile events that failed because no source " +
         * "device could be identified", CounterType.WARN); // is this really a
         * warning debugCounters.registerCounter(CNT_RECONCILE_NO_DEST,
         * "Number of flow reconcile events that failed because no " +
         * "destination device could be identified", CounterType.WARN); // is
         * this really a warning
         * debugCounters.registerCounter(CNT_BROADCAST_SOURCE,
         * "Number of packetIns that were discarded because the source " +
         * "MAC was broadcast or multicast", CounterType.WARN);
         * debugCounters.registerCounter(CNT_NO_SOURCE,
         * "Number of packetIns that were discarded because the " +
         * "could not identify a source device. This can happen if a " +
         * "packet is not allowed, appears on an illegal port, does not " +
         * "have a valid address space, etc.", CounterType.WARN);
         * debugCounters.registerCounter(CNT_NO_DEST,
         * "Number of packetIns that did not have an associated " +
         * "destination device. E.g., because the destination MAC is " +
         * "broadcast/multicast or is not yet known to the controller.",
         * CounterType.ALWAYS_COUNT);
         * debugCounters.registerCounter(CNT_DHCP_CLIENT_NAME_SNOOPED,
         * "Number of times a DHCP client name was snooped from a " +
         * "packetIn.", CounterType.ALWAYS_COUNT);
         * debugCounters.registerCounter(CNT_DEVICE_ON_INTERAL_PORT_NOT_LEARNED,
         * "Number of times packetIn was received on an internal port and" +
         * "no source device is known for the source MAC. The packetIn is " +
         * "discarded.", CounterType.WARN);
         * debugCounters.registerCounter(CNT_PACKET_NOT_ALLOWED,
         * "Number of times a packetIn was not allowed due to spoofing " +
         * "protection configuration.", CounterType.WARN); // is this really a
         * warning? debugCounters.registerCounter(CNT_NEW_DEVICE,
         * "Number of times a new device was learned",
         * CounterType.ALWAYS_COUNT); debugCounters.registerCounter(
         * CNT_PACKET_ON_INTERNAL_PORT_FOR_KNOWN_DEVICE,
         * "Number of times a packetIn was received on an internal port " +
         * "for a known device.", CounterType.ALWAYS_COUNT);
         * debugCounters.registerCounter(CNT_NEW_ENTITY,
         * "Number of times a new entity was learned for an existing device",
         * CounterType.ALWAYS_COUNT);
         * debugCounters.registerCounter(CNT_DEVICE_CHANGED,
         * "Number of times device properties have changed",
         * CounterType.ALWAYS_COUNT);
         * debugCounters.registerCounter(CNT_DEVICE_MOVED,
         * "Number of times devices have moved", CounterType.ALWAYS_COUNT);
         * debugCounters.registerCounter(CNT_CLEANUP_ENTITIES_RUNS,
         * "Number of times the entity cleanup task has been run",
         * CounterType.ALWAYS_COUNT);
         * debugCounters.registerCounter(CNT_ENTITY_REMOVED_TIMEOUT,
         * "Number of times entities have been removed due to timeout " +
         * "(entity has been inactive for " + ENTITY_TIMEOUT/1000 + "s)",
         * CounterType.ALWAYS_COUNT);
         * debugCounters.registerCounter(CNT_DEVICE_DELETED,
         * "Number of devices that have been removed due to inactivity",
         * CounterType.ALWAYS_COUNT);
         * debugCounters.registerCounter(CNT_DEVICE_RECLASSIFY_DELETE,
         * "Number of devices that required reclassification and have been " +
         * "temporarily delete for reclassification", CounterType.ALWAYS_COUNT);
         * debugCounters.registerCounter(CNT_DEVICE_STORED,
         * "Number of device entries written or updated to the sync store",
         * CounterType.ALWAYS_COUNT);
         * debugCounters.registerCounter(CNT_DEVICE_STORE_THROTTLED,
         * "Number of times a device update to the sync store was " +
         * "requested but not performed because the same device entities " +
         * "have recently been updated already", CounterType.ALWAYS_COUNT);
         * debugCounters.registerCounter(CNT_DEVICE_REMOVED_FROM_STORE,
         * "Number of devices that were removed from the sync store " +
         * "because the local controller removed the device due to " +
         * "inactivity", CounterType.ALWAYS_COUNT);
         * debugCounters.registerCounter(CNT_SYNC_EXCEPTION,
         * "Number of times an operation on the sync store resulted in " +
         * "sync exception", CounterType.WARN); // it this an error?
         * debugCounters.registerCounter(CNT_DEVICES_FROM_STORE,
         * "Number of devices that were read from the sync store after " +
         * "the local controller transitioned from SLAVE to MASTER",
         * CounterType.ALWAYS_COUNT);
         * debugCounters.registerCounter(CNT_CONSOLIDATE_STORE_RUNS,
         * "Number of times the task to consolidate entries in the " +
         * "store witch live known devices has been run",
         * CounterType.ALWAYS_COUNT);
         * debugCounters.registerCounter(CNT_CONSOLIDATE_STORE_DEVICES_REMOVED,
         * "Number of times a device has been removed from the sync " +
         * "store because no corresponding live device is known. " +
         * "This indicates a remote controller still writing device " +
         * "entries despite the local controller being MASTER or an " +
         * "incosistent store update from the local controller.",
         * CounterType.WARN);
         * debugCounters.registerCounter(CNT_TRANSITION_TO_MASTER,
         * "Number of times this controller has transitioned from SLAVE " +
         * "to MASTER role. Will be 0 or 1.", CounterType.ALWAYS_COUNT);
         */
    }

    @Override
    public HostNodeConnector hostFind(InetAddress networkAddress) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public HostNodeConnector hostQuery(InetAddress networkAddress) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Future<HostNodeConnector> discoverHost(InetAddress networkAddress) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<List<String>> getHostNetworkHierarchy(InetAddress hostAddress) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Set<HostNodeConnector> getAllHosts() {
        Collection<Device> devices = Collections
                .unmodifiableCollection(deviceMap.values());
        Iterator<Device> i = devices.iterator();
        Set<HostNodeConnector> nc = new HashSet<HostNodeConnector>();
        while (i.hasNext()) {
            Device device = i.next();
            nc.add(device.toHostNodeConnector());
        }
        return nc;
    }

    @Override
    public Set<HostNodeConnector> getActiveStaticHosts() {
        Collection<Device> devices = Collections
                .unmodifiableCollection(deviceMap.values());
        Iterator<Device> i = devices.iterator();
        Set<HostNodeConnector> nc = new HashSet<HostNodeConnector>();
        while (i.hasNext()) {
            Device device = i.next();
            if (device.isStaticHost())
                nc.add(device.toHostNodeConnector());
        }
        return nc;
    }

    @Override
    public Set<HostNodeConnector> getInactiveStaticHosts() {
        Collection<Entity> devices = Collections
                .unmodifiableCollection(inactiveStaticDevices.values());
        Iterator<Entity> i = devices.iterator();
        Set<HostNodeConnector> nc = new HashSet<HostNodeConnector>();
        while (i.hasNext()) {
            Entity ent = i.next();
                nc.add(ent.toHostNodeConnector());

        }
        return nc;
    }

    @Override
    public Status addStaticHost(String networkAddress, String dataLayerAddress,
            NodeConnector nc, String vlan) {
        Long mac = HexEncode.stringToLong(dataLayerAddress);
        try {
            InetAddress addr = InetAddress.getByName(networkAddress);
            int ip = toIPv4Address(addr.getAddress());
            Entity e = new Entity(mac, Short.valueOf(vlan), ip, nc, new Date());

            if (switchManager.isNodeConnectorEnabled(e.getPort())) {
                Device d = this.learnDeviceByEntity(e);
                d.setStaticHost(true);
            } else {
                logger.debug(
                        "Switch or switchport is not up, adding host {} to inactive list",
                        addr.getHostName());
                 inactiveStaticDevices.put(e.getPort(), e);
            }
            return new Status(StatusCode.SUCCESS);
        } catch (UnknownHostException e) {
            return new Status(StatusCode.INTERNALERROR);
        }
    }

    @Override
    public Status removeStaticHost(String networkAddress) {
        Integer addr;
        try {
            addr = toIPv4Address(InetAddress.getByName(networkAddress)
                    .getAddress());
        } catch (UnknownHostException e) {
            return new Status(StatusCode.NOTFOUND, "Host does not exist");
        }
        Iterator<Device> di = this.getDeviceIteratorForQuery(null, null, addr,
                null);
        List<IDeviceListener> listeners = deviceListeners.getOrderedListeners();
        while (di.hasNext()) {
            Device d = di.next();
            if (d.isStaticHost()) {
                deleteDevice(d);
                for (IfNewHostNotify notify : newHostNotify) {
                    notify.notifyHTClientHostRemoved(d.toHostNodeConnector());
                }
                for (IDeviceListener listener : listeners) {
                    listener.deviceRemoved(d);
                }
            }
        }
        //go through inactive entites.
        Set<HostNodeConnector> inactive = this.getInactiveStaticHosts();
        for(HostNodeConnector nc : inactive){
            Integer ip =toIPv4Address(nc.getNetworkAddress().getAddress());
            if(ip.equals(addr)){
                this.inactiveStaticDevices.remove(nc.getnodeConnector());
            }
        }


        return new Status(StatusCode.SUCCESS);
    }

    @Override
    public void notifyNode(Node node, UpdateType type,
            Map<String, Property> propMap) {
        if (node == null)
            return;
        List<IDeviceListener> listeners = deviceListeners.getOrderedListeners();
        switch (type) {
        case REMOVED:
            logger.debug("Received removed node {}", node);
            for (Entry<Long, Device> d : deviceMap.entrySet()) {
                Device device = d.getValue();
                HostNodeConnector host = device.toHostNodeConnector();
                if (host.getnodeconnectorNode().equals(node)) {
                    logger.debug("Node: {} is down, remove from Hosts_DB", node);
                    deleteDevice(device);
                    for (IfNewHostNotify notify : newHostNotify) {
                        notify.notifyHTClientHostRemoved(host);
                    }
                    for (IDeviceListener listener : listeners) {
                        listener.deviceRemoved(device);
                    }
                }
            }
            break;
        default:
            break;
        }
    }

    @Override
    public void notifyNodeConnector(NodeConnector nodeConnector,
            UpdateType type, Map<String, Property> propMap) {
        if (nodeConnector == null)
            return;
        List<IDeviceListener> listeners = deviceListeners.getOrderedListeners();
        boolean up = false;
        switch (type) {
        case ADDED:
            up = true;
            break;
        case REMOVED:
            break;
        case CHANGED:
            State state = (State) propMap.get(State.StatePropName);
            if ((state != null) && (state.getValue() == State.EDGE_UP)) {
                up = true;
            }
            break;
        default:
            return;
        }

        if (up) {
            logger.debug("handleNodeConnectorStatusUp {}", nodeConnector);

            Entity ent = inactiveStaticDevices.get(nodeConnector);
            Device device = this.learnDeviceByEntity(ent);
            if(device!=null){
                HostNodeConnector host = device.toHostNodeConnector();
                if (host != null) {
                    inactiveStaticDevices.remove(nodeConnector);
                    for (IfNewHostNotify notify : newHostNotify) {
                        notify.notifyHTClient(host);
                    }
                    for (IDeviceListener listener : listeners) {
                        listener.deviceAdded(device);
                    }
                } else {
                    logger.debug("handleNodeConnectorStatusDown {}", nodeConnector);
                }
            }
        }else{
                // remove all devices on the node that went down.
                for (Entry<Long, Device> entry : deviceMap.entrySet()) {
                    Device device = entry.getValue();
                    HostNodeConnector host = device.toHostNodeConnector();
                    if (host.getnodeConnector().equals(nodeConnector)) {
                        deleteDevice(device);
                        for (IfNewHostNotify notify : newHostNotify) {
                            notify.notifyHTClientHostRemoved(host);
                        }
                        for (IDeviceListener listener : listeners) {
                            listener.deviceRemoved(device);
                        }
                    }
                }

            }
        }

}
