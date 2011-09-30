package org.example.beans;

import org.example.beans.video.HippoExternalVideo;
import org.example.beans.video.HippoMediaMosaResource;
import org.example.beans.video.VideoLink;
import org.example.beans.video.VideoPlayLink;
import org.example.components.MediaMosaHstService;
import org.hippoecm.hst.content.beans.Node;
import org.hippoecm.hst.content.beans.standard.HippoGalleryImageSetBean;
import org.hippoecm.hst.core.component.HstRequest;

import java.util.Calendar;

@Node(jcrType = "myhippoproject:newsdocument")
public class NewsDocument extends TextDocument implements VideoPlayLink {

    public Calendar getDate() {
        return getProperty("myhippoproject:date");
    }

    /**
     * Get the imageset of the newspage
     *
     * @return the imageset of the newspage
     */
    public HippoGalleryImageSetBean getImage() {
        return getLinkedBean("myhippoproject:image", HippoGalleryImageSetBean.class);
    }

    public HippoExternalVideo getVideo() {
        VideoLink videoLink = (VideoLink) getBean("hippoexternal:link");
        if (videoLink != null) {
            return (HippoExternalVideo) videoLink.getReferencedBean();
        }
        return null;

    }

    String embedded;

    public void setEmbedded(HstRequest request) {
        String assetId = ((HippoMediaMosaResource) getVideo()).getAssetId();
        this.embedded = MediaMosaHstService.getPlayLink(request, assetId);
    }

    public String getEmbedded() {
        return embedded;
    }
}
