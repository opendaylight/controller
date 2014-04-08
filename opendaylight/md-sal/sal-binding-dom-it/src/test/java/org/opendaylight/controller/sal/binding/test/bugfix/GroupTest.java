/**
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.binding.test.bugfix;

import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Date;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.apply.actions._case.ApplyActionsBuilder;
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
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.SimpleNode;
import org.opendaylight.yangtools.yang.data.impl.ImmutableCompositeNode;
import org.opendaylight.yangtools.yang.data.impl.NodeFactory;
import org.opendaylight.yangtools.yang.data.impl.util.CompositeNodeBuilder;

public class GroupTest extends AbstractDataServiceTest {

	private final static Logger log = Logger.getLogger(GroupTest.class.getName());

    private static final Long GROUP_ID = (long) 130;    
    
    private static final URI uri = createUri("urn:opendaylight:inventory");
   
    private static final Date date = Date.valueOf("2013-10-24");
    
    private static final QName NODE_ID_QNAME = new QName(uri, date, "id");
    private static final QName GROUP_ID_QNAME = QName.create(Group.QNAME, "id");
    
    private static final String NODE_ID = "node:1";
    
    private static final NodeKey NODE_KEY = new NodeKey(new NodeId(NODE_ID));
    
    private static final InstanceIdentifier<Node> NODE_INSTANCE_ID_BA = InstanceIdentifier.builder(Nodes.class) //
            .child(Node.class, NODE_KEY).toInstance();
    
    private static final NodeRef NODE_REF = new NodeRef(NODE_INSTANCE_ID_BA); 
    
    private static final GroupKey GROUP_KEY = new GroupKey(GROUP_ID, NODE_REF);
    
    private static final InstanceIdentifier<Group> GROUP_INSTANCE_BA = InstanceIdentifier.builder(Groups.class) //
            .child(Group.class, GROUP_KEY).toInstance();
    private static final org.opendaylight.yangtools.yang.data.api.InstanceIdentifier NODE_INSTANCE_BI = org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.builder()
    		.nodeWithKey(new QName(createUri("urn:opendaylight:inventory"), date, "node"), NODE_ID_QNAME , NODE_ID).toInstance();
    private static final org.opendaylight.yangtools.yang.data.api.InstanceIdentifier GROUP_INSTANCE_BI = org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.builder(NODE_INSTANCE_BI)
    		.nodeWithKey(Group.QNAME, GROUP_ID_QNAME, GROUP_ID).toInstance();
    
    /**
     * crud test for group
     * @throws InterruptedException
     * @throws ExecutionException
     */
    @Test
    public void testForGroup() throws InterruptedException, ExecutionException{
    	Group group = createGroup();
    	Group groupUp = createGroupUp();
    	
    	DataObject createdGroup = CrudTestUtil.doCreateTest(group, baDataService, GROUP_INSTANCE_BA);
        CrudTestUtil.doReadTest(group, baDataService, GROUP_INSTANCE_BA);
        CrudTestUtil.doUpdateTest(groupUp, createdGroup, baDataService, GROUP_INSTANCE_BA);
        CrudTestUtil.doRemoveTest(groupUp, baDataService, GROUP_INSTANCE_BA);
        
        log.info("Test CRUD done for : " + group + " and was update to : " + groupUp);
        
       // CompositeNode groupBI = createGroupBI();
        
       // CompositeNode  createdGroupBI = CrudTestUtil.doCreateTest(groupBI, biDataService, GROUP_INSTANCE_BI);
        
        
    }
    
    private Group createGroupUp() {
    	BucketsBuilder buckets = new BucketsBuilder();
		BucketBuilder bucket = new BucketBuilder();
		
		bucket.setBucketId(new BucketId((long)12));
		
		ApplyActionsBuilder applyActions = new ApplyActionsBuilder();
        
        List<Action> actionList = new ArrayList<>();
		
		ActionBuilder action = new ActionBuilder();
        
		PopMplsActionBuilder popMplsAction = new PopMplsActionBuilder();
        popMplsAction.setEthernetType(34);
        actionList.add(new ActionBuilder().setAction(new PopMplsActionCaseBuilder().setPopMplsAction(popMplsAction.build()).build()).setOrder(10).build());
        
        bucket.setAction(actionList);
        
        List<Bucket> bucketList = Collections.<Bucket>singletonList(bucket.build());
        
        buckets.setBucket(bucketList);
		
		
    	return new GroupBuilder()
					.setKey(GROUP_KEY)
					.setGroupName("FOO")
					.setBarrier(false)
					.setBuckets(buckets.build())
				.build();
	}

	/**
     * create uri
     * @param uri
     * @return
     */
    private static URI createUri(String uri) {
			
		try {
			return new URI(uri);
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * BA
	 * create group
	 * @return
	 */
	private Group createGroup(){
		BucketsBuilder buckets = new BucketsBuilder();
		BucketBuilder bucket = new BucketBuilder();
		
		bucket.setBucketId(new BucketId((long)12));
		
		ApplyActionsBuilder applyActions = new ApplyActionsBuilder();
        
        List<Action> actionList = new ArrayList<>();
		
		ActionBuilder action = new ActionBuilder();
        
        DecNwTtlBuilder decNwTtl = new DecNwTtlBuilder();
        actionList.add(new ActionBuilder().setAction(new DecNwTtlCaseBuilder().setDecNwTtl(decNwTtl.build()).build()).setOrder(0).build());
        
        bucket.setAction(actionList);
        
        List<Bucket> bucketList = Collections.<Bucket>singletonList(bucket.build());
        
        buckets.setBucket(bucketList);
		
		
    	return new GroupBuilder()
					.setKey(GROUP_KEY)
					.setGroupName("FOO")
					.setBarrier(false)
					.setBuckets(buckets.build())
				.build();
    }
    
    /**
     * BI
     * CompositeNode for group
     * @return
     */
    private CompositeNode createGroupBI(){    			
    	SimpleNode simpleNode = NodeFactory.createImmutableSimpleNode(new QName(Group.QNAME, "group"), null, "id");
    	
    	
    	CompositeNodeBuilder<ImmutableCompositeNode> node = ImmutableCompositeNode.builder();
        node.setQName(new QName(createUri("urn:opendaylight:inventory"), date, "node"));
        
		return node.add(simpleNode).toInstance();
    }
}
