#!/usr/bin/env bash

# get the base image from the pom
POM_IMAGE_SHA=$(grep -Po '(?<=<from.image>).*?(?=</from.image>)' ./pom.xml)
BASE_IMAGE=$(cut --delimiter='@' -f1 <<< "$POM_IMAGE_SHA")

# get latest sha from remote
REMOTE_SHA=$(docker buildx imagetools inspect "$BASE_IMAGE":latest | head -n 3 | tail -n 1 | awk '{print $2}')
REMOTE_IMAGE_SHA="$BASE_IMAGE@$REMOTE_SHA"

echo "local (pom): $POM_IMAGE_SHA"
echo "remote: $REMOTE_IMAGE_SHA"

# Update if remote not equal to local
if [ "$POM_IMAGE_SHA" != "$REMOTE_IMAGE_SHA" ]; then
    echo "Images are different. Updating base image."
    sed -i "s|${POM_IMAGE_SHA}|${REMOTE_IMAGE_SHA}|" pom.xml
    echo "Image updated to $REMOTE_IMAGE_SHA"

else
    echo "Images are the same. No update needed."

fi
