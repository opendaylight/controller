package org.opendaylight.controller.md.statistics.manager.impl;

import com.google.common.util.concurrent.Futures;
import java.util.concurrent.Future;
import org.opendaylight.controller.md.statistics.manager.StatisticsManager;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.operational.status.rev141127.ChangeOperationalStatusInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.operational.status.rev141127.GetOperationalStatusOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.operational.status.rev141127.GetOperationalStatusOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.operational.status.rev141127.OperationalStatusService;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;

/**
 * Created by Martin Bobak mbobak@cisco.com on 11/27/14.
 */
public class SMOperationalStatusService implements OperationalStatusService {

    private static StatisticsManager SMInstance;

    public SMOperationalStatusService(StatisticsManager reference) {
        this.SMInstance = reference;
    }

    @Override
    public Future<RpcResult<Void>> changeOperationalStatus(ChangeOperationalStatusInput input) {
        this.SMInstance.setOperationalStatus(input.getOperationalStatus());
        return Futures.immediateFuture(RpcResultBuilder.<Void>success().build());
    }

    @Override
    public Future<RpcResult<GetOperationalStatusOutput>> getOperationalStatus() {
        GetOperationalStatusOutputBuilder getOperationalStatusOutputBuilder = new GetOperationalStatusOutputBuilder();
        getOperationalStatusOutputBuilder.setOperationalStatus(this.SMInstance.getOperationalStatus());
        RpcResultBuilder<GetOperationalStatusOutput> rpcResultBuilder = RpcResultBuilder.success();
        rpcResultBuilder.withResult(getOperationalStatusOutputBuilder.build());
        return Futures.immediateFuture(rpcResultBuilder.build());
    }
}
