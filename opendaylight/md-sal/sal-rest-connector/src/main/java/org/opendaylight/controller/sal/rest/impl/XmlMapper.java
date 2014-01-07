package org.opendaylight.controller.sal.rest.impl;

import javax.activation.UnsupportedDataTypeException;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.impl.codec.xml.XmlDocumentUtils;
import org.opendaylight.yangtools.yang.model.api.DataNodeContainer;
import org.w3c.dom.Document;


public class XmlMapper {
    public Document write(CompositeNode data, DataNodeContainer schema) throws UnsupportedDataTypeException {
        return XmlDocumentUtils.toDocument(data, schema, XmlDocumentUtils.defaultValueCodecProvider());
    }
}
