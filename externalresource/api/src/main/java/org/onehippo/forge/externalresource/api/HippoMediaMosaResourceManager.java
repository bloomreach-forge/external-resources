package org.onehippo.forge.externalresource.api;

import com.whirlycott.cache.Cache;
import com.whirlycott.cache.CacheException;
import com.whirlycott.cache.CacheManager;
import nl.uva.mediamosa.MediaMosaService;
import nl.uva.mediamosa.model.*;
import nl.uva.mediamosa.util.ServiceException;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.entity.mime.content.InputStreamBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreProtocolPNames;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.onehippo.forge.externalresource.api.utils.SynchronizationState;
import org.onehippo.forge.externalresource.api.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

/**
 * @version $Id:
 */
public class HippoMediaMosaResourceManager extends ResourceManager implements Embeddable, Synchronizable {
    @SuppressWarnings({"UnusedDeclaration"})
    private static Logger log = LoggerFactory.getLogger(HippoMediaMosaResourceManager.class);

    private String url;
    private String username;
    private String password;
    private String responseType;
    private int width;
    private boolean createThumbnail;
    private final MediaMosaService mediaMosaService;
    /*private final SchedulerFactory schedulerFactory = new StdSchedulerFactory();
    private Scheduler scheduler = null;*/

    private final ExecutorService service = Executors.newFixedThreadPool(3);


    public HippoMediaMosaResourceManager(IPluginConfig config) {
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
        if (config.containsKey("responseType")) {
            this.responseType = config.getString("responseType");
        }
        if (config.containsKey("width")) {
            this.width = config.getInt("width", 320);
        }
        if (config.containsKey("createThumbnail")) {
            this.createThumbnail = config.getBoolean("createThumbnail");
        }
        this.mediaMosaService = new MediaMosaService(getUrl());
        try {
            this.mediaMosaService.setCredentials(getUsername(), getPassword());
        } catch (ServiceException e) {
            log.error("Service exception on authenticating media mosa credentials", e);
        }
        /*try {
            scheduler = schedulerFactory.getScheduler();
            scheduler.start();
        } catch (SchedulerException e) {
            log.error("Could not get scheduler", e);
        }*/
    }

    public MediaMosaService getMediaMosaService() {
        return mediaMosaService;
    }

    public int getWidth() {
        return width;
    }

    @Override
    public void create(Node node, InputStream istream, String mimetype) throws Exception {
        try {

            String userId = getUsername();

            String assetId = mediaMosaService.createAsset(userId);
            String mediaFile = mediaMosaService.createMediafile(assetId, userId);

            UploadTicketType uploadTicketType = mediaMosaService.createUploadTicket(mediaFile, userId);

            String uploadUrl = uploadTicketType.getAction();

            int code = submitFile(istream, uploadUrl, mimetype, node.getName());
            //create image
            node.setProperty("hippomediamosa:assetid", assetId);
            node.setProperty("hippomediamosa:mediaid", mediaFile);

            AssetDetailsType assetDetails = mediaMosaService.getAssetDetails(assetId);

            MediafileDetailsType mediafileDetails = assetDetails.getMediafiles().getMediafile().get(0);

            int size = mediafileDetails.getMetadata().getFilesize();
            int height = mediafileDetails.getMetadata().getHeight();
            int width = mediafileDetails.getMetadata().getWidth();

            node.setProperty("hippoexternal:size", size);
            node.setProperty("hippoexternal:height", height);
            node.setProperty("hippoexternal:width", width);

            System.out.println(assetDetails.getVideotimestampmodified());
            System.out.println(assetDetails.getVideotimestamp());

            Calendar modified = assetDetails.getVideotimestampmodified();
            node.setProperty("hippoexternal:lastModifiedSyncDate", modified);

            LinkType embedLink = mediaMosaService.getPlayLink(assetId, mediafileDetails.getMediafileId(), getUsername(), this.width);

            Utils.addEmbeddedNode(node, embedLink.getOutput());

            System.out.println(mediaMosaService.doPostRequestString("/asset/" + assetId + "/still/create", "user_id=" + getUsername() + "&mediafile_id=" + mediaFile));

            System.out.println("trying to request still creation");

        } catch (ServiceException e) {
            log.error("", e);
        } catch (IOException e) {
            log.error("", e);
        }
    }

    @Override
    public void afterSave(Node node) {
        System.out.println("starting aftersave");
        if (createThumbnail) {
            /*JobDetail job = new JobDetail("afterSaveJob", "mediamosa_group", MediaMosaJob.class);
            SimpleTrigger trigger = new SimpleTrigger("myTrigger", "mediamosa_group", new Date(), null, SimpleTrigger.REPEAT_INDEFINITELY, 60L * 1000L);
            try {
                scheduler.scheduleJob(job, trigger);
            } catch (SchedulerException e) {
                log.error("Error execing job ", e);
            }  */
            try {
                final String path = node.getPath();

                Runnable task = new FutureTask<Boolean>(
                        new Callable<Boolean>() {
                            public Boolean call() throws Exception {
                                MediaMosaTask task = new MediaMosaTask();
                                return task.execute(path, mediaMosaService);
                            }
                        }
                );
                service.submit(task);
            } catch (RepositoryException e) {
                log.error("", e);
            }
        }
        System.out.println("done with aftersave");
    }

    @Override
    public void delete(Node node) {
        try {
            if (node.getParent().getPrimaryNodeType().getName().equals("hippo:handle")) {
                node.getParent().remove();
                node.getSession().save();
            }
        } catch (RepositoryException e) {
            log.error("", e);
        }

    }


    /* public String getEmbedded(Node node) {
        String embedded = null;
        try {
            if (node.hasNode("hippoexternal:embedded")) {
                Node embeddedNode = node.getNode("hippoexternal:embedded");
                if (embeddedNode.hasProperty("hippoexternal:embedded")) {
                    embedded = embeddedNode.getProperty("hippoexternal:embedded").getString();
                }
            }
        } catch (RepositoryException e) {
            log.error("", e);
        }
        return embedded;
    }*/

    public String getEmbedded(Node node) {
        String embedded = "<p>somethine happened, can't show video</p>";

        try {
            if (node.hasProperty("hippomediamosa:assetid")) {
                String assetId = node.getProperty("hippomediamosa:assetid").getString();
                if (cacheRetrieve(assetId) == null) {
                    AssetDetailsType assetDetails = mediaMosaService.getAssetDetails(assetId);
                    MediafileDetailsType mediafileDetails = assetDetails.getMediafiles().getMediafile().get(0);
                    LinkType embedLink = mediaMosaService.getPlayLink(assetId, mediafileDetails.getMediafileId(), getUsername(), this.width);
                    embedded = embedLink.getOutput();
                    cacheStore(assetId, embedded);
                } else {
                    embedded = cacheRetrieve(assetId);
                }
            }
        } catch (RepositoryException e) {
            log.error("", e);
        } catch (ServiceException e) {
            log.error("", e);
        } catch (IOException e) {
            log.error("", e);
        } catch (CacheException e) {
            log.error("", e);
        }
        return embedded;
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


    public static int submitFile(final InputStream inputStream, final String serverUrl, String mimeType, String fileName) throws Exception {
        HttpClient httpclient = new DefaultHttpClient();
        httpclient.getParams().setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);

        HttpPost httppost = new HttpPost(serverUrl);
        MultipartEntity mpEntity = new MultipartEntity();
        ContentBody cbFile = new InputStreamBody(inputStream, mimeType, fileName);

        mpEntity.addPart("file", cbFile);
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
        try {
            String assetId = node.getProperty("hippomediamosa:assetid").getString();
            AssetDetailsType assetDetailsType = mediaMosaService.getAssetDetails(assetId);
            Calendar external = assetDetailsType.getVideotimestampmodified();
            //todo here update propertyfields etc.
            node.setProperty("hippoexternal:state", "synchronized");
            node.setProperty("hippoexternal:lastModifiedSyncDate", external);
            node.getSession().save();
            return true;
        } catch (RepositoryException e) {
            log.error("", e);
        } catch (ServiceException e) {
            log.error("", e);
        } catch (IOException e) {
            log.error("", e);
        }

        return false;
    }

    public boolean commit(Node node) {
        try {
            String assetId = node.getProperty("hippomediamosa:assetid").getString();
            Map map = new HashMap();
            PropertyIterator it = node.getProperties();
            while (it.hasNext()) {
                Property next = it.nextProperty();
                if (next.getType() == PropertyType.STRING) {
                    try {
                        map.put(next.getName(), next.getValue().getString());
                        System.out.println(next.getName() + " - " + next.getString());
                    } catch (ValueFormatException e) {
                    }
                }
            }
            try {
                //todo doesn;t work!!!
                Response r = mediaMosaService.setMetadata(assetId, getUsername(), map);
                System.out.println(r.getHeader().getRequestResult());

                //lastsyncdate up!
                AssetDetailsType assetDetails = mediaMosaService.getAssetDetails(assetId);
                Calendar modified = assetDetails.getVideotimestampmodified();

                node.setProperty("hippoexternal:lastModifiedSyncDate", modified);
                node.getSession().save();
                return true;
            } catch (ServiceException e) {
                log.error("", e);
            } catch (IOException e) {
                log.error("", e);
            }
        } catch (RepositoryException e) {
            log.error("", e);
        }
        return false;
    }

    public SynchronizationState check(Node node) {
        try {
            String assetId = node.getProperty("hippomediamosa:assetid").getString();
            AssetDetailsType assetDetailsType = mediaMosaService.getAssetDetails(assetId);
            if (assetDetailsType != null) {
                Calendar external = assetDetailsType.getVideotimestampmodified();

                Calendar local = null;
                if (node.hasProperty("hippoexternal:lastModifiedSyncDate")) {
                    local = node.getProperty("hippoexternal:lastModifiedSyncDate").getDate();
                }
                if (local.getTime().getTime() == external.getTime().getTime()) {
                    System.out.println("check is correct");

                    if (!node.getProperty("hippoexternal:state").getString().equals("synchronized")) {
                        node.setProperty("hippoexternal:state", "synchronized");
                        node.getSession().save();
                    }
                    return SynchronizationState.SYNCHRONIZED;
                } else {
                    if (!node.getProperty("hippoexternal:state").getString().equals("unsynchronized")) {
                        node.setProperty("hippoexternal:state", "unsynchronized");
                        node.getSession().save();
                    }
                    System.out.println("check is not correct");
                    System.out.println(local.getTime().getTime());
                    System.out.println(external.getTime().getTime());
                    return SynchronizationState.OUT_OF_SYNC;
                }
            } else {
                if (!node.getProperty("hippoexternal:state").getString().equals("broken")) {
                    node.setProperty("hippoexternal:state", "broken");
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

    public String changeValue(Node node, String property, String value) {
        try {
            if (!node.getProperty(property).getString().equals(value)) {
                node.setProperty(property, value);
                node.getSession().save();
                return value;
            }
        } catch (RepositoryException e) {
            log.error("", e);
        }
        return null;
    }
}
