// MadelMade: small progressive enhancements. Every page works without this file.
(function () {
  'use strict';
  var $ = function (sel, root) { return (root || document).querySelector(sel); };
  var $$ = function (sel, root) { return Array.prototype.slice.call((root || document).querySelectorAll(sel)); };

  // Header turns solid once the hero scrolls away.
  var header = $('[data-header]');
  if (header && header.classList.contains('is-over-hero')) {
    var onScroll = function () { header.classList.toggle('is-solid', window.scrollY > 40); };
    window.addEventListener('scroll', onScroll, { passive: true });
    onScroll();
  }

  // Phone menu.
  var toggle = $('[data-menu-toggle]');
  var mobileNav = $('[data-mobile-nav]');
  if (toggle && mobileNav) {
    toggle.addEventListener('click', function () {
      var open = mobileNav.classList.toggle('is-open');
      toggle.setAttribute('aria-expanded', open ? 'true' : 'false');
    });
    $$('a', mobileNav).forEach(function (a) {
      a.addEventListener('click', function () { mobileNav.classList.remove('is-open'); toggle.setAttribute('aria-expanded', 'false'); });
    });
  }

  // Cart drawer and checkout popup.
  var drawer = $('[data-drawer]');
  var scrim = $('[data-scrim]');
  var modal = $('[data-checkout-modal]');
  function openDrawer() {
    if (!drawer) return;
    drawer.classList.add('is-open');
    drawer.setAttribute('aria-hidden', 'false');
    scrim.classList.remove('hidden');
    var close = $('[data-cart-close]', drawer);
    if (close) close.focus();
  }
  function closeAll() {
    if (drawer) { drawer.classList.remove('is-open'); drawer.setAttribute('aria-hidden', 'true'); }
    if (modal) modal.classList.add('hidden');
    if (scrim) scrim.classList.add('hidden');
  }
  if (drawer && drawer.classList.contains('is-open')) scrim.classList.remove('hidden');
  $$('[data-cart-open]').forEach(function (el) {
    el.addEventListener('click', function (e) { e.preventDefault(); openDrawer(); });
  });
  $$('[data-cart-close], [data-modal-close]').forEach(function (el) { el.addEventListener('click', closeAll); });
  if (scrim) scrim.addEventListener('click', closeAll);
  document.addEventListener('keydown', function (e) { if (e.key === 'Escape') closeAll(); });
  var popupBtn = $('[data-checkout-popup]');
  if (popupBtn && modal) {
    popupBtn.addEventListener('click', function () {
      drawer.classList.remove('is-open');
      modal.classList.remove('hidden');
      scrim.classList.remove('hidden');
      var first = $('a, button', modal);
      if (first) first.focus();
    });
  }

  // Menu category tabs filter without reloading.
  var tabs = $('[data-menu-tabs]');
  var grid = $('[data-menu-grid]');
  if (tabs && grid) {
    $$('[data-filter]', tabs).forEach(function (btn) {
      btn.addEventListener('click', function () {
        var f = btn.getAttribute('data-filter');
        $$('[data-filter]', tabs).forEach(function (b) {
          var on = b === btn;
          b.classList.toggle('is-on', on);
          b.setAttribute('aria-pressed', on ? 'true' : 'false');
        });
        $$('[data-category]', grid).forEach(function (card) {
          card.classList.toggle('hidden', f !== 'all' && card.getAttribute('data-category') !== f);
        });
      });
    });
  }

  // Product page: live price, chosen names, swatch preview, letters, custom notes and certificate.
  var form = $('[data-product-form]');
  if (form) {
    var base = parseFloat(form.getAttribute('data-base')) || 0;
    var certPrice = parseFloat(form.getAttribute('data-cert-price')) || 0;
    var img = $('[data-preview-img]', form);
    var previewText = $('[data-preview-text]', form);
    var lettersBox = $('[data-letters-box]', form);
    var lettersInput = lettersBox ? $('input', lettersBox) : null;
    var charCount = $('[data-char-count]', form);
    var notesLabel = $('[data-notes-label]', form);
    var notes = $('#notes', form);
    var certToggle = $('[data-cert-toggle]', form);
    var certBox = $('[data-cert-box]', form);
    var qtyInput = $('[data-qty-input]', form);
    var fmt = function (n) { return '$' + (Math.round(n * 100) % 100 === 0 ? Math.round(n) : n.toFixed(2)); };
    var lastSwatch = null;

    var update = function (changed) {
      var price = base, half = false, tbd = false, custom = false, limit = 0;
      $$('[data-group]', form).forEach(function (g) {
        var names = [];
        $$('input', g).forEach(function (i) {
          if (!i.checked) return;
          names.push(i.getAttribute('data-name'));
          price += parseFloat(i.getAttribute('data-price')) || 0;
          if (i.getAttribute('data-halves') === 'true') half = true;
          if (i.getAttribute('data-tbd') === 'true') tbd = true;
          if (i.getAttribute('data-custom') === 'true') custom = true;
          var l = parseInt(i.getAttribute('data-text-limit'), 10);
          if (l > 0) limit = l;
        });
        var chosen = $('[data-chosen]', g);
        if (chosen) chosen.textContent = names.join(', ');
      });
      if (half) price = price / 2;
      if (certToggle && certToggle.checked) price += certPrice;
      var qty = qtyInput ? Math.max(1, parseInt(qtyInput.value, 10) || 1) : 1;
      var text = fmt(price * qty);
      $('[data-price-text]', form).textContent = fmt(price);
      $('[data-price-button]', form).textContent = text + (tbd ? ' + custom' : '');
      $('[data-tbd-note]', form).hidden = !tbd;

      if (lettersBox) {
        lettersBox.hidden = limit === 0;
        if (lettersInput) {
          lettersInput.maxLength = limit || 40;
          lettersInput.required = limit > 0;
          if (charCount) charCount.textContent = (lettersInput.value.length) + ' / ' + (limit || 0);
        }
      }
      if (notesLabel) {
        notesLabel.textContent = custom ? 'Describe your idea (required)' : 'Anything special? (optional)';
        if (notes) notes.required = custom;
      }
      if (certBox && certToggle) {
        certBox.hidden = !certToggle.checked;
        var certName = $('#certName', form);
        if (certName) certName.required = certToggle.checked;
      }
      // Swatch preview: the chosen style swaps the large photo and its description.
      if (changed && changed.closest && changed.closest('.swatches')) lastSwatch = changed;
      var sw = lastSwatch || $('.swatches input:checked', form);
      if (sw && img) {
        var photo = sw.getAttribute('data-photo');
        img.src = photo || img.getAttribute('data-default') || img.src;
        img.alt = sw.getAttribute('data-name');
        if (previewText) {
          previewText.hidden = false;
          $('[data-preview-name]', previewText).textContent = sw.getAttribute('data-name');
          $('[data-preview-desc]', previewText).textContent = sw.getAttribute('data-desc') || '';
        }
      }
    };
    form.addEventListener('change', function (e) { update(e.target); });
    form.addEventListener('input', function () { update(null); });
    $$('[data-qty]', form).forEach(function (b) {
      b.addEventListener('click', function () {
        var v = (parseInt(qtyInput.value, 10) || 1) + parseInt(b.getAttribute('data-qty'), 10);
        qtyInput.value = Math.max(1, Math.min(6, v));
        update(null);
      });
    });
    update(null);
  }

  // Checkout: delivery address and priority dates.
  var co = $('[data-checkout-form]');
  if (co) {
    var addr = $('[data-address-field]', co);
    var pickupNote = $('[data-pickup-note]', co);
    var date = $('[data-date]', co);
    var help = $('[data-date-help]', co);
    var prio = $('[data-priority]', co);
    var minStd = co.getAttribute('data-min-standard');
    var minPri = co.getAttribute('data-min-priority');
    var pretty = function (iso) {
      var d = new Date(iso + 'T12:00:00');
      return d.toLocaleDateString('en-US', { weekday: 'long', month: 'long', day: 'numeric' });
    };
    var sync = function () {
      var checked = $('[data-fulfillment]:checked', co);
      var delivery = checked && checked.value === 'DELIVERY';
      if (addr) addr.hidden = !delivery;
      if (pickupNote) pickupNote.hidden = delivery;
      var min = prio && prio.checked ? minPri : minStd;
      if (date) {
        date.min = min;
        if (date.value && date.value < min) date.value = min;
      }
      if (help) help.textContent = 'Earliest: ' + pretty(min);
    };
    co.addEventListener('change', sync);
    sync();
  }

  // Confirm before destructive actions.
  $$('form[data-confirm]').forEach(function (f) {
    f.addEventListener('submit', function (e) {
      if (!window.confirm(f.getAttribute('data-confirm'))) e.preventDefault();
    });
  });

  $$('[data-print]').forEach(function (b) { b.addEventListener('click', function () { window.print(); }); });

  // Flash messages fade after a few seconds.
  var flash = $('[data-flash]');
  if (flash) setTimeout(function () { flash.style.transition = 'opacity .4s'; flash.style.opacity = '0'; setTimeout(function () { flash.remove(); }, 450); }, 6000);
})();
