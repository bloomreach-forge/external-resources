package org.onehippo.forge.externalresource.gallery.render;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.request.resource.PackageResourceReference;
import org.hippoecm.frontend.plugins.standards.icon.HippoIcon;
import org.hippoecm.frontend.plugins.standards.list.resolvers.IconRenderer;
import org.hippoecm.repository.api.HippoNodeType;

public class VideoContainerRenderer extends IconRenderer {

    private static final long serialVersionUID = -4385582873739274710L;

    @Override
    protected HippoIcon getIcon(String id, Node node) throws RepositoryException {
        if (node.isNodeType(HippoNodeType.NT_HANDLE)) {
            return HippoIcon.fromResource(id, new PackageResourceReference(VideoContainerRenderer.class, "res/video.png"));
        }
        return super.getIcon(id, node);
    }
}
