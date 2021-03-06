package net.rabbitknight.open.scanner.core.image.camerax

import androidx.camera.core.ImageProxy
import net.rabbitknight.open.scanner.core.image.ImageWrapper
import net.rabbitknight.open.scanner.core.image.WrapperOwner

fun ImageProxy.wrap(
    owner: WrapperOwner<ImageProxy>
): ImageWrapper<ImageProxy> =
    CameraXImage(owner, this)