package org.opendaylight.controller.forwardingrulesmanager.consumer.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.UpdateFlowInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.flow.update.OriginalFlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.flow.update.UpdatedFlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.service.rev130918.UpdateGroupInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.service.rev130918.group.update.OriginalGroupBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.service.rev130918.group.update.UpdatedGroupBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.GroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.config.rev131024.Meters;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.config.rev131024.meters.Meter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.config.rev131024.meters.MeterKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.service.rev130918.AddMeterInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.service.rev130918.MeterAdded;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.service.rev130918.MeterRemoved;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.service.rev130918.MeterUpdated;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.service.rev130918.RemoveMeterInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.service.rev130918.SalMeterListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.service.rev130918.SalMeterService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.service.rev130918.UpdateMeterInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.service.rev130918.meter.update.OriginalMeterBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.service.rev130918.meter.update.UpdatedMeterBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.types.rev130918.MeterId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.types.rev130918.band.type.BandType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.types.rev130918.band.type.band.type.Drop;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.types.rev130918.band.type.band.type.DscpRemark;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.types.rev130918.band.type.band.type.Experimenter;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.NotificationListener;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MeterConsumerImpl {
    protected static final Logger logger = LoggerFactory.getLogger(MeterConsumerImpl.class);
    private final MeterEventListener meterEventListener = new MeterEventListener();
    private Registration<NotificationListener> meterListener;
    private SalMeterService meterService;
    private MeterDataCommitHandler commitHandler;

    public MeterConsumerImpl() {
        InstanceIdentifier<? extends DataObject> path = InstanceIdentifier.builder(Meters.class).toInstance();
        meterService = FRMConsumerImpl.getProviderSession().getRpcService(SalMeterService.class);
        
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
    
    /**
     * Adds Meter to the southbound plugin and our internal database
     *
     * @param path
     * @param dataObject
     */
    private Status addMeter(InstanceIdentifier<?> path, Meter meterAddDataObject) {
        MeterKey meterKey = meterAddDataObject.getKey();
        
        if (null != meterKey && validateMeter(meterAddDataObject).isSuccess()) {        	 
            AddMeterInputBuilder meterBuilder = new AddMeterInputBuilder();
            meterBuilder.fieldsFrom(meterAddDataObject);            
            meterBuilder.setMeterId(new MeterId(meterAddDataObject.getId()));
            meterBuilder.setNode(meterAddDataObject.getNode());           
            meterService.addMeter(meterBuilder.build());
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
    private Status updateMeter(InstanceIdentifier<?> path, 
                Meter updatedMeter, Meter originalMeter) {        
        UpdatedMeterBuilder updateMeterBuilder = null;
        
        if (validateMeter(updatedMeter).isSuccess()) {                
            UpdateMeterInputBuilder updateMeterInputBuilder = new UpdateMeterInputBuilder();
            updateMeterInputBuilder.setNode(updatedMeter.getNode());
            updateMeterBuilder = new UpdatedMeterBuilder();
            updateMeterBuilder.fieldsFrom(updatedMeter);            
            updateMeterBuilder.setMeterId(new MeterId(updatedMeter.getId()));            
            updateMeterInputBuilder.setUpdatedMeter(updateMeterBuilder.build());
            OriginalMeterBuilder originalMeterBuilder = new OriginalMeterBuilder(originalMeter);
            updateMeterInputBuilder.setOriginalMeter(originalMeterBuilder.build());
            meterService.updateMeter(updateMeterInputBuilder.build());
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
    private Status removeMeter(InstanceIdentifier<?> path, Meter meterRemoveDataObject) {
        MeterKey meterKey = meterRemoveDataObject.getKey();

        if (null != meterKey && validateMeter(meterRemoveDataObject).isSuccess()) {            
            RemoveMeterInputBuilder meterBuilder = new RemoveMeterInputBuilder();
            meterBuilder.fieldsFrom(meterRemoveDataObject);
            meterBuilder.setNode(meterRemoveDataObject.getNode());            
            meterBuilder.setMeterId(new MeterId(meterRemoveDataObject.getId()));           
            meterService.removeMeter(meterBuilder.build());
        } else {
            return new Status(StatusCode.BADREQUEST, "Meter Key or attribute validation failed");
        }

        return new Status(StatusCode.SUCCESS);
    }

    public Status validateMeter(Meter meter) {        
        String meterName;
        Status returnStatus = null;

        if (null != meter) {
            meterName = meter.getMeterName();
            if (!FRMUtil.isNameValid(meterName)) {
                logger.error("Meter Name is invalid %s" + meterName);
                returnStatus = new Status(StatusCode.BADREQUEST, "Meter Name is invalid");
                return returnStatus;
            }

            for (int i = 0; i < meter.getMeterBandHeaders().getMeterBandHeader().size(); i++) {
                if (null != meter.getFlags() && !meter.getFlags().isMeterBurst()) {
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
            } else if (null != meter.getMeterBandHeaders()) {
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

    private RpcResult<Void> commitToPlugin(InternalTransaction transaction) {
        DataModification<InstanceIdentifier<?>, DataObject> modification = transaction.modification;         
        //get created entries      
        Set<Entry<InstanceIdentifier<? extends DataObject>, DataObject>> createdEntries = 
                                        modification.getCreatedConfigurationData().entrySet();
        
        //get updated entries
        Set<Entry<InstanceIdentifier<? extends DataObject>, DataObject>> updatedEntries = 
                    new HashSet<Entry<InstanceIdentifier<? extends DataObject>, DataObject>>(); 
        
        updatedEntries.addAll(modification.getUpdatedConfigurationData().entrySet());
        updatedEntries.removeAll(createdEntries);

        //get removed entries
        Set<InstanceIdentifier<? extends DataObject>> removeEntriesInstanceIdentifiers = 
                                                    modification.getRemovedConfigurationData();
        
        for (Entry<InstanceIdentifier<? extends DataObject >, DataObject> entry : createdEntries) { 
            if(entry.getValue() instanceof Meter) {   
                addMeter(entry.getKey(), (Meter)entry.getValue());   
            }   
        } 
        
        for (Entry<InstanceIdentifier<?>, DataObject> entry : updatedEntries) { 
            if(entry.getValue() instanceof Meter) {   
                Meter originalMeter = (Meter) modification.getOriginalConfigurationData().get(entry.getKey());    
                Meter updatedMeter = (Meter) entry.getValue(); 
                updateMeter(entry.getKey(), originalMeter, updatedMeter);   
            }   
        }   

        for (InstanceIdentifier<?> instanceId : removeEntriesInstanceIdentifiers ) {    
            DataObject removeValue = modification.getOriginalConfigurationData().get(instanceId);   
            if(removeValue instanceof Meter) {   
                removeMeter(instanceId, (Meter)removeValue); 
            }   
        }

        return Rpcs.getRpcResult(true, null, Collections.<RpcError>emptySet());
    }
    
    final class InternalTransaction implements DataCommitTransaction<InstanceIdentifier<?>, DataObject> {

        private final DataModification<InstanceIdentifier<?>, DataObject> modification;

        @Override
        public DataModification<InstanceIdentifier<?>, DataObject> getModification() {
            return modification;
        }

        public InternalTransaction(DataModification<InstanceIdentifier<?>, DataObject> modification) {
            this.modification = modification;
        }

        /**
         * We create a plan which flows will be added, which will be updated and
         * which will be removed based on our internal state.
         *
         */
        void prepareUpdate() {           
            
        }

        /**
         * We are OK to go with execution of plan
         *
         */
        @Override
        public RpcResult<Void> finish() throws IllegalStateException {

            RpcResult<Void> rpcStatus = commitToPlugin(this);           
            return rpcStatus;
        }

        /**
         *
         * We should rollback our preparation
         *
         */
        @Override
        public RpcResult<Void> rollback() throws IllegalStateException {            
            return Rpcs.getRpcResult(true, null, Collections.<RpcError>emptySet());

        }

    }
    
    private final class MeterDataCommitHandler implements DataCommitHandler<InstanceIdentifier<?>, DataObject> {
        @Override
        public org.opendaylight.controller.md.sal.common.api.data.DataCommitHandler.DataCommitTransaction<InstanceIdentifier<?>, DataObject> requestCommit(
                DataModification<InstanceIdentifier<?>, DataObject> modification) {
            // We should verify transaction
            InternalTransaction transaction = new InternalTransaction(modification);
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
}
