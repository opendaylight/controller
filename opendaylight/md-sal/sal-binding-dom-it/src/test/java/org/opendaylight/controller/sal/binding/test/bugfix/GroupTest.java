/**
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.binding.test.bugfix;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;

import org.junit.Test;
import org.opendaylight.controller.sal.binding.test.AbstractDataServiceTest;
import org.opendaylight.controller.sal.binding.test.connect.dom.CrudTestUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.DecNwTtlCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.PopMplsActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.dec.nw.ttl._case.DecNwTtlBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.pop.mpls.action._case.PopMplsActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.config.rev131024.Groups;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.config.rev131024.groups.Group;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.config.rev131024.groups.GroupBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.config.rev131024.groups.GroupKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.BucketId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.group.BucketsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.group.buckets.Bucket;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.group.buckets.BucketBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class GroupTest extends AbstractDataServiceTest{

    private final static Logger log = Logger.getLogger(GroupTest.class
            .getName());

    private static final Long GROUP_ID = (long) 130;

    private static final String NODE_ID = "node:1";

    private static final NodeKey NODE_KEY = new NodeKey(new NodeId(NODE_ID));

    private static final InstanceIdentifier<Node> NODE_INSTANCE_ID_BA = InstanceIdentifier
            .builder(Nodes.class)
            //
            .child(Node.class, NODE_KEY).toInstance();

    private static final NodeRef NODE_REF = new NodeRef(NODE_INSTANCE_ID_BA);

    private static final GroupKey GROUP_KEY = new GroupKey(GROUP_ID, NODE_REF);

    private static final InstanceIdentifier<Group> GROUP_INSTANCE_BA = InstanceIdentifier
            .builder(Groups.class)
            //
            .child(Group.class, GROUP_KEY).toInstance();

    /**
     * crud test for group
     * 
     * @throws InterruptedException
     * @throws ExecutionException
     */
    @Test
    public void testForGroup() throws InterruptedException, ExecutionException{
        Group group = this.createGroup();
        Group groupUp = this.createGroupUp();

        DataObject createdGroup = CrudTestUtil.doCreateTest(group,
                this.baDataService, GROUP_INSTANCE_BA);
        CrudTestUtil.doReadTest(group, this.baDataService, GROUP_INSTANCE_BA);
        CrudTestUtil.doUpdateTest(groupUp, createdGroup, this.baDataService,
                GROUP_INSTANCE_BA);
        CrudTestUtil.doRemoveTest(groupUp, this.baDataService,
                GROUP_INSTANCE_BA);

        log.info("Test CRUD done for : " + group + " and was update to : "
                + groupUp);

    }

    /**
     * BA create update group
     * 
     * @return group
     */
    private Group createGroupUp(){
        BucketsBuilder buckets = new BucketsBuilder();
        BucketBuilder bucket = new BucketBuilder();

        bucket.setBucketId(new BucketId((long) 12));

        List<Action> actionList = new ArrayList<>();

        PopMplsActionBuilder popMplsAction = new PopMplsActionBuilder();
        popMplsAction.setEthernetType(34);
        actionList.add(new ActionBuilder()
                .setAction(
                        new PopMplsActionCaseBuilder().setPopMplsAction(
                                popMplsAction.build()).build()).setOrder(10)
                .build());

        bucket.setAction(actionList);

        List<Bucket> bucketList = Collections.<Bucket> singletonList(bucket
                .build());

        buckets.setBucket(bucketList);

        return new GroupBuilder().setKey(GROUP_KEY).setGroupName("FOO")
                .setBarrier(false).setBuckets(buckets.build()).build();
    }

    /**
     * BA create group
     * 
     * @return group
     */
    private Group createGroup(){
        BucketsBuilder buckets = new BucketsBuilder();
        BucketBuilder bucket = new BucketBuilder();

        bucket.setBucketId(new BucketId((long) 12));

        List<Action> actionList = new ArrayList<>();

        DecNwTtlBuilder decNwTtl = new DecNwTtlBuilder();
        actionList.add(new ActionBuilder()
                .setAction(
                        new DecNwTtlCaseBuilder().setDecNwTtl(decNwTtl.build())
                                .build()).setOrder(0).build());

        bucket.setAction(actionList);

        List<Bucket> bucketList = Collections.<Bucket> singletonList(bucket
                .build());

        buckets.setBucket(bucketList);

        return new GroupBuilder().setKey(GROUP_KEY).setGroupName("FOO")
                .setBarrier(false).setBuckets(buckets.build()).build();
    }
}
