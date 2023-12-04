protocol Layer {}

class PictureLayer: Layer {
    var uri: String
    var width: Double
    var height: Double
    var x: Double
    var y: Double
    var skewX: Double?
    var skewY: Double?
    var radius: Double?

    init(uri: String,
         width: Double,
         height: Double,
         x: Double,
         y: Double,
         skewX: Double?,
         skewY: Double?,
         radius: Double?) {
        self.uri = uri
        self.width = width
        self.height = height
        self.x = x
        self.y = y
        self.skewX = skewX ?? 0.0
        self.skewY = skewY ?? 0.0
        self.radius = radius ?? 0.0
    }
}



class TextLayer : Layer {
    var text: String
    var fontSize: Int?
    var fontFamily: String?
    var color: String
    var opacity: Double?
    var width: Double
    var height: Double
    var x: Double
    var y: Double
    var alignment: String?
    var bold: Bool?
    
    
    init(text: String,
         fontSize: Int,
         width: Double,
         height: Double,
         x: Double,
         y: Double,
         fontFamily: String?,
         color: String,
         opacity: Double?,
         alignment: String?,
         bold: Bool?) {
        self.text = text;
        self.fontSize = fontSize;
        self.fontFamily = fontFamily ?? "Helvetica";
        self.color = color;
        self.opacity = opacity ?? 1.0;
        self.width = width;
        self.height = height;
        self.x = x;
        self.y = y;
        self.alignment = alignment ?? "center";
        self.bold = bold ?? false;
    }
}
