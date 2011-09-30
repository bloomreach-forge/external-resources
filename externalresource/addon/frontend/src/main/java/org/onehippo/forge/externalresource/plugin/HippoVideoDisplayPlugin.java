/*
 *  Copyright 2008 Hippo.
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.onehippo.forge.externalresource.plugin;

import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.MarkupStream;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.onehippo.forge.externalresource.VideobankPlugin;
import org.onehippo.forge.externalresource.api.Embeddable;
import org.onehippo.forge.externalresource.api.ResourceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

public class HippoVideoDisplayPlugin extends RenderPlugin<Node> {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(HippoVideoDisplayPlugin.class);

    public HippoVideoDisplayPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);
        add(createResourceFragment("fragment", getModel()));

    }

    protected VideobankPlugin getVideoService() {
        IPluginContext context = getPluginContext();
        VideobankPlugin service = context.getService(getPluginConfig().getString("video.processor.id",
                "video.processor.service"), VideobankPlugin.class);
        if (service != null) {
            return service;
        }
        return null;
    }

    private Fragment createResourceFragment(String id, IModel<Node> model) {
        try {
            final Node embeddable = model.getObject();

            VideobankPlugin service = getVideoService();

            ResourceManager processor = service.getResourceProcessor(embeddable.getPrimaryNodeType().getName());

            if (processor instanceof Embeddable) {
                final String embedded = ((Embeddable) processor).getEmbedded(embeddable);

                WebMarkupContainer frame = new WebMarkupContainer("value", model) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected void onComponentTagBody(final MarkupStream markupStream, final ComponentTag openTag) {
                        if (embedded != null) {
                            replaceComponentTagBody(markupStream, openTag, embedded);
                        } else {
                            renderComponentTagBody(markupStream, openTag);
                        }
                    }
                };

                Fragment fragment = new Fragment(id, "html", this);

                fragment.add(frame);
                return fragment;
            }
        } catch (RepositoryException e) {
            log.error("", e);
        }
        return new Fragment(id, "unknown", this);
    }


    @Override
    protected void onModelChanged() {
        replace(createResourceFragment("fragment", getModel()));
        super.onModelChanged();
        redraw();
    }

}
