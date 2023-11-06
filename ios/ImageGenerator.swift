import Accelerate
import Foundation


@objc(ImageGenerator)
class ImageGenerator: NSObject {
    
    func layerFactory(layer: NSDictionary) -> Layer {
        if layer["uri"] != nil {
            return PictureLayer(
                uri: layer["uri"] as! String,
                width: layer["width"] as! Float,
                height: layer["height"] as! Float,
                x: layer["x"] as! Float,
                y: layer["y"] as! Float,
                skewX: layer["skewX"] as? CGFloat ?? 0,
                skewY: layer["skewY"] as? CGFloat ?? 0,
                radius: layer["radius"] as? CGFloat ?? 0
            );
        } else {
            return TextLayer(
                text: layer["text"] as! String,
                fontSize: layer["fontSize"] as! Int,
                width: layer["width"] as! Float,
                height: layer["height"] as! Float,
                x: layer["x"] as! Float,
                y: layer["y"] as! Float,
                fontFamily: layer["fontFamily"] as? String,
                color: layer["color"] as? [Float],
                bold: layer["bold"] as? Bool ?? false
            )
        }
    }
    
    func getImageByUri(uri: String) -> UIImage? {
        if(uri.contains("http")) {
            let url = URL(string: uri);
            let data = try? Data(contentsOf: url!)
            return UIImage(data: data!);
        } else {
            return UIImage(named: uri)
        }
    }
    
    func drawPictureLayer(layer: PictureLayer) {
        let imageLayer = getImageByUri(uri: layer.uri)
        if imageLayer != nil {
            let radiusedImage = imageLayer?.imageWithRadius(radius: layer.radius)
            let skewedImage = radiusedImage!.imageSkewed(horizontalAngle: layer.skewX, verticalAngle: layer.skewY)

            let scaleX = 1.0 / cos(layer.skewY * .pi / 180)
            let scaleY = 1.0 / cos(layer.skewX * .pi / 180)
            
            UIGraphicsBeginImageContextWithOptions(radiusedImage!.size, false, radiusedImage!.scale)
            
            if let context = UIGraphicsGetCurrentContext() {
                context.scaleBy(x: scaleX, y: scaleY)
                skewedImage?.draw(at: .zero)
                let restoredImage = UIGraphicsGetImageFromCurrentImageContext()
                UIGraphicsEndImageContext()

                restoredImage?.draw(in: CGRect(
                    origin: CGPoint(x: CGFloat(layer.x), y: CGFloat(layer.y)),
                    size: CGSize(width: CGFloat(layer.width), height: CGFloat(layer.height))
                ))
            }
        }
    }
    
    func drawTextLayer(layer: TextLayer) {
        let textColor = UIColor(
            red: CGFloat(layer.color[0]),
            green: CGFloat(layer.color[1]),
            blue: CGFloat(layer.color[2]),
            alpha: CGFloat(layer.color[3])
        );
        let textFont = UIFont(name: layer.fontFamily, size: CGFloat(layer.fontSize))!
        let boldFontDescriptor = textFont.fontDescriptor.withSymbolicTraits(.traitBold)!
        let boldSatoshiFont = layer.bold ? UIFont(descriptor: boldFontDescriptor, size: CGFloat(layer.fontSize)) : textFont
        let text = NSString(string: layer.text);
        
        let maxSize = CGSize(width: CGFloat(layer.width), height: CGFloat(layer.height))
        let textRect = text.boundingRect(
            with: maxSize,
            options: [.usesLineFragmentOrigin, .usesFontLeading],
            attributes: [NSAttributedString.Key.font: boldSatoshiFont],
            context: nil
        )

        let textX = CGFloat(layer.x) + (maxSize.width - textRect.size.width) / 2
        let textY = CGFloat(layer.y) + (maxSize.height - textRect.size.height) / 2
        
        let paragraphStyle = NSMutableParagraphStyle()
        paragraphStyle.alignment = .center
        
        let textFontAttributes: [NSAttributedString.Key: Any] = [
            .font: boldSatoshiFont,
            .foregroundColor: textColor,
            .paragraphStyle: paragraphStyle
        ]

        text.draw(in: CGRect(
            origin: CGPoint(x: textX, y: textY),
            size: textRect.size),
            withAttributes: textFontAttributes
        )
    }

    
    func drawLayer(layer: Layer) {
        if layer is PictureLayer {
            drawPictureLayer(layer: layer as! PictureLayer);
        } else {
            drawTextLayer(layer: layer as! TextLayer);
        }
    }

    @objc(generate:withConfig:withResolver:withRejecter:)
    func generate(layers: NSDictionary,
                  config: NSDictionary,
                  resolve:RCTPromiseResolveBlock,
                  reject:RCTPromiseRejectBlock) {
        let width = config["width"] as! Float;
        let height = config["height"] as! Float;
        let filePath = config["filePath"] as! String;
        let base64 = config["base64"] as? Bool ?? false;
        
        let newSize = CGSize(width: CGFloat(width), height: CGFloat(height))
        let scale = UIScreen.main.scale

        UIGraphicsBeginImageContextWithOptions(newSize, false, scale)
        
        let layers: [Layer] = (layers["layers"] as! [NSDictionary]).map { (item) -> Layer in
         return layerFactory(layer: item)
        }
        
        for layer in layers {
            drawLayer(layer: layer);
        }
        
        let newImage = UIGraphicsGetImageFromCurrentImageContext()
        UIGraphicsEndImageContext()
        
        let imageURL = URL(fileURLWithPath: filePath)
        
        do {
            if(base64) {
                let jpegData = newImage!.pngData()!
                let base64Data = jpegData.base64EncodedString()
                let imgBase64 = "data:image/png;base64,\(base64Data)"
                resolve(imgBase64)
            } else {
                let pngData = newImage!.pngData();
                try pngData?.write(to: imageURL);
                resolve(imageURL.absoluteString);
            }
 
        } catch {
            reject("error save", "idk", error)
        }
        
        
    
    }
}

extension UIImage {
    func imageWithRadius(radius: CGFloat) -> UIImage? {
        let newSize = CGSize(width: self.size.width, height: self.size.height)
        UIGraphicsBeginImageContextWithOptions(newSize, false, self.scale)

        if let context = UIGraphicsGetCurrentContext() {
            let rect = CGRect(origin: .zero, size: newSize)
            UIBezierPath(roundedRect: rect, cornerRadius: radius).addClip()
            self.draw(in: rect)
            let radiusedImage = UIGraphicsGetImageFromCurrentImageContext()
            UIGraphicsEndImageContext()

            return radiusedImage
        }
        return nil
    }
    func imageSkewed(horizontalAngle: CGFloat, verticalAngle: CGFloat) -> UIImage? {
        let radiansHorizontal = horizontalAngle * .pi / 180
        let radiansVertical = verticalAngle * .pi / 180

        let transform = CGAffineTransform(a: 1.0, b: tan(radiansHorizontal), c: tan(radiansVertical), d: 1.0, tx: 0, ty: 0)

        let newSize = self.size.applying(transform)
        UIGraphicsBeginImageContextWithOptions(newSize, false, self.scale)

        if let context = UIGraphicsGetCurrentContext() {
            context.concatenate(transform)
            self.draw(at: .zero)
            let skewedImage = UIGraphicsGetImageFromCurrentImageContext()
            UIGraphicsEndImageContext()

            return skewedImage
        }
        return nil
    }
}
