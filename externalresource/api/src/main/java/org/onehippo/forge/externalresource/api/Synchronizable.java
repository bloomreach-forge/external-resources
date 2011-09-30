package org.onehippo.forge.externalresource.api;

import org.onehippo.forge.externalresource.api.utils.SynchronizationState;

import javax.jcr.Node;

/**
 * @version $Id$
 */
public interface Synchronizable {

   // public String Synchronize(Node node);

    public boolean update(Node node);

    public boolean commit(Node node);

    public SynchronizationState check(Node node);
}
