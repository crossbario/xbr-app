# input .fbs files for schema
FBS_SCHEMA_FILES=./schema/*.fbs

# output directories for generated bindings
FBS_PYTHON_OUTPUT=/tmp/test/python
FBS_BFBS_OUTPUT=/tmp/test/bfbs
FBS_JAVA_OUTPUT=/tmp/test/java

#FLATC=${HOME}/scm/3rdparty/flatbuffers/flatc
FLATC=/usr/local/bin/flatc

flatc_version:
	$(FLATC) --version

clean_fbs:
	rm -rf $(FBS_BFBS_OUTPUT)
	rm -rf $(FBS_PYTHON_OUTPUT)
	rm -rf $(FBS_JAVA_OUTPUT)
	mkdir -p $(FBS_BFBS_OUTPUT)
	mkdir -p $(FBS_PYTHON_OUTPUT)
	mkdir -p $(FBS_JAVA_OUTPUT)

build_fbs: build_fbs_bfbs build_fbs_python build_fbs_java

# generate schema type library (.bfbs binary) from schema files
build_fbs_bfbs:
	$(FLATC) -o $(FBS_BFBS_OUTPUT) --binary --schema --bfbs-comments --bfbs-builtins $(FBS_SCHEMA_FILES)

# generate python3 bindings from schema files
build_fbs_python:
	$(FLATC) -o $(FBS_PYTHON_OUTPUT) --python $(FBS_SCHEMA_FILES)

	# those are not generated, but required
	touch $(FBS_PYTHON_OUTPUT)/__init__.py

	# FIXME: wrong import:
	# "from .oid_t import oid_t" => "from ..oid_t import oid_t"
	# "from .ObjRef import ObjRef" => "from ..ObjRef import ObjRef"
	find $(FBS_PYTHON_OUTPUT) -name "*.py" -exec sed -i'' 's/from .oid_t/from ..oid_t/g' {} \;

# generate java bindings from schema files
build_fbs_java:
	$(FLATC) -o $(FBS_JAVA_OUTPUT) --java $(FBS_SCHEMA_FILES)
