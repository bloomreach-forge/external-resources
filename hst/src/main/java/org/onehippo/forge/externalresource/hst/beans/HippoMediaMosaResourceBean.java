package org.onehippo.forge.externalresource.hst.beans;

import org.hippoecm.hst.content.beans.Node;
import org.onehippo.forge.externalresource.api.EmbeddedHelper;
import org.onehippo.forge.externalresource.api.MediaMosaEmbeddedHelper;
import org.onehippo.forge.externalresource.hst.engine.ExternalResourceHstEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;

/**
 * @version $Id$
 */

@Node(jcrType = "hippomediamosa:resource")
public class HippoMediaMosaResourceBean extends HippoExternalVideoResourceBean {

    private static final EmbeddedHelper helper = new MediaMosaEmbeddedHelper();

    @SuppressWarnings({"UnusedDeclaration"})
    private static Logger log = LoggerFactory.getLogger(HippoMediaMosaResourceBean.class);

    public String getAssetId() {
        return getProperty("hippomediamosa:assetid");
    }

    public String getMediaId() {
        return getProperty("hippomediamosa:mediaid");
    }

    public String getEmbeddableVideo() {
        return ExternalResourceHstEngine.getInstance().getEmbedded(this, helper);
    }


}
