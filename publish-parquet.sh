#!/usr/bin/env bash
[ -z "$DEBUG" ] || set -x
set -eo pipefail

REPOHOME=$PWD
DEPLOYNAME=quarkus-parquet
DEPLOYROOT=../$DEPLOYNAME
rm -r "$DEPLOYROOT/snapshots" || true
mkdir -p "$DEPLOYROOT/snapshots/se/yolean"
if [ -d "$REPOHOME/snapshots/se/yolean/$DEPLOYNAME" ]; then
  rsync -av "$REPOHOME/snapshots/se/yolean/$DEPLOYNAME"* "$DEPLOYROOT/snapshots/se/yolean"
fi

cd $DEPLOYROOT
git status && test -z "$(git status --porcelain)"
GITREF=$(git rev-parse HEAD)
mvn clean deploy
cd $REPOHOME

rsync -av "$DEPLOYROOT/snapshots/se/yolean/$DEPLOYNAME"* "$REPOHOME/snapshots/se/yolean"
git add -f "snapshots/se/yolean/$DEPLOYNAME"*
git diff --cached --stat
git commit -m "$DEPLOYNAME $GITREF"
echo "TODO Review and push manually."
