# 版本和依赖

1.0.0 -> 3.1.0
1.0.1 -> 3.1.0

# Changes
## V 1.0.1
在es7.2.1中启动失败
```
Caused by: java.lang.IllegalArgumentException: if you don't need a prefix then use a regular logger
 at org.elasticsearch.common.logging.PrefixLogger.<init>(PrefixLogger.java:80) ~[elasticsearch-7.2.1.jar:7.2.1]
 at org.elasticsearch.common.logging.Loggers.getLogger(Loggers.java:69) ~[elasticsearch-7.2.1.jar:7.2.1]
 at com.mayabot.mynlp.es.nlpdata.NlpSchedulerJog.<init>(NlpSchedulerJog.kt:19) ~[?:?]
 at com.mayabot.mynlp.es.MynlpPlugin.createComponents(MynlpPlugin.java:85) ~[?:?]
``` 
