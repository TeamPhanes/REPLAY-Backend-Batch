REGISTRY := harbor.phanescloud.com

GIT_SHA ?= $(shell git rev-parse --short HEAD)

IMAGE_NAME := batch
IMAGE_PROJECT := replay
IMAGE_TAG := $(BRANCH)-$(GIT_SHA)
IMAGE := $(REGISTRY)/$(IMAGE_PROJECT)/$(IMAGE_NAME):$(IMAGE_TAG)

build: decrypt
	./gradlew clean
	./gradlew build

docker-build: decrypt
	docker build --network host -t ${IMAGE} .

docker-push: docker-build
	docker push ${IMAGE}

decrypt:
	@if grep -q "sops:" src/main/resources/application.yaml; then \
		sops -d -i src/main/resources/application.yaml; \
	fi
	@if grep -q "sops:" src/main/resources/application-local.yaml; then \
		sops -d -i src/main/resources/application-local.yaml; \
	fi
	@if grep -q "sops:" src/main/resources/application-dev.yaml; then \
		sops -d -i src/main/resources/application-dev.yaml; \
	fi

encrypt:
	sops -e -i src/main/resources/application.yaml
	sops -e -i src/main/resources/application-local.yaml
	sops -e -i src/main/resources/application-dev.yaml