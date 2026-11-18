## Spring Boot 4 + gRPC

Demo project treating gRPC & its associated protobuf files as first-class citizens.

Using buf to generate server stubs and client code.

Aim is for this to:
1. provide a realistic example of something you'd see in the wild, going beyond the standard medium.com copy and paste slop
2. Stay close to the bleeding edge

### Features
- Containerised via Jib using Google Debian 13 + java 25 distroless base image 
- Protovalidate for message validation
- Buf for protobuf compilation, driven by an in-repo Maven plugin ([buf-maven-plugin](src/tools/buf-maven-plugin))
- Remote proto definitions. Protos are pulled from a [separate git repository](https://github.com/BigMikeCodes/sb-grpc-protos)

## TODO
- [x] buf server stub generation
- [x] Proto Validate server side validation
- [x] Containerisation
- [ ] CI pipeline
- [x] Integration tests with Testcontainers
- [ ] Spring Security
- [ ] Front end client (Flutter app?)
- [ ] Deploy somewhere (GCP Cloud Run?)
- [ ] Expand tests for each type of gRPC method (unary, server streaming, client streaming, bidirectional streaming)
- [ ] Publish the container to a registry (Docker Hub? GCP Artifact Registry?)

### Setup
- No need to install buf: the buf binary is fetched from Maven Central by `buf-maven-plugin`. The version is set by `buf.version` in `src/be/grpc-server/pom.xml`
- Build from the repo root so the plugin is built first: `mvn -pl src/be/grpc-server -am verify`
  - Run at least `compile`: Maven can't use a plugin built in the same reactor before then, so `mvn generate-sources` on its own fails
  - Or run `mvn -pl src/tools/buf-maven-plugin install` once, then build from `src/be/grpc-server` as before