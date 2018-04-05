/**
 * MediaMosa API
 *
 * A partial implementation of the MediaMosa API in Java.
 *
 * Copyright 2010 Universiteit van Amsterdam
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package nl.uva.mediamosa;

import nl.uva.mediamosa.model.*;
import nl.uva.mediamosa.util.ServiceException;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;


public interface MediaMosa {
    /**
     *
     * @param getRequest
     * @return
     * @throws IOException
     */
    String doGetRequestString(String getRequest) throws IOException;

    /**
     *
     * @param post
     * @param getRequest
     * @return
     * @throws IOException
     */
    String doPostRequestString(String post, String getRequest) throws IOException;

    /**
     * Sets authentication credentials used for login.
     *
     * @param username
     * @param password
     * @throws java.io.IOException
     * @throws ServiceException
     */
    void setCredentials(String username, String password) throws ServiceException;

    /**
     * Sets hostname of MediaMosa server (for example http://yourserver.com)
     *
     * @param hostname
     */
    void setHostname(String hostname);

    /**
     * Returns boolean for login.
     *
     * @return boolean
     * @throws ServiceException
     */
    boolean login() throws IOException, ServiceException;

    /**
     * @return waarom?
     */
    boolean isValidCookie();

    /**
     * Return List of ErrorCodeType
     *
     * @return <code>List<ErrorcodeType></code>
     * @throws ServiceException
     */
    List<ErrorcodeType> getErrorCodes() throws ServiceException;

    /**
     * Returns protocol version of the MediaMosa webservice you are connected with.
     *
     * @return <code>String</code> containing the version number.
     * @throws ServiceException
     */
    String getVersion() throws ServiceException;

    /**
     * @param assetId
     * @param mediafileId
     * @param userId
     * @return <code>LinkType</code> object. The actual url of the media file is obtained by calling LinkType.getOutput()
     * @throws ServiceException
     */
    LinkType getPlayLink(String assetId, String mediafileId, String userId) throws ServiceException;

    /**
     * @param assetId
     * @param mediafileId
     * @param userId
     * @param responseType
     * @return <code>LinkType</code> object. ResponseType can be set to 'plain' (plain URL),
     *         'metafile' (metafile format, this depends on the type of video), 'object' (html object),
     *         'download' (download url) or 'still' (still). The actual content of the response is obtained by calling LinkType.getOutput()
     * @throws ServiceException
     */
    LinkType getPlayLink(String assetId, String mediafileId, String userId, String responseType) throws ServiceException;

    /**
     * @param assetId
     * @param mediafileId
     * @param userId
     * @param width
     * @return <code>LinkType</code> object. The actual content of the response is obtained by calling LinkType.getOutput(). It contains generated object code where 'width' is as specified, and 'height' is calculated based on ratio.
     * @throws ServiceException
     */
    LinkType getPlayLink(String assetId, String mediafileId, String userId, int width) throws ServiceException;


    /**
     * @param assetId
     * @param userId
     * @return <code>LinkType</code> object. The actual url of the still image is obtained by calling LinkType.getOutput()
     * @throws ServiceException
     */
    StillType getStillLink(String assetId, String userId) throws ServiceException;


    /**
     * @param mediafileId
     * @param userId
     * @return Ticket needed for uploading a file in the mediafile container specified by mediafileId
     * @throws ServiceException
     */
    UploadTicketType createUploadTicket(String mediafileId, String userId) throws ServiceException;

    //new
    UploadTicketType createUploadTicket(String mediafileId, String userId, boolean isStill) throws ServiceException;

    /**
     * @return <code>String</code> assetId of the newly created asset
     */
    String createAsset(String userId);


    /**
     * @return <code>String</code> mediafileId of the newly create mediafile
     */
    String createMediafile(String assetId, String userId);

    /**
     * @return List<AssetType>
     * @throws ServiceException
     */
    List<AssetType> getAssets() throws ServiceException;

    /**
     * @param properties
     * @return List<AssetType>
     * @throws ServiceException
     */
    List<AssetType> getAssets(Map properties) throws ServiceException;

    //new
    List<AssetType> getAssets(int limit, int offset, Map properties) throws ServiceException;

    //new
    long getAssetCount(int limit, Map properties) throws ServiceException;

    /**
     * @param assetId
     * @return AssetDetailsType
     * @throws ServiceException
     */
    AssetDetailsType getAssetDetails(String assetId) throws ServiceException;

    /**
     * @param assetId
     * @param userId
     * @param metadata
     * @return
     */
    Response setMetadata(String assetId, String userId, Map metadata);

    /**
     * @param assetId
     * @param userId
     * @throws ServiceException
     */
    void deleteMetadata(String assetId, String userId) throws ServiceException;

    /**
     * @param assetId
     * @param userId
     * @param cascade
     * @throws ServiceException
     */
    void deleteAsset(String assetId, String userId, boolean cascade) throws ServiceException;

    /**
     * @param mediafileId
     * @param userId
     * @param properties
     * @return
     */
    Response updateMediafile(String mediafileId, String userId, Map properties);

    /**
     * @param mediafileId
     * @param userId
     * @throws ServiceException
     */
    void deleteMediafile(String mediafileId, String userId) throws ServiceException;

    /**
     * @param mediafileId
     * @return
     */
    MediafileDetailsType getMediafileDetails(String mediafileId);

    /**
     * @return
     * @throws ServiceException
     */
    List<ProfileType> getProfiles() throws ServiceException;

    /**
     * @param cql
     * @return
     * @throws ServiceException
     */
    List<AssetType> getCqlAssets(String cql) throws ServiceException;

    //new
    long getAssetCount(String owner, String group) throws ServiceException;

    //new Still functionality
    JobType createStill(String assetId, String mediafileId, String userId, Map map) throws IOException, ServiceException;

    Response deleteStill(String assetId, String mediafileId, String userId, Map map) throws IOException, ServiceException;

    Response uploadStill(String uploadHost, String assetId, Map map, InputStream stream, String mimeType, String fileName) throws IOException, ServiceException;

    StillType getStills(String assetId, String userId, Map properties) throws ServiceException;

    Response setDefaultStill(String assetId, String userId, Map properties) throws ServiceException;

    //new  Job functionality
    JobDetailsType getJobStatus(String jobId, String userId) throws IOException, ServiceException;


    //new Reposts
    List<StatsPlayedstreamsType> getStatsPlayedStreams(int year, int month, String groupId, String ownerId, String playType, int limit, int offset) throws ServiceException;

    List<StatsPopularstreamsType> getStatsPopularStreams(int limit, int offset) throws ServiceException;

    List<StatsPopularcollectionsType> getStatsPopularCollections() throws ServiceException;

    List<StatsDatauploadType> getStatsDataUpload(int year, int month, String userId, String groupId, int limit, int offset) throws ServiceException;

    List<StatsDatausagevideoType> getStatsDatausageVideo(int year, int month, String type, int limit, int offset) throws ServiceException;

}
