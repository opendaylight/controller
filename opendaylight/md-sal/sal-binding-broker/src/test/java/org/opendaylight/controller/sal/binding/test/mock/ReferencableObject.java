package org.opendaylight.controller.sal.binding.test.mock;

import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.Identifiable;

public interface ReferencableObject extends DataObject,Identifiable<ReferencableObjectKey> {

}
