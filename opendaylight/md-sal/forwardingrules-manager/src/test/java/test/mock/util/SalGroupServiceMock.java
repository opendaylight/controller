package test.mock.util;

import org.opendaylight.yang.gen.v1.urn.opendaylight.group.service.rev130918.AddGroupInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.service.rev130918.AddGroupOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.service.rev130918.RemoveGroupInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.service.rev130918.RemoveGroupOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.service.rev130918.SalGroupService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.service.rev130918.UpdateGroupInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.service.rev130918.UpdateGroupOutput;
import org.opendaylight.yangtools.yang.common.RpcResult;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

public class SalGroupServiceMock implements SalGroupService {
    private List<AddGroupInput> addGroupCalls = new ArrayList<>();
    private List<RemoveGroupInput> removeGroupCalls = new ArrayList<>();
    private List<UpdateGroupInput> updateGroupCalls = new ArrayList<>();

    @Override
    public Future<RpcResult<AddGroupOutput>> addGroup(AddGroupInput input) {
        addGroupCalls.add(input);
        return null;
    }

    @Override
    public Future<RpcResult<RemoveGroupOutput>> removeGroup(RemoveGroupInput input) {
        removeGroupCalls.add(input);
        return null;
    }

    @Override
    public Future<RpcResult<UpdateGroupOutput>> updateGroup(UpdateGroupInput input) {
        updateGroupCalls.add(input);
        return null;
    }

    public List<AddGroupInput> getAddGroupCalls() {
        return addGroupCalls;
    }

    public List<RemoveGroupInput> getRemoveGroupCalls() {
        return removeGroupCalls;
    }

    public List<UpdateGroupInput> getUpdateGroupCalls() {
        return updateGroupCalls;
    }
}
