FROM yolean/builder-quarkus:3a9207474eea4e269b2dc214fa7d4d7c6a3b2481@sha256:21264cb6c62944f2ddc38818b0907cc0c854fe5c43004c3d4bceb188a86ebcce \
  as dev

COPY pom.xml .
RUN y-build-quarkus-cache

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
