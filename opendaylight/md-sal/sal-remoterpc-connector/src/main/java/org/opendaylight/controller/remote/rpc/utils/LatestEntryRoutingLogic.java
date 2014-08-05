/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.remote.rpc.utils;

import akka.actor.ActorRef;
import akka.japi.Pair;
import com.google.common.base.Preconditions;

import java.util.Collection;
import java.util.Comparator;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * This class will return First Entry
 */
public class LatestEntryRoutingLogic implements RoutingLogic{

  private SortedSet<Pair<ActorRef, Long>> actorRefSet;

  public LatestEntryRoutingLogic(Collection<Pair<ActorRef, Long>> entries) {
    Preconditions.checkNotNull(entries, "Entries should not be null");
    Preconditions.checkArgument(!entries.isEmpty(), "Entries collection should not be empty");

    actorRefSet = new TreeSet<>(new LatestEntryComparator());
    actorRefSet.addAll(entries);
  }

  @Override
  public ActorRef select() {
    return actorRefSet.last().first();
  }


  private class LatestEntryComparator implements Comparator<Pair<ActorRef, Long>> {

    @Override
    public int compare(Pair<ActorRef, Long> o1, Pair<ActorRef, Long> o2) {
      if(o1 == null && o2 == null) {
        return 0;
      }
      if(o1 == null && o2 != null) {
        return -1;
      }
      if(o1 != null && o2 == null) {
        return 1;
      }

      return o1.second().compareTo(o2.second());

    }

  }
}


