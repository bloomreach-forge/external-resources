package org.onehippo.forge.externalresource.api;

import org.onehippo.cms7.services.SingletonService;

import nl.uva.mediamosa.MediaMosaService;

@SingletonService
public interface MediamosaRemoteService extends ExternalService {

    MediaMosaService service();

    String getUsername();

    String getPassword();
}
