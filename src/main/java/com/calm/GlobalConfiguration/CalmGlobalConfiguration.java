package com.calm.GlobalConfiguration;


import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import hudson.Extension;
import hudson.security.ACL;
import hudson.util.ListBoxModel;
import jenkins.model.GlobalConfiguration;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;



@Extension
public class CalmGlobalConfiguration extends GlobalConfiguration {

    private String prismCentralIp;
    private String userName;
    private String password;
    private String credentials;

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

    public String getUserName() {
        return userName;
    }

    @DataBoundSetter
    public void setUserName(String userName) {
        this.userName = userName;
        save();
    }

    public String getPassword() {
        return password;
    }

    @DataBoundSetter
    public void setPassword(String password) {
        this.password = password;
        save();
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
