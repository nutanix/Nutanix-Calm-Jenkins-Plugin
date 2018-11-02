package com.calm.CalmHelpers;

import com.calm.Interface.Rest;
import com.calm.Logger.NutanixCalmLogger;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class Project {
    private final Rest rest;
    private final long THRESHOLD = 300000l;
    private static long lastUpdated;
    private static HashMap<String, String> projectNameUUID;
    private static List<String> projectNames;
    private static Project projectHelper;
    private final static NutanixCalmLogger LOGGER = new NutanixCalmLogger(Project.class);

    private Project(Rest rest) throws Exception{
        this.rest = rest;
        projectNameUUID = new HashMap<>();
        projectNames =  new ArrayList<>();
        fetchProjects();
        lastUpdated = System.currentTimeMillis();
    }

    public static Project getInstance(Rest rest) throws Exception{
//        if(projectHelper != null) {
//            long currentTime = System.currentTimeMillis();
//            if((currentTime - lastUpdated) > projectHelper.THRESHOLD) {
//                projectHelper.fetchProjects();
//                lastUpdated = currentTime;
//            }
//            return projectHelper;
//        }
        return new Project(rest);
    }
    private void fetchProjects(){
        JSONArray projectListObject;
        try {
            JSONObject response = rest.post("projects/list");
            projectListObject = response.getJSONArray("entities");
        }
        catch (Exception e){
            LOGGER.debug("ERROR occurred while fetching projects");
            LOGGER.debug(LOGGER.getStackTraceStr(e.getStackTrace()));
            projectNames = null;
            return;
        }
        for (int i = 0; i < projectListObject.length(); i++) {
            try {
                JSONObject project = projectListObject.getJSONObject(i);
                String projectName = project.getJSONObject("status").getString("name");
                String projectUuid = project.getJSONObject("metadata").getString("uuid");
                projectNameUUID.put(projectName, projectUuid);
                projectNames.add(projectName);
            }
            catch (Exception e){
                LOGGER.debug("ERROR occurred while paring a project object");
                LOGGER.debug(LOGGER.getStackTraceStr(e.getStackTrace()));
            }
        }
    }

    public List<String> getProjectNames(){
        return projectNames;
    }

    public String getProjectUUID(String projectName){

        return projectNameUUID.get(projectName);

    }
}
