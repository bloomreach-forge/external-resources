package org.onehippo.forge.externalresource.api.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

/**
 * @version $Id$
 */
public class Utils {
    @SuppressWarnings({"UnusedDeclaration"})
    private static Logger log = LoggerFactory.getLogger(Utils.class);

    public static void addEmbeddedNode(Node node, String embedded){
        try {
            Node embeddedNode = null;
            //System.out.println("why shouldnt i work");
            if(node.hasNode("hippoexternal:embedded")){
                embeddedNode = node.getNode("hippoexternal:embedded");
            } else{
                embeddedNode = node.addNode("hippoexternal:embedded", "hippoexternal:embedded");
            }
            embeddedNode.setProperty("hippoexternal:embedded", embedded);
        } catch (RepositoryException e) {
            log.error("", e);
        }
    }


}
