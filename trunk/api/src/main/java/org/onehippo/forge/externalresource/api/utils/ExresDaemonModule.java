package org.onehippo.forge.externalresource.api.utils;

import org.onehippo.repository.modules.DaemonModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 * @version $Id$
 */
@Deprecated //FIXME
public class ExresDaemonModule implements DaemonModule {

    @SuppressWarnings({"UnusedDeclaration"})
    private static Logger log = LoggerFactory.getLogger(ExresDaemonModule.class);

    @Override
    public void initialize(final Session s) throws RepositoryException {
    }

    @Override
    public void shutdown() {
    }

}
