/*
 * Copyright (c) 2014, 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.node;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.cluster.datastore.node.utils.NormalizedNodeGetter;
import org.opendaylight.controller.cluster.datastore.node.utils.NormalizedNodeNavigator;
import org.opendaylight.controller.cluster.datastore.node.utils.PathUtils;
import org.opendaylight.controller.cluster.datastore.node.utils.serialization.NormalizedNodeSerializer;
import org.opendaylight.controller.cluster.datastore.util.TestModel;
import org.opendaylight.controller.protobuff.messages.common.NormalizedNodeMessages.Container;
import org.opendaylight.controller.protobuff.messages.common.NormalizedNodeMessages.Node;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class NormalizedNodeToNodeCodecTest {
  private SchemaContext schemaContext;

  @Before
  public void setUp() {
    schemaContext = TestModel.createTestContext();
    assertNotNull("Schema context must not be null.", schemaContext);
  }

  private static YangInstanceIdentifier instanceIdentifierFromString(String s) {
      return PathUtils.toYangInstanceIdentifier(s);
  }

  @Test
  public void testNormalizeNodeAttributesToProtoBuffNode() {
    final NormalizedNode<?, ?> documentOne = TestModel.createTestContainer();
    String id =
        "/(urn:opendaylight:params:xml:ns:yang:controller:md:sal:dom:store:test?revision=2014-03-13)test"
            + "/(urn:opendaylight:params:xml:ns:yang:controller:md:sal:dom:store:test?revision=2014-03-13)outer-list"
            + "/(urn:opendaylight:params:xml:ns:yang:controller:md:sal:dom:store:test?revision=2014-03-13)outer-list[{(urn:opendaylight:params:xml:ns:yang:controller:md:sal:dom:store:test?revision=2014-03-13)id=2}]"
            + "/(urn:opendaylight:params:xml:ns:yang:controller:md:sal:dom:store:test?revision=2014-03-13)id";

    NormalizedNodeGetter normalizedNodeGetter = new NormalizedNodeGetter(id);
    new NormalizedNodeNavigator(normalizedNodeGetter).navigate(
        PathUtils.toString(YangInstanceIdentifier.builder().build()), documentOne);

    // Validate the value of id can be retrieved from the normalized node
    NormalizedNode<?, ?> output = normalizedNodeGetter.getOutput();
    assertNotNull(output);


    NormalizedNodeToNodeCodec codec =
        new NormalizedNodeToNodeCodec(schemaContext);
    long start = System.currentTimeMillis();
    Container container =
        codec.encode(output);
    long end = System.currentTimeMillis();

    System.out.println("Timetaken to encode :"+(end-start));

    assertNotNull(container);

    // Decode the normalized node from the ProtocolBuffer form
    // first get the node representation of normalized node
    final Node node = container.getNormalizedNode();

    start = System.currentTimeMillis();
    NormalizedNode<?, ?> normalizedNode =
        codec.decode(node);
    end = System.currentTimeMillis();

    System.out.println("Timetaken to decode :"+(end-start));

    assertEquals(normalizedNode.getValue().toString(), output.getValue()
        .toString());
  }

  @Test
  public void testThatANormalizedNodeToProtoBuffNodeEncodeDecode()
      throws Exception {
    final NormalizedNode<?, ?> documentOne = TestModel.createTestContainer();

    final NormalizedNodeToNodeCodec normalizedNodeToNodeCodec =
        new NormalizedNodeToNodeCodec(schemaContext);

    Container container =
        normalizedNodeToNodeCodec.encode(documentOne);


    final NormalizedNode<?, ?> decode =
        normalizedNodeToNodeCodec
            .decode(
                container.getNormalizedNode());
    assertNotNull(decode);

    // let us ensure that the return decode normalized node encode returns same container
    Container containerResult =
        normalizedNodeToNodeCodec.encode(decode);

    // check first level children are proper
    List<Node> childrenResult =
        containerResult.getNormalizedNode().getChildList();
    List<Node> childrenOriginal = container.getNormalizedNode().getChildList();

    System.out.println("-------------------------------------------------");

    System.out.println(childrenOriginal.toString());

    System.out.println("-------------------------------------------------");

    System.out.println(childrenResult.toString());

    boolean bFound;
    for (Node resultChild : childrenResult) {
      bFound = false;
      for (Node originalChild : childrenOriginal) {

        YangInstanceIdentifier.PathArgument result = NormalizedNodeSerializer.deSerialize(
              containerResult.getNormalizedNode(),
              resultChild.getPathArgument());

          YangInstanceIdentifier.PathArgument original = NormalizedNodeSerializer.deSerialize(
              container.getNormalizedNode(),
              originalChild.getPathArgument());

        if (original.equals(result)
            && resultChild.getIntType() == resultChild.getIntType()) {
          bFound = true;
          break;
        }
      }
      assertTrue(bFound);
    }

  }

  @Test
  public void addAugmentations() {
    String stringId =
        "/(urn:opendaylight:params:xml:ns:yang:controller:md:sal:dom:store:test?revision=2014-03-13)test"
            + "/(urn:opendaylight:params:xml:ns:yang:controller:md:sal:dom:store:test?revision=2014-03-13)augmented-list"
            + "/(urn:opendaylight:params:xml:ns:yang:controller:md:sal:dom:store:test?revision=2014-03-13)augmented-list[{(urn:opendaylight:params:xml:ns:yang:controller:md:sal:dom:store:test?revision=2014-03-13)id=1}]";

    YangInstanceIdentifier identifier = instanceIdentifierFromString(stringId);

    MapEntryNode uno = TestModel.createAugmentedListEntry(1, "Uno");

    NormalizedNodeToNodeCodec codec =
        new NormalizedNodeToNodeCodec(schemaContext);

    Container encode = codec.encode(uno);

    System.out.println(encode.getNormalizedNode());

    codec.decode(encode.getNormalizedNode());
  }

}
