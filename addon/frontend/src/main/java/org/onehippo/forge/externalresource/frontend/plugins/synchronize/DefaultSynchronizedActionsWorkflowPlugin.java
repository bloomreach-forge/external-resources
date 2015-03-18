package org.onehippo.forge.externalresource.frontend.plugins.synchronize;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.request.resource.ResourceReference;
import org.apache.wicket.util.string.Strings;
import org.apache.wicket.util.value.IValueMap;
import org.hippoecm.addon.workflow.StdWorkflow;
import org.hippoecm.addon.workflow.WorkflowDescriptorModel;
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.dialog.ExceptionDialog;
import org.hippoecm.frontend.dialog.IDialogService;
import org.hippoecm.frontend.editor.workflow.CopyNameHelper;
import org.hippoecm.frontend.editor.workflow.dialog.DeleteDialog;
import org.hippoecm.frontend.editor.workflow.dialog.WhereUsedDialog;
import org.hippoecm.frontend.i18n.model.NodeTranslator;
import org.hippoecm.frontend.i18n.types.TypeTranslator;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.NodeModelWrapper;
import org.hippoecm.frontend.model.nodetypes.JcrNodeTypeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standards.list.resolvers.CssClassAppender;
import org.hippoecm.frontend.service.IBrowseService;
import org.hippoecm.frontend.service.IEditor;
import org.hippoecm.frontend.service.IEditorManager;
import org.hippoecm.frontend.service.ISettingsService;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.StringCodec;
import org.hippoecm.repository.api.StringCodecFactory;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowDescriptor;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.api.WorkflowManager;
import org.hippoecm.repository.standardworkflow.DefaultWorkflow;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.forge.externalresource.api.ResourceHandler;
import org.onehippo.forge.externalresource.api.Synchronizable;
import org.onehippo.forge.externalresource.api.utils.HippoExtConst;
import org.onehippo.forge.externalresource.api.workflow.SynchronizedActionsWorkflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version $Id$
 */
public class DefaultSynchronizedActionsWorkflowPlugin extends RenderPlugin {

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(DefaultSynchronizedActionsWorkflowPlugin.class);


    private String synchState = "";
    StdWorkflow infoAction;

    private StdWorkflow editAction;
    private StdWorkflow deleteAction;
    private StdWorkflow renameAction;
    private StdWorkflow copyAction;
    private StdWorkflow moveAction;
    private StdWorkflow whereUsedAction;

    public DefaultSynchronizedActionsWorkflowPlugin(final IPluginContext context, final IPluginConfig config) {
        super(context, config);

        onModelChanged();
        /////
        final TypeTranslator translator = new TypeTranslator(new JcrNodeTypeModel("hippoexternal:synchronizable"));
        add(infoAction = new StdWorkflow("info", "info") {

            @Override
            protected IModel getTitle() {
                try {
                    Node document = ((WorkflowDescriptorModel) DefaultSynchronizedActionsWorkflowPlugin.this.getDefaultModel()).getNode();
                    Synchronizable sync = HippoServiceRegistry.getService(Synchronizable.class, document.getPrimaryNodeType().getName() + HippoExtConst.SYNCHRONIZABLE);
                    synchState = sync.check(document).getState();
                } catch (RepositoryException e) {
                    log.error("", e);
                }

                return translator.getValueName("hippoexternal:state", new PropertyModel(DefaultSynchronizedActionsWorkflowPlugin.this, "synchState"));
            }
        });
        ///

        add(editAction = new StdWorkflow("edit", new StringResourceModel("edit", this, null)) {
            @Override
            protected ResourceReference getIcon() {
                return new PackageResourceReference(getClass(), "edit-16.png");
            }

            @Override
            protected String execute(Workflow wf) throws Exception {
                Node docNode = ((WorkflowDescriptorModel) DefaultSynchronizedActionsWorkflowPlugin.this.getDefaultModel()).getNode();
                IEditorManager editorMgr = getPluginContext().getService(
                        getPluginConfig().getString(IEditorManager.EDITOR_ID), IEditorManager.class);
                if (editorMgr != null) {
                    JcrNodeModel docModel = new JcrNodeModel(docNode);
                    IEditor editor = editorMgr.getEditor(docModel);
                    if (editor == null) {
                        editorMgr.openEditor(docModel);
                    } else {
                        editor.setMode(IEditor.Mode.EDIT);
                    }
                } else {
                    log.warn("No editor found to edit {}", docNode.getPath());
                }
                return null;
            }
        });

        add(renameAction = new StdWorkflow("rename", new StringResourceModel("rename-label", this, null)) {
            public String targetName;
            public String uriName;

            @Override
            protected ResourceReference getIcon() {
                return new PackageResourceReference(getClass(), "rename-16.png");
            }

            @Override
            protected IDialogService.Dialog createRequestDialog() {
                try {
                    uriName = ((WorkflowDescriptorModel) getDefaultModel()).getNode().getName();
                    targetName = ((HippoNode) ((WorkflowDescriptorModel) getDefaultModel()).getNode())
                            .getLocalizedName();
                } catch (RepositoryException ex) {
                    uriName = targetName = "";
                }
                return new RenameDocumentDialog(this, new StringResourceModel("rename-title", DefaultSynchronizedActionsWorkflowPlugin.this, null));
            }

            @Override
            protected String execute(Workflow wf) throws Exception {
                if (targetName == null || targetName.trim().isEmpty()) {
                    throw new WorkflowException("No name for destination given");
                }
                HippoNode node = (HippoNode) ((WorkflowDescriptorModel) getDefaultModel()).getNode();
                String nodeName = getNodeNameCodec().encode(uriName);
                String localName = getLocalizeCodec().encode(targetName);
                if (nodeName != null && nodeName.isEmpty()) {
                    throw new IllegalArgumentException("You need to enter a name");
                }
                WorkflowManager manager = ((UserSession) Session.get()).getWorkflowManager();
                DefaultWorkflow defaultWorkflow = (DefaultWorkflow) manager.getWorkflow("core", node);
                if (!((WorkflowDescriptorModel) getDefaultModel()).getNode().getName().equals(nodeName)) {
                    ((DefaultWorkflow) wf).rename(nodeName);
                }
                if (!node.getLocalizedName().equals(localName)) {
                    defaultWorkflow.localizeName(localName);
                }
                return null;
            }
        });

        add(copyAction = new StdWorkflow("copy", new StringResourceModel("copy-label", this, null), null) {
            NodeModelWrapper destination = null;
            String name = null;

            @Override
            protected ResourceReference getIcon() {
                return new PackageResourceReference(getClass(), "copy-16.png");
            }

            @Override
            protected IDialogService.Dialog createRequestDialog() {
                destination = new NodeModelWrapper(getFolder()) {
                };
                CopyNameHelper copyNameHelper = new CopyNameHelper(getNodeNameCodec(), new StringResourceModel(
                        "copyof", DefaultSynchronizedActionsWorkflowPlugin.this, null).getString());
                try {
                    name = copyNameHelper.getCopyName(((HippoNode) ((WorkflowDescriptorModel) getDefaultModel())
                            .getNode()).getLocalizedName(), destination.getNodeModel().getNode());
                } catch (RepositoryException ex) {
                    return new ExceptionDialog(ex);
                }
                return new DestinationDialog(context, config,
                        new StringResourceModel("copy-title", DefaultSynchronizedActionsWorkflowPlugin.this, null),
                        new StringResourceModel("copy-name", DefaultSynchronizedActionsWorkflowPlugin.this, null),
                        new PropertyModel(this, "name"),
                        destination) {
                    {
                        setOkEnabled(true);
                    }
                };
            }

            @Override
            protected String execute(Workflow wf) throws Exception {
                JcrNodeModel folderModel = new JcrNodeModel("/");
                if (destination != null) {
                    folderModel = destination.getNodeModel();
                }
                StringCodec codec = getNodeNameCodec();
                String nodeName = codec.encode(name);

                DefaultWorkflow workflow = (DefaultWorkflow) wf;
                workflow.copy(new Document(folderModel.getNode().getUUID()), nodeName);
                JcrNodeModel copyMode = new JcrNodeModel(folderModel.getItemModel().getPath() + '/' + nodeName);
                HippoNode node = (HippoNode) copyMode.getNode().getNode(nodeName);

                String localName = getLocalizeCodec().encode(name);
                if (!node.getLocalizedName().equals(localName)) {
                    WorkflowManager manager = ((UserSession) Session.get()).getWorkflowManager();
                    DefaultWorkflow defaultWorkflow = (DefaultWorkflow) manager.getWorkflow("core", node);
                    defaultWorkflow.localizeName(localName);
                }
                browseTo(copyMode);
                return null;
            }
        });

        add(moveAction = new StdWorkflow("move", new StringResourceModel("move-label", this, null)) {
            public NodeModelWrapper destination = null;

            @Override
            protected ResourceReference getIcon() {
                return new PackageResourceReference(getClass(), "move-16.png");
            }

            @Override
            protected IDialogService.Dialog createRequestDialog() {
                destination = new NodeModelWrapper(getFolder()) {
                };
                return new DestinationDialog(context, config, new StringResourceModel("move-title",
                        DefaultSynchronizedActionsWorkflowPlugin.this, null), null, null, destination);
            }

            @Override
            protected String execute(Workflow wf) throws Exception {
                JcrNodeModel folderModel = new JcrNodeModel("/");
                if (destination != null) {
                    folderModel = destination.getNodeModel();
                }
                String nodeName = ((WorkflowDescriptorModel) getDefaultModel()).getNode().getName();
                DefaultWorkflow workflow = (DefaultWorkflow) wf;
                workflow.move(new Document(folderModel.getNode().getIdentifier()), nodeName);
                return null;
            }
        });

        add(deleteAction = new StdWorkflow("delete",
                new StringResourceModel("delete-label", this, null).getString()) {
            @Override
            protected ResourceReference getIcon() {
                return new PackageResourceReference(getClass(), "delete-16.png");
            }

            @Override
            protected IDialogService.Dialog createRequestDialog() {
                final IModel<String> docName = getDocumentName();
                IModel<String> message = new StringResourceModel("delete-message", DefaultSynchronizedActionsWorkflowPlugin.this, null,
                        docName);
                IModel<String> title = new StringResourceModel("delete-title", DefaultSynchronizedActionsWorkflowPlugin.this, null,
                        docName);
                return new DeleteDialog(title, this.getModel(), message, this, getEditorManager());
            }

            @Override
            protected String execute(Workflow wf) throws Exception {
                Node document = ((WorkflowDescriptorModel) getDefaultModel()).getNode();
                ResourceHandler manager = HippoServiceRegistry.getService(ResourceHandler.class, document.getPrimaryNodeType().getName() + HippoExtConst.EMBEDDABLE);
                ((SynchronizedActionsWorkflow) wf).delete(manager);
                return null;
            }
        });

        add(whereUsedAction = new StdWorkflow("where-used", new StringResourceModel("where-used-label", this, null)
                .getString()) {
            @Override
            protected ResourceReference getIcon() {
                return new PackageResourceReference(getClass(), "where-used-16.png");
            }

            @Override
            protected IDialogService.Dialog createRequestDialog() {
                WorkflowDescriptorModel wdm = (WorkflowDescriptorModel) getDefaultModel();
                return new WhereUsedDialog(wdm, getEditorManager());
            }

            @Override
            protected String execute(Workflow wf) throws Exception {
                return null;
            }
        });
    }

    private JcrNodeModel getFolder() {
        JcrNodeModel folderModel = new JcrNodeModel("/");
        try {
            WorkflowDescriptorModel wdm = (WorkflowDescriptorModel) getDefaultModel();
            if (wdm != null) {
                HippoNode node = (HippoNode) wdm.getNode();
                if (node != null) {
                    folderModel = new JcrNodeModel(node.getParent().getParent());
                }
            }
        } catch (RepositoryException ex) {
            log.warn("Could not determine folder path", ex);
        }
        return folderModel;
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

    private void browseTo(JcrNodeModel nodeModel) throws RepositoryException {
        //refresh session before IBrowseService.browse is called
        ((UserSession) org.apache.wicket.Session.get()).getJcrSession().refresh(false);

        IBrowseService service = getPluginContext().getService(getPluginConfig().getString(IBrowseService.BROWSER_ID), IBrowseService.class);
        if (service != null) {
            service.browse(nodeModel);
        } else {
            log.warn("No browser service found, cannot open document");
        }
    }

    IModel<String> getDocumentName() {
        try {
            return (new NodeTranslator(new JcrNodeModel(((WorkflowDescriptorModel) getDefaultModel()).getNode())))
                    .getNodeName();
        } catch (RepositoryException ex) {
            try {
                return new Model<>(((WorkflowDescriptorModel) getDefaultModel()).getNode().getName());
            } catch (RepositoryException e) {
                return new StringResourceModel("unknown", this, null);
            }
        }
    }

    IEditorManager getEditorManager() {
        return getPluginContext().getService(getPluginConfig().getString("editor.id"), IEditorManager.class);
    }

    @Override
    public void onModelChanged() {
        super.onModelChanged();
        WorkflowDescriptorModel model = (WorkflowDescriptorModel) getDefaultModel();
        if (model != null) {
            try {
                Node documentNode = model.getNode();
                //  new
                if (documentNode != null && documentNode.hasProperty("hippoexternal:state")) {
                    synchState = documentNode.getProperty("hippoexternal:state").getString();
                }
                //
                WorkflowDescriptor workflowDescriptor = model.getObject();
                if (workflowDescriptor != null) {
                    WorkflowManager manager = ((UserSession) org.apache.wicket.Session.get()).getWorkflowManager();
                    Workflow workflow = manager.getWorkflow(workflowDescriptor);
                    Map<String, Serializable> info = workflow.hints();
                    if (info != null) {
                        if (info.containsKey("edit") && info.get("edit") instanceof Boolean
                                && !(Boolean) info.get("edit")) {
                            editAction.setVisible(false);
                        }
                        if (info.containsKey("delete") && info.get("delete") instanceof Boolean
                                && !(Boolean) info.get("delete")) {
                            deleteAction.setVisible(false);
                        }
                        if (info.containsKey("rename") && info.get("rename") instanceof Boolean
                                && !(Boolean) info.get("rename")) {
                            renameAction.setVisible(false);
                        }
                        if (info.containsKey("move") && info.get("move") instanceof Boolean
                                && !(Boolean) info.get("move")) {
                            moveAction.setVisible(false);
                        }
                        if (info.containsKey("copy") && info.get("copy") instanceof Boolean
                                && !(Boolean) info.get("copy")) {
                            copyAction.setVisible(false);
                        }
                        if (info.containsKey("status") && info.get("status") instanceof Boolean
                                && !(Boolean) info.get("status")) {
                            whereUsedAction.setVisible(false);
                        }
                    }
                }
            } catch (RepositoryException | WorkflowException | RemoteException ex) {
                log.error(ex.getMessage());
            }
        }
    }

    public class RenameDocumentDialog extends AbstractDialog {
        private IModel title;
        private TextField nameComponent;
        private TextField uriComponent;
        private boolean uriModified;

        public RenameDocumentDialog(StdWorkflow action, IModel title) {

            this.title = title;

            final PropertyModel<String> nameModel = new PropertyModel<String>(action, "targetName");
            final PropertyModel<String> uriModel = new PropertyModel<String>(action, "uriName");

            String s1 = nameModel.getObject();
            String s2 = uriModel.getObject();
            uriModified = !s1.equals(s2);

            nameComponent = new TextField<>("name", nameModel);
            nameComponent.setRequired(true);
            nameComponent.setLabel(new StringResourceModel("name-label", DefaultSynchronizedActionsWorkflowPlugin.this, null));
            nameComponent.add(new OnChangeAjaxBehavior() {
                @Override
                protected void onUpdate(AjaxRequestTarget target) {
                    if (!uriModified) {
                        uriModel.setObject(getNodeNameCodec().encode(nameModel.getObject()));
                        target.add(uriComponent);
                    }
                }
            });
            //.setThrottleDelay(Duration.milliseconds(500)));
            nameComponent.setOutputMarkupId(true);
            setFocus(nameComponent);
            add(nameComponent);

            add(uriComponent = new TextField<String>("uriinput", uriModel) {
                @Override
                public boolean isEnabled() {
                    return uriModified;
                }
            });

            uriComponent.add(new CssClassAppender(new AbstractReadOnlyModel<String>() {
                @Override
                public String getObject() {
                    return uriModified ? "grayedin" : "grayedout";
                }
            }));
            uriComponent.setOutputMarkupId(true);

            AjaxLink<Boolean> uriAction = new AjaxLink<Boolean>("uriAction") {
                @Override
                public void onClick(AjaxRequestTarget target) {
                    uriModified = !uriModified;
                    if (!uriModified) {
                        uriModel.setObject(Strings.isEmpty(nameModel.getObject()) ? "" : getNodeNameCodec().encode(
                                nameModel.getObject()));
                    } else {
                        target.focusComponent(uriComponent);
                    }
                    target.add(RenameDocumentDialog.this);
                }
            };
            uriAction.add(new Label("uriActionLabel", new AbstractReadOnlyModel<String>() {
                @Override
                public String getObject() {
                    return uriModified ? getString("url-reset") : getString("url-edit");
                }
            }));
            add(uriAction);
        }

        @Override
        public IModel getTitle() {
            return title;
        }

        @Override
        public IValueMap getProperties() {
            return MEDIUM;
        }
    }
}
