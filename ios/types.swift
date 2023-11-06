protocol Layer {}

class PictureLayer: Layer {
    var uri: String
    var width: Float
    var height: Float
    var x: Float
    var y: Float
    var skewX: CGFloat
    var skewY: CGFloat
    var radius: CGFloat

    init(uri: String, width: Float, height: Float, x: Float, y: Float, skewX: CGFloat = 0, skewY: CGFloat = 0, radius: CGFloat = 0) {
        self.uri = uri
        self.width = width
        self.height = height
        self.x = x
        self.y = y
        self.skewX = skewX
        self.skewY = skewY
        self.radius = radius
    }
}



class TextLayer : Layer {
    var text: String
    var fontSize: Int
    var fontFamily: String
    var color: [Float]
    var width: Float
    var height: Float
    var x: Float
    var y: Float
    var bold: Bool
    
    
    init(text: String,
         fontSize: Int,
         width: Float,
         height: Float,
         x: Float,
         y: Float,
         fontFamily: String?,
         color: [Float]?,
         bold: Bool = false) {
        self.text = text;
        self.fontSize = fontSize;
        self.fontFamily = fontFamily ?? "Helvetica";
        self.color = color ?? [255, 255, 255, 1];
        self.width = width;
        self.height = height;
        self.x = x;
        self.y = y;
        self.bold = bold
    }
}
