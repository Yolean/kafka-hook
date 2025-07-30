#!/usr/bin/env bash
[ -z "$DEBUG" ] || set -x
set -eo pipefail

REPOHOME=$PWD
DEPLOYNAME=quarkus-fortnox
DEPLOYROOT=../$DEPLOYNAME
rm -r "$DEPLOYROOT/snapshots" || true
mkdir -p "$DEPLOYROOT/snapshots/se/yolean"
rsync -av "$REPOHOME/snapshots/se/yolean/$DEPLOYNAME"* "$DEPLOYROOT/snapshots/se/yolean"

cd $DEPLOYROOT
PROJECT_VERSION=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout)
if [ -d "$DEPLOYROOT/snapshots/se/yolean/$DEPLOYNAME/$PROJECT_VERSION" ]; then
  echo "$DEPLOYNAME $PROJECT_VERSION already exists, change version in pom.xml to publish a new version"
  exit 1
fi
git status && test -z "$(git status --porcelain)"
GITREF=$(git rev-parse HEAD)
mvn clean deploy
cd $REPOHOME

rsync -av "$DEPLOYROOT/snapshots/se/yolean/$DEPLOYNAME"* "$REPOHOME/snapshots/se/yolean"
git add -f "snapshots/se/yolean/$DEPLOYNAME"*
git diff --cached --stat
git commit -m "$DEPLOYNAME $GITREF"
echo "TODO Review and push manually."
