/**
 * MediaMosa API
 *
 * A partial implementation of the MediaMosa API in Java.
 *
 * Copyright 2010 Universiteit van Amsterdam
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.uva.mediamosa;

import nl.uva.mediamosa.util.ChallengeUtil;
import nl.uva.mediamosa.util.MD5Util;
import nl.uva.mediamosa.util.SHA1Util;
import nl.uva.mediamosa.util.XmlParserUtil;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.TrustStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


public class MediaMosaConnector {

    private static final Logger log = LoggerFactory.getLogger(MediaMosaConnector.class.getName());

    private final String host;
    private String username;
    private String password;
    private final HttpClient httpclient;
    private Header responseHeader;
    private CookieStore httpCookieStore = new BasicCookieStore();

    public MediaMosaConnector(String host) {
        this.host = host;
        this.httpclient = getHttpClient();
    }

    private HttpClient getHttpClient() {
        HttpClientBuilder b = HttpClientBuilder.create().setDefaultCookieStore(httpCookieStore);

        SSLContext sslContext = null;
        try {
            sslContext = new SSLContextBuilder().loadTrustMaterial(null, new TrustStrategy() {
                public boolean isTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
                    return true;
                }
            }).build();
        } catch (KeyManagementException | NoSuchAlgorithmException | KeyStoreException e) {
            log.warn("Unexpected error occurerd while setting up SSL context", e);
        }
        b.setSSLContext(sslContext);

        HostnameVerifier hostnameVerifier = SSLConnectionSocketFactory.getDefaultHostnameVerifier();

        SSLConnectionSocketFactory sslSocketFactory = new SSLConnectionSocketFactory(sslContext, hostnameVerifier);
        Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
                .register("http", PlainConnectionSocketFactory.getSocketFactory())
                .register("https", sslSocketFactory)
                .build();
        // allows multi-threaded use
        PoolingHttpClientConnectionManager connMgr = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
        b.setConnectionManager(connMgr);

        return b.build();
    }

    public void setCredentials(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public boolean doLogin() throws IOException {

        String challenge = null;
        HttpPost httppost = new HttpPost(host + "/login");
        List<NameValuePair> nvps = new ArrayList<>();
        nvps.add(new BasicNameValuePair("dbus", "AUTH DBUS_COOKIE_SHA1 " + this.username));
        httppost.setEntity(new UrlEncodedFormEntity(nvps, Charset.forName("UTF-8")));
        HttpResponse response = httpclient.execute(httppost);
        HttpEntity entity = response.getEntity();

        InputStream is = null;
        try {
            if (entity != null) {
                is = entity.getContent();
                String content = IOUtils.toString(is);
                challenge = ChallengeUtil.getChallenge(content);
                is.close();
            }

            // posting challenge and random value
            String randomValue = MD5Util.getRandomValue();
            String responseValue = SHA1Util.getSHA1(challenge + ":" + randomValue + ":" + this.password);
            nvps = new ArrayList<>();
            nvps.add(new BasicNameValuePair("dbus", "DATA " + randomValue + " " + responseValue));
            httppost.setEntity(new UrlEncodedFormEntity(nvps, Charset.forName("UTF-8")));
            response = httpclient.execute(httppost);
            entity = response.getEntity();

            if (entity != null) {
                is = entity.getContent();
                String content = IOUtils.toString(entity.getContent());
                this.responseHeader = XmlParserUtil.parseResponse(content);
                is.close();
            }
        } finally {
            if (is != null) {
                is.close();
            }
        }
        return responseHeader.getRequestResultId() == ErrorCodes.ERRORCODE_OKAY;
    }

    public String doGetRequest(String getRequest) throws IOException {
        String content = null;
        HttpGet httpget = new HttpGet(host + getRequest);
        HttpResponse response = httpclient.execute(httpget);
        HttpEntity entity = response.getEntity();

        InputStream is = null;
        try {
            if (entity != null) {
                is = entity.getContent();
                content = IOUtils.toString(is);
                this.responseHeader = XmlParserUtil.parseResponse(content);
            }
        } finally {
            if (is != null) {
                is.close();
            }
        }
        // if cookie is expired, then relogin
        if (responseHeader.getRequestResultId() == ErrorCodes.ERRORCODE_ACCES_DENIED) {
            doLogin();
            doGetRequest(getRequest);
        }
        return content;
    }

    public String doPostRequest(String postRequest, String postParams) throws IOException {
        String content = null;
        HttpPost httppost = new HttpPost(host + postRequest);
        List<NameValuePair> nvps = URLEncodedUtils.parse(postParams, Charset.forName("UTF-8"));
        httppost.setEntity(new UrlEncodedFormEntity(nvps, Charset.forName("UTF-8")));
        HttpResponse response = httpclient.execute(httppost);
        HttpEntity entity = response.getEntity();

        InputStream is = null;
        try {
            if (entity != null) {
                is = entity.getContent();
                content = IOUtils.toString(is);
                this.responseHeader = XmlParserUtil.parseResponse(content);
            }
        } finally {
            if (is != null) {
                is.close();
            }
        }

        // if cookie is expired, then relogin
        if (responseHeader.getRequestResultId() == ErrorCodes.ERRORCODE_ACCES_DENIED) {
            doLogin();
            doPostRequest(postRequest, postParams);
        }
        return content;
    }

    public boolean isValidCookie() {
        if (httpclient == null) {
            return false;
        } else {
            httpCookieStore.clearExpired(new Date());
            List<Cookie> cookies = httpCookieStore.getCookies();
            if (!cookies.isEmpty()) {
                for (int i = 0; i < cookies.size(); i++) {
                    log.debug("Cookie ExpiryDate: " + cookies.get(i).getExpiryDate().toString());
                }
            }
        }
        return false;
    }

}
