## Spring Boot 4 + gRPC

Demo project treating gRPC & it's associated protobuf files as first class citizens.

Using buf to generate server stubs and client code.

Aim is for this to:
1. provide a realistic example of something you'd see in the wild, going beyond the standard medium.com copy paste slop & 
2. Stay close to the bleeding edge

### Features
- Containerised via Jib using google Debian 13 + java 25 distroless base image 
- Protovalidate for message validation
- Buf for protobuf compilation

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
- Install buf ensuring that it is accessible via your path: https://docs.buf.build/installation