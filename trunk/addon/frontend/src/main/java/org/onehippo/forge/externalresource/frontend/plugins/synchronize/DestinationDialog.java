package org.onehippo.forge.externalresource.frontend.plugins.synchronize;

import java.util.Iterator;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.util.value.IValueMap;
import org.hippoecm.addon.workflow.CompatibilityWorkflowPlugin;
import org.hippoecm.frontend.PluginRequestTarget;
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.dialog.DialogConstants;
import org.hippoecm.frontend.model.IModelReference;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.NodeModelWrapper;
import org.hippoecm.frontend.model.event.IEvent;
import org.hippoecm.frontend.model.event.IObserver;
import org.hippoecm.frontend.plugin.IClusterControl;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IClusterConfig;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.config.IPluginConfigService;
import org.hippoecm.frontend.service.IRenderService;
import org.hippoecm.frontend.service.ServiceTracker;
import org.hippoecm.frontend.widgets.TextFieldWidget;


public class DestinationDialog extends AbstractDialog {

    private final IPluginContext context;
    private IModel title;
    private IRenderService dialogRenderer;
    private IClusterControl control;
    private String modelServiceId;
    private ServiceTracker tracker;

    public DestinationDialog(final IPluginContext context,final IPluginConfig config, IModel title, IModel question, PropertyModel nameModel, final NodeModelWrapper destination) {
        this.context = context;
        this.title = title;
        if (question != null) {
            add(new Label("question", question));
        } else {
            Label dummy = new Label("question");
            dummy.setVisible(false);
            add(dummy);
        }
        if (nameModel != null) {
            add(new TextFieldWidget("name", nameModel));
        } else {
            Label dummy = new Label("name");
            dummy.setVisible(false);
            add(dummy);
        }

        IPluginConfigService pluginConfigService = context.getService(IPluginConfigService.class.getName(),
                IPluginConfigService.class);
        IClusterConfig cluster = pluginConfigService.getCluster("cms-pickers/folders");
        control = context.newCluster(cluster, config.getPluginConfig("cluster.options"));
        IClusterConfig decorated = control.getClusterConfig();

        control.start();

        modelServiceId = decorated.getString("model.folder");
        tracker = new ServiceTracker<IModelReference>(IModelReference.class) {

            IModelReference modelRef;
            IObserver modelObserver;

            @Override
            protected void onServiceAdded(IModelReference service, String name) {
                super.onServiceAdded(service, name);
                if (modelRef == null) {
                    modelRef = service;
                    modelRef.setModel(destination.getChainedModel());
                    context.registerService(modelObserver = new IObserver<IModelReference>() {
                        private static final long serialVersionUID = 1L;

                        public IModelReference getObservable() {
                            return modelRef;
                        }

                        public void onEvent(Iterator<? extends IEvent<IModelReference>> events) {
                            IModel model = modelRef.getModel();
                            if (model != null && model instanceof JcrNodeModel && ((JcrNodeModel) model).getNode() != null) {
                                destination.setChainedModel(model);
                            }
                            DestinationDialog.this.setOkEnabled(true);
                        }
                    }, IObserver.class.getName());
                }
            }

            @Override
            protected void onRemoveService(IModelReference service, String name) {
                if (service == modelRef) {
                    context.unregisterService(modelObserver, IObserver.class.getName());
                    modelObserver = null;
                    modelRef = null;
                }
                super.onRemoveService(service, name);
            }

        };
        context.registerTracker(tracker, modelServiceId);

        dialogRenderer = context.getService(decorated.getString("wicket.id"), IRenderService.class);
        dialogRenderer.bind(null, "picker");
        add(dialogRenderer.getComponent());
        setFocusOnCancel();
        setOkEnabled(false);
    }

    @Override
    public void render(PluginRequestTarget target) {
        if (dialogRenderer != null) {
            dialogRenderer.render(target);
        }
        super.render(target);
    }

    @Override
    public final void onClose() {
        super.onClose();
        dialogRenderer.unbind();
        dialogRenderer = null;
        control.stop();
        context.unregisterTracker(tracker, modelServiceId);
        tracker = null;
    }

    @Override
    public IModel getTitle() {
        return title;
    }

    @Override
    public IValueMap getProperties() {
        return DialogConstants.LARGE;
    }
}
