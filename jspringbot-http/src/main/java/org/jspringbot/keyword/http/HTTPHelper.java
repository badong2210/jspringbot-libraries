/*
 * Copyright (c) 2012. JSpringBot. All Rights Reserved.
 *
 * See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The JSpringBot licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jspringbot.keyword.http;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.*;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.cookie.Cookie;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.AbstractHttpClient;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.jspringbot.keyword.json.JSONHelper;
import org.jspringbot.keyword.xml.XMLHelper;
import org.jspringbot.syntax.HighlightRobotLogger;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;

public class HTTPHelper {

    public static final HighlightRobotLogger LOG = HighlightRobotLogger.getLogger(HTTPHelper.class);

    public static final String GET_METHOD = "GET";
    public static final String POST_METHOD = "POST";
    public static final String PUT_METHOD = "PUT";
    public static final String DELETE_METHOD = "DELETE";
    public static final String ENCODING_UTF_8 = "utf-8";
    public static final String USER_AGENT = "User-Agent";

    protected AbstractHttpClient client;

    protected HttpContext context;

    protected HttpRequest request;

    protected HttpResponse response;

    protected String responseString;

    protected StatusLine status;

    protected HttpEntity responseEntity;

    protected HttpHost targetHost;

    protected List<NameValuePair> params;

    protected List<NameValuePair> headers;

    protected URI uri;

    protected JSONHelper jsonHelper;

    protected XMLHelper xmlHelper;

    public HTTPHelper(AbstractHttpClient client) {
        this.client = client;
        newSession();
    }

    public void setJsonHelper(JSONHelper jsonHelper) {
        this.jsonHelper = jsonHelper;
    }

    public void setXmlHelper(XMLHelper xmlHelper) {
        this.xmlHelper = xmlHelper;
    }

    public void consume() {
        try {
            if (responseEntity != null) {
                EntityUtils.consume(responseEntity);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void newSession() {
        LOG.info("Created New HTTP Session.");

        this.context = new BasicHttpContext();
        BasicCookieStore cookieStore = new BasicCookieStore();
        context.setAttribute(ClientContext.COOKIE_STORE, cookieStore);
    }

    /**
     * Create an Http Request
     * @param url
     * @throws java.net.MalformedURLException
     * @throws java.net.URISyntaxException
     */
    public void createRequest(String url) throws MalformedURLException, URISyntaxException {
        createRequest(url, GET_METHOD);
    }

    /**
     * Create an Http Get Request
     * @param url
     * @throws java.net.MalformedURLException
     * @throws java.net.URISyntaxException
     */
    public void createGetRequest(String url) throws MalformedURLException, URISyntaxException {
        createRequest(url, GET_METHOD);
    }

    /**
     * Create an Http Post Request
     * @param url
     * @throws java.net.MalformedURLException
     * @throws java.net.URISyntaxException
     */
    public void createPostRequest(String url) throws MalformedURLException, URISyntaxException {
        createRequest(url, POST_METHOD);
    }

    /**
     * Create an Http Put Request
     * @param url
     * @throws MalformedURLException
     * @throws URISyntaxException
     */
    public void createPutRequest(String url) throws MalformedURLException, URISyntaxException {
        createRequest(url, PUT_METHOD);
    }

    /**
     * Create an Http Delete Reqest
     * @param url
     * @throws MalformedURLException
     * @throws URISyntaxException
     */
    public void createDeleteRequest(String url) throws MalformedURLException, URISyntaxException {
        createRequest(url, DELETE_METHOD);
    }

    /**
     * Create an Http Request
     * @param paramUrl
     * @param method
     * @throws java.net.MalformedURLException
     * @throws java.net.URISyntaxException
     */
    public void createRequest(String paramUrl, String method) throws MalformedURLException, URISyntaxException {

        uri = new URL(paramUrl).toURI();
        targetHost = new HttpHost(uri.getHost(), uri.getPort(), uri.getScheme());

        String uriPath = uri.getPath();

        if(uri.getQuery() != null) {
            uriPath += "?" + uri.getQuery();
        }

        LOG.createAppender()
            .appendBold("Create Request:")
            .appendProperty("URI", uriPath)
            .appendProperty("Method", method)
            .log();

        if (method.equalsIgnoreCase(POST_METHOD)) {
            request  = new HttpPost(uriPath);
        } else if (method.equalsIgnoreCase(GET_METHOD)) {
            request = new HttpGet(uriPath);
        } else if (method.equalsIgnoreCase(PUT_METHOD)) {
            request = new HttpPut(uriPath);
        } else if (method.equalsIgnoreCase(DELETE_METHOD)) {
            request = new HttpDelete(uriPath);
        } else {
            throw new IllegalArgumentException(String.format("Unknown http method '%s' for url '%s'.", method, paramUrl));
        }

        if (responseEntity != null) {
            consume();
        }

        params = new LinkedList<NameValuePair>();
        headers = new LinkedList<NameValuePair>();
        status = null;
        responseString = null;
        responseEntity = null;
        response = null;
    }

    /**
     * Add Http Request Header
     * @param name
     * @param value
     */
    public void addRequestHeader(String name, String value) {
        LOG.createAppender()
            .appendBold("Add Request Header:")
            .appendProperty("Name", name)
            .appendProperty("Value", value)
            .log();

        headers.add(new BasicNameValuePair(name, value));
    }

    /**
     * Add Http Request Parameter
     * @param name
     * @param value
     */
    public void addRequestParameter(String name, String value) {
        LOG.createAppender()
            .appendBold("Add Request Parameter:")
            .appendProperty("Name", name)
            .appendProperty("Value", value)
            .log();

        params.add(new BasicNameValuePair(name, value));
    }

    /**
     *
     * @param stringBody
     */
    public void setRequestBody(String stringBody) throws UnsupportedEncodingException {
        if(!HttpEntityEnclosingRequest.class.isInstance(request)) {
            throw new IllegalArgumentException("Does not support string entity.");
        }

        LOG.createAppender()
            .appendBold("Set Request Body:")
            .appendCode(stringBody)
            .log();

        ((HttpEntityEnclosingRequest) request).setEntity(new StringEntity(stringBody, ENCODING_UTF_8));
    }

    /**
     * Set Basic Authentication
     * @param username
     * @param password
     */
    public void setBasicAuthentication(String username, String password) {
        UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(username, password);

        client.getCredentialsProvider().setCredentials(new AuthScope(targetHost.getHostName(), targetHost.getPort()), credentials);

        // Create AuthCache instance
        AuthCache authCache = new BasicAuthCache();

        // Generate BASIC scheme object and add it to the local auth cache
        BasicScheme basicAuth = new BasicScheme();
        authCache.put(targetHost, basicAuth);

        LOG.createAppender()
            .appendBold("Set Basic Authentication:")
            .appendProperty("Username", username)
            .appendProperty("Password", password)
            .log();

        context.setAttribute(ClientContext.AUTH_CACHE, authCache);
    }

    private void addRequestHeadersToHttpRequest() {
        for(NameValuePair nv : headers) {
            String name = nv.getName();
            String value = nv.getValue();

            if (name.equalsIgnoreCase(USER_AGENT)) {
                request.setHeader(USER_AGENT, value);
                return;
            }

            request.addHeader(name, value);
        }
    }

    /**
     * Invoke Request
     */
    public void invokeRequest() throws IOException {
        String actualUri = uri.getPath();

        if (uri.getQuery() != null) {
            actualUri += "?" + uri.getQuery();
        }

        if (CollectionUtils.isNotEmpty(params)) {
            if (request instanceof HttpPost) {
                ((HttpPost) request).setEntity(new UrlEncodedFormEntity(params, ENCODING_UTF_8));
            } else {
                String queryString = URLEncodedUtils.format(params, ENCODING_UTF_8);

                if(actualUri.contains("?")) {
                    actualUri += "&" + queryString;
                } else {
                    actualUri += "?" + queryString;
                }

                if (request instanceof HttpGet) {
                    request = new HttpGet(actualUri);
                } else if (request instanceof HttpPut) {
                    request = new HttpPut(actualUri);
                } else if (request instanceof HttpDelete) {
                    request = new HttpDelete(actualUri);
                }
            }
        }

        addRequestHeadersToHttpRequest();

        LOG.createAppender()
            .appendBold("Request Line:")
            .appendCode(String.valueOf(request.getRequestLine()))
            .log();

        response = client.execute(targetHost, request, context);
        status = response.getStatusLine();
        responseEntity = response.getEntity();

        // request headers
        HighlightRobotLogger.HtmlAppender appender = LOG.createAppender().appendBold("Request Headers:");
        for(Header header : request.getAllHeaders()) {
            appender.appendProperty(header.getName(), header.getValue());
        }
        appender.log();

        LOG.createAppender()
            .appendBold("Response Status Line:")
            .appendCode(String.valueOf(status))
            .log();

        // response headers
        appender = LOG.createAppender().appendBold("Response Headers:");
        for(Header header : response.getAllHeaders()) {
            appender.appendProperty(header.getName(), header.getValue());
        }
        appender.log();
    }

    public String getResponseHeader(String name) {
        String value = response.getFirstHeader(name).getValue();

        LOG.createAppender()
                .appendBold("Get Response Header:")
                .appendProperty("name", name)
                .appendProperty("value", value)
                .log();


        return value;
    }


    public String getCookieValue(String name) {
        List<Cookie> cookies = client.getCookieStore().getCookies();

        String cookieValue = null;
        for(Cookie cookie : cookies) {
            if(StringUtils.equals(cookie.getName(), name)) {
                cookieValue = cookie.getValue();
                break;
            }
        }

        LOG.createAppender()
                .appendBold("Get Cookie:")
                .appendProperty("name", name)
                .appendProperty("value", cookieValue)
                .log();


        return cookieValue;
    }

    /**
     * Response should be XML
     * @throws IOException
     */
    public void responseShouldBeXML () throws IOException {
        try {
            if(responseString == null) {
                responseString = EntityUtils.toString(responseEntity);
                if (xmlHelper == null) {
                    throw new  IllegalStateException("Json checking is not supported");
                }

                xmlHelper.setXmlString(responseString);
                consume();
            }
        } catch (SAXException e) {
            throw new IOException(e);
        }
    }

    /**
     *
     * @throws IOException
     */
    public void responseShouldBeJson () throws IOException {
        if(responseString == null) {
            responseString = EntityUtils.toString(responseEntity);

            if (jsonHelper == null) {
                throw new  IllegalStateException("Json checking is not supported");
            }

            jsonHelper.setJsonString(responseString);
            consume();
        }
    }

    /**
     * Response Status Code Should Be Equal To
     * @param statusCode
     */
    public void responseStatusCodeShouldBeEqualTo(int statusCode) {
        if(status.getStatusCode() != statusCode) {
            throw new IllegalStateException(String.format("Expecting response status '%d' but was '%s'.", statusCode, status.toString()));
        }
    }

    /**
     * Response Should Contain
     * @param value
     * @throws java.io.IOException
     */
    public void responseShouldContain(String value) throws IOException {
        if(responseString == null) {
            responseString = EntityUtils.toString(responseEntity);

            LOG.createAppender()
                .appendBold("Response Content:")
                .appendCode(responseString)
                .log();

            consume();
        }

        if(!responseString.contains(value)) {
            throw new IllegalStateException(String.format("Expecting response should contain value '%s'.", value));
        }
    }
}
