// Frames RadarNebo simulator captures into captioned App Store screenshots and draws the product page header.
//
//   swiftc -O tools/make-appstore-assets.swift -o /tmp/make-appstore-assets
//   /tmp/make-appstore-assets <captures dir with iphone/ and ipad/> <icon.png> <output dir>
//
// Output: <output>/<locale>/<iphone|ipad>/<n>.png (no alpha) and <output>/<locale>/header.png (3840 × 1646).
import CoreGraphics
import CoreText
import Foundation
import ImageIO
import UniformTypeIdentifiers

let shots = ["map-dark", "favorites-dark", "cinematic-dark", "map-light", "settings-dark"]
let captions: [String: [(String, String)]] = [
    "en-US": [
        ("Air threats over Ukraine", "Live alerts by region on one map"),
        ("Watch the places you care about", "See when a threat comes within your radius"),
        ("Cinematic mode", "A clean, full-screen view of the map"),
        ("Light or dark", "Clear to read at any time of day"),
        ("Make it yours", "Themes, color schemes and map backgrounds"),
    ],
    "uk": [
        ("Повітряні загрози над Україною", "Тривоги за регіонами на одній карті"),
        ("Стежте за важливими місцями", "Бачте, коли загроза в межах вашого радіуса"),
        ("Кінематографічний режим", "Чиста повноекранна карта"),
        ("Світла чи темна", "Зручно читати в будь-який час доби"),
        ("Налаштуйте під себе", "Теми, кольорові схеми та фони карти"),
    ],
    "nl-NL": [
        ("Luchtdreiging boven Oekraïne", "Live alarmen per regio op één kaart"),
        ("Houd belangrijke plekken in de gaten", "Zie wanneer een dreiging binnen je straal komt"),
        ("Filmische modus", "Een strakke kaart op het volledige scherm"),
        ("Licht of donker", "Goed leesbaar op elk moment van de dag"),
        ("Maak het van jou", "Thema's, kleurenschema's en kaartachtergronden"),
    ],
]
let sizes: [(name: String, w: Int, h: Int, top: CGFloat, shotW: CGFloat, title: CGFloat, sub: CGFloat)] = [
    ("iphone", 1320, 2868, 560, 1040, 92, 54),
    ("ipad", 2064, 2752, 440, 1680, 96, 56),
]

func load(_ path: String) -> CGImage {
    let src = CGImageSourceCreateWithURL(URL(fileURLWithPath: path) as CFURL, nil)!
    return CGImageSourceCreateImageAtIndex(src, 0, nil)!
}
func canvas(_ w: Int, _ h: Int) -> CGContext {
    CGContext(data: nil, width: w, height: h, bitsPerComponent: 8, bytesPerRow: 0, space: CGColorSpace(name: CGColorSpace.sRGB)!,
              bitmapInfo: CGImageAlphaInfo.noneSkipLast.rawValue)!
}
func save(_ ctx: CGContext, _ path: String) {
    try! FileManager.default.createDirectory(atPath: (path as NSString).deletingLastPathComponent, withIntermediateDirectories: true)
    let d = CGImageDestinationCreateWithURL(URL(fileURLWithPath: path) as CFURL, UTType.png.identifier as CFString, 1, nil)!
    CGImageDestinationAddImage(d, ctx.makeImage()!, nil)
    precondition(CGImageDestinationFinalize(d))
}
func backdrop(_ ctx: CGContext, _ w: CGFloat, _ h: CGFloat) {
    let g = CGGradient(colorsSpace: CGColorSpace(name: CGColorSpace.sRGB)!, colors: [
        CGColor(red: 0.13, green: 0.20, blue: 0.09, alpha: 1), CGColor(red: 0.07, green: 0.07, blue: 0.07, alpha: 1),
    ] as CFArray, locations: [0, 1])!
    ctx.drawLinearGradient(g, start: CGPoint(x: w / 2, y: h), end: CGPoint(x: w / 2, y: 0), options: [])
}
/// Draws wrapped, centered [text] from the top of [rect] and returns the height it used.
func text(_ ctx: CGContext, _ s: String, _ font: String, _ size: CGFloat, _ c: CGColor, _ rect: CGRect, center: Bool = true) -> CGFloat {
    let f = CTFontCreateWithName(font as CFString, size, nil)
    var a: CTTextAlignment = center ? .center : .left
    let style = withUnsafePointer(to: &a) { p in
        CTParagraphStyleCreate([CTParagraphStyleSetting(spec: .alignment, valueSize: MemoryLayout<CTTextAlignment>.size, value: p)], 1)
    }
    let str = NSAttributedString(string: s, attributes: [
        NSAttributedString.Key(kCTFontAttributeName as String): f,
        NSAttributedString.Key(kCTForegroundColorAttributeName as String): c,
        NSAttributedString.Key(kCTParagraphStyleAttributeName as String): style,
    ])
    let fs = CTFramesetterCreateWithAttributedString(str)
    let used = CTFramesetterSuggestFrameSizeWithConstraints(fs, CFRange(), nil, CGSize(width: rect.width, height: .greatestFiniteMagnitude), nil)
    let box = CGRect(x: rect.minX, y: rect.maxY - ceil(used.height), width: rect.width, height: ceil(used.height))
    CTFrameDraw(CTFramesetterCreateFrame(fs, CFRange(), CGPath(rect: box, transform: nil), nil), ctx)
    return ceil(used.height)
}
func rounded(_ ctx: CGContext, _ img: CGImage, _ r: CGRect, _ radius: CGFloat) {
    ctx.saveGState()
    ctx.setShadow(offset: CGSize(width: 0, height: -12), blur: 60, color: CGColor(red: 0, green: 0, blue: 0, alpha: 0.6))
    ctx.addPath(CGPath(roundedRect: r, cornerWidth: radius, cornerHeight: radius, transform: nil))
    ctx.setFillColor(CGColor(red: 0, green: 0, blue: 0, alpha: 1)); ctx.fillPath()
    ctx.restoreGState()
    ctx.saveGState()
    ctx.addPath(CGPath(roundedRect: r, cornerWidth: radius, cornerHeight: radius, transform: nil)); ctx.clip()
    ctx.interpolationQuality = .high
    ctx.draw(img, in: r)
    ctx.restoreGState()
}

let args = CommandLine.arguments
let (captures, iconPath, out) = (args[1], args[2], args[3])
let white = CGColor(red: 1, green: 1, blue: 1, alpha: 1), soft = CGColor(red: 0.86, green: 0.93, blue: 0.80, alpha: 1), brand = CGColor(red: 0.55, green: 0.76, blue: 0.29, alpha: 1)

for (locale, lines) in captions { for size in sizes { for (i, shot) in shots.enumerated() {
    let (w, h) = (CGFloat(size.w), CGFloat(size.h))
    let ctx = canvas(size.w, size.h)
    backdrop(ctx, w, h)
    let img = load("\(captures)/\(size.name)/\(shot).png")
    let area = CGRect(x: 90, y: h - size.top, width: w - 180, height: size.top - 110)
    let used = text(ctx, lines[i].0, "HelveticaNeue-Bold", size.title, white, area)
    _ = text(ctx, lines[i].1, "HelveticaNeue", size.sub, soft, CGRect(x: area.minX, y: area.minY, width: area.width, height: area.height - used - 26))
    let sh = size.shotW * CGFloat(img.height) / CGFloat(img.width)
    rounded(ctx, img, CGRect(x: (w - size.shotW) / 2, y: h - size.top - sh, width: size.shotW, height: sh), 64)
    save(ctx, "\(out)/\(locale)/\(size.name)/\(i + 1).png")
} } }

// Mac: rendered windows, framed the same way on the 2880 × 1800 canvas the Mac App Store takes.
let macShots = ["map", "shahed", "missile", "cinematic"]
let macCaptions: [String: [(String, String)]] = [
    "en-US": [
        ("Air threats over Ukraine", "Alerts by region, on your big screen"),
        ("Follow tracked targets", "Estimated paths and threat types at a glance"),
        ("See where a target may head", "Forecasts are estimates, not official warnings"),
        ("Cinematic mode", "A clean, full-screen view of the map"),
    ],
    "uk": [
        ("Повітряні загрози над Україною", "Тривоги за регіонами на великому екрані"),
        ("Стежте за цілями", "Орієнтовні маршрути й типи загроз одразу"),
        ("Куди може рухатися ціль", "Прогнози є оцінками, а не офіційними попередженнями"),
        ("Кінематографічний режим", "Чиста повноекранна карта"),
    ],
    "nl-NL": [
        ("Luchtdreiging boven Oekraïne", "Alarmen per regio op je grote scherm"),
        ("Volg gevolgde doelen", "Geschatte routes en dreigingstypes in één oogopslag"),
        ("Zie waar een doel heen kan gaan", "Voorspellingen zijn schattingen, geen officiële waarschuwingen"),
        ("Filmische modus", "Een strakke kaart op het volledige scherm"),
    ],
]
for (locale, lines) in macCaptions { for (i, shot) in macShots.enumerated() {
    let (w, h) = (CGFloat(2880), CGFloat(1800))
    let ctx = canvas(2880, 1800)
    backdrop(ctx, w, h)
    let area = CGRect(x: 120, y: h - 300, width: w - 240, height: 240)
    let used = text(ctx, lines[i].0, "HelveticaNeue-Bold", 84, white, area)
    _ = text(ctx, lines[i].1, "HelveticaNeue", 48, soft, CGRect(x: area.minX, y: area.minY, width: area.width, height: area.height - used - 16))
    let img = load("\(captures)/mac/\(shot).png")
    let sw: CGFloat = 2280, sh = sw * CGFloat(img.height) / CGFloat(img.width)
    rounded(ctx, img, CGRect(x: (w - sw) / 2, y: h - 300 - sh + 40, width: sw, height: sh), 36)
    // These come from the app's preview scenario, so they say so rather than pass for a live situation.
    let sample = ["en-US": "Sample data shown", "uk": "Показано приклад даних", "nl-NL": "Voorbeeldgegevens getoond"][locale]!
    _ = text(ctx, sample, "HelveticaNeue-Medium", 36, soft, CGRect(x: 2520, y: 60, width: 340, height: 120), center: false)
    save(ctx, "\(out)/\(locale)/mac/\(i + 1).png")
} }

// Header: the dark map's Ukraine band on the right, the icon and name on the left.
let headerLines = [
    "en-US": "Air threats over Ukraine, live on one map",
    "uk": "Повітряні загрози над Україною на одній живій карті",
    "nl-NL": "Luchtdreiging boven Oekraïne, live op één kaart",
]
let map = load("\(captures)/ipad/map-dark.png")
let crop = map.cropping(to: CGRect(x: 0, y: 760, width: 1300, height: 1000))!
for (locale, line) in headerLines {
    let (hw, hh) = (3840, 1646)
    let hdr = canvas(hw, hh)
    backdrop(hdr, CGFloat(hw), CGFloat(hh))
    hdr.saveGState()
    hdr.clip(to: CGRect(x: 1500, y: 0, width: 2340, height: hh))
    hdr.interpolationQuality = .high
    hdr.draw(crop, in: CGRect(x: 1500, y: -60, width: 2340, height: 1800))
    hdr.restoreGState()
    let fade = CGGradient(colorsSpace: CGColorSpace(name: CGColorSpace.sRGB)!, colors: [
        CGColor(red: 0.07, green: 0.07, blue: 0.07, alpha: 1), CGColor(red: 0.07, green: 0.07, blue: 0.07, alpha: 0),
    ] as CFArray, locations: [0, 1])!
    hdr.setFillColor(CGColor(red: 0.07, green: 0.07, blue: 0.07, alpha: 1)); hdr.fill(CGRect(x: 0, y: 0, width: 1500, height: hh))
    hdr.drawLinearGradient(fade, start: CGPoint(x: 1500, y: 0), end: CGPoint(x: 2100, y: 0), options: [])
    rounded(hdr, load(iconPath), CGRect(x: 200, y: 930, width: 420, height: 420), 94)
    _ = text(hdr, "RadarNebo", "HelveticaNeue-Bold", 200, white, CGRect(x: 190, y: 600, width: 1500, height: 280), center: false)
    _ = text(hdr, line, "HelveticaNeue", 84, soft, CGRect(x: 196, y: 330, width: 1300, height: 260), center: false)
    _ = text(hdr, "by Gusak Lab", "HelveticaNeue-Medium", 64, brand, CGRect(x: 196, y: 150, width: 1300, height: 100), center: false)
    save(hdr, "\(out)/\(locale)/header.png")
}
