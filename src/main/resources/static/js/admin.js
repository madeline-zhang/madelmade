// Dashboard enhancements. The photo editor lives in photo-editor.js.
(function () {
  'use strict';
  // Switches that save as soon as they're flipped.
  document.querySelectorAll('[data-autosubmit]').forEach(function (el) {
    el.addEventListener('change', function () { el.form.submit(); });
  });
})();
