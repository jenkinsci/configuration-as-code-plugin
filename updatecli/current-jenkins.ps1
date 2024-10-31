param(
  [Parameter(Position = 0)]
  [string] $PomPath
)

[xml]$xml = Get-Content $PomPath
$xml.project.properties.'jenkins.version'
