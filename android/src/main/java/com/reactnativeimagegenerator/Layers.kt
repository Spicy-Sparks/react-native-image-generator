package com.reactnativeimagegenerator

interface Layer

data class PictureLayer(
  var uri: String,
  var width: Double,
  var height: Double,
  var x: Double,
  var y: Double,
  var skewX: Double = 0.0,
  var skewY: Double = 0.0,
  var radius: Double = 0.0
) : Layer

data class TextLayer(
  var text: String,
  var fontSize: Double?,
  var fontFamily: String?,
  var color: ArrayList<Double>,
  var width: Double,
  var height: Double,
  var x: Double,
  var y: Double,
  var maxLines: Double
) : Layer
