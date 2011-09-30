package org.onehippo.forge.externalresource.api;

import nl.uva.mediamosa.MediaMosaService;
import nl.uva.mediamosa.model.AssetDetailsType;
import nl.uva.mediamosa.util.ServiceException;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.onehippo.forge.externalresource.api.utils.WorkaroundModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;

/**
 * @version $Id$
 */
public class MediaMosaTask {
    @SuppressWarnings({"UnusedDeclaration"})
    private static Logger log = LoggerFactory.getLogger(MediaMosaTask.class);

    private final long TIMEOUT = 100000;

    public Boolean execute(final String path, final MediaMosaService mediaMosaService) {
        try {
            Thread.sleep(500);

            Session session = WorkaroundModule.getSession();

            if (session.itemExists(path)) {
                Node node = session.getNode(path);

                String assetId = node.getProperty("hippomediamosa:assetid").getString();

                boolean hasNoImage = true;

                long start = System.currentTimeMillis();

                System.out.println("trying to do " + assetId);

                while (hasNoImage) {
                    if (System.currentTimeMillis() > (start + TIMEOUT)) {
                        log.error("TIMEOUT {}");
                        System.out.println("timeout occured");
                        break;
                    }

                    Thread.sleep(10000);

                    System.out.println("start  " + assetId);
                    //String response = mediaMosaService.doGetRequestString("/asset/" + assetId + "/still?user_id=" + manager.getUsername());
                    AssetDetailsType detail = mediaMosaService.getAssetDetails(assetId);

                    if (StringUtils.isNotBlank(detail.getVpxStillUrl())) {
                        String imageUrl = detail.getVpxStillUrl();

                        System.out.println("image found  " + assetId);

                        hasNoImage = false;

                        HttpClient client = new HttpClient();
                        HttpMethod getMethod = new GetMethod(imageUrl);
                        InputStream is = null;
                        //ByteArrayOutputStream output =null;
                        try {
                            client.executeMethod(getMethod);
                            is = getMethod.getResponseBodyAsStream();
                            String mimeType = getMethod.getResponseHeader("content-type").getValue();
                            //
                            if (mimeType.startsWith("image")) {
                                if (node.hasNode("hippoexternal:thumbnail") && node.hasNode("hippoexternal:preview")) {
                                    //output = new ByteArrayOutputStream();
                                    //IOUtils.copy(is, output);
                                    Node preview = node.getNode("hippoexternal:preview");
                                    preview.setProperty("jcr:data", node.getSession().getValueFactory().createBinary(is));
                                    preview.setProperty("jcr:mimeType", mimeType);
                                    preview.setProperty("jcr:lastModified", Calendar.getInstance());

                                    Node thumbnail = node.getNode("hippoexternal:thumbnail");
                                    thumbnail.setProperty("jcr:data", node.getSession().getValueFactory().createValue(preview.getProperty("jcr:data").getBinary()));
                                    thumbnail.setProperty("jcr:mimeType", mimeType);
                                    thumbnail.setProperty("jcr:lastModified", Calendar.getInstance());
                                    session.save();
                                }

                            }
                            break;
                        } catch (IOException e) {
                            log.error("", e);
                        } finally {
                            IOUtils.closeQuietly(is);
                            //IOUtils.closeQuietly(output);
                            break;
                        }

                    }
                }
            } else {
                System.out.println("item doesn't exist");
            }
        } catch (RepositoryException e) {
            log.error("", e);
        } catch (InterruptedException e) {
            log.error("", e);
        } catch (ServiceException e) {
            log.error("", e);
        } catch (IOException e) {
            log.error("", e);
        }

        return null;
    }


}
