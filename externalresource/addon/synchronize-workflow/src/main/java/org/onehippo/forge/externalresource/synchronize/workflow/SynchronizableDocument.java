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
package org.onehippo.forge.externalresource.synchronize.workflow;

import org.hippoecm.repository.api.Document;

import javax.jdo.annotations.*;
import java.util.Date;

@PersistenceCapable(identityType=IdentityType.DATASTORE,cacheable="false",detachable="false")
@DatastoreIdentity(strategy=IdGeneratorStrategy.NATIVE)
@Inheritance(strategy=InheritanceStrategy.SUBCLASS_TABLE)
@Discriminator(strategy=DiscriminatorStrategy.CLASS_NAME)
public class SynchronizableDocument extends Document {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    @Persistent(column="hippoexternal:state")
    private String state;

    @Persistent(column="hippoexternal:lastModifiedSyncDate")
    protected Date lastModifiedSyncDate;



    public SynchronizableDocument() {
        /*this.state = UNPUBLISHED;
        this.availability = null;*/
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        /*PublishableDocument clonedDocument = (PublishableDocument) super.clone();
        clonedDocument.creationDate = creationDate;
        clonedDocument.createdBy = createdBy;
        clonedDocument.lastModificationDate = lastModificationDate;
        clonedDocument.lastModifiedBy = lastModifiedBy;
        clonedDocument.publicationDate = null;
        clonedDocument.availability = new String[0];*/
        return this;
    }

   public void setState(String state) {
        if (!this.state.equals(state)) {
           this.state = state;
        }
    }

    public String getState() {
        return state;
    }

    public void setLastSyncDate(Date date) {
        this.lastModifiedSyncDate = date;
    }

    public Date getLastModifiedSyncDate() {
        return lastModifiedSyncDate;
    }



}
