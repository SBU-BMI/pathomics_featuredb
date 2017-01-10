FROM ubuntu:latest
MAINTAINER Tahsin Kurc 

RUN apt-get -y update
RUN apt-get -y install build-essential \
		curl \
		cmake \
		zip \
		ant \
		git \
		default-jdk

# Install libraries
WORKDIR /tmp/

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
RUN git clone -b handle-quip-output https://github.com/SBU-BMI/pathomics_featuredb && \
	cd pathomics_featuredb/src && \
	gradle build && \
	gradle installDist && \
	cd /tmp && mv pathomics_featuredb /usr/local/. && \
	cp $FEATUREDB_DIR/docker_scripts/run* /usr/local/bin/. && \
	cp $FEATUREDB_DIR/script/run* /usr/local/bin/. && \
	cd /tmp

CMD ["/bin/bash"]

