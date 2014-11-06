/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.messages;


public class PrimaryFound implements SerializableMessage {
  public static final Class<PrimaryFound> SERIALIZABLE_CLASS = PrimaryFound.class;
  private final String primaryPath;

  public PrimaryFound(final String primaryPath) {
    this.primaryPath = primaryPath;
  }

  public String getPrimaryPath() {
    return primaryPath;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
        return true;
    }
    if (o == null || getClass() != o.getClass()) {
        return false;
    }

    PrimaryFound that = (PrimaryFound) o;

    if (!primaryPath.equals(that.primaryPath)) {
        return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    return primaryPath.hashCode();
  }

  @Override
  public String toString() {
    return "PrimaryFound{" +
            "primaryPath='" + primaryPath + '\'' +
            '}';
  }


  @Override
  public Object toSerializable() {
    return  this;
  }

  public static PrimaryFound fromSerializable(final Object message){
    return (PrimaryFound) message;
  }
}
