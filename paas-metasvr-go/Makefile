#!/bin/bash

PROJECTNAME=$(shell basename "$(PWD)")

# Go related variables.
GOBASE=$(shell pwd)
GOBIN=$(GOBASE)/bin
GOFILES=$(wildcard *.go)

# Make is verbose in Linux. Make it silent.
MAKEFLAGS += --silent

all: clean get build

clean:
	@echo "> cleaning build cache"
	@GOPATH=$(GOPATH) GOBIN=$(GOBIN) go clean
	@rm -rf $(GOBIN)/$(PROJECTNAME)

get:
	@echo "> checking if there is any missing dependencies..."
	@GOPATH=$(GOPATH) GOBIN=$(GOBIN) go get $(get)

build:
	@echo "> building binary..."
	@mkdir -p $(GOBIN)
	@GOPATH=$(GOPATH) GOBIN=$(GOBIN) go build -o $(GOBIN)/$(PROJECTNAME) $(GOFILES)

install:
	@echo "> install binary..."
	@GOPATH=$(GOPATH) GOBIN=$(GOBIN) go install $(PROJECTNAME)

