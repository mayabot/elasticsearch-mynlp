package com.mayabot.mynlp.es.nlpdata

import com.mayabot.nlp.Mynlps
import com.mayabot.nlp.segment.lexer.core.BiGramTableDictionary
import com.mayabot.nlp.segment.lexer.core.CoreDictionary
import org.elasticsearch.common.component.AbstractLifecycleComponent
import org.elasticsearch.common.logging.Loggers
import org.elasticsearch.common.unit.TimeValue
import org.elasticsearch.threadpool.Scheduler
import org.elasticsearch.threadpool.ThreadPool
import java.util.concurrent.TimeUnit



class NlpSchedulerJog(val threadPool: ThreadPool, val nodeId:String): AbstractLifecycleComponent() {

    lateinit var schedule: Scheduler.Cancellable

    //val logger = Loggers.getLogger(NlpSchedulerJog::class.java)
    // it will be throw an execption ava.lang.IllegalArgumentException: if you don't need a prefix then use a regular logger

    override fun doStart() {

        schedule = threadPool.scheduleWithFixedDelay(
            Runnable {

                Mynlps.instanceOf(CoreDictionary::class.java).refresh()

                Mynlps.instanceOf(BiGramTableDictionary::class.java).refresh()


            }, TimeValue(1,TimeUnit.MINUTES),ThreadPool.Names.GENERIC)
    }

    override fun doStop() {
        schedule.cancel()
    }

    override fun doClose() {

    }

}


