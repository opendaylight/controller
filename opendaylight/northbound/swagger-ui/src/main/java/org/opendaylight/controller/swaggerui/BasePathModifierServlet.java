package org.opendaylight.controller.swaggerui;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Servlet implementation class BasePathModifierServlet
 */
public class BasePathModifierServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory
            .getLogger(BasePathModifierServlet.class);

    private static final String API_BASE_PATH_SUFFIX = "/swagger/apis";
    private static final String BASE_PATH_KEY = "basePath";

    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
     *      response)
     */
    @Override
    protected void doGet(HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException {
        String resourcePath = request.getRequestURI().substring(
                request.getContextPath().length());
        logger.debug("Locating resource : {}.", resourcePath);
        JsonObject jsonObject = null;
        try {

            InputStream stream = this.getServletContext().getResourceAsStream(
                    resourcePath);
            if (stream == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND,
                        "API / API Listing not found");
                return;
            }
            BufferedReader streamReader = new BufferedReader(
                    new InputStreamReader(stream, "UTF-8"));
            StringBuilder responseStrBuilder = new StringBuilder();

            String inputStr;
            while ((inputStr = streamReader.readLine()) != null)
                responseStrBuilder.append(inputStr);

            JsonElement jelement = new JsonParser().parse(responseStrBuilder
                    .toString());
            jsonObject = jelement.getAsJsonObject();

            String basePath = jsonObject.get(BASE_PATH_KEY).getAsString();

            // construct base path
            StringBuilder requestURL = new StringBuilder();

            requestURL.append(request.isSecure() ? "https://" : "http://")
                    .append(request.getServerName()).append(":")
                    .append(request.getServerPort());
            if (!basePath.contains(requestURL)) {
                String endPath = "";
                if (basePath.contains(API_BASE_PATH_SUFFIX)) {
                    endPath = basePath.substring(basePath
                            .indexOf(API_BASE_PATH_SUFFIX));
                }
                basePath = requestURL + endPath;
                logger.debug("Modified Base Path is {}", basePath);
                jsonObject.addProperty(BASE_PATH_KEY, basePath);
            }
        } catch (Exception ex) {
            logger.error("Error processing JSON data", ex);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Could not read API Listing or APIs");
            return;
        }

        try {
            response.setContentType("application/json");
            PrintWriter out = response.getWriter();
            out.print(jsonObject);
            out.flush();
        } catch (Exception ex) {
            logger.error("Error while writing response", ex);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Internal Error while writing resposne");
        }
    }

}
