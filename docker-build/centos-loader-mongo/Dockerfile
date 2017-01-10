FROM centos:latest
MAINTAINER Tahsin Kurc 

RUN yum -y update && yum -y install \
	build-essential \
	curl \
	git \
	ant \
	ncurses-devel \
	vim \
	gtk2-devel \
	pkgconfig \
	python-devel \
	numpy \
	tbb \
	tbb-devel \
	jasper-devel \
	zip \
	unzip \
	prelink \
	epel-release \
	wget

RUN yum -y group install "Development Tools"

RUN yum -y install libxml2-devel 
RUN yum -y install sqlite-devel 
RUN yum -y install cmake*
RUN yum -y install java-1.8.0-openjdk java-1.8.0-openjdk-devel

# Install libraries
WORKDIR /tmp/

# Install libraries needed for openslide 3.4.1
RUN curl -O -J -L http://downloads.sourceforge.net/lcms/lcms2-2.7.tar.gz && \
	tar -xzvf lcms2-2.7.tar.gz && \
	cd lcms2-2.7 && ./configure && \
	make -j4 && make install && \
	cd .. && rm -rf lcms2-2.7* 

RUN curl -O -J -L http://downloads.sourceforge.net/libpng/libpng-1.6.22.tar.xz && \
	tar -xvf libpng-1.6.22.tar.xz && \
	cd libpng-1.6.22 && ./configure && \
	make -j4 && make install && \
	cd .. && rm -rf libpng-1.6.22*

RUN curl -O -J -L http://download.osgeo.org/libtiff/tiff-4.0.6.tar.gz && \
	tar -xzvf tiff-4.0.6.tar.gz && \
	cd tiff-4.0.6 && ./configure && \
	make -j4 && make install && \
	cd .. && rm -rf tiff-4.0.6* 

RUN curl -O -J -L http://downloads.sourceforge.net/openjpeg.mirror/openjpeg-2.1.0.tar.gz && \
	tar -xzvf openjpeg-2.1.0.tar.gz && \
	cd openjpeg-2.1.0 && mkdir build && \
	cd build && cmake3 ../ && \
	make -j4 && make install && \
	cd ../.. && rm -rf openjpeg-2.1.0*

RUN yum -y install cairo-devel gdk-pixbuf2-devel

ENV PKG_CONFIG_PATH=/usr/local/lib/pkgconfig/

# Install openslide 3.4.1
RUN curl -O -J -L https://github.com/openslide/openslide/releases/download/v3.4.1/openslide-3.4.1.tar.gz && \
	tar -xzvf openslide-3.4.1.tar.gz && \
	cd openslide-3.4.1 && \
	./configure && \
	make -j4 && \
	make install && \
	cd .. && \
	rm -rf openslide-3.4.1*

# Install OpenCV 3.1.0 
RUN curl -O -J -L https://github.com/Itseez/opencv/archive/3.1.0.zip && \
	unzip opencv-3.1.0.zip && \
	mkdir /tmp/opencv-build && \
	cd /tmp/opencv-build && \
	cmake -D CMAKE_BUILD_TYPE=RELEASE -D BUILD_SHARED_LIBS=OFF -D BUILD_TESTS=OFF -D CMAKE_INSTALL_PREFIX=/usr/local ../opencv-3.1.0 && \
	make -j4 && \
	make install && \
	cd /tmp && \
	rm -rf opencv-build && \
	rm -rf opencv-3.1.0* 

# Install MongoDB 3.2.6
RUN curl -O -J -L https://fastdl.mongodb.org/linux/mongodb-linux-x86_64-rhel70-3.2.6.tgz && \
	tar -xzvf mongodb-linux-x86_64-rhel70-3.2.6.tgz && \
	cp mongodb-linux-x86_64-rhel70-3.2.6/bin/* /usr/local/bin && \
	rm -rf mongodb-linux-x86_64-rhel70-3.2.6*

# Install gradle
RUN curl -O -J -L https://services.gradle.org/distributions/gradle-2.13-bin.zip && \
	unzip gradle-2.13-bin.zip && \
	mv gradle-2.13 /usr/local && \
	rm -rf gradle-2.13* 

ENV GRADLE_HOME=/usr/local/gradle-2.13
ENV PATH=$GRADLE_HOME/bin:$PATH
ENV OPENCV_JAVA_DIR=/usr/local/share/OpenCV/java
ENV OPENCV_JAVA_LIB=$OPENCV_JAVA_DIR/opencv-310.jar
ENV FEATUREDB_DIR=/usr/local/pathomics_featuredb

# Install Java loader for FeatureDB
WORKDIR /tmp/
RUN git clone https://github.com/SBU-BMI/pathomics_featuredb && \
	cd pathomics_featuredb/src && \
	gradle build && \
	cd ../image_metadata && \
	mkdir build && cd build && \
	cmake ../ && make -j4 && \
	cd /tmp && mv pathomics_featuredb /usr/local/. && \
	cp $FEATUREDB_DIR/docker_scripts/run* /usr/local/bin/. && \
	cp $FEATUREDB_DIR/script/run* /usr/local/bin/. && \
	cp $FEATUREDB_DIR/image_metadata/build/quip_image_metadata /usr/local/bin/. && \
	cd /tmp

RUN curl --silent --location https://rpm.nodesource.com/setup_4.x | bash - && \
	yum -y install nodejs && \
	cd $FEATUREDB_DIR/query_server && \
	npm install mongodb 

# fix the opencv java shared library
RUN execstack -c $OPENCV_JAVA_DIR/libopencv_java310.so

# Add data folder
RUN mkdir -p /data/db

EXPOSE 27017
EXPOSE 3000

CMD ["run_docker_mongodb_query.sh"]
