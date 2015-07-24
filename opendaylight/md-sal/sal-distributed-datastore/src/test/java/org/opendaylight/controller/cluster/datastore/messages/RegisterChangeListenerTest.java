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
import org.opendaylight.controller.md.cluster.datastore.model.TestModel;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker;
import org.opendaylight.controller.protobuff.messages.common.NormalizedNodeMessages;
import org.opendaylight.controller.protobuff.messages.registration.ListenerRegistrationMessages;

public class RegisterChangeListenerTest extends AbstractActorTest {

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
        RegisterChangeListener registerChangeListener = new RegisterChangeListener(TestModel.TEST_PATH, testActor
                , AsyncDataBroker.DataChangeScope.BASE, false);

        ListenerRegistrationMessages.RegisterChangeListener serialized
                = registerChangeListener.toSerializable();

        NormalizedNodeMessages.InstanceIdentifier path = serialized.getInstanceIdentifierPath();

        assertEquals("urn:opendaylight:params:xml:ns:yang:controller:md:sal:dom:store:test", path.getCode(0));
        assertEquals(Serialization.serializedActorPath(testActor), serialized.getDataChangeListenerActorPath());
        assertEquals(AsyncDataBroker.DataChangeScope.BASE.ordinal(), serialized.getDataChangeScope());
        assertEquals(false, serialized.getRegisterOnAllInstances());

    }

    @Test
    public void testFromSerializable(){
        TestActorRef<Actor> testActor = factory.createTestActor(MessageCollectorActor.props());
        RegisterChangeListener registerChangeListener = new RegisterChangeListener(TestModel.TEST_PATH, testActor
                , AsyncDataBroker.DataChangeScope.SUBTREE, true);

        ListenerRegistrationMessages.RegisterChangeListener serialized
                = registerChangeListener.toSerializable();


        RegisterChangeListener fromSerialized = RegisterChangeListener.fromSerializable(getSystem(), serialized);

        assertEquals(TestModel.TEST_PATH, registerChangeListener.getPath());
        assertEquals(testActor.path().toString(), fromSerialized.getDataChangeListenerPath().toString());
        assertEquals(AsyncDataBroker.DataChangeScope.SUBTREE, fromSerialized.getScope());
        assertEquals(true, fromSerialized.isRegisterOnAllInstances());

    }
}