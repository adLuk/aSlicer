package cz.ad.print3d.aslicer.logic.net.ssl;

import io.netty.handler.ssl.util.SimpleTrustManagerFactory;
import javax.net.ssl.ManagerFactoryParameters;
import javax.net.ssl.TrustManager;
import java.security.KeyStore;

/**
 * TrustManagerFactory that provides {@link InteractiveTrustManager}.
 */
public class InteractiveTrustManagerFactory extends SimpleTrustManagerFactory {

    private final InteractiveTrustManager trustManager;

    /**
     * Constructs a new InteractiveTrustManagerFactory.
     *
     * @throws Exception if there is an error creating the InteractiveTrustManager.
     */
    public InteractiveTrustManagerFactory() throws Exception {
        this.trustManager = new InteractiveTrustManager();
    }

    @Override
    protected void engineInit(KeyStore keyStore) throws Exception {
        // Not needed for this implementation
    }

    @Override
    protected void engineInit(ManagerFactoryParameters managerFactoryParameters) throws Exception {
        // Not needed for this implementation
    }

    @Override
    protected TrustManager[] engineGetTrustManagers() {
        return new TrustManager[] { trustManager };
    }

    /**
     * Gets the InteractiveTrustManager instance used by this factory.
     *
     * @return the {@link InteractiveTrustManager}.
     */
    public InteractiveTrustManager getTrustManager() {
        return trustManager;
    }
}
