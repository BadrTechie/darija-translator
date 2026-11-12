Darija Translator Project 2 submission archive
================================================

The ZIP is generated only from Git-tracked files, which excludes secrets and build output.

Rebuild from the repository root after committing final changes:

  New-Item -ItemType Directory -Force -Path .\submission | Out-Null
  git archive --format=zip --prefix=Badreddine_Lechhab_project2/ --output=.\submission\Badreddine_Lechhab_project2.zip HEAD

Inspect without extracting:

  Add-Type -AssemblyName System.IO.Compression.FileSystem
  $zip = [IO.Compression.ZipFile]::OpenRead((Resolve-Path .\submission\Badreddine_Lechhab_project2.zip))
  try { $zip.Entries | Select-Object FullName, Length } finally { $zip.Dispose() }

The archive must contain README.md, backend/mvnw, backend/mvnw.cmd, backend source,
all four clients, docs/uml/*.puml, docs/uml/*.svg, and docs/demo-script.md.
