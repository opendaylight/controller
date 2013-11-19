package org.opendaylight.controller.sal.connector.remoterpc;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.sal.connector.remoterpc.dto.MessageWrapper;
import org.opendaylight.controller.sal.connector.remoterpc.dto.Message;

import java.util.concurrent.TimeoutException;

public class ClientTest {

  @Before
  public void setup(){
    Client.requestQueue().clear();
  }

  @Test
  public void testStop() throws Exception {

  }

  @Test
  public void testPool() throws Exception {

  }

  @Test
  public void process_AddAMessage_ShouldAddToQueue() throws Exception {
    Client.process(getEmptyMessageWrapper());
    Assert.assertEquals(1, Client.requestQueue().size());
  }

  /**
   * Queue size is 100. Adding 101 message should time out in 2 sec
   * if server does not process it
   * @throws Exception
   */
  @Test(expected = TimeoutException.class)
  public void process_Add101Message_ShouldThrow() throws Exception {
    for (int i=0;i<101;i++){
      Client.process(getEmptyMessageWrapper());
    }
  }

  @Test
  public void testStart() throws Exception {
  }

  private MessageWrapper getEmptyMessageWrapper(){
    return new MessageWrapper(new Message(), null);
  }
}
