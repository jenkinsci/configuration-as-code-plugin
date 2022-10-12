param(
  [Parameter(Position = 0)]
  [string] $PomPath,
  [Parameter(Position = 1)]
  [version] $NewVersion
)

$changed = $false
if ($null -eq $ENV:DRY_RUN) {
  $ENV:DRY_RUN = $false
}

$pom = New-Object System.Xml.XmlDocument
$pom.PreserveWhitespace = $true
$pom.Load($PomPath)

[version]$CurrentVersion = $pom.project.properties.'jenkins.version'
if ($null -ne $CurrentVersion -and $NewVersion -gt $CurrentVersion) {
  $changed = $true
  $pom.project.properties.'jenkins.version' = $NewVersion
}

if ($changed) {
  Write-Output "$NewVersion"

  if ($ENV:DRY_RUN -eq $false) {
    $utf8WithoutBom = New-Object System.Text.UTF8Encoding($false)
    $streamWriter = New-Object System.IO.StreamWriter($PomPath, $false, $utf8WithoutBom)
    $pom.Save($streamWriter)
    $streamWriter.Close()
  }
}
