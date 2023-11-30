package com.reactnativeimagegenerator

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.*
import android.net.Uri
import android.os.Build
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.Base64
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.facebook.react.bridge.*
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.net.URL
import kotlin.math.tan

private const val WRITE_EXTERNAL_STORAGE_REQUEST_CODE = 123

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
        val colorArray = data["color"] as ArrayList<Double>
        TextLayer(
          data["text"] as String,
          data["fontSize"] as Double?,
          data["fontFamily"] as String?,
          colorArray,
          data["width"] as Double,
          data["height"] as Double,
          data["x"] as Double,
          data["y"] as Double,
          data["maxLines"] as Double
        )
      } catch (e: Exception) {
        TextLayer("", 0.0, null, arrayListOf(0.0), 0.0, 0.0, 0.0, 0.0, 0.0)
      }
    }
  }

  private fun getBitmapForLayer(layer: PictureLayer): Bitmap {
    return try {
      if ("http" in layer.uri) {
        val url = URL(layer.uri)
        BitmapFactory.decodeStream(url.openConnection().getInputStream())
      } else {
        @SuppressLint("DiscouragedApi")
        val resId = reactApplicationContext.resources.getIdentifier(layer.uri, "mipmap", reactApplicationContext.packageName)
        BitmapFactory.decodeResource(reactApplicationContext.resources, resId)
      }
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

    canvas.drawBitmap(
      finalBitmap,
      null,
      Rect(
        layer.x.toInt(),
        layer.y.toInt(),
        (layer.x + layer.width).toInt(),
        (layer.y + layer.height).toInt()
      ),
      null
    )
  }

  private fun getRoundedCornerBitmap(bitmap: Bitmap, radius: Float): Bitmap {
    val output = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(output)

    val paint = Paint()
    val rect = Rect(0, 0, bitmap.width, bitmap.height)
    val rectF = RectF(rect)

    paint.isAntiAlias = true
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

    val colorArray = layer.color
    if (colorArray.size == 4) {
      val alpha = (colorArray[3] * 255).toInt()
      val red = colorArray[0].toInt()
      val green = colorArray[1].toInt()
      val blue = colorArray[2].toInt()
      paint.color = Color.argb(alpha, red, green, blue)
    } else {
      paint.color = Color.WHITE
    }

    val padding = layer.x
    val textWidth = layer.width.toFloat() - padding

    var staticLayout =
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        StaticLayout.Builder.obtain(layer.text, 0, layer.text.length, paint, textWidth.toInt())
          .setAlignment(Layout.Alignment.ALIGN_CENTER)
          .setLineSpacing(0f, 1f)
          .setIncludePad(false)
          .build()
      } else {
        @Suppress("DEPRECATION")
        StaticLayout(
          layer.text,
          paint,
          textWidth.toInt(),
          Layout.Alignment.ALIGN_CENTER,
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
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setLineSpacing(0f, 1f)
            .setIncludePad(false)
            .build()
        } else {
          @Suppress("DEPRECATION")
          StaticLayout(
            truncatedText,
            paint,
            textWidth.toInt(),
            Layout.Alignment.ALIGN_CENTER,
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
    if (ContextCompat.checkSelfPermission(
        reactApplicationContext,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
      ) != PackageManager.PERMISSION_GRANTED
    ) {
      reactApplicationContext.currentActivity?.let {
        ActivityCompat.requestPermissions(
          it,
          arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
          WRITE_EXTERNAL_STORAGE_REQUEST_CODE
        )
      }
    }

    var uri = Uri.parse(filePath)
    if (uri.scheme == null) {
      uri = Uri.parse("file://$filePath")
    }
    val outputStream: OutputStream? = reactApplicationContext.contentResolver
      .openOutputStream(uri, if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) "w" else "rwt")
    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
    outputStream?.close()

    return "file://$filePath"
  }

  @ReactMethod
  fun generate(layersData: ReadableMap, config: ReadableMap, promise: Promise) {
    try {
      val layers = layersData.getArray("layers")
      val width = config.getInt("width")
      val height = config.getInt("height")
      val filePath = config.getString("filePath")
      val base64: Boolean =
        if (config.hasKey("base64")) {
          config.getBoolean("base64")
        } else {
          false
        }
      val bgBitMapConfig = Bitmap.Config.ARGB_8888
      val bgBitmap = Bitmap.createBitmap(width, height, bgBitMapConfig)
      val canvas = Canvas(bgBitmap)
      if (layers != null) {
        @Suppress("UNCHECKED_CAST")
        for (item in layers.toArrayList()) drawLayer(canvas, createLayer(item as Map<String, Any>))
      }
      if (base64) {
        val baos = ByteArrayOutputStream()
        bgBitmap.compress(Bitmap.CompressFormat.PNG, 100, baos)
        val result = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT)
        promise.resolve("data:image/png;base64,$result")
      } else {
        val result = filePath?.let { saveBitmap(bgBitmap, it) }
        promise.resolve(result)
      }
    } catch (e: Exception) {
      promise.reject("Error generating image: ", e.stackTraceToString())
    }
  }
}
