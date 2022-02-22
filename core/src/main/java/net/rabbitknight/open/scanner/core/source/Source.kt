package net.rabbitknight.open.scanner.core.source

import net.rabbitknight.open.scanner.core.image.ImageWrapper

interface Source {
    /**
     * 可获取图片的数量
     */
    fun available(): Int

    /**
     * 取出一帧图片
     */
    fun take(): ImageWrapper

    fun close()
}