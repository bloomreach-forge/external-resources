package org.onehippo.forge.externalresource.api.scheduler;

import org.hippoecm.repository.quartz.JCRSchedulingContext;
import org.quartz.Scheduler;
import org.quartz.core.QuartzScheduler;
import org.quartz.core.SchedulingContext;
import org.quartz.impl.StdScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Session;

/**
 * @version $Id$
 */
public class ExternalResourceScheduler extends StdScheduler implements Scheduler {
    @SuppressWarnings({"UnusedDeclaration"})
    private static Logger log = LoggerFactory.getLogger(ExternalResourceScheduler.class);

    private QuartzScheduler qs;
    private SchedulingContext ctx;


    /**
     * <p>
     * Construct a <code>StdScheduler</code> instance to proxy the given
     * <code>QuartzScheduler</code> instance, and with the given <code>SchedulingContext</code>.
     * </p>
     */
    public ExternalResourceScheduler(QuartzScheduler sched, SchedulingContext schedCtxt) {
        super(sched, schedCtxt);
        this.qs = sched;
        this.ctx = schedCtxt;

    }


    public ExternalResourceScheduler(ExternalResourceScheduler sched, Session session) {
        super(sched.qs, new JCRSchedulingContext(sched.ctx, session));
    }

    public SchedulingContext getCtx() {
        return ctx;
    }
}
