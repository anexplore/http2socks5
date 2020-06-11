FROM openjdk:8
LABEL MAINTAINER anexplore@github.com

ADD run.sh logback.xml http2socks5-1.0-SNAPSHOT-jar-with-dependencies.jar /home/http2socks5/
WORKDIR /home/http2socks5
RUN chmod 755 run.sh
CMD ./run.sh 
