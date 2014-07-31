package org.opendaylight.controller.remote.rpc.registry;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.PoisonPill;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.japi.Pair;
import akka.testkit.JavaTestKit;
import com.typesafe.config.ConfigFactory;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opendaylight.controller.remote.rpc.RouteIdentifierImpl;
import org.opendaylight.controller.sal.connector.api.RpcRouter;
import org.opendaylight.yangtools.yang.common.QName;
import scala.concurrent.duration.Duration;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.opendaylight.controller.remote.rpc.registry.RpcRegistry.Messages.AddOrUpdateRoute;
import static org.opendaylight.controller.remote.rpc.registry.RpcRegistry.Messages.FindRouters;
import static org.opendaylight.controller.remote.rpc.registry.RpcRegistry.Messages.FindRoutersReply;
import static org.opendaylight.controller.remote.rpc.registry.RpcRegistry.Messages.SetLocalRouter;

public class RpcRegistryTest {

    /**
     * Goals:
     * 1. Test a 2 node system
     * 2. Publish rpc on node 1 and find it on node 2
     * 2. Publish rpc on node 2 and find it on node 1
     *
     */

    private static ActorSystem node1;
    private static ActorSystem node2;

    private ActorRef registry1;
    private ActorRef registry2;

    @BeforeClass
    public static void setup() throws InterruptedException {
        Thread.sleep(1000); //give some time for previous test to close netty ports
        node1 = ActorSystem.create("opendaylight-rpc", ConfigFactory.load().getConfig("memberA"));
        node2 = ActorSystem.create("opendaylight-rpc", ConfigFactory.load().getConfig("memberB"));
    }

    @AfterClass
    public static void teardown(){
        JavaTestKit.shutdownActorSystem(node1);
        if (node1 != null)
            node1.shutdown();
        if (node2 != null)
            node2.shutdown();
    }

    @Before
    public void createRpcRegistry() throws InterruptedException {
        Thread.sleep(1000); //give some time for actor system init.
        registry1 = node1.actorOf(Props.create(RpcRegistry.class), "registry");
        registry2 = node2.actorOf(Props.create(RpcRegistry.class), "registry");
        Thread.sleep(1000); //give some time for actor creation.
    }

    //@Test
    public void testWhenRpcAddedOnNode1ShouldAppearOnNode2() throws URISyntaxException, InterruptedException {
        new JavaTestKit(node1) {
            {
                Thread.sleep(5000);//let system warm up

                //final JavaTestKit probe1 = new JavaTestKit(node1);

                registry1.tell(new SetLocalRouter(getRef()), getRef());
                registry1.tell(getAddRouteMessage(), getRef());
                //find the route in registry
                registry1.tell(new FindRouters(createRouteId()), getRef());

                expectMsgClass(JavaTestKit.duration("10 second"), FindRoutersReply.class);

                FindRoutersReply message = (FindRoutersReply) receiveOne(Duration.create(10, TimeUnit.SECONDS));
                Thread.sleep(5000);
                System.out.println("**********Got routers :");
                System.out.println("**********Got routers :" + message);
                System.out.println("**********Got routers :");
                //System.out.println("**********Last sender :" + getLastSender());
                //Gossiper would start after initial delay of 1 sec. So wait for a few seconds before checking
//                final String out = (String) new ExpectMsg("FindRoutersReply") {
//                    protected String match(Object in) {
//                        if (in instanceof FindRoutersReply)
//                            return "match";
//                        else
//                            throw noMatch();
//                    }
//                }.get(); // this extracts the received message
//
//                System.out.println("**********Matched? :" + out);
                Thread.sleep(5000);

                expectNoMsg();
                registry1.tell(PoisonPill.getInstance(), ActorRef.noSender());
                registry2.tell(PoisonPill.getInstance(), ActorRef.noSender());
            }};
    }

    @Test
    public void testRegistry() throws Exception {
        ActorRef printer = node1.actorOf(Props.create(Printer.class), "printer");

        Thread.sleep(5000);//let system warm up

        //final JavaTestKit probe1 = new JavaTestKit(node1);

        registry1.tell(new SetLocalRouter(printer), printer);
        registry1.tell(getAddRouteMessage(), printer);
        Thread.sleep(15000);
        //find the route in registry
        //registry1.tell(new FindRouters(createRouteId()), printer);
        //Thread.sleep(5000);
        registry2.tell(new FindRouters(createRouteId()), printer);
        Thread.sleep(5000);
        printer.tell(PoisonPill.getInstance(), ActorRef.noSender());
    }
    private AddOrUpdateRoute getAddRouteMessage() throws URISyntaxException {
        return new AddOrUpdateRoute(createRouteId());
    }

    private RpcRouter.RouteIdentifier<?,?,?> createRouteId() throws URISyntaxException {
        QName type = new QName(new URI("/mockrpc"), "mockrpc");
        return new RouteIdentifierImpl(null, type, null);
    }

    public static class Printer extends UntypedActor{
        @Override
        public void onReceive(Object message) throws Exception {
            System.out.println("*************************************");
            System.out.println("Printer received message: " + message);
            System.out.println("*************************************");

            if (message instanceof FindRoutersReply){
                FindRoutersReply reply = (FindRoutersReply) message;
                List<Pair<ActorRef, Long>> routers =  reply.getRouterWithUpdateTime();
                System.out.println("Routers : " + routers);

            }
        }
    }
}