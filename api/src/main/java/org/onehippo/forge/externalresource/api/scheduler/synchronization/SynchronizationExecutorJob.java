package org.onehippo.forge.externalresource.api.scheduler.synchronization;

import org.hippoecm.repository.api.HippoNodeType;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.repository.scheduling.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import java.util.Date;

import static org.hippoecm.repository.quartz.HippoSchedJcrConstants.HIPPOSCHED_SUBJECT_ID;
import static org.hippoecm.repository.quartz.HippoSchedJcrConstants.HIPPOSCHED_WORKFLOW_JOB;

/**
 * @version $Id$
 */
public class SynchronizationExecutorJob implements RepositoryJob {
    private static final Logger LOG = LoggerFactory.getLogger(SynchronizationExecutorJob.class);
    private static final String QUERY = "content/videos//element(*,hippoexternal:synchronizable)";

    /**
     * Name of attribute that must be set on RepositoryJobInfo when scheduling SynchronizationExecutorJob.
     */
    public static final String JOB_GROUP = "jobGroup";

    @Override
    public void execute(RepositoryJobExecutionContext context) {
        String jobGroup = context.getAttribute(JOB_GROUP);
        Session session = null;
        try {
            session = context.createSystemSession();
            RepositoryScheduler repositoryScheduler = HippoServiceRegistry.getService(RepositoryScheduler.class);

            NodeIterator it = session.getWorkspace().getQueryManager().createQuery(QUERY, Query.XPATH).execute().getNodes();
            while (it.hasNext()) {
                Node node = it.nextNode();
                Node handleNode = node.getParent();
                RepositoryJobInfo jobInfo = new SynchronizationJobInfo(handleNode.getIdentifier(), jobGroup);
                jobInfo.setAttribute(SynchronizationJob.IDENTIFIER, node.getIdentifier());
                RepositoryJobSimpleTrigger trigger = new RepositoryJobSimpleTrigger("now", new Date());
                repositoryScheduler.scheduleJob(jobInfo, trigger);
            }
        } catch (RepositoryException e) {
            if (LOG.isDebugEnabled()) {
                LOG.error("External resources cannot find nodes hippoexternal:synchronizable", e);
            } else {
                LOG.error("External resources cannot find nodes hippoexternal:synchronizable");
            }
        } finally {
            if (session != null && session.isLive()) {
                session.logout();
            }
        }
    }

    private static class SynchronizationJobInfo extends RepositoryJobInfo {

        private final String handleIdentifier;

        public SynchronizationJobInfo(final String handleIdentifier, final String jobGroup) {
            super(HippoNodeType.HIPPO_REQUEST, jobGroup, SynchronizationJob.class);
            this.handleIdentifier = handleIdentifier;
            setAttribute(HIPPOSCHED_SUBJECT_ID, handleIdentifier);
        }

        @Override
        public Node createNode(final Session session) throws RepositoryException {
            final Node handleNode = session.getNodeByIdentifier(handleIdentifier);
            return handleNode.addNode(HippoNodeType.HIPPO_REQUEST, HIPPOSCHED_WORKFLOW_JOB);
        }
    }


}
