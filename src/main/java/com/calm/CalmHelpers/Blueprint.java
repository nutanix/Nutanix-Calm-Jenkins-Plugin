package com.calm.CalmHelpers;

import com.calm.Interface.Rest;
import com.calm.Logger.NutanixCalmLogger;
import net.sf.json.processors.JsonBeanProcessor;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.*;


public class Blueprint {
    private final Rest rest;
    private final long THRESHOLD = 300000l;
    private static long lastUpdated;
    public static Map<String, String> blueprintNameUuidPair;
    private static Map<String, List<String>> projectBlueprintPairs;
    private static Blueprint blueprintHelper;
    private final static NutanixCalmLogger LOGGER = new NutanixCalmLogger(Blueprint.class);

    private Blueprint(Rest rest) throws Exception{
        blueprintNameUuidPair = new HashMap<>();
        projectBlueprintPairs = new HashMap<>();
        this.rest = rest;
        fetchBlueprintList();
        lastUpdated = System.currentTimeMillis();
    }

    public static Blueprint getInstance(Rest rest)throws Exception{
//        if(blueprintHelper != null) {
//            long currentTime = System.currentTimeMillis();
//            if((currentTime - lastUpdated) > blueprintHelper.THRESHOLD) {
//                blueprintHelper.fetchBlueprintList();
//                lastUpdated = currentTime;
//            }
//            return blueprintHelper;
//        }
        return new Blueprint(rest);
    }

    private void fetchBlueprintList(){
        /**
         * This method fetches the blueprints from pc. It stores the whole bp objects as json in blueprintList, and
         * project, bp name as pairs in projectBlueprintPairs
         */
        JSONObject responseObject;
        JSONArray blueprintEntities;
        try {
            responseObject = rest.post("blueprints/list");
            blueprintEntities = responseObject.getJSONArray("entities");
        }
        catch (Exception e){
            LOGGER.debug("ERROR occurred while fetching blueprints " +e.getMessage());
            LOGGER.debug(LOGGER.getStackTraceStr(e.getStackTrace()));
            blueprintNameUuidPair = null;
            projectBlueprintPairs = null;
            return;
        }
        for (int i = 0; i < blueprintEntities.length(); i++) {
            JSONObject blueprint = null;
            try {
                blueprint = blueprintEntities.getJSONObject(i);
                String status = blueprint.getJSONObject("status").getString("state");
                if (status.equals("ACTIVE") && blueprint.getJSONObject("metadata").has("project_reference")) {
                    String project_name = blueprint.getJSONObject("metadata").getJSONObject("project_reference").getString("name");
                    String blueprintName = blueprint.getJSONObject("status").getString("name");
                    blueprintNameUuidPair.put(blueprintName, blueprint.getJSONObject("metadata").getString("uuid"));
                    List<String> currentList = projectBlueprintPairs.get(project_name) != null ?
                            projectBlueprintPairs.get(project_name) : new ArrayList<String>();
                    currentList.add(blueprintName);
                    projectBlueprintPairs.put(project_name, currentList);
                }
            }
            catch (Exception e){
                LOGGER.debug("ERROR occurred while parsing the blueprint: " + blueprint.toString());
                LOGGER.debug(LOGGER.getStackTraceStr(e.getStackTrace()));
            }
        }
    }

    public List<String> getBlueprintsList(String projectName){
        /**
         * This method returns the list of blueprint names for the given project. If the relevant project is not found
         * returns null
         */
        if(projectBlueprintPairs != null) {
            List<String> blueprints =  projectBlueprintPairs.get(projectName);
            if(blueprints != null)
                return blueprints;
            return new ArrayList<>();
        }
        return null;
    }

    public String fetchBlueprintDescription(String blueprintName){
        String blueprintUuid = blueprintNameUuidPair.get(blueprintName);
        try{
            JSONObject blueprint = getBlueprintDetails(blueprintUuid);
            return blueprint.getJSONObject("status").getString("description");
        }
        catch(Exception e){
            LOGGER.debug("ERROR occurred while fetching blueprint details");
            LOGGER.debug(LOGGER.getStackTraceStr(e.getStackTrace()));
        }
        return "";
    }

    public JSONObject getBlueprintDetails(String blueprintUuid) throws Exception{
        return rest.get("blueprints/" + blueprintUuid);
    }
    public JSONObject getBlueprintSpec(String blueprintName, String appProfileName, String runtimeVariables,
                                       String applicationName) throws Exception{
        String blueprintUuid = blueprintNameUuidPair.get(blueprintName);
        JSONObject blueprintJson = getBlueprintDetails(blueprintUuid);
        JSONObject runtimeVariablesJson = new JSONObject(runtimeVariables);
        //JSONObject appProfileVariables = runtimeVariablesJson.getJSONObject("appProfileVariables");
        if(runtimeVariablesJson.length() > 0)
            blueprintJson = patchAppProfileVariables(blueprintJson, runtimeVariablesJson, appProfileName);
        String appname = applicationName;
        blueprintJson.remove("status");
        blueprintJson.getJSONObject("spec").remove("name");
        blueprintJson.getJSONObject("spec").put("application_name", appname);
        String appProfileUuid = null;
        JSONArray appProfileList = blueprintJson.getJSONObject("spec").getJSONObject("resources").getJSONArray("app_profile_list");
        for (int i = 0; i < appProfileList.length(); i++) {
            JSONObject appProfile = appProfileList.getJSONObject(i);
            if (appProfile.getString("name").equals(appProfileName)) {
                appProfileUuid = appProfile.getString("uuid");
                break;
            }
        }
        if (appProfileUuid == null) {
            throw new Exception("App profile with name " + appProfileName + " not found in list");
        }
        JSONObject appProfileReference = new JSONObject();
        appProfileReference.put("kind", "app_profile");
        appProfileReference.put("uuid", appProfileUuid);
        blueprintJson.getJSONObject("spec").put("app_profile_reference", appProfileReference);
        return blueprintJson;
    }


    public List<String> getAppProfiles(String blueprintName){
        /**
         * This method returns the appProfile list for the given blueprint
         */
        List<String> appProfileList = new ArrayList<>();
        JSONArray appProfiles;
        try {
            String blueprintUuid = blueprintNameUuidPair.get(blueprintName);
            JSONObject blueprint = getBlueprintDetails(blueprintUuid);
            appProfiles = blueprint.getJSONObject("spec").
                    getJSONObject("resources").getJSONArray("app_profile_list");
        }
        catch (Exception e){
            LOGGER.debug("ERROR occurred while fetching app profile for: " + blueprintName);
            LOGGER.debug(LOGGER.getStackTraceStr(e.getStackTrace()));
            return null;
        }
        for (int i = 0; i < appProfiles.length(); i++) {
            JSONObject appProfile = null;
            try {
                appProfile = appProfiles.getJSONObject(i);
                String profileName = appProfile.getString("name");
                appProfileList.add(profileName);
            }
            catch (Exception e){
                LOGGER.debug("ERROR occurred while parsing: "+ appProfile.toString());
                LOGGER.debug(LOGGER.getStackTraceStr(e.getStackTrace()));
            }
        }
        return appProfileList;
    }

    public List<String> getProfileActions(String blueprintName, String appProfileName) throws Exception{
        String blueprintUuid = blueprintNameUuidPair.get(blueprintName);
        JSONObject blueprint = getBlueprintDetails(blueprintUuid);
        JSONArray appProfileList =  blueprint.getJSONObject("spec").getJSONObject("resources").
                                                                                getJSONArray("app_profile_list");
        List<String> profileActions = new ArrayList<String>();
        for (int i = 0; i < appProfileList.length(); i++) {
            JSONObject profilesList = appProfileList.getJSONObject(i);
            if (profilesList.getString("name").equals(appProfileName)) {
                JSONArray actionList;
                try {
                    actionList = profilesList.getJSONArray("action_list");
                    for (int j = 0; j < actionList.length(); j++) {
                        JSONObject eachAction = actionList.getJSONObject(j);
                        String actionName = eachAction.getString("name");
                        profileActions.add(actionName);
                    }
                    break;
                }
                catch (Exception e){
                    LOGGER.debug("ERROR occurred while fetching actions for"+ appProfileName);
                    LOGGER.debug(LOGGER.getStackTraceStr(e.getStackTrace()));
                    return null;
                }
            }
        }
        profileActions.addAll(Application.getSystemActionsList());
        return profileActions;
    }

    public String fetchRunTimeProfileVariables(String blueprintName, String appProfileName) throws Exception{
        String blueprintUuid = blueprintNameUuidPair.get(blueprintName);
        JSONObject blueprint = getBlueprintDetails(blueprintUuid);
        JSONArray appProfileList = blueprint.getJSONObject("spec").getJSONObject("resources").
                                                                                    getJSONArray("app_profile_list");
        HashMap<String, String> appProfileRuntimeMap = new HashMap<String, String>();
        for (int i = 0; i < appProfileList.length(); i++) {
            JSONObject appProfile = appProfileList.getJSONObject(i);
            if(appProfile.getString("name").equals(appProfileName)) {
                try {
                    appProfileRuntimeMap = getRuntimeVariables(appProfile.getJSONArray("variable_list"));
                    break;
                }
                catch(Exception e){
                    LOGGER.debug("ERROR occurred while fetching runtime variables");
                    LOGGER.debug(LOGGER.getStackTraceStr(e.getStackTrace()));
                    return null;
                }

            }
        }
        JSONObject appProfileVariables = new JSONObject();
        for(String key : appProfileRuntimeMap.keySet())
            appProfileVariables.put(key, appProfileRuntimeMap.get(key));
        return appProfileVariables.toString();
    }

    public String fetchRunTimeActionVariables(String blueprintName, String appProfileName,
                                              String actionName) throws Exception{
        String blueprintUuid = blueprintNameUuidPair.get(blueprintName);
        JSONObject blueprint = getBlueprintDetails(blueprintUuid);
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
                        profileActionsRuntimeMap = getRuntimeVariables(variableList);
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


    public HashMap<String, String> getRuntimeVariables(JSONArray variableList) throws Exception{
        HashMap<String, String> keyValuePair = new HashMap<>();
        for (int i = 0; i < variableList.length(); i++) {
            JSONObject variable = variableList.getJSONObject(i);
            if (variable.has("editables")) {
                JSONObject editables = variable.getJSONObject("editables");
                if ((editables.has("value") && editables.getBoolean("value"))) {
                    if ((variable.has("attrs")) && (variable.getJSONObject("attrs").has("is_secret_modified"))) {
                        JSONObject attrs = variable.getJSONObject("attrs");
                        String name = (variable.getString("name"));
                        String value = "";
                        keyValuePair.put(name, value);
                    } else {
                        String name = (variable.getString("name"));
                        String value = (variable.getString("value"));
                        keyValuePair.put(name, value);
                    }
                }
            }
        }
        return keyValuePair;
    }

    private JSONObject patchAppProfileVariables(JSONObject blueprintJson, JSONObject profileVariables,
                                                String appProfileName) throws Exception{
        JSONArray appProfileList = blueprintJson.getJSONObject("spec").getJSONObject("resources").getJSONArray("app_profile_list");
        for (int i = 0; i < appProfileList.length(); i++) {
            JSONObject appProfile = appProfileList.getJSONObject(i);
            if (appProfile.getString("name").equals(appProfileName)) {
                Iterator<String> iterator = profileVariables.keys();
                while (iterator.hasNext()) {
                    String key = iterator.next();
                    String val = (String) profileVariables.get(key);
                    JSONArray varlist = appProfile.getJSONArray("variable_list");
                    for (int k = 0; k < varlist.length(); k++) {
                        JSONObject variable = varlist.getJSONObject(k);
                        if (variable.getString("name").equals(key)) {
                            blueprintJson.getJSONObject("spec").getJSONObject("resources").getJSONArray("app_profile_list").
                                    getJSONObject(i).getJSONArray("variable_list").getJSONObject(k).put("value", val);
                        }
                    }
                }
                break;
            }

        }
        return blueprintJson;
    }

    public JSONObject launchBlueprint(String blueprintUuid, JSONObject blueprintSpec) throws Exception{
        return rest.post("blueprints/" + blueprintUuid +"/launch", blueprintSpec.toString());
    }
}




