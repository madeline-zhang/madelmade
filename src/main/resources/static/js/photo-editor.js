// Photo editor: rotate, straighten, a crop box with draggable corners (shape fixed per page),
// and brightness, contrast, saturation and warmth. The preview is drawn exactly as it will be saved.
(function () {
  'use strict';
  var root = document.querySelector('[data-editor]');
  if (!root) return;
  var canvas = root.querySelector('[data-canvas]');
  var ctx = canvas.getContext('2d');
  var empty = root.querySelector('[data-empty]');
  var errorBox = root.querySelector('[data-error]');
  var MAX_OUT = 1600;     // longest side of the saved photo
  var MAX_PREVIEW = 1000; // longest side of the preview canvas
  var ASPECT = parseFloat(root.dataset.aspect) || 1; // crop width / height
  var SLIDERS = ['brightness', 'contrast', 'saturation', 'warmth'];

  function num(v, fallback) { var n = parseFloat(v); return isNaN(n) ? fallback : n; }
  function clamp(v, lo, hi) { return Math.max(lo, Math.min(hi, v)); }

  var state = {
    rotate: num(root.dataset.rotate, 0),
    tilt: clamp(num(root.dataset.tilt, 0), -45, 45),
    crop: parseCrop(root.dataset.crop), // {x, y, w, h} as fractions of the straightened photo
    brightness: num(root.dataset.brightness, 0),
    contrast: num(root.dataset.contrast, 0),
    saturation: num(root.dataset.saturation, 0),
    warmth: num(root.dataset.warmth, 0)
  };
  var img = null;
  var file = null;
  var showOriginal = false;

  function parseCrop(s) {
    var p = (s || '').split(',').map(parseFloat);
    if (p.length !== 4 || p.some(isNaN) || p[2] <= 0 || p[3] <= 0) return null;
    return { x: p[0], y: p[1], w: p[2], h: p[3] };
  }

  // ---- Geometry ----

  // The photo after quarter turns: its width and height in original pixels.
  function frameSize() {
    var quarter = ((state.rotate % 360) + 360) % 360;
    var swap = quarter === 90 || quarter === 270;
    return { w: swap ? img.naturalHeight : img.naturalWidth, h: swap ? img.naturalWidth : img.naturalHeight, quarter: quarter };
  }

  // Largest crop of the right shape, centered.
  function defaultCrop() {
    var f = frameSize();
    var w = f.w, h = f.w / ASPECT;
    if (h > f.h) { h = f.h; w = f.h * ASPECT; }
    return { x: (f.w - w) / 2 / f.w, y: (f.h - h) / 2 / f.h, w: w / f.w, h: h / f.h };
  }

  // Keeps a saved crop the right shape and inside the photo.
  function fixCrop(c) {
    var f = frameSize();
    if (!c) return defaultCrop();
    var w = clamp(c.w * f.w, 20, f.w);
    var h = w / ASPECT;
    if (h > f.h) { h = f.h; w = h * ASPECT; }
    return {
      x: clamp(c.x * f.w, 0, f.w - w) / f.w,
      y: clamp(c.y * f.h, 0, f.h - h) / f.h,
      w: w / f.w,
      h: h / f.h
    };
  }

  // Draws the straightened photo (or part of it) into target at the given scale.
  function drawPhoto(target, scale, region, original) {
    var f = frameSize();
    var r = region || { x: 0, y: 0, w: 1, h: 1 };
    var outW = Math.max(1, Math.round(r.w * f.w * scale));
    var outH = Math.max(1, Math.round(r.h * f.h * scale));
    target.width = outW;
    target.height = outH;
    var c = target.getContext('2d');
    c.save();
    c.fillStyle = '#ffffff';
    c.fillRect(0, 0, outW, outH);
    c.translate(-r.x * f.w * scale, -r.y * f.h * scale);
    c.translate(f.w * scale / 2, f.h * scale / 2);
    var t = (original ? 0 : state.tilt) * Math.PI / 180;
    var cos = Math.cos(Math.abs(t)), sin = Math.sin(Math.abs(t));
    // Zoom just enough that straightening never shows empty corners.
    var zoom = Math.max(cos + (f.h / f.w) * sin, cos + (f.w / f.h) * sin);
    c.rotate(f.quarter * Math.PI / 180 + t);
    c.scale(scale * zoom, scale * zoom);
    c.imageSmoothingQuality = 'high';
    c.drawImage(img, -img.naturalWidth / 2, -img.naturalHeight / 2);
    c.restore();
    if (!original) adjust(c, outW, outH);
  }

  // ---- Brightness, contrast, saturation, warmth (each -100 to 100) ----

  function adjust(c, w, h) {
    var b = state.brightness / 100, k = state.contrast / 100, s = state.saturation / 100, t = state.warmth / 100;
    if (!b && !k && !s && !t) return;
    var gamma = 1 - b * 0.5;            // brightness lifts or deepens midtones, keeping pure black and white
    var factor = k >= 0 ? 1 + k : 1 + k * 0.8;
    var mult = [1 + 0.18 * t, 1 + 0.04 * t, 1 - 0.18 * t];
    var luts = mult.map(function (m) {
      var lut = new Uint8ClampedArray(256);
      for (var v = 0; v < 256; v++) {
        var x = Math.min(255, v * m) / 255;
        x = Math.pow(x, gamma);
        x = (x - 0.5) * factor + 0.5;
        lut[v] = x * 255;
      }
      return lut;
    });
    var data = c.getImageData(0, 0, w, h);
    var px = data.data, sat = 1 + s;
    for (var i = 0; i < px.length; i += 4) {
      var r = luts[0][px[i]], g = luts[1][px[i + 1]], bl = luts[2][px[i + 2]];
      if (s) {
        var gray = 0.299 * r + 0.587 * g + 0.114 * bl;
        r = gray + (r - gray) * sat;
        g = gray + (g - gray) * sat;
        bl = gray + (bl - gray) * sat;
      }
      px[i] = r; px[i + 1] = g; px[i + 2] = bl;
    }
    c.putImageData(data, 0, 0);
  }

  // ---- Preview with the crop box ----

  var base = document.createElement('canvas'); // adjusted photo at preview size
  var pad = 0; // room around the photo so the corner handles are never cut off
  var baseDirty = true;
  var pending = false;

  function draw() {
    if (pending) return;
    pending = true;
    requestAnimationFrame(function () { pending = false; paint(); });
  }

  function paint() {
    if (!img) return;
    var f = frameSize();
    var scale = Math.min(1, MAX_PREVIEW / Math.max(f.w, f.h));
    if (baseDirty || showOriginal) {
      drawPhoto(base, scale, null, showOriginal);
      baseDirty = showOriginal; // redraw the adjusted version after letting go
    }
    pad = Math.round(Math.max(base.width, base.height) * 0.03);
    canvas.width = base.width + pad * 2;
    canvas.height = base.height + pad * 2;
    ctx.drawImage(base, pad, pad);
    if (showOriginal) return;

    var r = cropPx();
    // Grey out everything outside the crop.
    ctx.save();
    ctx.fillStyle = 'rgba(20, 20, 20, 0.62)';
    ctx.beginPath();
    ctx.rect(pad, pad, base.width, base.height);
    ctx.rect(r.x, r.y, r.w, r.h);
    ctx.fill('evenodd');
    // Thirds lines.
    var line = Math.max(1, base.width / 600);
    ctx.strokeStyle = 'rgba(255, 255, 255, 0.45)';
    ctx.lineWidth = line;
    ctx.beginPath();
    for (var i = 1; i < 3; i++) {
      ctx.moveTo(r.x + r.w * i / 3, r.y); ctx.lineTo(r.x + r.w * i / 3, r.y + r.h);
      ctx.moveTo(r.x, r.y + r.h * i / 3); ctx.lineTo(r.x + r.w, r.y + r.h * i / 3);
    }
    ctx.stroke();
    // Border and corner handles.
    ctx.strokeStyle = '#ffffff';
    ctx.lineWidth = line * 1.5;
    ctx.strokeRect(r.x, r.y, r.w, r.h);
    var len = Math.min(r.w, r.h) * 0.12 + handleSize() * 0.5, thick = line * 5;
    ctx.fillStyle = '#ffffff';
    [[r.x, r.y, 1, 1], [r.x + r.w, r.y, -1, 1], [r.x, r.y + r.h, 1, -1], [r.x + r.w, r.y + r.h, -1, -1]].forEach(function (k) {
      ctx.fillRect(k[2] > 0 ? k[0] - thick / 2 : k[0] - len + thick / 2, k[1] - thick / 2, len, thick);
      ctx.fillRect(k[0] - thick / 2, k[3] > 0 ? k[1] - thick / 2 : k[1] - len + thick / 2, thick, len);
    });
    ctx.restore();
  }

  // The crop box in canvas pixels (the photo sits inside a small margin).
  function cropPx() {
    return { x: pad + state.crop.x * base.width, y: pad + state.crop.y * base.height, w: state.crop.w * base.width, h: state.crop.h * base.height };
  }
  function setCropPx(r) {
    state.crop = { x: (r.x - pad) / base.width, y: (r.y - pad) / base.height, w: r.w / base.width, h: r.h / base.height };
  }
  // Canvas pixels per screen pixel, so handles feel the same size whatever the zoom.
  function ratio() { return canvas.width / (canvas.getBoundingClientRect().width || canvas.width); }
  function handleSize() { return 28 * ratio(); }

  // ---- Dragging the crop box ----

  var drag = null;
  function point(e) {
    var b = canvas.getBoundingClientRect();
    return { x: (e.clientX - b.left) * canvas.width / b.width, y: (e.clientY - b.top) * canvas.height / b.height };
  }
  function hit(p) {
    var r = cropPx(), hs = handleSize();
    var corners = [
      { name: 'nw', x: r.x, y: r.y, ax: r.x + r.w, ay: r.y + r.h },
      { name: 'ne', x: r.x + r.w, y: r.y, ax: r.x, ay: r.y + r.h },
      { name: 'sw', x: r.x, y: r.y + r.h, ax: r.x + r.w, ay: r.y },
      { name: 'se', x: r.x + r.w, y: r.y + r.h, ax: r.x, ay: r.y }
    ];
    for (var i = 0; i < corners.length; i++) {
      if (Math.abs(p.x - corners[i].x) <= hs && Math.abs(p.y - corners[i].y) <= hs) return { mode: 'resize', corner: corners[i] };
    }
    if (p.x > r.x && p.x < r.x + r.w && p.y > r.y && p.y < r.y + r.h) return { mode: 'move' };
    return null;
  }
  function cursorFor(h) {
    if (!h) return 'default';
    if (h.mode === 'move') return 'move';
    return h.corner.name === 'nw' || h.corner.name === 'se' ? 'nwse-resize' : 'nesw-resize';
  }

  canvas.addEventListener('pointerdown', function (e) {
    if (!img || showOriginal) return;
    var p = point(e), h = hit(p);
    if (!h) return;
    e.preventDefault();
    drag = { hit: h, start: p, crop: cropPx() };
    canvas.setPointerCapture(e.pointerId);
  });
  canvas.addEventListener('pointermove', function (e) {
    if (!img) return;
    var p = point(e);
    if (!drag) { canvas.style.cursor = cursorFor(hit(p)); return; }
    // The photo's edges in canvas pixels.
    var L = pad, T = pad, R = pad + base.width, B = pad + base.height, c = drag.crop;
    if (drag.hit.mode === 'move') {
      setCropPx({
        x: clamp(c.x + p.x - drag.start.x, L, R - c.w),
        y: clamp(c.y + p.y - drag.start.y, T, B - c.h),
        w: c.w, h: c.h
      });
    } else {
      // The opposite corner stays put; the box keeps its shape.
      var k = drag.hit.corner, sx = k.x > k.ax ? 1 : -1, sy = k.y > k.ay ? 1 : -1;
      var w = Math.max(Math.abs(p.x - k.ax), Math.abs(p.y - k.ay) * ASPECT);
      var maxW = Math.min(sx > 0 ? R - k.ax : k.ax - L, (sy > 0 ? B - k.ay : k.ay - T) * ASPECT);
      w = clamp(w, Math.min(60 * ratio(), maxW), maxW);
      var h = w / ASPECT;
      setCropPx({ x: sx > 0 ? k.ax : k.ax - w, y: sy > 0 ? k.ay : k.ay - h, w: w, h: h });
    }
    draw();
  });
  ['pointerup', 'pointercancel'].forEach(function (ev) {
    canvas.addEventListener(ev, function () { drag = null; });
  });

  // ---- Controls ----

  var inputs = {}, outs = {};
  ['tilt'].concat(SLIDERS).forEach(function (k) {
    inputs[k] = root.querySelector('[data-input="' + k + '"]');
    outs[k] = root.querySelector('[data-out="' + k + '"]');
  });
  function label(k) {
    if (k === 'tilt') return (Math.round(state.tilt * 10) / 10) + '°';
    return (state[k] > 0 ? '+' : '') + state[k];
  }
  function syncControls() {
    Object.keys(inputs).forEach(function (k) { inputs[k].value = state[k]; outs[k].textContent = label(k); });
  }
  Object.keys(inputs).forEach(function (k) {
    inputs[k].addEventListener('input', function () {
      state[k] = parseFloat(inputs[k].value);
      outs[k].textContent = label(k);
      baseDirty = true;
      draw();
    });
    inputs[k].addEventListener('dblclick', function () {
      state[k] = 0;
      syncControls();
      baseDirty = true;
      draw();
    });
  });
  root.querySelector('[data-reset-adjust]').addEventListener('click', function () {
    SLIDERS.forEach(function (k) { state[k] = 0; });
    syncControls();
    baseDirty = true;
    draw();
  });
  root.querySelector('[data-reset-crop]').addEventListener('click', function () {
    if (!img) return;
    state.crop = defaultCrop();
    draw();
  });
  root.querySelectorAll('[data-rotate]').forEach(function (b) {
    b.addEventListener('click', function () {
      if (!img) return;
      state.rotate = (state.rotate + parseInt(b.dataset.rotate, 10) + 360) % 360;
      state.crop = defaultCrop();
      baseDirty = true;
      draw();
    });
  });
  var hold = root.querySelector('[data-hold-original]');
  ['pointerdown', 'keydown'].forEach(function (ev) { hold.addEventListener(ev, function () { showOriginal = true; draw(); }); });
  ['pointerup', 'pointerleave', 'keyup', 'blur'].forEach(function (ev) {
    hold.addEventListener(ev, function () { if (!showOriginal) return; showOriginal = false; baseDirty = true; draw(); });
  });

  function load(src, fresh) {
    var i = new Image();
    i.onload = function () {
      img = i;
      if (empty) empty.remove();
      state.crop = fresh ? defaultCrop() : fixCrop(state.crop);
      baseDirty = true;
      draw();
    };
    i.onerror = function () { showError('That photo could not be opened. Please try a JPEG or PNG.'); };
    i.src = src;
  }

  function showError(msg) {
    errorBox.textContent = msg;
    errorBox.classList.remove('hidden');
  }

  root.querySelector('[data-file]').addEventListener('change', function (e) {
    file = e.target.files[0] || null;
    if (!file) return;
    errorBox.classList.add('hidden');
    // A new photo starts over: no turn or tilt, and every slider back to the starting look from Settings.
    var fresh = (root.dataset.fresh || '').split(',');
    state.rotate = 0;
    state.tilt = 0;
    SLIDERS.forEach(function (k, i) { state[k] = num(fresh[i], 0); });
    syncControls();
    // Lets picking the same file again still count as a new pick.
    e.target.value = '';
    load(URL.createObjectURL(file), true);
  });

  root.querySelector('[data-save]').addEventListener('click', function () {
    if (!img) { showError('Choose a photo first.'); return; }
    var btn = this;
    btn.disabled = true;
    btn.textContent = 'Saving…';
    var f = frameSize(), c = state.crop;
    var out = document.createElement('canvas');
    drawPhoto(out, Math.min(1, MAX_OUT / Math.max(c.w * f.w, c.h * f.h)), c, false);
    out.toBlob(function (blob) {
      var fd = new FormData();
      fd.append('target', root.dataset.target);
      if (file) fd.append('original', file, file.name);
      fd.append('edited', blob, 'edited.jpg');
      fd.append('rotate', state.rotate);
      fd.append('tilt', state.tilt);
      fd.append('crop', [c.x, c.y, c.w, c.h].map(function (v) { return v.toFixed(4); }).join(','));
      SLIDERS.forEach(function (k) { fd.append(k, Math.round(state[k])); });
      var caption = root.querySelector('[data-caption]');
      if (caption && caption.value.trim()) fd.append('caption', caption.value.trim());
      var token = document.querySelector('meta[name="_csrf"]').content;
      var header = document.querySelector('meta[name="_csrf_header"]').content;
      var headers = {};
      headers[header] = token;
      fetch('/admin/photos/save', { method: 'POST', body: fd, headers: headers, credentials: 'same-origin' })
        .then(function (r) { return r.json().then(function (j) { return { ok: r.ok, body: j }; }); })
        .then(function (res) {
          if (res.ok && res.body.redirect) { window.location = res.body.redirect; return; }
          throw new Error(res.body.error || 'Saving failed.');
        })
        .catch(function (err) {
          showError(err.message || 'Saving failed. Please try again.');
          btn.disabled = false;
          btn.textContent = 'Save photo';
        });
    }, 'image/jpeg', 0.88);
  });

  window.addEventListener('resize', draw);
  syncControls();
  if (root.dataset.original) load(root.dataset.original, false);
})();
