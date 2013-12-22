package org.opendaylight.controller.frm.flow;

import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;

import org.opendaylight.controller.forwardingrulesmanager.consumer.impl.FRMUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.Flow;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FlowTransactionValidator {
    protected static final Logger logger = LoggerFactory.getLogger(FlowTransactionValidator.class);

    public static void validate(FlowTransaction transaction) throws IllegalStateException {
        // NOOP
        Set<Entry<InstanceIdentifier<?>, DataObject>> createdEntries = transaction.getModification()
                .getCreatedConfigurationData().entrySet();

        Set<Entry<InstanceIdentifier<?>, DataObject>> updatedEntries = new HashSet<Entry<InstanceIdentifier<?>, DataObject>>();
        updatedEntries.addAll(transaction.getModification().getUpdatedConfigurationData().entrySet());
        updatedEntries.removeAll(createdEntries);

        Set<InstanceIdentifier<?>> removeEntriesInstanceIdentifiers = transaction.getModification()
                .getRemovedConfigurationData();
        transaction.getModification().getOriginalConfigurationData();

        for (Entry<InstanceIdentifier<?>, DataObject> entry : createdEntries) {
            if (entry.getValue() instanceof Flow) {
                logger.debug("Coming add cc in FlowDatacommitHandler");
                Flow flow = (Flow) entry.getValue();
                boolean status = validate(flow);
                if (!status) {
                    return;
                }
            }
        }

        for (Entry<InstanceIdentifier<?>, DataObject> entry : updatedEntries) {
            if (entry.getValue() instanceof Flow) {
                logger.debug("Coming update cc in FlowDatacommitHandler");
                Flow updatedFlow = (Flow) entry.getValue();
                Flow originalFlow = (Flow) transaction.getModification().getOriginalConfigurationData()
                        .get(entry.getKey());
                boolean status = validate(updatedFlow);
                if (!status) {
                    return;
                }
            }
        }

        for (InstanceIdentifier<?> instanceId : removeEntriesInstanceIdentifiers) {
            DataObject removeValue = transaction.getModification().getOriginalConfigurationData().get(instanceId);
            if (removeValue instanceof Flow) {
                logger.debug("Coming remove cc in FlowDatacommitHandler");
                Flow flow = (Flow) removeValue;
                boolean status = validate(flow);
                if (!status) {
                    return;
                }
            }
        }
    }

    public static boolean validate(Flow flow) {

        String msg = ""; // Specific part of warn/error log

        boolean result = true;
        // flow Name validation
        if (!FRMUtil.isNameValid(flow.getFlowName())) {
            msg = "Invalid Flow name";
            result = false;
        }

        // TODO: Validate we are seeking to program a flow against a valid
        // Node

        if (result == true && flow.getPriority() != null) {
            if (flow.getPriority() < 0 || flow.getPriority() > 65535) {
                msg = String.format("priority %s is not in the range 0 - 65535", flow.getPriority());
                result = false;
            }
        }

        if (!FRMUtil.validateMatch(flow)) {
            logger.error("Not a valid Match");
            result = false;
        }
        if (!FRMUtil.validateInstructions(flow)) {
            logger.error("Not a valid Instruction");
            result = false;
        }
        if (result == false) {
            logger.warn("Invalid Configuration for flow {}. The failure is {}", flow, msg);
            logger.error("Invalid Configuration ({})", msg);
        }
        return result;
    }

}
