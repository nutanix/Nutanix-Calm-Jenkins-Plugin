package com.calm.GlobalConfiguration;


import hudson.Extension;
import jenkins.model.GlobalConfiguration;
import org.kohsuke.stapler.DataBoundSetter;



@Extension
public class CalmGlobalConfiguration extends GlobalConfiguration {

    private String prismCentralIp;
    private String userName;
    private String password;

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


}
