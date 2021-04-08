FROM yolean/builder-quarkus:907bcbc85d22a29d3243e2af97a0b09fba2ee4ce@sha256:91ef470b901eb6a0031f278f4a04d26ee1844f514b08826f5e7c16d661d8525d \
  as dev

COPY --chown=nonroot:nogroup pom.xml .
COPY --chown=nonroot:nogroup model/pom.xml model/
COPY --chown=nonroot:nogroup lib/pom.xml lib/
COPY --chown=nonroot:nogroup rest/pom.xml rest/

RUN mkdir -p lib/target rest/target/
RUN cd model && y-build-quarkus-cache

COPY --chown=nonroot:nogroup . .

# https://github.com/quarkusio/quarkus/blob/1.13.1.Final/extensions/kafka-client/deployment/src/main/java/io/quarkus/kafka/client/deployment/KafkaProcessor.java#L194
# https://github.com/quarkusio/quarkus/blob/1.13.1.Final/extensions/kafka-client/runtime/src/main/java/io/quarkus/kafka/client/runtime/KafkaRecorder.java#L23
RUN mkdir -p rest/src/main/resources/org/xerial/snappy/native/Linux/x86_64 \
  && cp -v /usr/lib/x86_64-linux-gnu/jni/libsnappyjava.so rest/src/main/resources/org/xerial/snappy/native/Linux/x86_64/libsnappyjava.so \
  && ldd -v rest/src/main/resources/org/xerial/snappy/native/Linux/x86_64/libsnappyjava.so

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

FROM yolean/java:907bcbc85d22a29d3243e2af97a0b09fba2ee4ce@sha256:63674354bd7f6f6660af89b483df98124c7d3062ce1e59a12ec012a47be769a3 \
  as jvm

WORKDIR /app
COPY --from=dev /workspace/rest/target/quarkus-app /app

EXPOSE 8080
ENTRYPOINT [ "java", \
  "-Dquarkus.http.host=0.0.0.0", \
  "-Dquarkus.http.port=8080", \
  "-Djava.util.logging.manager=org.jboss.logmanager.LogManager", \
  "-jar", "quarkus-run.jar" ]

FROM yolean/runtime-quarkus-ubuntu:907bcbc85d22a29d3243e2af97a0b09fba2ee4ce@sha256:d192704054b0eb6e089f379a77f83f90fb9ad5061e9748910ce48887766e8b81

COPY --from=dev /workspace/rest/target/*-runner /usr/local/bin/quarkus
