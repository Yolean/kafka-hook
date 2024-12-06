#!/usr/bin/env bash
[ -z "$DEBUG" ] || set -x
set -eo pipefail

MVNREPOPROJECT=$PWD
cd ../quarkus-gitea
git status && test -z "$(git status --porcelain)"
GITREF=$(git rev-parse HEAD);
mvn clean deploy
cd $MVNREPOPROJECT
cp -av ../quarkus-gitea/snapshots/se/yolean/quarkus-gitea* snapshots/se/yolean
git add -f snapshots/se/yolean/quarkus-gitea*
git diff --cached --stat
echo "TODO commit and push manually"
