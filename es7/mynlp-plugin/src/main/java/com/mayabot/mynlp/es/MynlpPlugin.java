/*
 *  Copyright 2017 mayabot.com authors. All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package com.mayabot.mynlp.es;

import com.google.common.collect.Lists;
import com.mayabot.mynlp.es.nlpdata.NlpSchedulerJog;
import com.mayabot.nlp.MynlpBuilder;
import com.mayabot.nlp.Mynlps;
import org.apache.lucene.analysis.Analyzer;
import org.elasticsearch.SpecialPermission;
import org.elasticsearch.client.Client;
import org.elasticsearch.cluster.service.ClusterService;
import org.elasticsearch.common.io.stream.NamedWriteableRegistry;
import org.elasticsearch.common.settings.Setting;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.NamedXContentRegistry;
import org.elasticsearch.env.Environment;
import org.elasticsearch.env.NodeEnvironment;
import org.elasticsearch.index.analysis.AnalyzerProvider;
import org.elasticsearch.index.analysis.TokenizerFactory;
import org.elasticsearch.indices.analysis.AnalysisModule;
import org.elasticsearch.plugins.AnalysisPlugin;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.plugins.SearchPlugin;
import org.elasticsearch.script.ScriptService;
import org.elasticsearch.search.fetch.FetchSubPhase;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.watcher.ResourceWatcherService;

import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;

/**
 * @author jimichan
 */
public class MynlpPlugin extends Plugin implements AnalysisPlugin, SearchPlugin {

    /**
     * cws模型比较重，默认不开启。
     */
    private boolean enableCws;
    private String mynlpServer;


    public MynlpPlugin(Settings settings, Path configPath) {

        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(new SpecialPermission());
        }

        enableCws =  MynlpPluginSettings.getEnableCws().get(settings);
        mynlpServer = MynlpPluginSettings.getServer().get(settings);

        Mynlps.install(builder -> {
            builder.set("mynlp.server",mynlpServer);
        });
    }

    @Override
    public Collection<Object> createComponents(Client client, ClusterService clusterService, ThreadPool threadPool, ResourceWatcherService resourceWatcherService, ScriptService scriptService, NamedXContentRegistry xContentRegistry, Environment environment, NodeEnvironment nodeEnvironment, NamedWriteableRegistry namedWriteableRegistry) {
        //
        // 这里可以监控NlpService。每隔1分钟轮训一次。查看是否更新了资源。
        // client.n
        // nodeEnvironment.nodeId()
        // 然后把同步过程的日志信息写入固定的索引，其他系统可以在日志里面查询到每个节点的数据同步情况。
        //

        NlpSchedulerJog job = new NlpSchedulerJog(threadPool, nodeEnvironment.nodeId());

        return Lists.newArrayList(job);
    }

    @Override
    public List<Setting<?>> getSettings() {
        return Arrays.asList(
                MynlpPluginSettings.getServer(),
                MynlpPluginSettings.getEnableCws());
    }


    @Override
    public Map<String, AnalysisModule.AnalysisProvider<TokenizerFactory>> getTokenizers() {
        Map<String, AnalysisModule.AnalysisProvider<TokenizerFactory>> extra = new HashMap<>();

        extra.put("mynlp", MynlpTokenizerFactory::new);
        extra.put("mynlp-core", MynlpTokenizerFactory::new);

        if (enableCws) {
            extra.put("mynlp-cws", MynlpTokenizerFactory::new);
        }

        extra.put("issue", ChineseIssueTokenizerFactory::new);
        extra.put("hash", StringHashTokenizerFactory::new);

        extra.put("pinyin", PinyinTokenizerFactory::new);
        extra.put("pinyin-head", PinyinTokenizerFactory::new);
        extra.put("pinyin-stream", PinyinTokenizerFactory::new);
        extra.put("pinyin-fuzzy", PinyinTokenizerFactory::new);
        extra.put("pinyin-keyword", PinyinTokenizerFactory::new);
        extra.put("pinyin-head-keyword", PinyinTokenizerFactory::new);
        extra.put("pinyin-fuzzy-keyword", PinyinTokenizerFactory::new);

        return extra;
    }


    @Override
    public Map<String, AnalysisModule.AnalysisProvider<AnalyzerProvider<? extends Analyzer>>> getAnalyzers() {

        //开启异步线程，先执行一个core的分词，试图去下载依赖的资源

        Map<String, AnalysisModule.AnalysisProvider<AnalyzerProvider<? extends Analyzer>>> map = new HashMap<>(10);

        map.put("issue", ChineseIssueNumProvider::new);
        map.put("hash", StringHashProvider::new);

        map.put("pinyin", PinyinProvider::new);
        map.put("pinyin-head", PinyinProvider::new);
        map.put("pinyin-stream", PinyinProvider::new);
        map.put("pinyin-fuzzy", PinyinProvider::new);
        map.put("pinyin-keyword", PinyinProvider::new);
        map.put("pinyin-head-keyword", PinyinProvider::new);
        map.put("pinyin-fuzzy-keyword", PinyinProvider::new);


        map.put("mynlp", MynlpAnalyzerProvider::new);
        map.put("mynlp-core", MynlpAnalyzerProvider::new);
        if (enableCws) {
            map.put("mynlp-cws", MynlpAnalyzerProvider::new);
        }

        return map;
    }

    @Override
    public List<SearchExtSpec<?>> getSearchExts() {
        return Lists.newArrayList(SummaryExtKt.regSummarySearchExtBuilder());
    }

    @Override
    public List<FetchSubPhase> getFetchSubPhases(FetchPhaseConstructionContext context){
        return Lists.newArrayList(new SummaryFetchSubPhase());
    }
}
