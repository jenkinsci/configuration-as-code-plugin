# Configuration Validation (check endpoint)

The Configuration as Code plugin can validate a YAML configuration without applying
it. This is useful to verify that a configuration file is correct before actually
loading it into Jenkins.

## How to use

* Send a `POST` request to the check endpoint with a valid CRUMB and authentication:

  ```
  JENKINS_URL/manage/configuration-as-code/check
  ```

* A valid configuration returns an empty JSON array `[]`.
* Validation errors are returned as a JSON array of issues.
* A malformed request returns an HTTP `400` response.
* By default, the endpoint requires the `Administer` permission.
* To allow a user that only has the `SystemRead` permission (without `Administer`)
  to run the check, start Jenkins with the following Java system property:

  ```
  -Dio.jenkins.plugins.casc.allowSystemReadCheck=true
  ```

## Example

```bash
curl -X POST \
  -u "user:token" \
  -H "Content-Type: application/yaml" \
  --data-binary @jenkins.yaml \
  "https://jenkins.example.com/manage/configuration-as-code/check"
```