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
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.config.rev131024.Groups;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.config.rev131024.groups.Group;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.config.rev131024.groups.GroupKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.service.rev130918.AddGroupInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.service.rev130918.GroupAdded;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.service.rev130918.GroupRemoved;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.service.rev130918.GroupUpdated;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.service.rev130918.SalGroupListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.service.rev130918.SalGroupService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.service.rev130918.UpdateGroupInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.service.rev130918.group.update.UpdatedGroupBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.GroupTypes.GroupType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.group.Buckets;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.group.buckets.Bucket;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.NotificationListener;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("unused")
public class GroupConsumerImpl implements IForwardingRulesManager {

    protected static final Logger logger = LoggerFactory.getLogger(GroupConsumerImpl.class);
    private final GroupEventListener groupEventListener = new GroupEventListener();
    private Registration<NotificationListener> groupListener;
    private SalGroupService groupService;
    private GroupDataCommitHandler commitHandler;

    private ConcurrentMap<GroupKey, Group> originalSwGroupView;
    private ConcurrentMap<GroupKey, Group> installedSwGroupView;

    private ConcurrentMap<Node, List<Group>> nodeGroups;
    private ConcurrentMap<GroupKey, Group> inactiveGroups;

    private IClusterContainerServices clusterGroupContainerService = null;
    private IContainer container;

    public GroupConsumerImpl() {

        InstanceIdentifier<? extends DataObject> path = InstanceIdentifier.builder(Groups.class).child(Group.class)
                .toInstance();
        groupService = FRMConsumerImpl.getProviderSession().getRpcService(SalGroupService.class);

        clusterGroupContainerService = FRMConsumerImpl.getClusterContainerService();
        container = FRMConsumerImpl.getContainer();

        if (!(cacheStartup())) {
            logger.error("Unanle to allocate/retrieve group cache");
            System.out.println("Unable to allocate/retrieve group cache");
        }

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

        commitHandler = new GroupDataCommitHandler();
        FRMConsumerImpl.getDataProviderService().registerCommitHandler(path, commitHandler);
    }

    private boolean allocateGroupCaches() {
        if (this.clusterGroupContainerService == null) {
            logger.warn("Group: Un-initialized clusterGroupContainerService, can't create cache");
            return false;
        }

        try {
            clusterGroupContainerService.createCache("frm.originalSwGroupView",
                    EnumSet.of(IClusterServices.cacheMode.TRANSACTIONAL));

            clusterGroupContainerService.createCache("frm.installedSwGroupView",
                    EnumSet.of(IClusterServices.cacheMode.TRANSACTIONAL));

            clusterGroupContainerService.createCache("frm.inactiveGroups",
                    EnumSet.of(IClusterServices.cacheMode.TRANSACTIONAL));

            clusterGroupContainerService.createCache("frm.nodeGroups",
                    EnumSet.of(IClusterServices.cacheMode.TRANSACTIONAL));

            // TODO for cluster mode
            /*
             * clusterGroupContainerService.createCache(WORK_STATUS_CACHE,
             * EnumSet.of(IClusterServices.cacheMode.NON_TRANSACTIONAL,
             * IClusterServices.cacheMode.ASYNC));
             *
             * clusterGroupContainerService.createCache(WORK_ORDER_CACHE,
             * EnumSet.of(IClusterServices.cacheMode.NON_TRANSACTIONAL,
             * IClusterServices.cacheMode.ASYNC));
             */

        } catch (CacheConfigException cce) {
            logger.error("Group CacheConfigException");
            return false;

        } catch (CacheExistException cce) {
            logger.error(" Group CacheExistException");
        }

        return true;
    }

    private void nonClusterGroupObjectCreate() {
        originalSwGroupView = new ConcurrentHashMap<GroupKey, Group>();
        installedSwGroupView = new ConcurrentHashMap<GroupKey, Group>();
        nodeGroups = new ConcurrentHashMap<Node, List<Group>>();
        inactiveGroups = new ConcurrentHashMap<GroupKey, Group>();
    }

    @SuppressWarnings({ "unchecked" })
    private boolean retrieveGroupCaches() {
        ConcurrentMap<?, ?> map;

        if (this.clusterGroupContainerService == null) {
            logger.warn("Group: un-initialized clusterGroupContainerService, can't retrieve cache");
            nonClusterGroupObjectCreate();
            return false;
        }

        map = clusterGroupContainerService.getCache("frm.originalSwGroupView");
        if (map != null) {
            originalSwGroupView = (ConcurrentMap<GroupKey, Group>) map;
        } else {
            logger.error("Retrieval of cache(originalSwGroupView) failed");
            return false;
        }

        map = clusterGroupContainerService.getCache("frm.installedSwGroupView");
        if (map != null) {
            installedSwGroupView = (ConcurrentMap<GroupKey, Group>) map;
        } else {
            logger.error("Retrieval of cache(installedSwGroupView) failed");
            return false;
        }

        map = clusterGroupContainerService.getCache("frm.inactiveGroups");
        if (map != null) {
            inactiveGroups = (ConcurrentMap<GroupKey, Group>) map;
        } else {
            logger.error("Retrieval of cache(inactiveGroups) failed");
            return false;
        }

        map = clusterGroupContainerService.getCache("frm.nodeGroups");
        if (map != null) {
            nodeGroups = (ConcurrentMap<Node, List<Group>>) map;
        } else {
            logger.error("Retrieval of cache(nodeGroup) failed");
            return false;
        }

        return true;
    }

    private boolean cacheStartup() {
        if (allocateGroupCaches()) {
            if (retrieveGroupCaches()) {
                return true;
            }
        }

        return false;
    }

    public Status validateGroup(Group group, FRMUtil.operation operation) {
        String containerName;
        String groupName;
        Iterator<Bucket> bucketIterator;
        boolean returnResult;
        Buckets groupBuckets;

        if (null != group) {
            containerName = group.getContainerName();

            if (null == containerName) {
                containerName = GlobalConstants.DEFAULT.toString();
            } else if (!FRMUtil.isNameValid(containerName)) {
                logger.error("Container Name is invalid %s" + containerName);
                return new Status(StatusCode.BADREQUEST, "Container Name is invalid");
            }

            groupName = group.getGroupName();
            if (!FRMUtil.isNameValid(groupName)) {
                logger.error("Group Name is invalid %s" + groupName);
                return new Status(StatusCode.BADREQUEST, "Group Name is invalid");
            }

            returnResult = doesGroupEntryExists(group.getKey(), groupName, containerName);

            if (FRMUtil.operation.ADD == operation && returnResult) {
                logger.error("Record with same Group Name exists");
                return new Status(StatusCode.BADREQUEST, "Group record exists");
            } else if (!returnResult) {
                logger.error("Group record does not exist");
                return new Status(StatusCode.BADREQUEST, "Group record does not exist");
            }

            if (!(group.getGroupType().getIntValue() >= GroupType.GroupAll.getIntValue() && group.getGroupType()
                    .getIntValue() <= GroupType.GroupFf.getIntValue())) {
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

    private boolean doesGroupEntryExists(GroupKey key, String groupName, String containerName) {
        if (!originalSwGroupView.containsKey(key)) {
            return false;
        }

        for (ConcurrentMap.Entry<GroupKey, Group> entry : originalSwGroupView.entrySet()) {
            if (entry.getValue().getGroupName().equals(groupName)) {
                if (entry.getValue().getContainerName().equals(containerName)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Update Group entries to the southbound plugin/inventory and our internal
     * database
     *
     * @param path
     * @param dataObject
     */
    private Status updateGroup(InstanceIdentifier<?> path, Group groupUpdateDataObject) {
        GroupKey groupKey = groupUpdateDataObject.getKey();
        UpdatedGroupBuilder updateGroupBuilder = null;

        Status groupOperationStatus = validateGroup(groupUpdateDataObject, FRMUtil.operation.UPDATE);

        if (!groupOperationStatus.isSuccess()) {
            logger.error("Group data object validation failed %s" + groupUpdateDataObject.getGroupName());
            return groupOperationStatus;
        }

        if (originalSwGroupView.containsKey(groupKey)) {
            originalSwGroupView.remove(groupKey);
            originalSwGroupView.put(groupKey, groupUpdateDataObject);
        }

        if (groupUpdateDataObject.isInstall()) {
            UpdateGroupInputBuilder groupData = new UpdateGroupInputBuilder();
            updateGroupBuilder = new UpdatedGroupBuilder();
            updateGroupBuilder.fieldsFrom(groupUpdateDataObject);
            groupData.setUpdatedGroup(updateGroupBuilder.build());
            // TODO how to get original group and modified group.

            if (installedSwGroupView.containsKey(groupKey)) {
                installedSwGroupView.remove(groupKey);
                installedSwGroupView.put(groupKey, groupUpdateDataObject);
            }

            groupService.updateGroup(groupData.build());
        }

        return groupOperationStatus;
    }

    /**
     * Adds Group to the southbound plugin and our internal database
     *
     * @param path
     * @param dataObject
     */
    private Status addGroup(InstanceIdentifier<?> path, Group groupAddDataObject) {
        GroupKey groupKey = groupAddDataObject.getKey();
        Status groupOperationStatus = validateGroup(groupAddDataObject, FRMUtil.operation.ADD);

        if (!groupOperationStatus.isSuccess()) {
            logger.error("Group data object validation failed %s" + groupAddDataObject.getGroupName());
            return groupOperationStatus;
        }

        originalSwGroupView.put(groupKey, groupAddDataObject);

        if (groupAddDataObject.isInstall()) {
            AddGroupInputBuilder groupData = new AddGroupInputBuilder();
            groupData.setBuckets(groupAddDataObject.getBuckets());
            groupData.setContainerName(groupAddDataObject.getContainerName());
            groupData.setGroupId(groupAddDataObject.getGroupId());
            groupData.setGroupType(groupAddDataObject.getGroupType());
            groupData.setNode(groupAddDataObject.getNode());
            installedSwGroupView.put(groupKey, groupAddDataObject);
            groupService.addGroup(groupData.build());
        }

        return groupOperationStatus;
    }

    private RpcResult<Void> commitToPlugin(internalTransaction transaction) {
        for (Entry<InstanceIdentifier<?>, Group> entry : transaction.additions.entrySet()) {

            if (!addGroup(entry.getKey(), entry.getValue()).isSuccess()) {
                transaction.additions.remove(entry.getKey());
                return Rpcs.getRpcResult(false, null, null);
            }
        }

        for (Entry<InstanceIdentifier<?>, Group> entry : transaction.updates.entrySet()) {

            if (!updateGroup(entry.getKey(), entry.getValue()).isSuccess()) {
                transaction.updates.remove(entry.getKey());
                return Rpcs.getRpcResult(false, null, null);
            }
        }

        for (InstanceIdentifier<?> removal : transaction.removals) {
            // removeFlow(removal);
        }

        return Rpcs.getRpcResult(true, null, null);
    }

    private final class GroupDataCommitHandler implements DataCommitHandler<InstanceIdentifier<?>, DataObject> {

        @SuppressWarnings("unchecked")
        @Override
        public DataCommitTransaction<InstanceIdentifier<?>, DataObject> requestCommit(
                DataModification<InstanceIdentifier<?>, DataObject> modification) {
            // We should verify transaction
            System.out.println("Coming in GroupDatacommitHandler");
            internalTransaction transaction = new internalTransaction(modification);
            transaction.prepareUpdate();
            return transaction;
        }
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

        Map<InstanceIdentifier<?>, Group> additions = new HashMap<>();
        Map<InstanceIdentifier<?>, Group> updates = new HashMap<>();
        Set<InstanceIdentifier<?>> removals = new HashSet<>();

        /**
         * We create a plan which flows will be added, which will be updated and
         * which will be removed based on our internal state.
         *
         */
        void prepareUpdate() {

            Set<Entry<InstanceIdentifier<?>, DataObject>> puts = modification.getUpdatedConfigurationData().entrySet();
            for (Entry<InstanceIdentifier<?>, DataObject> entry : puts) {
                if (entry.getValue() instanceof Group) {
                    Group group = (Group) entry.getValue();
                    preparePutEntry(entry.getKey(), group);
                }

            }

            removals = modification.getRemovedConfigurationData();
        }

        private void preparePutEntry(InstanceIdentifier<?> key, Group group) {

            Group original = originalSwGroupView.get(key);
            if (original != null) {
                // It is update for us

                updates.put(key, group);
            } else {
                // It is addition for us

                additions.put(key, group);
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

    final class GroupEventListener implements SalGroupListener {

        List<GroupAdded> addedGroups = new ArrayList<>();
        List<GroupRemoved> removedGroups = new ArrayList<>();
        List<GroupUpdated> updatedGroups = new ArrayList<>();

        @Override
        public void onGroupAdded(GroupAdded notification) {
            System.out.println("added Group..........................");
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

    @Override
    public List<DataObject> get() {

        List<DataObject> orderedList = new ArrayList<DataObject>();
        Collection<Group> groupList = originalSwGroupView.values();
        for (Iterator<Group> iterator = groupList.iterator(); iterator.hasNext();) {
            orderedList.add(iterator.next());
        }
        return orderedList;
    }

    @Override
    public DataObject getWithName(String name, Node n) {

        if (this instanceof GroupConsumerImpl) {
            Collection<Group> groupList = originalSwGroupView.values();
            for (Iterator<Group> iterator = groupList.iterator(); iterator.hasNext();) {
                Group group = iterator.next();
                if (group.getNode().equals(n) && group.getGroupName().equals(name)) {

                    return group;
                }
            }
        }
        return null;
    }
}
