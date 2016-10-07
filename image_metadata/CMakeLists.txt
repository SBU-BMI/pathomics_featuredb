cmake_minimum_required(VERSION 2.8)
if(COMMAND cmake_policy)
    cmake_policy(SET CMP0003 NEW)
endif(COMMAND cmake_policy)

set(projName "tcga_svs_img_cvs")
project(${projName})

set(CMAKE_C_FLAGS "-std=c99")

set(CMAKE_MODULE_PATH ${CMAKE_MODULE_PATH} "${CMAKE_SOURCE_DIR}/cmake/")

################################################################################
# find OpenSlide path
find_path(OpenSlide_INSTALLATION_PATH include/openslide/openslide.h REQUIRED)
if(NOT OpenSlide_INSTALLATION_PATH)
   message(FATAL_ERROR "Please enter the installation path of OpenSlide, i.e., the one after --prefix= when configuring OpenSlide")
endif(NOT OpenSlide_INSTALLATION_PATH)

include_directories(${OpenSlide_INSTALLATION_PATH}/include/openslide/)

set(OpenSlideLibs
  ${OpenSlide_INSTALLATION_PATH}/lib/libopenslide.so
  )

################################################################################
# put all libs together
set(Libraries
    ${OpenSlideLibs}
    ${Libraries}
    )

################################################################################
# include dir
include_directories("${PROJECT_SOURCE_DIR}")
include_directories("${PROJECT_BINARY_DIR}")


add_executable(tcga_svs_image_csv tcga_svs_image_csv.c)
target_link_libraries(tcga_svs_image_csv ${Libraries})

add_executable(quip_image_metadata quip_image_metadata.c)
target_link_libraries(quip_image_metadata ${Libraries})

add_executable(validate_image_file validate_image_file.c)
target_link_libraries(validate_image_file ${Libraries})
