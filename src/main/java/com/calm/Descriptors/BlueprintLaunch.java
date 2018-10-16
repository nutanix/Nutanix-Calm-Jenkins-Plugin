package com.calm.Descriptors;


import com.calm.CalmHelpers.Blueprint;
import com.calm.CalmHelpers.Project;
import com.calm.Executor.CalmExecutor;
import com.calm.GlobalConfiguration.CalmGlobalConfiguration;
import com.calm.Interface.Rest;
import com.calm.Logger.NutanixCalmLogger;
import com.cloudbees.plugins.credentials.common.*;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import hudson.security.*;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import org.kohsuke.stapler.bind.JavaScriptMethod;
import java.io.*;
import java.util.*;
import net.sf.json.*;
import hudson.*;
import hudson.model.*;
import hudson.tasks.*;
import org.kohsuke.stapler.*;
import jenkins.tasks.SimpleBuildStep;


public class BlueprintLaunch extends Builder implements SimpleBuildStep {


    private final String projectName, blueprintName, applicationName, appProfileName, actionName, runtimeVariables;
    private String blueprintDescription;
    private final boolean waitForSuccessFulLaunch;


    // Fields in config.jelly must match the parameter names in the "DataBoundConstructor"
    @DataBoundConstructor
    public BlueprintLaunch(String projectName, String blueprintName, String applicationName, String appProfileName,
                           String actionName, String runtimeVariables, boolean waitForSuccessFulLaunch, String blueprintDescription) {
        this.projectName = projectName;
        this.blueprintName = blueprintName;
        this.applicationName = applicationName;
        this.appProfileName = appProfileName;
        this.actionName = actionName;
        this.runtimeVariables = runtimeVariables;
        this.waitForSuccessFulLaunch = waitForSuccessFulLaunch;
        this.blueprintDescription = blueprintDescription;
    }

    public String getProjectName() {
        return projectName;
    }

    public String getBlueprintName() {
        return blueprintName;
    }

    public String getApplicationName() {
        return applicationName;
    }

    public String getAppProfileName() {
        return appProfileName;
    }

    public String getActionName() {
        return actionName;
    }

    public String getRuntimeVariables() {
        return runtimeVariables;
    }

    public boolean getWaitForSuccessFulLaunch() {
        return waitForSuccessFulLaunch;
    }

    public String getBlueprintDescription(){
        return blueprintDescription;
    }

    @Override
    public void perform(Run build, FilePath workspace, Launcher launcher, TaskListener listener)
            throws IOException, InterruptedException {
        // This is where you 'build' the project.
        EnvVars envVars = new EnvVars();
        final EnvVars env = build.getEnvironment(listener);
        PrintStream log = listener.getLogger();
        //Expanding appname to include the env variables in it's name
        String expandedApplicationName = env.expand(applicationName);
        log.println("Executing Nutanix Calm Blueprint launch Build Step");
        CalmGlobalConfiguration calmGlobalConfiguration = CalmGlobalConfiguration.get();
        String prismCentralIp = calmGlobalConfiguration.getPrismCentralIp();
        String credId = calmGlobalConfiguration.getCredentials();
        String userName = null, password = null;
        List<StandardUsernamePasswordCredentials> standardCredentials = CredentialsProvider.lookupCredentials
                (StandardUsernamePasswordCredentials.class, Jenkins.getInstance(), ACL.SYSTEM, Collections.<DomainRequirement>emptyList());
        for(StandardUsernamePasswordCredentials credential : standardCredentials){
            if(credential.getId().equals(credId)){
                userName = credential.getUsername();
                password = credential.getPassword().getPlainText();
                break;
            }
        }
        CalmExecutor calmExecutor = new CalmExecutor(prismCentralIp, userName, password, projectName, blueprintName,
                appProfileName, actionName, runtimeVariables, expandedApplicationName, waitForSuccessFulLaunch, log);
        log.println("##Connecting to calm instance##");
        List<String> globalError = new ArrayList<String>();
        try{
            if (prismCentralIp == null || prismCentralIp.length() == 0) {
                globalError.add("IP Address is mandatory parameter");
            }

            if (userName == null || userName.length() == 0) {
                globalError.add("Username is mandatory parameter");
            }

            if (password == null || password.length() == 0) {
                globalError.add("Password is mandatory parameter");
            }

            if (applicationName == null || applicationName.length() == 0) {
                globalError.add("Application name is mandatory");
            }

            if (globalError.size() > 0){
                listener.error("Nutanix Calm Prism Central Details Required" + globalError);
                build.setResult(Result.FAILURE);
            }
            calmExecutor.launchBlueprint();
        }
        catch (Exception e){
            log.println(e.getMessage());
            build.setResult(Result.FAILURE);

        }
    }

    // Overridden for better type safety.
    // If your plugin doesn't really define any property on Descriptor,
    // you don't have to do this.
    @Override
    public BlueprintLaunchDescriptorImpl getDescriptor() {
        return (BlueprintLaunchDescriptorImpl) super.getDescriptor();
    }


    @Extension // This indicates to Jenkins that this is an implementation of an extension point.
    public static final class BlueprintLaunchDescriptorImpl extends BuildStepDescriptor<Builder> {

        private String prismCentralIp;
        private String userName;
        private String password;
        private Project projectHelper;
        private Blueprint blueprintHelper;
        private int lastEditorId = 0;
        private Rest rest;
        private final static NutanixCalmLogger LOGGER = new NutanixCalmLogger(BlueprintLaunchDescriptorImpl.class);
        private String getPrismCentralIp() {
            return prismCentralIp;
        }

        private String getUserName() {
            return userName;
        }

        private String getPassword() {
            return password;
        }

        public BlueprintLaunchDescriptorImpl(){
            load();
        }

        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            // Indicates that this builder can be used with all kinds of project types
            return true;
        }


        public String getDisplayName() {
            return "Nutanix Calm Blueprint Launch";
        }


        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            save();
            LOGGER.debug("Inside configure");
            return super.configure(req, formData);

        }

        @JavaScriptMethod
        public synchronized String createEditorId() {
            Jenkins.getInstance().checkPermission(Permission.CONFIGURE);
            CalmGlobalConfiguration calmGlobalConfiguration = CalmGlobalConfiguration.get();
            prismCentralIp = calmGlobalConfiguration.getPrismCentralIp();
            String credId = calmGlobalConfiguration.getCredentials();
            List<StandardUsernamePasswordCredentials> standardCredentials = CredentialsProvider.lookupCredentials
                    (StandardUsernamePasswordCredentials.class, Jenkins.getInstance(), ACL.SYSTEM, Collections.<DomainRequirement>emptyList());
            for(StandardUsernamePasswordCredentials credential : standardCredentials){
                if(credential.getId().equals(credId)){
                    userName = credential.getUsername();
                    password = credential.getPassword().getPlainText();
                    break;
                }
            }
            rest = new Rest(prismCentralIp, userName, password);
            return String.valueOf(lastEditorId++);
        }


        @JavaScriptMethod
        public List<String> fetchProjects()throws Exception{
            /**
             * This method gets called by the javascript in the config.jelly for listing projects in UI
             */
            Jenkins.getInstance().checkPermission(Permission.CONFIGURE);
            return Project.getInstance(rest).getProjectNames();
        }


        @JavaScriptMethod
        public List<String> fetchBlueprints(String projectName)throws Exception{
            /**
             * This method get called by the javascript in the config.jelly for listing blueprints in UI
             */
            Jenkins.getInstance().checkPermission(Permission.CONFIGURE);
            blueprintHelper = Blueprint.getInstance(rest);
            return blueprintHelper.getBlueprintsList(projectName);
        }

        @JavaScriptMethod
        public List<String> fetchAppProfiles(String blueprintName)throws Exception{
            /**
             * This method get called by the javascript in the config.jelly for listing profiles in UI
             */
            Jenkins.getInstance().checkPermission(Permission.CONFIGURE);
            return blueprintHelper.getAppProfiles(blueprintName);
        }

        @JavaScriptMethod
        public List<String> fetchProfileActions(String blueprintName,String appProfileName)throws Exception{
            Jenkins.getInstance().checkPermission(Permission.CONFIGURE);
            return blueprintHelper.getProfileActions(blueprintName, appProfileName);
        }

        @JavaScriptMethod
        public  String fetchRuntimeProfileVariables(String blueprintName, String appProfileName)throws Exception{
            Jenkins.getInstance().checkPermission(Permission.CONFIGURE);
            return blueprintHelper.fetchRunTimeProfileVariables(blueprintName, appProfileName);
        }

        @JavaScriptMethod
        public String fetchBlueprintDescription(String blueprintName){
            Jenkins.getInstance().checkPermission(Permission.CONFIGURE);
            return blueprintHelper.fetchBlueprintDescription(blueprintName);
        }

        public ListBoxModel doFillProjectNameItems(@QueryParameter("projectName") String projectName){
            return new ListBoxModel(new ListBoxModel.Option(projectName));
        }

        public ListBoxModel doFillBlueprintNameItems(@QueryParameter("blueprintName") String blueprintName){
            return new ListBoxModel(new ListBoxModel.Option(blueprintName));
        }

        public ListBoxModel doFillAppProfileNameItems(@QueryParameter("appProfileName") String appProfileName){
            return new ListBoxModel(new ListBoxModel.Option(appProfileName));
        }


        public ListBoxModel doFillActionNameItems(@QueryParameter("actionName") String actionName){
            return new ListBoxModel(new ListBoxModel.Option(actionName));
        }

    }


}
