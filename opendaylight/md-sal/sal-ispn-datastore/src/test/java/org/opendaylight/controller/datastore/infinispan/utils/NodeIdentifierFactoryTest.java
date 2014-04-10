package org.opendaylight.controller.datastore.infinispan.utils;

import org.junit.Test;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;

import static org.jgroups.util.Util.assertTrue;

public class NodeIdentifierFactoryTest {

    private static final String AUGMENTATION_ID = "AugmentationIdentifier{childNames=[(urn:opendaylight:flow:inventory?revision=2013-08-19)description, " +
            "(urn:opendaylight:flow:inventory?revision=2013-08-19)group, " +
            "(urn:opendaylight:flow:inventory?revision=2013-08-19)hardware, " +
            "(urn:opendaylight:flow:inventory?revision=2013-08-19)manufacturer, " +
            "(urn:opendaylight:flow:inventory?revision=2013-08-19)meter, " +
            "(urn:opendaylight:flow:inventory?revision=2013-08-19)serial-number, " +
            "(urn:opendaylight:flow:inventory?revision=2013-08-19)software, " +
            "(urn:opendaylight:flow:inventory?revision=2013-08-19)supported-actions, " +
            "(urn:opendaylight:flow:inventory?revision=2013-08-19)supported-instructions, " +
            "(urn:opendaylight:flow:inventory?revision=2013-08-19)supported-match-types, " +
            "(urn:opendaylight:flow:inventory?revision=2013-08-19)switch-features, " +
            "(urn:opendaylight:flow:inventory?revision=2013-08-19)table]}";

    private static final String NODE_ID_WITH_PREDICATES_ID = "(urn:opendaylight:flow:inventory?revision=2013-08-19)table[{(urn:opendaylight:flow:inventory?revision=2013-08-19)foobar=valu}]";

    private static final String NODE_ID = "(urn:opendaylight:flow:inventory?revision=2013-08-19)table";

    private static final String NODE_ID_WITH_VALUE_ID = "(urn:opendaylight:flow:inventory?revision=2013-08-19)table[foobar]";

    @Test
    public void testAugmentationIdentifier(){
        assertTrue(NodeIdentifierFactory.getArgument(AUGMENTATION_ID) instanceof InstanceIdentifier.AugmentationIdentifier);
    }

    @Test
    public void testNodeIdentifier(){
        assertTrue(NodeIdentifierFactory.getArgument(NODE_ID) instanceof InstanceIdentifier.NodeIdentifier);
    }

    @Test
    public void testNodeIdentifierWithPredicates(){
        assertTrue(NodeIdentifierFactory.getArgument(NODE_ID_WITH_PREDICATES_ID) instanceof InstanceIdentifier.NodeIdentifierWithPredicates);
    }

    @Test
    public void testNodeIdentifierWithValue(){
        assertTrue(NodeIdentifierFactory.getArgument(NODE_ID_WITH_VALUE_ID) instanceof InstanceIdentifier.NodeWithValue);
    }
}
