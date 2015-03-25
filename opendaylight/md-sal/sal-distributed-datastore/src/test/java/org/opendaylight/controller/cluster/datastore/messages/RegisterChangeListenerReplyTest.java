package org.opendaylight.controller.cluster.datastore.messages;

import static junit.framework.TestCase.assertEquals;
import akka.actor.Actor;
import akka.serialization.Serialization;
import akka.testkit.TestActorRef;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.cluster.datastore.AbstractActorTest;
import org.opendaylight.controller.cluster.raft.TestActorFactory;
import org.opendaylight.controller.cluster.raft.utils.MessageCollectorActor;
import org.opendaylight.controller.protobuff.messages.registration.ListenerRegistrationMessages;

public class RegisterChangeListenerReplyTest extends AbstractActorTest {

    private TestActorFactory factory;


    @Before
    public void setUp(){
        factory = new TestActorFactory(getSystem());
    }

    @After
    public void shutDown(){
        factory.close();
    }

    @Test
    public void testToSerializable(){
        TestActorRef<Actor> testActor = factory.createTestActor(MessageCollectorActor.props());

        RegisterChangeListenerReply registerChangeListenerReply = new RegisterChangeListenerReply(testActor);

        ListenerRegistrationMessages.RegisterChangeListenerReply serialized
                = registerChangeListenerReply.toSerializable();

        assertEquals(Serialization.serializedActorPath(testActor), serialized.getListenerRegistrationPath());
    }

    @Test
    public void testFromSerializable(){
        TestActorRef<Actor> testActor = factory.createTestActor(MessageCollectorActor.props());

        RegisterChangeListenerReply registerChangeListenerReply = new RegisterChangeListenerReply(testActor);

        ListenerRegistrationMessages.RegisterChangeListenerReply serialized
                = registerChangeListenerReply.toSerializable();


        RegisterChangeListenerReply fromSerialized
                = RegisterChangeListenerReply.fromSerializable(getSystem(), serialized);

        assertEquals(testActor.path().toString(), fromSerialized.getListenerRegistrationPath().toString());
    }

}