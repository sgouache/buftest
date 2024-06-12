# Minimum CMake required
cmake_minimum_required(VERSION 3.21)

# Project
project(protobuf-examples)

# Abseil requires C++14
#set(CMAKE_CXX_STANDARD 14)
# We use std::bytes
set(CMAKE_CXX_STANDARD 17)

include_directories(include)
set(CMAKE_CXX_STANDARD_REQUIRED on)
# set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -Wall -Wextra -Werror")

include(FindProtobuf)
find_package(Protobuf CONFIG REQUIRED)

# Import Abseil's CMake targets
find_package(absl CONFIG REQUIRED)
find_package(utf8_range REQUIRED CONFIG)

include_directories(${Protobuf_INCLUDE_DIRS})

set(PROTOBUF_DIRS khiops/v101)


#add_executable(main test.cc proto/${PROTOBUF_DIRS}/calls.proto proto/${PROTOBUF_DIRS}/basics.proto)
add_executable(test_proto test.cc gen/cpp/${PROTOBUF_DIRS}/calls.pb.cc gen/cpp/${PROTOBUF_DIRS}/basics.pb.cc)

# to find *.bp.h files
include_directories(gen/cpp/)
#include_directories(gen/cpp/proto)

# file(MAKE_DIRECTORY gen/cpp/${PROTOBUF_DIRS})

# protobuf_generate(
#   TARGET main
#   IMPORT_DIRS proto proto/${PROTOBUF_DIRS}
#   PROTOC_OUT_DIR gen/cpp/${PROTOBUF_DIRS}
#   #PROTOC_OUT_DIR gen/cpp
#   #PROTOS proto/${PROTOBUF_DIRS}/basics.proto proto/${PROTOBUF_DIRS}/calls.proto
#   #PROTOS ${PROTOBUF_DIRS}/calls.proto
#   APPEND_PATH
# )

set(protobuf_ABSL_USED_TARGETS
absl::absl_check
absl::absl_log
absl::algorithm
absl::base
absl::bind_front
absl::bits
absl::btree
absl::cleanup
absl::cord
absl::core_headers
absl::debugging
absl::die_if_null
absl::dynamic_annotations
absl::flags
absl::flat_hash_map
absl::flat_hash_set
absl::function_ref
absl::hash
absl::if_constexpr
absl::layout
absl::log_initialize
absl::log_globals
absl::log_severity
absl::memory
absl::node_hash_map
absl::node_hash_set
absl::optional
absl::random_distributions
absl::random_random
absl::span
absl::status
absl::statusor
absl::strings
absl::synchronization
absl::time
absl::type_traits
absl::utility
absl::variant
)

target_link_libraries(test_proto PUBLIC
  protobuf::libprotobuf
  ${protobuf_ABSL_USED_TARGETS}
  utf8_range::utf8_validity
)# PRIVATE absl_log_internal_message absl_log_internal_check_op)