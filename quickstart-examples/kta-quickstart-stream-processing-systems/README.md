# Quick Start: Stream Processing Systems

Files of different Stream Processing Systems for the Quick Start Guide.

- To apply code formatting to all sub-projects, use `./gradlew spotlessApplyAll` target.
- To build all sub-projects locally, use `./gradlew cleanAll buildAll`.
- To test if building the container images works, switch to the respective project directory and run

```bash
docker build -t <tag> -f docker/Dockerfile  .
```
