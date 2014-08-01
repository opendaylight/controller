package org.opendaylight.controller.protobuff.messages.shard;

/**
 * This test case is present to ensure that if others have used proper version of protocol buffer
 * for the ShardManager.proto messages
 *
 * If a different version of protocol buffer and there is change in serializaiton format
 * this test would break as we are comparing with protocol buffer 2.5 generated
 * serialized data.
 *
 * @author: syedbahm
 *
 */

import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.controller.protobuff.messages.AbstractMessagesTest;

public class ShardManagerMessagesTest extends AbstractMessagesTest {

  @Test
  public void verifySerialization() throws Exception {
    ShardManagerMessages.FindPrimary.Builder builder =
        ShardManagerMessages.FindPrimary.newBuilder();
    builder.setShardName("Inventory");

    writeToFile((com.google.protobuf.GeneratedMessage.Builder<?>) builder);


    // Here we will read the same and check we got back what we had saved
    ShardManagerMessages.FindPrimary findPrimaryNew =
        (ShardManagerMessages.FindPrimary) readFromFile(ShardManagerMessages.FindPrimary.PARSER);

    Assert.assertEquals("Inventory", findPrimaryNew.getShardName());

    // Here we compare with the version we had shipped to catch any protobuff compiler version
    // changes
    ShardManagerMessages.FindPrimary findPrimaryOriginal =
        (ShardManagerMessages.FindPrimary) readFromTestDataFile(ShardManagerMessages.FindPrimary.PARSER);

    Assert.assertEquals(findPrimaryNew.getShardName(),
        findPrimaryOriginal.getShardName());

  }

  @Override
  public String getTestFileName() {
    return ShardManagerMessagesTest.class.getSimpleName();
  }

}
