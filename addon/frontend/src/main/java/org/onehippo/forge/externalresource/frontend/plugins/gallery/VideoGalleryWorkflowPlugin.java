/*
 *  Copyright 2009 Hippo.
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
package org.onehippo.forge.externalresource.frontend.plugins.gallery;

import java.io.IOException;
import java.io.InputStream;
import java.rmi.RemoteException;
import java.util.LinkedList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.Component;
import org.apache.wicket.Session;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.apache.wicket.markup.repeater.data.ListDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.request.resource.ResourceReference;
import org.apache.wicket.util.lang.Bytes;
import org.hippoecm.addon.workflow.StdWorkflow;
import org.hippoecm.addon.workflow.WorkflowDescriptorModel;
import org.hippoecm.frontend.dialog.IDialogService.Dialog;
import org.hippoecm.frontend.i18n.types.TypeChoiceRenderer;
import org.hippoecm.frontend.model.JcrItemModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.jquery.upload.multiple.JQueryFileUploadDialog;
import org.hippoecm.frontend.service.IBrowseService;
import org.hippoecm.frontend.service.ISettingsService;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.frontend.translation.ILocaleProvider;
import org.hippoecm.frontend.widgets.AbstractView;
import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.StringCodec;
import org.hippoecm.repository.api.StringCodecFactory;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.api.WorkflowManager;
import org.hippoecm.repository.gallery.GalleryWorkflow;
import org.hippoecm.repository.standardworkflow.DefaultWorkflow;
import org.onehippo.forge.externalresource.api.ResourceHandler;
import org.onehippo.forge.externalresource.api.utils.MediaMosaServices;
import org.onehippo.forge.externalresource.frontend.plugins.type.mediamosa.dialog.imports.MediaMosaImportDialog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VideoGalleryWorkflowPlugin extends RenderPlugin<GalleryWorkflow> {
    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(VideoGalleryWorkflowPlugin.class);

    public class UploadDialog extends JQueryFileUploadDialog {
        private static final long serialVersionUID = 1L;

        public UploadDialog(IPluginContext pluginContext, IPluginConfig pluginConfig) {
            super(pluginContext, pluginConfig);
        }

        @Override
        protected void onFileUpload(final FileUpload fileUpload) {
            log.debug("uploaded file {} with size {}", fileUpload.getClientFileName(), fileUpload.getSize());
            createGalleryItem(fileUpload);
        }

        public IModel getTitle() {
            return new StringResourceModel(VideoGalleryWorkflowPlugin.this.getPluginConfig().getString("option.text", ""), VideoGalleryWorkflowPlugin.this, null);
        }

        @Override
        protected void onOk() {
            super.onOk();
            afterUploadItems();
        }
    }

    public String type;
    private List<String> newItems;

    public VideoGalleryWorkflowPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);
        newItems = new LinkedList<>();
        onModelChanged();
    }


    @Override
    public void onModelChanged() {
        AbstractView<StdWorkflow> add;
        addOrReplace(add = new AbstractView<StdWorkflow>("new", createListDataProvider(getPluginContext())) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(Item item) {
                item.add((StdWorkflow) item.getModelObject());
            }
        });
        add.populate();
    }

    private void createGalleryItem(FileUpload upload) {
        try {
            //VideoService service = getVideoService();
            //this is where the magic starts
            String filename = upload.getClientFileName();
            String mimetype;

            mimetype = upload.getContentType();
            InputStream istream = upload.getInputStream();
            WorkflowManager manager = ((UserSession) Session.get()).getWorkflowManager();
            HippoNode node = null;
            try {
                WorkflowDescriptorModel workflowDescriptorModel = (WorkflowDescriptorModel) VideoGalleryWorkflowPlugin.this
                        .getDefaultModel();
                GalleryWorkflow workflow = (GalleryWorkflow) manager.getWorkflow(workflowDescriptorModel.getObject());
                String nodeName = getNodeNameCodec().encode(filename);
                String localName = getLocalizeCodec().encode(filename);
                //here is where it goes wrong
                Document document = workflow.createGalleryItem(nodeName, type);
                ((UserSession) Session.get()).getJcrSession().refresh(true);

                node = (HippoNode) new JcrNodeModel(new JcrItemModel(document.getIdentity(), false)).getNode();

                DefaultWorkflow defaultWorkflow = (DefaultWorkflow) manager.getWorkflow("core", node);
                if (!node.getDisplayName().equals(localName)) {
                    defaultWorkflow.setDisplayName(localName);
                }
            } catch (WorkflowException | RepositoryException ex) {
                VideoGalleryWorkflowPlugin.log.error(ex.getMessage());
                error(ex);
            }
            if (node != null) {
                try {
                    node.setProperty("hippoexternal:mimeType", mimetype);
                    final ResourceHandler processor = MediaMosaServices.forNode(node).getResourceHandler();
                    processor.create(node, istream, mimetype);
                    node.getSession().save();
                    processor.afterSave(node);

                } catch (Exception ex) {
                    if (VideoGalleryWorkflowPlugin.log.isDebugEnabled()) {
                        VideoGalleryWorkflowPlugin.log.info(ex.getMessage(), ex);
                    } else {
                        VideoGalleryWorkflowPlugin.log.info(ex.getMessage());
                    }
                    error(ex);
                    try {
                        DefaultWorkflow defaultWorkflow = (DefaultWorkflow) manager.getWorkflow("core", node);
                        defaultWorkflow.delete();
                    } catch (WorkflowException | RepositoryException e) {
                        VideoGalleryWorkflowPlugin.log.error(e.getMessage());
                    }
                    try {
                        node.getSession().refresh(false);
                    } catch (RepositoryException e) {
                        // deliberate ignore
                    }
                }
                newItems.add(node.getPath());
            }
        } catch (IOException ex) {
            VideoGalleryWorkflowPlugin.log.info("upload of image truncated");
            error((new StringResourceModel("upload-failed-label", VideoGalleryWorkflowPlugin.this, null).getString()));
        } catch (RepositoryException e) {
            VideoGalleryWorkflowPlugin.log.error("upload of image failed", e);
            error((new StringResourceModel("upload-failed-label", VideoGalleryWorkflowPlugin.this, null).getString()));
        }
    }

    private void afterUploadItems() {
        int threshold = getPluginConfig().getAsInteger("select.after.create.threshold", 1);
        if (newItems.size() <= threshold) {
            for (String path : newItems) {
                select(new JcrNodeModel(path));
            }
        }
        newItems.clear();
    }


    protected IDataProvider<StdWorkflow> createListDataProvider(final IPluginContext pluginContext) {
        List<StdWorkflow> list = new LinkedList<>();
        list.add(0, new StdWorkflow("add", new StringResourceModel(getPluginConfig().getString("option.label", "add"), this, null, "Add"), pluginContext, (WorkflowDescriptorModel) getDefaultModel()) {
            private static final long serialVersionUID = 1L;

            @Override
            protected ResourceReference getIcon() {
                return new PackageResourceReference(getClass(), "film-add-icon.png");
            }

            @Override
            protected Dialog createRequestDialog() {
                return createUploadDialog();
            }
        });
        if (getPluginConfig().getAsBoolean("importDisabled", false) == false) {
            //delegate to the WorkflowitemManager
            list.add(1, new StdWorkflow("import", new StringResourceModel(getPluginConfig().getString("option.label.import", "import-video-label"), this, null, "Add"), pluginContext, (WorkflowDescriptorModel) getDefaultModel()) {
                private static final long serialVersionUID = 1L;

                @Override
                protected ResourceReference getIcon() {
                    return new PackageResourceReference(getClass(), "import-16.png");
                }

                @Override
                protected Dialog createRequestDialog() {
                    return createImportDialog();
                }
            });
        }
        return new ListDataProvider<>(list);
    }

    private Dialog createImportDialog() {
        WorkflowDescriptorModel workflowDescriptorModel = (WorkflowDescriptorModel) VideoGalleryWorkflowPlugin.this.getDefaultModel();
        Node node = null;
        try {
            node = workflowDescriptorModel.getNode();
        } catch (RepositoryException e) {
            log.error("", e);
        }
        JcrNodeModel nodeModel = new JcrNodeModel(node);

        return new MediaMosaImportDialog(nodeModel, getPluginContext(), getPluginConfig());
    }

    private Dialog createUploadDialog() {
        List<String> galleryTypes = null;
        try {
            WorkflowManager manager = ((UserSession) Session.get()).getWorkflowManager();
            WorkflowDescriptorModel workflowDescriptorModel = (WorkflowDescriptorModel) VideoGalleryWorkflowPlugin.this.getDefaultModel();
            GalleryWorkflow workflow = (GalleryWorkflow) manager.getWorkflow(workflowDescriptorModel.getObject());
            if (workflow == null) {
                VideoGalleryWorkflowPlugin.log.error("No gallery workflow accessible");
            } else {
                galleryTypes = workflow.getGalleryTypes();
            }
        } catch (RemoteException | RepositoryException ex) {
            VideoGalleryWorkflowPlugin.log.error(ex.getMessage(), ex);
        }

        Component typeComponent;
        if (galleryTypes != null && galleryTypes.size() > 1) {
            type = galleryTypes.get(0);
            typeComponent = new DropDownChoice("type", new PropertyModel(this, "type"), galleryTypes,
                    new TypeChoiceRenderer(this)).setNullValid(false).setRequired(true);
        } else if (galleryTypes != null && galleryTypes.size() == 1) {
            type = galleryTypes.get(0);
            typeComponent = new Label("type", type).setVisible(false);
        } else {
            type = null;
            typeComponent = new Label("type", "default").setVisible(false);
        }

        UploadDialog dialog = new UploadDialog(getPluginContext(), getPluginConfig());
        if (getPluginConfig().containsKey("maxSize")) {
            dialog.setMaxSize(Bytes.valueOf(getPluginConfig().getString("maxSize")));
        }
        dialog.add(typeComponent);
        return dialog;
    }

    protected StringCodec getLocalizeCodec() {
        ISettingsService settingsService = getPluginContext().getService(ISettingsService.SERVICE_ID,
                ISettingsService.class);
        StringCodecFactory stringCodecFactory = settingsService.getStringCodecFactory();
        return stringCodecFactory.getStringCodec("encoding.display");
    }

    protected StringCodec getNodeNameCodec() {
        ISettingsService settingsService = getPluginContext().getService(ISettingsService.SERVICE_ID,
                ISettingsService.class);
        StringCodecFactory stringCodecFactory = settingsService.getStringCodecFactory();
        return stringCodecFactory.getStringCodec("encoding.node");
    }

    protected ILocaleProvider getLocaleProvider() {
        return getPluginContext().getService(
                getPluginConfig().getString(ILocaleProvider.SERVICE_ID, ILocaleProvider.class.getName()),
                ILocaleProvider.class);
    }

    @SuppressWarnings("unchecked")
    public void select(JcrNodeModel nodeModel) {
        IBrowseService<JcrNodeModel> browser = getPluginContext().getService(
                getPluginConfig().getString(IBrowseService.BROWSER_ID), IBrowseService.class);
        if (browser != null) {
            try {
                if (nodeModel.getNode() != null
                        && (nodeModel.getNode().isNodeType(HippoNodeType.NT_DOCUMENT) || nodeModel.getNode()
                        .isNodeType(HippoNodeType.NT_HANDLE))) {
                    browser.browse(nodeModel);
                }
            } catch (RepositoryException ex) {
                log.error(ex.getClass().getName() + ": " + ex.getMessage(), ex);
            }
        }
    }


}