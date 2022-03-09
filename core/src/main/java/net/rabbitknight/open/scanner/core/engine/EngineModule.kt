package net.rabbitknight.open.scanner.core.engine

import android.os.Handler
import net.rabbitknight.open.scanner.core.config.Config
import net.rabbitknight.open.scanner.core.image.ImageWrapper
import net.rabbitknight.open.scanner.core.lifecycle.IModule
import net.rabbitknight.open.scanner.core.result.ImageResult
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue

class EngineModule(vararg engines: Class<out Engine>) : IModule {
    private val inputQueue = ArrayBlockingQueue<ImageWrapper>(3)
    private var resultListener: Pair<Handler?, (ImageResult) -> Unit>? = null

    override fun onConfig(config: Config) {
    }

    override fun onStart() {
    }

    override fun onStop() {
    }

    override fun onStep() {

    }

    fun getInput(): BlockingQueue<ImageWrapper> = inputQueue

    fun getOutput(handler: Handler?, callback: (ImageResult) -> Unit) {
        resultListener = Pair(handler, callback)
    }
}