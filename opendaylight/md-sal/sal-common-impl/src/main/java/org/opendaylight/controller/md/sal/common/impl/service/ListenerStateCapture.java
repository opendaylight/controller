package org.opendaylight.controller.md.sal.common.impl.service;

import java.util.Map;

import org.opendaylight.controller.md.sal.common.api.data.DataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.DataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.DataModification;
import org.opendaylight.yangtools.concepts.Path;

import com.google.common.base.Predicate;

public final class ListenerStateCapture<P extends Path<P>, D, DCL extends DataChangeListener<P, D>> {

    final P path;

    final Iterable<DataChangeListenerRegistration<P, D, DCL>> listeners;

    D initialOperationalState;

    D initialConfigurationState;

    D finalConfigurationState;

    D finalOperationalState;

    Map<P, D> additionalConfigOriginal;
    Map<P, D> additionalConfigCreated;
    Map<P, D> additionalConfigUpdated;
    Map<P, D> additionalConfigDeleted;

    Map<P, D> additionalOperOriginal;
    Map<P, D> additionalOperCreated;
    Map<P, D> additionalOperUpdated;
    Map<P, D> additionalOperDeleted;

    RootedChangeSet<P, D> normalizedConfigurationChanges;
    RootedChangeSet<P, D> normalizedOperationalChanges;

    private final Predicate<P> containsPredicate;

    public ListenerStateCapture(P path, Iterable<DataChangeListenerRegistration<P, D, DCL>> listeners,
            Predicate<P> containsPredicate) {
        super();
        this.path = path;
        this.listeners = listeners;
        this.containsPredicate = containsPredicate;
    }

    protected D getInitialOperationalState() {
        return initialOperationalState;
    }

    protected void setInitialOperationalState(D initialOperationalState) {
        this.initialOperationalState = initialOperationalState;
    }

    protected D getInitialConfigurationState() {
        return initialConfigurationState;
    }

    protected void setInitialConfigurationState(D initialConfigurationState) {
        this.initialConfigurationState = initialConfigurationState;
    }

    protected P getPath() {
        return path;
    }

    protected Iterable<DataChangeListenerRegistration<P, D, DCL>> getListeners() {
        return listeners;
    }

    protected D getFinalConfigurationState() {
        return finalConfigurationState;
    }

    protected void setFinalConfigurationState(D finalConfigurationState) {
        this.finalConfigurationState = finalConfigurationState;
    }

    protected D getFinalOperationalState() {
        return finalOperationalState;
    }

    protected void setFinalOperationalState(D finalOperationalState) {
        this.finalOperationalState = finalOperationalState;
    }

    protected RootedChangeSet<P, D> getNormalizedConfigurationChanges() {
        return normalizedConfigurationChanges;
    }

    protected void setNormalizedConfigurationChanges(RootedChangeSet<P, D> normalizedConfigurationChanges) {
        this.normalizedConfigurationChanges = normalizedConfigurationChanges;
    }

    protected RootedChangeSet<P, D> getNormalizedOperationalChanges() {
        return normalizedOperationalChanges;
    }

    protected void setNormalizedOperationalChanges(RootedChangeSet<P, D> normalizedOperationalChange) {
        this.normalizedOperationalChanges = normalizedOperationalChange;
    }

    protected DataChangeEvent<P, D> createEvent(DataModification<P, D> modification) {
        return ImmutableDataChangeEvent.<P, D> builder()//
                .addTransaction(modification, containsPredicate) //
                .addConfigurationChangeSet(normalizedConfigurationChanges) //
                .addOperationalChangeSet(normalizedOperationalChanges) //
                .setOriginalConfigurationSubtree(initialConfigurationState) //
                .setOriginalOperationalSubtree(initialOperationalState) //
                .setUpdatedConfigurationSubtree(finalConfigurationState) //
                .setUpdatedOperationalSubtree(finalOperationalState) //
                .build();

    }

}
