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
package org.onehippo.forge.externalresource.frontend.plugins.gallery.impl;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;

import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.WorkflowContext;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.ext.InternalWorkflow;
import org.hippoecm.repository.gallery.GalleryWorkflow;
import org.hippoecm.repository.standardworkflow.FolderWorkflowImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// FIXME: this implementation should be totally rewritten as it should not
// implement InternalWorkflow, but could and should be a plain POJO workflow.

public class VideoGalleryWorkflowImpl extends FolderWorkflowImpl implements InternalWorkflow, GalleryWorkflow {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final Logger log = LoggerFactory.getLogger(VideoGalleryWorkflowImpl.class);

    private Node subject;


    public VideoGalleryWorkflowImpl(WorkflowContext context, Session userSession, Session rootSession, Node subject) throws RemoteException, RepositoryException {
        super(context, userSession, rootSession, subject);
        this.subject = subject;
    }

    public Map<String, Serializable> hints() {
        return null;
    }

    public List<String> getGalleryTypes() throws RepositoryException {
        List<String> list = new LinkedList<>();
        Value[] values = subject.getProperty("hippostd:gallerytype").getValues();
        for (int i = 0; i < values.length; i++) {
            list.add(values[i].getString());
        }
        return list;
    }

    public Document createGalleryItem(String name, String type) throws RemoteException, RepositoryException {
        // FIXME: this implementation is totally hardcoded and unlike the workflow in the FolderWorkflowImpl cannot be
        // customized with auto created properties, like user, current time, and -most importantly- also not the
        // hippo:availability property.  This implementation should be revoked entirely.
        try {
            String path = add("new-video", type, name);
            return new Document(path);
        } catch (WorkflowException e) {
            log.error("", e);
        }
        return null;
    }

    @Override
    public String add(String category, String template, String name) throws WorkflowException, RepositoryException, RemoteException {
        Map<String, String> arguments = new TreeMap<String, String>();
        arguments.put("name", name);
        return add(category, template, arguments);
    }


}
