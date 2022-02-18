FROM --platform=$BUILDPLATFORM docker.io/yolean/builder-quarkus:8fcf5d01ecddc1253f87f352e02b7760603534e0@sha256:2230db63340d9768c63bae7fdb5662f1b5ffceb9d486e9a742e26102cd41664e \
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

FROM --platform=$TARGETPLATFORM docker.io/yolean/runtime-quarkus-ubuntu-jre:d091be226e9a62ee3cba9816cafedb8a06a17012@sha256:a4e85350a79341fe2216001ec500511066094ea0c387acb2f3627ca11951882c \
  as jvm

WORKDIR /app
COPY --from=dev /workspace/rest/target/quarkus-app /app

EXPOSE 8080
ENTRYPOINT [ "java", \
  "-Dquarkus.http.host=0.0.0.0", \
  "-Dquarkus.http.port=8080", \
  "-Djava.util.logging.manager=org.jboss.logmanager.LogManager", \
  "-jar", "quarkus-run.jar" ]

FROM --platform=$TARGETPLATFORM docker.io/yolean/runtime-quarkus-ubuntu:d091be226e9a62ee3cba9816cafedb8a06a17012@sha256:a4b4e77494c26720321b54a442b4806cdb59d426ae2266802bc5c8ed794dd2b6

COPY --from=dev /workspace/rest/target/*-runner /usr/local/bin/quarkus
