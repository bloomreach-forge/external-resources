package org.onehippo.forge.externalresource.frontend.plugins.type.mediamosa.dialog.still;

import nl.uva.mediamosa.MediaMosaService;
import nl.uva.mediamosa.model.JobDetailsType;
import nl.uva.mediamosa.model.JobType;
import nl.uva.mediamosa.model.Response;
import nl.uva.mediamosa.model.StillDetailType;
import nl.uva.mediamosa.model.StillType;
import nl.uva.mediamosa.model.UploadTicketType;
import nl.uva.mediamosa.util.ServiceException;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AbstractAjaxTimerBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.IHeaderContributor;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.RefreshingView;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.resource.CssResourceReference;
import org.apache.wicket.request.resource.ResourceReference;
import org.apache.wicket.util.time.Duration;
import org.apache.wicket.util.value.IValueMap;
import org.apache.wicket.util.value.ValueMap;
import org.hippoecm.frontend.behaviors.EventStoppingBehavior;
import org.hippoecm.frontend.editor.plugins.resource.ResourceHelper;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.yui.upload.FileUploadWidget;
import org.hippoecm.frontend.plugins.yui.upload.FileUploadWidgetSettings;
import org.onehippo.forge.externalresource.api.MediamosaRemoteService;
import org.onehippo.forge.externalresource.api.scheduler.mediamosa.MediaMosaJobState;
import org.onehippo.forge.externalresource.api.utils.MediaMosaServices;
import org.onehippo.forge.externalresource.frontend.plugins.type.dialog.AbstractExternalResourceDialog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @version $Id$
 */
public class StillManagerDialog extends AbstractExternalResourceDialog implements IHeaderContributor {
    @SuppressWarnings({"UnusedDeclaration"})
    private static Logger log = LoggerFactory.getLogger(StillManagerDialog.class);

    private final StillUploadPanel uploadPanel;
    private final StillCreationPanel creationPanel;
    private StillProgressPanel progressPanel;
    private StillViewPanel viewPanel;

    private MediaMosaService service;
    private String assetId;
    private String mediaId;

    private StillTimerManager timer;

    private List<IModel<JobType>> jobList = Collections.synchronizedList(new ArrayList<IModel<JobType>>());

    private StillDetailType selected;
    private StillDetailType intitial;
    private AjaxLink stillLink;

    private boolean created;
    private MediamosaRemoteService resourceManager;


    public StillManagerDialog(IModel iModel, IPluginContext context, IPluginConfig config) {
        super(iModel, context, config);

        Node node = (Node) getModelObject();
        try {
            node = node.getParent();
            if (node.hasProperty("hippomediamosa:assetid")) {
                this.assetId = node.getProperty("hippomediamosa:assetid").getString();
            }
            if (node.hasProperty("hippomediamosa:mediaid")) {
                this.mediaId = node.getProperty("hippomediamosa:mediaid").getString();
            }
            this.resourceManager = MediaMosaServices.forNode(node).getMediamosaRemoteService();
            this.service = resourceManager.service();
        } catch (RepositoryException e) {
            log.error("Error fetching asset id", e);
        }

        this.uploadPanel = new StillUploadPanel("still-upload");
        this.creationPanel = new StillCreationPanel("still-creation");
        this.progressPanel = new StillProgressPanel("still-progress");
        this.viewPanel = new StillViewPanel("still-view");

        add(uploadPanel);
        add(creationPanel);
        add(progressPanel);
        add(viewPanel);

        this.timer = new StillTimerManager(Duration.seconds(5)) {
            @Override
            protected void onTimer(AjaxRequestTarget target) {
                if (shouldUpdate(viewPanel)) {
                    target.add(viewPanel);
                    put(viewPanel, false);
                }
                target.add(progressPanel);
            }
        };
        add(timer);
    }

    @Override
    protected void onCancel() {
        if (created && intitial != null) {
            Map<String, String> map = new HashMap<>();
            map.put("mediafile_id", mediaId);
            map.put("still_id", intitial.getStillId());
            try {
                log.info(service.setDefaultStill(assetId, resourceManager.getUsername(), map).getHeader().getRequestResult());

            } catch (ServiceException | IOException e) {
                log.error("", e);
            }
        }
    }

    @Override
    protected void onOk() {
        InputStream is = null;
        try {
            Node node = (Node) getModelObject();
            if (selected != null) {
                String id = selected.getStillId();
                StillType type = service.getStills(assetId, resourceManager.getUsername(), null);
                boolean found = false;
                for (StillDetailType detail : type.getStills()) {
                    if (detail.getStillId().equals(id)) {
                        String url = detail.getStillTicket();

                        HttpClient client = new HttpClient();
                        HttpMethod getMethod = new GetMethod(url);
                        client.executeMethod(getMethod);
                        String mimeType = getMethod.getResponseHeader("content-type").getValue();
                        if (!mimeType.startsWith("image")) {
                            log.error("Illegal mimetype used: {}", mimeType);
                            throw new IllegalArgumentException();
                        }
                        is = getMethod.getResponseBodyAsStream();
                        node.setProperty("jcr:data", ResourceHelper.getValueFactory(node).createBinary(is));
                        node.setProperty("jcr:mimeType", mimeType);
                        node.setProperty("jcr:lastModified", Calendar.getInstance());
                        Map<String,String> map = new HashMap<>();
                        map.put("mediafile_id", mediaId);
                        map.put("still_id", id);
                        log.info(service.setDefaultStill(assetId, resourceManager.getUsername(), map).getHeader().getRequestResult());
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    //todo error()
                }
            }
        } catch (ServiceException | RepositoryException | IOException e) {
            log.error("", e);
        } finally {
            org.apache.commons.io.IOUtils.closeQuietly(is);
        }
    }

    public MediaMosaService getService() {
        return service;
    }


    private class StillUploadPanel extends Panel {

        private FileUploadForm form;

        public StillUploadPanel(String id) {
            super(id);

            add(form = new FileUploadForm("form"));
            String mode = config.getString("mode", "edit");
            form.setVisible("edit".equals(mode));
            add(new EventStoppingBehavior("onclick"));
        }

        private class FileUploadForm extends Form {
            private static final long serialVersionUID = 1L;

            private FileUploadWidget widget;

            public FileUploadForm(String name) {
                super(name);


                FileUploadWidgetSettings settings = new FileUploadWidgetSettings();
                settings.setAutoUpload(true);
                settings.setClearAfterUpload(true);
                settings.setClearTimeout(1000);
                settings.setHideBrowseDuringUpload(true);
                settings.setButtonWidth("154px");
                if (config.containsKey("file.extensions")) {
                    settings.setFileExtensions(config.getStringArray("file.extensions"));
                }


                add(widget = new FileUploadWidget("multifile", settings) {
                    @Override
                    protected void onFileUpload(FileUpload fileUpload) {
                        handleUpload(fileUpload);
                    }

                });

            }

            private void handleUpload(FileUpload upload) {
                InputStream stream = null;
                try {
                    stream = upload.getInputStream();
                    String filename = upload.getClientFileName();
                    String mimeType = upload.getContentType();

                    UploadTicketType ticket = service.createUploadTicket(mediaId, resourceManager.getUsername(), true);
                    String action = ticket.getAction();
                    String uploadTicket = action.substring(action.indexOf('=') + 1);

                    URL actionUrl = new URL(action);
                    String uploadHost = actionUrl.getProtocol() + "://" + actionUrl.getAuthority();

                    Map<String, String> map = new HashMap<String, String>();
                    map.put("upload_ticket", uploadTicket);
                    map.put("mediafile_id", mediaId);

                    Response response = service.uploadStill(uploadHost, assetId, map, stream, mimeType, filename);
                    //timer.put(viewPanel, response.getHeader().getRequestResult().equals("success"));
                    timer.put(viewPanel, true);
                } catch (IOException | ServiceException e) {
                    log.error("", e);
                    error(e.getLocalizedMessage());
                } finally {
                    org.apache.commons.io.IOUtils.closeQuietly(stream);
                }
            }
        }
    }


    private class StillCreationPanel extends Panel {

        private String creationString;

        private StillCreationPanel(String id) {
            super(id);

            final TextField<String> creationInput = new TextField<String>("creationTextfield", new PropertyModel<String>(this, "creationString"));
            creationInput.setOutputMarkupId(true);
            add(creationInput);

            AjaxButton creationButton = new AjaxButton("creationButton", new StringResourceModel("creation-button", this, null)) {
                @Override
                protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                    try {
                        Map<String, Object> map = new HashMap<>();
                        int seconds = Integer.valueOf(creationString);
                        map.put("still_type", "SECOND");
                        map.put("still_every_second", 1);
                        map.put("start_time", seconds);
                        map.put("end_time", seconds + 1);
                        jobList.add(new Model(getService().createStill(assetId, mediaId, resourceManager.getUsername(), map)));
                        created = true;
                    } catch (IOException | ServiceException e) {
                        log.error("", e);
                    } catch (NumberFormatException e) {
                        log.error("", e);
                        error(e.getMessage());
                    }
                }
            };
            add(creationButton);
        }
    }

    private class StillProgressPanel extends Panel {

        private StillProgressPanel(String id) {
            super(id);
            setOutputMarkupId(true);

            final RefreshingView<JobType> jobView = new RefreshingView<JobType>("job-item") {
                @Override
                protected Iterator<IModel<JobType>> getItemModels() {
                    List<IModel<JobType>> copyList = new ArrayList<>();
                    copyList.addAll(jobList);
                    return copyList.iterator();
                }

                @Override
                protected void populateItem(Item<JobType> jobTypeItem) {
                    JobType job = jobTypeItem.getModelObject();
                    try {
                        JobDetailsType jobDetail = getService().getJobStatus(String.valueOf(job.getJobId()), resourceManager.getUsername());
                        Label jobLabel = new Label("job-item-label", new StringResourceModel("job-item-label", this, null, new Object[]{jobDetail.getId(), jobDetail.getStatus()}));
                        jobTypeItem.add(jobLabel);
                        switch (MediaMosaJobState.getType(jobDetail.getStatus())) {
                            case FINISHED:
                                timer.put(viewPanel, jobList.remove(jobTypeItem.getModel()));
                                break;
                            case CANCELLED:
                            case FAILED:
                                jobList.remove(jobTypeItem.getModel());
                                error(jobDetail.getErrorDescription());
                                break;
                        }
                    } catch (IOException | ServiceException e) {
                        log.error("", e);
                    }
                }
            };
            jobView.setOutputMarkupId(true);
            add(jobView);
        }
    }

    private class StillViewPanel extends Panel {

        public StillViewPanel(String id) {
            super(id);
            setOutputMarkupId(true);

            RefreshingView<StillDetailType> stillView = new RefreshingView<StillDetailType>("list-item") {
                @Override
                protected Iterator<IModel<StillDetailType>> getItemModels() {
                    List<IModel<StillDetailType>> list = new ArrayList<>();
                    try {
                        StillType stillType = getService().getStills(assetId, resourceManager.getUsername(), null);
                        if (stillType != null) {
                            for (StillDetailType detail : stillType.getStills()) {
                                list.add(new Model<>(detail));
                            }
                        } else {
                            log.warn("No stills");
                            info(new StringResourceModel("no-stills-available", StillManagerDialog.this, null).getString());
                        }
                    } catch (ServiceException | IOException e) {
                        log.error("Exception occurred while getting stills", e);
                        error("Exception occurred while getting stills");
                    }
                    return list.iterator();
                }

                @Override
                protected void populateItem(Item<StillDetailType> stillDetailTypeItem) {
                    final StillDetailType stillDetailType = stillDetailTypeItem.getModelObject();

                    if (stillDetailType.isStillDefault() && created) {
                        selected = stillDetailType;
                    } else if (stillDetailType.isStillDefault()) {
                        intitial = stillDetailType;
                    }

                    final AjaxLink<StillDetailType> imageLink = new AjaxLink<StillDetailType>("list-item-link") {
                        @Override
                        public void onClick(AjaxRequestTarget target) {
                            created = false;
                            selected = stillDetailType;
                            if (stillLink != null) {
                                stillLink.add(new AttributeModifier("class", "still"));
                                target.add(stillLink);
                            }
                            this.add(new AttributeModifier("class", "new-still"));
                            stillLink = this;
                            target.add(this);
                        }
                    };
                    imageLink.setOutputMarkupId(true);

                    imageLink.add(new AttributeAppender("class", new AbstractReadOnlyModel() {
                        private static final long serialVersionUID = 1L;

                        public Object getObject() {
                            return (stillDetailType.isStillDefault()) ? "default-still" : "still";
                        }
                    }, " "));

                    ExternalImageFallback image = new ExternalImageFallback("list-item-image", stillDetailType.getStillTicket(), null);
                    //imageLink.add(image);

                    stillDetailTypeItem.add(image);

                    stillDetailTypeItem.add(imageLink);

                    Label timeCode = new Label("duration", new Model(stillDetailType.getStillTimeCode()));
                    stillDetailTypeItem.add(timeCode);

                    if (!stillDetailType.isStillDefault()) {

                        Fragment fragment = new Fragment("delete", "delete-fragment", this);
                        AjaxLink deleteLink = new AjaxLink("delete-link") {
                            @Override
                            public void onClick(AjaxRequestTarget target) {
                                Map<String, String> map = new HashMap<>();
                                map.put("still_id", stillDetailType.getStillId());
                                try {
                                    log.info(service.deleteStill(stillDetailType.getAssetId(), stillDetailType.getMediafileId(), resourceManager.getUsername(), map).getHeader().getRequestResultDescription());
                                    target.add(viewPanel);
                                } catch (IOException | ServiceException e) {
                                    log.error("", e);
                                }
                            }
                        };
                        fragment.add(deleteLink);
                        stillDetailTypeItem.add(fragment);
                    } else {
                        stillDetailTypeItem.add(new EmptyPanel("delete"));
                    }
                }
            };
            stillView.setOutputMarkupId(true);
            add(stillView);
        }
    }


    public IModel getTitle() {
        return new StringResourceModel("title", this, null);
    }

    protected final static IValueMap CUSTOM = new ValueMap("width=835,height=650").makeImmutable();
    private static final ResourceReference CSS = new CssResourceReference(StillManagerDialog.class, "StillManagerDialog.css");

    @Override
    public IValueMap getProperties() {
        return CUSTOM;
    }

    @Override
    public void renderHead(IHeaderResponse response) {
        super.renderHead(response);
        response.render(CssHeaderItem.forReference(CSS));
    }

    private abstract class StillTimerManager extends AbstractAjaxTimerBehavior {
        private Map<Component, Boolean> map = new HashMap<>();

        public StillTimerManager(Duration updateInterval) {
            super(updateInterval);
        }

        @Override
        abstract protected void onTimer(AjaxRequestTarget target);

        public synchronized void put(Component component, boolean bool) {
            map.put(component, bool);
        }

        public synchronized boolean shouldUpdate(Component component) {
            if (map.containsKey(component)) {
                return map.get(component);
            }
            return false;
        }
    }
}
