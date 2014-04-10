package org.opendaylight.controller.datastore.infinispan.utils;

import org.junit.Test;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;

import static junit.framework.Assert.assertNotNull;

public class NodeIdentifierWithPredicatesGeneratorTest {
    @Test
    public void testBasic(){
        final NodeIdentifierWithPredicatesGenerator nodeIdentifierWithPredicatesGenerator = new NodeIdentifierWithPredicatesGenerator("(urn:TBD:params:xml:ns:yang:network-topology?revision=2013-10-21)link[{(urn:TBD:params:xml:ns:yang:network-topology?revision=2013-10-21)link-id=openflow:10:2}]");
        final InstanceIdentifier.NodeIdentifierWithPredicates pathArgument = nodeIdentifierWithPredicatesGenerator.getPathArgument();

        assertNotNull(pathArgument.getKeyValues().get(QName.create("(urn:TBD:params:xml:ns:yang:network-topology?revision=2013-10-21)link-id")));

    }
}
