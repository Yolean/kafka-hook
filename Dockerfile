FROM solsson/kafka:2.5.0-graalvm@sha256:281c03203b9d0d822474de2878492f0b1c64ced229a2c52add1ce757fd6bf73e \
  as dev

WORKDIR /workspace
COPY pom.xml .

RUN set -e; \
  export QUARKUS_VERSION=$(cat pom.xml | grep '<quarkus.platform.version>' | sed 's/.*>\(.*\)<.*/\1/'); \
  echo "Quarkus version: $QUARKUS_VERSION"; \
  mkdir -p src/test/java/org; \
  echo 'package org; public class T { @org.junit.jupiter.api.Test public void t() { } }' > src/test/java/org/T.java; \
  mvn package -Pnative -Dquarkus.native.additional-build-args=--dry-run \
  || echo "= BUILD ERROR IS OK: Caching dependencies."; \
  rm -r src

COPY . .

ENTRYPOINT [ "mvn", "quarkus:dev" ]
CMD [ "-Dquarkus.http.host=0.0.0.0", "-Dquarkus.http.port=8090" ]

# The jar and the lib folder is required for the jvm target even when the native target is the end result
# Also we want to run the tests here, regardless of build target
#RUN mvn --batch-mode package
# TEMP
RUN mvn --batch-mode package -Dmaven.test.skip=true
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
  native-image $(curl -sL https://github.com/solsson/quarkus-graalvm-builds/raw/302596d2dc005a7c4b84e291d4c459772070a553/rest-json-quickstart.txt | sed 's/__APP__/kafka-hook-1.0-SNAPSHOT/g') && \
  mv *-runner ../ \
)

FROM solsson/kafka:2.5.0-jre@sha256:5d90c12f3ebae522daf35ed5f0bdcb845ee250b8f10da9c56f42da60800f975e \
  as jvm

WORKDIR /app
COPY --from=dev /workspace/target/lib ./lib
COPY --from=dev /workspace/target/*-runner.jar ./app.jar

EXPOSE 8090
ENTRYPOINT [ "java", \
  "-Dquarkus.http.host=0.0.0.0", \
  "-Dquarkus.http.port=8090", \
  "-Djava.util.logging.manager=org.jboss.logmanager.LogManager", \
  "-cp", "./lib/*", \
  "-jar", "./app.jar" ]

FROM gcr.io/distroless/base-debian10:nonroot@sha256:78f2372169e8d9c028da3856bce864749f2bb4bbe39c69c8960a6e40498f8a88

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
