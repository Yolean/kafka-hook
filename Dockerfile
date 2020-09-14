FROM yolean/builder-quarkus:6e76f75eb32a56bbb9fef78ab92e621e30ae9ab1@sha256:c0f12ccb889e7270817a25df947a7e8e1f0ecd00cbe761771ffa0376fc8b4033 \
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

RUN test "$build" != "native-image" || ( \
  cd rest/target/*-native-image-source-jar && \
  native-image $(curl -sL https://github.com/solsson/quarkus-graalvm-builds/raw/593699ab9795414fd8b992922dd4d9611c3184bf/rest-json-quickstart.txt | sed 's/__APP__/kafka-hook-rest-1.0-SNAPSHOT/g') && \
  mv *-runner ../ \
)

FROM yolean/java:6e76f75eb32a56bbb9fef78ab92e621e30ae9ab1@sha256:3838b874d68be7e466aa2e5c17b3be649b4aa9554652ab4540ff254543ec328a \
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

FROM yolean/runtime-quarkus:6e76f75eb32a56bbb9fef78ab92e621e30ae9ab1@sha256:6b7907fae51dff29a23299fa274bdbb9b5c611b4b59ae8e2a4e987a9b0d18d09

COPY --from=dev /workspace/rest/target/*-runner /usr/local/bin/quarkus
