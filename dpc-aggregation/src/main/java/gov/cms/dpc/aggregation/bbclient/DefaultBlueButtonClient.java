package gov.cms.dpc.aggregation.bbclient;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import gov.cms.dpc.aggregation.AggregationEngine;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContexts;
import org.hl7.fhir.dstu3.model.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.*;
import java.security.cert.CertificateException;

public class DefaultBlueButtonClient implements BlueButtonCliet {

    private static final Logger logger = LoggerFactory.getLogger(AggregationEngine.class);
    private static final String KEY_STORE_TYPE = "JKS";
    private static final String DEFAULT_KEY_STORE_PASSWORD = "changeit";
    private static final String SEARCH_URL = "ExplanationOfBenefit";

    private String serverBaseUrl;

    public DefaultBlueButtonClient(String blueButtonServerBaseUrl){
        // Add slash to end of URL if it doesn't already exist
        if (blueButtonServerBaseUrl.endsWith("/")){
            this.serverBaseUrl = blueButtonServerBaseUrl;
        } else {
            this.serverBaseUrl = blueButtonServerBaseUrl + "/";
        }
    }

    public Bundle requestFhirBundle(String beneficiaryID) throws BlueButtonClientException {
        // From http://hapifhir.io/doc_rest_client.html
        Bundle results;
        String keyStorePath = System.getProperty("javax.net.ssl.keyStore");

        try {

            InputStream keyStoreStream = new FileInputStream(keyStorePath);
            KeyStore keyStore = KeyStore.getInstance(KEY_STORE_TYPE);
            keyStore.load(keyStoreStream, DEFAULT_KEY_STORE_PASSWORD.toCharArray());

            SSLContext sslContext = SSLContexts.custom()
                    .loadKeyMaterial(keyStore, DEFAULT_KEY_STORE_PASSWORD.toCharArray())
                    .build();

            HttpClient mutualTlsHttpClient = HttpClients.custom().setSSLContext(sslContext).build();
            FhirContext ctx = FhirContext.forDstu3();

            ctx.getRestfulClientFactory().setHttpClient(mutualTlsHttpClient);
            IGenericClient client = ctx.newRestfulGenericClient(this.serverBaseUrl);
            results = client.search()
                    .byUrl(buildSearchUrl(beneficiaryID))
                    .returnBundle(Bundle.class)
                    .execute();

        } catch (FileNotFoundException ex){
            throw new BlueButtonClientException("Could not find keystore at location: " + keyStorePath, ex);
        } catch (KeyStoreException ex){
            throw new BlueButtonClientException("Wrong keystore type: " + KEY_STORE_TYPE, ex);
        } catch (IOException | NoSuchAlgorithmException | UnrecoverableKeyException | CertificateException ex) {
            throw new BlueButtonClientException(
                    "Error reading the keystore: either the default password is wrong or the keystore has been corrupted",
                    ex
            );
        } catch (KeyManagementException ex){
            throw new BlueButtonClientException("Error loading the keystore", ex);
        }

        return results;
    }

    private String buildSearchUrl(String patientId){
        return String.format("%s?patient=%s", SEARCH_URL, patientId);
    }

}
