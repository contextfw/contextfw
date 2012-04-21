package net.contextfw.web.commons.cloud.internal.mongo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * For internal use only
 */
public abstract class ExceptionSafeExecution implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(ExceptionSafeExecution.class);
    
    public final void run() {
        try {
            execute();
        } catch (Exception e) {
            LOG.info("Error while execution scheduled task", e);
        }
    }

    @edu.umd.cs.findbugs.annotations.SuppressWarnings(
            value="SignatureDeclareThrowsException", 
            justification="This is exception safe")
    public abstract void execute() throws Exception;
}
