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

#mvn-cmd += LD_PRELOAD=/usr/lib64/clang/11/lib/libclang_rt.asan-x86_64.so
mvn-cmd += mvn

sources := $(shell find src/ -type f -name '*.java')
sources += $(shell find . -type f -name '*.xml')

all: test

test: $(sources)
> $(mvn-cmd) test

clean:
> mvn clean
.PHONY: clean

debug:
> $(mvn-cmd) -Dmaven.surefire.debug="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=*:5005 -Xnoagent -Djava.compiler=NONE" test
.PHONY: debug
