package com.lulucloud.touchscript.core.automation

data class DebugScriptDraft(
    val scriptName: String,
    val content: String
)

object DebugScriptDraftStore {
    @Volatile
    private var currentDraft: DebugScriptDraft? = null

    fun set(draft: DebugScriptDraft) {
        currentDraft = draft
    }

    fun get(): DebugScriptDraft? = currentDraft

    fun clear() {
        currentDraft = null
    }
}
