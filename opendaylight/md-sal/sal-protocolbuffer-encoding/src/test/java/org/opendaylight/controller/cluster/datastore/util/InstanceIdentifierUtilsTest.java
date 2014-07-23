package org.opendaylight.controller.cluster.datastore.util;

import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.controller.protobuff.messages.common.NormalizedNodeMessages;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

public class InstanceIdentifierUtilsTest {

    private static QName TEST_QNAME = QName.create("(urn:opendaylight:params:xml:ns:yang:controller:md:sal:dom:store:test?revision=2014-03-13)test");
    private static QName NODE_WITH_VALUE_QNAME = QName.create("(urn:opendaylight:params:xml:ns:yang:controller:md:sal:dom:store:test?revision=2014-03-13)value");
    private static QName NODE_WITH_PREDICATES_QNAME = QName.create("(urn:opendaylight:params:xml:ns:yang:controller:md:sal:dom:store:test?revision=2014-03-13)pred");
    private static QName NAME_QNAME = QName.create("(urn:opendaylight:params:xml:ns:yang:controller:md:sal:dom:store:test?revision=2014-03-13)name");

    @Test
    public void testSerializationOfNodeIdentifier(){
        InstanceIdentifier.PathArgument p1 =
            new InstanceIdentifier.NodeIdentifier(TEST_QNAME);

        List<InstanceIdentifier.PathArgument> arguments = new ArrayList<>();

        arguments.add(p1);

        InstanceIdentifier expected = InstanceIdentifier.create(arguments);

        NormalizedNodeMessages.InstanceIdentifier instanceIdentifier =
            InstanceIdentifierUtils.toSerializable(expected);

        InstanceIdentifier actual =
            InstanceIdentifierUtils.fromSerializable(instanceIdentifier);


        Assert.assertEquals(expected.getLastPathArgument(),
            actual.getLastPathArgument());


    }

    @Test
    public void testSerializationOfNodeWithValue(){

        withValue((short) 1);
        withValue((long) 2);
        withValue(3);
        withValue(true);

    }

    private void withValue(Object value){
        InstanceIdentifier.PathArgument p1 =
            new InstanceIdentifier.NodeIdentifier(TEST_QNAME);

        InstanceIdentifier.PathArgument p2 =
            new InstanceIdentifier.NodeWithValue(NODE_WITH_VALUE_QNAME, value);


        List<InstanceIdentifier.PathArgument> arguments = new ArrayList<>();

        arguments.add(p1);
        arguments.add(p2);

        InstanceIdentifier expected = InstanceIdentifier.create(arguments);

        NormalizedNodeMessages.InstanceIdentifier instanceIdentifier =
            InstanceIdentifierUtils.toSerializable(expected);

        InstanceIdentifier actual =
            InstanceIdentifierUtils.fromSerializable(instanceIdentifier);


        Assert.assertEquals(expected.getLastPathArgument(),
            actual.getLastPathArgument());
    }


    @Test
    public void testSerializationOfNodeIdentifierWithPredicates(){

        withPredicates((short) 1);
        withPredicates((long) 2);
        withPredicates(3);
        withPredicates(true);

    }

    private void withPredicates(Object value){
        InstanceIdentifier.PathArgument p1 =
            new InstanceIdentifier.NodeIdentifier(TEST_QNAME);

        InstanceIdentifier.PathArgument p2 =
            new InstanceIdentifier.NodeIdentifierWithPredicates(NODE_WITH_PREDICATES_QNAME, NAME_QNAME, value);


        List<InstanceIdentifier.PathArgument> arguments = new ArrayList<>();

        arguments.add(p1);
        arguments.add(p2);

        InstanceIdentifier expected = InstanceIdentifier.create(arguments);

        NormalizedNodeMessages.InstanceIdentifier instanceIdentifier =
            InstanceIdentifierUtils.toSerializable(expected);

        InstanceIdentifier actual =
            InstanceIdentifierUtils.fromSerializable(instanceIdentifier);


        Assert.assertEquals(expected.getLastPathArgument(),
            actual.getLastPathArgument());
    }

    @Test
    public void testAugmentationIdentifier(){
        InstanceIdentifier.PathArgument p1 =
            new InstanceIdentifier.AugmentationIdentifier(new HashSet(Arrays.asList(TEST_QNAME)));

        List<InstanceIdentifier.PathArgument> arguments = new ArrayList<>();

        arguments.add(p1);

        InstanceIdentifier expected = InstanceIdentifier.create(arguments);

        NormalizedNodeMessages.InstanceIdentifier instanceIdentifier =
            InstanceIdentifierUtils.toSerializable(expected);

        InstanceIdentifier actual =
            InstanceIdentifierUtils.fromSerializable(instanceIdentifier);


        Assert.assertEquals(expected.getLastPathArgument(),
            actual.getLastPathArgument());

    }

}
