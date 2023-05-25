
# --- Configurable setting
releaseMode=release-fast

# --- Runtime / layers
IMAGE_NAME=native-lambda-runtime:latest
runtimeContainer := "$(shell docker create $(IMAGE_NAME))"
build-ScalaNativeRuntime:
	docker build -t $(IMAGE_NAME) layers/runtime/
	docker cp $(runtimeContainer):/build/runtime $(ARTIFACTS_DIR)/sn-runtime
	docker rm -v $(runtimeContainer)

# --- Handlers
build-HelloWorld: 
	$(MAKE) handlerSubDir=hello mainClass=hello.HelloWorld build-handler

build-HelloWorldJVM: 
	$(MAKE) handlerSubDir=hello build-handler-jvm

build-TapirExample:
	$(MAKE) handlerSubDir=http mainClass=TapirExample build-handler

build-TapirExampleJVM:
	$(MAKE) handlerSubDir=http build-handler-jvm

build-RequestUnicorn:
	smithy4s generate --dependencies com.disneystreaming.smithy:aws-dynamodb-spec:2023.02.10 -o ./handlers/wildrides
	ls ./handlers/wildrides/
	$(MAKE) handlerSubDir=wildrides mainClass=wildrides.RequestUnicorn build-handler

# --- Internals
TARGET_FLAGS=--target=x86_64-unknown-linux-gnu
NATIVE_FLAGS=--native \
		--native-mode=$(releaseMode) \
		--native-lto=thin \
		--native-compile=$(TARGET_FLAGS)" \
		--native-linking=$(TARGET_FLAGS)"
SRC_PATH=/build/main
HANDLERS_PATH=$(SRC_PATH)/handlers
RUNTIME_PATH=$(SRC_PATH)/runtime
OUTPUT_DIR="/build/output/"
CLI_PACKAGE = \
	cp /build/runtime/bootstrap $(OUTPUT_DIR) && \
	scala-cli --power package \
		--platform=native \
		--output=$(OUTPUT_DIR)/lambdaHandler \
		--force \
		${NATIVE_FLAGS} \
		$(RUNTIME_PATH) $(HANDLERS_PATH)/*.scala

build-handler:
	docker run -it \
		-v $$(pwd):$(SRC_PATH) \
		-v $(ARTIFACTS_DIR):$(OUTPUT_DIR) \
		-v ~/.ivy2:/root/.ivy2 \
		-v ~/.cache/coursier:/root/.cache/coursier \
		${IMAGE_NAME} \
		sh -c '${CLI_PACKAGE} --main-class=$(mainClass) $(HANDLERS_PATH)/$(handlerSubDir) '

build-handler-jvm:
		mkdir -p $(ARTIFACTS_DIR)/lib/
		scala-cli --power \
		  package --assembly --preamble=false \
			--output=$(ARTIFACTS_DIR)/lib/LambdaHandler.jar \
			--force \
			--platform=jvm \
			runtime/ handlers/*.scala handlers/$(handlerSubDir) 

