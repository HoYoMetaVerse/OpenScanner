package net.rabbitknight.open.scanner.core.engine

import android.util.Log
import net.rabbitknight.open.scanner.core.C
import net.rabbitknight.open.scanner.core.ContextProvider
import net.rabbitknight.open.scanner.core.ScannerException
import net.rabbitknight.open.scanner.core.config.Config
import net.rabbitknight.open.scanner.core.image.ImageFormat
import net.rabbitknight.open.scanner.core.image.ImageWrapper
import net.rabbitknight.open.scanner.core.image.WrapperOwner
import net.rabbitknight.open.scanner.core.image.wrap
import net.rabbitknight.open.scanner.core.process.ImageFrame
import net.rabbitknight.open.scanner.core.process.base.BaseModule
import net.rabbitknight.open.scanner.core.utils.ImageUtils

/**
 * EngineModule 用来完成对引擎的管理，使用引擎来做数据处理
 */
class EngineModule(val engines: Array<Class<out Engine>>) : BaseModule() {
    companion object {
        private const val TAG = "EngineModule"
    }

    private val engineImpls = hashMapOf<Class<out Engine>, Engine>()

    private lateinit var config: Config


    override fun onConfig(config: Config) {
        this.config = config
    }

    override fun onCreate() {
        super.onCreate()
        engines.forEach {
            val impl = it.newInstance()
            engineImpls[it] = impl

            impl.init(ContextProvider.context())
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        engineImpls.forEach {
            val impl = it.value
            impl.release()
        }
        engineImpls.clear()
    }

    override fun onProcess(frame: ImageFrame) {
        val config = this.config

        // 解码
        val decode: (Map.Entry<Class<out Engine>, Engine>) -> Boolean = {
            val engine = it.value
            val clazz = it.key

            val wantedFormat = engine.preferImageFormat()
            // 获取图像
            val image = frame.cvtImage.getOrPut(wantedFormat) {
                cvtImage(frame.cropImage, wantedFormat)
            }
            // 引擎解码
            val imageResult = engine.decode(image)

            // 添加全部结果
            frame.result.add(imageResult)

            // lambda返回值
            imageResult.code == C.CODE_SUCCESS
        }
        if (config.multimode == C.ENGINE_MUTIMODE_ALL) {
            engineImpls.count(decode)
        } else {
            engineImpls.any(decode)
        }

        // 输出到下个模块
        getSink().offer(frame)
    }

    /**
     * 将输入的图像转换为引擎期望的格式
     * @param from 输入到EngineModule的原始格式
     * @param wantedFormat 引擎实例 期望的格式
     */
    private fun cvtImage(
        from: ImageWrapper<ByteArray>,
        @ImageFormat.Format wantedFormat: String
    ): ImageWrapper<ByteArray> {
        val cache = when (wantedFormat) {
            ImageFormat.ARGB -> {
                val cache = this.acquire(from.width, from.height, ImageFormat.ARGB)
                val rst = ImageUtils.convertByteArrayToARGB(from, cache)
                if (!rst) {
                    Log.e(TAG, "cvtImage: to@${wantedFormat},from@${from},fail!!")
                }
                cache
            }
            ImageFormat.YV12 -> {
                val cache = this.acquire(from.width, from.height, ImageFormat.YV12)
                val rst = ImageUtils.convertByteArrayToYV12(from, cache)
                if (!rst) {
                    Log.e(TAG, "cvtImage: to@${wantedFormat},from@${from},fail!!")
                }
                cache
            }
            else -> {
                throw ScannerException("not support $wantedFormat")
            }
        }
        return cache.wrap(cvtRecycleOwner, wantedFormat, from.width, from.height, from.timestamp)
    }


    /**
     * cvtImage回收会到这个接口此处完成真正的数据回收
     */
    private val cvtRecycleOwner = object : WrapperOwner<ByteArray> {
        override fun close(payload: ByteArray) {
            release(payload)
        }
    }
}