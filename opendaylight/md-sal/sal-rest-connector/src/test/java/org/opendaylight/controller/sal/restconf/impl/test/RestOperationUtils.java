package org.opendaylight.controller.sal.restconf.impl.test;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;

import com.google.common.base.Charsets;

public class RestOperationUtils {

    static final String JSON = "+json";
    static final String XML = "+xml";

    private RestOperationUtils() {
    }

    static Entity<String> entity(String data, MediaType mediaType) {
        return Entity.entity(data, mediaType);
    }

    static Entity<String> entity(String data, String mediaType) {
        return Entity.entity(data, mediaType);
    }

    static String createUri(String prefix, String encodedPart) throws UnsupportedEncodingException {
        return URI.create(prefix + URLEncoder.encode(encodedPart, Charsets.US_ASCII.name()).toString()).toASCIIString();
    }
}
