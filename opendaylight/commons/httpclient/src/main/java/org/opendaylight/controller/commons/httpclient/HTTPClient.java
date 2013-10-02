package org.opendaylight.controller.commons.httpclient;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class HTTPClient {
    static public HTTPResponse sendRequest(HTTPRequest request) throws Exception {
        HTTPResponse ans = new HTTPResponse();
        URL url = new URL(request.getUri());
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod(request.getMethod());
        Map<String, List<String>> headers = request.getHeaders();
        Iterator<String> iterator = headers.keySet().iterator();
        while (iterator.hasNext()) {
            String key = iterator.next();
            List<String> values = headers.get(key);
            Iterator<String> vIterator = values.iterator();
            while (vIterator.hasNext())
                connection.setRequestProperty(key, vIterator.next());
        }

        if (request.getEntity() != null) {
            connection.setDoOutput(true);
            OutputStreamWriter wr = new OutputStreamWriter(connection.getOutputStream());
            wr.write(request.getEntity());
            wr.flush();
        }
        connection.connect();
        connection.getContentType();

        // Response code for success should be 2xx
        int httpResponseCode = connection.getResponseCode();
        if (httpResponseCode > 299) {
            ans.setStatus(httpResponseCode);
            ans.setEntity(connection.getResponseMessage());
            ans.setHeaders(connection.getHeaderFields());
            return ans;
        }

        InputStream is = connection.getInputStream();
        BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
        StringBuilder sb = new StringBuilder();
        int cp;
        while ((cp = rd.read()) != -1) {
            sb.append((char) cp);
        }
        is.close();
        connection.disconnect();
        ans.setStatus(httpResponseCode);
        ans.setEntity(sb.toString());
        ans.setHeaders(connection.getHeaderFields());
        return ans;

    }
}
