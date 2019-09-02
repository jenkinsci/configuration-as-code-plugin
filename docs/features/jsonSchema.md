# JSON Schema 

The new JSON Schema is structured to validate the yaml files that are loaded via JCasC.
The structure and validation of the Schema is done based on the user-installed plugins.

=== Version

The Schema uses JSON draft v07. 
http://json-schema.org/draft-07/schema#

=== How to use

* The schema will be available at /configuration-as-code/newJSONSchema
* Users can use various online JSON validators to check against their yaml/json.

=== Progress

* The new JSON Schema is partially working and is in beta mode.
* The schema validates any missing  configurator objects and invalid data formats.
* It is built around the existing executable xml (jelly files) and follows the latest draft version of JSON.
* The schema does not yet validate against deep nested configurations.
* We are working towards supporting deep nested yml files.

== Improvements

* The Old Schema was generated using jelly files that did not generate a valid JSON Schema.
* Without a valid schema it is not possible to validate a yaml file rendering the schema unusable.
* The previous schema included an `"$ref":` for an `object` type, the new draft of the schema makes it mandatory
  to use `"$id":` hence the new schema uses it instead of ref.

== What the schema does

* The schema validates simple yaml files with root configurators.
```yaml
# config truncated
jenkins:
  numExecutors: 2
```
* In the above example if the data type of numExecutors is entered wrong, the schema will invalidate it.

== What the schema does not do

* It cannot validated nested yml files, with multiple levels of configurators.
```yaml
# config truncated
jenkins:
  numExecutors: 0

  nodes:
    - dumb:
        mode: NORMAL
        name: "agent1"
```
* The above yaml has a deep level of nesting which the JSON Schema currently does not support.


