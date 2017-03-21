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
package nl.uva.mediamosa.impl;

import nl.uva.mediamosa.MediaMosa;
import nl.uva.mediamosa.MediafileProperties;
import nl.uva.mediamosa.model.*;
import nl.uva.mediamosa.util.*;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.cookie.Cookie;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.TrustStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.xml.transform.TransformerException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static nl.uva.mediamosa.ErrorCodes.ERRORCODE_COULD_NOT_FIND_STILL;
import static nl.uva.mediamosa.ErrorCodes.ERRORCODE_OKAY;


public class MediaMosaImpl implements MediaMosa {

    private static final long ONE_HOUR_MILLIS = TimeUnit.SECONDS.toMillis(60L * 60L);
    private String username;
    private String password;
    private HttpClient httpclient;
    private CookieStore httpCookieStore = new BasicCookieStore();
    private final AtomicLong cookieExpireTime = new AtomicLong(0);
    private String hostname;
    private String vpxVersion;
    private static final Logger log = LoggerFactory.getLogger(MediaMosaImpl.class.getName());

    /**
     * Maximum of items to retrieve at once
     */
    public static final int LIMIT = 200;

    /**
     * Default value of (optional) offset
     */
    public static final int DEFAULT_OFFSET = 0;

    /* (non-Javadoc)
      * @see nl.uva.mediamosa.VpCore#setHostname(java.lang.String)
      */
    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    /* (non-Javadoc)
      * @see nl.uva.mediamosa.VpCore#setCredentials(java.lang.String, java.lang.String)
      */
    public void setCredentials(String username, String password) {
        this.username = username;
        this.password = password;
    }

    /**
     * @param getRequest
     * @return
     * @throws java.io.IOException
     */
    public Response doGetRequest(String getRequest) throws IOException {
        HttpGet httpGet = new HttpGet(hostname + getRequest);
        return UnmarshallUtil.unmarshall(getXmlTransformedResponse(httpGet));
    }

    /**
     *
     * @param getRequest
     * @return
     * @throws IOException
     */
    public String doGetRequestString(String getRequest) throws IOException {
        HttpGet httpGet = new HttpGet(hostname + getRequest);
        return getStringResponse(httpGet);
    }

    /**
     * @param postRequest
     * @param postParams
     * @return
     * @throws IOException
     */
    public String doPostRequestString(String postRequest, String postParams) throws IOException {
        HttpPost httpPost = new HttpPost(hostname + postRequest);

        List<NameValuePair> nvps = URLEncodedUtils.parse(postParams, Charset.forName("UTF-8"));
        httpPost.setEntity(new UrlEncodedFormEntity(nvps, Charset.forName("UTF-8")));

        return getXmlTransformedResponse(httpPost);
    }

    /**
     * @param postRequest
     * @param postParams
     * @return
     * @throws java.io.IOException
     */
    public Response doPostRequest(String postRequest, String postParams) throws IOException {
        return UnmarshallUtil.unmarshall(doPostRequestString(postRequest, postParams));
    }

    /**
     * @param postRequest
     * @param postParams
     * @return
     * @throws java.io.IOException
     */
    public Response doPostRequest(String uploadHost, String postRequest, String postParams, InputStream stream, String mimeType, String filename) throws IOException {
        HttpPost httpPost = new HttpPost(uploadHost + postRequest);

        MultipartEntityBuilder mpBuilder = MultipartEntityBuilder.create();
        mpBuilder.addBinaryBody("file", stream, ContentType.create(mimeType), filename);

        List<NameValuePair> nvps = URLEncodedUtils.parse(postParams, Charset.forName("UTF-8"));
        for (NameValuePair nameValuePair : nvps) {
            mpBuilder.addTextBody(nameValuePair.getName(), nameValuePair.getValue());
        }
        httpPost.setEntity(mpBuilder.build());

        return UnmarshallUtil.unmarshall(getXmlTransformedResponse(httpPost));
    }

    private String getStringResponse(HttpUriRequest request) throws IOException {
        HttpResponse response = httpclient.execute(request);
        HttpEntity entity = response.getEntity();
        InputStream is = null;
        String content = null;

        try {
            if (response != null && entity != null) {
                is = entity.getContent();
                content = IOUtils.toString(is);
            } else {
                log.warn("Empty response received from {}", request.getURI().toURL().toString());
            }
        } catch (UnsupportedOperationException | MalformedURLException e) {
            log.error("Unexpected error occurred", e);
        } finally {
            if (is != null) {
                is.close();
            }
        }
        return content;
    }

    private String getXmlTransformedResponse(HttpUriRequest request) throws IOException {
        String content = getStringResponse(request);
        String transformedContent = null;
        if (content != null) {
            try {
                transformedContent = XsltUtil.transform(content, getClass().getResourceAsStream("/convert_item_elements.xsl"));
            } catch (TransformerException e) {
                if (log.isDebugEnabled()) {
                    log.debug(e.getMessage(), e);
                } else {
                    log.error(e.getMessage());
                }
            }
        }

        return transformedContent;
    }

    /* (non-Javadoc)
      * @see nl.uva.mediamosa.VpCore#isValidCookie()
      */
    public boolean isValidCookie() {
        boolean status = false;
        if (httpclient != null) {
            // Do not let cookies get too old. Stupid workaround for very old sessions.
            if (cookieExpireTime.get() < System.currentTimeMillis()) {
                httpCookieStore.clear();
                cookieExpireTime.set(System.currentTimeMillis() + ONE_HOUR_MILLIS);
            }

            // remove expired cookies
            httpCookieStore.clearExpired(new Date());
            List<Cookie> cookies = httpCookieStore.getCookies();
            if (!cookies.isEmpty()) {
                // still a valid cookie
                status = true;
            }
        }
        return status;
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

    public boolean login() throws IOException, ServiceException {
        String challenge = null;
        httpclient = getHttpClient();
        HttpPost httppost = new HttpPost(hostname + "/login");
        List<NameValuePair> nvps = new ArrayList<>();
        nvps.add(new BasicNameValuePair("dbus", "AUTH DBUS_COOKIE_SHA1 " + this.username));
        httppost.setEntity(new UrlEncodedFormEntity(nvps, Charset.forName("UTF-8")));
        HttpResponse response = httpclient.execute(httppost);
        HttpEntity entity = response.getEntity();
        InputStream is = null;
        Response vpxResponse = null;
        try {
            if (entity != null) {
                is = entity.getContent();
                challenge = ChallengeUtil.getChallenge(IOUtils.toString(is));
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
                vpxResponse = UnmarshallUtil.unmarshall(is);
                is.close();
            }
        } catch (IOException e) {
            throw e;
        } finally {
            if (is != null) {
                is.close();
            }
        }

        // set VPX version
        this.vpxVersion = vpxResponse.getHeader().getVpxVersion();

        return vpxResponse.getHeader().getRequestResultId() == ERRORCODE_OKAY;
    }


    /* (non-Javadoc)
      * @see nl.uva.mediamosa.VpCore#getVersion()
      */
    public String getVersion() {
        return vpxVersion;
    }

    /* (non-Javadoc)
      * @see nl.uva.mediamosa.VpCore#getAssets()
      */
    public List<AssetType> getAssets() throws ServiceException {
        int limit = LIMIT;
        int offset = DEFAULT_OFFSET;
        return getAssets(limit, offset, null);
    }

    /*
     public List<AssetType> getAssets(int offset) throws ServiceException {
         int limit = LIMIT;
         return getAssets(limit, offset, null);
     }

     public List<AssetType> getAssets(int limit) throws ServiceException {
         int offset = DEFAULT_OFFSET;
         return getAssets(limit);
     }
     */

    // wat als offset groter is dan total items?

    //Map options Object gebruiken voor int limit, int offset?

    /* (non-Javadoc)
      * @see nl.uva.mediamosa.VpCore#getAssets(java.util.Map)
      */
    public List<AssetType> getAssets(Map properties) throws ServiceException {

        String requestUrl = "/asset";
        String parameters = "limit=" + LIMIT;
        List<AssetType> assets = new ArrayList<AssetType>();
        Response vpxResponse = null;

        if (properties != null) {
            // iterate over key value pairs
            Iterator it = properties.entrySet().iterator();
            StringBuilder sb = new StringBuilder();
            while (it.hasNext()) {
                sb.append(parameters).append('&');
                Map.Entry pairs = (Map.Entry) it.next();
                sb.append(pairs.getKey()).append('=').append(pairs.getValue());
            }
            parameters = sb.toString();
        }
        requestUrl += "?" + parameters;

        try {
            vpxResponse = doGetRequest(requestUrl);
        } catch (IOException e) {
            throw new ServiceException(e);
        }

        if (vpxResponse.getHeader().getRequestResultId() == ERRORCODE_OKAY) {
            ItemsType items = vpxResponse.getItems();

            for (Object o : items.getLinkOrAssetOrAssetDetails()) {
                if (o instanceof AssetType) {
                    assets.add((AssetType) o);
                }
            }
        } else {
            log.error("MM failed request: " + vpxResponse.getHeader().getRequestResultDescription());
        }
        return assets;
    }

    /**
     * @param limit
     * @param properties
     * @return
     * @throws ServiceException
     */
    public long getAssetCount(int limit, Map properties) throws ServiceException {
        String requestUrl = "/asset";
        String parameters = "";
        if (limit <= LIMIT) {
            parameters = "limit=" + limit;
        } else {
            parameters = "limit=" + LIMIT;
        }

        Response vpxResponse = null;

        if (properties != null) {
            // iterate over key value pairs
            Iterator it = properties.entrySet().iterator();
            StringBuilder sb = new StringBuilder();
            while (it.hasNext()) {
                sb.append('&');
                Map.Entry pairs = (Map.Entry) it.next();
                sb.append(pairs.getKey()).append('=').append(pairs.getValue());
            }
            parameters = parameters + sb.toString();
        }

        requestUrl += "?" + parameters;

        try {
            vpxResponse = doGetRequest(requestUrl);
        } catch (IOException e) {
            throw new ServiceException(e);
        }

        return vpxResponse.getHeader().getItemCountTotal();
    }

    /**
     * @param limit
     * @param offset
     * @param properties
     * @return
     * @throws ServiceException
     */
    public List<AssetType> getAssets(int limit, int offset, Map properties) throws ServiceException {
        String requestUrl = "/asset";
        String parameters = "";
        if (limit <= LIMIT) {
            parameters = "limit=" + limit;
        } else {
            parameters = "limit=" + LIMIT;
        }
        if (offset > DEFAULT_OFFSET) {
            parameters += String.format("&offset=%s&", offset);
        }

        List<AssetType> assets = new ArrayList<AssetType>();

        Response vpxResponse = null;

        if (properties != null) {
            // iterate over key value pairs
            Iterator it = properties.entrySet().iterator();
            StringBuilder sb = new StringBuilder();
            while (it.hasNext()) {
                sb.append('&');
                Map.Entry pairs = (Map.Entry) it.next();
                sb.append(pairs.getKey()).append('=').append(pairs.getValue());
            }
            parameters = parameters + sb.toString();
        }

        requestUrl += "?" + parameters;

        try {
            vpxResponse = doGetRequest(requestUrl);
        } catch (IOException e) {
            throw new ServiceException(e);
        }

        if (vpxResponse.getHeader().getRequestResultId() == ERRORCODE_OKAY) {
            ItemsType items = vpxResponse.getItems();

            for (Object o : items.getLinkOrAssetOrAssetDetails()) {
                if (o instanceof AssetType) {
                    assets.add((AssetType) o);
                }
            }
        } else {
            log.error("MM failed request: " + vpxResponse.getHeader().getRequestResultDescription());
        }
        return assets;
    }


    /* (non-Javadoc)
      * @see nl.uva.mediamosa.VpCore#getAssetDetails(java.lang.String)
      */
    public AssetDetailsType getAssetDetails(String assetId) throws ServiceException {
        String requestUrl = "/asset/" + assetId;
        AssetDetailsType assetDetails = null;
        Response vpxResponse = null;

        try {
            vpxResponse = doGetRequest(requestUrl);
        } catch (IOException e) {
            throw new ServiceException(e);
        }

        if (vpxResponse.getHeader().getRequestResultId() == ERRORCODE_OKAY) {

            ItemsType items = vpxResponse.getItems();
            if (!items.getLinkOrAssetOrAssetDetails().isEmpty()) {
                assetDetails = (AssetDetailsType) items.getLinkOrAssetOrAssetDetails().get(0);
            }
        } else {
            log.error("MM failed request: " + vpxResponse.getHeader().getRequestResultDescription());
        }
        return assetDetails;
    }

    // is_admin=true en userid paramaters

    /**
     * @return
     * @throws ServiceException
     */
    public List<ErrorcodeType> getErrorCodes() throws ServiceException {
        String requestUrl = "/errorcodes";
        List<ErrorcodeType> errorCodes = new ArrayList<ErrorcodeType>();
        Response vpxResponse = null;

        try {
            vpxResponse = doGetRequest(requestUrl);
        } catch (IOException e) {
            throw new ServiceException(e);
        }

        if (vpxResponse.getHeader().getRequestResultId() == ERRORCODE_OKAY) {

            ItemsType items = vpxResponse.getItems();

            for (Object o : items.getLinkOrAssetOrAssetDetails()) {
                if (o instanceof ErrorcodeType) {
                    errorCodes.add((ErrorcodeType) o);
                }
            }
        } else {
            log.error("MM failed request: " + vpxResponse.getHeader().getRequestResultDescription());
        }
        return errorCodes;
    }

    /**
     * @param assetId
     * @param mediafileId
     * @param userId
     * @param width
     * @return
     * @throws ServiceException
     */
    public LinkType getPlayLink(String assetId, String mediafileId, String userId, int width) throws ServiceException {
        String requestUrl = String.format("/asset/%s/play", assetId);
        String parameters = "mediafile_id=" + mediafileId + "&user_id=" + userId + "&response=object&width=" + width;
        requestUrl += "?" + parameters;
        LinkType playLink = null;
        Response vpxResponse = null;

        try {
            vpxResponse = doGetRequest(requestUrl);
        } catch (IOException e) {
            throw new ServiceException(e);
        }

        if (vpxResponse.getHeader().getRequestResultId() == ERRORCODE_OKAY) {

            ItemsType items = vpxResponse.getItems();
            if (!items.getLinkOrAssetOrAssetDetails().isEmpty()) {
                playLink = (LinkType) items.getLinkOrAssetOrAssetDetails().get(0);
            }
        } else {
            log.error("MM failed request: " + vpxResponse.getHeader().getRequestResultDescription());
        }
        return playLink;
    }


    /**
     * @param assetId
     * @param mediafileId
     * @param userId
     * @param responseType
     * @return
     * @throws ServiceException
     */
    public LinkType getPlayLink(String assetId, String mediafileId, String userId, String responseType) throws ServiceException {
        String requestUrl = String.format("/asset/%s/play", assetId);
        String parameters = "mediafile_id=" + mediafileId + "&user_id=" + userId + "&response=" + responseType;
        requestUrl += "?" + parameters;
        LinkType playLink = null;
        Response vpxResponse = null;

        try {
            vpxResponse = doGetRequest(requestUrl);
        } catch (IOException e) {
            throw new ServiceException(e);
        }

        if (vpxResponse.getHeader().getRequestResultId() == ERRORCODE_OKAY) {

            ItemsType items = vpxResponse.getItems();
            if (!items.getLinkOrAssetOrAssetDetails().isEmpty()) {
                playLink = (LinkType) items.getLinkOrAssetOrAssetDetails().get(0);
            }
        } else {
            log.error("MM failed request: " + vpxResponse.getHeader().getRequestResultDescription());
        }
        return playLink;
    }

    /**
     * @param assetId
     * @param mediafileId
     * @param userId
     * @return
     * @throws ServiceException
     */
    public LinkType getPlayLink(String assetId, String mediafileId, String userId) throws ServiceException {
        String requestUrl = String.format("/asset/%s/play", assetId);
        String parameters = "mediafile_id=" + mediafileId + "&user_id=" + userId;
        //parameters += "&is_app_admin=true";
        requestUrl += "?" + parameters;
        LinkType playLink = null;
        Response vpxResponse = null;

        try {
            vpxResponse = doGetRequest(requestUrl);
        } catch (IOException e) {
            throw new ServiceException(e);
        }

        if (vpxResponse.getHeader().getRequestResultId() == ERRORCODE_OKAY) {

            ItemsType items = vpxResponse.getItems();
            if (!items.getLinkOrAssetOrAssetDetails().isEmpty()) {
                playLink = (LinkType) items.getLinkOrAssetOrAssetDetails().get(0);
            }
        } else {
            log.error("MM failed request: " + vpxResponse.getHeader().getRequestResultDescription());
        }
        return playLink;
    }

    /**
     * @param assetId
     * @param userId
     * @return
     * @throws ServiceException
     */
    public LinkType getStillLink(String assetId, String userId) throws ServiceException {
        String requestUrl = String.format("/asset/%s/still", assetId);
        String parameters = "user_id=" + userId;
        // parameters += "&is_app_admin=true";
        requestUrl += "?" + parameters;
        LinkType stillLink = null;
        Response vpxResponse = null;

        try {
            vpxResponse = doGetRequest(requestUrl);
        } catch (IOException e) {
            throw new ServiceException(e);
        }

        if (vpxResponse.getHeader().getRequestResultId() == ERRORCODE_OKAY) {

            ItemsType items = vpxResponse.getItems();
            if (!items.getLinkOrAssetOrAssetDetails().isEmpty()) {
                stillLink = (LinkType) items.getLinkOrAssetOrAssetDetails().get(0);
            }
        } else {
            log.error("MM failed request: " + vpxResponse.getHeader().getRequestResultDescription());
        }
        return stillLink;
    }


    /**
     * @param mediafileId
     * @param userId
     * @return
     * @throws ServiceException
     */
    public UploadTicketType createUploadTicket(String mediafileId, String userId) throws ServiceException {
        return createUploadTicket(mediafileId, userId, false);
    }

    /**
     * @param mediafileId
     * @param userId
     * @param isStill
     * @return
     * @throws ServiceException
     */
    public UploadTicketType createUploadTicket(String mediafileId, String userId, boolean isStill) throws ServiceException {
        String requestUrl = String.format("/mediafile/%s/uploadticket/create", mediafileId);
        String parameters = "user_id=" + userId;

        if (isStill) {
            parameters += "&still_upload=TRUE";
        }
        // parameters += "&is_app_admin=true";
        requestUrl += "?" + parameters;
        UploadTicketType uploadTicket = null;
        Response vpxResponse = null;

        try {
            vpxResponse = doGetRequest(requestUrl);
        } catch (IOException e) {
            throw new ServiceException(e);
        }

        if (vpxResponse.getHeader().getRequestResultId() == ERRORCODE_OKAY) {

            ItemsType items = vpxResponse.getItems();
            if (!items.getLinkOrAssetOrAssetDetails().isEmpty()) {
                uploadTicket = (UploadTicketType) items.getLinkOrAssetOrAssetDetails().get(0);
            }
        } else {
            log.error("MM failed request: " + vpxResponse.getHeader().getRequestResultDescription());
        }
        return uploadTicket;
    }

    /**
     * @param userId
     * @return
     */
    public String createAsset(String userId) {
        String requestUrl = "/asset/create";
        String parameters = "user_id=" + userId;
        String assetId = null;
        Response vpxResponse = null;
        try {
            vpxResponse = doPostRequest(requestUrl, parameters);
            if (vpxResponse.getHeader().getRequestResultId() == ERRORCODE_OKAY) {
                ItemsType items = vpxResponse.getItems();
                if (!items.getLinkOrAssetOrAssetDetails().isEmpty()) {
                    AssetIdType id = (AssetIdType) items.getLinkOrAssetOrAssetDetails().get(0);
                    assetId = id.getAssetId();
                }
            } else {
                log.error("MM failed request: " + vpxResponse.getHeader().getRequestResultDescription());
            }
        } catch (IOException e) {
            log.error(e.getMessage());
        }
        return assetId;
    }

    /**
     * @param assetId
     * @param userId
     * @return
     */
    public String createMediafile(String assetId, String userId) {
        String requestUrl = "/mediafile/create";
        String parameters = "user_id=" + userId + "&asset_id=" + assetId;
        String mediafileId = null;
        Response vpxResponse = null;
        try {
            vpxResponse = doPostRequest(requestUrl, parameters);
            if (vpxResponse.getHeader().getRequestResultId() == ERRORCODE_OKAY) {
                ItemsType items = vpxResponse.getItems();
                if (!items.getLinkOrAssetOrAssetDetails().isEmpty()) {
                    MediafileType mediafile = (MediafileType) items.getLinkOrAssetOrAssetDetails().get(0);
                    mediafileId = mediafile.getMediafileId();
                }
            } else {
                log.error("MM failed request: " + vpxResponse.getHeader().getRequestResultDescription());
            }
        } catch (IOException e) {
            log.error(e.getMessage());
        }
        return mediafileId;
    }

    /**
     * @param assetId
     * @param userId
     * @param metadata
     * @return
     */
    public Response setMetadata(String assetId, String userId, Map metadata) {
        String requestUrl = String.format("/asset/%s/metadata", assetId);
        String parameters = "user_id=" + userId;

        // iterate over key value pairs
        Iterator it = metadata.entrySet().iterator();
        StringBuilder sb = new StringBuilder(parameters);
        while (it.hasNext()) {
            Map.Entry pairs = (Map.Entry) it.next();
            sb.append('&').append(pairs.getKey()).append('=').append(pairs.getValue());
        }
        parameters = sb.toString();

        Response vpxResponse = null;
        try {
            vpxResponse = doPostRequest(requestUrl, parameters);
        } catch (IOException e) {
            log.error(e.getMessage());
        }
        return vpxResponse;
    }

    // is dit verwijderen van metadata of van asset

    /**
     * @param assetId
     * @param userId
     * @throws ServiceException
     */
    public void deleteMetadata(String assetId, String userId) throws ServiceException {
        String requestUrl = String.format("/asset/%s/delete", assetId);
        String parameters = "user_id=" + userId;
        Response vpxResponse = null;
        try {
            vpxResponse = doPostRequest(requestUrl, parameters);
        } catch (IOException e) {
            log.error(e.getMessage());
        }

        if (vpxResponse.getHeader().getRequestResultId() != ERRORCODE_OKAY) {
            log.error("MM failed request: " + vpxResponse.getHeader().getRequestResultDescription());
            throw new ServiceException(vpxResponse.getHeader().getRequestResultDescription());
        }
    }

    // cascade (true) verwijdert alle onderliggende items (mediafiles, stills, koppelingen met collections)

    /**
     * @param assetId
     * @param userId
     * @param cascade
     * @throws ServiceException
     */
    public void deleteAsset(String assetId, String userId, boolean cascade) throws ServiceException {
        String requestUrl = String.format("/asset/%s/delete", assetId);
        String parameters = "user_id=" + userId;
        if (cascade) {
            parameters += "&delete=cascade";
        }
        Response vpxResponse = null;
        try {
            vpxResponse = doPostRequest(requestUrl, parameters);
        } catch (IOException e) {
            log.error(e.getMessage());
        }

        if (vpxResponse.getHeader().getRequestResultId() != ERRORCODE_OKAY) {
            log.error("MM failed request: " + vpxResponse.getHeader().getRequestResultDescription());
            throw new ServiceException(vpxResponse.getHeader().getRequestResultDescription());
        }
    }

    /**
     * @param mediafileId
     * @param userId
     * @throws ServiceException
     */
    public void deleteMediafile(String mediafileId, String userId) throws ServiceException {
        String requestUrl = String.format("/mediafile/%s/delete", mediafileId);
        String parameters = "user_id=" + userId;
        Response vpxResponse = null;
        try {
            vpxResponse = doPostRequest(requestUrl, parameters);
        } catch (IOException e) {
            log.error(e.getMessage());
        }
        if (vpxResponse.getHeader().getRequestResultId() != ERRORCODE_OKAY) {
            log.error("MM failed request: " + vpxResponse.getHeader().getRequestResultDescription());
            throw new ServiceException(vpxResponse.getHeader().getRequestResultDescription());
        }
    }

    /**
     * @param mediafileId
     * @param userId
     * @param properties
     * @return
     */
    public Response updateMediafile(String mediafileId, String userId, Map properties) {
        String requestUrl = "/mediafile/" + mediafileId;
        String parameters = "user_id=" + userId;
        Response vpxResponse = null;

        if (properties != null) {
            // if uri is set, ignore filename and is_downloablde
            if (properties.containsKey(MediafileProperties.URI)) {
                properties.remove(MediafileProperties.FILENAME);
                properties.remove(MediafileProperties.ISDOWNLOADABLE);
            }

            // if filename or is_ downloadable is set, ignore uri
            if (properties.containsKey(MediafileProperties.FILENAME) || properties.containsKey(MediafileProperties.ISDOWNLOADABLE)) {
                properties.remove(MediafileProperties.URI);
            }

            // iterate over key value pairs
            Iterator it = properties.entrySet().iterator();
            StringBuilder sb = new StringBuilder();
            while (it.hasNext()) {
                sb.append(parameters).append('&');
                Map.Entry pairs = (Map.Entry) it.next();
                sb.append(pairs.getKey()).append('=').append(pairs.getValue());
            }
            parameters = sb.toString();
        }

        try {
            vpxResponse = doPostRequest(requestUrl, parameters);
        } catch (IOException e) {
            log.error(e.getMessage());
        }
        return vpxResponse;
    }

    /**
     * @param mediafileId
     * @return
     */
    public MediafileDetailsType getMediafileDetails(String mediafileId) {
        String requestUrl = "/mediafile/" + mediafileId;
        MediafileDetailsType mediafileDetails = null;
        Response vpxResponse = null;

        try {
            vpxResponse = doGetRequest(requestUrl);
            if (vpxResponse.getHeader().getRequestResultId() == ERRORCODE_OKAY) {
                ItemsType items = vpxResponse.getItems();
                if (!items.getLinkOrAssetOrAssetDetails().isEmpty()) {
                    mediafileDetails = (MediafileDetailsType) items.getLinkOrAssetOrAssetDetails().get(0);
                }
            }
        } catch (IOException e) {
            log.error(e.getMessage());
        }
        return mediafileDetails;
    }

    /**
     * @param year
     * @param month
     * @param type
     * @param limit
     * @param offset
     * @return
     * @throws ServiceException
     */
    public List<StatsDatausagevideoType> getStatsDatausageVideo(int year, int month, String type, int limit, int offset) throws ServiceException {
        String requestUrl = "/statistics/datausagevideo";
        String parameters = "limit=" + LIMIT;
        requestUrl += "?" + parameters + "&year=" + year + "&month=" + month + "&type=" + type;
        List<StatsDatausagevideoType> datausagevideo = null;
        Response vpxResponse = null;

        try {
            vpxResponse = doGetRequest(requestUrl);
            if (vpxResponse.getHeader().getRequestResultId() == ERRORCODE_OKAY) {
                ItemsType items = vpxResponse.getItems();
                datausagevideo = new ArrayList<StatsDatausagevideoType>();

                for (Object o : items.getLinkOrAssetOrAssetDetails()) {
                    if (o instanceof StatsDatausagevideoType) {
                        datausagevideo.add((StatsDatausagevideoType) o);
                    }
                }
            } else {
                log.error("MM failed request: " + vpxResponse.getHeader().getRequestResultDescription());
            }
        } catch (IOException e) {
            throw new ServiceException(e);
        }
        return datausagevideo;
    }

    /**
     * @return
     * @throws ServiceException
     */
    public List<ProfileType> getProfiles() throws ServiceException {
        String requestUrl = "/transcode/profiles";
        List<ProfileType> profiles = null;
        Response vpxResponse = null;

        try {
            vpxResponse = doGetRequest(requestUrl);
        } catch (IOException e) {
            throw new ServiceException(e);
        }

        if (vpxResponse.getHeader().getRequestResultId() == ERRORCODE_OKAY) {
            ItemsType items = vpxResponse.getItems();
            profiles = new ArrayList<ProfileType>();

            for (Object o : items.getLinkOrAssetOrAssetDetails()) {
                if (o instanceof ProfileType) {
                    profiles.add((ProfileType) o);
                }
            }
        } else {
            log.error("MM failed request: " + vpxResponse.getHeader().getRequestResultDescription());
        }
        return profiles;
    }

    /**
     * @param cql
     * @return
     * @throws ServiceException
     */
    public List<AssetType> getCqlAssets(String cql) throws ServiceException {
        String requestUrl = "/asset";
        String parameters = "limit=" + LIMIT;
        List<AssetType> assets = new ArrayList<AssetType>();
        Response vpxResponse = null;
        String query = "";

        try {
            query = URLEncoder.encode(cql, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            log.error(e.getMessage());
        }
        StringBuilder sb = new StringBuilder();
        sb.append(parameters).append("&cql=").append(query);
        parameters = sb.toString();
        requestUrl += "?" + parameters;
        try {
            vpxResponse = doGetRequest(requestUrl);
        } catch (IOException e) {
            throw new ServiceException(e);
        }

        if (vpxResponse.getHeader().getRequestResultId() == ERRORCODE_OKAY) {
            ItemsType items = vpxResponse.getItems();

            for (Object o : items.getLinkOrAssetOrAssetDetails()) {
                if (o instanceof AssetType) {
                    assets.add((AssetType) o);
                }
            }
        } else {
            log.error("MM failed request: " + vpxResponse.getHeader().getRequestResultDescription());
        }
        return assets;
    }

    /**
     * @param str
     * @return
     */
    public static boolean isEmpty(String str) {
        return str == null || str.length() == 0;
    }

    /**
     * @param owner
     * @param group
     * @return
     * @throws ServiceException
     */
    public long getAssetCount(String owner, String group) throws ServiceException {
        String requestUrl = "/asset/count";
        Response vpxResponse = null;

        StringBuilder sb = new StringBuilder();
        if (!isEmpty(owner)) {
            sb.append(String.format("owner_id=%s&", owner));
        }
        if (!isEmpty(group)) {
            sb.append(String.format("group_id=%s", group));
        }

        requestUrl += "?" + sb.toString();
        try {
            vpxResponse = doGetRequest(requestUrl);
        } catch (IOException e) {
            throw new ServiceException(e);
        }

        if (vpxResponse.getHeader().getRequestResultId() == ERRORCODE_OKAY) {
            return vpxResponse.getHeader().getItemCountTotal();
        } else {
            log.error("MM failed request: " + vpxResponse.getHeader().getRequestResultDescription());
        }
        return 0;
    }

    /**
     * @param assetId
     * @param mediafileId
     * @param userId
     * @param properties
     * @return
     * @throws IOException
     * @throws ServiceException
     */
    public JobType createStill(String assetId, String mediafileId, String userId, Map properties) throws IOException, ServiceException {
        String requestUrl = String.format("/asset/%s/still/create", assetId);
        String parameters = String.format("user_id=%s&mediafile_id=%s", userId, mediafileId);

        Response vpxResponse = null;

        if (properties != null) {
            // iterate over key value pairs
            Iterator it = properties.entrySet().iterator();
            StringBuilder sb = new StringBuilder();
            while (it.hasNext()) {
                sb.append('&');
                Map.Entry pairs = (Map.Entry) it.next();
                sb.append(pairs.getKey()).append('=').append(pairs.getValue());
            }
            parameters = parameters + sb.toString();
        }

        try {
            vpxResponse = doPostRequest(requestUrl, parameters);
        } catch (IOException e) {
            throw new ServiceException(e);
        }

        return (JobType) vpxResponse.getItems().getLinkOrAssetOrAssetDetails().get(0);
    }

    /**
     * @param assetId
     * @param mediafileId
     * @param userId
     * @param properties
     * @return
     * @throws IOException
     * @throws ServiceException
     */
    public Response deleteStill(String assetId, String mediafileId, String userId, Map properties) throws IOException, ServiceException {
        String requestUrl = String.format("/asset/%s/still/delete", assetId);
        String parameters = String.format("user_id=%s&mediafile_id=%s", userId, mediafileId);

        Response vpxResponse = null;

        if (properties != null) {
            // iterate over key value pairs
            Iterator it = properties.entrySet().iterator();
            StringBuilder sb = new StringBuilder();
            while (it.hasNext()) {
                sb.append('&');
                Map.Entry pairs = (Map.Entry) it.next();
                sb.append(pairs.getKey()).append('=').append(pairs.getValue());
            }
            parameters = parameters + sb.toString();
        }


        try {
            vpxResponse = doPostRequest(requestUrl, parameters);
        } catch (IOException e) {
            throw new ServiceException(e);
        }

        return vpxResponse;
    }

    /**
     * @param uploadHost
     * @param assetId
     * @param properties
     * @param stream
     * @param mimeType
     * @param fileName
     * @return
     */
    public Response uploadStill(String uploadHost, String assetId, Map properties, InputStream stream, String mimeType, String fileName) {
        String requestUrl = String.format("/asset/%s/still/upload", assetId);
        String parameters = "";

        if (properties != null) {
            // iterate over key value pairs
            Iterator it = properties.entrySet().iterator();
            StringBuilder sb = new StringBuilder();
            while (it.hasNext()) {
                sb.append(parameters).append('&');
                Map.Entry pairs = (Map.Entry) it.next();
                sb.append(pairs.getKey()).append('=').append(pairs.getValue());
            }
            parameters = sb.toString();
        }

        Response vpxResponse = null;
        try {
            vpxResponse = doPostRequest(uploadHost, requestUrl, parameters, stream, mimeType, fileName);
            if (vpxResponse.getHeader().getRequestResultId() != ERRORCODE_OKAY) {
                log.error("MM failed request: " + vpxResponse.getHeader().getRequestResultDescription());
            }
        } catch (IOException e) {
            log.error(e.getMessage());
        }
        return vpxResponse;
    }

    /**
     * @param assetId
     * @param userId
     * @param properties
     * @return
     * @throws ServiceException
     */
    public StillType getStills(String assetId, String userId, Map properties) throws ServiceException {
        String requestUrl = String.format("/asset/%s/still", assetId);
        String parameters = String.format("user_id=%s", userId);

        Response vpxResponse = null;

        if (properties != null) {
            // iterate over key value pairs
            Iterator it = properties.entrySet().iterator();
            StringBuilder sb = new StringBuilder();
            while (it.hasNext()) {
                sb.append('&');
                Map.Entry pairs = (Map.Entry) it.next();
                sb.append(pairs.getKey()).append('=').append(pairs.getValue());
            }
            parameters = parameters + sb.toString();
        }

        requestUrl += "?" + parameters;

        try {
            vpxResponse = doGetRequest(requestUrl);
        } catch (IOException e) {
            throw new ServiceException(e);
        }

        if (vpxResponse.getHeader().getRequestResultId() == ERRORCODE_OKAY) {
            return (StillType) vpxResponse.getItems().getLinkOrAssetOrAssetDetails().get(0);
        } else if (vpxResponse.getHeader().getRequestResultId() == ERRORCODE_COULD_NOT_FIND_STILL) {
            return new StillType();
        } else {
            log.error("MM failed request: " + vpxResponse.getHeader().getRequestResultDescription());
        }

        return null;
    }

    /**
     * @param assetId
     * @param userId
     * @param properties
     * @return
     * @throws ServiceException
     */
    public Response setDefaultStill(String assetId, String userId, Map properties) throws ServiceException {
        String requestUrl = String.format("/asset/%s/still/default", assetId);
        String parameters = String.format("user_id=%s", userId);

        Response vpxResponse = null;

        if (properties != null) {
            // iterate over key value pairs
            Iterator it = properties.entrySet().iterator();
            StringBuilder sb = new StringBuilder();
            while (it.hasNext()) {
                sb.append('&');
                Map.Entry pairs = (Map.Entry) it.next();
                sb.append(pairs.getKey()).append('=').append(pairs.getValue());
            }
            parameters = parameters + sb.toString();
        }
        try {
            vpxResponse = doPostRequest(requestUrl, parameters);
        } catch (IOException e) {
            throw new ServiceException(e);
        }

        return vpxResponse;
    }

    /**
     * @param jobId
     * @param userId
     * @return
     * @throws IOException
     * @throws ServiceException
     */
    public JobDetailsType getJobStatus(String jobId, String userId) throws IOException, ServiceException {
        String requestUrl = String.format("/job/%s/status", jobId);
        String parameters = String.format("user_id=%s", userId);

        requestUrl += "?" + parameters;

        Response vpxResponse = null;

        try {
            vpxResponse = doGetRequest(requestUrl);
        } catch (IOException e) {
            throw new ServiceException(e);
        }

        return (JobDetailsType) vpxResponse.getItems().getLinkOrAssetOrAssetDetails().get(0);
    }


    /**
     * @param year
     * @param month
     * @param type
     * @param groupId
     * @param ownerId
     * @param limit
     * @param offset
     * @return
     * @throws ServiceException
     */
    public List<StatsPlayedstreamsType> getStatsPlayedStreams(int year, int month, String type, String groupId, String ownerId, int limit, int offset) throws ServiceException {
        String requestUrl = "/statistics/playedstreams";
        String parameters = "limit=" + LIMIT;
        requestUrl += "?" + parameters + "&year=" + year + "&month=" + month + "&play_type=" + type;
        if (groupId != null && !"".equals(groupId)) {
            requestUrl += "&group_id=" + groupId;
        }
        if (ownerId != null && !"".equals(ownerId)) {
            requestUrl += "&owner_id=" + ownerId;
        }

        List<StatsPlayedstreamsType> playedStreams = null;
        Response vpxResponse = null;

        try {
            vpxResponse = doGetRequest(requestUrl);
            if (vpxResponse.getHeader().getRequestResultId() == ERRORCODE_OKAY) {
                ItemsType items = vpxResponse.getItems();
                playedStreams = new ArrayList<StatsPlayedstreamsType>();

                for (Object o : items.getLinkOrAssetOrAssetDetails()) {
                    if (o instanceof StatsPlayedstreamsType) {
                        playedStreams.add((StatsPlayedstreamsType) o);
                    }
                }
            } else {
                log.error("MM failed request: " + vpxResponse.getHeader().getRequestResultDescription());
            }
        } catch (IOException e) {
            throw new ServiceException(e);
        }
        return playedStreams;
    }


    /**
     * @param limit
     * @param offset
     * @return
     * @throws ServiceException
     */
    public List<StatsPopularstreamsType> getStatsPopularStreams(int limit, int offset) throws ServiceException {
        String requestUrl = "/statistics/popularmediafiles";
        String parameters = "limit=" + LIMIT;
        requestUrl += "?" + parameters;
        List<StatsPopularstreamsType> popularStreams = null;
        Response vpxResponse = null;

        try {
            vpxResponse = doGetRequest(requestUrl);
            if (vpxResponse.getHeader().getRequestResultId() == ERRORCODE_OKAY) {
                ItemsType items = vpxResponse.getItems();
                popularStreams = new ArrayList<StatsPopularstreamsType>();

                for (Object o : items.getLinkOrAssetOrAssetDetails()) {
                    if (o instanceof StatsPopularstreamsType) {
                        popularStreams.add((StatsPopularstreamsType) o);
                    }
                }
            } else {
                log.error("MM failed request: " + vpxResponse.getHeader().getRequestResultDescription());
            }
        } catch (IOException e) {
            throw new ServiceException(e);
        }
        return popularStreams;
    }


    /**
     * @return
     * @throws ServiceException
     */
    public List<StatsPopularcollectionsType> getStatsPopularCollections() throws ServiceException {
        String requestUrl = "/statistics/popularcollections";
        List<StatsPopularcollectionsType> popularCollections = null;
        Response vpxResponse = null;

        try {
            vpxResponse = doGetRequest(requestUrl);
            if (vpxResponse.getHeader().getRequestResultId() == ERRORCODE_OKAY) {
                ItemsType items = vpxResponse.getItems();
                popularCollections = new ArrayList<StatsPopularcollectionsType>();

                for (Object o : items.getLinkOrAssetOrAssetDetails()) {
                    if (o instanceof StatsPopularcollectionsType) {
                        popularCollections.add((StatsPopularcollectionsType) o);
                    }
                }
            } else {
                log.error("MM failed request: " + vpxResponse.getHeader().getRequestResultDescription());
            }
        } catch (IOException e) {
            throw new ServiceException(e);
        }
        return popularCollections;
    }

    /**
     * @param year
     * @param month
     * @param userId
     * @param groupId
     * @param limit
     * @param offset
     * @return
     * @throws ServiceException
     */
    public List<StatsDatauploadType> getStatsDataUpload(int year, int month, String userId, String groupId, int limit, int offset) throws ServiceException {
        String requestUrl = "/statistics/dataupload";
        String parameters = "limit=" + LIMIT;
        requestUrl += "?" + parameters + "&year=" + year + "&month=" + month;
        if (groupId != null && !"".equals(groupId)) {
            requestUrl += "&group_id=" + groupId;
        }
        if (userId != null && !"".equals(userId)) {
            requestUrl += "&user_id=" + userId;
        }
        List<StatsDatauploadType> dataUploads = null;
        Response vpxResponse = null;

        try {
            vpxResponse = doGetRequest(requestUrl);
            if (vpxResponse.getHeader().getRequestResultId() == ERRORCODE_OKAY) {
                ItemsType items = vpxResponse.getItems();
                dataUploads = new ArrayList<StatsDatauploadType>();

                for (Object o : items.getLinkOrAssetOrAssetDetails()) {
                    if (o instanceof StatsDatauploadType) {
                        dataUploads.add((StatsDatauploadType) o);
                    }
                }
            } else {
                log.error("MM failed request: " + vpxResponse.getHeader().getRequestResultDescription());
            }
        } catch (IOException e) {
            throw new ServiceException(e);
        }
        return dataUploads;
    }


    /**
     * @param url
     * @param properties
     * @param parameters
     * @return
     * @throws IOException
     */
    public Response doPostRequestWithParameters(String url, Map properties, Map parameters) throws IOException {
        RestTemplate template = new RestTemplate();
        String uri = template.getForObject(url, String.class, properties);
        String paramstring = "";

        if (parameters != null) {
            // iterate over key value pairs
            Iterator it = parameters.entrySet().iterator();
            StringBuilder sb = new StringBuilder();
            while (it.hasNext()) {
                sb.append('&');
                Map.Entry pairs = (Map.Entry) it.next();
                sb.append(pairs.getKey()).append('=').append(pairs.getValue());
            }
            paramstring = sb.toString();
        }

        return doPostRequest(uri, paramstring);
    }

    /**
     * @param url
     * @param properties
     * @param parameters
     * @return
     * @throws IOException
     */
    public Response doGetRequestWithParameters(String url, Map properties, Map parameters) throws IOException {
        RestTemplate template = new RestTemplate();
        String uri = template.getForObject(url, String.class, properties);
        String paramstring = "";

        if (parameters != null) {
            // iterate over key value pairs
            Iterator it = parameters.entrySet().iterator();
            StringBuilder sb = new StringBuilder();
            while (it.hasNext()) {
                sb.append('&');
                Map.Entry pairs = (Map.Entry) it.next();
                sb.append(pairs.getKey()).append('=').append(pairs.getValue());
            }
            paramstring += "?" + sb.toString();
        }

        return doGetRequest(uri + paramstring);
    }

}
