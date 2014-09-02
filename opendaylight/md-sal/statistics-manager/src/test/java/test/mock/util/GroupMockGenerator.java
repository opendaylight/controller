package test.mock.util;

import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.GroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.GroupTypes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.groups.Group;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.groups.GroupBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.groups.GroupKey;

import java.util.Random;

public class GroupMockGenerator {
    private static final Random RND = new Random();
    private static final GroupBuilder GROUP_BUILDER = new GroupBuilder();

    public static Group getRandomGroup() {
        GROUP_BUILDER.setKey(new GroupKey(new GroupId(nextLong(0, 4294967295L))));
        GROUP_BUILDER.setContainerName("container." + RND.nextInt(1000));
        GROUP_BUILDER.setBarrier(RND.nextBoolean());
        GROUP_BUILDER.setGroupName("group." + RND.nextInt(1000));
        GROUP_BUILDER.setGroupType(GroupTypes.forValue(RND.nextInt(4)));
        return GROUP_BUILDER.build();
    }

    private static long nextLong(long RangeBottom, long rangeTop) {
        return RangeBottom + ((long)(RND.nextDouble()*(rangeTop - RangeBottom)));
    }
}
