package org.onehippo.forge.externalresource.api.scheduler.synchronization;

import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.api.WorkflowException;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.forge.externalresource.api.Synchronizable;
import org.onehippo.forge.externalresource.api.utils.SynchronizationState;
import org.onehippo.forge.externalresource.api.workflow.SynchronizedActionsWorkflow;
import org.onehippo.repository.scheduling.RepositoryJob;
import org.onehippo.repository.scheduling.RepositoryJobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.rmi.RemoteException;

/**
 * @version $Id$
 */
public class SynchronizationJob implements RepositoryJob {
    private static final Logger LOG = LoggerFactory.getLogger(SynchronizationJob.class);

    @Override
    public void execute(RepositoryJobExecutionContext context) throws RepositoryException {

        try {
            String uuid = context.getAttribute("identifier");
            Session session = context.createSystemSession();
            Node node = ((HippoNode) session.getNodeByIdentifier(uuid)).getCanonicalNode();
            Synchronizable synchronizable = HippoServiceRegistry.getService(Synchronizable.class, context.getAttribute("synchronizable"));
            LOG.debug(uuid);
            SynchronizedActionsWorkflow workflow = (SynchronizedActionsWorkflow) ((HippoWorkspace) session.getWorkspace()).getWorkflowManager().getWorkflow("synchronization", node);
            SynchronizationState state = workflow.check(synchronizable);

            switch (state) {
                case UNSYNCHRONIZED:
                    workflow.update(synchronizable);
                    break;
                case BROKEN:
                    //workflow.delete(resourceManager);
                    break;
                default:
                    break;
            }
        } catch (RepositoryException e) {
        } catch (RemoteException e) {
        } catch (WorkflowException e) {
        }
    }
}
