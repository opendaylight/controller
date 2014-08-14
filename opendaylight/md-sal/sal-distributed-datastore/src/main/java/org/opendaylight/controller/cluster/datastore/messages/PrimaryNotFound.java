/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.messages;

import com.google.common.base.Preconditions;

public class PrimaryNotFound implements SerializableMessage {
  public static final Class SERIALIZABLE_CLASS = PrimaryNotFound.class;

    private final String shardName;

    public PrimaryNotFound(String shardName){

        Preconditions.checkNotNull(shardName, "shardName should not be null");

        this.shardName = shardName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PrimaryNotFound that = (PrimaryNotFound) o;

        if (shardName != null ? !shardName.equals(that.shardName) : that.shardName != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return shardName != null ? shardName.hashCode() : 0;
    }

  @Override
  public Object toSerializable() {
    return this;
  }

  public static PrimaryNotFound fromSerializable(Object message){
    return (PrimaryNotFound) message;
  }
}
