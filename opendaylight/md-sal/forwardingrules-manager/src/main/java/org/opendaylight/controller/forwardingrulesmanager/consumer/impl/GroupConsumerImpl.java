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
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.config.rev131024.Groups;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.config.rev131024.groups.Group;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.config.rev131024.groups.GroupKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.service.rev130918.AddGroupInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.service.rev130918.GroupAdded;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.service.rev130918.GroupRemoved;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.service.rev130918.GroupUpdated;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.service.rev130918.RemoveGroupInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.service.rev130918.SalGroupListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.service.rev130918.SalGroupService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.service.rev130918.UpdateGroupInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.service.rev130918.group.update.OriginalGroupBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.service.rev130918.group.update.UpdatedGroupBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.GroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.GroupTypes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.group.Buckets;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.group.buckets.Bucket;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.NotificationListener;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("unused")
public class GroupConsumerImpl {

    protected static final Logger logger = LoggerFactory.getLogger(GroupConsumerImpl.class);
    private final GroupEventListener groupEventListener = new GroupEventListener();
    private Registration<NotificationListener> groupListener;
    private SalGroupService groupService;
    private GroupDataCommitHandler groupCommitHandler;

    private IContainer container;

    public GroupConsumerImpl() {

        InstanceIdentifier<? extends DataObject> path = InstanceIdentifier.builder(Groups.class).toInstance();
        groupService = FRMConsumerImpl.getProviderSession().getRpcService(SalGroupService.class);      

        if (null == groupService) {
            logger.error("Consumer SAL Group Service is down or NULL. FRM may not function as intended");
            System.out.println("Consumer SAL Group Service is down or NULL.");
            return;
        }

        // For switch events
        groupListener = FRMConsumerImpl.getNotificationService().registerNotificationListener(groupEventListener);

        if (null == groupListener) {
            logger.error("Listener to listen on group data modifcation events");
            System.out.println("Listener to listen on group data modifcation events.");
            return;
        }

        groupCommitHandler = new GroupDataCommitHandler();
        FRMConsumerImpl.getDataProviderService().registerCommitHandler(path, groupCommitHandler);
    }  

    public Status validateGroup(Group group) {        
        String groupName;
        Iterator<Bucket> bucketIterator;
        boolean returnResult;
        Buckets groupBuckets;

        if (null != group) {   
            groupName = group.getGroupName();
            if (!FRMUtil.isNameValid(groupName)) {
                logger.error("Group Name is invalid %s" + groupName);
                return new Status(StatusCode.BADREQUEST, "Group Name is invalid");
            }
            
            if (!(group.getGroupType().getIntValue() >= GroupTypes.GroupAll.getIntValue() && group.getGroupType()
                    .getIntValue() <= GroupTypes.GroupFf.getIntValue())) {
                logger.error("Invalid Group type %d" + group.getGroupType().getIntValue());
                return new Status(StatusCode.BADREQUEST, "Invalid Group type");
            }

            groupBuckets = group.getBuckets();

            if (null != groupBuckets && null != groupBuckets.getBucket()) {
                bucketIterator = groupBuckets.getBucket().iterator();

                while (bucketIterator.hasNext()) {
                    if (!(FRMUtil.validateActions(bucketIterator.next().getAction()))) {
                        logger.error("Error in action bucket");
                        return new Status(StatusCode.BADREQUEST, "Invalid Group bucket contents");
                    }
                }
            }
        }

        return new Status(StatusCode.SUCCESS);
    }

    /**
     * Update Group entries to the southbound plugin/inventory and our internal
     * database
     *
     * @param path
     * @param dataObject
     */
    private void updateGroup(InstanceIdentifier<?> path, 
        Group originalGroupDataObject, Group updatedGroupDataObject) {
        
        GroupKey groupKey = updatedGroupDataObject.getKey();
       // Node nodeInstanceID = path.firstIdentifierOf("Node");
        UpdatedGroupBuilder updateGroupBuilder = null;
        Status groupOperationStatus = validateGroup(updatedGroupDataObject);

        if (!groupOperationStatus.isSuccess()) {
            logger.error("Group data object validation failed %s" + updatedGroupDataObject.getGroupName());
            return;
        }
        
        UpdateGroupInputBuilder groupInputBuilder = new UpdateGroupInputBuilder();
        groupInputBuilder.setNode(updatedGroupDataObject.getNode());
        updateGroupBuilder = new UpdatedGroupBuilder(updatedGroupDataObject);
        updateGroupBuilder.setGroupId(new GroupId(updatedGroupDataObject.getId()));        
        groupInputBuilder.setUpdatedGroup(updateGroupBuilder.build());       
        OriginalGroupBuilder originalGroupBuilder = new OriginalGroupBuilder(originalGroupDataObject);
        groupInputBuilder.setOriginalGroup(originalGroupBuilder.build());
        groupService.updateGroup(groupInputBuilder.build());
        return;
    }

    /**
     * Adds Group to the southbound plugin and our internal database
     *
     * @param path
     * @param dataObject
     */
    private void addGroup(InstanceIdentifier<?> path, Group groupAddDataObject) {
        GroupKey groupKey = groupAddDataObject.getKey();
        Status groupOperationStatus = validateGroup(groupAddDataObject);

        if (!groupOperationStatus.isSuccess()) {
            logger.error("Group data object validation failed %s" + groupAddDataObject.getGroupName());
            return;
        }
        
        AddGroupInputBuilder groupData = new AddGroupInputBuilder();
        groupData.setBuckets(groupAddDataObject.getBuckets());
        groupData.setContainerName(groupAddDataObject.getContainerName());
        groupData.setGroupId(new GroupId(groupAddDataObject.getId()));
        groupData.setGroupType(groupAddDataObject.getGroupType());
        groupData.setNode(groupAddDataObject.getNode());    
        groupService.addGroup(groupData.build());
        return;
    }

    /**
     * Remove Group to the southbound plugin and our internal database
     *
     * @param path
     * @param dataObject
     */
    private void removeGroup(InstanceIdentifier<?> path, Group groupRemoveDataObject) {
        GroupKey groupKey = groupRemoveDataObject.getKey();
        Status groupOperationStatus = validateGroup(groupRemoveDataObject);

        if (!groupOperationStatus.isSuccess()) {
            logger.error("Group data object validation failed %s" + groupRemoveDataObject.getGroupName());
            return;
        }
       
        RemoveGroupInputBuilder groupData = new RemoveGroupInputBuilder();
        groupData.setBuckets(groupRemoveDataObject.getBuckets());
        groupData.setContainerName(groupRemoveDataObject.getContainerName());
        groupData.setGroupId(new GroupId(groupRemoveDataObject.getId()));
        groupData.setGroupType(groupRemoveDataObject.getGroupType());
        groupData.setNode(groupRemoveDataObject.getNode());    
        groupService.removeGroup(groupData.build());  
        return;
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
            if(entry.getValue() instanceof Group) {   
                addGroup(entry.getKey(), (Group)entry.getValue());   
            }   
        } 
        
        for (Entry<InstanceIdentifier<?>, DataObject> entry : updatedEntries) { 
            if(entry.getValue() instanceof Group) {   
                Group originalGroup = (Group) modification.getOriginalConfigurationData().get(entry.getKey());    
                Group updatedGroup = (Group) entry.getValue(); 
                updateGroup(entry.getKey(), originalGroup, updatedGroup);   
            }   
        }   

        for (InstanceIdentifier<?> instanceId : removeEntriesInstanceIdentifiers ) {    
            DataObject removeValue = modification.getOriginalConfigurationData().get(instanceId);   
            if(removeValue instanceof Group) {   
                removeGroup(instanceId, (Group)removeValue); 
            }   
        }

        return Rpcs.getRpcResult(true, null, Collections.<RpcError>emptySet());
    }

    private final class GroupDataCommitHandler implements DataCommitHandler<InstanceIdentifier<?>, DataObject> {

        @Override
        public DataCommitTransaction<InstanceIdentifier<?>, DataObject> requestCommit(
                DataModification<InstanceIdentifier<?>, DataObject> modification) {            
            InternalTransaction transaction = new InternalTransaction(modification);
            transaction.prepareUpdate();
            return transaction;
        }
    }

    private final class InternalTransaction implements DataCommitTransaction<InstanceIdentifier<?>, DataObject> {      

        private final DataModification<InstanceIdentifier<?>, DataObject> modification;
        
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
            
            ///needs to be implemented as per gerrit 3314
            return Rpcs.getRpcResult(true, null, Collections.<RpcError>emptySet());
        }

        @Override
        public DataModification<InstanceIdentifier<?>, DataObject> getModification() {            
            return modification;
        }

    }

    final class GroupEventListener implements SalGroupListener {

        List<GroupAdded> addedGroups = new ArrayList<>();
        List<GroupRemoved> removedGroups = new ArrayList<>();
        List<GroupUpdated> updatedGroups = new ArrayList<>();

        @Override
        public void onGroupAdded(GroupAdded notification) {
            addedGroups.add(notification);
        }

        @Override
        public void onGroupRemoved(GroupRemoved notification) {
            // TODO Auto-generated method stub

        }

        @Override
        public void onGroupUpdated(GroupUpdated notification) {
            // TODO Auto-generated method stub

        }
    }
 }
