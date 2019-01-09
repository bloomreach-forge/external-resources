package org.onehippo.forge.externalresource.frontend.plugins.gallery;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.rmi.RemoteException;
import java.util.LinkedList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.Component;
import org.apache.wicket.Session;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.markup.html.panel.EmptyPanel;
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
import org.hippoecm.frontend.plugins.standardworkflow.AddDocumentArguments;
import org.hippoecm.frontend.plugins.yui.upload.validation.DefaultUploadValidationService;
import org.hippoecm.frontend.plugins.yui.upload.validation.FileUploadValidationService;
import org.hippoecm.frontend.service.IBrowseService;
import org.hippoecm.frontend.service.ISettingsService;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.frontend.session.UserSession;
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
    private List<String> newItems;
    private AddDocumentArguments addDocumentModel = new AddDocumentArguments();

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

    public class UploadDialog extends JQueryFileUploadDialog {
        private static final long serialVersionUID = 1L;
        private File uploadedVideoFile;
        private String filename;
        private String mimetype;

        public UploadDialog(IPluginContext pluginContext, IPluginConfig pluginConfig, AddDocumentArguments addDocumentModel) {
            super(pluginContext, pluginConfig);

            List<String> galleryTypes = null;
            try {
                WorkflowManager manager = ((UserSession) Session.get()).getWorkflowManager();
                WorkflowDescriptorModel workflowDescriptorModel = (WorkflowDescriptorModel) VideoGalleryWorkflowPlugin.this.getDefaultModel();
                GalleryWorkflow workflow = (GalleryWorkflow) manager.getWorkflow(workflowDescriptorModel.getObject());
                if (workflow == null) {
                    log.error("No gallery workflow accessible");
                } else {
                    galleryTypes = workflow.getGalleryTypes();
                }
            } catch (RemoteException | RepositoryException ex) {
                log.error(ex.getMessage(), ex);
            }

            Component typeComponent;

            final PropertyModel<String> prototypeModel = new PropertyModel<>(addDocumentModel, "prototype");
            if (galleryTypes != null && galleryTypes.size() > 1) {
                typeComponent = new DropDownChoice("type", prototypeModel , galleryTypes,
                        new TypeChoiceRenderer(this)).setNullValid(false).setRequired(true);
            } else if (galleryTypes != null && galleryTypes.size() == 1) {
                typeComponent = new EmptyPanel("type").setVisible(false);
                prototypeModel.setObject(galleryTypes.iterator().next());
            } else {
                typeComponent = new EmptyPanel("type").setVisible(false);
            }

            add(typeComponent);
        }

        @Override
        protected void onFileUpload(final FileUpload fileUpload) {
            log.debug("uploaded file {} with size {}", fileUpload.getClientFileName(), fileUpload.getSize());
            try {
                this.uploadedVideoFile = fileUpload.writeToTempFile();
                this.filename = fileUpload.getClientFileName();
                this.mimetype = fileUpload.getContentType();
            } catch (IOException e) {
                log.error("cannot store the uploaded file in temp", e);
            }
        }

        @Override
        public IModel getTitle() {
            return new StringResourceModel(VideoGalleryWorkflowPlugin.this.getPluginConfig().getString("option.text", ""), VideoGalleryWorkflowPlugin.this, null);
        }

        @Override
        protected FileUploadValidationService getValidator() {
            FileUploadValidationService fileUploadValidationService = DefaultUploadValidationService.getValidationService(getPluginContext(), getPluginConfig(), "service.gallery.video.validation");
            return fileUploadValidationService;
        }

        @Override
        protected void onFinished() {
            createGalleryItem();
            super.onFinished();
        }

        private void createGalleryItem() {
            try {
                //this is where the magic starts
                InputStream istream = new FileInputStream(uploadedVideoFile);
                uploadedVideoFile.deleteOnExit();
                WorkflowManager manager = ((UserSession) Session.get()).getWorkflowManager();
                HippoNode node = null;
                try {
                    WorkflowDescriptorModel workflowDescriptorModel = (WorkflowDescriptorModel) VideoGalleryWorkflowPlugin.this
                            .getDefaultModel();
                    GalleryWorkflow workflow = (GalleryWorkflow) manager.getWorkflow(workflowDescriptorModel.getObject());
                    String nodeName = getNodeNameCodec().encode(filename);
                    String localName = getLocalizeCodec().encode(filename);
                    //here is where it goes wrong
                    Document document = workflow.createGalleryItem(nodeName, addDocumentModel.getPrototype());
                    ((UserSession) Session.get()).getJcrSession().refresh(true);

                    node = (HippoNode) new JcrNodeModel(new JcrItemModel(document.getIdentity(), false)).getNode();

                    DefaultWorkflow defaultWorkflow = (DefaultWorkflow) manager.getWorkflow("core", node);
                    if (!node.getDisplayName().equals(localName)) {
                        defaultWorkflow.setDisplayName(localName);
                    }
                } catch (WorkflowException | RepositoryException ex) {
                    log.error(ex.getMessage());
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
                        if (log.isDebugEnabled()) {
                            log.info(ex.getMessage(), ex);
                        } else {
                            log.info(ex.getMessage());
                        }
                        error(ex);
                        try {
                            DefaultWorkflow defaultWorkflow = (DefaultWorkflow) manager.getWorkflow("core", node);
                            defaultWorkflow.delete();
                        } catch (WorkflowException | RepositoryException e) {
                            log.warn("error executing workflow delete", e);
                        }
                        try {
                            node.getSession().refresh(false);
                        } catch (RepositoryException e) {
                            log.warn("could not refresh the session", e);
                        }
                    }
                    newItems.add(node.getPath());
                }
            } catch (IOException ex) {
                log.warn("upload of image truncated", ex);
                error((new StringResourceModel("upload-failed-label", VideoGalleryWorkflowPlugin.this, null).getString()));
                if (uploadedVideoFile != null && uploadedVideoFile.exists()) {
                    boolean tempFileDeleted = uploadedVideoFile.delete();
                    if (!tempFileDeleted) {
                        log.debug("Temp file {} was not deleted",uploadedVideoFile.getAbsolutePath());
                    }
                }
            } catch (RepositoryException e) {
                log.error("upload of image failed", e);
                error((new StringResourceModel("upload-failed-label", VideoGalleryWorkflowPlugin.this, null).getString()));
            }

            int threshold = getPluginConfig().getAsInteger("select.after.create.threshold", 1);
            if (newItems.size() <= threshold) {
                for (String path : newItems) {
                    select(new JcrNodeModel(path));
                }
            }
            newItems.clear();
        }
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
        if (!getPluginConfig().getAsBoolean("importDisabled", false)) {
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

        UploadDialog dialog = new UploadDialog(getPluginContext(), getPluginConfig(), addDocumentModel);
        if (getPluginConfig().containsKey("maxSize")) {
            dialog.setMaxSize(Bytes.valueOf(getPluginConfig().getString("maxSize")));
        }
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