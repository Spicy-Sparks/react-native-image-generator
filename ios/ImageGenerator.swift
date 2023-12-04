import Accelerate
import Foundation


@objc(ImageGenerator)
class ImageGenerator: NSObject {
    
    func layerFactory(layer: NSDictionary) -> Layer {
        if layer["uri"] != nil {
            return PictureLayer(
                uri: layer["uri"] as? String ?? "",
                width: layer["width"] as? Double ?? 0.0,
                height: layer["height"] as? Double ?? 0.0,
                x: layer["x"] as? Double ?? 0.0,
                y: layer["y"] as? Double ?? 0.0,
                skewX: layer["skewX"] as? Double ?? 0.0,
                skewY: layer["skewY"] as? Double ?? 0.0,
                radius: layer["radius"] as? Double ?? 0.0
            );
        } else {
            return TextLayer(
                text: layer["text"] as? String ?? "",
                fontSize: layer["fontSize"] as? Int ?? 0,
                width: layer["width"] as? Double ?? 0.0,
                height: layer["height"] as? Double ?? 0.0,
                x: layer["x"] as? Double ?? 0.0,
                y: layer["y"] as? Double ?? 0.0,
                fontFamily: layer["fontFamily"] as? String ?? "",
                color: layer["color"] as? String ?? "#FFFFFF",
                opacity: layer["opacity"] as? Double ?? 0.0,
                alignment: layer["alignment"] as? String ?? "center",
                bold: layer["bold"] as? Bool ?? false
            )
        }
    }
    
    func getImageByUri(uri: String) -> UIImage? {
        if uri.contains("http"), let url = URL(string: uri), let data = try? Data(contentsOf: url), let image = UIImage(data: data) {
            return image
        } else if let localImage = UIImage(named: uri) {
            return localImage
        } else {
            return nil
        }

    }
    
    func drawPictureLayer(layer: PictureLayer) {
        let imageLayer = getImageByUri(uri: layer.uri)
        if imageLayer != nil {
            let radiusedImage = imageLayer?.imageWithRadius(radius: CGFloat(layer.radius ?? 0))
            let skewedImage = radiusedImage?.imageSkewed(horizontalAngle: CGFloat(layer.skewX ?? 0), verticalAngle: CGFloat(layer.skewY ?? 0))

            let scaleX = 1.0 / cos((layer.skewY ?? 0) * .pi / 180)
            let scaleY = 1.0 / cos((layer.skewX ?? 0) * .pi / 180)

            UIGraphicsBeginImageContextWithOptions(radiusedImage?.size ?? CGSizeZero, false, radiusedImage?.scale ?? CGFloat(0.0))

            if let context = UIGraphicsGetCurrentContext() {
                context.scaleBy(x: CGFloat(scaleX), y: CGFloat(scaleY))
                skewedImage?.draw(at: .zero)

                if let restoredImage = UIGraphicsGetImageFromCurrentImageContext() {
                    UIGraphicsEndImageContext()

                    restoredImage.draw(in: CGRect(
                        origin: CGPoint(x: CGFloat(layer.x), y: CGFloat(layer.y)),
                        size: CGSize(width: CGFloat(layer.width), height: CGFloat(layer.height))
                    ))
                } else {
                    UIGraphicsEndImageContext()
                }
            } else {
                UIGraphicsEndImageContext()
            }
        }
    }
    
    //            if (layer.alignment == "left") {
    //                .left
    //            } else if (layer.alignment == "right") {
    //                .right
    //            } else {
    //                .center
    //            }

    func drawTextLayer(layer: TextLayer) {
        let rgbColor = hexToRgb(layer.color)
        let textColor = UIColor(
            red: CGFloat(Float(rgbColor[0]) / 255.0),
            green: CGFloat(Float(rgbColor[1]) / 255.0),
            blue: CGFloat(Float(rgbColor[2]) / 255.0),
            alpha: CGFloat(Float(layer.opacity ?? 1.0))
        )
            
        let textFont = UIFont(name: layer.fontFamily ?? "", size: CGFloat(layer.fontSize ?? 0))
        let boldFontDescriptor = textFont?.fontDescriptor.withSymbolicTraits(.traitBold) ?? nil
        let boldSatoshiFont = layer.bold ?? false ? UIFont(descriptor: boldFontDescriptor ?? UIFontDescriptor(), size: CGFloat(layer.fontSize ?? 0)) : textFont
        let text = NSString(string: layer.text);
        
        let maxSize = CGSize(width: CGFloat(layer.width), height: CGFloat(layer.height))
        let textRect = text.boundingRect(
            with: maxSize,
            options: [.usesLineFragmentOrigin, .usesFontLeading],
            attributes: [NSAttributedString.Key.font: boldSatoshiFont ?? textFont ?? UIFont()],
            context: nil
        )
        
        let textX: CGFloat = {
            switch layer.alignment {
                case "center": return CGFloat(layer.x) + (maxSize.width - textRect.size.width) / 2
                case "left": return CGFloat(layer.x)
                case "right": return CGFloat(layer.x) + maxSize.width - textRect.size.width
                default: return CGFloat(layer.x) + (maxSize.width - textRect.size.width) / 2
            }
        }()
        let textY = CGFloat(layer.y) + (maxSize.height - textRect.size.height) / 2
        
        let paragraphStyle = NSMutableParagraphStyle()
        paragraphStyle.alignment = .center

        let textFontAttributes: [NSAttributedString.Key: Any] = [
            .font: boldSatoshiFont ?? textFont ?? UIFont(),
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
            drawPictureLayer(layer: layer as? PictureLayer ?? PictureLayer(uri: "", width: 0.0, height: 0.0, x: 0.0, y: 0.0, skewX: 0.0, skewY: 0.0, radius: 0.0));
        } else {
            drawTextLayer(layer: layer as? TextLayer ?? TextLayer(text: "", fontSize: 0, width: 0.0, height: 0.0, x: 0.0, y: 0.0, fontFamily: "", color: "#FFFFFF", opacity: 0.0, alignment: "center", bold: false));
        }
    }

    @objc(generate:withConfig:withResolver:withRejecter:)
    func generate(layers: NSDictionary,
                  config: NSDictionary,
                  resolve:RCTPromiseResolveBlock,
                  reject:RCTPromiseRejectBlock) {
        let width = config["width"] as? Float ?? 0.0;
        let height = config["height"] as? Float ?? 0.0;
        let filePath = config["filePath"] as? String ?? "";
        let base64 = config["base64"] as? Bool ?? false;
        
        let newSize = CGSize(width: CGFloat(width), height: CGFloat(height))
        let scale = UIScreen.main.scale

        UIGraphicsBeginImageContextWithOptions(newSize, false, scale)
        
        let layers: [Layer] = (layers["layers"] as? [NSDictionary] ?? []).map { (item) -> Layer in
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
                let jpegData = newImage?.pngData()
                let base64Data = jpegData?.base64EncodedString()
                let imgBase64 = "data:image/png;base64,\(base64Data ?? "")"
                resolve(imgBase64)
            } else {
                let pngData = newImage?.pngData();
                try pngData?.write(to: imageURL);
                resolve(imageURL.absoluteString);
            }
 
        } catch {
            reject("error save", "idk", error)
        }
    }
}

func hexToRgb(_ hex: String) -> [Int] {
  let regex = try? NSRegularExpression(pattern: "^#?([a-f\\d]{2})([a-f\\d]{2})([a-f\\d]{2})$", options: .caseInsensitive)
  let range = NSRange(location: 0, length: hex.count)
  let result = regex?.firstMatch(in: hex, options: [], range: range)
  
  if let result = result {
    let r = Int((hex as NSString).substring(with: result.range(at: 1)), radix: 16) ?? 255
    let g = Int((hex as NSString).substring(with: result.range(at: 2)), radix: 16) ?? 255
    let b = Int((hex as NSString).substring(with: result.range(at: 3)), radix: 16) ?? 255
    return [r, g, b]
  } else {
    return [255, 255, 255]
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
