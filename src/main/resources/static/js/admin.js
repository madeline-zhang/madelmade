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

  // Collapsible sections. Option groups are <details>, closed unless left open before or linked to
  // (a save comes back to #group-N). Cards with a title fold on a click of the title and start open.
  var store = {
    get: function (k) { try { return window.localStorage.getItem(k); } catch (e) { return null; } },
    set: function (k, v) { try { window.localStorage.setItem(k, v); } catch (e) { /* private mode */ } }
  };
  var pageKey = 'fold:' + location.pathname + ':';
  document.querySelectorAll('details[data-collapsible]').forEach(function (d) {
    var key = pageKey + d.id;
    if (location.hash === '#' + d.id || store.get(key) === 'open') d.open = true;
    d.addEventListener('toggle', function () { store.set(key, d.open ? 'open' : 'closed'); });
  });
  if (location.hash) {
    var target = document.getElementById(location.hash.slice(1));
    if (target && target.tagName === 'DETAILS') target.scrollIntoView();
  }
  document.querySelectorAll('.admin-main .card > h2.card-title').forEach(function (title) {
    var card = title.parentNode;
    var key = pageKey + title.textContent.trim();
    title.classList.add('is-collapsible');
    title.setAttribute('role', 'button');
    title.setAttribute('tabindex', '0');
    var set = function (collapsed) {
      card.classList.toggle('is-collapsed', collapsed);
      title.setAttribute('aria-expanded', collapsed ? 'false' : 'true');
    };
    set(store.get(key) === 'closed' && location.hash !== '#' + card.id);
    var flip = function () {
      var collapsed = !card.classList.contains('is-collapsed');
      set(collapsed);
      store.set(key, collapsed ? 'closed' : 'open');
    };
    title.addEventListener('click', flip);
    title.addEventListener('keydown', function (e) {
      if (e.key === 'Enter' || e.key === ' ') { e.preventDefault(); flip(); }
    });
  });

  // Autosave: forms marked data-autosave save in the background as soon as something changes,
  // so there's no Save button to remember. Text saves when you pause typing or leave the field.
  // Inputs marked data-no-autosave (like "New choice") wait for their own button.
  var toast = null, toastTimer = null;
  function showToast(text, kind) {
    if (!toast) {
      toast = document.createElement('div');
      toast.className = 'autosave-toast';
      toast.setAttribute('role', 'status');
      document.body.appendChild(toast);
    }
    toast.textContent = text;
    toast.className = 'autosave-toast is-on' + (kind ? ' is-' + kind : '');
    clearTimeout(toastTimer);
    if (kind === 'ok') toastTimer = setTimeout(function () { toast.className = 'autosave-toast'; }, 1600);
  }
  var pending = 0;
  window.addEventListener('beforeunload', function (e) {
    if (pending > 0) { e.preventDefault(); e.returnValue = ''; }
  });
  document.querySelectorAll('form[data-autosave]').forEach(function (form) {
    form.querySelectorAll('[data-autosave-hide]').forEach(function (el) { el.hidden = true; });
    var timer = null, busy = false, again = false;
    function data() {
      var fd = new FormData(form);
      form.querySelectorAll('[data-no-autosave][name]').forEach(function (el) { fd.delete(el.name); });
      return new URLSearchParams(fd);
    }
    function save(report, leaving) {
      clearTimeout(timer);
      timer = null;
      if (busy) { again = true; return; }
      if (!form.checkValidity()) {
        if (report) form.reportValidity();
        showToast('Not saved yet: fill in the highlighted field.', 'error');
        return;
      }
      busy = true;
      pending++;
      showToast('Saving…');
      fetch(form.action, {
        method: 'POST', body: data(), credentials: 'same-origin', keepalive: !!leaving,
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' }
      }).then(function (r) {
        return r.text().then(function (html) { return { ok: r.ok, html: html }; });
      }).then(function (res) {
        var doc = new DOMParser().parseFromString(res.html, 'text/html');
        var err = doc.querySelector('.alert-error');
        if (!res.ok || err) {
          showToast(err ? err.textContent.trim().replace(/\s+/g, ' ') : 'Couldn\'t save. Please try again.', 'error');
          return;
        }
        // The server may adjust ticks (e.g. only one "Picked by default"), so copy them back.
        var fresh = doc.querySelector('form[action="' + form.getAttribute('action') + '"]');
        if (fresh) {
          form.querySelectorAll('input[type=checkbox][name]').forEach(function (box) {
            var match = fresh.querySelector('input[type=checkbox][name="' + box.name + '"][value="' + box.value + '"]');
            if (match) box.checked = match.hasAttribute('checked');
          });
        }
        showToast('Saved', 'ok');
      }).catch(function () {
        showToast('Couldn\'t save. Check your connection and try again.', 'error');
      }).then(function () {
        busy = false;
        pending--;
        if (again) { again = false; save(false); }
      });
    }
    function skip(el) { return !el || !el.name || el.hasAttribute('data-no-autosave') || el.type === 'file'; }
    form.addEventListener('change', function (e) { if (!skip(e.target)) save(true); });
    form.addEventListener('input', function (e) {
      if (skip(e.target) || e.target.type === 'checkbox' || e.target.type === 'radio') return;
      clearTimeout(timer);
      timer = setTimeout(function () { save(false); }, 900);
    });
    window.addEventListener('pagehide', function () { if (timer) save(false, true); });
  });

  // Switches that save as soon as they're flipped.
  document.querySelectorAll('[data-autosubmit]').forEach(function (el) {
    el.addEventListener('change', function () { if (el.form.requestSubmit) el.form.requestSubmit(); else el.form.submit(); });
  });
})();
