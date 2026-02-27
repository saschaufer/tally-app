#!/bin/bash

# Get the arguments passed to the script.
while getopts n:g:v:s: flag
do
    case "${flag}" in
        n) appname=${OPTARG};;
        g) appgroup=${OPTARG};;
        v) appversion=${OPTARG};;
        s) sboms=${OPTARG};;
        *) # nothing
    esac
done

targetdir="target/sbom"
sbomsarray=()
for i in $sboms; do
  sbomsarray+=($i)
done

echo "
--------------------------------
Merge SBOMs
App name        : $appname
App group       : $appgroup
App version     : $appversion
SBOMs to merge  : ${sbomsarray[*]}
Target directory: $targetdir
--------------------------------"

mkdir -p "$targetdir"

# Uses cyclonedx-cli: https://github.com/CycloneDX/cyclonedx-cli
$CYCLONEDX_CLI merge \
  --input-files ${sbomsarray[*]} \
  --output-file "$targetdir/sbom.json" \
  --output-format "json" \
  --output-version "v1_6" \
  --hierarchical \
  --group "$appgroup" \
  --name "$appname" \
  --version "$appversion"
