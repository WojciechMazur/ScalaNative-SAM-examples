## Building
Use provided SAM (AWS Serveless Application Model) template for building the app:
```bash
# Build all layers and functions
sam build 
# Build single function
sam build HelloWorld 
```
SAM would use `Makefile` to build each of the components. To ensure that AWS environment would contain all required libraries, handlers are being build inside a docker container based on Amazon Linux distribution using `layers/runtime/Dockerfile`. The first build might take long time, becoue building `openssl/openssl`, `cmake`, `aws/s2n-tls` from sources (might not be required anymore). All the required libs would be attached to the Lambda Handlers via `ScalaNativeRuntime` layer containing all the required dynamic libraries and bootstrap script. 

All Native handlers/layers are cached. Becouse of reusing the sam `CodeUri` properties for Native and JVM versions of the handlers, the JVM handlers are never cached (SAM limitation)

### Tapir examples
The Tapir examples require locally published version of `https://github.com/softwaremill/tapir/pull/2906`. If needed update the required version of `tapir-aws-lambda` in `handlers/config.scala`

## Testing
Testing execution locally: `sam local invoke` would start local lambda service and would execute once a given handler, pass the event body using `-e <event.json>`
```bash
sam local invoke HelloWorldJVM -e events/helloWorld.json 
sam local invoke HelloWorld    -e events/helloWorld.json

sam local invoke TapirExampleJVM -e events/httpExample.json
sam local invoke TapirExample    -e events/httpExample.json

# Use DynamoDB: requires credentials
sam local invoke RequestUnicorn -e events/requestUnicorn.json
```

Testing API: `sam local start-api` would create a lambda service allowing to test performance of warmed up instance, allowing to process more then 1 event and integrate with local app deployments. It would start `TapirExample` handler and expose it's api at `http://127.0.0.1:3000/api/hello/`
```bash
sam local start-api
```