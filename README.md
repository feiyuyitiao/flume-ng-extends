#flume数据源优化

----

#SpoolDirectoryTailFileSource

* [场景](#场景)
* [要求](#要求)
* [解决方案](#解决方案)
	* [组件版本](#组件版本)
	* [预装环境](#预装环境)
	* [安装插件](#安装插件)
	* [配置文件](#配置文件)
	* [配置参数详解](#配置参数详解)
	* [约定条件](#约定条件)
	* [可靠性分析](#可靠性分析)
* [微信券活动分析](#微信券活动分析)
	
##场景

收集日志，具体场景：

* 应用运行时，每天都在指定目录下，产生一个新的日志文件，日志文件名称随意如：`info.log`
* 应用运行时，向当日日志文件追加内容，例如，在2015年02月25日，应用的运行信息，会实时追加到`info.log`文件中；


##要求

在上述场景下，要求，实时收集应用的运行日志，整体性能上几点：

* 实时：日志产生后，应以秒级的延时，收集发送走；
* 可靠：一旦日志收集程序终止，保证flume重启之后，日志数据不丢失，并且不重复发送；
* 历史日志文件处理策略：已经收集过的历史日志文件，会标记文件名字到一个白名单中:日志收集目录spoolDir下的.flumespooltail/.flumespooltailfile-file-names-queue.properties



##解决方案


###组件版本

工程中，涉及到的组件版本，见下表：

|组件|版本|
|----|----|
|Flume NG|`1.5.2`|


###预装环境

编译工程前，需要预装环境：

* JDK 1.6+
* [Apache Maven 3][Apache Maven 3]




###安装插件

1. 准备好工程的jar包：`flume-ng-extends-source-x.x.x.jar`；
1. 两种方法，可以在Flume下安装插件：

**方法一**：标准插件安装 *(Recommended Approach)*，具体步骤：

* 在`${FLUME_HOME}`找到目录`plugins.d`，如果没有找到这一目录，则创建目录`${FLUME_HOME}/plugins.d`；
* 在`${FLUME_HOME}/plugins.d`目录下，创建目录`flume-ng-extends-source`，并在其下创建`lib`和`libext`两个子目录；
* 将`flume-ng-extends-source-x.x.x.jar`复制到`plugins.d/flume-ng-extends-source/lib`目录中；

至此，安装插件后，目录结构如下：

	${FLUME_HOME}
	 |-- plugins.d
			|-- flume-ng-extends-source/lib
				|-- lib
					|-- flume-ng-extends-source-x.x.x.jar
				|-- libext

Flume插件安装的更多细节，参考[Flume User Guide](https://flume.apache.org/FlumeUserGuide.html#the-plugins-d-directory)


**方法二**：快速插件安装 *(Quick and Dirty Approach)*，具体步骤：

* 将`flume-ng-extends-source-x.x.x.jar`复制到`${FLUME_HOME}/lib`目录中；



###配置文件

在flume的配置文件`flume-conf.properties`中，配置`agent`下`SpoolDirectoryTailFileSource` source，参考代码如下：

	#Name the compents on this a2
a2.sources = spoolDirTailFile
a2.sinks = k1
a2.channels = c1
a2.sources.spoolDirTailFile.type = com.github.ningg.flume.source.SpoolDirectoryTailFileSource
a2.sources.spoolDirTailFile.spoolDir =/home/song/workspace/bigdata/firstMavenTest/logs
a2.sources.spoolDirTailFile.channels = c1
a2.sources.spoolDirTailFile.interceptors=i1
a2.sources.spoolDirTailFile.interceptors.i1.type=regex_filter
a2.sources.spoolDirTailFile.interceptors.i1.regex=yazuo
a2.sources.spoolDirTailFile.deletePolicy =filenames
a2.sources.spoolDirTailFile.ignorePattern =^$
a2.sources.spoolDirTailFile.targetPattern = .*(\\d){4}-(\\d){2}-(\\d){2}.*
a2.sources.spoolDirTailFile.targetFilename =info.log
a2.sources.spoolDirTailFile.trackerDir = .flumespooltail
a2.sources.spoolDirTailFile.consumeOrder=oldest
a2.sources.spoolDirTailFile.batchSize =100
a2.sources.spoolDirTailFile.inputCharset = UTF-8
a2.sources.spoolDirTailFile.decodeErrorPolicy = REPLACE
a2.sources.spoolDirTailFile.deserializer = LINE/wrapLine

a2.sinks.k1.type = logger
a2.sinks.k1.channel = c1
a2.channels.c1.type =file
a2.channels.c1.capacity = 1000
a2.channels.c1.transactionCapacity = 100



###配置参数详解

详细配置参数如下表（Required properties are in **bold**.）：

|Property Name|	Default|	Description|
|------|------|------|
|**channels**|	–	 |  |
|**type**|	–	|The component type name, needs to be `com.github.ningg.flume.source.SpoolDirectoryTailFileSource`.|
|**spoolDir**|	–	|The directory from which to read files from.|
|fileSuffix|	`.COMPLETED`|	Suffix to append to completely ingested files|
|deletePolicy|	`filenames`|	How to determine completed files: `filenames` or `never` or `immediate`|
|fileHeader|	`false`|	Whether to add a header storing the absolute path filename.|
|fileHeaderKey|	`file`|	Header key to use when appending absolute path filename to event header.|
|basenameHeader|	`false`|	Whether to add a header storing the basename of the file.|
|basenameHeaderKey|	`basename`|	Header Key to use when appending basename of file to event header.|
|ignorePattern|	`^$`	|Regular expression specifying which files to ignore (skip)|
|**targetPattern**|	`.*(\\d){4}-(\\d){2}-(\\d){2}.*`	|Regular expression specifying which files to collect|
|**targetFilename**|	`yyyy-MM-dd`	|The Target File's DateFormat, which refers to [java.text.SimpleDateFormat][java.text.SimpleDateFormat]. Infact, there is strong relationship between Property: **targetFilename** and Property: **targetPattern** |
|trackerDir|	`.flumespooltail`|	Directory to store metadata related to processing of files. If this path is not an absolute path, then it is interpreted as relative to the spoolDir.|
|consumeOrder|	`oldest`	|In which order files in the spooling directory will be consumed `oldest`, `youngest` and `random`. In case of `oldest` and `youngest`, the last modified time of the files will be used to compare the files. In case of a tie, the file with smallest laxicographical order will be consumed first. In case of `random` any file will be picked randomly. When using `oldest` and `youngest` the whole directory will be scanned to pick the oldest/youngest file, which might be slow if there are a large number of files, while using random may cause old files to be consumed very late if new files keep coming in the spooling directory.|
|maxBackoff	|4000	|The maximum time (in millis) to wait between consecutive attempts to write to the channel(s) if the channel is full. The source will start at a low backoff and increase it exponentially each time the channel throws a ChannelException, upto the value specified by this parameter.|
|batchSize	|100|	Granularity at which to batch transfer to the channel|
|inputCharset|	`UTF-8`|	Character set used by deserializers that treat the input file as text.|
|decodeErrorPolicy|	`FAIL`|	What to do when we see a non-decodable character in the input file. `FAIL`: Throw an exception and fail to parse the file. `REPLACE`: Replace the unparseable character with the “replacement character” char, typically Unicode `U+FFFD`. `IGNORE`: Drop the unparseable character sequence.|
|deserializer|	`LINE`|	Specify the deserializer used to parse the file into events. Defaults to parsing each line as an event. The class specified must implement `EventDeserializer.Builder`.|
|deserializer.*	| 	|Varies per event deserializer.*(设置每个deseralizer的实现类，对应的配置参数)*|
|bufferMaxLines|	–	|(Obselete) This option is now ignored.|
|bufferMaxLineLength|	5000|	(Deprecated) Maximum length of a line in the commit buffer. Use `deserializer.maxLineLength` instead.|
|selector.type|	`replicating`|	`replicating` or `multiplexing`|
|selector.*	| 	|Depends on the `selector.type` value|
|interceptors|	–	|Space-separated list of interceptors|
|interceptors.*	|  |  |


	
	
###约定条件

使用上述`SpoolDirectoryTailFileSource`的几个约束：

* 按日期，每日生成一个新日志文件；
* 只以追加方式写入当日的日志文件；
* 在source 监听当日的日志文件时，其他进程不会删除当日的日志文件；
* 不会在同一目录下，生成名称完全相同的文件；


###可靠性分析

下述3种情况下，`SpoolDirectoryTailFileSource`都有很高的可靠性，保证不丢失数据、不重复发送数据，几种情况*（启动时间、重启时间，两个维度）*：

* 日志一直在追加更新，隔日才启动实时收集程序；
* 实时收集程序，当日终止，当日重启；
* 实时收集程序，当日终止，(0-n)次日重启；

上面都是借助meta文件来实现的。




##微信券活动分析

###flume配置文件更改

### weixin_coupon

* a2.sources.spoolDirTailFile.spoolDir =/home/weixin/tomcat_weixin/logs/weixinCardCoupon
* a2.sources.spoolDirTailFile.interceptors.i1.regex=AppLogUtil

### weixin_api
* a2.sources.spoolDirTailFile.spoolDir=/yazuo_apps/logs/weixin-api/log
* a2.sources.spoolDirTailFile.interceptors.i1.regex=AppLogUtil


参考[flume-ng-extends-sourc](https://github.com/ningg/flume-ng-extends-source)
