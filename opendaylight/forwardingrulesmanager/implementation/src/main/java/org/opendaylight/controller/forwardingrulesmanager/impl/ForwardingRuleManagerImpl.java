package org.opendaylight.controller.forwardingrulesmanager.impl;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.opendaylight.controller.md.sal.common.api.data.DataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.DataCommitHandler;
import org.opendaylight.controller.md.sal.common.api.data.DataModification;
import org.opendaylight.controller.md.sal.common.api.data.DataReader;

import org.opendaylight.controller.sal.binding.api.data.DataChangeListener;
import org.opendaylight.controller.sal.binding.api.data.DataProviderService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.config.rev130819.Flows;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.config.rev130819.FlowsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.config.rev130819.flows.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.config.rev130819.flows.FlowKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.OpendaylightFlowStatisticsService;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import static org.opendaylight.controller.forwardingrulesmanager.impl.FlowManagementConstants.*;
import static org.opendaylight.controller.forwardingrulesmanager.impl.FlowStatisticsUtils.*;

public class ForwardingRuleManagerImpl implements 
        DataReader<InstanceIdentifier<? extends DataObject>, DataObject>,
        DataCommitHandler<InstanceIdentifier<? extends DataObject>, DataObject> {

    private DataProviderService dataService;
    private OpendaylightFlowStatisticsService flowStatistics;

    ConcurrentHashMap<FlowKey, ManagedFlowEntry> flows;

    FlowManagementReader configuration = new ConfigurationReader();
    FlowManagementReader operational = new OperationalReader();

    void init() {
        dataService.registerDataChangeListener(FLOWS_PATH, changeListener);
        dataService.registerCommitHandler(FLOWS_PATH, this);
    }

    private final DataChangeListener changeListener = new DataChangeListener() {

        @Override
        public void onDataChanged(
                DataChangeEvent<InstanceIdentifier<? extends DataObject>, DataObject> change) {
        }
    };

    @Override
    public DataObject readConfigurationData(
            InstanceIdentifier<? extends DataObject> path) {
        return readFrom(configuration, path);
    }

    @Override
    public DataObject readOperationalData(
            InstanceIdentifier<? extends DataObject> path) {
        return readFrom(operational, path);
    }

    private DataObject readFrom(FlowManagementReader store,
            InstanceIdentifier<? extends DataObject> path) {
        if (FLOWS_PATH.equals(path)) {
            return (Flows) store.readAllFlows();
        }
        if (FLOWS_PATH.contains(path)) {
            return null;
        }
        return null;
    }

    private class OperationalReader implements FlowManagementReader {

        @Override
        public Flows readAllFlows() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Flow readFlow(FlowKey key) {
            ManagedFlowEntry cfgFlow = flows.get(key);
            if (cfgFlow == null)
                return null; // We return null - flow is not managed by this
                             // store

            flowStatistics
                    .getFlowStatistics(flowStatisticsRequest(cfgFlow.flow)
                            .build());

            // FIXME: DO processing

            return null;
        }
    }

    private class ManagedFlowEntry {
        public final InstanceIdentifier<Flow> id;
        public Flow flow;

        public ManagedFlowEntry(InstanceIdentifier<Flow> id) {
            this.id = id;
        }
    }

    private class ConfigurationReader implements FlowManagementReader {

        @Override
        public Flows readAllFlows() {
            FlowsBuilder flowsBuilder = new FlowsBuilder();
            // FIXME: Add export of all configuration flows.
            flows.entrySet();

            return flowsBuilder.build();
        }

        @Override
        public Flow readFlow(FlowKey key) {
            ManagedFlowEntry entry = flows.get(key);
            if (entry == null)
                return null;
            return entry.flow;
        }
    }

    @Override
    public FlowCommitTransaction requestCommit(
            DataModification<InstanceIdentifier<? extends DataObject>, DataObject> modification) {
        return startTransactionInternal(modification);
    }

    private FlowCommitTransaction startTransactionInternal(
            DataModification<InstanceIdentifier<? extends DataObject>, DataObject> modification) {

        return null;
    }

    public static class FlowCommitTransaction
            implements
            DataCommitTransaction<InstanceIdentifier<? extends DataObject>, DataObject> {

        final DataModification<InstanceIdentifier<? extends DataObject>, DataObject> modification;

        Map<FlowKey, ManagedFlowEntry> additions;
        Map<FlowKey, ManagedFlowEntry> updates;
        Map<FlowKey, ManagedFlowEntry> removals;

        final LinkedHashSet<ManagedFlowEntry> installedFlows;

        public FlowCommitTransaction(
                DataModification<InstanceIdentifier<? extends DataObject>, DataObject> modification) {
            super();
            this.modification = modification;
            this.installedFlows = new LinkedHashSet<>();
        }

        @Override
        public DataModification<InstanceIdentifier<? extends DataObject>, DataObject> getModification() {
            return modification;
        }

        @Override
        public RpcResult<Void> finish() throws IllegalStateException {
            return null;
        }

        @Override
        public RpcResult<Void> rollback() throws IllegalStateException {
            return null;
        }

        void processModification() {
            Map<InstanceIdentifier<? extends DataObject>, DataObject> updated = modification
                    .getUpdatedConfigurationData();

            Set<InstanceIdentifier<? extends DataObject>> removed = modification
                    .getRemovedConfigurationData();

        }
    }
}
