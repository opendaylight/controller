package org.opendaylight.controller.frm.group;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;

import org.opendaylight.controller.forwardingrulesmanager.consumer.impl.FRMUtil;
import org.opendaylight.controller.md.sal.common.api.data.DataModification;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.Group;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.GroupTypes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.group.Buckets;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.group.buckets.Bucket;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GroupTransactionValidator {
    protected static final Logger logger = LoggerFactory.getLogger(GroupTransactionValidator.class);

    public static void validate(GroupTransaction transaction) throws IllegalStateException {
        // NOOP
        DataModification<InstanceIdentifier<?>, DataObject> modification = transaction.getModification();
        // get created entries
        Set<Entry<InstanceIdentifier<? extends DataObject>, DataObject>> createdEntries = modification
                .getCreatedConfigurationData().entrySet();

        // get updated entries
        Set<Entry<InstanceIdentifier<? extends DataObject>, DataObject>> updatedEntries = new HashSet<Entry<InstanceIdentifier<? extends DataObject>, DataObject>>();

        updatedEntries.addAll(modification.getUpdatedConfigurationData().entrySet());
        updatedEntries.removeAll(createdEntries);

        // get removed entries
        Set<InstanceIdentifier<? extends DataObject>> removeEntriesInstanceIdentifiers = modification
                .getRemovedConfigurationData();

        for (Entry<InstanceIdentifier<? extends DataObject>, DataObject> entry : createdEntries) {
            if (entry.getValue() instanceof Group) {
                Group group = (Group) entry.getValue();
                boolean status = validateGroup(group);
                if (!status) {
                    return;
                }
            }
        }

        for (Entry<InstanceIdentifier<?>, DataObject> entry : updatedEntries) {
            if (entry.getValue() instanceof Group) {
                Group originalGroup = (Group) modification.getOriginalConfigurationData().get(entry.getKey());
                Group updatedGroup = (Group) entry.getValue();
                boolean status = validateGroup(updatedGroup);
                if (!status) {
                    return;
                }
            }
        }

        for (InstanceIdentifier<?> instanceId : removeEntriesInstanceIdentifiers) {
            DataObject removeValue = modification.getOriginalConfigurationData().get(instanceId);
            if (removeValue instanceof Group) {
                Group removeGroup = (Group) removeValue;
                boolean status = validateGroup(removeGroup);
                if (!status) {
                    return;
                }
            }
        }
    }

    public static boolean validateGroup(Group group) {
        String groupName;
        Iterator<Bucket> bucketIterator;
        Buckets groupBuckets;

        if (null != group) {
            groupName = group.getGroupName();
            if (!FRMUtil.isNameValid(groupName)) {
                logger.error("Group Name is invalid %s" + groupName);
                return false;
            }

            if (!(group.getGroupType().getIntValue() >= GroupTypes.GroupAll.getIntValue() && group.getGroupType()
                    .getIntValue() <= GroupTypes.GroupFf.getIntValue())) {
                logger.error("Invalid Group type %d" + group.getGroupType().getIntValue());
                return false;
            }

            groupBuckets = group.getBuckets();

            if (null != groupBuckets && null != groupBuckets.getBucket()) {
                bucketIterator = groupBuckets.getBucket().iterator();

                while (bucketIterator.hasNext()) {
                    if (!(FRMUtil.validateActions(bucketIterator.next().getAction()))) {
                        logger.error("Error in action bucket");
                        return false;
                    }
                }
            }
        }

        return false;

    }

}
