// Draws the Google Play graphics for MissionControl-Nebo (mission-control/android) in the server console icon's
// style (tools/make-icons.swift): the 512 × 512 store icon, full-bleed since Play rounds the corners itself, and the
// 1024 × 500 feature graphic. It also frames the app captures in listings/<language>/captures/<n>.png into 1080 × 1920
// (9:16) phone screenshots in listings/<language>/phone-screenshots/<n>.png, captioned from that language's
// screenshot-captions.txt (one "title|subtitle" line per capture).
//
//   swiftc -O tools/make-play-assets.swift -o /tmp/make-play-assets && /tmp/make-play-assets mission-control/android/play
import CoreGraphics
import CoreText
import Foundation
import ImageIO
import UniformTypeIdentifiers

let cyan = CGColor(red: 0.31, green: 0.82, blue: 0.91, alpha: 1)
let red = CGColor(red: 1.0, green: 0.35, blue: 0.37, alpha: 1)
let amber = CGColor(red: 0.96, green: 0.65, blue: 0.14, alpha: 1)
let green = CGColor(red: 0.27, green: 0.82, blue: 0.48, alpha: 1)

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

func blip(_ ctx: CGContext, _ at: CGPoint, _ c: CGColor, _ r: CGFloat) {
    ctx.setFillColor(color(c, 0.22))
    ctx.fillEllipse(in: CGRect(x: at.x - r * 2.4, y: at.y - r * 2.4, width: r * 4.8, height: r * 4.8))
    ctx.setFillColor(c)
    ctx.fillEllipse(in: CGRect(x: at.x - r, y: at.y - r, width: r * 2, height: r * 2))
}

/// The server stack and its waves, as in make-icons.swift's serverIcon, in a 1024 × 1024 space.
func serverGlyph(_ ctx: CGContext) {
    let source = CGPoint(x: 512, y: 600)
    ctx.setLineCap(.round)
    for (i, r) in ([95, 175, 255] as [CGFloat]).enumerated() {
        ctx.setStrokeColor(color(cyan, 0.9 - CGFloat(i) * 0.25))
        ctx.setLineWidth(22)
        ctx.addArc(center: source, radius: r, startAngle: 35 * .pi / 180, endAngle: 145 * .pi / 180, clockwise: false)
        ctx.strokePath()
    }
    blip(ctx, source, cyan, 26)
    blip(ctx, CGPoint(x: 700, y: 800), red, 18)
    for (i, y) in ([430, 310, 190] as [CGFloat]).enumerated() {
        let path = CGPath(roundedRect: CGRect(x: 272, y: y, width: 480, height: 96), cornerWidth: 22, cornerHeight: 22, transform: nil)
        ctx.addPath(path)
        ctx.setFillColor(CGColor(red: 0.09, green: 0.2, blue: 0.29, alpha: 1))
        ctx.fillPath()
        ctx.addPath(path)
        ctx.setStrokeColor(color(cyan, 0.55))
        ctx.setLineWidth(6)
        ctx.strokePath()
        for (j, led) in [green, cyan, i == 0 ? amber : green].enumerated() {
            ctx.setFillColor(led)
            ctx.fillEllipse(in: CGRect(x: 318 + CGFloat(j) * 44 - 11, y: y + 37, width: 22, height: 22))
        }
        ctx.setStrokeColor(color(cyan, 0.4))
        ctx.setLineWidth(8)
        for k in 0..<2 {
            let ly = y + 34 + CGFloat(k) * 28
            ctx.move(to: CGPoint(x: 520, y: ly)); ctx.addLine(to: CGPoint(x: 712, y: ly))
        }
        ctx.strokePath()
    }
}

func text(_ ctx: CGContext, _ string: String, at: CGPoint, size: CGFloat, weight: CGFloat, color c: CGColor) {
    let font = CTFontCreateWithFontDescriptor(CTFontDescriptorCreateWithAttributes([
        kCTFontFamilyNameAttribute: "Helvetica Neue",
        kCTFontTraitsAttribute: [kCTFontWeightTrait: weight],
    ] as CFDictionary), size, nil)
    let line = CTLineCreateWithAttributedString(NSAttributedString(string: string, attributes: [
        NSAttributedString.Key(kCTFontAttributeName as String): font,
        NSAttributedString.Key(kCTForegroundColorAttributeName as String): c,
    ]))
    ctx.textPosition = at
    CTLineDraw(line, ctx)
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

let out = URL(fileURLWithPath: CommandLine.arguments.count > 1 ? CommandLine.arguments[1] : ".")
try FileManager.default.createDirectory(at: out, withIntermediateDirectories: true)

render(width: 512, height: 512, to: out.appendingPathComponent("icon-512.png")) { ctx in
    backdrop(ctx, CGRect(x: 0, y: 0, width: 512, height: 512), grid: 64)
    // The glyph a little larger than on the desktop icon, which leaves room for its own rounded square.
    ctx.translateBy(x: -40, y: -36)
    ctx.scaleBy(x: 0.58, y: 0.58)
    serverGlyph(ctx)
}

render(width: 1024, height: 500, to: out.appendingPathComponent("feature-graphic.png")) { ctx in
    backdrop(ctx, CGRect(x: 0, y: 0, width: 1024, height: 500), grid: 62.5)
    ctx.saveGState()
    ctx.translateBy(x: -50, y: -30)
    ctx.scaleBy(x: 0.52, y: 0.52)
    serverGlyph(ctx)
    ctx.restoreGState()
    text(ctx, "MissionControl-Nebo", at: CGPoint(x: 470, y: 262), size: 48, weight: 0.4, color: CGColor(gray: 0.95, alpha: 1))
    text(ctx, "RadarNebo server monitor", at: CGPoint(x: 472, y: 208), size: 30, weight: 0, color: cyan)
    blip(ctx, CGPoint(x: 482, y: 160), green, 7)
    text(ctx, "/health", at: CGPoint(x: 502, y: 150), size: 26, weight: 0, color: color(cyan, 0.6))
}

/// Centred, wrapped text in [rect], top-aligned; returns the height it used.
@discardableResult
func paragraph(_ ctx: CGContext, _ string: String, in rect: CGRect, size: CGFloat, weight: CGFloat, color c: CGColor) -> CGFloat {
    let font = CTFontCreateWithFontDescriptor(CTFontDescriptorCreateWithAttributes([
        kCTFontFamilyNameAttribute: "Helvetica Neue",
        kCTFontTraitsAttribute: [kCTFontWeightTrait: weight],
    ] as CFDictionary), size, nil)
    var alignment = CTTextAlignment.center
    let style = withUnsafeBytes(of: &alignment) { bytes in
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

func loadImage(_ url: URL) -> CGImage? {
    guard let source = CGImageSourceCreateWithURL(url as CFURL, nil) else { return nil }
    return CGImageSourceCreateImageAtIndex(source, 0, nil)
}

let listings = out.appendingPathComponent("listings")
for language in (try? FileManager.default.contentsOfDirectory(atPath: listings.path))?.sorted() ?? [] {
    let folder = listings.appendingPathComponent(language)
    guard let captions = try? String(contentsOf: folder.appendingPathComponent("screenshot-captions.txt"), encoding: .utf8) else { continue }
    let shots = folder.appendingPathComponent("phone-screenshots")
    try FileManager.default.createDirectory(at: shots, withIntermediateDirectories: true)
    for (i, line) in captions.split(separator: "\n").enumerated() {
        guard let capture = loadImage(folder.appendingPathComponent("captures/\(i + 1).png")) else { continue }
        let parts = line.split(separator: "|", maxSplits: 1).map(String.init)
        render(width: 1080, height: 1920, to: shots.appendingPathComponent("\(i + 1).png")) { ctx in
            backdrop(ctx, CGRect(x: 0, y: 0, width: 1080, height: 1920), grid: 90)
            let used = paragraph(ctx, parts[0], in: CGRect(x: 70, y: 1500, width: 940, height: 330), size: 64, weight: 0.4,
                                 color: CGColor(gray: 0.96, alpha: 1))
            if parts.count > 1 {
                paragraph(ctx, parts[1], in: CGRect(x: 90, y: 1300, width: 900, height: 1830 - used - 24 - 1300), size: 36, weight: 0, color: cyan)
            }
            // The capture as a phone screen, bleeding off the bottom edge.
            let width: CGFloat = 820
            let height = width * CGFloat(capture.height) / CGFloat(capture.width)
            let screen = CGRect(x: (1080 - width) / 2, y: 1510 - height, width: width, height: height)
            let frame = screen.insetBy(dx: -18, dy: -18)
            let shape = CGPath(roundedRect: frame, cornerWidth: 64, cornerHeight: 64, transform: nil)
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
            ctx.addPath(CGPath(roundedRect: screen, cornerWidth: 46, cornerHeight: 46, transform: nil))
            ctx.clip()
            ctx.interpolationQuality = .high
            ctx.draw(capture, in: screen)
            ctx.restoreGState()
        }
    }
}
