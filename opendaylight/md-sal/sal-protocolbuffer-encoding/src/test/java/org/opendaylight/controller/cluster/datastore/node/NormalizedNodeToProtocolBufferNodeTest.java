package org.opendaylight.controller.cluster.datastore.node;


import org.junit.Test;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;

import java.util.Iterator;

/**
 * @author: syedbahm
 * Date: 7/2/14
 */
public class NormalizedNodeToProtocolBufferNodeTest {

  private String instanceIdentifierToString(InstanceIdentifier id){
    Iterable<InstanceIdentifier.PathArgument> iterable = id.getPathArguments();
    Iterator iterator = iterable.iterator();
    String path="";
    while (iterator.hasNext()) {
       path += "/"+iterator.next().toString();

    }
    return path;
  }
  @Test
  public void testNormalizedNodeToNodeSerialization (){
//    NormalizedNode<?,?> nn = NormalizedNodeXmlConverterTest.augmentChoiceExpectedNode();
//    InstanceIdentifier id = new InstanceIdentifier(
//        Lists.newArrayList(NormalizedNodeXmlConverterTest.getNodeIdentifier("container")));
//
//    NormalizedNodeToProtocolBufferNode nnn = new NormalizedNodeToProtocolBufferNode();
//    nnn.encode(instanceIdentifierToString(id), nn);
//    NormalizedNodeMessages.Node node = nnn.getContainer().getNormalizedNode();
//    Assert.assertTrue(node.getChildCount()>0);
  }

  @Test
  public void testNormalizedNodeToNodeSerializationChoiceNode() {
//    QName CH2_QNAME = QName.create("urn:opendaylight:params:xml:ns:yang:controller:test", "2014-03-13", "ch2");
//    NormalizedNode
//        choice = NormalizedNodeXmlConverterTest.augmentChoiceExpectedNode()
//        .getChild(new InstanceIdentifier.NodeIdentifier(CH2_QNAME))
//        .get();
//
//    InstanceIdentifier id =  new InstanceIdentifier(
//        Lists.newArrayList(NormalizedNodeXmlConverterTest.getNodeIdentifier("ch2")));
//
//    NormalizedNodeToProtocolBufferNode nnn = new NormalizedNodeToProtocolBufferNode();
//    nnn.encode(instanceIdentifierToString(id), choice);
//
//    NormalizedNodeMessages.Node node = nnn.getContainer().getNormalizedNode();
//
//    Assert.assertTrue(node.getChildCount()==2);
//
//

  }

}
