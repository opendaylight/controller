package org.opendaylight.controller.datastore.infinispan.utils;

import org.junit.Test;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;

import java.util.Set;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

public class AugmentationIdentifierGeneratorTest {
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



    @Test
    public void testBasic(){
        final InstanceIdentifier.AugmentationIdentifier pathArgument = new AugmentationIdentifierGenerator(AUGMENTATION_ID).getPathArgument();

        assertEquals(null, pathArgument.getNodeType());

        final Set<QName> possibleChildNames = pathArgument.getPossibleChildNames();

        QName supportedInstructions = QName.create("(urn:opendaylight:flow:inventory?revision=2013-08-19)supported-instructions");
        assertTrue(possibleChildNames.contains(supportedInstructions));


        QName group = QName.create("(urn:opendaylight:flow:inventory?revision=2013-08-19)group");
        assertTrue(possibleChildNames.contains(group));


        QName nonExistent = QName.create("(urn:opendaylight:flow:inventory?revision=2013-08-19)non-existent");
        assertFalse(possibleChildNames.contains(nonExistent));
    }

}
