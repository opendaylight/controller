package org.opendaylight.controller.sal.rest.impl;

import static org.opendaylight.controller.sal.restconf.impl.MediaTypes.API;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import org.opendaylight.controller.sal.rest.api.RestconfService;
import org.opendaylight.controller.sal.restconf.impl.StructuredData;
import org.opendaylight.yangtools.yang.model.api.DataNodeContainer;

import com.google.gson.stream.JsonWriter;

@Provider
@Produces({ API + RestconfService.JSON })
public enum StructuredDataToJsonProvider implements MessageBodyWriter<StructuredData> {
    INSTANCE;
    
    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return true;
    }

    @Override
    public long getSize(StructuredData t, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return -1;
    }

    @Override
    public void writeTo(StructuredData t, Class<?> type, Type genericType, Annotation[] annotations,
            MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream)
            throws IOException, WebApplicationException {
        JsonWriter writer = new JsonWriter(new OutputStreamWriter(entityStream, "UTF-8"));
        writer.setIndent("    ");
        JsonMapper jsonMapper = new JsonMapper();
        jsonMapper.write(writer, t.getData(), (DataNodeContainer) t.getSchema());
        writer.flush();
    }
    
}
