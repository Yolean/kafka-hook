FROM yolean/builder-quarkus:2171b3f888b2b6fcbdbd36d91658f90611acf606@sha256:04b41a280c45e8081cb86ccdbe2ab4296e023382e05557e7086feb81a477cb8e \
  as dev

COPY pom.xml .

# This kind of caching step should be moved to a script in yolean/builder-qarkus
RUN set -e; \
  export QUARKUS_VERSION=$(cat pom.xml | grep '<quarkus.platform.version>' | sed 's/.*>\(.*\)<.*/\1/'); \
  echo "Quarkus version: $QUARKUS_VERSION"; \
  mkdir -p src/test/java/org; \
  echo 'package org; public class T { @org.junit.jupiter.api.Test public void t() { } }' > src/test/java/org/T.java; \
  mkdir -p src/main/resources/v1-schema; \
  echo 'type: object' > src/main/resources/v1-schema/Dummy.yaml; \
  mvn --batch-mode package; \
  mvn --batch-mode package -Pnative -Dquarkus.native.additional-build-args=--dry-run \
  || echo "= BUILD ERROR IS OK: Caching dependencies."; \
  rm -r src

COPY . .

ENTRYPOINT [ "mvn", "quarkus:dev" ]
CMD [ "-Dquarkus.http.host=0.0.0.0", "-Dquarkus.http.port=8080" ]

# The jar and the lib folder is required for the jvm target even when the native target is the end result
# Also we want to run the tests here, regardless of build target
RUN mvn --batch-mode package
# Produce the input to native builds, it's cheap until we actually run native-image
RUN mvn --batch-mode package -Pnative -Dquarkus.native.additional-build-args=--dry-run -Dmaven.test.skip=true \
  || echo "= BUILD ERROR IS OK: Producing native-image source jar."

# Defaults to native-image build because that's the default target
ARG build="native-image"
# JVM builds can use a dummy mvn target: --target=jvm --build-arg=build="validate"
# Native builds can be in-maven (+2GB mem): build="package -Pnative -Dmaven.test.skip=true"

RUN test "$build" = "native-image" || mvn --batch-mode $build

RUN test "$build" != "native-image" || ( \
  cd target/*-native-image-source-jar && \
  native-image $(curl -sL https://github.com/solsson/quarkus-graalvm-builds/raw/593699ab9795414fd8b992922dd4d9611c3184bf/rest-json-quickstart.txt | sed 's/__APP__/kafka-hook-1.0-SNAPSHOT/g') && \
  mv *-runner ../ \
)

FROM solsson/kafka:jre@sha256:9374540e6643ac577056e900872793cc4a96687025e3b492e93ad8d10c8e429b \
  as jvm

WORKDIR /app
COPY --from=dev /workspace/target/lib ./lib
COPY --from=dev /workspace/target/*-runner.jar ./app.jar

EXPOSE 8080
ENTRYPOINT [ "java", \
  "-Dquarkus.http.host=0.0.0.0", \
  "-Dquarkus.http.port=8080", \
  "-Djava.util.logging.manager=org.jboss.logmanager.LogManager", \
  "-cp", "./lib/*", \
  "-jar", "./app.jar" ]

FROM gcr.io/distroless/base-debian10:nonroot@sha256:f4a1b1083db512748a305a32ede1d517336c8b5bead1c06c6eac2d40dcaab6ad

COPY --from=dev \
  /lib/x86_64-linux-gnu/libz.so.* \
  /lib/x86_64-linux-gnu/

COPY --from=dev \
  /usr/lib/x86_64-linux-gnu/libzstd.so.* \
  /usr/lib/x86_64-linux-gnu/libsnappy.so.* \
  /usr/lib/x86_64-linux-gnu/liblz4.so.* \
  /usr/lib/x86_64-linux-gnu/

COPY --from=dev /workspace/target/*-runner /usr/local/bin/app

EXPOSE 8080
ENTRYPOINT ["app", "-Djava.util.logging.manager=org.jboss.logmanager.LogManager"]
CMD ["-Dquarkus.http.host=0.0.0.0", "-Dquarkus.http.port=8080"]
