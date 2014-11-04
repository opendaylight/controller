/*
 * Copyright IBM Corporation, 2013.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.commons.httpclient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.apache.http.Header;
import org.apache.http.HeaderIterator;
import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

public class HTTPClient {
    static public HTTPResponse sendRequest(HTTPRequest request) throws Exception {

        CloseableHttpClient httpclient = HttpClients.createDefault();
        if (httpclient == null)
            throw new ClientProtocolException("Couldn't create an HTTP client");
        RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(request.getTimeout())
            .setConnectTimeout(request.getTimeout()).build();
        HttpRequestBase httprequest;
        String method = request.getMethod();

        if (method.equalsIgnoreCase("GET")) {
            httprequest = new HttpGet(request.getUri());
        } else if (method.equalsIgnoreCase("POST")) {
            httprequest = new HttpPost(request.getUri());
            if (request.getEntity() != null) {
                StringEntity sentEntity = new StringEntity(request.getEntity());
                sentEntity.setContentType(request.getContentType());
                ((HttpEntityEnclosingRequestBase) httprequest).setEntity(sentEntity);
            }
        } else if (method.equalsIgnoreCase("PUT")) {
            httprequest = new HttpPut(request.getUri());
            if (request.getEntity() != null) {
                StringEntity sentEntity = new StringEntity(request.getEntity());
                sentEntity.setContentType(request.getContentType());
                ((HttpEntityEnclosingRequestBase) httprequest).setEntity(sentEntity);
            }
        } else if (method.equalsIgnoreCase("DELETE")) {
            httprequest = new HttpDelete(request.getUri());
        } else {
            httpclient.close();
            throw new IllegalArgumentException("This profile class only supports GET, POST, PUT, and DELETE methods");
        }
        httprequest.setConfig(requestConfig);

        // add request headers
        Iterator<String> headerIterator = request.getHeaders().keySet().iterator();
        while (headerIterator.hasNext()) {
            String header = headerIterator.next();
            Iterator<String> valueIterator = request.getHeaders().get(header).iterator();
            while (valueIterator.hasNext()) {
                httprequest.addHeader(header, valueIterator.next());
            }
        }

        CloseableHttpResponse response = httpclient.execute(httprequest);
        try {
            HttpEntity receivedEntity = response.getEntity();
            int httpResponseCode = response.getStatusLine().getStatusCode();
            HTTPResponse ans = new HTTPResponse();
            HashMap<String, List<String>> headerMap = new HashMap<String, List<String>>();

            // copy response headers
            HeaderIterator it = response.headerIterator();
            while (it.hasNext()) {
                Header h = it.nextHeader();
                String name = h.getName();
                String value = h.getValue();
                if (headerMap.containsKey(name))
                    headerMap.get(name).add(value);
                else {
                    List<String> list = new ArrayList<String>();
                    list.add(value);
                    headerMap.put(name, list);
                }
            }
            ans.setHeaders(headerMap);

            if (httpResponseCode > 299) {
                ans.setStatus(httpResponseCode);
                ans.setEntity(response.getStatusLine().getReasonPhrase());
                return ans;
            }
            ans.setStatus(response.getStatusLine().getStatusCode());
            if (receivedEntity != null)
                ans.setEntity(EntityUtils.toString(receivedEntity));
            else
                ans.setEntity(null);
            return ans;
        } finally {
            response.close();
        }
    }
}
