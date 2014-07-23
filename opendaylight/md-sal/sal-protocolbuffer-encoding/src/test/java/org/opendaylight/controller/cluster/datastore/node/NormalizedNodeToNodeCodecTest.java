/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.node;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.cluster.datastore.node.utils.NodeIdentifierFactory;
import org.opendaylight.controller.cluster.datastore.node.utils.NormalizedNodeGetter;
import org.opendaylight.controller.cluster.datastore.node.utils.NormalizedNodeNavigator;
import org.opendaylight.controller.cluster.datastore.util.TestModel;
import org.opendaylight.controller.protobuff.messages.common.NormalizedNodeMessages.Container;
import org.opendaylight.controller.protobuff.messages.common.NormalizedNodeMessages.Node;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

import java.util.ArrayList;
import java.util.List;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;

public class NormalizedNodeToNodeCodecTest {



    private SchemaContext schemaContext;

    @Before
    public void setUp(){
        schemaContext = TestModel.createTestContext();
        assertNotNull("Schema context must not be null.", schemaContext);
    }

    private InstanceIdentifier instanceIdentifierFromString(String s){

        String[] ids = s.split("/");

        List<InstanceIdentifier.PathArgument> pathArguments = new ArrayList<>();
        for(String nodeId : ids){
            if(!"".equals(nodeId)) {
                pathArguments.add(NodeIdentifierFactory.getArgument(nodeId));
            }
        }
        final InstanceIdentifier instanceIdentifier = InstanceIdentifier.create(pathArguments);
        return instanceIdentifier;
    }


    @Test
    public void testNormalizeNodeAttributesToProtoBuffNode(){
        final NormalizedNode<?, ?> documentOne = TestModel.createTestContainer();
        String id = "/(urn:opendaylight:params:xml:ns:yang:controller:md:sal:dom:store:test?revision=2014-03-13)test" +
            "/(urn:opendaylight:params:xml:ns:yang:controller:md:sal:dom:store:test?revision=2014-03-13)outer-list" +
            "/(urn:opendaylight:params:xml:ns:yang:controller:md:sal:dom:store:test?revision=2014-03-13)outer-list[{(urn:opendaylight:params:xml:ns:yang:controller:md:sal:dom:store:test?revision=2014-03-13)id=2}]" +
            "/(urn:opendaylight:params:xml:ns:yang:controller:md:sal:dom:store:test?revision=2014-03-13)id";

        NormalizedNodeGetter normalizedNodeGetter = new NormalizedNodeGetter(id);
        new NormalizedNodeNavigator(normalizedNodeGetter).navigate(
            InstanceIdentifier.builder().build().toString(), documentOne);

        // Validate the value of id can be retrieved from the normalized node
        NormalizedNode output = normalizedNodeGetter.getOutput();
        assertNotNull(output);


      NormalizedNodeToNodeCodec codec = new NormalizedNodeToNodeCodec(schemaContext);
      Container container = codec.encode(instanceIdentifierFromString(id),output);

       assertNotNull(container);
       assertEquals(id, container.getParentPath()+"/"+container.getNormalizedNode().getPath()) ;

      // Decode the normalized node from the ProtocolBuffer form
      //first get the node representation of normalized node
      final Node node = container.getNormalizedNode();

      NormalizedNode<?,?> normalizedNode = codec.decode(instanceIdentifierFromString(id),node);

      assertEquals(normalizedNode.getValue().toString(),output.getValue().toString());
    }

    @Test
    public void testThatANormalizedNodeToProtoBuffNodeEncodeDecode() throws Exception {
        final NormalizedNode<?, ?> documentOne = TestModel.createTestContainer();

        final NormalizedNodeToNodeCodec normalizedNodeToNodeCodec = new NormalizedNodeToNodeCodec(schemaContext);

        Container container = normalizedNodeToNodeCodec.encode(InstanceIdentifier.builder().build(), documentOne);


        final NormalizedNode<?, ?> decode = normalizedNodeToNodeCodec.decode(instanceIdentifierFromString("/(urn:opendaylight:params:xml:ns:yang:controller:md:sal:dom:store:test?revision=2014-03-13)test"),container.getNormalizedNode());
        assertNotNull(decode != null);

        //let us ensure that the return decode normalized node encode returns same container
        Container containerResult =  normalizedNodeToNodeCodec.encode(InstanceIdentifier.builder().build(), decode);

        assertEquals(container.getParentPath(),containerResult.getParentPath());
        assertEquals(container.getNormalizedNode().getChildCount(),container.getNormalizedNode().getChildCount());

        Assert.assertEquals(containerResult.getNormalizedNode().getChildCount(),container.getNormalizedNode().getChildCount());

        //check first level children are proper
        List<Node>childrenResult = containerResult.getNormalizedNode().getChildList();
        List<Node>childrenOriginal = container.getNormalizedNode().getChildList();

        System.out.println("-------------------------------------------------");

        System.out.println(childrenOriginal.toString());

        System.out.println("-------------------------------------------------");

        System.out.println(childrenResult.toString());

       boolean bFound;
        for(Node resultChild: childrenResult){
           bFound = false;
          for(Node originalChild:childrenOriginal){
            if(originalChild.getPath().equals(resultChild.getPath())
                && resultChild.getType().equals(resultChild.getType())){
               bFound=true;
               break;
            }
          }
          Assert.assertTrue(bFound);
        }

    }

    @Test
    public void addAugmentations(){
        String stringId = "/(urn:opendaylight:params:xml:ns:yang:controller:md:sal:dom:store:test?revision=2014-03-13)test" +
            "/(urn:opendaylight:params:xml:ns:yang:controller:md:sal:dom:store:test?revision=2014-03-13)augmented-list" +
            "/(urn:opendaylight:params:xml:ns:yang:controller:md:sal:dom:store:test?revision=2014-03-13)augmented-list[{(urn:opendaylight:params:xml:ns:yang:controller:md:sal:dom:store:test?revision=2014-03-13)id=1}]";

        InstanceIdentifier identifier = instanceIdentifierFromString(stringId);

        MapEntryNode uno = TestModel.createAugmentedListEntry(1, "Uno");

        NormalizedNodeToNodeCodec codec =
            new NormalizedNodeToNodeCodec(schemaContext);

        Container encode = codec
            .encode(identifier, uno);

        System.out.println(encode.getNormalizedNode());

        codec.decode(identifier, encode.getNormalizedNode());
    }

}
