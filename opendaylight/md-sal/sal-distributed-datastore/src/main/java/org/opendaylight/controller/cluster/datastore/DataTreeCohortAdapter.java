package org.opendaylight.controller.cluster.datastore;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import com.google.common.base.Preconditions;
import javax.annotation.concurrent.GuardedBy;
import org.opendaylight.controller.cluster.datastore.utils.ActorContext;
import org.opendaylight.mdsal.dom.api.DOMDataTreeCommitCohort;
import org.opendaylight.mdsal.dom.api.DOMDataTreeCommitCohortRegistration;
import org.opendaylight.mdsal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.yangtools.concepts.AbstractObjectRegistration;

public class DataTreeCohortAdapter<C extends DOMDataTreeCommitCohort> extends AbstractObjectRegistration<C>
        implements DOMDataTreeCommitCohortRegistration<C> {

    private final ActorRef actor;
    private final ActorContext actorContext;

    @GuardedBy("this")
    private ActorSelection listenerRegistrationActor;

    public DataTreeCohortAdapter(ActorContext actorContext, DOMDataTreeIdentifier subtree, C cohort) {
        super(cohort);
        this.actorContext = Preconditions.checkNotNull(actorContext);
        this.actor = actorContext.getActorSystem().actorOf(
                DataTreeCohortActor.props(getInstance()).withDispatcher(actorContext.getNotificationDispatcherPath()));
    }


    public void init(String shardName) {
        // FIXME: Add registration

    }

    @Override
    protected void removeRegistration() {
        // FIXME: Remove registration

    }
}
