package org.example.components;

import org.example.beans.video.VideoPlayLink;
import org.hippoecm.hst.component.support.bean.BaseHstComponent;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Detail extends BaseHstComponent {

    public static final Logger log = LoggerFactory.getLogger(Detail.class);

    @Override
    public void doBeforeRender(HstRequest request, HstResponse response) throws HstComponentException {

        HippoBean n = getContentBean(request);

        if (n instanceof VideoPlayLink) {
            log.info("n is a videoembeddable thing {}", n);
            ((VideoPlayLink) n).setEmbedded(request);
        }

        if (n == null) {
            return;
        }
        request.setAttribute("document", n);

    }

}
