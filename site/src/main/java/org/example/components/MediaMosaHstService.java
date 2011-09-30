package org.example.components;

import com.whirlycott.cache.Cache;
import com.whirlycott.cache.CacheException;
import com.whirlycott.cache.CacheManager;
import nl.uva.mediamosa.MediaMosaService;
import nl.uva.mediamosa.model.AssetDetailsType;
import nl.uva.mediamosa.model.LinkType;
import nl.uva.mediamosa.model.MediafileDetailsType;
import nl.uva.mediamosa.util.ServiceException;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.config.impl.JcrPluginConfig;
import org.hippoecm.hst.core.component.HstRequest;
import org.onehippo.forge.externalresource.api.HippoMediaMosaResourceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.ServletException;
import java.io.IOException;

public class MediaMosaHstService {

    public static final Logger log = LoggerFactory.getLogger(MediaMosaHstService.class);

    private static String location = "/hippo:configuration/hippo:frontend/cms/cms-services/videogalleryService/hippomediamosa:resource";

    public static String getPlayLink(HstRequest request, String assetId) {
        try {
            String player = "";
            if (org.apache.commons.lang.StringUtils.isNotBlank(assetId)) {
                if (cacheRetrieve(assetId) == null) {
                    Session session = request.getRequestContext().getSession();
                    HippoMediaMosaResourceManager manager = new HippoMediaMosaResourceManager(new JcrPluginConfig(new JcrNodeModel(session.getNode(location))));

                    MediaMosaService service = manager.getMediaMosaService();
                    service.getAssetDetails(assetId);
                    AssetDetailsType assetDetails = null;
                    try {
                        assetDetails = service.getAssetDetails(assetId);
                        // MediafileDetails mediafileDetails = service.getMediafileDetails(mediafileId);
                    } catch (ServiceException e) {
                        ServletException exception = new ServletException(e.getMessage(), e);
                    }
                    // does asset exist or is it deleted?
                    if (assetDetails == null) {
                        player = String.format("Sorry, the requested asset (%s) is unavailable. It was removed by the owner.", assetId);
                        // asset still exists
                    } else {
                        // MediafileDetailsType mediafileDetails = assetDetails.getMediafiles().getMediafile().get(0);
                        MediafileDetailsType mediafileDetails = null;
                        if (assetDetails.getMediafiles().getMediafile().size() > 1) {
                            // transcoded version, used for instance when orignal .3gp file cannot be streamed.
                            mediafileDetails = assetDetails.getMediafiles().getMediafile().get(1);
                        } else {
                            // original version, not transcoded
                            mediafileDetails = assetDetails.getMediafiles().getMediafile().get(0);
                        }
                        LinkType link = null;
                        try {
                            // check if mediafile width is greater than maximum width specified in config settings.
                            int maxwidth = manager.getWidth();
                            int mediafileWidth = mediafileDetails.getMetadata().getWidth();
                            if (mediafileWidth > maxwidth) {
                                link = service.getPlayLink(assetId, mediafileDetails.getMediafileId(), manager.getUsername(), maxwidth);
                            } else {
                                link = service.getPlayLink(assetId, mediafileDetails.getMediafileId(), manager.getUsername(), "object");
                            }
                        } catch (ServiceException e) {

                        }
                        if (link != null) {
                            player = link.getOutput();
                        }
                        cacheStore(assetId, player);
                    }
                    // retrieve from cache

                } else {
                    player = cacheRetrieve(assetId);
                }
            }
            return player;
        } catch (RepositoryException e) {
            log.error("", e);
        } catch (CacheException e) {
            log.error("", e);
        } catch (ServiceException e) {
            log.error("", e);
        } catch (IOException e) {
            log.error("", e);
        }
        return null;
    }

    private static String cacheRetrieve(String id) throws CacheException {
        Cache c = CacheManager.getInstance().getCache();

        return (String) c.retrieve(id);
    }

    private static void cacheStore(String id, String player) throws CacheException {
        Cache c = CacheManager.getInstance().getCache();
        // expire after 9 minutes (in milliseconds)
        // long expire = 540000;
        // set caching timeout to 2 minutes = 120000 ms
        long expire = 120000;
        c.store(id, player, expire);
    }


}
