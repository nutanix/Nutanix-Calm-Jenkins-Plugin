package com.calm.Descriptors;

import com.calm.CalmHelpers.Application;
import com.calm.CalmHelpers.Blueprint;
import com.calm.Executor.CalmExecutor;
import com.calm.GlobalConfiguration.CalmGlobalConfiguration;
import com.calm.Interface.Rest;
import com.calm.Logger.NutanixCalmLogger;
import hudson.util.ListBoxModel;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.bind.JavaScriptMethod;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import hudson.*;
import hudson.model.*;
import hudson.tasks.*;
import org.kohsuke.stapler.*;
import jenkins.tasks.SimpleBuildStep;


public class RunApplicationAction extends Builder implements SimpleBuildStep {

    private final String applicationName, actionName, runtimeVariables;
    private String applicationUuid;

    @DataBoundConstructor
    public RunApplicationAction(String applicationName, String actionName, String runtimeVariables){
        this.applicationName = applicationName;
        this.actionName = actionName;
        this.runtimeVariables = runtimeVariables;
    }

    public String getApplicationName() {
        return applicationName;
    }


    public String getActionName(){
        return actionName;
    }

    public String getRuntimeVariables(){
        return runtimeVariables;
    }


    @Override
    public void perform(Run build, FilePath workspace, Launcher launcher, TaskListener listener) throws IOException, InterruptedException {
        PrintStream log = listener.getLogger();
        RunActionDescriptorImpl actionDescriptor = getDescriptor();
        String prismCentralIp = actionDescriptor.getPrismCentralIp();
        String userName = actionDescriptor.getUserName();
        String password = actionDescriptor.getPassword();
        Rest rest = actionDescriptor.getRest();
        EnvVars envVars = new EnvVars();
        final EnvVars env = build.getEnvironment(listener);
        //Expanding appname to include the env variables in it's name
        String expandedApplicationName = env.expand(applicationName);

        try {
            applicationUuid = Application.getInstance(rest).getAppUUID(expandedApplicationName);
        }
        catch (Exception e) {
            log.println(e.getMessage());
        }
        log.println(" ");
        log.println("Executing Nutanix Calm Application Action Run Build Step");
        CalmExecutor calmExecutor = new CalmExecutor(prismCentralIp, userName, password, expandedApplicationName,  actionName, runtimeVariables, log);
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

            if (globalError.size() > 0){
                listener.error("Nutanix Calm Prism Central Details Required" + globalError);
                build.setResult(Result.FAILURE);
            }
            calmExecutor.runAppAction();
            String action = Application.systemActions.get(actionName) != null ? Application.systemActions.get(actionName) : actionName;
            Application.getInstance(rest).taskOutput(applicationUuid, action, log);
        }
        catch (Exception e){
            log.println(e.getMessage());
            build.setResult(Result.FAILURE);
        }
    }


    @Override
    public RunActionDescriptorImpl getDescriptor() {
        return (RunActionDescriptorImpl) super.getDescriptor();
    }

    @Extension // This indicates to Jenkins that this is an implementation of an extension point.
    public static final class RunActionDescriptorImpl extends BuildStepDescriptor<Builder> {

        private String prismCentralIp;
        private String userName;
        private String password;
        private Rest rest;
        private int lastEditorId = 0;
        private Application applicationHelper;
        private final static NutanixCalmLogger LOGGER = new NutanixCalmLogger(RunActionDescriptorImpl.class);

        private String getPrismCentralIp() {
            return prismCentralIp;
        }

        private String getUserName() {
            return userName;
        }

        private String getPassword() {
            return password;
        }

        private Rest getRest (){ return rest;}

        public RunActionDescriptorImpl(){
            load();
        }

        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        public String getDisplayName() {
            return "Nutanix Calm Application Action Run";
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            save();
            return super.configure(req, formData);
        }

        @JavaScriptMethod
        public synchronized String createEditorId() {
            CalmGlobalConfiguration calmGlobalConfiguration = CalmGlobalConfiguration.get();
            prismCentralIp = calmGlobalConfiguration.getPrismCentralIp();
            userName = calmGlobalConfiguration.getUserName();
            password = calmGlobalConfiguration.getPassword();
            rest = new Rest(prismCentralIp, userName, password);
//            try{
//                applicationHelper = Application.getInstance(rest);
//            }
//            catch (Exception e){
//                LOGGER.debug("ERROR occurred while initializing the application helper");
//                LOGGER.debug(LOGGER.getStackTraceStr(e.getStackTrace()));
//            }
            return String.valueOf(lastEditorId++);
        }


        @JavaScriptMethod
        public List<String> fetchApplications()throws Exception{
            applicationHelper = Application.getInstance(rest);
            return applicationHelper.getApplicationNames();
        }

        @JavaScriptMethod
        public List<String> fetchApplicationActions(String applicationName)throws Exception{
            return applicationHelper.getApplicationActions(applicationName);
        }

        @JavaScriptMethod
        public  String fetchRuntimeProfileActionVariables(String applicationName, String actionName)throws Exception{
            return  applicationHelper.fetchRuntimeProfileActionVariables(applicationName, actionName);
        }

        @JavaScriptMethod
        public List<String> getProfileActionsFromBlueprint(String blueprintName, String appProfileName)throws Exception{
            return applicationHelper.getProfileActionsFromBlueprint(blueprintName, appProfileName);
        }

        @JavaScriptMethod
        public String getProfileActionsVariablesFromBlueprint(String blueprintName, String appProfileName, String actionName)throws Exception{
            return applicationHelper.getProfileActionsVariablesFromBlueprint(blueprintName, appProfileName, actionName);
        }

        public ListBoxModel doFillApplicationNameItems(@QueryParameter("applicationName") String applicationName){
            return new ListBoxModel(new ListBoxModel.Option(applicationName));
        }

        public ListBoxModel doFillActionNameItems(@QueryParameter("actionName") String actionName){
            return new ListBoxModel(new ListBoxModel.Option(actionName));
        }

    }

}
