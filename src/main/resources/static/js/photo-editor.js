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
    crop: root.dataset.crop || 'original',
    warmth: parseInt(root.dataset.warmth, 10),
    brightness: parseInt(root.dataset.brightness, 10)
  };
  if (isNaN(state.warmth)) state.warmth = 5;
  if (isNaN(state.brightness)) state.brightness = 5;
  var img = null;
  var file = null;
  var showOriginal = false;

  var inputs = {
    tilt: root.querySelector('[data-input="tilt"]'),
    warmth: root.querySelector('[data-input="warmth"]'),
    brightness: root.querySelector('[data-input="brightness"]')
  };
  var outs = {
    tilt: root.querySelector('[data-out="tilt"]'),
    warmth: root.querySelector('[data-out="warmth"]'),
    brightness: root.querySelector('[data-out="brightness"]')
  };

  function syncControls() {
    inputs.tilt.value = state.tilt;
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
    // Zoom just enough that tilting never shows empty corners.
    var zoom = Math.abs(tiltRad) > 0 ? Math.cos(Math.abs(tiltRad)) + Math.sin(Math.abs(tiltRad)) * Math.max(outW / outH, outH / outW) : 1;
    c.rotate((quarter * Math.PI / 180) + tiltRad);
    c.scale(scale * zoom, scale * zoom);
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
    syncControls();
    load(URL.createObjectURL(file));
  });
  root.querySelectorAll('[data-rotate]').forEach(function (b) {
    b.addEventListener('click', function () { state.rotate = (state.rotate + parseInt(b.dataset.rotate, 10) + 360) % 360; draw(); });
  });
  Object.keys(inputs).forEach(function (k) {
    inputs[k].addEventListener('input', function () {
      state[k] = k === 'tilt' ? parseFloat(inputs[k].value) : parseInt(inputs[k].value, 10);
      outs[k].textContent = k === 'tilt' ? state[k] + '°' : state[k] + ' of 10';
      draw();
    });
  });
  root.querySelectorAll('[data-crop-choice]').forEach(function (r) {
    r.addEventListener('change', function () { state.crop = r.value; draw(); });
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
      fd.append('crop', state.crop);
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
