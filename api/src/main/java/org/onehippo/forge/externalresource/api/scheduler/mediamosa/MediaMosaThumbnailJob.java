package org.onehippo.forge.externalresource.api.scheduler.mediamosa;

import java.io.IOException;
import java.io.InputStream;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.onehippo.forge.externalresource.api.MediamosaRemoteService;
import org.onehippo.forge.externalresource.api.utils.MediaMosaServices;
import org.onehippo.forge.externalresource.api.utils.Utils;
import org.onehippo.repository.scheduling.RepositoryJob;
import org.onehippo.repository.scheduling.RepositoryJobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nl.uva.mediamosa.model.AssetDetailsType;
import nl.uva.mediamosa.model.LinkType;
import nl.uva.mediamosa.util.ServiceException;

/**
 * @version $Id$
 */
public class MediaMosaThumbnailJob implements RepositoryJob {

    private static final Logger LOG = LoggerFactory.getLogger(MediaMosaThumbnailJob.class);
    private static final String RESOURCE_QUERY_STRING = "content/videos//element(*,hippomediamosa:resource)[@hippomediamosa:assetid='%s']";
    public static final String ASSET_ID_ATTRIBUTE = "assetId";

    @Override
    public void execute(RepositoryJobExecutionContext context) throws RepositoryException {
        Session session = null;
        try {

            String assetId = context.getAttribute(ASSET_ID_ATTRIBUTE);

            session = context.createSystemSession();
            QueryManager queryManager = session.getWorkspace().getQueryManager();
            Query query = queryManager.createQuery(String.format(RESOURCE_QUERY_STRING, assetId), Query.XPATH);
            NodeIterator it = query.execute().getNodes();
            while (it.hasNext()) {
                Node mediamosaAsset = it.nextNode();
                MediamosaRemoteService mediamosaRemoteService = MediaMosaServices.forNode(mediamosaAsset).getMediamosaRemoteService();
                if (mediamosaRemoteService != null) {
                    AssetDetailsType detail = mediamosaRemoteService.service().getAssetDetails(assetId);
                    if (StringUtils.isNotBlank(detail.getVpxStillUrl())) {
                        String imageUrl = detail.getVpxStillUrl();
                        HttpClient client = Utils.getHttpClient();
                        InputStream is = null;
                        try {
                            HttpResponse httpResponse = client.execute(new HttpGet(imageUrl));
                            is = httpResponse.getEntity().getContent();
                            String mimeType = httpResponse.getFirstHeader("content-type").getValue();
                            if (mimeType.startsWith("image")) {
                                if (mediamosaAsset.hasNode("hippoexternal:thumbnail")) {
                                    Node thumbnail = mediamosaAsset.getNode("hippoexternal:thumbnail");
                                    thumbnail.setProperty("jcr:data", session.getValueFactory().createBinary(is));
                                    thumbnail.setProperty("jcr:mimeType", mimeType);
                                    thumbnail.setProperty("jcr:lastModified", java.util.Calendar.getInstance());
                                    session.save();
                                }
                            }
                        } catch (IOException e) {
                            LOG.error(e.getMessage(), e);
                        } finally {
                            IOUtils.closeQuietly(is);
                        }

                        if ( mediamosaAsset.hasProperty("hippomediamosa:mediaid") && mediamosaAsset.hasProperty("hippoexternal:width") && mediamosaAsset.hasNode("hippoexternal:embedded")) {
                            String mediaFileId = mediamosaAsset.getProperty("hippomediamosa:mediaid").getString();
                            int width = Integer.parseInt(mediamosaAsset.getProperty("hippoexternal:width").getString());
                            LinkType embedLink = mediamosaRemoteService.service().getPlayLink(assetId, mediaFileId, mediamosaRemoteService.getUsername(), width);
                            if (embedLink != null && embedLink.getOutput() != null) {
                                Utils.addEmbeddedNode(mediamosaAsset, embedLink.getOutput());
                                session.save();
                            }
                        }
                    }
                }
            }
        } catch (ServiceException | IOException e) {
            LOG.error(e.getMessage(), e);
        } finally {
            if (session != null && session.isLive()) {
                session.logout();
            }
        }
    }
}
