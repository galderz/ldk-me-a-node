SHELL := bash
.ONESHELL:
.SHELLFLAGS := -eu -o pipefail -c
.DELETE_ON_ERROR:
MAKEFLAGS += --warn-undefined-variables
MAKEFLAGS += --no-builtin-rules

ifeq ($(origin .RECIPEPREFIX), undefined)
  $(error This Make does not support .RECIPEPREFIX. Please use GNU Make 4.0 or later)
endif
.RECIPEPREFIX = >

mvn += LD_PRELOAD=/usr/lib64/clang/13/lib/libclang_rt.asan-x86_64.so
mvn += ASAN_OPTIONS=detect_leaks=0
mvn += JAVA_HOME=/opt/java-11
mvn += /opt/maven/bin/mvn

sources := $(shell find src/ -type f -name '*.java')
sources += $(shell find . -type f -name '*.xml')

all: test

test: $(sources)
> $(mvn) test

clean:
> mvn clean
.PHONY: clean

debug:
> $(mvn-cmd) -Dmaven.surefire.debug="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=*:5005 -Xnoagent -Djava.compiler=NONE" test
.PHONY: debug

# Building LDK Java bindings
#
# If any issues, check:
# https://github.com/lightningdevkit/ldk-garbagecollected/blob/main/.github/workflows/build.yml
#
# It Requires cbindgen, e.g.
# $ cargo install cbindgen

LDK_BIND_GC_HOME := /opt/ldk-garbagecollected
LDK_BIND_C_HOME := /opt/ldk-c-bindings
RUST_LN_HOME := /opt/matt-rust-lightning

ldk_jni_so = $(LDK_GC_HOME)/liblightningjni.so

$(LDK_GC_HOME):
> cd /opt
> git clone https://github.com/lightningdevkit/ldk-garbagecollected

$(RUST_LDK):
> cd /opt
> git clone https://git.bitcoin.ninja/rust-lightning matt-rust-lightning

$(LDK_BIND_C_HOME):
> cd /opt
> git clone https://github.com/lightningdevkit/ldk-c-bindings

update-bindings: update-rust-ln update-ldk-bind-c update-ldk-bind-gc build-bindings
.PHONY: update-bindings

build-bindings: build-ldk-bind-c build-ldk-bind-gc
.PHONY: build-bindings

build-ldk-bind-gc:
> cd $(LDK_BIND_GC_HOME)
> ./genbindings.sh $(LDK_BIND_C_HOME) "-I/opt/java-11/include/ -I/opt/java-11/include/linux/" true false || true
> $(mvn) -DskipTests install
.PHONY: build-ldk-bind-gc

build-ldk-bind-c:
> cd $(LDK_BIND_C_HOME)
> ./genbindings.sh $(RUST_LN_HOME) true
.PHONY: build-ldk-bind-c

update-rust-ln: $(RUST_LN_HOME)
> cd $(RUST_LN_HOME)
> git fetch origin
> git checkout origin/2021-03-java-bindings-base
.PHONY: update-rust-ln

update-ldk-bind-c: $(LDK_BIND_C_HOME)
> cd $(LDK_BIND_C_HOME)
> git fetch --all --tags -f
> git checkout v0.0.103.1
.PHONY: update-ldk-bind-c

update-ldk-bind-gc: $(LDK_BIND_GC_HOME)
> cd $(LDK_BIND_GC_HOME)
> git fetch --all --tags -f
> git checkout v0.0.103.1
.PHONY: update-ldk-bind-gc

reset:
> cd $(LDK_BIND_C_HOME)
> git checkout -f
> git reset --hard HEAD
> git clean -f -d
> cd $(LDK_BIND_GC_HOME)
> git checkout -f
> git reset --hard HEAD
> git clean -f -d
.PHONY: reset
