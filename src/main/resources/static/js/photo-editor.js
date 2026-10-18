// Photo editor: rotate, fine tilt, crop, warmth and brightness, previewed exactly as it will be saved.
(function () {
  'use strict';
  var root = document.querySelector('[data-editor]');
  if (!root) return;
  var canvas = root.querySelector('[data-canvas]');
  var ctx = canvas.getContext('2d');
  var empty = root.querySelector('[data-empty]');
  var errorBox = root.querySelector('[data-error]');
  var MAX = 1600;

  var state = {
    rotate: parseInt(root.dataset.rotate, 10) || 0,
    tilt: parseFloat(root.dataset.tilt) || 0,
    crop: 'original',
    zoom: 1,
    panX: 0,
    panY: 0,
    warmth: parseInt(root.dataset.warmth, 10),
    brightness: parseInt(root.dataset.brightness, 10)
  };
  // Crop is saved as "shape:zoom:x:y" so a re-edit keeps the framing, e.g. "square:1.4:0.2:-0.5".
  (function () {
    var parts = (root.dataset.crop || 'original').split(':');
    state.crop = parts[0] || 'original';
    state.zoom = clamp(parseFloat(parts[1]) || 1, 1, 3);
    state.panX = clamp(parseFloat(parts[2]) || 0, -1, 1);
    state.panY = clamp(parseFloat(parts[3]) || 0, -1, 1);
  })();
  function clamp(v, lo, hi) { return Math.max(lo, Math.min(hi, v)); }
  function cropValue() {
    return state.crop + ':' + state.zoom.toFixed(2) + ':' + state.panX.toFixed(3) + ':' + state.panY.toFixed(3);
  }
  if (isNaN(state.warmth)) state.warmth = 5;
  if (isNaN(state.brightness)) state.brightness = 5;
  var img = null;
  var file = null;
  var showOriginal = false;

  var inputs = {
    tilt: root.querySelector('[data-input="tilt"]'),
    zoom: root.querySelector('[data-input="zoom"]'),
    warmth: root.querySelector('[data-input="warmth"]'),
    brightness: root.querySelector('[data-input="brightness"]')
  };
  var outs = {
    tilt: root.querySelector('[data-out="tilt"]'),
    zoom: root.querySelector('[data-out="zoom"]'),
    warmth: root.querySelector('[data-out="warmth"]'),
    brightness: root.querySelector('[data-out="brightness"]')
  };

  function syncControls() {
    inputs.tilt.value = state.tilt;
    inputs.zoom.value = state.zoom;
    outs.zoom.textContent = Math.round(state.zoom * 100) + '%';
    inputs.warmth.value = state.warmth;
    inputs.brightness.value = state.brightness;
    outs.tilt.textContent = state.tilt + '°';
    outs.warmth.textContent = state.warmth + ' of 10';
    outs.brightness.textContent = state.brightness + ' of 10';
    root.querySelectorAll('[data-crop-choice]').forEach(function (r) { r.checked = r.value === state.crop; });
  }

  // The same math the server keeps alongside the photo, so a re-edit starts where it left off.
  function render(target, maxSide, original) {
    if (!img) return;
    var quarter = ((state.rotate % 360) + 360) % 360;
    var swap = quarter === 90 || quarter === 270;
    var w0 = swap ? img.naturalHeight : img.naturalWidth;
    var h0 = swap ? img.naturalWidth : img.naturalHeight;

    var ratio = { square: 1, portrait: 4 / 5, landscape: 4 / 3 }[state.crop] || (w0 / h0);
    var cw = w0, ch = w0 / ratio;
    if (ch > h0) { ch = h0; cw = h0 * ratio; }
    var scale = Math.min(1, maxSide / Math.max(cw, ch));
    var outW = Math.round(cw * scale), outH = Math.round(ch * scale);
    target.width = outW;
    target.height = outH;
    var c = target.getContext('2d');
    c.save();
    c.fillStyle = '#ffffff';
    c.fillRect(0, 0, outW, outH);
    c.translate(outW / 2, outH / 2);
    var tiltRad = (original ? 0 : state.tilt) * Math.PI / 180;
    // Madeline's own framing: zoom in, then slide the photo within the frame.
    var userZoom = original ? 1 : state.zoom;
    var spareX = Math.max(0, (w0 * scale * userZoom - outW) / 2);
    var spareY = Math.max(0, (h0 * scale * userZoom - outH) / 2);
    lastFrame = { spareX: spareX, spareY: spareY, outW: outW };
    if (!original) c.translate(state.panX * spareX, state.panY * spareY);
    // Zoom just enough that tilting never shows empty corners.
    var zoom = Math.abs(tiltRad) > 0 ? Math.cos(Math.abs(tiltRad)) + Math.sin(Math.abs(tiltRad)) * Math.max(outW / outH, outH / outW) : 1;
    c.rotate((quarter * Math.PI / 180) + tiltRad);
    c.scale(scale * zoom * userZoom, scale * zoom * userZoom);
    c.drawImage(img, -img.naturalWidth / 2, -img.naturalHeight / 2);
    c.restore();
    if (original) return;

    var t = (state.warmth - 5) / 5;
    var b = 1 + (state.brightness - 5) * 0.04;
    if (t === 0 && b === 1) return;
    var data = c.getImageData(0, 0, outW, outH);
    var px = data.data;
    var rf = (1 + 0.12 * t) * b, gf = (1 + 0.03 * t) * b, bf = (1 - 0.12 * t) * b;
    for (var i = 0; i < px.length; i += 4) {
      px[i] = Math.min(255, px[i] * rf);
      px[i + 1] = Math.min(255, px[i + 1] * gf);
      px[i + 2] = Math.min(255, px[i + 2] * bf);
    }
    c.putImageData(data, 0, 0);
  }

  var lastFrame = null;
  var pending = false;
  function draw() {
    if (pending) return;
    pending = true;
    requestAnimationFrame(function () { pending = false; render(canvas, 900, showOriginal); });
  }

  function load(src) {
    var i = new Image();
    i.onload = function () { img = i; if (empty) empty.remove(); draw(); };
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
    state.rotate = 0;
    state.tilt = 0;
    state.zoom = 1;
    state.panX = 0;
    state.panY = 0;
    syncControls();
    load(URL.createObjectURL(file));
  });
  root.querySelectorAll('[data-rotate]').forEach(function (b) {
    b.addEventListener('click', function () { state.rotate = (state.rotate + parseInt(b.dataset.rotate, 10) + 360) % 360; draw(); });
  });
  Object.keys(inputs).forEach(function (k) {
    inputs[k].addEventListener('input', function () {
      state[k] = k === 'tilt' || k === 'zoom' ? parseFloat(inputs[k].value) : parseInt(inputs[k].value, 10);
      outs[k].textContent = k === 'tilt' ? state[k] + '°' : k === 'zoom' ? Math.round(state[k] * 100) + '%' : state[k] + ' of 10';
      draw();
    });
  });
  root.querySelectorAll('[data-crop-choice]').forEach(function (r) {
    r.addEventListener('change', function () { state.crop = r.value; draw(); });
  });
  // Drag the photo to frame it.
  var drag = null;
  canvas.addEventListener('pointerdown', function (e) {
    if (!img) return;
    drag = { x: e.clientX, y: e.clientY, panX: state.panX, panY: state.panY };
    canvas.setPointerCapture(e.pointerId);
    canvas.classList.add('is-dragging');
  });
  canvas.addEventListener('pointermove', function (e) {
    if (!drag || !lastFrame) return;
    // Screen pixels to canvas pixels, since the preview is shown smaller than it's drawn.
    var k = canvas.width / canvas.clientWidth;
    if (lastFrame.spareX > 0) state.panX = clamp(drag.panX + (e.clientX - drag.x) * k / lastFrame.spareX, -1, 1);
    if (lastFrame.spareY > 0) state.panY = clamp(drag.panY + (e.clientY - drag.y) * k / lastFrame.spareY, -1, 1);
    draw();
  });
  ['pointerup', 'pointercancel'].forEach(function (ev) {
    canvas.addEventListener(ev, function () { drag = null; canvas.classList.remove('is-dragging'); });
  });
  root.querySelector('[data-reset-frame]').addEventListener('click', function () {
    state.zoom = 1; state.panX = 0; state.panY = 0; syncControls(); draw();
  });

  var hold = root.querySelector('[data-hold-original]');
  ['pointerdown', 'keydown'].forEach(function (ev) { hold.addEventListener(ev, function () { showOriginal = true; draw(); }); });
  ['pointerup', 'pointerleave', 'keyup', 'blur'].forEach(function (ev) { hold.addEventListener(ev, function () { showOriginal = false; draw(); }); });

  root.querySelector('[data-save]').addEventListener('click', function () {
    if (!img) { showError('Choose a photo first.'); return; }
    var btn = this;
    btn.disabled = true;
    btn.textContent = 'Saving…';
    var out = document.createElement('canvas');
    render(out, MAX, false);
    out.toBlob(function (blob) {
      var fd = new FormData();
      fd.append('target', root.dataset.target);
      if (file) fd.append('original', file, file.name);
      fd.append('edited', blob, 'edited.jpg');
      fd.append('rotate', state.rotate);
      fd.append('tilt', state.tilt);
      fd.append('crop', cropValue());
      fd.append('warmth', state.warmth);
      fd.append('brightness', state.brightness);
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
    }, 'image/jpeg', 0.86);
  });

  syncControls();
  if (root.dataset.original) load(root.dataset.original);
})();
