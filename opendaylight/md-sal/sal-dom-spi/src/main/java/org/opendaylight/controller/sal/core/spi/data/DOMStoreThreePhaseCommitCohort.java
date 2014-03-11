package org.opendaylight.controller.sal.core.spi.data;

import com.google.common.util.concurrent.ListenableFuture;

public interface DOMStoreThreePhaseCommitCohort {

    /**
     * Sends transaction associated with this three phase commit instance to the
     * participant, participant votes on the transaction, if the transaction
     * should be commited or aborted.
     *
     * @return ListenableFuture with vote of the participant. Vote
     *         {@link ListenableFuture#get()} is following:
     *         <ul>
     *         <li>
     *         true if transaction is approved by data store.
     *         <li>false if the transaction is not approved by data store and
     *         should be aborted.
     */
    ListenableFuture<Boolean> canCommit();

    /**
     *
     *  Initiates a pre-commit phase of associated transaction on datastore.
     *
     *  This message is valid only and only if the participant responded
     *  on {@link #canCommit()} call with positive response.
     *
     * @return Listenable Future representing acknowledgment for participant
     *        that pre-commit message was received and processed.
     */
    ListenableFuture<Void> preCommit();

    /**
    *
    *  Initiates a abort phase of associated transaction on data store.
    *
    *
    * @return Listenable Future representing acknowledgment for participant
    *        that abort message was received.
    */
    ListenableFuture<Void> abort();

    /**
    *
    *  Initiates a commit phase on of associated transaction on data store.
    *
    * @return Listenable Future representing acknowledgment for participant
    *        that commit message was received and commit of transaction was
    *        processed.
    */
    ListenableFuture<Void> commit();
}
