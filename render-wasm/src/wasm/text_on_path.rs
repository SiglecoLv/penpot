//! Contrast: glyph placements of a text-on-path run (see shapes::text_on_path).
//!
//! Buffer layout (little endian):
//!   0..16   font family uuid (4 x u32)
//!   16      font weight (u32)
//!   20      font style (u8: 0 normal, 1 italic) + 3 bytes padding
//!   24      font size (f32)
//!   28      fill color (u32, ARGB)
//!   32      glyph count (u32)
//!   36..    per glyph, 16 bytes: codepoint (u32), x (f32), y (f32), angle (f32)
use macros::wasm_error;

use super::fonts::RawFontStyle;
use crate::mem;
use crate::shapes::{FontFamily, PathGlyph, TextOnPath};
use crate::utils::uuid_from_u32_quartet;
use crate::with_current_shape_mut;

const HEADER_SIZE: usize = 36;
const GLYPH_SIZE: usize = 16;

fn u32_at(bytes: &[u8], offset: usize) -> u32 {
    u32::from_le_bytes([
        bytes[offset],
        bytes[offset + 1],
        bytes[offset + 2],
        bytes[offset + 3],
    ])
}

fn f32_at(bytes: &[u8], offset: usize) -> f32 {
    f32::from_bits(u32_at(bytes, offset))
}

fn parse(bytes: &[u8]) -> Option<TextOnPath> {
    if bytes.len() < HEADER_SIZE {
        return None;
    }
    let id = uuid_from_u32_quartet(
        u32_at(bytes, 0),
        u32_at(bytes, 4),
        u32_at(bytes, 8),
        u32_at(bytes, 12),
    );
    let weight = u32_at(bytes, 16);
    let style = RawFontStyle::from(bytes[20]);
    let size = f32_at(bytes, 24);
    let color = skia_safe::Color::new(u32_at(bytes, 28));
    let count = u32_at(bytes, 32) as usize;

    let glyphs = (0..count)
        .filter_map(|i| {
            let o = HEADER_SIZE + i * GLYPH_SIZE;
            if o + GLYPH_SIZE > bytes.len() {
                return None;
            }
            Some(PathGlyph {
                ch: char::from_u32(u32_at(bytes, o))?,
                x: f32_at(bytes, o + 4),
                y: f32_at(bytes, o + 8),
                angle: f32_at(bytes, o + 12),
            })
        })
        .collect();

    Some(TextOnPath {
        family: FontFamily::new(id, weight, style.into()),
        size,
        color,
        glyphs,
    })
}

#[no_mangle]
#[wasm_error]
pub extern "C" fn set_shape_text_on_path() -> Result<()> {
    with_current_shape_mut!(state, |shape: &mut Shape| {
        let bytes = mem::bytes();
        shape.set_text_on_path(parse(&bytes));
        mem::free_bytes()?;
    });
    Ok(())
}

#[no_mangle]
#[wasm_error]
pub extern "C" fn clear_shape_text_on_path() -> Result<()> {
    with_current_shape_mut!(state, |shape: &mut Shape| {
        shape.set_text_on_path(None);
    });
    Ok(())
}
