package org.onehippo.forge.externalresource.api;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import nl.uva.mediamosa.MediaMosaService;
import nl.uva.mediamosa.model.*;
import nl.uva.mediamosa.util.ServiceException;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.entity.mime.content.InputStreamBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreProtocolPNames;
import org.hippoecm.frontend.editor.plugins.resource.ResourceHelper;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.util.JcrUtils;
import org.onehippo.forge.externalresource.api.scheduler.mediamosa.MediaMosaJobContext;
import org.onehippo.forge.externalresource.api.scheduler.mediamosa.MediaMosaJobListener;
import org.onehippo.forge.externalresource.api.scheduler.mediamosa.MediaMosaJobScheduler;
import org.onehippo.forge.externalresource.api.scheduler.mediamosa.MediaMosaThumbnailJob;
import org.onehippo.forge.externalresource.api.scheduler.synchronization.SynchronizationExecutorJob;
import org.onehippo.forge.externalresource.api.utils.*;
import org.onehippo.repository.modules.ProvidesService;
import org.onehippo.repository.scheduling.RepositoryJobCronTrigger;
import org.onehippo.repository.scheduling.RepositoryJobInfo;
import org.onehippo.repository.scheduling.RepositoryJobSimpleTrigger;
import org.onehippo.repository.scheduling.RepositoryScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import static org.hippoecm.repository.quartz.HippoSchedJcrConstants.HIPPOSCHED_SUBJECT_ID;
import static org.hippoecm.repository.quartz.HippoSchedJcrConstants.HIPPOSCHED_WORKFLOW_JOB;

/**
 * @version $Id:
 */

public class HippoMediaMosaResourceManager extends ResourceManager implements Embeddable, Synchronizable, MediamosaRemoteService {

    private static final String SYNCHRONIZATION_CRONEXPRESSION = "synchronization.cronexpression";
    private static final String SYNCHRONIZATION_ENABLED = "synchronization.enabled";
    private static final String SYNCHRONIZABLE = "synchronizable";
    public static final Long DEFAULT_TTL = 1000L;
    public static final Long DEFAULT_CACHE_SIZE = 100L;
    public static final Long DEFAULT_TTI = 1000L;
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
    private MediaMosaService mediaMosaService;

    public static final String MASS_SYNC_JOB = "MediaMosaMassSyncJob";
    public static final String MASS_SYNC_JOB_TRIGGER = MASS_SYNC_JOB + "Trigger";
    public static final String MASS_SYNC_JOB_TRIGGER_GROUP = MASS_SYNC_JOB_TRIGGER + "Group";
    public static final String MASS_SYNC_JOB_GROUP = MASS_SYNC_JOB + "Group";
    private static final Long DEFAULT_WIDTH = 320L;
    private EmbeddedHelper embeddedHelper;
    private MediaMosaServices services;

    public HippoMediaMosaResourceManager(final ResourceInvocationType type) {
        super(type);
    }

    @Override
    public void configure(final Node config) {
        try {
            this.url = JcrUtils.getStringProperty(config, "url", null);
            this.username = JcrUtils.getStringProperty(config, "username", null);
            this.password = JcrUtils.getStringProperty(config, "password", null);
            this.responseType = JcrUtils.getStringProperty(config, "responseType", null);
            this.width = JcrUtils.getLongProperty(config, "width", DEFAULT_WIDTH);
            this.createThumbnail = JcrUtils.getBooleanProperty(config, "createThumbnail", Boolean.FALSE);
            this.playbackVideoCodec = JcrUtils.getStringProperty(config, "playbackVideoCodec", null);
            this.profiles = getStringArray(config, "profiles");
            this.mediaMosaService = new MediaMosaService(getUrl());
            try {
                this.mediaMosaService.setCredentials(getUsername(), getPassword());
            } catch (ServiceException e) {
                log.error("Service exception on setting media mosa credentials", e);
            }

            this.services = MediaMosaServices.init(JcrUtils.getStringProperty(config, "type", "hippomediamosa:resource"));

            Map<String, Object> propertyMap = new HashMap<>();
            propertyMap.put("url", url);
            propertyMap.put("username", username);
            propertyMap.put("password", password);
            propertyMap.put("width", width);
            propertyMap.put("cache.size", JcrUtils.getLongProperty(config, "cache.size", DEFAULT_CACHE_SIZE));
            propertyMap.put("cache.overflowToDisk", JcrUtils.getBooleanProperty(config, "cache.overflowToDisk", Boolean.FALSE));
            propertyMap.put("cache.eternal", JcrUtils.getBooleanProperty(config, "cache.eternal", Boolean.FALSE));
            propertyMap.put("cache.timeToLiveSeconds", JcrUtils.getLongProperty(config, "cache.timeToLiveSeconds", DEFAULT_TTL));
            propertyMap.put("cache.timeToIdleSeconds", JcrUtils.getLongProperty(config, "cache.timeToIdleSeconds", DEFAULT_TTI));
            embeddedHelper = new MediaMosaEmbeddedHelper();
            embeddedHelper.initialize(propertyMap);

        } catch (RepositoryException e) {
            log.error("Error configuring module", e);
        }
        if (getType() == ResourceInvocationType.CMS) {
            try {
                RepositoryScheduler repositoryScheduler = getRepositoryScheduler();
                final String cronExpression = JcrUtils.getStringProperty(config, SYNCHRONIZATION_CRONEXPRESSION, null);
                if (JcrUtils.getBooleanProperty(config, SYNCHRONIZATION_ENABLED, Boolean.FALSE)
                        && !repositoryScheduler.checkExists(MASS_SYNC_JOB, MASS_SYNC_JOB_GROUP)
                        && !Strings.isNullOrEmpty(cronExpression)) {
                    RepositoryJobInfo jobInfo = new RepositoryJobInfo(MASS_SYNC_JOB, MASS_SYNC_JOB_GROUP, SynchronizationExecutorJob.class);
                    jobInfo.setAttribute(SynchronizationExecutorJob.JOB_GROUP, MASS_SYNC_JOB_TRIGGER_GROUP);
                    RepositoryJobCronTrigger trigger = new RepositoryJobCronTrigger(MASS_SYNC_JOB_TRIGGER, cronExpression);
                    repositoryScheduler.scheduleJob(jobInfo, trigger);
                }
            } catch (RepositoryException e) {
                log.error("Error starting scheduled job " + MASS_SYNC_JOB, e);
            }
            services.register(this);
        }
    }

    private String[] getStringArray(Node node, String relPath) throws RepositoryException {
        ImmutableList.Builder<String> builder = ImmutableList.builder();
        if (node.hasProperty(relPath)) {
            for (Value value : node.getProperty(relPath).getValues()) {
                builder.add(value.getString());
            }
        }
        return builder.build().toArray(ArrayUtils.EMPTY_STRING_ARRAY);
    }

    @Override
    public void close() {
        try {
            RepositoryScheduler repositoryScheduler = getRepositoryScheduler();
            if (repositoryScheduler.checkExists(MASS_SYNC_JOB, MASS_SYNC_JOB_GROUP)) {
                repositoryScheduler.deleteJob(MASS_SYNC_JOB, MASS_SYNC_JOB_GROUP);
            }
        } catch (RepositoryException e) {
            log.error("Error stopping scheduled job " + MASS_SYNC_JOB, e);
        }

        // make sure we unregister first (reload after config)
        services.unregister(this);
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
            log.debug("Response code (file submit action): {}", code);

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

            final Node handleNode = node.getParent();

            //node.setProperty("hippoexternal:state", "inprogress");
            log.debug(assetDetails.getVideotimestampmodified().toString());
            log.debug(assetDetails.getVideotimestamp().toString());

            Calendar modified = assetDetails.getVideotimestampmodified();
            node.setProperty("hippoexternal:lastModifiedSyncDate", modified);

//            LinkType embedLink = mediaMosaService.getPlayLink(assetId, mediafileDetails.getMediafileId(), getUsername(), getWidth());
//
//            if (embedLink != null) {
//                Utils.addEmbeddedNode(node, embedLink.getOutput());
//            }

            String videoCodec = mediafileDetails.getMetadata().getVideoCodec();
            if (StringUtils.isNotBlank(videoCodec)) {
                Map<String, Object> map = new HashMap<>();
                map.put("still_type", "NORMAL");
                map.put("still_per_mediafile", 6);

                JobType job = mediaMosaService.createStill(assetId, mediaFile, getUsername(), map);

                MediaMosaJobListener listener = new MediaMosaJobListener() {
                    public void whileInprogress(String assetId) {
                    }

                    public void onFinished(String assetId) {
                        try {
                            RepositoryJobInfo jobInfo = new MediaMosaResourceThumbnailJobInfo(handleNode.getIdentifier(), MASS_SYNC_JOB_TRIGGER_GROUP);
                            jobInfo.setAttribute(MediaMosaThumbnailJob.ASSET_ID_ATTRIBUTE, assetId);
                            RepositoryJobSimpleTrigger now = new RepositoryJobSimpleTrigger("now", new Date());

                            getRepositoryScheduler().scheduleJob(jobInfo, now);
                        } catch (RepositoryException e) {
                            log.error(e.getMessage(), e);
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
        } catch (ServiceException | IOException e) {
            log.error("Error executing create", e);
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
        } catch (RepositoryException | ServiceException | IOException e) {
            log.error("Error deleting asset", e);
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

    @Override
    public String getUsername() {
        return username;
    }


    public void setUsername(String username) {
        this.username = username;
    }

    @Override
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public MediaMosaService service() {
        return mediaMosaService;
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
                HttpClient client = Utils.getHttpClient();
                HttpResponse httpResponse = client.execute(new HttpGet(url));
                String mimeType = httpResponse.getFirstHeader("content-type").getValue();
                if (!mimeType.startsWith("image")) {
                    log.error("Illegal mimetype used: {}", mimeType);
                    throw new IllegalArgumentException();
                }

                is = httpResponse.getEntity().getContent();
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
            Map<String, String> map = new HashMap<>();
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
        } catch (RepositoryException | ServiceException | IOException e) {
            log.error("Error executing state check", e);
        }
        return SynchronizationState.UNKNOWN;
    }


    public static class MediaMosaResourceThumbnailJobInfo extends RepositoryJobInfo {

        private final String handleIdentifier;

        public MediaMosaResourceThumbnailJobInfo(final String handleIdentifier, final String jobGroup) {
            super(HippoNodeType.HIPPO_REQUEST, jobGroup, MediaMosaThumbnailJob.class);
            this.handleIdentifier = handleIdentifier;
            setAttribute(HIPPOSCHED_SUBJECT_ID, handleIdentifier);
        }

        @Override
        public Node createNode(final Session session) throws RepositoryException {
            final Node handleNode = session.getNodeByIdentifier(handleIdentifier);
            return handleNode.addNode(HippoNodeType.HIPPO_REQUEST, HIPPOSCHED_WORKFLOW_JOB);
        }
    }

}
