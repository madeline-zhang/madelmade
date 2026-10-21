// Dashboard enhancements. The photo editor lives in photo-editor.js.
(function () {
  'use strict';
  // Promo codes: the percent box only matters for "Percent off".
  document.querySelectorAll('select[name="kind"]').forEach(function (sel) {
    var pct = sel.form && sel.form.querySelector('[name="percentOff"]');
    var field = pct && pct.closest('.field');
    if (!field) return;
    function sync() { field.hidden = sel.value === 'free'; }
    sel.addEventListener('change', sync);
    sync();
  });

  // Option groups have a form per choice. Whichever Save is clicked, save every changed form in the
  // same group first, so edits to several choices aren't lost.
  var snapshot = function (f) { return new URLSearchParams(new FormData(f)).toString(); };
  document.querySelectorAll('.group-card').forEach(function (card) {
    var forms = Array.prototype.slice.call(card.querySelectorAll('form')).filter(function (f) {
      return !f.hasAttribute('data-confirm') && (f.getAttribute('method') || '').toLowerCase() === 'post';
    });
    forms.forEach(function (f) { f.dataset.saved = snapshot(f); });
    forms.forEach(function (f) {
      f.addEventListener('submit', function (e) {
        if (f.dataset.saving) return;
        var others = forms.filter(function (o) { return o !== f && snapshot(o) !== o.dataset.saved; });
        if (!others.length) return;
        e.preventDefault();
        Promise.all(others.map(function (o) {
          return fetch(o.action, { method: 'POST', body: new URLSearchParams(new FormData(o)), credentials: 'same-origin', redirect: 'manual' });
        })).then(function () {
          f.dataset.saving = '1';
          if (f.requestSubmit) f.requestSubmit(); else f.submit();
        });
      });
    });
  });

  // Switches that save as soon as they're flipped.
  document.querySelectorAll('[data-autosubmit]').forEach(function (el) {
    el.addEventListener('change', function () { if (el.form.requestSubmit) el.form.requestSubmit(); else el.form.submit(); });
  });
})();
