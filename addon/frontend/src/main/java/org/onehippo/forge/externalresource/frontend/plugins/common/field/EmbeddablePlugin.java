package org.onehippo.forge.externalresource.frontend.plugins.common.field;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.cxf.common.util.StringUtils;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.MarkupStream;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.frontend.editor.plugins.field.FieldPluginHelper;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.frontend.types.IFieldDescriptor;
import org.onehippo.forge.externalresource.api.Embeddable;
import org.onehippo.forge.externalresource.api.service.ExternalResourceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Editor Plugin rendering the HTML embed code for videos, getting it from a type processor when available from the
 * CMS external resources service.
 *
 * @version $Id$
 */
public class EmbeddablePlugin extends RenderPlugin<Node> {
    @SuppressWarnings({"UnusedDeclaration"})
    private static Logger log = LoggerFactory.getLogger(EmbeddablePlugin.class);

    private JcrNodeModel nodeModel;
    private FieldPluginHelper helper;

    public EmbeddablePlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        nodeModel = (JcrNodeModel) getDefaultModel();

        helper = new FieldPluginHelper(context, config);
        // use caption for backwards compatibility; i18n should use field name
        add(new Label("name", getCaptionModel()));

        add(createResourceFragment("fragment"));
    }

    /**
     * Get the CMS service by id (as defined by external.processor.id) and class ExternalResourceService.
     */
    protected ExternalResourceService getExternalResourceService() {
        final String serviceId = getPluginConfig().getString("external.processor.id", "external.processor.service");
        final ExternalResourceService service = getPluginContext().getService(serviceId, ExternalResourceService.class);
        if (service == null) {
            log.warn("No external resource service found by id {} (id configured by property external.processor.id)", service);
        }
        return service;
    }

    protected IModel<String> getCaptionModel() {
        IFieldDescriptor field = getFieldHelper().getField();
        String caption = getPluginConfig().getString("caption");
        String captionKey = field != null ? field.getName() : caption;
        if (captionKey == null) {
            return new Model("undefined");
        }
        if (caption == null && field != null && field.getName().length() >= 1) {
            caption = field.getName().substring(0, 1).toUpperCase() + field.getName().substring(1);
        }
        return new StringResourceModel(captionKey, this, null, caption);
    }

    protected FieldPluginHelper getFieldHelper() {
        return helper;
    }


    private Fragment createResourceFragment(String id) {

        final Node node = nodeModel.getNode();
        String primaryNodeType = null;
        try {
            primaryNodeType = node.getPrimaryNodeType().getName();
        } catch (RepositoryException e) {
            log.error("RepositoryException creating resource fragment", e);
        }

        if (primaryNodeType != null) {

            ExternalResourceService service = getExternalResourceService();

            if (service != null) {
                Embeddable processor = service.getEmbeddableProcessor(primaryNodeType);

                if (processor == null) {
                    log.warn("No embeddable processor found in ExternalResourceService by primaryNodeType {}",  primaryNodeType);
                }
                else {
                    final String embedded = processor.getEmbedded(node);

                    WebMarkupContainer frame = new WebMarkupContainer("value") {
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
            }
        }

        return new Fragment(id, "unknown", this);
    }

}
