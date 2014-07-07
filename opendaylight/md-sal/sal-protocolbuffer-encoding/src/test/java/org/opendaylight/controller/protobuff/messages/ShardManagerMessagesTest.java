package org.opendaylight.controller.protobuff.messages;

/**
 * This test case is present to ensure that if others have used proper version of protocol buffer.
 *
 * If a different version of protocol buffer is used then it would generate different java sources
 * and would result in breaking of this test case.
 *
 * @author: syedbahm Date: 6/20/14
 *
 */

import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.controller.protobuff.messages.shard.ShardManagerMessages;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

public class ShardManagerMessagesTest {

  @Test
  public void verifySerialization() throws Exception {
    ShardManagerMessages.FindPrimary.Builder builder =
        ShardManagerMessages.FindPrimary.newBuilder();
    builder.setShardName("Inventory");
    File testFile = new File("./test");
    FileOutputStream output = new FileOutputStream(testFile);
    builder.build().writeTo(output);
    output.close();

    // Here we will read the same and check we got back what we had saved
    ShardManagerMessages.FindPrimary findPrimary =
        ShardManagerMessages.FindPrimary
            .parseFrom(new FileInputStream(testFile));
    Assert.assertEquals("Inventory", findPrimary.getShardName());

    testFile.delete();

  }
}
