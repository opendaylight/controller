/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.raft;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.event.LoggingAdapter;

import java.util.Map;

/**
 * The RaftActorContext contains that portion of the RaftActors state that
 * needs to be shared with it's behaviors. A RaftActorContext should NEVER be
 * used in any actor context outside the RaftActor that constructed it.
 */
public interface RaftActorContext {
    /**
     * Create a new local actor
      * @param props
     * @return
     */
    ActorRef actorOf(Props props);

    /**
     * Create a actor selection
     * @param path
     * @return
     */
    ActorSelection actorSelection(String path);

    /**
     * Get the identifier for the RaftActor. This identifier represents the
     * name of the actor whose common state is being shared. For example the
     * id could be 'inventory'
     * @return the identifier
     */
    String getId();

    /**
     * A reference to the RaftActor itself. This could be used to send messages
     * to the RaftActor
     * @return
     */
    ActorRef getActor();

    /**
     * Get the ElectionTerm information
     * @return
     */
    ElectionTerm getTermInformation();

    /**
     * index of highest log entry known to be
     * committed (initialized to 0, increases
     *    monotonically)
     * @return
     */
    long getCommitIndex();


    /**
     *
     */
    void setCommitIndex(long commitIndex);

    /**
     * index of highest log entry applied to state
     * machine (initialized to 0, increases
     *    monotonically)
     * @return
     */
    long getLastApplied();


    /**
     *
     */
    void setLastApplied(long lastApplied);

    /**
     *
     * @param replicatedLog
     */
    public void setReplicatedLog(ReplicatedLog replicatedLog);

    /**
     * @return A representation of the log
     */
    ReplicatedLog getReplicatedLog();

    /**
     * @return The ActorSystem associated with this context
     */
    ActorSystem getActorSystem();

    /**
     * Get the logger to be used for logging messages
     *
     * @return
     */
    LoggingAdapter getLogger();

    /**
     * Get a mapping of peerId's to their addresses
     *
     * @return
     *
     */
    Map<String, String> getPeerAddresses();

    /**
     * Get the address of the peer as a String. This is the same format in
     * which a consumer would provide the address
     *
     * @param peerId
     * @return The address of the peer or null if the address has not yet been
     *         resolved
     */
    String getPeerAddress(String peerId);

    /**
     * Add to actor peers
     * @param name
     * @param address
     */
    void addToPeers(String name, String address);

    /**
     *
     * @param name
     */
    public void removePeer(String name);

    /**
     * Given a peerId return the corresponding actor
     * <p>
     *
     *
     * @param peerId
     * @return The actorSelection corresponding to the peer or null if the
     *         address has not yet been resolved
     */
    ActorSelection getPeerActorSelection(String peerId);

    /**
     * Set Peer Address can be called at a later time to change the address of
     * a known peer.
     *
     * <p>
     * Throws an IllegalStateException if the peer is unknown
     *
     * @param peerId
     * @param peerAddress
     */
    void setPeerAddress(String peerId, String peerAddress);

    /**
     * @return ConfigParams
     */
    public ConfigParams getConfigParams();

    public void setSnapshotCaptureInitiated(boolean snapshotCaptureInitiated);

    public boolean isSnapshotCaptureInitiated();

    public void setConfigParams(ConfigParams configParams);

    public void setPeerAddresses(Map<String, String> peerAddresses);
}
