package org.opendaylight.controller.forwardingrulesmanager.consumer.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.config.rev131024.Meters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.config.rev131024.meters.Meter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.config.rev131024.meters.MeterKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.service.rev130918.AddMeterInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.service.rev130918.MeterAdded;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.service.rev130918.MeterRemoved;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.service.rev130918.MeterUpdated;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.service.rev130918.SalMeterListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.service.rev130918.SalMeterService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.service.rev130918.UpdateMeterInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.service.rev130918.meter.update.UpdatedMeterBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.types.rev130918.band.type.BandType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.types.rev130918.band.type.band.type.Drop;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.types.rev130918.band.type.band.type.DscpRemark;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.types.rev130918.band.type.band.type.Experimenter;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.NotificationListener;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MeterConsumerImpl implements IForwardingRulesManager {
    protected static final Logger logger = LoggerFactory.getLogger(MeterConsumerImpl.class);
    private final MeterEventListener meterEventListener = new MeterEventListener();
    private Registration<NotificationListener> meterListener;
    private SalMeterService meterService;
    private MeterDataCommitHandler commitHandler;

    private ConcurrentMap<MeterKey, Meter> originalSwMeterView;
    private ConcurrentMap<MeterKey, Meter> installedSwMeterView;

    private ConcurrentMap<Node, List<Meter>> nodeMeters;
    private ConcurrentMap<MeterKey, Meter> inactiveMeters;

    private IClusterContainerServices clusterMeterContainerService = null;
    private IContainer container;

    public MeterConsumerImpl() {
        InstanceIdentifier<? extends DataObject> path = InstanceIdentifier.builder(Meters.class).child(Meter.class)
                .toInstance();
        meterService = FRMConsumerImpl.getProviderSession().getRpcService(SalMeterService.class);
        clusterMeterContainerService = FRMConsumerImpl.getClusterContainerService();

        container = FRMConsumerImpl.getContainer();

        if (!(cacheStartup())) {
            logger.error("Unable to allocate/retrieve meter cache");
            System.out.println("Unable to allocate/retrieve meter cache");
        }

        if (null == meterService) {
            logger.error("Consumer SAL Meter Service is down or NULL. FRM may not function as intended");
            System.out.println("Consumer SAL Meter Service is down or NULL.");
            return;
        }

        // For switch/plugin events
        meterListener = FRMConsumerImpl.getNotificationService().registerNotificationListener(meterEventListener);

        if (null == meterListener) {
            logger.error("Listener to listen on meter data modifcation events");
            System.out.println("Listener to listen on meter data modifcation events.");
            return;
        }

        commitHandler = new MeterDataCommitHandler();
        FRMConsumerImpl.getDataProviderService().registerCommitHandler(path, commitHandler);
    }

    private boolean allocateMeterCaches() {
        if (this.clusterMeterContainerService == null) {
            logger.warn("Meter: Un-initialized clusterMeterContainerService, can't create cache");
            return false;
        }

        try {
            clusterMeterContainerService.createCache("frm.originalSwMeterView",
                    EnumSet.of(IClusterServices.cacheMode.TRANSACTIONAL));

            clusterMeterContainerService.createCache("frm.installedSwMeterView",
                    EnumSet.of(IClusterServices.cacheMode.TRANSACTIONAL));

            clusterMeterContainerService.createCache("frm.inactiveMeters",
                    EnumSet.of(IClusterServices.cacheMode.TRANSACTIONAL));

            clusterMeterContainerService.createCache("frm.nodeMeters",
                    EnumSet.of(IClusterServices.cacheMode.TRANSACTIONAL));

            // TODO for cluster mode
            /*
             * clusterMeterContainerService.createCache(WORK_STATUS_CACHE,
             * EnumSet.of(IClusterServices.cacheMode.NON_TRANSACTIONAL,
             * IClusterServices.cacheMode.ASYNC));
             *
             * clusterMeterContainerService.createCache(WORK_ORDER_CACHE,
             * EnumSet.of(IClusterServices.cacheMode.NON_TRANSACTIONAL,
             * IClusterServices.cacheMode.ASYNC));
             */

        } catch (CacheConfigException cce) {
            logger.error("Meter CacheConfigException");
            return false;

        } catch (CacheExistException cce) {
            logger.error(" Meter CacheExistException");
        }

        return true;
    }

    private void nonClusterMeterObjectCreate() {
        originalSwMeterView = new ConcurrentHashMap<MeterKey, Meter>();
        installedSwMeterView = new ConcurrentHashMap<MeterKey, Meter>();
        nodeMeters = new ConcurrentHashMap<Node, List<Meter>>();
        inactiveMeters = new ConcurrentHashMap<MeterKey, Meter>();
    }

    @SuppressWarnings({ "unchecked" })
    private boolean retrieveMeterCaches() {
        ConcurrentMap<?, ?> map;

        if (this.clusterMeterContainerService == null) {
            logger.warn("Meter: un-initialized clusterMeterContainerService, can't retrieve cache");
            nonClusterMeterObjectCreate();
            return false;
        }

        map = clusterMeterContainerService.getCache("frm.originalSwMeterView");
        if (map != null) {
            originalSwMeterView = (ConcurrentMap<MeterKey, Meter>) map;
        } else {
            logger.error("Retrieval of cache(originalSwMeterView) failed");
            return false;
        }

        map = clusterMeterContainerService.getCache("frm.installedSwMeterView");
        if (map != null) {
            installedSwMeterView = (ConcurrentMap<MeterKey, Meter>) map;
        } else {
            logger.error("Retrieval of cache(installedSwMeterView) failed");
            return false;
        }

        map = clusterMeterContainerService.getCache("frm.inactiveMeters");
        if (map != null) {
            inactiveMeters = (ConcurrentMap<MeterKey, Meter>) map;
        } else {
            logger.error("Retrieval of cache(inactiveMeters) failed");
            return false;
        }

        map = clusterMeterContainerService.getCache("frm.nodeMeters");
        if (map != null) {
            nodeMeters = (ConcurrentMap<Node, List<Meter>>) map;
        } else {
            logger.error("Retrieval of cache(nodeMeter) failed");
            return false;
        }

        return true;
    }

    private boolean cacheStartup() {
        if (allocateMeterCaches()) {
            if (retrieveMeterCaches()) {
                return true;
            }
        }

        return false;
    }

    /**
     * Adds Meter to the southbound plugin and our internal database
     *
     * @param path
     * @param dataObject
     */
    private Status addMeter(InstanceIdentifier<?> path, Meter meterAddDataObject) {
        MeterKey meterKey = meterAddDataObject.getKey();

        if (null != meterKey && validateMeter(meterAddDataObject, FRMUtil.operation.ADD).isSuccess()) {
            if (meterAddDataObject.isInstall()) {
                AddMeterInputBuilder meterBuilder = new AddMeterInputBuilder();

                meterBuilder.setContainerName(meterAddDataObject.getContainerName());
                meterBuilder.setFlags(meterAddDataObject.getFlags());
                meterBuilder.setMeterBandHeaders(meterAddDataObject.getMeterBandHeaders());
                meterBuilder.setMeterId(meterAddDataObject.getMeterId());
                meterBuilder.setNode(meterAddDataObject.getNode());
                originalSwMeterView.put(meterKey, meterAddDataObject);
                meterService.addMeter(meterBuilder.build());
            }

            originalSwMeterView.put(meterKey, meterAddDataObject);
        } else {
            return new Status(StatusCode.BADREQUEST, "Meter Key or attribute validation failed");
        }

        return new Status(StatusCode.SUCCESS);
    }

    /*
     * Update Meter to the southbound plugin and our internal database
     *
     * @param path
     *
     * @param dataObject
     */
    private Status updateMeter(InstanceIdentifier<?> path, Meter meterUpdateDataObject) {
        MeterKey meterKey = meterUpdateDataObject.getKey();
        UpdatedMeterBuilder updateMeterBuilder = null;

        if (null != meterKey && validateMeter(meterUpdateDataObject, FRMUtil.operation.UPDATE).isSuccess()) {

            if (originalSwMeterView.containsKey(meterKey)) {
                originalSwMeterView.remove(meterKey);
                originalSwMeterView.put(meterKey, meterUpdateDataObject);
            }

            if (meterUpdateDataObject.isInstall()) {
                UpdateMeterInputBuilder updateMeterInputBuilder = new UpdateMeterInputBuilder();
                updateMeterBuilder = new UpdatedMeterBuilder();
                updateMeterBuilder.fieldsFrom(meterUpdateDataObject);
                updateMeterInputBuilder.setUpdatedMeter(updateMeterBuilder.build());

                if (installedSwMeterView.containsKey(meterKey)) {
                    installedSwMeterView.remove(meterKey);
                    installedSwMeterView.put(meterKey, meterUpdateDataObject);
                }

                meterService.updateMeter(updateMeterInputBuilder.build());
            }

        } else {
            return new Status(StatusCode.BADREQUEST, "Meter Key or attribute validation failed");
        }

        return new Status(StatusCode.SUCCESS);
    }

    /*
     * Remove Meter to the southbound plugin and our internal database
     *
     * @param path
     *
     * @param dataObject
     */
    private Status RemoveMeter(InstanceIdentifier<?> path, Meter meterUpdateDataObject) {
        MeterKey meterKey = meterUpdateDataObject.getKey();

        if (null != meterKey && validateMeter(meterUpdateDataObject, FRMUtil.operation.ADD).isSuccess()) {
            if (meterUpdateDataObject.isInstall()) {
                UpdateMeterInputBuilder updateMeterBuilder = new UpdateMeterInputBuilder();

                installedSwMeterView.put(meterKey, meterUpdateDataObject);
                meterService.updateMeter(updateMeterBuilder.build());
            }

            originalSwMeterView.put(meterKey, meterUpdateDataObject);
        } else {
            return new Status(StatusCode.BADREQUEST, "Meter Key or attribute validation failed");
        }

        return new Status(StatusCode.SUCCESS);
    }

    public Status validateMeter(Meter meter, FRMUtil.operation operation) {
        String containerName;
        String meterName;
        Status returnStatus = null;
        boolean returnResult;

        if (null != meter) {
            containerName = meter.getContainerName();

            if (null == containerName) {
                containerName = GlobalConstants.DEFAULT.toString();
            } else if (!FRMUtil.isNameValid(containerName)) {
                logger.error("Container Name is invalid %s" + containerName);
                returnStatus = new Status(StatusCode.BADREQUEST, "Container Name is invalid");
                return returnStatus;
            }

            meterName = meter.getMeterName();
            if (!FRMUtil.isNameValid(meterName)) {
                logger.error("Meter Name is invalid %s" + meterName);
                returnStatus = new Status(StatusCode.BADREQUEST, "Meter Name is invalid");
                return returnStatus;
            }

            returnResult = doesMeterEntryExists(meter.getKey(), meterName, containerName);

            if (FRMUtil.operation.ADD == operation && returnResult) {
                logger.error("Record with same Meter Name exists");
                returnStatus = new Status(StatusCode.BADREQUEST, "Meter record exists");
                return returnStatus;
            } else if (!returnResult) {
                logger.error("Group record does not exist");
                returnStatus = new Status(StatusCode.BADREQUEST, "Meter record does not exist");
                return returnStatus;
            }

            for (int i = 0; i < meter.getMeterBandHeaders().getMeterBandHeader().size(); i++) {
                if (!meter.getFlags().isMeterBurst()) {
                    if (0 < meter.getMeterBandHeaders().getMeterBandHeader().get(i).getBurstSize()) {
                        logger.error("Burst size should only be associated when Burst FLAG is set");
                        returnStatus = new Status(StatusCode.BADREQUEST,
                                "Burst size should only be associated when Burst FLAG is set");
                        break;
                    }
                }
            }

            if (null != returnStatus && !returnStatus.isSuccess()) {
                return returnStatus;
            } else {
                BandType setBandType = null;
                DscpRemark dscpRemark = null;
                for (int i = 0; i < meter.getMeterBandHeaders().getMeterBandHeader().size(); i++) {
                    setBandType = meter.getMeterBandHeaders().getMeterBandHeader().get(i).getBandType();
                    if (setBandType instanceof DscpRemark) {
                        dscpRemark = (DscpRemark) setBandType;
                        if (0 > dscpRemark.getRate()) {

                        }
                    } else if (setBandType instanceof Drop) {
                        if (0 < dscpRemark.getPercLevel()) {
                            logger.error("Number of drop Precedence level");
                        }
                    } else if (setBandType instanceof Experimenter) {

                    }
                }
            }
        }
        return new Status(StatusCode.SUCCESS);
    }

    private boolean doesMeterEntryExists(MeterKey key, String meterName, String containerName) {
        if (!originalSwMeterView.containsKey(key)) {
            return false;
        }

        for (Entry<MeterKey, Meter> entry : originalSwMeterView.entrySet()) {
            if (entry.getValue().getMeterName().equals(meterName)) {
                if (entry.getValue().getContainerName().equals(containerName)) {
                    return true;
                }
            }
        }
        return false;
    }

    private RpcResult<Void> commitToPlugin(internalTransaction transaction) {
        for (Entry<InstanceIdentifier<?>, Meter> entry : transaction.additions.entrySet()) {

            if (!addMeter(entry.getKey(), entry.getValue()).isSuccess()) {
                return Rpcs.getRpcResult(false, null, null);
            }
        }
        for (@SuppressWarnings("unused")
        Entry<InstanceIdentifier<?>, Meter> entry : transaction.updates.entrySet()) {

            if (!updateMeter(entry.getKey(), entry.getValue()).isSuccess()) {
                return Rpcs.getRpcResult(false, null, null);
            }
        }

        for (InstanceIdentifier<?> removal : transaction.removals) {
            /*
             * if (!removeMeter(entry.getKey(),entry.getValue()).isSuccess()) {
             * return Rpcs.getRpcResult(false, null, null); }
             */
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

        Map<InstanceIdentifier<?>, Meter> additions = new HashMap<>();
        Map<InstanceIdentifier<?>, Meter> updates = new HashMap<>();
        Set<InstanceIdentifier<?>> removals = new HashSet<>();

        /**
         * We create a plan which flows will be added, which will be updated and
         * which will be removed based on our internal state.
         *
         */
        void prepareUpdate() {

            Set<Entry<InstanceIdentifier<?>, DataObject>> puts = modification.getUpdatedConfigurationData().entrySet();
            for (Entry<InstanceIdentifier<?>, DataObject> entry : puts) {
                if (entry.getValue() instanceof Meter) {
                    Meter Meter = (Meter) entry.getValue();
                    preparePutEntry(entry.getKey(), Meter);
                }

            }

            removals = modification.getRemovedConfigurationData();
        }

        private void preparePutEntry(InstanceIdentifier<?> key, Meter meter) {

            Meter original = originalSwMeterView.get(key);
            if (original != null) {
                // It is update for us

                updates.put(key, meter);
            } else {
                // It is addition for us

                additions.put(key, meter);
            }
        }

        /**
         * We are OK to go with execution of plan
         *
         */
        @Override
        public RpcResult<Void> finish() throws IllegalStateException {

            RpcResult<Void> rpcStatus = commitToPlugin(this);
            // We return true if internal transaction is successful.
            // return Rpcs.getRpcResult(true, null, Collections.emptySet());
            return rpcStatus;
        }

        /**
         *
         * We should rollback our preparation
         *
         */
        @Override
        public RpcResult<Void> rollback() throws IllegalStateException {
            // NOOP - we did not modified any internal state during
            // requestCommit phase
            // return Rpcs.getRpcResult(true, null, Collections.emptySet());
            return Rpcs.getRpcResult(true, null, null);

        }

    }

    private final class MeterDataCommitHandler implements DataCommitHandler<InstanceIdentifier<?>, DataObject> {
        @Override
        public org.opendaylight.controller.md.sal.common.api.data.DataCommitHandler.DataCommitTransaction<InstanceIdentifier<?>, DataObject> requestCommit(
                DataModification<InstanceIdentifier<?>, DataObject> modification) {
            // We should verify transaction
            System.out.println("Coming in MeterDataCommitHandler");
            internalTransaction transaction = new internalTransaction(modification);
            transaction.prepareUpdate();
            return transaction;
        }
    }

    final class MeterEventListener implements SalMeterListener {

        List<MeterAdded> addedMeter = new ArrayList<>();
        List<MeterRemoved> removeMeter = new ArrayList<>();
        List<MeterUpdated> updatedMeter = new ArrayList<>();

        @Override
        public void onMeterAdded(MeterAdded notification) {
            // TODO Auto-generated method stub

        }

        @Override
        public void onMeterRemoved(MeterRemoved notification) {
            // TODO Auto-generated method stub

        }

        @Override
        public void onMeterUpdated(MeterUpdated notification) {
            // TODO Auto-generated method stub

        }
    }

    @Override
    public List<DataObject> get() {

        List<DataObject> orderedList = new ArrayList<DataObject>();
        Collection<Meter> meterList = originalSwMeterView.values();
        for (Iterator<Meter> iterator = meterList.iterator(); iterator.hasNext();) {
            orderedList.add(iterator.next());
        }
        return orderedList;
    }

    @Override
    public DataObject getWithName(String name, Node n) {
        if (this instanceof MeterConsumerImpl) {
            Collection<Meter> meterList = originalSwMeterView.values();
            for (Iterator<Meter> iterator = meterList.iterator(); iterator.hasNext();) {
                Meter meter = iterator.next();
                if (meter.getNode().equals(n) && meter.getMeterName().equals(name)) {

                    return meter;
                }
            }
        }
        return null;
    }
}
