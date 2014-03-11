package org.opendaylight.controller.md.sal.common.api.data;

import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.concepts.Path;

public interface ScopedDataChangePublisher<P extends Path<P>, D,L extends DataChangeListener<P,D>> extends DataChangePublisher<P, D, L>{


    /**
    *
    * Scope of Data Change
    *
    * Represents scope of data change (addition, replacement, deletion).
    *
    */
   public enum DataChangeScope {

       /**
        * Represents only a direct change of the node, such as replacement of node,
        * addition or deletion.
        *
        */
       BASE,
       /**
        * Represent a change (addition,replacement,deletion)
        * of the node or one of it's direct childs.
        *
        */
       DIRECT_CHILD,
       /**
        * Represents a change of the node or any of it's child nodes.
        *
        */
       SUBTREE

   }

    /**
     * Registers {@link DataChangeListener} for Data Change callbacks
     * which will be triggered on Operational and Configuration changes.
     *
     * @param path Path (subtree identifier) on which client listener will be invoked.
     * @param listener Instance of listener which should be invoked on
     * @param triggeringScope Scope of change which triggers callback.
     * @return
     */
    ListenerRegistration<L> registerDataChangeListener(P path, L listener, DataChangeScope triggeringScope);

    /**
     * Registers {@link DataChangeListener} for Data Change callbacks
     * which will be triggered on Operational and Configuration changes.
     *
     * @param path Path (subtree identifier) on which client listener will be invoked.
     * @param listener Instance of listener which should be invoked on
     * @param triggeringScope Scope of change which triggers callback.
     * @return
     */
    ListenerRegistration<L> registerOperationalDataChangeListener(P path, L listener, DataChangeScope triggeringScope);

    /**
     * Registers {@link DataChangeListener} for Data Change callbacks
     * which will be triggered on Operational and Configuration changes.
     *
     * @param path Path (subtree identifier) on which client listener will be invoked.
     * @param listener Instance of listener which should be invoked on
     * @param triggeringScope Scope of change which triggers callback.
     * @return
     */
    ListenerRegistration<L> registerConfigurationDataChangeListener(P path, L listener, DataChangeScope triggeringScope);

}
