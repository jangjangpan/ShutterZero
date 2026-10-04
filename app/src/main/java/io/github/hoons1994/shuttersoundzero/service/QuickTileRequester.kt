package io.github.hoons1994.shuttersoundzero.service

import android.app.StatusBarManager
import android.content.ComponentName
import android.content.Context
import android.graphics.drawable.Icon
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit
import io.github.hoons1994.shuttersoundzero.R

/**
 * 알림창(빠른 설정)에 셔터음 타일을 넣어 달라고 시스템에 요청하고, 들어가 있는지 기억한다.
 *
 * 타일이 실제로 패널에 있는지는 앱이 조회할 수 없으므로 [CameraMuteTileService] 의
 * 추가·제거·표시 콜백과 시스템 요청 결과로 상태를 맞춘다.
 */
object QuickTileRequester {
    private const val PREFS = "quick_tile_prefs"
    private const val KEY_ADDED = "added"
    private const val KEY_AUTO_ASKED = "auto_asked"

    /** requestAddTileService 는 Android 13(API 33)부터 있다. */
    val isSupported: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    var isAdded by mutableStateOf(false)
        private set
    private var loaded = false

    fun load(context: Context) {
        if (loaded) return
        loaded = true
        isAdded = prefs(context).getBoolean(KEY_ADDED, false)
    }

    fun markAdded(context: Context, added: Boolean) {
        loaded = true
        isAdded = added
        prefs(context).edit { putBoolean(KEY_ADDED, added) }
    }

    /** 1회 설정이 끝난 뒤 한 번만 자동으로 물어보기 위한 표시. */
    fun shouldAutoAsk(context: Context): Boolean {
        load(context)
        return isSupported && !isAdded && !prefs(context).getBoolean(KEY_AUTO_ASKED, false)
    }

    /**
     * 시스템의 "타일 추가" 창을 띄운다. 앱이 화면에 떠 있을 때만 동작한다.
     * @param onResult 추가됐거나 이미 있으면 true
     */
    fun request(context: Context, onResult: (Boolean) -> Unit = {}) {
        if (!isSupported) {
            onResult(false)
            return
        }
        prefs(context).edit { putBoolean(KEY_AUTO_ASKED, true) }
        val statusBar = context.getSystemService(StatusBarManager::class.java)
        if (statusBar == null) {
            onResult(false)
            return
        }
        runCatching {
            statusBar.requestAddTileService(
                ComponentName(context, CameraMuteTileService::class.java),
                context.getString(R.string.tile_name),
                Icon.createWithResource(context, R.drawable.ic_qs_camera_mute),
                context.mainExecutor
            ) { result ->
                val added = result == StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ADDED ||
                    result == StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ALREADY_ADDED
                if (added) markAdded(context, true)
                onResult(added)
            }
        }.onFailure { onResult(false) }
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
