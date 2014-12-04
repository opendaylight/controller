package test.mock.util;

import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.GroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.GroupTypes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.groups.Group;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.groups.GroupBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.groups.GroupKey;

import java.util.Random;

public class GroupMockGenerator {
    private static final Random rnd = new Random();
    private static final GroupBuilder groupBuilder = new GroupBuilder();

    public static Group getRandomGroup() {
        groupBuilder.setKey(new GroupKey(new GroupId(TestUtils.nextLong(0, 4294967295L))));
        groupBuilder.setContainerName("container." + rnd.nextInt(1000));
        groupBuilder.setBarrier(rnd.nextBoolean());
        groupBuilder.setGroupName("group." + rnd.nextInt(1000));
        groupBuilder.setGroupType(GroupTypes.forValue(rnd.nextInt(4)));
        return groupBuilder.build();
    }
}
