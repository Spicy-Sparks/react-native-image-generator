package com.reactnativeimagegenerator

import android.annotation.SuppressLint
import android.graphics.*
import android.net.Uri
import android.os.Build
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.Base64
import com.facebook.react.bridge.*
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.net.URL
import kotlin.math.tan
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class ImageGeneratorModule(reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {

  override fun getName(): String {
    return "ImageGenerator"
  }

  private fun createLayer(data: Map<String, Any>): Layer {
    return if (data["uri"] != null) {
      try {
        PictureLayer(
          data["uri"] as String,
          data["width"] as Double,
          data["height"] as Double,
          data["x"] as Double,
          data["y"] as Double,
          (data["skewX"] as Double?) ?: 0.0,
          (data["skewY"] as Double?) ?: 0.0,
          (data["radius"] as Double?) ?: 0.0
        )
      } catch (e: Exception) {
        PictureLayer("", 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
      }
    } else {
      try {
        @Suppress("UNCHECKED_CAST")
        TextLayer(
          data["text"] as String,
          (data["fontSize"] as Double?) ?: 0.0,
          (data["fontFamily"] as String?) ?: "",
          data["color"] as String,
          (data["opacity"] as Double?) ?: 1.0,
          data["width"] as Double,
          data["height"] as Double,
          data["x"] as Double,
          data["y"] as Double,
          data["maxLines"] as Double,
          (data["alignment"] as String?) ?: "center"
        )
      } catch (e: Exception) {
        TextLayer("", 0.0, null, "#FFFFFF", 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, "center")
      }
    }
  }

  private fun getBitmapForLayer(layer: PictureLayer?): Bitmap {
    if (layer == null || layer.uri.isNullOrEmpty()) {
      return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
    }

    return try {
      val bitmap: Bitmap = when {
        layer.uri.startsWith("http") -> {
          val url = URL(layer.uri)
          BitmapFactory.decodeStream(url.openConnection().getInputStream())
        }
        layer.uri.startsWith("file") -> {
          val uri = Uri.parse(layer.uri)
          val path = uri.path ?: ""
          val file = File(path)
          if (file.exists()) {
            BitmapFactory.decodeFile(path)
          } else {
            Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
          }
        }
        else -> {
          @SuppressLint("DiscouragedApi")
          val resId = reactApplicationContext.resources.getIdentifier(layer.uri, "mipmap", reactApplicationContext.packageName)
          BitmapFactory.decodeResource(reactApplicationContext.resources, resId)
        }
      }
      bitmap
    } catch (e: Exception) {
      Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
    }
  }

  private fun drawPictureLayer(canvas: Canvas, layer: PictureLayer) {
    val bitmap = getBitmapForLayer(layer)
    val roundedBitmap = getRoundedCornerBitmap(bitmap, layer.radius.toFloat())
    val finalBitmap =
      if (layer.skewX.toInt() == 0 && layer.skewY.toInt() == 0) {
      roundedBitmap
      } else {
        getSkewedBitmap(roundedBitmap, layer.skewX, layer.skewY)
      }

    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    canvas.drawBitmap(
      finalBitmap,
      null,
      Rect(
        layer.x.toInt(),
        layer.y.toInt(),
        (layer.x + layer.width).toInt(),
        (layer.y + layer.height).toInt()
      ),
      paint
    )
  }

  private fun getRoundedCornerBitmap(bitmap: Bitmap, radius: Float): Bitmap {
    val output = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(output)

    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    val rect = Rect(0, 0, bitmap.width, bitmap.height)
    val rectF = RectF(rect)

    canvas.drawARGB(0, 0, 0, 0)
    canvas.drawRoundRect(rectF, radius, radius, paint)
    paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
    canvas.drawBitmap(bitmap, rect, rect, paint)

    return output
  }

  private fun getSkewedBitmap(bitmap: Bitmap, skewX: Double, skewY: Double): Bitmap {
    val matrix = Matrix()
    matrix.postSkew(
      tan(Math.toRadians(skewY)).toFloat(),
      tan(Math.toRadians(skewX)).toFloat()
    )

    return Bitmap.createBitmap(
      bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true
    )
  }

  private fun drawTextLayer(canvas: Canvas, layer: TextLayer) {
    val paint = TextPaint(Paint.ANTI_ALIAS_FLAG)
    paint.style = Paint.Style.FILL
    paint.textSize = layer.fontSize?.toFloat() ?: 65F

    val typeface = Typeface.createFromAsset(reactApplicationContext.assets, "fonts/${layer.fontFamily}.ttf")
    paint.typeface = typeface

    val rgbColor = hexToRgb(layer.color)
    val alpha = ((layer.opacity ?: 1.0) * 255).toInt()
    val red = rgbColor[0]
    val green = rgbColor[1]
    val blue = rgbColor[2]
    paint.color = Color.argb(alpha, red, green, blue)

    val padding = layer.x
    val textWidth = layer.width.toFloat() - padding

    val alignment =
      if (layer.alignment == "center") {
        Layout.Alignment.ALIGN_CENTER
      } else if (layer.alignment == "left") {
        Layout.Alignment.ALIGN_NORMAL
      } else if (layer.alignment == "right") {
        Layout.Alignment.ALIGN_OPPOSITE
      } else {
        Layout.Alignment.ALIGN_CENTER
      }

    var staticLayout =
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        StaticLayout.Builder.obtain(layer.text, 0, layer.text.length, paint, textWidth.toInt())
          .setAlignment(alignment)
          .setLineSpacing(0f, 1f)
          .setIncludePad(false)
          .build()
      } else {
        @Suppress("DEPRECATION")
        StaticLayout(
          layer.text,
          paint,
          textWidth.toInt(),
          alignment,
          1f,
          0f,
          false
        )
      }

    if (staticLayout.lineCount > layer.maxLines) {
      val truncatedText = layer.text.substring(0, staticLayout.getLineEnd(layer.maxLines.toInt() - 1)).trimEnd()
      staticLayout =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
          StaticLayout.Builder.obtain(
            truncatedText,
            0,
            truncatedText.length,
            paint,
            textWidth.toInt()
          )
            .setAlignment(alignment)
            .setLineSpacing(0f, 1f)
            .setIncludePad(false)
            .build()
        } else {
          @Suppress("DEPRECATION")
          StaticLayout(
            truncatedText,
            paint,
            textWidth.toInt(),
            alignment,
            1f,
            0f,
            false
          )
        }
    }

    canvas.save()
    canvas.translate(
      (layer.x + padding / 2).toFloat(),
      layer.y.toFloat() + (layer.height.toFloat() - staticLayout.height) / 2
    )
    staticLayout.draw(canvas)
    canvas.restore()
  }

  private fun drawLayer(canvas: Canvas, layer: Layer) {
    if (layer is PictureLayer) {
      drawPictureLayer(canvas, layer)
    } else {
      drawTextLayer(canvas, layer as TextLayer)
    }
  }

  private fun saveBitmap(bitmap: Bitmap, filePath: String): String {
    var uri = Uri.parse(filePath)
    if (uri.scheme == null) {
      uri = Uri.parse("file://$filePath")
    }
    val outputStream: OutputStream? = reactApplicationContext.contentResolver
      .openOutputStream(uri, if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) "w" else "rwt")
    if (outputStream != null) {
      bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
    }
    outputStream?.close()

    return filePath
  }

  private fun hexToRgb(hex: String): List<Int> {
    val regex = Regex("^#?([a-f\\d]{2})([a-f\\d]{2})([a-f\\d]{2})$", RegexOption.IGNORE_CASE)
    val matchResult = regex.find(hex)

    if (matchResult != null) {
      val r = matchResult.groupValues[1].toIntOrNull(16) ?: 255
      val g = matchResult.groupValues[2].toIntOrNull(16) ?: 255
      val b = matchResult.groupValues[3].toIntOrNull(16) ?: 255
      return listOf(r, g, b)
    } else {
      return listOf(255, 255, 255)
    }
  }

  private val lock = Any()

  private fun generateImage(layersData: ReadableMap, config: ReadableMap): String {
    return synchronized(lock) {
      val layers = layersData.getArray("layers")
      val width = config.getInt("width")
      val height = config.getInt("height")
      val filePath = config.getString("filePath")
      val base64: Boolean = config.hasKey("base64") && config.getBoolean("base64")

      val bgBitMapConfig = Bitmap.Config.ARGB_8888
      val bgBitmap = Bitmap.createBitmap(width, height, bgBitMapConfig)
      val canvas = Canvas(bgBitmap)

      if (layers != null) {
        @Suppress("UNCHECKED_CAST")
        for (item in layers.toArrayList()) drawLayer(canvas, createLayer(item as Map<String, Any>))
      }

      return if (base64) {
        val baos = ByteArrayOutputStream()
        bgBitmap.compress(Bitmap.CompressFormat.PNG, 100, baos)
        "data:image/png;base64," + Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT)
      } else {
        filePath?.let { saveBitmap(bgBitmap, it) }?.toString() ?: ""
      }
    }
  }

  @ReactMethod
  fun generate(layersData: ReadableMap, config: ReadableMap, promise: Promise) {
    CoroutineScope(Dispatchers.IO).launch {
      try {
        withContext(Dispatchers.Default) {
          val result = generateImage(layersData, config)
          promise.resolve(result)
        }
      } catch (e: Exception) {
        withContext(Dispatchers.Default) {
          promise.reject("Error generating image: ", e.stackTraceToString())
        }
      }
    }
  }
}
