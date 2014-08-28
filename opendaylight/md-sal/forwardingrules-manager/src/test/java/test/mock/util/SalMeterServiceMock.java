package test.mock.util;

import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.service.rev130918.AddMeterInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.service.rev130918.AddMeterOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.service.rev130918.RemoveMeterInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.service.rev130918.RemoveMeterOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.service.rev130918.SalMeterService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.service.rev130918.UpdateMeterInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.service.rev130918.UpdateMeterOutput;
import org.opendaylight.yangtools.yang.common.RpcResult;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

public class SalMeterServiceMock implements SalMeterService {
    private List<AddMeterInput> addMeterCalls = new ArrayList<>();
    private List<RemoveMeterInput> removeMeterCalls = new ArrayList<>();
    private List<UpdateMeterInput> updateMeterCalls = new ArrayList<>();

    @Override
    public Future<RpcResult<AddMeterOutput>> addMeter(AddMeterInput input) {
        addMeterCalls.add(input);
        return null;
    }

    @Override
    public Future<RpcResult<RemoveMeterOutput>> removeMeter(RemoveMeterInput input) {
        removeMeterCalls.add(input);
        return null;
    }

    @Override
    public Future<RpcResult<UpdateMeterOutput>> updateMeter(UpdateMeterInput input) {
        updateMeterCalls.add(input);
        return null;
    }

    public List<AddMeterInput> getAddMeterCalls() {
        return addMeterCalls;
    }

    public List<RemoveMeterInput> getRemoveMeterCalls() {
        return removeMeterCalls;
    }

    public List<UpdateMeterInput> getUpdateMeterCalls() {
        return updateMeterCalls;
    }
}
