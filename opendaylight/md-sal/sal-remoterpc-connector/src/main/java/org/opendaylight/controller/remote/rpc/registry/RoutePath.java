/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.remote.rpc.registry;


public class RoutePath implements Comparable{

  String actorPath;
  //TODO: change it to vector clock
  long timestamp;

  public RoutePath(String actorPath, long timestamp) {
    this.actorPath = actorPath;
    this.timestamp = timestamp;
  }

  public String getActorPath() {
    return actorPath;
  }

  public long getTimestamp() {
    return timestamp;
  }

  @Override
  public int compareTo(Object o) {
    if (o == null || getClass() != o.getClass()) {
      return 0;
    }
    RoutePath that = (RoutePath) o;
    return Long.compare(this.timestamp, that.getTimestamp());
  }
}
