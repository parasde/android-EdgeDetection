cmake_minimum_required(VERSION 3.4.1)

set(pathPROJECT /Users/seungb/AndroidStudioProjects/EdgeDetection)
set(pathOPENCV /Users/seungb/AndroidOpenCV/OpenCV-android-sdk-4.5.5/sdk)
set(pathLIBOPENCV_JAVA /Users/seungb/AndroidStudioProjects/EdgeDetection/sdk/native/libs/${ANDROID_ABI}/libopencv_java4.so)

set(CMAKE_VERBOSE_MAKEFILE on)
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=gnu++11")

include_directories(${pathOPENCV}/native/jni/include)

add_library(camera SHARED ${pathPROJECT}/app/src/main/jni/camera.cpp)


add_library(lib_opencv SHARED IMPORTED)

set_target_properties(lib_opencv PROPERTIES IMPORTED_LOCATION ${pathLIBOPENCV_JAVA})
find_library(log-lib log)

target_link_libraries(camera lib_opencv ${log-lib})
