//! Contrast: text laid out along a curve ("text on path").
//!
//! The run is laid out in the browser — with the very model the on-path text
//! editor uses for its caret and selection band (`app.contrast.text-on-path`) —
//! and each glyph's placement is sent here. Rust only paints: every character
//! is drawn with the shape's font at its own baseline origin and rotation, so
//! the canvas and the editor can never disagree about where a glyph is.
//!
//! Glyph origins are relative to the shape's selrect top-left corner, so the
//! text follows the shape while it is being moved (the placements are
//! recomputed by the browser once the change is committed).
use skia_safe::{self as skia};

use super::{Color, FontFamily};
use crate::globals::get_resources;
use crate::math::{self, Bounds};

#[derive(Debug, Clone, Copy, PartialEq)]
pub struct PathGlyph {
    pub ch: char,
    /// baseline origin of the glyph, relative to the selrect top-left
    pub x: f32,
    pub y: f32,
    /// rotation around the origin, in degrees
    pub angle: f32,
}

#[derive(Debug, Clone, PartialEq)]
pub struct TextOnPath {
    pub family: FontFamily,
    pub size: f32,
    pub color: Color,
    pub glyphs: Vec<PathGlyph>,
}

impl TextOnPath {
    /// Bounds of the painted run in page coordinates: every glyph origin
    /// inflated by the font size (ascent/descent and the advance in any
    /// rotation), so tiles never clip a glyph.
    pub fn bounds(&self, selrect: &math::Rect) -> Option<Bounds> {
        if self.glyphs.is_empty() {
            return None;
        }
        let pad = self.size * 1.5;
        let (mut x0, mut y0, mut x1, mut y1) = (f32::MAX, f32::MAX, f32::MIN, f32::MIN);
        for g in &self.glyphs {
            x0 = x0.min(g.x);
            y0 = y0.min(g.y);
            x1 = x1.max(g.x);
            y1 = y1.max(g.y);
        }
        let rect = math::Rect::from_ltrb(
            selrect.left() + x0 - pad,
            selrect.top() + y0 - pad,
            selrect.left() + x1 + pad,
            selrect.top() + y1 + pad,
        );
        Some(Bounds::from_rect(&rect))
    }
}

fn typeface_for(alias: &str) -> Option<skia::Typeface> {
    get_resources()
        .fonts
        .font_provider()
        .match_family_style(alias, skia::FontStyle::default())
}

/// The primary typeface, or the first fallback font that has a glyph for `ch`
/// (e.g. Cyrillic text set in a Latin-only face).
fn typeface_with_glyph(primary: &Option<skia::Typeface>, ch: char) -> Option<skia::Typeface> {
    if let Some(tf) = primary {
        if tf.unichar_to_glyph(ch as i32) != 0 {
            return Some(tf.clone());
        }
    }
    let fonts = &get_resources().fonts;
    let has = |tf: &skia::Typeface| tf.unichar_to_glyph(ch as i32) != 0;
    // the fallback faces penpot loaded for this content first…
    for alias in fonts.get_fallback().iter() {
        if let Some(tf) = typeface_for(alias).filter(has) {
            return Some(tf);
        }
    }
    // …then any loaded face that has the character (a fallback face may have
    // been stored before it was flagged as one)
    for alias in fonts.font_provider().family_names() {
        if let Some(tf) = typeface_for(&alias).filter(has) {
            return Some(tf);
        }
    }
    primary.clone()
}

/// Paints the run. The canvas is expected in page coordinates (the same state
/// the text paragraphs are drawn in).
pub fn render(text_on_path: &TextOnPath, selrect: &math::Rect, canvas: &skia::Canvas) {
    let primary = typeface_for(&text_on_path.family.to_string());
    let mut paint = skia::Paint::default();
    paint.set_color(text_on_path.color);
    paint.set_anti_alias(true);

    let mut buf = [0u8; 4];
    for glyph in &text_on_path.glyphs {
        if glyph.ch.is_whitespace() {
            continue;
        }
        let Some(typeface) = typeface_with_glyph(&primary, glyph.ch) else {
            continue;
        };
        let mut font = skia::Font::from_typeface(typeface, text_on_path.size);
        font.set_subpixel(true);
        font.set_edging(skia::font::Edging::AntiAlias);

        canvas.save();
        canvas.translate((selrect.left() + glyph.x, selrect.top() + glyph.y));
        canvas.rotate(glyph.angle, None);
        canvas.draw_str(glyph.ch.encode_utf8(&mut buf), (0.0, 0.0), &font, &paint);
        canvas.restore();
    }
}
