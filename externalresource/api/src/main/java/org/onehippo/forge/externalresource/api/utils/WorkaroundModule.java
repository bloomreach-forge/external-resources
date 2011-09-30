package org.onehippo.forge.externalresource.api.utils;

import org.hippoecm.repository.ext.DaemonModule;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 * @version $Id$
 */
public class WorkaroundModule implements DaemonModule {

    private static Session session;


    public void initialize(final Session s) throws RepositoryException {

        session = s;
    }


    public void shutdown() {
    }

    public static Session getSession() {
        return session;
    }
}
