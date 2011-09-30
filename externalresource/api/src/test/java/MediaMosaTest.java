import nl.uva.mediamosa.MediaMosaService;
import nl.uva.mediamosa.model.AssetDetailsType;
import nl.uva.mediamosa.model.AssetType;
import nl.uva.mediamosa.model.MediafileDetailsType;
import nl.uva.mediamosa.model.UploadTicketType;
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
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * @version $Id$
 */
//@Ignore
public class MediaMosaTest {
    @SuppressWarnings({"UnusedDeclaration"})
    private static Logger log = LoggerFactory.getLogger(MediaMosaTest.class);

    MediaMosaService mediaMosaService;

    /*public AssetDetailsType AssetDetail(String assetId) throws IOException, ServiceException {
        AssetDetailsType assetDetails = mediaMosaService.getAssetDetails(assetId);
        return assetDetails;
    }*/

    private String userId = "Hippo";
    private String password = "VERUnK2fWlc0F3IceA21awRj";
    private String url = "http://gir.ic.uva.nl/mediamosa";

    public MediafileDetailsType getMediaFile(AssetDetailsType assetDetailsType) {
        return assetDetailsType.getMediafiles().getMediafile().get(0);
    }

    @Test
    public void MediaMosa() {


        mediaMosaService = new MediaMosaService("http://gir.ic.uva.nl/mediamosa");

        try {
            mediaMosaService.setCredentials("Hippo", "VERUnK2fWlc0F3IceA21awRj");

            Map map = new HashMap();
            map.put("title", "test1");

            System.out.println("bla");

            //mediaMosaService.createMediaFileStill()
            //mediaMosaService.getAssets();
            List<AssetType> lsit = mediaMosaService.getAssets();

            //for(AssetType type : lsit){
            //   mediaMosaService.deleteAsset(type.getAssetId(), "Hippo", true);

            //}

            mediaMosaService.setMetadata(lsit.get(lsit.size() - 1).getAssetId(), "Hippo", map);
            mediaMosaService.setMetadata(lsit.get(lsit.size() - 2).getAssetId(), "Hippo", map);
            mediaMosaService.deleteAsset(lsit.get(lsit.size() - 3).getAssetId(), "Hippo", true);
            /*for (AssetType asset : lsit) {
                System.out.println(asset.getAssetId()) ;
                //mediaMosaService.deleteAsset(asset.getAssetId(), "Hippo", true);
                //mediaMosaService.getAssetDetails(asset.getAssetId())
                mediaMosaService.setMetadata(asset.getAssetId(), "Hippo", map)   ;
                //mediaMosaService.getAssetDetails(asset.getAssetId()).get
            }*/


            /*String userId = "hippo-admin";
            String assetId = mediaMosaService.createAsset(userId);
            String mediaFile = mediaMosaService.createMediafile(assetId, userId);
            UploadTicketType uploadTicketType = mediaMosaService.createUploadTicket(mediaFile, userId);
            String uploadUrl = uploadTicketType.getAction();

            InputStream inputStream = null;

            HttpClient httpclient = new DefaultHttpClient();
            httpclient.getParams().setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);

            HttpPost httppost = new HttpPost(uploadUrl);
            MultipartEntity mpEntity = new MultipartEntity();
            ContentBody cbFile = new InputStreamBody(inputStream, "video/x-ms-wmv", "mediatest");

            mpEntity.addPart("file", cbFile);
            httppost.setEntity(mpEntity);
            HttpResponse response = httpclient.execute(httppost);
            //HttpEntity resEntity = response.getEntity();
            //String red5Url = EntityUtils.toString(resEntity);
            log.debug("Status {}", response.getStatusLine());
            httpclient.getConnectionManager().shutdown();

            List<AssetType> assetList = mediaMosaService.getAssets();

            if (assetList.size() > 0) {
                //String id = assetList.get(0).getAssetId();
                AssetDetailsType assetDetails = mediaMosaService.getAssetDetails(assetId);
                MediafileDetailsType mediafileDetails = assetDetails.getMediafiles().getMediafile().get(0);
// request link to play video
                LinkType link = mediaMosaService.getPlayLink(assetId, mediafileDetails.getMediafileId(), userId);

            }*/

            // mediaMosaService.get

        } catch (ServiceException e) {
            log.error("", e);
        } catch (IOException e) {
            log.error("", e);
        }


        //mediaMosaService.updateMediafile()
    }



    public String requestForVideo() {
        try {
            String assetId = mediaMosaService.createAsset(userId);

            String mediaFile = mediaMosaService.createMediafile(assetId, userId);

            UploadTicketType uploadTicketType = mediaMosaService.createUploadTicket(mediaFile, userId);

            String uploadUrl = uploadTicketType.getAction();
            return uploadUrl;
        } catch (ServiceException e) {
            log.error("", e);
        } catch (IOException e) {
            log.error("", e);
        }
        return null;
    }

    public int submitFile(final InputStream inputStream, String mimeType, String fileName) throws Exception {
        String serverUrl = requestForVideo();

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


}
