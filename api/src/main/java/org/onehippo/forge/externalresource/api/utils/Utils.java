package org.onehippo.forge.externalresource.api.utils;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.TrustStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Calendar;

/**
 * @version $Id$
 */
public class Utils {
    @SuppressWarnings({"UnusedDeclaration"})
    private static Logger log = LoggerFactory.getLogger(Utils.class);

    private static final String HIPPOEXTERNAL_EMBEDDED = "hippoexternal:embedded";

    public static void addEmbeddedNode(Node node, String embedded) {
        try {
            Node embeddedNode;
            if (node.hasNode(HIPPOEXTERNAL_EMBEDDED)) {
                embeddedNode = node.getNode(HIPPOEXTERNAL_EMBEDDED);
            } else {
                embeddedNode = node.addNode(HIPPOEXTERNAL_EMBEDDED, HIPPOEXTERNAL_EMBEDDED);
            }
            embeddedNode.setProperty(HIPPOEXTERNAL_EMBEDDED, embedded);
        } catch (RepositoryException e) {
            log.error("", e);
        }
    }

    public static boolean resolveThumbnailToVideoNode(String imageUrl, Node node) {
        try {
            if (node.isNodeType("hippoexternal:video")) {
                HttpClient client = getHttpClient();
                InputStream is = null;
                try {
                    HttpResponse httpResponse = client.execute(new HttpGet(imageUrl));
                    is = httpResponse.getEntity().getContent();
                    String mimeType = httpResponse.getFirstHeader("content-type").getValue();
                    if (mimeType.startsWith("image")) {
                        if (node.hasNode("hippoexternal:thumbnail")) {
                            Node thumbnail = node.getNode("hippoexternal:thumbnail");
                            thumbnail.setProperty("jcr:data", node.getSession().getValueFactory().createBinary(is));
                            thumbnail.setProperty("jcr:mimeType", mimeType);
                            thumbnail.setProperty("jcr:lastModified", Calendar.getInstance());
                            node.getSession().save();
                            return true;
                        }
                    }
                } catch (IOException e) {
                    log.error("", e);
                } finally {
                    IOUtils.closeQuietly(is);
                }
            }
        } catch (RepositoryException e) {
            log.error("", e);
        }
        return false;
    }

    public static HttpClient getHttpClient() {
        HttpClientBuilder b = HttpClientBuilder.create();

        SSLContext sslContext = null;
        try {
            sslContext = new SSLContextBuilder().loadTrustMaterial(null, new TrustStrategy() {
                public boolean isTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
                    return true;
                }
            }).build();
        } catch (KeyManagementException | NoSuchAlgorithmException | KeyStoreException e) {
            e.printStackTrace();
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

}
