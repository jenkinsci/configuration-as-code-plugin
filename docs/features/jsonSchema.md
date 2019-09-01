# JSON Schema 

The JSON Schema is structured to validate the yaml files that are loaded via JCasC.The structure and validation of the Schema is done based on the user-installed plugins.

=== Version

The Schema uses JSON draft v07. 
http://json-schema.org/draft-07/schema#


=== Progress

* The new JSON Schema is partially working and is in beta mode.
* The schema validates any missing  configurator objects and invalid data formats.
* It is built around the existing executable xml (jelly files) and follows the latest draft version of JSON.
* The schema does not yet validate against deep nested configurations.
* We are working towards supporting deep nested yml files.

=== How to use

* The schema will be available at /configuration-as-code/NewJSONSchema
* Users can use various online JSON validators to check against their yaml/json.

