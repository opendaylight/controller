package org.opendaylight.controller.cluster.datastore.node.utils;

import junit.framework.Assert;
import org.junit.Test;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;

public class NodeIdentifierFactoryTest {

    @Test
    public void validateAugmentationIdentifier(){
        InstanceIdentifier.PathArgument argument = NodeIdentifierFactory
            .getArgument(
                "AugmentationIdentifier{childNames=[(urn:opendaylight:flow:table:statistics?revision=2013-12-15)flow-table-statistics]}");

        Assert.assertTrue(argument instanceof InstanceIdentifier.AugmentationIdentifier);


    }

}
