package org.onehippo.forge.externalresource.api;

import nl.uva.mediamosa.MediaMosaService;
import nl.uva.mediamosa.model.*;
import nl.uva.mediamosa.util.ServiceException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.entity.mime.content.InputStreamBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreProtocolPNames;
import org.hippoecm.frontend.editor.plugins.resource.ResourceHelper;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.forge.externalresource.api.scheduler.mediamosa.MediaMosaJobContext;
import org.onehippo.forge.externalresource.api.scheduler.mediamosa.MediaMosaJobListener;
import org.onehippo.forge.externalresource.api.scheduler.mediamosa.MediaMosaJobScheduler;
import org.onehippo.forge.externalresource.api.scheduler.mediamosa.MediaMosaThumbnailJob;
import org.onehippo.forge.externalresource.api.scheduler.synchronization.SynchronizationExecutorJob;
import org.onehippo.forge.externalresource.api.utils.ResourceInvocationType;
import org.onehippo.forge.externalresource.api.utils.SynchronizationState;
import org.onehippo.forge.externalresource.api.utils.Utils;
import org.onehippo.repository.scheduling.RepositoryJobCronTrigger;
import org.onehippo.repository.scheduling.RepositoryJobInfo;
import org.onehippo.repository.scheduling.RepositoryJobSimpleTrigger;
import org.onehippo.repository.scheduling.RepositoryScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @version $Id:
 */
public class HippoMediaMosaResourceManager extends ResourceManager implements Embeddable, Synchronizable {
    private static final String SYNCHRONIZATION_CRONEXPRESSION = "synchronization.cronexpression";
    private static final String SYNCHRONIZATION_ENABLED = "synchronization.enabled";
    private static final String SYNCHRONIZABLE = "synchronizable";
    @SuppressWarnings({"UnusedDeclaration"})
    private static Logger log = LoggerFactory.getLogger(HippoMediaMosaResourceManager.class);

    private String url;
    private String username;
    private String password;
    private String[] profiles = new String[0];
    private String playbackVideoCodec;
    private String responseType;
    private Long width;
    private boolean createThumbnail;
    private final MediaMosaService mediaMosaService;

    private static final String MASS_SYNC_JOB = "MediaMosaMassSyncJob";
    private static final String MASS_SYNC_JOB_TRIGGER = MASS_SYNC_JOB + "Trigger";
    private static final String MASS_SYNC_JOB_TRIGGER_GROUP = MASS_SYNC_JOB_TRIGGER + "Group";
    private static final String MASS_SYNC_JOB_GROUP = MASS_SYNC_JOB + "Group";

    private static final Long DEFAULT_WIDTH = 320L;

    private final static Map<String, String> map = new HashMap<String, String>();
    private final EmbeddedHelper embeddedHelper;

    static {
        map.put("hippomediamosa:title", "title");
        map.put("hippomediamosa:description", "description");
    }

    public static final String SYNCHRONIZABLE_SERVICE = "synchronizable." + HippoMediaMosaResourceManager.class.getSimpleName();

    public HippoMediaMosaResourceManager(IPluginConfig config, ResourceInvocationType type) {
        super(config, type);
        if (config.containsKey("url")) {
            this.url = config.getString("url");
        }
        if (config.containsKey("username")) {
            this.username = config.getString("username");
        }
        if (config.containsKey("password")) {
            this.password = config.getString("password");
        }
        if (config.containsKey("responseType")) {
            this.responseType = config.getString("responseType");
        }
        if (config.containsKey("width")) {
            this.width = config.getLong("width", DEFAULT_WIDTH);
        }
        if (config.containsKey("createThumbnail")) {
            this.createThumbnail = config.getBoolean("createThumbnail");
        }
        if (config.containsKey("profiles")) {
            this.profiles = config.getStringArray("profiles");
        }
        if (config.containsKey("playbackVideoCodec")) {
            this.playbackVideoCodec = config.getString("playbackVideoCodec");
        }


        this.mediaMosaService = new MediaMosaService(getUrl());
        try {
            this.mediaMosaService.setCredentials(getUsername(), getPassword());
        } catch (ServiceException e) {
            log.error("Service exception on setting media mosa credentials", e);
        }

        Map<String, Object> propertyMap = new HashMap<>();
        propertyMap.put("url", url);
        propertyMap.put("username", username);
        propertyMap.put("password", password);
        propertyMap.put("width", width);
        propertyMap.put("cache.size", config.get("cache.size"));
        propertyMap.put("cache.overflowToDisk", config.get("cache.overflowToDisk"));
        propertyMap.put("cache.eternal", config.get("cache.eternal"));
        propertyMap.put("cache.timeToLiveSeconds", config.get("cache.timeToLiveSeconds"));
        propertyMap.put("cache.timeToIdleSeconds", config.get("cache.timeToIdleSeconds"));
        embeddedHelper = new MediaMosaEmbeddedHelper();
        embeddedHelper.initialize(propertyMap);

//        HippoServiceRegistry.registerService(this, Synchronizable.class, SYNCHRONIZABLE_SERVICE);
    }

    @Override
    public void initSitePlugin() {
    }

    @Override
    public void initCmsPlugin() {
        try {
            RepositoryScheduler repositoryScheduler = getRepositoryScheduler();
            if (repositoryScheduler.checkExists(MASS_SYNC_JOB, MASS_SYNC_JOB_GROUP)) {
                repositoryScheduler.deleteJob(MASS_SYNC_JOB, MASS_SYNC_JOB_GROUP);
            }
            if (getPluginConfig().getAsBoolean(SYNCHRONIZATION_ENABLED, false)) {
                if (getPluginConfig().containsKey(SYNCHRONIZATION_CRONEXPRESSION)) {
                    String cronExpression = getPluginConfig().getString(SYNCHRONIZATION_CRONEXPRESSION);

                    RepositoryJobInfo jobInfo = new RepositoryJobInfo(MASS_SYNC_JOB, MASS_SYNC_JOB_GROUP, SynchronizationExecutorJob.class);
                    jobInfo.setAttribute(SYNCHRONIZABLE, SYNCHRONIZABLE_SERVICE);
                    RepositoryJobCronTrigger trigger = new RepositoryJobCronTrigger(MASS_SYNC_JOB_TRIGGER, cronExpression);
                    repositoryScheduler.scheduleJob(jobInfo, trigger);
                }
            }
        } catch (RepositoryException e) {
            log.error("RepositoryException (re)scheduling job", e);
        }
    }


    public MediaMosaService getMediaMosaService() {
        return mediaMosaService;
    }

    /**
     * Return the width as an integer. Internally a Long value is used.
     *
     * @return the width
     */
    public int getWidth() {
        return width.intValue();
    }

    @Override
    public void create(Node node, InputStream istream, String mimetype) throws Exception {
        try {

            String userId = getUsername();

            String assetId = mediaMosaService.createAsset(userId);
            String mediaFile = mediaMosaService.createMediafile(assetId, userId);

            UploadTicketType uploadTicketType = mediaMosaService.createUploadTicket(mediaFile, userId);

            String uploadUrl = uploadTicketType.getAction();

            int code = submitFile(istream, uploadUrl, mimetype, node.getName(), profiles);

            AssetDetailsType assetDetails = mediaMosaService.getAssetDetails(assetId);

            List<MediafileDetailsType> mediafiles = mediaMosaService.getAssetDetails(assetId).getMediafiles().getMediafile();
            MediafileDetailsType mediafileDetails = mediafiles.get(0);

            if (StringUtils.isNotBlank(playbackVideoCodec)) {
                for (MediafileDetailsType mf : mediafiles) {
                    if (playbackVideoCodec.equals(mf.getMetadata().getVideoCodec())) {
                        mediafileDetails = mf;
                        break;
                    }
                }
            }
            mediaFile = mediafileDetails.getMediafileId();

            int size = mediafileDetails.getMetadata().getFilesize();
            int height = mediafileDetails.getMetadata().getHeight();
            int width = mediafileDetails.getMetadata().getWidth();

            node.setProperty("hippomediamosa:assetid", assetId);
            node.setProperty("hippomediamosa:mediaid", mediaFile);
            node.setProperty("hippoexternal:size", size);
            node.setProperty("hippoexternal:height", height);
            node.setProperty("hippoexternal:width", width);

            //node.setProperty("hippoexternal:state", "inprogress");
            log.debug(assetDetails.getVideotimestampmodified().toString());
            log.debug(assetDetails.getVideotimestamp().toString());

            Calendar modified = assetDetails.getVideotimestampmodified();
            node.setProperty("hippoexternal:lastModifiedSyncDate", modified);

            LinkType embedLink = mediaMosaService.getPlayLink(assetId, mediafileDetails.getMediafileId(), getUsername(), getWidth());

            if (embedLink != null) {
                Utils.addEmbeddedNode(node, embedLink.getOutput());
            }

            String videoCodec = mediafileDetails.getMetadata().getVideoCodec();
            if (StringUtils.isNotBlank(videoCodec)) {
                Map map = new HashMap();
                map.put("still_type", "NORMAL");
                map.put("still_per_mediafile", 6);

                JobType job = mediaMosaService.createStill(assetId, mediaFile, getUsername(), map);

                MediaMosaJobListener listener = new MediaMosaJobListener() {
                    public void whileInprogress(String assetId) {
                    }

                    public void onFinished(String assetId) {
                        RepositoryJobInfo jobInfo = new RepositoryJobInfo("thumbnail." + assetId, MediaMosaThumbnailJob.class);
                        jobInfo.setAttribute(MediaMosaThumbnailJob.ASSET_ID_ATTRIBUTE, assetId);
                        RepositoryJobSimpleTrigger now = new RepositoryJobSimpleTrigger("now", new Date());
                        try {
                            getRepositoryScheduler().scheduleJob(jobInfo, now);
                        } catch (RepositoryException e) {
                            log.error(e.getMessage(),e);
                        }
                    }

                    public void whileWaiting(String assetId) {
                    }

                    public void onFailed(String assetId) {
                    }

                    public void onCancelled(String assetId) {
                    }
                };

                MediaMosaJobContext context = new MediaMosaJobContext();
                context.add(listener);
                context.setResourceManager(this);
                context.setJobId(String.valueOf(job.getJobId()));
                context.setAssetId(assetId);

                MediaMosaJobScheduler.getInstance().offer(context);
                log.debug("trying to request still creation");
            }
        } catch (ServiceException e) {
            log.error("", e);
        } catch (IOException e) {
            log.error("", e);
        }
    }

    @Override
    public void afterSave(Node node) {
        log.debug("starting aftersave");
        log.debug("done with aftersave");
    }

    @Override
    public void delete(Node node) {
        try {
            mediaMosaService.deleteAsset(node.getProperty("hippomediamosa:assetid").getString(), getUsername(), true);
            log.debug("deleting asset");
        } catch (RepositoryException e) {
            log.error("", e);
        } catch (ServiceException e) {
            log.error("", e);
        } catch (IOException e) {
            log.error("", e);
        }
    }

    public String getEmbedded(Node node) {
        return embeddedHelper.getEmbedded(node);
    }

    @Deprecated
    public static int submitFile(final InputStream inputStream, final String serverUrl, String mimeType, String fileName) throws Exception {
        return submitFile(inputStream, serverUrl, mimeType, fileName, ArrayUtils.EMPTY_STRING_ARRAY);
    }

    public static int submitFile(final InputStream inputStream, final String serverUrl, String mimeType, String fileName, String[] profiles) throws Exception {
        HttpClient httpclient = new DefaultHttpClient();
        httpclient.getParams().setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);

        HttpPost httppost = new HttpPost(serverUrl);
        MultipartEntity mpEntity = new MultipartEntity();
        ContentBody cbFile = new InputStreamBody(inputStream, mimeType, fileName);

        mpEntity.addPart("file", cbFile);

        for (String profile : profiles) {
            mpEntity.addPart("transcode[]", new StringBody(profile));
        }
        httppost.setEntity(mpEntity);
        HttpResponse response = httpclient.execute(httppost);
        int statusCode = response.getStatusLine().getStatusCode();
        log.debug("Status {}", response.getStatusLine());
        httpclient.getConnectionManager().shutdown();
        return statusCode;
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

    public boolean update(Node node) {
        InputStream is = null;
        try {
            String assetId = node.getProperty("hippomediamosa:assetid").getString();
            AssetDetailsType assetDetailsType = mediaMosaService.getAssetDetails(assetId);
            Calendar external = assetDetailsType.getVideotimestampmodified();

            MediafileDetailsType mediafileDetails = assetDetailsType.getMediafiles().getMediafile().get(0);
            node.setProperty("hippomediamosa:mediaid", mediafileDetails.getMediafileId());
            node.setProperty("hippoexternal:title", mediafileDetails.getMediafileId());
            node.setProperty("hippoexternal:width", mediafileDetails.getMetadata().getWidth());
            node.setProperty("hippoexternal:height", mediafileDetails.getMetadata().getHeight());
            node.setProperty("hippoexternal:mimeType", mediafileDetails.getMetadata().getMimeType());
            node.setProperty("hippoexternal:size", mediafileDetails.getMetadata().getFilesize());

            node.setProperty("hippoexternal:title", assetDetailsType.getDublinCore().getTitle());
            node.setProperty("hippoexternal:description", assetDetailsType.getDublinCore().getDescription());
            node.setProperty("hippoexternal:lastModified", external);

            if (assetDetailsType.getMediafileDuration() != null) {
                node.setProperty("hippoexternal:duration", assetDetailsType.getMediafileDuration().toXMLFormat());
            }

            node.setProperty("hippoexternal:state", SynchronizationState.SYNCHRONIZED.getState());
            node.setProperty("hippoexternal:lastModifiedSyncDate", external);

            if (StringUtils.isNotEmpty(assetDetailsType.getVpxStillUrl())) {
                String url = assetDetailsType.getVpxStillUrl();
                //Utils.resolveThumbnailToVideoNode(url, node);
                org.apache.commons.httpclient.HttpClient client = new org.apache.commons.httpclient.HttpClient();
                HttpMethod getMethod = new GetMethod(url);
                client.executeMethod(getMethod);
                String mimeType = getMethod.getResponseHeader("content-type").getValue();
                if (!mimeType.startsWith("image")) {
                    log.error("Illegal mimetype used: {}", mimeType);
                    throw new IllegalArgumentException();
                }

                is = getMethod.getResponseBodyAsStream();
                if (node.hasNode("hippoexternal:thumbnail")) {
                    Node thumbnail = node.getNode("hippoexternal:thumbnail");
                    thumbnail.setProperty("jcr:data", ResourceHelper.getValueFactory(node).createBinary(is));
                    thumbnail.setProperty("jcr:mimeType", mimeType);
                    thumbnail.setProperty("jcr:lastModified", Calendar.getInstance());
                }
            }
            node.getSession().save();
            return true;
        } catch (ServiceException e) {
            log.error("ServiceException on update", e);
        } catch (IOException e) {
            log.error("IOException on update", e);
        } catch (RepositoryException e) {
            log.error("RepositoryException on update", e);
        } finally {
            IOUtils.closeQuietly(is);
        }

        return false;
    }

    public boolean commit(Node node) {
        try {
            String assetId = node.getProperty("hippomediamosa:assetid").getString();
            Map map = new HashMap();
            map.put("title", node.getProperty("hippoexternal:title").getString());
            map.put("description", node.getProperty("hippoexternal:description").getString());

            Response r = mediaMosaService.setMetadata(assetId, getUsername(), map);
            log.debug(r.getHeader().getRequestResult());

            //lastsyncdate up!
            AssetDetailsType assetDetails = mediaMosaService.getAssetDetails(assetId);
            Calendar modified = assetDetails.getVideotimestampmodified();
            node.setProperty("hippoexternal:lastModifiedSyncDate", modified);
            node.getSession().save();
            return true;
        } catch (ServiceException e) {
            log.error("ServiceException while committing", e);
        } catch (IOException e) {
            log.error("IOException while committing", e);
        } catch (RepositoryException e) {
            log.error("RepositoryException while committing", e);
        }
        return false;
    }

    public SynchronizationState check(Node node) {
        try {
            if (!node.hasProperty("hippomediamosa:assetid")) {
                log.debug("no available hippomediamosa:assetid on node {}", node.getPath());
                return SynchronizationState.UNKNOWN;
            }

            String assetId = node.getProperty("hippomediamosa:assetid").getString();
            AssetDetailsType assetDetailsType = mediaMosaService.getAssetDetails(assetId);
            SynchronizationState currentState = SynchronizationState.getType(node.getProperty("hippoexternal:state").getString());
            if (assetDetailsType != null) {
                Calendar external = assetDetailsType.getVideotimestampmodified();
                Calendar local = null;
                if (node.hasProperty("hippoexternal:lastModifiedSyncDate")) {
                    local = node.getProperty("hippoexternal:lastModifiedSyncDate").getDate();
                }
                if (local.getTime().equals(external.getTime())) {
                    log.debug("check is correct");
                    if (!currentState.equals(SynchronizationState.SYNCHRONIZED)) {
                        node.setProperty("hippoexternal:state", SynchronizationState.SYNCHRONIZED.getState());
                        node.getSession().save();
                    }
                    return SynchronizationState.SYNCHRONIZED;
                } else {
                    if (!currentState.equals(SynchronizationState.UNSYNCHRONIZED)) {
                        node.setProperty("hippoexternal:state", SynchronizationState.UNSYNCHRONIZED.getState());
                        node.getSession().save();
                    }
                    log.debug("check is not correct");
                    return SynchronizationState.UNSYNCHRONIZED;
                }
            } else {
                if (!currentState.equals(SynchronizationState.BROKEN)) {
                    node.setProperty("hippoexternal:state", SynchronizationState.BROKEN.getState());
                    node.getSession().save();
                }
                return SynchronizationState.BROKEN;
            }
        } catch (RepositoryException e) {
            log.error("", e);
        } catch (ServiceException e) {
            log.error("", e);
        } catch (IOException e) {
            log.error("", e);
        }
        return SynchronizationState.UNKNOWN;
    }


//    /**
//     * Create a cache holding the embedded video codes per asset id
//     */
//    protected void createAssetCache(final IPluginConfig config) {
//
//        if (!cacheManager.cacheExists(EMBEDDED_CACHE_NAME)) {
//
//            final int cacheSize = config.getInt("cache.size", CACHE_DEFAULT_SIZE);
//            final boolean overflowToDisk = config.getBoolean("cache.overflowToDisk");
//            final boolean eternal = config.getBoolean("cache.eternal");
//            final int timeToLiveSeconds = config.getInt("cache.timeToLiveSeconds", CACHE_DEFAULT_TIME_TO_LIVE);
//            final int timeToIdleSeconds = config.getInt("cache.timeToIdleSeconds", CACHE_DEFAULT_TIME_TO_IDLE);
//
//            Cache cache = new Cache(new CacheConfiguration(EMBEDDED_CACHE_NAME, cacheSize)
//                    .overflowToDisk(overflowToDisk)
//                    .eternal(eternal)
//                    .timeToLiveSeconds(timeToLiveSeconds)
//                    .timeToIdleSeconds(timeToIdleSeconds));
//            cacheManager.addCache(cache);
//            log.info("creating cache '{}': {}", EMBEDDED_CACHE_NAME, cache);
//        }
//    }
//
//    protected String cacheRetrieve(String assetId) {
//        log.info("trying to retrieving from cache with assetId: {}", assetId);
//        Cache cache = cacheManager.getCache(EMBEDDED_CACHE_NAME);
//        Element element = cache.get(assetId);
//        if (element == null) {
//            log.info("trying failed with assetId: {} .. return null", assetId);
//            return null;
//        } else {
//            log.info("trying succeeded to retrieving from cache with assetId: {}", assetId);
//            return (String) element.getObjectValue();
//        }
//    }
//
//    protected void cacheStore(String assetId, String embeddedCode) {
//        log.info("storing to cache with assetId: {} and embedded code : {}", assetId, embeddedCode);
//        Cache cache = cacheManager.getCache(EMBEDDED_CACHE_NAME);
//        Element element = new Element(assetId, embeddedCode);
//        cache.put(element);
//    }
}
