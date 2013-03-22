
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.utils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;

/**
 * This interface is needed to allow object reconstruction when reading serialized objects from
 * inside a package other than the one where ObjectReader is defined (otherwise you hit the
 * java.lang.ClassNotFoundException). This interface will allow to deserialize a class from inside
 * the package where the class is defined. All the exception handling can still happen in
 * ObjectReader and the implementer of IObjectReader only need to throws such exceptions
 *
 *
 *
 */
public interface IObjectReader {
    public Object readObject(ObjectInputStream ois)
            throws FileNotFoundException, IOException, ClassNotFoundException;
}
