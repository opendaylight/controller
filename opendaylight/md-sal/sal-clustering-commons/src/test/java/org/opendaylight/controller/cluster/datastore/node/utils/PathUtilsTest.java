/*
 * Copyright (c) 2014, 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.node.utils;

import com.google.common.collect.ImmutableSet;
import org.junit.Test;
import org.opendaylight.controller.cluster.datastore.util.TestModel;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import static junit.framework.TestCase.assertEquals;

public class PathUtilsTest {

    @Test
    public void toStringNodeIdentifier(){
        YangInstanceIdentifier.PathArgument pathArgument = nodeIdentifier();

        String expected = "(urn:opendaylight:params:xml:ns:yang:controller:md:sal:dom:store:test?revision=2014-03-13)test";

        assertEquals(expected , PathUtils.toString(pathArgument));
    }

    @Test
    public void toStringAugmentationIdentifier(){
        String expected = "AugmentationIdentifier{childNames=[(urn:opendaylight:flow:table:statistics?revision=2013-12-15)flow-table-statistics]}";

        YangInstanceIdentifier.PathArgument pathArgument = augmentationIdentifier();

        assertEquals(expected, PathUtils.toString(pathArgument));
    }

    @Test
    public void toStringNodeWithValue(){

        YangInstanceIdentifier.PathArgument pathArgument = nodeWithValue();

        String expected = "(urn:opendaylight:params:xml:ns:yang:controller:md:sal:dom:store:test?revision=2014-03-13)test[100]";

        assertEquals(expected, PathUtils.toString(pathArgument));
    }


    @Test
    public void toStringNodeIdentifierWithPredicates(){

        YangInstanceIdentifier.PathArgument pathArgument = nodeIdentifierWithPredicates();

        String expected = "(urn:opendaylight:params:xml:ns:yang:controller:md:sal:dom:store:test?revision=2014-03-13)test[{(urn:opendaylight:params:xml:ns:yang:controller:md:sal:dom:store:test?revision=2014-03-13)id=100}]";

        assertEquals(expected, PathUtils.toString(pathArgument));
    }

    @Test
    public void toStringYangInstanceIdentifier(){

        YangInstanceIdentifier path =
            YangInstanceIdentifier.create(nodeIdentifier())
                .node(nodeIdentifierWithPredicates())
                .node(augmentationIdentifier()).node(nodeWithValue());


        String expected = "(urn:opendaylight:params:xml:ns:yang:controller:md:sal:dom:store:test?revision=2014-03-13)test/" +
            "(urn:opendaylight:params:xml:ns:yang:controller:md:sal:dom:store:test?revision=2014-03-13)test[{(urn:opendaylight:params:xml:ns:yang:controller:md:sal:dom:store:test?revision=2014-03-13)id=100}]/" +
            "AugmentationIdentifier{childNames=[(urn:opendaylight:flow:table:statistics?revision=2013-12-15)flow-table-statistics]}/" +
            "(urn:opendaylight:params:xml:ns:yang:controller:md:sal:dom:store:test?revision=2014-03-13)test[100]";

        assertEquals(expected, PathUtils.toString(path));

    }

    @Test
    public void toYangInstanceIdentifier(){
        String expected = "(urn:opendaylight:params:xml:ns:yang:controller:md:sal:dom:store:test?revision=2014-03-13)test/" +
            "(urn:opendaylight:params:xml:ns:yang:controller:md:sal:dom:store:test?revision=2014-03-13)test[{(urn:opendaylight:params:xml:ns:yang:controller:md:sal:dom:store:test?revision=2014-03-13)id=100}]/" +
            "AugmentationIdentifier{childNames=[(urn:opendaylight:flow:table:statistics?revision=2013-12-15)flow-table-statistics]}/" +
            "(urn:opendaylight:params:xml:ns:yang:controller:md:sal:dom:store:test?revision=2014-03-13)test[100]";

        YangInstanceIdentifier yangInstanceIdentifier =
            PathUtils.toYangInstanceIdentifier(expected);

        String actual = PathUtils.toString(yangInstanceIdentifier);

        assertEquals(expected, actual);

    }

    private YangInstanceIdentifier.NodeIdentifier nodeIdentifier(){
        return new YangInstanceIdentifier.NodeIdentifier(TestModel.TEST_QNAME);
    }

    private YangInstanceIdentifier.AugmentationIdentifier augmentationIdentifier(){
        Set<QName> childNames = ImmutableSet.of(QNameFactory.create("(urn:opendaylight:flow:table:statistics?revision=2013-12-15)flow-table-statistics"));

        return new YangInstanceIdentifier.AugmentationIdentifier(childNames);
    }

    private YangInstanceIdentifier.NodeWithValue nodeWithValue(){
        return new YangInstanceIdentifier.NodeWithValue(TestModel.TEST_QNAME, Integer.valueOf(100));
    }

    private YangInstanceIdentifier.NodeIdentifierWithPredicates nodeIdentifierWithPredicates(){
        Map<QName, Object> keys = new HashMap<>();

        keys.put(TestModel.ID_QNAME, Integer.valueOf(100));

        return new YangInstanceIdentifier.NodeIdentifierWithPredicates(TestModel.TEST_QNAME, keys);
    }
}
