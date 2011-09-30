package org.onehippo.forge.externalresource.api;

import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.entity.mime.content.InputStreamBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.util.EntityUtils;
import org.hippoecm.frontend.editor.plugins.resource.ResourceHelper;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.onehippo.forge.externalresource.resize.ImageProcessorRule;
import org.onehippo.forge.externalresource.resize.ResizeToFitResizeRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.jcr.Node;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.Calendar;

/**
 * @version $Id$
 */

public class HippoRedFiveResourceManager extends ResourceManager {
    @SuppressWarnings({"UnusedDeclaration"})
    private static Logger log = LoggerFactory.getLogger(HippoRedFiveResourceManager.class);

    private String url;
    private String username;
    private String password;

    public HippoRedFiveResourceManager(IPluginConfig config) {
        super(config);
        if (config.containsKey("url")) {
            this.url = config.getString("url");
        }
        if (config.containsKey("username")) {
            this.username = config.getString("username");
        }
        if (config.containsKey("password")) {
            this.password = config.getString("password");
        }
    }

    @Override
    public void create(Node node, InputStream istream, String mimetype) throws Exception {

        String response = submitFile(istream, getUrl(), mimetype, node.getName());

        Document document = parseXmlFile(response, true);

        String videoUrl = document.getElementsByTagName("url").item(0).getTextContent();
        String imageUrl = document.getElementsByTagName("image").item(0).getTextContent();

        org.apache.commons.httpclient.HttpClient client = new org.apache.commons.httpclient.HttpClient();
        HttpMethod getMethod = new GetMethod(imageUrl);

        client.executeMethod(getMethod);

        InputStream is = getMethod.getResponseBodyAsStream();
        String mimeType = getMethod.getResponseHeader("content-type").getValue();

        Node preview;
        if (node.hasNode("hippoexternal:preview")) {
            preview = node.getNode("hippoexternal:preview");
        } else {
            preview = node.addNode("hippoexternal:preview", "hippo:resource");
        }

        preview.setProperty("jcr:data", ResourceHelper.getValueFactory(node).createBinary(is));
        preview.setProperty("jcr:mimeType", mimeType);
        preview.setProperty("jcr:lastModified", Calendar.getInstance());

        Node thumbnail;
        if (node.hasNode("hippoexternal:thumbnail")) {
            thumbnail = node.getNode("hippoexternal:thumbnail");
        } else {
            thumbnail = node.addNode("hippoexternal:thumbnail", "hippo:resource");
        }

        InputStream stream = preview.getProperty("jcr:data").getBinary().getStream();

        ImageProcessorRule rule = new ResizeToFitResizeRule(60, 60);
        InputStream out = createThumbnailWithResizeRule(stream, rule, mimeType);
        thumbnail.setProperty("jcr:data", ResourceHelper.getValueFactory(node).createBinary(out));
        thumbnail.setProperty("jcr:mimeType", mimeType);
        thumbnail.setProperty("jcr:lastModified", Calendar.getInstance());

        node.setProperty("hipporedfive:url", videoUrl);
    }

    @Override
    public void afterSave(Node node) {
    }

    @Override
    public void delete(Node node) {
    }



    public static String submitFile(final InputStream inputStream, final String serverUrl, String mimeType, String fileName) throws Exception {
        HttpClient httpclient = new DefaultHttpClient();
        httpclient.getParams().setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);

        HttpPost httppost = new HttpPost(serverUrl);
        MultipartEntity mpEntity = new MultipartEntity();
        ContentBody cbFile = new InputStreamBody(inputStream, mimeType, fileName);

        mpEntity.addPart("file", cbFile);
        httppost.setEntity(mpEntity);
        HttpResponse response = httpclient.execute(httppost);
        HttpEntity resEntity = response.getEntity();
        String red5Url = EntityUtils.toString(resEntity);
        log.debug("Status {}", response.getStatusLine());
        httpclient.getConnectionManager().shutdown();
        return red5Url;
    }

    public static Document parseXmlFile(String string, boolean validating) {
        try {
            // Create a builder factory
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setValidating(validating);

            // Create the builder and parse the file
            Document doc = factory.newDocumentBuilder().parse(new InputSource(new StringReader(string)));
            return doc;
        } catch (SAXException e) {
            // A parsing error occurred; the xml input is not valid
        } catch (ParserConfigurationException e) {
        } catch (IOException e) {
        }
        return null;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

}
