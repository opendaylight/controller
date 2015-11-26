/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.node.utils.stream;

import com.google.common.base.Preconditions;

abstract class AbstractNormalizedNodeStream<T extends AbstractNormalizedNodeDictionary> implements DictionaryAware<T> {
    private T dictionary;

    AbstractNormalizedNodeStream(final T dictionary) {
        this.dictionary = Preconditions.checkNotNull(dictionary);
    }

    final T dictionary() {
        Preconditions.checkState(dictionary != null, "Dictionary has been detached");
        return dictionary;
    }

    @Override
    public final T detachDictionary() {
        Preconditions.checkState(dictionary != null, "Dictionary has already been detached");

        final T ret = dictionary;
        dictionary.detach(this);
        dictionary = null;
        return ret;
    }
}
