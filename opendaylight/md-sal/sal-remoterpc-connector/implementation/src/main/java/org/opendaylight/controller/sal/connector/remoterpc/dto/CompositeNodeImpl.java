/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.connector.remoterpc.dto;

import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.*;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Skeleton implementation of CompositeNodeImpl.
 */
public class CompositeNodeImpl implements CompositeNode, Serializable {

  private QName key;

  private List<Node<?>> children;

  @Override
  public List<Node<?>> getChildren() {
    return children;
  }

  @Override
  public List<CompositeNode> getCompositesByName(QName children) {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<CompositeNode> getCompositesByName(String children) {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<SimpleNode<?>> getSimpleNodesByName(QName children) {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<SimpleNode<?>> getSimpleNodesByName(String children) {
    throw new UnsupportedOperationException();
  }

  @Override
  public CompositeNode getFirstCompositeByName(QName container) {
    throw new UnsupportedOperationException();
  }

  @Override
  public SimpleNode<?> getFirstSimpleByName(QName leaf) {
    throw new UnsupportedOperationException();
  }

  @Override
  public MutableCompositeNode asMutable() {
    throw new UnsupportedOperationException();
  }

  @Override
  public QName getKey() {
    return key;
  }

  @Override
  public List<Node<?>> setValue(List<Node<?>> value) {
    throw new UnsupportedOperationException();
  }

  @Override
  public int size() {
    return 0;
  }

  @Override
  public boolean isEmpty() {
    return false;
  }

  @Override
  public boolean containsKey(Object key) {
    return false;
  }

  @Override
  public boolean containsValue(Object value) {
    return false;
  }

  @Override
  public List<Node<?>> get(Object key) {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<Node<?>> put(QName key, List<Node<?>> value) {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<Node<?>> remove(Object key) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void putAll(Map<? extends QName, ? extends List<Node<?>>> m) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void clear() {
    throw new UnsupportedOperationException();
  }

  @Override
  public Set<QName> keySet() {
    throw new UnsupportedOperationException();
  }

  @Override
  public Collection<List<Node<?>>> values() {
    throw new UnsupportedOperationException();
  }

  @Override
  public Set<Entry<QName, List<Node<?>>>> entrySet() {
    throw new UnsupportedOperationException();
  }

  @Override
  public QName getNodeType() {
    throw new UnsupportedOperationException();
  }

  @Override
  public CompositeNode getParent() {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<Node<?>> getValue() {
    throw new UnsupportedOperationException();
  }

  @Override
  public ModifyAction getModificationAction() {
    throw new UnsupportedOperationException();
  }
}
