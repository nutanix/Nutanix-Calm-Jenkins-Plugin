package com.calm.CalmHelpers;

import com.calm.Executor.CalmExecutor;
import com.calm.Interface.Rest;
import com.calm.Logger.NutanixCalmLogger;
import hudson.EnvVars;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.PrintStream;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class Application {
    private final Rest rest;
    private final long THRESHOLD = 300000l;
    public static Map<String, String> systemActions;
    private Blueprint blueprintHelper;
    static {
        systemActions = new HashMap<>();
        systemActions.put("START", "action_start");
        systemActions.put("STOP", "action_stop");
        systemActions.put("RESTART", "action_restart");
        systemActions.put("DELETE", "action_delete");
        systemActions.put("SOFT_DELETE", "action_soft_delete");
    }
    private static long lastUpdated;
    private static List<String> applicationNames;
    private static Application applicationHelper;
    public static Map<String, String> applicationNameUuidPair;
    private final static NutanixCalmLogger LOGGER = new NutanixCalmLogger(Application.class);

    private Application(Rest rest) throws Exception{
        this.rest = rest;
        applicationNames = new ArrayList<>();
        applicationNameUuidPair = new HashMap<>();
        fetchApplications();
        blueprintHelper = Blueprint.getInstance(rest);
        lastUpdated = System.currentTimeMillis();
    }

    public static Application getInstance(Rest rest) throws Exception{
//        if(applicationHelper != null) {
//            long currentTime = System.currentTimeMillis();
//            if((currentTime - lastUpdated) > applicationHelper.THRESHOLD) {
//                applicationHelper.fetchApplications();
//                lastUpdated = currentTime;
//            }
//            return applicationHelper;
//        }
        return new Application(rest);
    }

    private void fetchApplications() {
        JSONObject responseObject;
        JSONArray entities;
        try {
            responseObject = rest.post("apps/list");
            entities = responseObject.getJSONArray("entities");
        }
        catch (Exception e) {
            applicationNames = null;
            LOGGER.debug("Error occurred while fetching the applications :" + e.getMessage());
            return;
        }

        for (int i = 0; i < entities.length(); i++) {
            try {
                    JSONObject entity = entities.getJSONObject(i);
                    String state = entity.getJSONObject("status").getString("state");
                    if ((state.equals("running")) || (state.equals("stopped"))) {
                        String applicationName = entity.getJSONObject("status").getString("name");
                        applicationNames.add(applicationName);
                        applicationNameUuidPair.put(applicationName, entity.getJSONObject("metadata").getString("uuid"));
                    }
            }
            catch (Exception e){
                //Skip this entity and go ahead
                LOGGER.debug("Error occurred while parsing the response");
                LOGGER.debug(LOGGER.getStackTraceStr(e.getStackTrace()));
            }
        }
    }

    public List<String> getApplicationNames(){
        return applicationNames;
    }

    public String getApplicationUuidByRequestId(String blueprintUuid, String requestId) throws Exception{
        String appUuid = null;
        do {
            JSONObject  applicationJSON = rest.get("blueprints/" + blueprintUuid + "/pending_launches/" + requestId);
            appUuid = applicationJSON.getJSONObject("status").getString("application_uuid");
            TimeUnit.SECONDS.sleep(15);
        } while (appUuid.equals("null"));
        return appUuid;
    }

    public JSONObject getApplicationDetails(String applicationUuid) throws Exception{
        return rest.get("apps/" + applicationUuid);
    }

    public List<String> getApplicationActions(String applicationName){
        List<String> applicationActions = new ArrayList<>();
        String  appUuid;
        JSONObject applicationJson;
        JSONArray entities;
        try {
            appUuid = applicationNameUuidPair.get(applicationName);
            applicationJson = getApplicationDetails(appUuid);
            entities = applicationJson.getJSONObject("status").getJSONObject("resources").getJSONArray("action_list");
        }
        catch (Exception e){
            LOGGER.debug("ERROR occurred while fetching actions for: " + applicationName );
            LOGGER.debug(LOGGER.getStackTraceStr(e.getStackTrace()));
            return null;
        }

        for (int i = 0; i < entities.length(); i++) {
            try{
                JSONObject entity = entities.getJSONObject(i);
                String actionName = entity.getString("name");
                if (!entity.getString("type").equals("system")) {
                    applicationActions.add(actionName);
                }
            }
            catch (Exception e){
                LOGGER.debug("ERROR occurred while parsing the entity: ");
                LOGGER.debug(LOGGER.getStackTraceStr(e.getStackTrace()));
            }
        }
        applicationActions.addAll(getSystemActionsList());
        return applicationActions;
    }

    public String waitForApplicationToGoIntoSuccessState(String appName, String applicationUuid, String state_name, PrintStream logger) throws Exception{
        JSONObject applicationJson = getApplicationDetails(applicationUuid);
        String applicationStatus = applicationJson.getJSONObject("status").getString("state");
        while (!applicationStatus.equals(state_name)) {
            TimeUnit.SECONDS.sleep(45);
            applicationJson = getApplicationDetails(applicationUuid);
            applicationStatus = applicationJson.getJSONObject("status").getString("state");
            if (applicationStatus.equals(state_name) || applicationStatus.equalsIgnoreCase("error") || applicationStatus.equalsIgnoreCase("failure"))
                break;
            logger.println(" Application " + appName + " status is : " + applicationStatus);
        }
        return applicationStatus;
    }

    public String fetchRuntimeProfileActionVariables(String applicationName, String actionName)throws Exception{
        String appUuid = applicationNameUuidPair.get(applicationName);
        JSONObject applicationJson = getApplicationDetails(appUuid);
        Map<String, String> keyValuePair = new HashMap<>();
        JSONArray entities = applicationJson.getJSONObject("status").getJSONObject("resources").getJSONArray("action_list");
        for (int i = 0; i < entities.length(); i++) {
            JSONObject entity = entities.getJSONObject(i);
            if(entity.getString("name").equals(actionName)){
                JSONArray varlist;
                try {
                    varlist = entity.getJSONObject("runbook").getJSONArray("variable_list");
                }
                catch (Exception e){
                    LOGGER.debug("ERROR occurred while getting runtime variables for application " + applicationName +
                                 " action " + actionName);
                    LOGGER.debug(LOGGER.getStackTraceStr(e.getStackTrace()));
                    return null;
                }
                for (int j = 0; j < varlist.length(); j++) {
                    JSONObject num = varlist.getJSONObject(j);
                    try {
                        if (num.has("editables")) {
                            JSONObject editables = num.getJSONObject("editables");
                            if ((editables.has("value") && editables.getBoolean("value"))) {
                                if ((num.has("attrs")) && (num.getJSONObject("attrs").has("is_secret_modified"))) {
                                    JSONObject attrs = num.getJSONObject("attrs");
                                    String name = (num.getString("name"));
                                    String value = "";
                                    keyValuePair.put(name, value);
                                } else {
                                    String name = (num.getString("name"));
                                    String value = (num.getString("value"));
                                    keyValuePair.put(name, value);
                                }
                            }
                        }
                    }
                    catch (Exception e){
                        LOGGER.debug("ERROR occurred while parsing runtime variable: "+ num.toString());
                        LOGGER.debug(LOGGER.getStackTraceStr(e.getStackTrace()));
                    }

                }
                break;
            }
        }
        JSONObject appProfileActionVariables = new JSONObject();
        for(String key : keyValuePair.keySet())
            appProfileActionVariables.put(key, keyValuePair.get(key));
        return appProfileActionVariables.toString();
    }

    public JSONObject patchAppProfileActionVariables(JSONObject actionSpec, String applicationName, String actionName,
                                                 String runtimeVariablesStr, PrintStream logger)throws Exception{
            JSONArray args_list = new JSONArray();
            String appUuid = applicationNameUuidPair.get(applicationName);
            JSONObject appResponse = getApplicationDetails(appUuid);
            JSONObject appProfileActionVariables = new JSONObject(runtimeVariablesStr);
            JSONArray actionList = appResponse.getJSONObject("status").getJSONObject("resources").getJSONArray("action_list");
            for (int i = 0; i < actionList.length(); i++) {
                JSONObject action = actionList.getJSONObject(i);
                if (action.getString("name").equals(actionName)){
                    JSONArray variableList = action.getJSONObject("runbook").getJSONArray("variable_list");
                    for(int j = 0;j < variableList.length();j++){
                        JSONObject variable = variableList.getJSONObject(j);
                        JSONObject var = new JSONObject();
                        String variableName = variable.getString("name");
                        var.put("name", variableName);
                        if(appProfileActionVariables.has(variableName))
                            var.put("value", appProfileActionVariables.get(variableName));
                        else
                            var.put("value", variable.getString("value"));
                        args_list.put(var);
                    }
                    break;
                }
            }
            actionSpec.getJSONObject("spec").put("args", args_list);
            return actionSpec;
    }

    public String getActionUuid(String applicationName, String actionName) throws Exception{
        String appUuid = applicationNameUuidPair.get(applicationName);
        JSONObject applicationDetails = getApplicationDetails(appUuid);
        JSONArray actionList = applicationDetails.getJSONObject("status").getJSONObject("resources").getJSONArray("action_list");
        for (int i = 0; i < actionList.length(); i++) {
            JSONObject action = actionList.getJSONObject(i);
            if(action.getString("name").equals(actionName))
                return action.getString("uuid");

        }
        return null;
    }

    public JSONObject runAction(String applicationName, String actionName, JSONObject actionSpec) throws Exception{
        String applicationUUid = applicationNameUuidPair.get(applicationName);
        actionName = systemActions.get(actionName) != null ? systemActions.get(actionName) : actionName;
        switch (actionName){
            case "action_delete"         : return rest.delete("apps/" + applicationUUid);
            case "action_soft_delete"    : return rest.delete("apps/" + applicationUUid + "?type=soft");
            default               : String actionUuid = getActionUuid(applicationName, actionName);
                                    JSONObject applicationJson = getApplicationDetails(applicationUUid);
                                    String applicationUuid = applicationJson.getJSONObject("metadata").getString("uuid");
                                    return rest.post("apps/"+ applicationUuid + "/actions/" +
                                            actionUuid+"/run", actionSpec.toString());
        }
    }


    public String waitForActionToComplete(String applicationName, String runlogUuid, PrintStream logger)throws Exception{
        String applicationUuid = applicationNameUuidPair.get(applicationName);
        JSONObject actionStatusResponse = rest.get("apps/" + applicationUuid + "/app_runlogs/" + runlogUuid);
        String actionStatus = actionStatusResponse.getJSONObject("status").getString("state");
        String state = "SUCCESS";
        String actionName = actionStatusResponse.getJSONObject("status").getJSONObject("action_reference").getString("name");

        while (!actionStatus.equals(state)) {
            TimeUnit.SECONDS.sleep(45);
            actionStatusResponse = rest.get("apps/" + applicationUuid + "/app_runlogs/" + runlogUuid);
            actionStatus = actionStatusResponse.getJSONObject("status").getString("state");
            if (actionStatus.equals(state) || actionStatus.equalsIgnoreCase("error") || actionStatus.equalsIgnoreCase("failure"))
                break;
            logger.println(" Application Action " + actionName + " status is : " + actionStatus);
        }
        return actionStatus;
    }

    public String getAppUUID(String appName)throws Exception {
        return applicationNameUuidPair.get(appName);
    }
    public void taskOutput(String appUuid, String action, PrintStream logger)throws Exception {
        String actionParentReference = null;
        String actionData;
        String spec = "{\"filter\": \"application_reference==" + appUuid + ";(type==action_runlog,type==audit_runlog)\"}";
        JSONObject response = rest.post("apps/" + appUuid + "/app_runlogs/list", spec);
        JSONArray entities =  response.getJSONArray("entities");
        for (int i = 0; i < entities.length(); i++) {
            JSONObject eachAction = entities.getJSONObject(i);
            String actionName = eachAction.getJSONObject("status").getJSONObject("action_reference").getString("name");
            if (actionName.equals(action)) {
                actionParentReference = eachAction.getJSONObject("metadata").getString("uuid");
                actionData = "{\"filter\": \"root_reference==" + actionParentReference + "\" }";
                response = rest.post("apps/" + appUuid + "/app_runlogs/list", actionData);
                entities = response.getJSONArray("entities");
                for (int j = 0; j < entities.length(); j++) {
                    eachAction = entities.getJSONObject(j);
                    if (eachAction.getJSONObject("status").has("task_reference")) {
                        String taskName = eachAction.getJSONObject("status").getJSONObject("task_reference").getString("name");
                        String taskUuid = eachAction.getJSONObject("metadata").getString("uuid");
                        response = rest.get("apps/" + appUuid + "/app_runlogs/" + taskUuid + "/output");
                        JSONArray outputList = response.getJSONObject("status").getJSONArray("output_list");
                        String output = outputList.getJSONObject(0).getString("output");
                        logger.println("Task Name: " + taskName);
                        logger.println(taskName + " output is " + output);
                    }
                }
            }
            break;
        }
    }

    public String getAppName(String appUUID) throws Exception {
        JSONObject appResponse = getApplicationDetails(appUUID);
        return appResponse.getJSONObject("status").getString("name");
    }

    public String getAppState(String appName)throws Exception{
        String appUUID = applicationNameUuidPair.get(appName);
        JSONObject appResponse = getApplicationDetails(appUUID);
        return appResponse.getJSONObject("status").getString("state");
    }

    public static List<String> getSystemActionsList(){
        return new ArrayList<>(systemActions.keySet());
    }

    public List<String> getProfileActionsFromBlueprint(String blueprintName, String appProfileName) throws Exception{
        String blueprintUuid = Blueprint.blueprintNameUuidPair.get(blueprintName);
        JSONObject blueprint = blueprintHelper.getBlueprintDetails(blueprintUuid);
        JSONArray appProfileList =  blueprint.getJSONObject("spec").getJSONObject("resources").
                getJSONArray("app_profile_list");
        List<String> profileActions = new ArrayList<String>();
        for (int i = 0; i < appProfileList.length(); i++) {
            JSONObject profilesList = appProfileList.getJSONObject(i);
            if (profilesList.getString("name").equals(appProfileName)) {
                JSONArray actionList = profilesList.getJSONArray("action_list");
                for (int j = 0; j < actionList.length(); j++) {
                    JSONObject eachAction = actionList.getJSONObject(j);
                    String actionName = eachAction.getString("name");
                    profileActions.add(actionName);
                }
                break;
            }
        }
        profileActions.addAll(Application.getSystemActionsList());
        return profileActions;
    }


    public String getProfileActionsVariablesFromBlueprint(String blueprintName, String appProfileName, String actionName) throws Exception{
        String blueprintUuid = Blueprint.blueprintNameUuidPair.get(blueprintName);
        JSONObject blueprint = blueprintHelper.getBlueprintDetails(blueprintUuid);
        JSONArray appProfileList = blueprint.getJSONObject("spec").getJSONObject("resources").
                getJSONArray("app_profile_list");
        HashMap<String, String> profileActionsRuntimeMap = new HashMap<String, String>();
        for (int i = 0; i < appProfileList.length(); i++) {
            JSONObject appProfile = appProfileList.getJSONObject(i);
            if(appProfile.getString("name").equals(appProfileName)) {
                //fetch profile action runtime variables
                JSONArray actionList = appProfile.getJSONArray("action_list");
                for (int j = 0; j < actionList.length(); j++) {
                    JSONObject action = actionList.getJSONObject(j);
                    if (action.getString("name").equals(actionName)) {
                        JSONArray variableList = action.getJSONObject("runbook").getJSONArray("variable_list");
                        profileActionsRuntimeMap = blueprintHelper.getRuntimeVariables(variableList);
                        break;
                    }
                }
                break;
            }
        }
        JSONObject appProfileActionVariables = new JSONObject();
        for(String key : profileActionsRuntimeMap.keySet())
            appProfileActionVariables.put(key, profileActionsRuntimeMap.get(key));
        return appProfileActionVariables.toString();
    }

    public HashMap<String, String> applicationDetails(String applicationName) throws Exception{
        HashMap<String, String> keyValuePair = new HashMap<>();
        String  appUuid;
        JSONObject applicationJson;
        JSONArray entities;
        try {
            appUuid = applicationNameUuidPair.get(applicationName);
            applicationJson = getApplicationDetails(appUuid);
            entities = applicationJson.getJSONObject("status").getJSONObject("resources").getJSONArray("deployment_list");
        }
        catch (Exception e){
            LOGGER.debug("ERROR occurred while fetching deployment list for: " + applicationName );
            LOGGER.debug(LOGGER.getStackTraceStr(e.getStackTrace()));
            return null;
        }
        for (int i = 0; i < entities.length(); i++) {
            try{
                JSONObject entity = entities.getJSONObject(i);
                JSONArray elementList = entity.getJSONObject("substrate_configuration").getJSONArray("element_list");
                for (int j = 0; j < elementList.length(); j++) {
                    JSONObject element = elementList.getJSONObject(j);
                    String instanceName =  element.getString("instance_name");
                    String type = element.getString("type");
                    String address = element.getString("address");
                    if(elementList.length() > 1) {
                        String vmDetails = instanceName+ j + "_" +type;
                        keyValuePair.put(vmDetails, address);
                    }
                    else{
                        String vmDetails = instanceName + "_" +type;
                        keyValuePair.put(vmDetails, address);
                    }
                }
            }
            catch (Exception e){
                LOGGER.debug("ERROR occurred while parsing the entity: ");
                LOGGER.debug(LOGGER.getStackTraceStr(e.getStackTrace()));
            }
        }
        return keyValuePair;
    }

    public static void main(String[] args) throws Exception{
        Rest rest =  new Rest("10.46.6.2", "admin", "Nutanix.123", false);
        Application app = Application.getInstance(rest);
        HashMap<String, String> appDetails = app.applicationDetails("Parallels300_1");
        System.out.println(appDetails);
    }


}





