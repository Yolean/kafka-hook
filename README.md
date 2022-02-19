# Kafka hook

An incoming webhook endpoint that tries tries to never reject a message.
The sequel to [kafka-keyvalue](https://github.com/Yolean/kafka-keyvalue) in our need
for Kafka-related microservices components that do one thing and do it well.
To aid both happy and not-so-happy paths for integrations it captures most attributes of the incoming request alongside the payload.

## Decisions taken along the way of exploring this concept

 * Using Kafka record headers instead of an envelope makes sense when we want to save all payloads because we don't need to inspect them.
   - Manual inspection can use `kafkacat -J` and the result will be consumable through `jq`.
   - Or `kafkacat -f '%k: %h %s\n' etc.
   - Thus we need to flatten http headers fields into individual cloudevents extension keys
 * How do we, as an [extension](), prefix http headers?
   - ... and the prefix will be prefixed by `ce_`
   - We're probably predating (and/or obstructing) real extensions for this kind of thing, so let's not use `http` or `header`
   - Let's use something reasonable searchable:
     `hook_` because it's more readable than x-yolean- or yolean.se/whatever.
 * With a single topic name, configured at start, we can produce more reliably that with dynamic topic selection
   - TODO we could, and probably should, validate topic existence on start
 * How do we handle long header values?
   - We should probably always ignore `cookie`
     - can make this configurable
   - We could cap the length, and substring rather than ignore.
 * How do we handle large payloads?
   - TODO A configurable limit, that we set low
 * How do we handle headers with multiple values?
   - Use [JAX-RS](https://jax-rs.github.io/apidocs/2.1/javax/ws/rs/core/HttpHeaders.html#getHeaderString-java.lang.String-)'s concatenation (spoiler: it's commas)

## Pixy drop-in replacement, POST without dropped messages

At Yolean we use [kafka-pixy](https://github.com/mailgun/kafka-pixy) for many use cases with occasional, as opposed to constant, message production.
We never use pixy for consumption,
mainly because [kafka-keyvalue](https://github.com/Yolean/kafka-keyvalue) sidecars covers a wider use case for occasional consumption.

Happy paths work great with pixy,
but it has a habit of silently skipping message production for requests that don't meet assumptions on for example content-type or path.
To increase the confusion in such cases, HTTP responses contain no clues.

Kafka-hook is designed to do anything it can to forward the request to kafka,
and if it fails anyway it should be expected to log stack traces.
Also it tries to send an

Supported:
 - POST /topic/{ignored}/messages
   - As with all messages produced from this service,
     it's up to the consumer to check integrity and sanity before taking action.
 - `{"partition": , "offset": }` status 200 response body
   - With additional props, see [the Receipt schema](./src/main/resources/v1-schema/Receipt.yaml)
 - `{"error": }` status 500 response body
   - See [the HookError schema](./src/main/resources/v1-schema/HookError.yaml)

Not supported:
 - GET
 - POST /cluster/...
 - Pixy's command line arguments
   - We could probably use a Quarkus [main](https://quarkus.io/guides/lifecycle#the-main-method) method
 - `X-Kafka-` HTTP headers
   - They do get included with prefix in http headers thouhg
 - `sync` query parameter (kafka-hook is always sync=true)
 - `key` query parameter

## Building

```
GIT_STATUS=$(git status --untracked-files=no --porcelain=v2)
SOURCE_COMMIT=$(git rev-parse --verify HEAD)
[ -z "$GIT_STATUS" ] || SOURCE_COMMIT="$SOURCE_COMMIT-dirty"
docker buildx build --platform=linux/amd64,linux/arm64/v8 -t yolean/kafka-hook:$SOURCE_COMMIT-jvm --build-arg=build="validate" --target=jvm .
# On ARM build host such as OSX Docker for Mac
docker buildx build --platform=linux/arm64/v8 -t yolean/kafka-hook:$SOURCE_COMMIT-arm64 .
docker buildx build --platform=linux/arm64/v8 -t yolean/kafka-hook:$SOURCE_COMMIT-arm64 --push .
# For amd64 see nerdctl below
```

## Build using rootless Buildkit + nerdctl

```
GIT_STATUS=$(git status --untracked-files=no --porcelain=v2)
SOURCE_COMMIT=$(git rev-parse --verify HEAD)
[ -z "$GIT_STATUS" ] || SOURCE_COMMIT="$SOURCE_COMMIT-dirty"
nerdctl build --platform=linux/amd64,linux/arm64/v8 \
  -t yolean/kafka-hook:$SOURCE_COMMIT-jvm --build-arg=build="validate" --target=jvm .
nerdctl build --platform=linux/amd64 \
  -t yolean/kafka-hook:$SOURCE_COMMIT-amd64 .
nerdctl push --platform=linux/amd64,linux/arm64/v8 yolean/kafka-hook:$SOURCE_COMMIT-jvm
nerdctl push --platform=linux/amd64 yolean/kafka-hook:$SOURCE_COMMIT-amd64
```

## Combine to a multi-arch image

```
cat multiarch-native.Dockerfile | docker buildx build --platform=linux/amd64,linux/arm64/v8 \
  --build-arg=SOURCE_COMMIT="$SOURCE_COMMIT" -t yolean/kafka-hook:$SOURCE_COMMIT -
# Or
cat multiarch-native.Dockerfile | nerdctl build --platform=linux/amd64,linux/arm64/v8 \
  --build-arg=SOURCE_COMMIT="$SOURCE_COMMIT" -t yolean/kafka-hook:$SOURCE_COMMIT -
```

### Workaround for port collision

Note that buildkit defaults to running patforms in parallel,
which means that when Quarkus allocates a port during unit tests
one of the builds is likely to fail on port already in use.

Can [not](https://github.com/moby/buildkit/issues/1032#issuecomment-938253351) be done per build,
so try adding the following to `~/.config/buildkit/buildkitd.toml`:

````
[worker.oci]
  max-parallelism = 1
```
