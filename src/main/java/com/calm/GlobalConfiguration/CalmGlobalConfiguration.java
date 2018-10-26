package com.calm.GlobalConfiguration;

import com.calm.Interface.Rest;
import com.calm.Logger.NutanixCalmLogger;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import hudson.Extension;
import hudson.security.ACL;
import hudson.util.ListBoxModel;
import jenkins.model.GlobalConfiguration;
import jenkins.model.Jenkins;
import org.json.JSONObject;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.bind.JavaScriptMethod;

import java.util.Collections;
import java.util.List;


@Extension
public class CalmGlobalConfiguration extends GlobalConfiguration {

    private String prismCentralIp;
    private String credentials;
    private String status;
    private boolean validateCertificates;
    private final static NutanixCalmLogger LOGGER = new NutanixCalmLogger(CalmGlobalConfiguration.class);

    public String getCredentials() {
        return credentials;
    }

    @DataBoundSetter
    public void setCredentials(String credentials) {
        this.credentials = credentials;
    }

    public String getPrismCentralIp() {
        return prismCentralIp;
    }

    @DataBoundSetter
    public void setPrismCentralIp(String prismCentralIp) {
        this.prismCentralIp = prismCentralIp;
        save();
    }

    public boolean isValidateCertificates() {
        return validateCertificates;
    }

    @DataBoundSetter
    public void setValidateCertificates(boolean validateCertificates) {
        this.validateCertificates = validateCertificates;
        save();
    }

    public String getStatus() {
        return status;
    }

    @DataBoundSetter
    public void setStatus(String status) {
        this.status = status;
        save();
    }

    @JavaScriptMethod
    public String isCalmEnabled(String prismCentralIp, String credentialId, boolean verifyCertificates){
        String userName = null, password = null;
        List<StandardUsernamePasswordCredentials> standardCredentials = CredentialsProvider.lookupCredentials
                (StandardUsernamePasswordCredentials.class, Jenkins.getInstance(), ACL.SYSTEM, Collections.<DomainRequirement>emptyList());
        for(StandardUsernamePasswordCredentials credential : standardCredentials){
            if(credential.getId().equals(credentialId)){
                userName = credential.getUsername();
                password = credential.getPassword().getPlainText();
                break;
            }
        }
        Rest rest = new Rest(prismCentralIp, userName, password, verifyCertificates);
        String message = "";
        try {
            JSONObject response = rest.get("services/nucalm/status");
            String calmEnablement = response.getString("service_enablement_status");
            if (!calmEnablement.equalsIgnoreCase("ENABLED"))
                message = "CALM is not enabled";
        }
        catch (Exception e){
            if(e.getMessage().contains("UNAUTHORIZED"))
                message = "Invalid credentials";
            else if(e.getMessage().contains("CertificateException"))
                message = "Certificate validation failed";
            else if (e.getMessage().contains("Internal Server Error"))
                message = "PC is not accessible";
            setStatus("Verification Failed\nCause: "+ message);
            return "Verification Failed\nCause: "+ message;
        }
        setStatus("Verification Successful\n" + message);
        return "Verification Successful\n" + message;
    }

    public static CalmGlobalConfiguration get() {
        return GlobalConfiguration.all().get(CalmGlobalConfiguration.class);
    }

    public CalmGlobalConfiguration() {
        // When Jenkins is restarted, load any saved configuration from disk.
        load();
    }

    public ListBoxModel doFillCredentialsItems(@QueryParameter String credentials) {
        return CredentialsProvider.listCredentials(StandardUsernamePasswordCredentials.class,
                Jenkins.getInstance(), ACL.SYSTEM,null,null);
    }
}
