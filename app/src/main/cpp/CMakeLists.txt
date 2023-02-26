cmake_minimum_required(VERSION 3.22.1)

project("softwareanc")

add_library(softwareanc SHARED softwareanc.cpp)

find_library(log-lib log)

set(OBOE_DIR "C:\\D\\oboe\\oboe")
add_subdirectory(${OBOE_DIR} ./oboe)
include_directories(${OBOE_DIR}/include)

target_link_libraries(softwareanc ${log-lib} oboe)