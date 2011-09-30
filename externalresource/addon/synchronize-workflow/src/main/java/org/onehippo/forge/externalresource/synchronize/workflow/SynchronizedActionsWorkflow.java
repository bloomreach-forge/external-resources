package org.onehippo.forge.externalresource.synchronize.workflow;

import org.hippoecm.repository.api.MappingException;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.standardworkflow.DefaultWorkflow;
import org.hippoecm.repository.standardworkflow.EditableWorkflow;

import javax.jcr.RepositoryException;
import java.rmi.RemoteException;

/**
 * @version $Id$
 */
public interface SynchronizedActionsWorkflow extends Workflow, EditableWorkflow, DefaultWorkflow {

    /**
     * Update of document.
     */
    public void update()
        throws WorkflowException, MappingException, RepositoryException, RemoteException;
/**
     * Commit of document.
     */
    public void commit()
        throws WorkflowException, MappingException, RepositoryException, RemoteException;




}
