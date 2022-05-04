package net.rabbitknight.open.scanner.core.process

import android.os.Handler
import net.rabbitknight.open.scanner.core.C
import net.rabbitknight.open.scanner.core.config.Config
import net.rabbitknight.open.scanner.core.lifecycle.IModule
import net.rabbitknight.open.scanner.core.result.*
import java.util.concurrent.LinkedBlockingQueue
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * 后处理
 * 1. 检测结果是否复合
 */
class Postprocessor() : IModule {
    private var resultListener: Pair<Handler, (ImageResult) -> Unit>? = null
    private val source = LinkedBlockingQueue<ImageFrame>()

    fun getOutput(handler: Handler, callback: (ImageResult) -> Unit) {
        resultListener = Pair(handler, callback)
    }

    fun getSource() = source

    override fun onConfig(config: Config) {
    }

    override fun onStart() {
    }

    override fun onStop() {

    }

    override fun onStep() {
        val imageFrame = source.take()

        // 不同engine检测结果去重
        val mergeResults = mutableListOf<BarcodeResult>()

        val allResults = mutableListOf<BarcodeResult>()

        imageFrame.result.forEach {
            allResults.addAll(it.result)
        }

        allResults.forEach { result ->
            // 找到相同的两个结果 (可能是不同引擎输出的
            val index = mergeResults.indexOfFirst {
                it.format == result.format  // 格式相同
                    && it.payload == result.payload // 结果相同
                    && cross(it.rect, result.rect)  // 兴趣框相交
            }
            // 如果可以找到 则合并结果
            if (index != -1) {
                val old = mergeResults.removeAt(index)
                val merge = merge(old.rect, result.rect)
                val replace = BarcodeResult(
                    result.format, merge, result.payload, result.rawBytes
                )
                mergeResults.add(replace)
            }
        }

        // crop的坐标 转化为 图像的坐标
        val results = mergeResults.map {
            val left = it.rect.left + imageFrame.cropRect.left
            val top = it.rect.top + imageFrame.cropRect.top
            val right = it.rect.right + imageFrame.cropRect.left
            val bottom = it.rect.bottom + imageFrame.cropRect.top
            val rect = Rect(left, top, right, bottom)
            BarcodeResult(it.format, rect, it.payload, it.rawBytes)
        }

        // 结果
        val code = if (results.isNotEmpty()) {
            C.CODE_SUCCESS
        } else {
            imageFrame.result.first().code
        }

        // 结果输出
        val imageResult = ImageResult(
            code, imageFrame.timestamp, results
        )
        resultListener?.let {
            val handler = it.first
            val listener = it.second
            handler.post {
                listener.invoke(imageResult)
            }
        }
        // todo 焦距
    }

    /**
     * 判断两个矩形是否相交
     */
    internal fun cross(a: Rect, b: Rect): Boolean {
        val aCenterX = a.centerX()
        val aCenterY = a.centerY()
        val bCenterX = b.centerX()
        val bCenterY = b.centerY()
        return (abs(aCenterX - bCenterX) <= (a.width() + b.width()) / 2.0f)
            && (abs(aCenterY - bCenterY) <= (a.height() + b.height()) / 2.0f)
    }

    /**
     * 合并两个矩形
     */
    internal fun merge(a: Rect, b: Rect): Rect {
        val left = min(a.left, b.left)
        val top = min(a.top, b.top)
        val right = max(a.right, b.right)
        val bottom = max(a.bottom, b.bottom)
        return Rect(left, top, right, bottom)
    }
}