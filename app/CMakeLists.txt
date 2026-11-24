# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.

cmake_minimum_required(VERSION 3.22.1)
project(APlayerNative LANGUAGES C CXX)

# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.
# You can define multiple libraries, and CMake builds them for you.
# Gradle automatically packages shared libraries with your APK.

add_library( # Sets the name of the library.
             blur

             # Sets the library as a shared library.
             SHARED

             # Provides a relative path to your source file(s).
             src/main/cpp/blur.cpp )

# Searches for a specified prebuilt library and stores the path as a
# variable. Because CMake includes system libraries in the search path by
# default, you only need to specify the name of the public NDK library
# you want to add. CMake verifies that the library exists before
# completing its build.

find_library( # Sets the name of the path variable.
              log-lib

              # Specifies the name of the NDK library that
              # you want CMake to locate.
              log )

# Specifies libraries CMake should link to your target library. You
# can link multiple libraries, such as libraries you define in this
# build script, prebuilt third-party libraries, or system libraries.


target_link_libraries( # Specifies the target library.
                       blur
                       -ljnigraphics

                       # Links the target library to the log library
                       # included in the NDK.
                       ${log-lib}
                       "-Wl,--build-id=none")

# NDK clang can embed host-specific compiler-build metadata in the ELF
# .comment section (for example, different BOLT/MLGO settings between
# macOS and Linux host toolchains). The section is not used at runtime,
# so remove it to keep libblur.so byte-for-byte reproducible.
add_custom_command(TARGET blur POST_BUILD
        COMMAND ${CMAKE_OBJCOPY}
        --remove-section .comment
        $<TARGET_FILE:blur>
        VERBATIM)

# SMB2/SMB3 client; compiled from the pinned upstream Git submodule.
set(LIBSMB2_ROOT "${CMAKE_CURRENT_SOURCE_DIR}/../third-party/libsmb2")
if(NOT EXISTS "${LIBSMB2_ROOT}/CMakeLists.txt")
  message(FATAL_ERROR "libsmb2 submodule is missing. Run: git submodule update --init --recursive")
endif()

# Keep the same minimized feature set used for the size measurement.
set(BUILD_SHARED_LIBS ON CACHE BOOL "" FORCE)
set(ENABLE_EXAMPLES OFF CACHE BOOL "" FORCE)
set(ENABLE_UTILS OFF CACHE BOOL "" FORCE)
set(ENABLE_LIBDCERPC OFF CACHE BOOL "" FORCE)
set(ENABLE_LIBKRB5 OFF CACHE BOOL "" FORCE)
set(ENABLE_GSSAPI OFF CACHE BOOL "" FORCE)
set(ENABLE_OPENSSL OFF CACHE BOOL "" FORCE)
add_subdirectory("${LIBSMB2_ROOT}" "${CMAKE_CURRENT_BINARY_DIR}/third-party-libsmb2" EXCLUDE_FROM_ALL)
# Android must load libsmb2 by a plain SONAME, not a host build path.
# Clear version properties rather than setting them to empty strings (which
# produces "libsmb2.so." on CMake/ELF toolchains).
set_property(TARGET smb2 PROPERTY VERSION)
set_property(TARGET smb2 PROPERTY SOVERSION)
set_target_properties(smb2 PROPERTIES NO_SONAME FALSE)

add_library(aplayer_smb SHARED src/main/cpp/smb/SmbJni.cpp)
target_compile_features(aplayer_smb PRIVATE cxx_std_17)
target_compile_options(aplayer_smb PRIVATE -Os -fvisibility=hidden -ffunction-sections -fdata-sections)
target_link_options(aplayer_smb PRIVATE -Wl,--gc-sections)
target_link_libraries(aplayer_smb PRIVATE smb2 log)
