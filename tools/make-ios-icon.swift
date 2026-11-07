// Draws RadarNebo's Apple icons in Gusak Lab's colors (#121212, #8BC34A, #DCECCB): a radar sweep that stays legible at
// home-screen size. The iOS icon is full-bleed without alpha, as App Store icons must be (iOS rounds the corners); the
// macOS one sits on the platform's inset rounded square, with transparent margins.
//
//   swiftc -O tools/make-ios-icon.swift -o /tmp/make-ios-icon && /tmp/make-ios-icon <ios.png> <macos.png>
import CoreGraphics
import Foundation
import ImageIO
import UniformTypeIdentifiers

let space = CGColorSpace(name: CGColorSpace.sRGB)!
func rgb(_ r: CGFloat, _ g: CGFloat, _ b: CGFloat, _ a: CGFloat = 1) -> CGColor { CGColor(colorSpace: space, components: [r, g, b, a])! }
let cyan = (CGFloat(0x8B) / 255, CGFloat(0xC3) / 255, CGFloat(0x4A) / 255) // #8BC34A
let light = rgb(CGFloat(0xDC) / 255, CGFloat(0xEC) / 255, CGFloat(0xCB) / 255) // #DCECCB
func cyanA(_ a: CGFloat) -> CGColor { rgb(cyan.0, cyan.1, cyan.2, a) }
let size = 1024
let ctx = CGContext(data: nil, width: size, height: size, bitsPerComponent: 8, bytesPerRow: 0, space: space,
                    bitmapInfo: CGImageAlphaInfo.noneSkipLast.rawValue)!
let c = CGPoint(x: 512, y: 512)
let r: CGFloat = 400

// The night sky: lighter behind the radar, falling off to the corners.
let sky = CGGradient(colorsSpace: space, colors: [rgb(0.13, 0.20, 0.09), rgb(0.09, 0.11, 0.08), rgb(0.07, 0.07, 0.07)] as CFArray,
                     locations: [0, 0.62, 1])!
ctx.setFillColor(rgb(0.07, 0.07, 0.07)); ctx.fill(CGRect(x: 0, y: 0, width: size, height: size))
ctx.drawRadialGradient(sky, startCenter: c, startRadius: 0, endCenter: c, endRadius: 760, options: .drawsAfterEndLocation)

// Rings and crosshair.
ctx.setStrokeColor(cyanA(0.75)); ctx.setLineWidth(18)
for f in [0.5, 1.0] as [CGFloat] { ctx.strokeEllipse(in: CGRect(x: c.x - r * f, y: c.y - r * f, width: r * 2 * f, height: r * 2 * f)) }
ctx.setStrokeColor(cyanA(0.35)); ctx.setLineWidth(10)
ctx.move(to: CGPoint(x: c.x - r, y: c.y)); ctx.addLine(to: CGPoint(x: c.x + r, y: c.y))
ctx.move(to: CGPoint(x: c.x, y: c.y - r)); ctx.addLine(to: CGPoint(x: c.x, y: c.y + r))
ctx.strokePath()

// The sweep: a wedge fading away counter-clockwise from its leading edge.
let edge: CGFloat = 38, span: CGFloat = 115, steps = 900
ctx.saveGState()
ctx.addEllipse(in: CGRect(x: c.x - r + 9, y: c.y - r + 9, width: 2 * (r - 9), height: 2 * (r - 9))); ctx.clip()
// Without antialiasing the wedges meet exactly; with it, each seam would show as a faint streak.
ctx.setShouldAntialias(false)
for i in 0..<steps {
    let t = CGFloat(i) / CGFloat(steps)
    let a0 = (edge + span * t) * .pi / 180, a1 = (edge + span * (t + 1 / CGFloat(steps))) * .pi / 180
    ctx.move(to: c); ctx.addArc(center: c, radius: r + 20, startAngle: a0, endAngle: a1, clockwise: false); ctx.closePath()
    ctx.setFillColor(cyanA(0.72 * pow(1 - t, 2.2))); ctx.fillPath()
}
ctx.restoreGState()
let tip = CGPoint(x: c.x + (r - 9) * cos(edge * .pi / 180), y: c.y + (r - 9) * sin(edge * .pi / 180))
ctx.setLineCap(.round); ctx.setStrokeColor(light); ctx.setLineWidth(26)
ctx.move(to: c); ctx.addLine(to: tip); ctx.strokePath()
ctx.setFillColor(light); ctx.fillEllipse(in: CGRect(x: c.x - 40, y: c.y - 40, width: 80, height: 80))

// Targets: the one just caught by the sweep glows, as a threat does on the map.
func blip(_ at: CGPoint, _ col: (CGFloat, CGFloat, CGFloat), _ rad: CGFloat) {
    let glow = CGGradient(colorsSpace: space, colors: [rgb(col.0, col.1, col.2, 0.65), rgb(col.0, col.1, col.2, 0)] as CFArray, locations: [0, 1])!
    ctx.drawRadialGradient(glow, startCenter: at, startRadius: rad * 0.6, endCenter: at, endRadius: rad * 2.9, options: [])
    ctx.setFillColor(rgb(col.0, col.1, col.2)); ctx.fillEllipse(in: CGRect(x: at.x - rad, y: at.y - rad, width: rad * 2, height: rad * 2))
}
blip(CGPoint(x: 735, y: 655), (1, 0.33, 0.36), 50)
blip(CGPoint(x: 330, y: 400), (0.98, 0.68, 0.16), 36)

func save(_ image: CGImage, _ path: String) {
    let d = CGImageDestinationCreateWithURL(URL(fileURLWithPath: path) as CFURL, UTType.png.identifier as CFString, 1, nil)!
    CGImageDestinationAddImage(d, image, nil)
    precondition(CGImageDestinationFinalize(d))
}
let art = ctx.makeImage()!
save(art, CommandLine.arguments[1])

let mac = CGContext(data: nil, width: size, height: size, bitsPerComponent: 8, bytesPerRow: 0, space: space,
                    bitmapInfo: CGImageAlphaInfo.premultipliedLast.rawValue)!
let body = CGRect(x: 100, y: 100, width: 824, height: 824)
mac.addPath(CGPath(roundedRect: body, cornerWidth: 185, cornerHeight: 185, transform: nil)); mac.clip()
mac.interpolationQuality = .high
mac.draw(art, in: body)
save(mac.makeImage()!, CommandLine.arguments[2])
