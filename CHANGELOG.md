#### v1.2.2
Release date: _Feb 29, 2024_

* Remove unused org.jglobus from pom.xml
* Upgraded json from 20090211 to 20231013

#### v1.2.1
Release date: _Jun 23, 2022_

* Bug fix for blueprint launch failing with JSONObject["application_uuid"] not a string.
* Bug fix for action run failing with JSONArray(0) not found in Calm 3.5.0
* Update jenkins core requirement to 2.235.1

#### v1.2
Release date: _Jan 04, 2019_

* Ability to reference the Jenkins Environment variables in the Run time variables of Calm Blueprint
* Calm Service IP application variable is exposed as a Jenkins environment variable.


#### v1.1
Release date: _Nov 02, 2018_

* The Prism Central credentials are handled by the credentials plugin as prescribed by Jenkins.
* By default, the PC IP/URL expects an SSL certificate. There is an exclusive option to override the same.


#### v1.0
Release date: _Oct 18, 2018_

* In this release Plugin can be used with Jenkins Freestyle projects and Pipeline.
Launch Calm Blueprints and run application actions from the Jenkins build steps.
