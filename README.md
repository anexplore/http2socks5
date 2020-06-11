# http2socks5
A http proxy adapter for socks 5 proxy.

##### 1.build
~~~shell
mvn package
~~~
This will create a jar with all dependencies in target/ dir

build docker images
~~~shell script
docker build -t http2socks5:latest .
~~~

##### 2. start
~~~shell
java -Dsocks5ProxyHost=127.0.0.1 -Dsocks5ProxyPort=1080 -jar http2socks5-${version}-jar-with-dependencies.jar
~~~
or start a docker container
~~~shell script
docker run -d \
  -e socks5ProxyHost=127.0.0.1 -e socks5ProxyPort=1080 \
  -p 80:80 \
  --name http2socks5 \
  http2socks5:latest 
~~~

This is will create adapter http server on (0.0.0.0:80), and will forward http request from client to socks5ProxyHost:socks5ProxyPort(127.0.0.1:1080) 

##### 3.configs
we can pass configs by os env or use -Dxx=xx.

>in docker mode, set configs by os env

###### configs

|name|must|default value|note|
|:---:|:----:|:---:|:---:|
|socks5ProxyHost|true|-|socks5 proxy server host|
|socks5ProxyPort|true|-|socks5 proxy server port|
|httpServerBindLocalAddress|false|0.0.0.0|local server bind host|
|httpServerBindLocalPort|false|80|local server bind port|
|maxConnectionBacklog|false|1000|local server tcp accept backlog|
|workerEventGroupNumber|false|jvm cpu logic processor number|io worker thread number|
|idleTimeoutForClient|false|10000|idle timeout when no io, ms|
|connectionTimeoutToSocks5ProxyServer|false|10000|timeout for connect to socks 5 proxy server, ms|
|openNettyLoggingHandler|false|0|wether open netty LoggingHandler for debug info, 1 open |

##### 4.aims to?
>this is a very simple adapter server, we omits many feature like client access auth/traffic control/friendly 4xx/5xx html response/html cache/channel pool... etc.
* for spiders only support http proxy. we have no 5xx/4xx friendly page/page cache, so this will not disturb the spider's extracting.
* for software do not support socks5 proxy. you can start this adapter on your local machine to forward request to your local socks5 proxy.
* where no need client access auth control. like, in same idc, on same machine.

##### 5.performance
> need to do


 


