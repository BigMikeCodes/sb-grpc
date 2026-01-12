## Spring Boot 4 + gRPC

Demo project treating gRPC & it's associated protobuf files as first class citizens.

Using buf to generate server stubs and client code.

Aim is for this to provide a realistic example of something you'd see in the wild, going beyond the standard medium.com copy paste slop.

### Features
- Containerised via Jib using google Debian 13 + java 25 distroless base image 
- Protovalidate for message validation
- Buf for protobuf compilation

## TODO
- [x] buf server stub generation
- [x] Proto Validate server side validation
- [x] Containerisation
- [ ] CI pipeline
- [ ] Tests
- [ ] Spring Security
- [ ] Front end client (Flutter app?)
- [ ] Deploy somewhere (GCP Cloud Run?)

### Setup
- Install buf ensuring that it is accessible via your path: https://docs.buf.build/installation