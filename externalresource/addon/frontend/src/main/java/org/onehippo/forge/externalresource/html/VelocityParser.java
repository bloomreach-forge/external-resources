package org.onehippo.forge.externalresource.html;

import org.apache.velocity.app.Velocity;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.onehippo.forge.externalresource.api.ResourceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import java.io.StringWriter;
import java.util.Properties;

/**
 * @version $Id$
 */
public class VelocityParser {

    @SuppressWarnings({"UnusedDeclaration"})
    private static Logger log = LoggerFactory.getLogger(VelocityParser.class);

    private final static VelocityParser instance = new VelocityParser();
    private final VelocityEngine engine;

    public static VelocityParser getInstance() {
        return instance;
    }

    /**
     * Configures the engine to use classpath to find templates
     *
     * @param engine velocity engine to configure
     * @throws Exception init exception
     */
    private void configure(final VelocityEngine engine) throws Exception {
        Properties props = new Properties();
        props.setProperty(VelocityEngine.RESOURCE_LOADER, "classpath");
        props.setProperty("classpath." + VelocityEngine.RESOURCE_LOADER + ".class", ClasspathResourceLoader.class.getName());
        engine.init(props);
    }

    /**
     * private constructor
     */
    private VelocityParser() {
        try {
            Velocity.init();
            engine = new VelocityEngine();
            configure(engine);
        } catch (Exception e) {
            log.error("Error initializing Velocity", e);
            throw new RuntimeException("Couldn't initialize velocity engine. Please check if all project dependencies are satisfied");
        }
    }

    public String populateFromProcessor(ResourceManager processor) {
        try {
            //final Template template = processor.getVelocityTemplate(engine);

            //final VelocityContext context = processor.getVelocityContext();

            final StringWriter writer = new StringWriter();

            //template.merge(context, writer);

            return writer.toString();
        } catch (Exception e) {
            log.error("Error parsing template", e);
        }
        return "";
    }

     public String populateFromProcessor(ResourceManager processor, Node node) {
        try {
            //processor.setNode(node);
            populateFromProcessor(processor);
        } catch (Exception e) {
            log.error("Error parsing template", e);
        }
        return "";
    }

}
