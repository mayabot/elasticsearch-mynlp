package com.mayabot.mynlp.es

import org.elasticsearch.common.settings.Setting
import org.elasticsearch.common.settings.Settings


object MynlpPluginSettings {

    @JvmStatic
    val server = Setting.simpleString("mynlp.server", "", Setting.Property.NodeScope)

    @JvmStatic
    val enableCws = Setting.boolSetting("mynlp.cws.enabled", false, Setting.Property.NodeScope)

}