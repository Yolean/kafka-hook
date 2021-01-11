FROM yolean/builder-quarkus:f7f5b1c5790e0c755e85bbc00d576875d4fa7bf5@sha256:d6bffbecb6c5ba48029206a39c1ee9292a4911b12283517b12cb64718cf0b871 \
  as dev

COPY --chown=nonroot:nogroup pom.xml .
COPY --chown=nonroot:nogroup model/pom.xml model/
COPY --chown=nonroot:nogroup lib/pom.xml lib/
COPY --chown=nonroot:nogroup rest/pom.xml rest/

RUN mkdir -p lib/target rest/target/
RUN cd model && y-build-quarkus-cache

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

FROM yolean/java:f7f5b1c5790e0c755e85bbc00d576875d4fa7bf5@sha256:ed802e9d138b0e0059698d01d930bda65241d70c286590667922a532552cc694 \
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

FROM yolean/runtime-quarkus:f7f5b1c5790e0c755e85bbc00d576875d4fa7bf5@sha256:a75181747faceeaab2f360d9c19af06be79943182b8fd8666b1a7ef716ef3b47

COPY --from=dev /workspace/rest/target/*-runner /usr/local/bin/quarkus
