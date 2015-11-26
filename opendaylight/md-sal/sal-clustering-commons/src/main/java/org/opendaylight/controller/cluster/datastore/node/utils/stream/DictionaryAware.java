/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.node.utils.stream;

import javax.annotation.Nonnull;

/**
 * Interface for detaching a dictionary.
 *
 * @param <T> Dictionary type
 */
interface DictionaryAware<T extends AbstractStreamDictionary> {
    /**
     * Detach a dictionary from this object, for potential reuse. The object is rendered inoperable, as far as
     * dictionary-based operations are concerned.
     *
     * @return Dictionary used by this reader.
     * @throws IllegalStateException if the dictionary has already been detached
     */
    @Nonnull T detachDictionary();
}
