package org.onehippo.forge.externalresource.api.scheduler.synchronization;

import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.repository.scheduling.RepositoryJob;
import org.onehippo.repository.scheduling.RepositoryJobExecutionContext;
import org.onehippo.repository.scheduling.RepositoryJobInfo;
import org.onehippo.repository.scheduling.RepositoryJobSimpleTrigger;
import org.onehippo.repository.scheduling.RepositoryScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import java.util.Date;

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
                RepositoryJobInfo jobInfo = new RepositoryJobInfo(node.getIdentifier(), jobGroup, SynchronizationJob.class);
                jobInfo.setAttribute(SynchronizationJob.IDENTIFIER, node.getIdentifier());
                RepositoryJobSimpleTrigger trigger = new RepositoryJobSimpleTrigger("now", new Date());
                repositoryScheduler.scheduleJob(jobInfo, trigger);
            }
            //TODO call listener for SynchronizationListPanel
//            if(jobDataMap.containsKey("listener")){
//                SynchronizationListener listener = (SynchronizationListener) jobDataMap.get("listener");
//                listener.onFinished();
//            }
        } catch (RepositoryException e) {
            if (LOG.isDebugEnabled()) {
                LOG.error("External resources cannot find nodes hippoexternal:synchronizable" , e);
            } else {
                LOG.error("External resources cannot find nodes hippoexternal:synchronizable");
            }
        } finally {
            if (session != null && session.isLive()) {
                session.logout();
            }
        }
    }
}
