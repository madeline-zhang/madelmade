// Dashboard enhancements. The photo editor lives in photo-editor.js.
(function () {
  'use strict';
  // Phone menu: the hamburger opens the full list of dashboard pages.
  var menuBtn = document.querySelector('[data-admin-menu-toggle]');
  var menu = document.querySelector('[data-admin-menu]');
  if (menuBtn && menu) {
    menuBtn.addEventListener('click', function () {
      var open = menu.classList.toggle('is-open');
      menuBtn.setAttribute('aria-expanded', open ? 'true' : 'false');
      menuBtn.setAttribute('aria-label', open ? 'Close admin menu' : 'Open admin menu');
    });
  }

  // Promo codes: the percent box only matters for "Percent off".
  document.querySelectorAll('select[name="kind"]').forEach(function (sel) {
    var pct = sel.form && sel.form.querySelector('[name="percentOff"]');
    var field = pct && pct.closest('.field');
    if (!field) return;
    function sync() { field.hidden = sel.value === 'free'; }
    sel.addEventListener('change', sync);
    sync();
  });

  // Switches that save as soon as they're flipped.
  document.querySelectorAll('[data-autosubmit]').forEach(function (el) {
    el.addEventListener('change', function () { if (el.form.requestSubmit) el.form.requestSubmit(); else el.form.submit(); });
  });
})();
