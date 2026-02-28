// Logo generation script for Finio app
// Generates icon.png, adaptive-icon.png, splash-icon.png, favicon.png

const { createCanvas } = require('canvas');
const fs = require('fs');
const path = require('path');

const ASSETS_DIR = path.join(__dirname, '..', 'assets');

function hexToRgb(hex) {
  const r = parseInt(hex.slice(1, 3), 16);
  const g = parseInt(hex.slice(3, 5), 16);
  const b = parseInt(hex.slice(5, 7), 16);
  return { r, g, b };
}

function drawLogo(canvas, { iconScale = 1, rounded = true, bgGradient = true } = {}) {
  const ctx = canvas.getContext('2d');
  const W = canvas.width;
  const H = canvas.height;
  const cx = W / 2;
  const cy = H / 2;

  // --- Background ---
  if (bgGradient) {
    const grad = ctx.createLinearGradient(0, 0, W, H);
    grad.addColorStop(0, '#9B93FF');   // light purple top-left
    grad.addColorStop(0.5, '#6C63FF'); // primary purple center
    grad.addColorStop(1, '#3D35C8');   // deep purple bottom-right
    ctx.fillStyle = grad;
  } else {
    // Transparent background (for adaptive icon foreground)
    ctx.clearRect(0, 0, W, H);
    ctx.fillStyle = 'transparent';
  }

  if (rounded) {
    const r = W * 0.22; // corner radius ~22% of width
    ctx.beginPath();
    ctx.moveTo(r, 0);
    ctx.lineTo(W - r, 0);
    ctx.quadraticCurveTo(W, 0, W, r);
    ctx.lineTo(W, H - r);
    ctx.quadraticCurveTo(W, H, W - r, H);
    ctx.lineTo(r, H);
    ctx.quadraticCurveTo(0, H, 0, H - r);
    ctx.lineTo(0, r);
    ctx.quadraticCurveTo(0, 0, r, 0);
    ctx.closePath();
    ctx.fill();
  } else {
    if (bgGradient) ctx.fillRect(0, 0, W, H);
  }

  // --- Wallet Icon (Lucide-style) ---
  // Scale the 24-unit viewbox to our canvas
  // Target icon size: ~55% of canvas width
  const iconSize = W * 0.52 * iconScale;
  const unit = iconSize / 24;

  // Center the icon
  const ox = cx - (24 * unit) / 2;
  const oy = cy - (24 * unit) / 2;

  const strokeW = unit * 1.6; // stroke width proportional to icon size

  ctx.save();
  ctx.translate(ox, oy);
  ctx.scale(unit, unit);

  ctx.strokeStyle = 'rgba(255,255,255,0.95)';
  ctx.lineWidth = strokeW / unit;
  ctx.lineCap = 'round';
  ctx.lineJoin = 'round';

  // Lucide Wallet icon paths (viewBox 0 0 24 24)
  // Path 1: Wallet body top flap + back card slot
  // M 21 12 V 7 H 5 a 2 2 0 0 1 0 -4 h 14 v 4
  ctx.beginPath();
  ctx.moveTo(21, 12);
  ctx.lineTo(21, 7);
  ctx.lineTo(5, 7);
  // arc: center at (5, 5), radius 2, from 90deg (bottom) to 270deg (top) — left semicircle
  ctx.arc(5, 5, 2, Math.PI / 2, (3 * Math.PI) / 2, false);
  ctx.lineTo(19, 3);
  ctx.lineTo(19, 7);
  ctx.stroke();

  // Path 2: Main wallet body
  // M 3 5 v 14 a 2 2 0 0 0 2 2 h 16 v -5
  ctx.beginPath();
  ctx.moveTo(3, 5);
  ctx.lineTo(3, 19);
  ctx.arc(5, 19, 2, Math.PI, Math.PI / 2, true);
  ctx.lineTo(21, 21);
  ctx.lineTo(21, 16);
  ctx.stroke();

  // Path 3: Front pocket / coin holder
  // M 18 12 a 2 2 0 0 0 0 4 h 4 v -4 Z
  ctx.beginPath();
  ctx.moveTo(18, 12);
  // arc: from (18,12) curving to (18,16), it's a 2-radius arc
  ctx.arc(18, 14, 2, (3 * Math.PI) / 2, Math.PI / 2, false);
  ctx.lineTo(22, 16);
  ctx.lineTo(22, 12);
  ctx.closePath();
  ctx.stroke();

  ctx.restore();
}

function saveCanvas(canvas, filename) {
  const buffer = canvas.toBuffer('image/png');
  const filepath = path.join(ASSETS_DIR, filename);
  fs.writeFileSync(filepath, buffer);
  console.log(`✔  Saved ${filename} (${canvas.width}×${canvas.height})`);
}

// 1. Main app icon — 1024×1024, rounded corners
const icon = createCanvas(1024, 1024);
drawLogo(icon, { rounded: true, bgGradient: true });
saveCanvas(icon, 'icon.png');

// 2. Adaptive icon (Android foreground) — 1024×1024, no rounded corners (Android clips it)
const adaptive = createCanvas(1024, 1024);
drawLogo(adaptive, { rounded: false, bgGradient: true });
saveCanvas(adaptive, 'adaptive-icon.png');

// 3. Splash icon — 200×200, no background (Expo composites it over splash bg)
const splash = createCanvas(200, 200);
const sCtx = splash.getContext('2d');
// Draw just the wallet icon on transparent background for splash
const splashFull = createCanvas(200, 200);
drawLogo(splashFull, { rounded: false, bgGradient: false, iconScale: 0.9 });
// For splash, we want icon on a solid surface
const splashCanvas = createCanvas(200, 200);
const sc = splashCanvas.getContext('2d');
const splashGrad = sc.createLinearGradient(0, 0, 200, 200);
splashGrad.addColorStop(0, '#9B93FF');
splashGrad.addColorStop(0.5, '#6C63FF');
splashGrad.addColorStop(1, '#3D35C8');
sc.fillStyle = splashGrad;
sc.fillRect(0, 0, 200, 200);

// Draw wallet on splash
const unit = (200 * 0.52) / 24;
const ox = 100 - (24 * unit) / 2;
const oy = 100 - (24 * unit) / 2;
const strokeW = unit * 1.6;
sc.save();
sc.translate(ox, oy);
sc.scale(unit, unit);
sc.strokeStyle = 'rgba(255,255,255,0.95)';
sc.lineWidth = strokeW / unit;
sc.lineCap = 'round';
sc.lineJoin = 'round';

sc.beginPath();
sc.moveTo(21, 12);
sc.lineTo(21, 7);
sc.lineTo(5, 7);
sc.arc(5, 5, 2, Math.PI / 2, (3 * Math.PI) / 2, false);
sc.lineTo(19, 3);
sc.lineTo(19, 7);
sc.stroke();

sc.beginPath();
sc.moveTo(3, 5);
sc.lineTo(3, 19);
sc.arc(5, 19, 2, Math.PI, Math.PI / 2, true);
sc.lineTo(21, 21);
sc.lineTo(21, 16);
sc.stroke();

sc.beginPath();
sc.moveTo(18, 12);
sc.arc(18, 14, 2, (3 * Math.PI) / 2, Math.PI / 2, false);
sc.lineTo(22, 16);
sc.lineTo(22, 12);
sc.closePath();
sc.stroke();

sc.restore();

saveCanvas(splashCanvas, 'splash-icon.png');

// 4. Favicon — 48×48
const favicon = createCanvas(48, 48);
const fCtx = favicon.getContext('2d');
const fGrad = fCtx.createLinearGradient(0, 0, 48, 48);
fGrad.addColorStop(0, '#9B93FF');
fGrad.addColorStop(1, '#3D35C8');
fCtx.fillStyle = fGrad;
// Rounded corners for favicon
const fr = 10;
fCtx.beginPath();
fCtx.moveTo(fr, 0);
fCtx.lineTo(48 - fr, 0);
fCtx.quadraticCurveTo(48, 0, 48, fr);
fCtx.lineTo(48, 48 - fr);
fCtx.quadraticCurveTo(48, 48, 48 - fr, 48);
fCtx.lineTo(fr, 48);
fCtx.quadraticCurveTo(0, 48, 0, 48 - fr);
fCtx.lineTo(0, fr);
fCtx.quadraticCurveTo(0, 0, fr, 0);
fCtx.closePath();
fCtx.fill();

const fu = (48 * 0.52) / 24;
const fox = 24 - (24 * fu) / 2;
const foy = 24 - (24 * fu) / 2;
const fsw = fu * 1.7;
fCtx.save();
fCtx.translate(fox, foy);
fCtx.scale(fu, fu);
fCtx.strokeStyle = 'rgba(255,255,255,0.95)';
fCtx.lineWidth = fsw / fu;
fCtx.lineCap = 'round';
fCtx.lineJoin = 'round';

fCtx.beginPath();
fCtx.moveTo(21, 12);
fCtx.lineTo(21, 7);
fCtx.lineTo(5, 7);
fCtx.arc(5, 5, 2, Math.PI / 2, (3 * Math.PI) / 2, false);
fCtx.lineTo(19, 3);
fCtx.lineTo(19, 7);
fCtx.stroke();

fCtx.beginPath();
fCtx.moveTo(3, 5);
fCtx.lineTo(3, 19);
fCtx.arc(5, 19, 2, Math.PI, Math.PI / 2, true);
fCtx.lineTo(21, 21);
fCtx.lineTo(21, 16);
fCtx.stroke();

fCtx.beginPath();
fCtx.moveTo(18, 12);
fCtx.arc(18, 14, 2, (3 * Math.PI) / 2, Math.PI / 2, false);
fCtx.lineTo(22, 16);
fCtx.lineTo(22, 12);
fCtx.closePath();
fCtx.stroke();

fCtx.restore();

saveCanvas(favicon, 'favicon.png');

console.log('\nAll logo assets generated successfully!');
