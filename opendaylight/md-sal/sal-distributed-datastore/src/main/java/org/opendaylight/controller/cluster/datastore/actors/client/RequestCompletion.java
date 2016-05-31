package org.opendaylight.controller.cluster.datastore.actors.client;

import com.google.common.annotations.Beta;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opendaylight.controller.cluster.access.concepts.Request;
import org.opendaylight.controller.cluster.access.concepts.Response;

/**
 * A callback invoked when a particular {@link Request} completes.
 *
 * @author Robert Varga
 */
@Beta
@FunctionalInterface
public interface RequestCompletion {
    @Nullable ClientActorBehavior requestCompleted(@Nonnull Response<?, ?> response);
}
