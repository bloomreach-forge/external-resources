package org.onehippo.forge.externalresource.api.scheduler.mediamosa;

import nl.uva.mediamosa.model.AssetDetailsType;
import nl.uva.mediamosa.util.ServiceException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.onehippo.forge.externalresource.api.MediamosaRemoteService;
import org.onehippo.forge.externalresource.api.utils.MediaMosaServices;
import org.onehippo.repository.scheduling.RepositoryJob;
import org.onehippo.repository.scheduling.RepositoryJobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import java.io.IOException;
import java.io.InputStream;

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
                        //Utils.resolveThumbnailToVideoNode(imageUrl, mediamosaAsset);
                        org.apache.commons.httpclient.HttpClient client = new org.apache.commons.httpclient.HttpClient();
                        HttpMethod getMethod = new GetMethod(imageUrl);
                        InputStream is = null;
                        try {
                            client.executeMethod(getMethod);
                            is = getMethod.getResponseBodyAsStream();
                            String mimeType = getMethod.getResponseHeader("content-type").getValue();
                            if (mimeType.startsWith("image")) {
                                if (mediamosaAsset.hasNode("hippoexternal:thumbnail")) {
                                    Node thumbnail = mediamosaAsset.getNode("hippoexternal:thumbnail");
                                    thumbnail.setProperty("jcr:data", session.getValueFactory().createBinary(is));
                                    thumbnail.setProperty("jcr:mimeType", mimeType);
                                    thumbnail.setProperty("jcr:lastModified", java.util.Calendar.getInstance());
                                    // mediamosaAsset.setProperty("hippoexternal:state", SynchronizationState.SYNCHRONIZED.getState());
                                    session.save();
                                }
                            }
                        } catch (IOException e) {
                            LOG.error(e.getMessage(), e);
                        } finally {
                            IOUtils.closeQuietly(is);
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
