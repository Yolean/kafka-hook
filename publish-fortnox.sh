#!/usr/bin/env bash
[ -z "$DEBUG" ] || set -x
set -eo pipefail

MVNREPOPROJECT=$PWD
cd ../quarkus-fortnox
git status && test -z "$(git status --porcelain)"
GITREF=$(git rev-parse HEAD);
mvn clean deploy
cd $MVNREPOPROJECT
cp -av ../quarkus-fortnox/snapshots/se/yolean/quarkus-fortnox* snapshots/se/yolean
git add -f snapshots/se/yolean/quarkus-fortnox*
git diff --cached --stat
git commit -m "quarkus-fortnox $GITREF"
echo "TODO Review and push manually."
