#!/bin/bash

# Get the arguments passed to the script.
while getopts v:r:p: flag
do
    case "${flag}" in
        v) version=${OPTARG};;
        r) repository=${OPTARG};;
        p) passphrase=${OPTARG};;
    esac
done

dir=target/archive-pack
archive="tally-$version.tar.gz"

echo "Archive artifacts."

# Create target directory
mkdir -p $dir

# Copy everything for the archive
cp target/backend*.jar $dir/tally.jar

# Make archive
echo "---"
(cd $dir && tar -czvf "../$archive" *)
echo "---"

if [ ! -f "target/$archive" ]; then
    echo "target/$archive does not exist."
    exit 2
fi

echo "Upload archive."

# Upload the archive via curl.
http_status=$(curl -s -o /dev/null -w "%{http_code}" -X PUT \
  -H "Authorization: Basic $passphrase" \
  -H "Content-Type: application/octet-stream" \
  --data-binary "@target/$archive" \
  --url "$repository/tally/$archive" \
)

if [ "$http_status" -ne 201 ]; then

  echo "Failed uploading archive ($http_status)."
  exit 22

fi

echo "Archive successfully uploaded ($http_status)."

exit $?
