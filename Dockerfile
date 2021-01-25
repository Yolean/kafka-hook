FROM yolean/builder-quarkus:b06240e1543be9ce1800744b07e6a975e3031496@sha256:b82a1f82745834bb0d19acef72707a3adcf92811dd694a0671dae7102aec8466 \
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

FROM yolean/java:b06240e1543be9ce1800744b07e6a975e3031496@sha256:4d61e888e9f4c58b4bfcbef45b2ecc3929b21b78ee01b1ae152a9c123ca2d3da \
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

FROM yolean/runtime-quarkus:b06240e1543be9ce1800744b07e6a975e3031496@sha256:c7e0c861e99058f8dccd948eed2bd228d8f54496565657fe6e3edcf107803627

COPY --from=dev /workspace/rest/target/*-runner /usr/local/bin/quarkus
