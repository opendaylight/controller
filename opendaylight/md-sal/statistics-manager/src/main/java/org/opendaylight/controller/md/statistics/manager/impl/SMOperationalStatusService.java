package org.opendaylight.controller.md.statistics.manager.impl;

import com.google.common.util.concurrent.Futures;
import java.util.concurrent.Future;
import org.opendaylight.controller.md.statistics.manager.OperationalStatusHolder;
import org.opendaylight.controller.md.statistics.manager.StatisticsManager;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.statistics.manager.rev140925.ChangeOperationalStatusInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.statistics.manager.rev140925.GetOperationalStatusOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.statistics.manager.rev140925.GetOperationalStatusOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.statistics.manager.rev140925.OperStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.statistics.manager.rev140925.StatisticsManagerService;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by Martin Bobak mbobak@cisco.com on 11/27/14.
 */
public class SMOperationalStatusService implements StatisticsManagerService {

    private StatisticsManager SMInstance;
    private static final Logger LOG = LoggerFactory.getLogger(SMOperationalStatusService.class);

    public SMOperationalStatusService(StatisticsManager reference) {
        this.SMInstance = reference;
    }

    @Override
    public Future<RpcResult<Void>> changeOperationalStatus(ChangeOperationalStatusInput input) {
        OperationalStatusHolder.setOperationalStatus(input.getOperationalStatus());
        if (input.getOperationalStatus().equals(OperStatus.STANDBY)) {
            SMInstance.sleep();
        } else if (input.getOperationalStatus().equals(OperStatus.RUN)) {
            SMInstance.wakeUp();
        }
        return Futures.immediateFuture(RpcResultBuilder.<Void>success().build());
    }

    @Override
    public Future<RpcResult<GetOperationalStatusOutput>> getOperationalStatus() {
        GetOperationalStatusOutputBuilder getOperationalStatusOutputBuilder = new GetOperationalStatusOutputBuilder();
        getOperationalStatusOutputBuilder.setOperationalStatus(OperationalStatusHolder.getOperationalStatus());
        RpcResultBuilder<GetOperationalStatusOutput> rpcResultBuilder = RpcResultBuilder.success();
        rpcResultBuilder.withResult(getOperationalStatusOutputBuilder.build());
        return Futures.immediateFuture(rpcResultBuilder.build());
    }
}
