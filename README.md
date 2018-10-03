# Nutanix Calm Plugin
Nutanix Calm Jenkins plugin allows you to launch Nutanix Calm blueprint, provision infrastructure, and services in a multi cloud environment and subsequently run actions/tasks on those applications.

#### License:
* All source code is licensed under the MIT license.

#### Supported Versions:
* Jenkins versions: 2.107.2 and later
* Nutanix Calm versions: 5.7.1 and later
* Google Chrome version:  69.0.3497.100 (Official Build) (64-bit)

#### Plugin Installation:
* Navigate to Manage Jenkins-> Manage Plugin-> Available. Search for "Nutanix Calm".  Select the Nutanix Calm Plugin. Click to install.
* Navigate to Manage Jenkins→ Manage Plugin → Advanced.  Upload the plugin file.

#### Plugin Configuration:
* To configure the plugin first Navigate to Manage Jenkins -> Configure System -> Nutanix Calm Plugin Configuration. Provide the Prism Central IP, Username and Password.

#### Jenkins Freestyle job Setup:
* Click on New Item. Select Freestyle project. Enter an item name. Click OK.
* Click on Add Build step. Select **Nutanix Calm Blueprint Launch**.
* In the section
    * Select the Calm project.
    * Select the Blueprint to launch. Blueprint description is fetched and displayed.
    * Select the Application profile.
    * Modify the values for runtime variables available for that application profile.
    * Provide an application name. BUILD_ID is appended by default to the application name to uniquely identify it in Calm.
    * Select the option if you want Jenkins job to wait for blueprint launch to complete before proceeding to the next step.
* To invoke actions defined in the Calm blueprint/ application, click on Add Build Step. Select **Nutanix Calm Application Action Run**.
* In the section
    * Select the application name.
    * Select the application actions available.
    * If necessary, modify the values for the runtime variables available.

#### Jenkins Pipeline:
* Click on New Item. Select Pipeline. Enter an item name. Click OK.
* To generate the pipeline syntax for **Nutanix Calm Blueprint Launch**, click on the Pipeline Syntax at the bottom.
* In the Pipeline Syntax window, select the General build Step in the Sample step dropdown.
* Select Nutanix Calm Blueprint Launch in Build Step
* In the section
    * Select the Calm project.
    * Select the Blueprint to launch. Blueprint description is fetched and displayed.
    * Select the Application profile.
    * Modify the values for runtime variables available for that application profile.
    * Provide an application name. BUILD_ID is appended by default to the application name to uniquely identify it in Calm.
    * Select the option if you want Jenkins job to wait for blueprint launch to complete before proceeding to the next step.
* Click on Generate Pipeline Script. Copy and paste the text in the box below into the pipeline script box.
* Follow same steps to generate the pipeline syntax for **Nutanix Calm Application Action Run**.
