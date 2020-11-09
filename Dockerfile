FROM yolean/builder-quarkus:b0095ce7f26bf362a9adb8fe4670ad3c038ae932@sha256:c14ddaa7d79af07ef788cf87df5ea69a9494f7385fd9426659aca2ff84376196 \
  as dev

COPY --chown=nonroot:nogroup pom.xml .
COPY --chown=nonroot:nogroup lib/pom.xml lib/
COPY --chown=nonroot:nogroup rest/pom.xml rest/

RUN mkdir -p rest/target/
RUN cd lib && y-build-quarkus-cache

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

RUN test "$build" != "native-image" || mvn --batch-mode package -Pnative -Dmaven.test.skip=true

FROM yolean/java:f63772d02556021dbcb9f49fb9eff3d3dbe1b636@sha256:1bc5b3456a64fb70c85825682777c55a0999d9be56aca9bb1f507fe9b9171f83 \
  as jvm

WORKDIR /app
COPY --from=dev /workspace/rest/target/lib ./lib
COPY --from=dev /workspace/rest/target/*-runner.jar ./app.jar

EXPOSE 8080
ENTRYPOINT [ "java", \
  "-Dquarkus.http.host=0.0.0.0", \
  "-Dquarkus.http.port=8080", \
  "-Djava.util.logging.manager=org.jboss.logmanager.LogManager", \
  "-cp", "./lib/*", \
  "-jar", "./app.jar" ]

FROM yolean/runtime-quarkus:b0095ce7f26bf362a9adb8fe4670ad3c038ae932@sha256:34abf0213998f11fcca26b9a3313577d0cc0c442b7df3669d200d2dc10f6436c

COPY --from=dev /workspace/rest/target/*-runner /usr/local/bin/quarkus
