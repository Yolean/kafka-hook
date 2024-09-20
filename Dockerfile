FROM --platform=$TARGETPLATFORM ghcr.io/yolean/builder-quarkus:0ab2411c78f7bb4d2daebbf3fc1f270af3776578@sha256:4992c2ad9ba58ea600e450d1eaa0b17df756407942590159814b181a1307a5f0 \
  as jnilib

# https://github.com/xerial/snappy-java/blob/master/src/main/java/org/xerial/snappy/OSInfo.java#L113
RUN set -ex; \
  curl -o snappy.jar -sLSf https://repo1.maven.org/maven2/org/xerial/snappy/snappy-java/1.1.9.0/snappy-java-1.1.9.0.jar; \
  LIBPATH=$(java -cp snappy.jar org.xerial.snappy.OSInfo); \
  ARCH=$(java -cp snappy.jar org.xerial.snappy.OSInfo --arch); \
  mkdir -pv native/$LIBPATH; \
  cp -v /usr/lib/$ARCH-linux-gnu/jni/* native/$LIBPATH/

FROM --platform=$TARGETPLATFORM ghcr.io/yolean/builder-quarkus:0ab2411c78f7bb4d2daebbf3fc1f270af3776578@sha256:4992c2ad9ba58ea600e450d1eaa0b17df756407942590159814b181a1307a5f0 \
  as dev

COPY --chown=nonroot:nogroup pom.xml .
COPY --chown=nonroot:nogroup model/pom.xml model/
COPY --chown=nonroot:nogroup lib/pom.xml lib/
COPY --chown=nonroot:nogroup rest/pom.xml rest/

RUN mkdir -p lib/target rest/target/
RUN cd model && y-build-quarkus-cache

COPY --chown=nonroot:nogroup . .

# https://github.com/quarkusio/quarkus/blob/1.13.1.Final/extensions/kafka-client/deployment/src/main/java/io/quarkus/kafka/client/deployment/KafkaProcessor.java#L194
# https://github.com/quarkusio/quarkus/blob/2.7.1.Final/extensions/kafka-client/deployment/src/main/java/io/quarkus/kafka/client/deployment/KafkaProcessor.java#L268
# https://github.com/quarkusio/quarkus/blob/1.13.1.Final/extensions/kafka-client/runtime/src/main/java/io/quarkus/kafka/client/runtime/KafkaRecorder.java#L23
# https://github.com/quarkusio/quarkus/blob/2.7.1.Final/extensions/kafka-client/runtime/src/main/java/io/quarkus/kafka/client/runtime/KafkaRecorder.java#L26
# TODO check that for "$build" == "native-image" TARGETPLATFORM == BUILDPLATFORM
COPY --from=jnilib /workspace/native/Linux rest/src/main/resources/org/xerial/snappy/native/Linux
# TODO need to verify?
#RUN ldd -v rest/src/main/resources/org/xerial/snappy/native/Linux/x86_64/libsnappyjava.so

ENTRYPOINT [ "mvn", "compile", "quarkus:dev" ]
CMD [ "-Dquarkus.http.host=0.0.0.0" ]

# The jar and the lib folder is required for the jvm target even when the native target is the end result
# MUST be followed by a real build, or we risk pushing images despite test failures
RUN mvn package -Dmaven.test.skip=true

# For a regular JRE image run: docker build --build-arg build="package" --target=jvm
ARG build="package -Pnative"

RUN mvn --batch-mode $build

FROM --platform=$TARGETPLATFORM ghcr.io/yolean/runtime-quarkus-ubuntu-jre:0ab2411c78f7bb4d2daebbf3fc1f270af3776578@sha256:ce1ae9f9993ac0ff05100276dc0413a587c1db042c9337b31a9194fa493c09c0 \
  as jvm

WORKDIR /app
COPY --from=dev /workspace/rest/target/quarkus-app /app

EXPOSE 8080
ENTRYPOINT [ "java", \
  "-Dquarkus.http.host=0.0.0.0", \
  "-Dquarkus.http.port=8080", \
  "-Djava.util.logging.manager=org.jboss.logmanager.LogManager", \
  "-jar", "quarkus-run.jar" ]

ENV SOURCE_COMMIT=${SOURCE_COMMIT} SOURCE_BRANCH=${SOURCE_BRANCH} IMAGE_NAME=${IMAGE_NAME}

FROM --platform=$TARGETPLATFORM ghcr.io/yolean/runtime-quarkus-ubuntu:0ab2411c78f7bb4d2daebbf3fc1f270af3776578@sha256:70bbcf56fc7061f6bf97fda8761dde75ef0a52cf07452da20f66c357cd41cfac

COPY --from=dev /workspace/rest/target/*-runner /usr/local/bin/quarkus
