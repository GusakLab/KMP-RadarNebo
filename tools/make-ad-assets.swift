// Draws the Google Ads image assets for the RadarNebo app campaign in the Play graphics' style
// (tools/make-play-assets.swift): the navy grid backdrop, a caption, and an app screen in a rounded phone frame.
// For each language it writes the three ratios App campaigns take, one image per scene:
//   landscape-<n>.png 1200 × 628 (1.91:1), square-<n>.png 1200 × 1200, portrait-<n>.png 1200 × 1500 (4:5).
// The screens are the Compose preview renders in docs/screenshots (demo data); their "DEMO DATA" status banner is
// cropped off the top so it doesn't read as part of the ad.
//
//   swiftc -O tools/make-ad-assets.swift -o /tmp/make-ad-assets && /tmp/make-ad-assets docs/screenshots app/android/ads
import CoreGraphics
import CoreText
import Foundation
import ImageIO
import UniformTypeIdentifiers

let cyan = CGColor(red: 0.31, green: 0.82, blue: 0.91, alpha: 1)
let white = CGColor(gray: 0.96, alpha: 1)

func color(_ c: CGColor, _ alpha: CGFloat) -> CGColor { c.copy(alpha: alpha)! }

/// The navy gradient with the map's faint grid, over all of [rect].
func backdrop(_ ctx: CGContext, _ rect: CGRect, grid: CGFloat) {
    let gradient = CGGradient(colorsSpace: CGColorSpaceCreateDeviceRGB(), colors: [
        CGColor(red: 0.07, green: 0.16, blue: 0.24, alpha: 1),
        CGColor(red: 0.02, green: 0.05, blue: 0.08, alpha: 1),
    ] as CFArray, locations: [0, 1])!
    ctx.drawLinearGradient(gradient, start: CGPoint(x: rect.midX, y: rect.maxY), end: CGPoint(x: rect.midX, y: rect.minY), options: [])
    ctx.setStrokeColor(color(cyan, 0.06))
    ctx.setLineWidth(3)
    for x in stride(from: rect.minX, through: rect.maxX, by: grid) {
        ctx.move(to: CGPoint(x: x, y: rect.minY)); ctx.addLine(to: CGPoint(x: x, y: rect.maxY))
    }
    for y in stride(from: rect.minY, through: rect.maxY, by: grid) {
        ctx.move(to: CGPoint(x: rect.minX, y: y)); ctx.addLine(to: CGPoint(x: rect.maxX, y: y))
    }
    ctx.strokePath()
}

func render(width: Int, height: Int, to url: URL, _ draw: (CGContext) -> Void) {
    let ctx = CGContext(data: nil, width: width, height: height, bitsPerComponent: 8, bytesPerRow: 0,
                        space: CGColorSpaceCreateDeviceRGB(), bitmapInfo: CGImageAlphaInfo.noneSkipLast.rawValue)!
    ctx.setShouldAntialias(true)
    draw(ctx)
    let dest = CGImageDestinationCreateWithURL(url as CFURL, UTType.png.identifier as CFString, 1, nil)!
    CGImageDestinationAddImage(dest, ctx.makeImage()!, nil)
    CGImageDestinationFinalize(dest)
}

/// Wrapped text in [rect], top-aligned; returns the height it used.
@discardableResult
func paragraph(_ ctx: CGContext, _ string: String, in rect: CGRect, size: CGFloat, weight: CGFloat, color c: CGColor,
               alignment: CTTextAlignment = .center) -> CGFloat {
    let font = CTFontCreateWithFontDescriptor(CTFontDescriptorCreateWithAttributes([
        kCTFontFamilyNameAttribute: "Helvetica Neue",
        kCTFontTraitsAttribute: [kCTFontWeightTrait: weight],
    ] as CFDictionary), size, nil)
    var align = alignment
    let style = withUnsafeBytes(of: &align) { bytes in
        CTParagraphStyleCreate([CTParagraphStyleSetting(spec: .alignment, valueSize: MemoryLayout<CTTextAlignment>.size, value: bytes.baseAddress!)], 1)
    }
    let attributed = NSAttributedString(string: string, attributes: [
        NSAttributedString.Key(kCTFontAttributeName as String): font,
        NSAttributedString.Key(kCTForegroundColorAttributeName as String): c,
        NSAttributedString.Key(kCTParagraphStyleAttributeName as String): style,
    ])
    let setter = CTFramesetterCreateWithAttributedString(attributed)
    let fit = CTFramesetterSuggestFrameSizeWithConstraints(setter, CFRange(), nil, CGSize(width: rect.width, height: .greatestFiniteMagnitude), nil)
    let frame = CTFramesetterCreateFrame(setter, CFRange(), CGPath(rect: CGRect(x: rect.minX, y: rect.maxY - ceil(fit.height), width: rect.width, height: ceil(fit.height)), transform: nil), nil)
    CTFrameDraw(frame, ctx)
    return ceil(fit.height)
}

/// The preview render with its status banner cropped off the top.
func loadScreen(_ url: URL) -> CGImage {
    guard let source = CGImageSourceCreateWithURL(url as CFURL, nil), let image = CGImageSourceCreateImageAtIndex(source, 0, nil) else {
        fatalError("Can't read \(url.path)")
    }
    let cut = image.width * 112 / 630
    return image.cropping(to: CGRect(x: 0, y: cut, width: image.width, height: image.height - cut))!
}

/// [screen] as a phone in a rounded frame [width] wide, centred on [centerX], from [top] down to [bottom]. A screen
/// taller than that keeps its top part, or its bottom part when [keepBottom], where that scene's point is.
func phone(_ ctx: CGContext, _ full: CGImage, centerX: CGFloat, top: CGFloat, bottom: CGFloat, width: CGFloat, keepBottom: Bool) {
    let fullHeight = width * CGFloat(full.height) / CGFloat(full.width)
    let height = min(fullHeight, top - bottom)
    let rows = Int(CGFloat(full.width) * height / width)
    let screen = full.cropping(to: CGRect(x: 0, y: keepBottom ? full.height - rows : 0, width: full.width, height: rows))!
    let rect = CGRect(x: centerX - width / 2, y: top - height, width: width, height: height)
    let border = width * 0.022
    let frame = rect.insetBy(dx: -border, dy: -border)
    let shape = CGPath(roundedRect: frame, cornerWidth: width * 0.078, cornerHeight: width * 0.078, transform: nil)
    ctx.saveGState()
    ctx.setShadow(offset: CGSize(width: 0, height: -12), blur: 60, color: CGColor(gray: 0, alpha: 0.6))
    ctx.addPath(shape)
    ctx.setFillColor(CGColor(red: 0.02, green: 0.04, blue: 0.06, alpha: 1))
    ctx.fillPath()
    ctx.restoreGState()
    ctx.addPath(shape)
    ctx.setStrokeColor(color(cyan, 0.35))
    ctx.setLineWidth(4)
    ctx.strokePath()
    ctx.saveGState()
    ctx.addPath(CGPath(roundedRect: rect, cornerWidth: width * 0.056, cornerHeight: width * 0.056, transform: nil))
    ctx.clip()
    ctx.interpolationQuality = .high
    ctx.draw(screen, in: rect)
    ctx.restoreGState()
}

struct Scene { let screen: String; var keepBottom = false; let title: [String: String]; let subtitle: [String: String] }

let scenes = [
    Scene(screen: "map-overview.png",
          title: ["uk": "Повітряні загрози над Україною", "en": "Air threats over Ukraine"],
          subtitle: ["uk": "Тривоги й цілі на одній живій карті", "en": "Alerts and targets on one live map"]),
    Scene(screen: "warning-radius.png", keepBottom: true,
          title: ["uk": "Стежте за важливими місцями", "en": "Watch the places you care about"],
          subtitle: ["uk": "Бачте, коли загроза в межах вашого радіуса", "en": "See when a threat comes within your radius"]),
    Scene(screen: "cinematic.png", keepBottom: true,
          title: ["uk": "Кінематографічний режим", "en": "Cinematic mode"],
          subtitle: ["uk": "Чиста повноекранна карта", "en": "A clean, full-screen view of the map"]),
]

let args = CommandLine.arguments
let screens = URL(fileURLWithPath: args.count > 1 ? args[1] : "docs/screenshots")
let out = URL(fileURLWithPath: args.count > 2 ? args[2] : "app/android/ads")

for language in ["uk", "en"] {
    let folder = out.appendingPathComponent(language)
    try FileManager.default.createDirectory(at: folder, withIntermediateDirectories: true)
    for (i, scene) in scenes.enumerated() {
        let screen = loadScreen(screens.appendingPathComponent(scene.screen))
        let title = scene.title[language]!, subtitle = scene.subtitle[language]!

        render(width: 1200, height: 628, to: folder.appendingPathComponent("landscape-\(i + 1).png")) { ctx in
            backdrop(ctx, CGRect(x: 0, y: 0, width: 1200, height: 628), grid: 78.5)
            let used = paragraph(ctx, title, in: CGRect(x: 70, y: 200, width: 600, height: 300), size: 54, weight: 0.4,
                                 color: white, alignment: .left)
            paragraph(ctx, subtitle, in: CGRect(x: 72, y: 100, width: 580, height: 500 - used - 28 - 100), size: 30, weight: 0,
                      color: cyan, alignment: .left)
            phone(ctx, screen, centerX: 930, top: 590, bottom: 38, width: 300, keepBottom: scene.keepBottom)
        }

        render(width: 1200, height: 1200, to: folder.appendingPathComponent("square-\(i + 1).png")) { ctx in
            backdrop(ctx, CGRect(x: 0, y: 0, width: 1200, height: 1200), grid: 100)
            let used = paragraph(ctx, title, in: CGRect(x: 80, y: 900, width: 1040, height: 230), size: 62, weight: 0.4, color: white)
            paragraph(ctx, subtitle, in: CGRect(x: 100, y: 800, width: 1000, height: 1130 - used - 20 - 800), size: 36, weight: 0, color: cyan)
            phone(ctx, screen, centerX: 600, top: 1130 - used - 20 - 60 - 60, bottom: 50, width: 560, keepBottom: scene.keepBottom)
        }

        render(width: 1200, height: 1500, to: folder.appendingPathComponent("portrait-\(i + 1).png")) { ctx in
            backdrop(ctx, CGRect(x: 0, y: 0, width: 1200, height: 1500), grid: 100)
            let used = paragraph(ctx, title, in: CGRect(x: 80, y: 1200, width: 1040, height: 230), size: 68, weight: 0.4, color: white)
            paragraph(ctx, subtitle, in: CGRect(x: 100, y: 1080, width: 1000, height: 1430 - used - 24 - 1080), size: 40, weight: 0, color: cyan)
            phone(ctx, screen, centerX: 600, top: 1430 - used - 24 - 70 - 70, bottom: 60, width: 640, keepBottom: scene.keepBottom)
        }
    }
}
