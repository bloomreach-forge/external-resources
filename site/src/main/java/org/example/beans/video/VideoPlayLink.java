package org.example.beans.video;

import org.hippoecm.hst.core.component.HstRequest;

/**
 * @version $Id$
 */
public interface VideoPlayLink {

    void setEmbedded(HstRequest request);

    String getEmbedded();

}
