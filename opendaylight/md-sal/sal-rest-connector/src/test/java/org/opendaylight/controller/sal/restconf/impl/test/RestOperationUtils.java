package org.opendaylight.controller.sal.restconf.impl.test;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;

import com.google.common.base.Charsets;

public class RestOperationUtils {

    static final String JSON = "+json";
    static final String XML = "+xml";

    private RestOperationUtils() {
    }

    static String createUri(String prefix, String encodedPart) throws UnsupportedEncodingException {
        return URI.create(prefix + URLEncoder.encode(encodedPart, Charsets.US_ASCII.name()).toString()).toASCIIString();
    }
}
