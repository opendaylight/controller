package org.opendaylight.controller.cluster.datastore;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.testkit.JavaTestKit;
import com.google.common.base.Optional;
import com.google.common.util.concurrent.ListenableFuture;
import junit.framework.Assert;
import org.junit.Test;
import org.opendaylight.controller.cluster.datastore.messages.CreateTransactionReply;
import org.opendaylight.controller.cluster.datastore.messages.ReadDataReply;
import org.opendaylight.controller.cluster.datastore.utils.DoNothingActor;
import org.opendaylight.controller.cluster.datastore.utils.MockActorContext;
import org.opendaylight.controller.md.cluster.datastore.model.TestModel;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;

public class TransactionProxyTest extends AbstractActorTest {



    @Test
    public void testRead() throws Exception {

        new JavaTestKit(getSystem()) {{
            final Props props = Props.create(DoNothingActor.class);
            final ActorRef actorRef = getSystem().actorOf(props);

            final MockActorContext actorContext = new MockActorContext(this.getSystem());
            actorContext.setExecuteShardOperationResponse(new CreateTransactionReply(actorRef.path()));
            actorContext.setExecuteRemoteOperationResponse("message");

            TransactionProxy transactionProxy =
                new TransactionProxy(actorContext,
                    TransactionProxy.TransactionType.READ_ONLY);


            ListenableFuture<Optional<NormalizedNode<?, ?>>> read =
                transactionProxy.read(TestModel.TEST_PATH);

            Optional<NormalizedNode<?, ?>> normalizedNodeOptional = read.get();

            Assert.assertFalse(normalizedNodeOptional.isPresent());

            actorContext.setExecuteRemoteOperationResponse(new ReadDataReply(
                ImmutableNodes.containerNode(TestModel.TEST_QNAME)));

            read = transactionProxy.read(TestModel.TEST_PATH);

            normalizedNodeOptional = read.get();

            Assert.assertTrue(normalizedNodeOptional.isPresent());

        }};
    }
}
