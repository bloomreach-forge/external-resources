package org.onehippo.forge.externalresource.api;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import nl.uva.mediamosa.MediaMosaService;
import nl.uva.mediamosa.model.AssetDetailsType;
import nl.uva.mediamosa.model.LinkType;
import nl.uva.mediamosa.model.MediafileDetailsType;
import nl.uva.mediamosa.util.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


public class MediaMosaEmbeddedHelper extends EmbeddedHelper {

    private static Logger log = LoggerFactory.getLogger(MediaMosaEmbeddedHelper.class);

    private Map<String, Object> map = new HashMap<String, Object>();

    private final CacheManager cacheManager = CacheManager.create();

    private MediaMosaService mediaMosaService;

    private static final String EMBEDDED_CACHE_NAME = "EMBEDDED_ASSETS_CACHE";

    private static final int CACHE_DEFAULT_SIZE = 500;
    private static final int CACHE_DEFAULT_TIME_TO_LIVE = 30;
    private static final int CACHE_DEFAULT_TIME_TO_IDLE = 30;


    public void initialize(Map<String, Object> properties) {
        this.map = properties;
        if (!map.isEmpty()) {
            mediaMosaService = new MediaMosaService((String) getProperty("host"));
            try {
                mediaMosaService.setCredentials((String) map.get("username"), (String) map.get("password"));
            } catch (ServiceException e) {

            }
        }
        createAssetCache();
    }


    public <T> T getProperty(String name, Object def) {
        if (map.containsKey(name)) {
            return (T) map.get(name);
        }
        return (T) def;
    }

    public <T> T getProperty(String name) {
        if (map.containsKey(name)) {
            return (T) map.get(name);
        }
        return null;
    }

    /**
     * Create a cache holding the embedded video codes per asset id
     */
    protected void createAssetCache() {

        if (!cacheManager.cacheExists(EMBEDDED_CACHE_NAME)) {
            final int cacheSize = (Integer) getProperty("cache.size", CACHE_DEFAULT_SIZE);
            final boolean overflowToDisk = (Boolean) getProperty("cache.overflowToDisk", false);
            final boolean eternal = (Boolean) getProperty("cache.eternal", false);
            final int timeToLiveSeconds = (Integer) getProperty("cache.timeToLiveSeconds", CACHE_DEFAULT_TIME_TO_LIVE);
            final int timeToIdleSeconds = (Integer) getProperty("cache.timeToIdleSeconds", CACHE_DEFAULT_TIME_TO_IDLE);

            Cache cache = new Cache(new CacheConfiguration(EMBEDDED_CACHE_NAME, cacheSize)
                    .overflowToDisk(overflowToDisk)
                    .eternal(eternal)
                    .timeToLiveSeconds(timeToLiveSeconds)
                    .timeToIdleSeconds(timeToIdleSeconds));
            cacheManager.addCache(cache);
            log.info("creating cache '{}': {}", EMBEDDED_CACHE_NAME, cache);
        }
    }

    public String getEmbedded(Node node) {
        String embedded = "<p>Something happened, can't show video</p>";
        try {
            if (node.hasProperty("hippomediamosa:assetid")) {
                String assetId = node.getProperty("hippomediamosa:assetid").getString();
                String cache = cacheRetrieve(assetId);
                if (cache == null) {
                    AssetDetailsType assetDetails = mediaMosaService.getAssetDetails(assetId);
                    MediafileDetailsType mediafileDetails = assetDetails.getMediafiles().getMediafile().get(0);
                    LinkType embedLink = mediaMosaService.getPlayLink(assetId, mediafileDetails.getMediafileId(), (String) getProperty("username"), (String) getProperty("width"));
                    if (embedLink != null) {
                        embedded = embedLink.getOutput();
                        cacheStore(assetId, embedded);
                    }
                } else {
                    embedded = cache;
                }
            }
        } catch (RepositoryException e) {
            log.error("", e);
            embedded = e.getLocalizedMessage();
        } catch (ServiceException e) {
            log.error("", e);
            embedded = e.getLocalizedMessage();
        } catch (IOException e) {
            log.error("", e);
            embedded = e.getLocalizedMessage();
        }
        return embedded;
    }

    protected String cacheRetrieve(String assetId) {
        log.info("trying to retrieving from cache with assetId: {}", assetId);
        Cache cache = cacheManager.getCache(EMBEDDED_CACHE_NAME);
        Element element = cache.get(assetId);
        if (element == null) {
            log.info("trying failed with assetId: {} .. return null", assetId);
            return null;
        } else {
            log.info("trying succeeded to retrieving from cache with assetId: {}", assetId);
            return (String) element.getObjectValue();
        }
    }

    protected void cacheStore(String assetId, String embeddedCode) {
        log.info("storing to cache with assetId: {} and embedded code : {}", assetId, embeddedCode);
        Cache cache = cacheManager.getCache(EMBEDDED_CACHE_NAME);
        Element element = new Element(assetId, embeddedCode);
        cache.put(element);
    }
}
