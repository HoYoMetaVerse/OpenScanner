package net.rabbitknight.open.scanner.core

import android.os.Handler
import net.rabbitknight.open.scanner.core.config.Config
import net.rabbitknight.open.scanner.core.image.ImageWrapper
import net.rabbitknight.open.scanner.core.result.ImageResult

interface Scanner {
    /**
     * 配置
     */
    fun setConfig(config: Config)

    /**
     * 处理图像
     * @param frameListener 单帧image对应的处理结果，但是内部缓存时，使用了弱引用
     */
    fun process(
        image: ImageWrapper<Any>,
        frameListener: ScanResultListener = NullFrameListener
    ): Boolean

    /**
     * 获取结果
     */
    fun getResult(handler: Handler? = null, callback: ScanResultListener)

    /**
     * 释放扫描器
     * 一旦调用该方法则认为扫描器不应再被使用
     */
    fun release()
}

typealias ScanResultListener = (ImageWrapper<Any>, ImageResult) -> Unit


private object NullFrameListener : ScanResultListener {
    override fun invoke(p1: ImageWrapper<Any>, p2: ImageResult) {
    }
}
