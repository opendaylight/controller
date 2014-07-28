/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.remote.rpc.registry;


import java.io.Serializable;

public class GossipElement implements Serializable{

  String node;

  long version;

  public GossipElement(String node, long version) {
    this.node = node;
    this.version = version;
  }

  public String getNode() {
    return node;
  }

  public long getVersion() {
    return version;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    GossipElement that = (GossipElement) o;

    if (version != that.version) return false;
    if (!node.equals(that.node)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = node.hashCode();
    result = 31 * result + (int) (version ^ (version >>> 32));
    return result;
  }
}
