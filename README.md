# Calm_Jenkins_Plugin
Jenkins Nutanix Calm plugin allows you to launch Nutanix Calm blueprint, provision infrastructure and services in multi cloud environment and subsequently run actions/tasks on those applications.

#### License:
* All source code is licensed under the MIT license.

#### Supported Versions:
* Jenkins support versions : 2.107.2 and later
* Calm Tested versions : 5.7.1 and later
* Google browser Tested version : Version 69.0.3497.100 (Official Build) (64-bit)

#### Plugin Installation:
* Navigate to Manage Jenkins-> Manage Plugin-> Available. Search for Nutanix Calm plugin. Click on install.

#### Plugin Configuration:
* To configure the plugin first Navigate to Manage Jenkins -> Configure System -> Nutanix Calm Plugin Configuration. Provide the Prism Central IP, Username and Password. The username can be of any user authorized in the SSP.

#### Jenkins Freestyle job Setup:
* Now that we have configured the plugin , we can go and launch the Nutanix Clam blueprint. First let us look at Adding the Calm specific build steps in the Freestyle projects, navigate to new item, select Free style project, Enter an item name, select OK.
Click Add Build step. Select Nutanix Calm Blueprint Launch. In the section, select the Calm project, Select the blueprint to launch, Select the application profile listed and modify the values for runtime variables available for that application profile. Provide an application name. BUILD_ID is appended by default to the application name to uniquely identify it in Calm. Select the option if you want Jenkins job to wait for blueprint launch to complete before proceeding to the next step.

* We can also invoke actions on the blueprint launched in the previous step or invoke any actions for the existing applications that are running in the Nutanix calm instance.
Click on Add Build Step. Select Nutanix Calm Application Action Run.  In the section select the application name. Select the application actions available. If necessary, modify the values for the runtime variables available.

#### Jenkins Pipeline:
* To utilize this plugin in the Jenkins pipeline, navigate to new item, select Pipeline, Enter an item name, select OK.
To generate the pipeline syntax , click on the Pipeline Syntax at the bottom.
* In the Pipeline Syntax window , Select the General build Step in the dropdown. IN the build step dropdown select the Nutanix Calm blueprint launch. The section similar to the one in the Freestyle project shows up. Select the project, Blueprint, Application profile , Variables , App name . Click on Generate Pipeline Script. Copy and paste the text in the box below into the pipeline script box.
* NOTE: We can also use Jenkinsfile from the any Source control management.






