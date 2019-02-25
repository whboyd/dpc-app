package gov.cms.dpc.aggregation.bbclient;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import com.typesafe.config.Config;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContexts;
import org.hl7.fhir.dstu3.model.Patient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.net.ssl.SSLContext;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Paths;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.MissingResourceException;

public class DefaultBlueButtonClient implements BlueButtonClient {

    private static final Logger logger = LoggerFactory.getLogger(DefaultBlueButtonClient.class);
    // Used to retrieve the keystore from the JAR resources. This path is relative to the Resources root.
    private static final String KEYSTORE_RESOURCE_KEY = "/bb.keystore";
    private static final String MALFORED_URL = "Malformed base URL for bluebutton server";

    private URL serverBaseUrl;
    private IGenericClient client;

    @Inject
    public DefaultBlueButtonClient(Config conf) {
        String keyStoreType = conf.getString("aggregation.bbclient.keyStore.type");
        String defaultKeyStorePassword = conf.getString("aggregation.bbclient.keyStore.defaultPassword");

        try {
            serverBaseUrl = new URL(conf.getString("aggregation.bbclient.serverBaseUrl"));

        } catch (MalformedURLException ex) {
            logger.error(MALFORED_URL, ex);
            throw new BlueButtonClientException(MALFORED_URL, ex);
        }

        try (final InputStream keyStoreStream = getKeyStoreStream(conf)) {
            // Need to build a custom HttpClient to handle mutual TLS authentication
            KeyStore keyStore = KeyStore.getInstance(keyStoreType);
            keyStore.load(keyStoreStream, defaultKeyStorePassword.toCharArray());

            SSLContext sslContext = SSLContexts.custom()
                    .loadKeyMaterial(keyStore, defaultKeyStorePassword.toCharArray())
                    .loadTrustMaterial(keyStore, null)
                    .build();

            HttpClient mutualTlsHttpClient = HttpClients.custom().setSSLContext(sslContext).build();
            FhirContext ctx = FhirContext.forDstu3();

            ctx.getRestfulClientFactory().setHttpClient(mutualTlsHttpClient);
            client = ctx.newRestfulGenericClient(serverBaseUrl.toString());

        } catch (KeyStoreException ex) {
            logger.error("Cannot open keystore of type {}", ex);
            throw new BlueButtonClientException("Wrong keystore type: " + keyStoreType, ex);
        } catch (IOException | NoSuchAlgorithmException | UnrecoverableKeyException | CertificateException ex) {
            throw new BlueButtonClientException(
                    "Error reading the keystore: either the default password is wrong or the keystore has been corrupted",
                    ex
            );
        } catch (KeyManagementException ex) {
            logger.error("Cannot load keystore keys", ex);
            throw new BlueButtonClientException("Error loading the keystore", ex);
        }
    }

    public Patient requestFHIRFromServer(String beneficiaryID) {
        Patient patient;

        try {
            patient = client.read().resource(Patient.class).withUrl(buildSearchUrl(beneficiaryID)).execute();

        } catch (MalformedURLException ex) {
            throw new BlueButtonClientException(
                    "There was an error building the URL from the patientID: " + beneficiaryID,
                    ex
            );

        } catch (ResourceNotFoundException ex) {
            throw new BlueButtonClientException("Could not find beneficiary with ID: " + beneficiaryID, ex);
        }

        return patient;
    }

    private String buildSearchUrl(String beneficiaryID) throws MalformedURLException {
        return new URL(serverBaseUrl, "Patient/" + beneficiaryID).toString();
    }

    /**
     * Helper function to get the keystore from either the location specified in the Configuration file, or from the JAR resources.
     * If the Config path is set, the helper will try to pull from the absolute file path.
     * Otherwise it looks for the {@link DefaultBlueButtonClient#KEYSTORE_RESOURCE_KEY} in the resources path.
     *
     * @param config - {@link Config} Configuration settings for bbclient
     * @return - {@link InputStream} to keystore
     */
    // TODO(isears-cms): This should be injected by Guice
    private static InputStream getKeyStoreStream(Config config) {
        final InputStream keyStoreStream;

        if (!config.hasPath("aggregation.bbclient.keyStore.location")) {
            keyStoreStream = DefaultBlueButtonClient.class.getResourceAsStream(KEYSTORE_RESOURCE_KEY);
            if (keyStoreStream == null) {
                logger.error("KeyStore location is empty, cannot find keyStore {} in resources", KEYSTORE_RESOURCE_KEY);
                throw new BlueButtonClientException("Unable to get keystore from resources",
                        new MissingResourceException("", DefaultBlueButtonClient.class.getName(), KEYSTORE_RESOURCE_KEY));
            }
        } else {
            final String keyStorePath = config.getString("aggregation.bbclient.keyStore.location");
            logger.debug("Opening keystream from location: {}", keyStorePath);
            try {
                keyStoreStream = new FileInputStream(keyStorePath);
            } catch (FileNotFoundException e) {
                logger.error("Could not find keystore at location: {}" + Paths.get(keyStorePath).toAbsolutePath().toString());
                throw new BlueButtonClientException("Unable to find keystore", e);
            }
        }
        return keyStoreStream;
    }
}
