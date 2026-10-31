// MadelMade: small progressive enhancements. Every page works without this file.
(function () {
  'use strict';
  var $ = function (sel, root) { return (root || document).querySelector(sel); };
  var $$ = function (sel, root) { return Array.prototype.slice.call((root || document).querySelectorAll(sel)); };

  // Missing or invalid fields: instead of the browser's brief bubble, put a clear message right
  // under the field (e.g. "Please provide your name.") and bring the first one into view.
  var lastInvalidAt = 0;
  var labelFor = function (el, field) {
    var label = (el.id && $('label[for="' + el.id + '"]')) || (field && $('label, .label', field));
    var text = label ? label.textContent.replace(/\(.*?\)/g, '').trim() : '';
    return text.toLowerCase();
  };
  var invalidMessage = function (el, field) {
    if (el.getAttribute('data-required-msg')) return el.getAttribute('data-required-msg');
    var v = el.validity;
    var label = labelFor(el, field);
    if (v.valueMissing) {
      if (el.type === 'radio' || el.type === 'checkbox' || el.tagName === 'SELECT') return 'Please choose ' + (label ? 'a ' + label : 'an option') + '.';
      if (el.type === 'date') return 'Please choose a date.';
      var personal = /name|email|phone|address/.test(label) && !!el.closest('[data-checkout-form], form[action*="signin"], form[action*="profile"], [data-feedback-form]');
      return label ? (personal ? 'Please provide your ' + label + '.' : 'Please fill in ' + label + '.') : 'Please fill this in.';
    }
    if (v.typeMismatch && el.type === 'email') return 'Please enter a valid email address, like name@example.com.';
    if (v.rangeUnderflow && el.type === 'date') return 'Please choose a later date.';
    if (v.rangeOverflow && el.getAttribute('data-max-msg')) return el.getAttribute('data-max-msg');
    return el.validationMessage;
  };
  var clearInvalid = function (el) {
    var field = el.closest('.field') || el.parentNode;
    var box = field && $('.field-error[data-live]', field);
    if (box) box.remove();
    el.removeAttribute('aria-invalid');
  };
  document.addEventListener('invalid', function (e) {
    var el = e.target;
    if (!el || !el.closest) return;
    e.preventDefault();
    // A missing field inside a folded section opens it so the message can be seen.
    var folded = el.closest('details');
    if (folded) folded.open = true;
    var card = el.closest('.is-collapsed');
    if (card) card.classList.remove('is-collapsed');
    var field = el.closest('.field') || el.closest('fieldset') || el.parentNode;
    var box = $('.field-error[data-live]', field);
    if (!box) {
      box = document.createElement('div');
      box.className = 'field-error';
      box.setAttribute('data-live', '');
      box.setAttribute('role', 'alert');
      field.appendChild(box);
    }
    box.textContent = invalidMessage(el, field);
    el.setAttribute('aria-invalid', 'true');
    var now = Date.now();
    if (now - lastInvalidAt > 300) {
      el.focus({ preventScroll: true });
      el.scrollIntoView({ block: 'center' });
    }
    lastInvalidAt = now;
  }, true);
  ['input', 'change'].forEach(function (type) {
    document.addEventListener(type, function (e) {
      var el = e.target;
      if (el && el.getAttribute && el.getAttribute('aria-invalid') === 'true' && el.checkValidity && el.validity.valid) clearInvalid(el);
    });
  });

  // Feedback button: send without leaving the page.
  var fb = $('[data-feedback]');
  if (fb) {
    var fbForm = $('[data-feedback-form]', fb);
    var fbResult = $('[data-feedback-result]', fb);
    var closeFb = function () { fb.open = false; };
    $('[data-feedback-close]', fb).addEventListener('click', closeFb);
    document.addEventListener('keydown', function (e) { if (e.key === 'Escape' && fb.open) closeFb(); });
    fb.addEventListener('toggle', function () {
      if (fb.open) { fbResult.hidden = true; $('textarea', fb).focus(); }
    });
    fbForm.addEventListener('submit', function (e) {
      e.preventDefault();
      var button = $('button[type=submit]', fbForm);
      button.disabled = true;
      fetch(fbForm.action, { method: 'POST', body: new URLSearchParams(new FormData(fbForm)),
        headers: { 'X-Requested-With': 'fetch' }, credentials: 'same-origin' })
        .then(function (r) { return r.text().then(function (t) { return { ok: r.ok, text: t }; }); })
        .then(function (res) {
          fbResult.hidden = false;
          fbResult.textContent = res.ok ? res.text : (res.text || 'That didn\'t send. Please try again.');
          if (res.ok) { $('textarea', fbForm).value = ''; setTimeout(closeFb, 2500); }
        })
        .catch(function () { fbResult.hidden = false; fbResult.textContent = 'That didn\'t send. Please try again.'; })
        .then(function () { button.disabled = false; });
    });
  }

  // Header turns solid once the hero scrolls away.
  var header = $('[data-header]');
  if (header && header.classList.contains('is-over-hero')) {
    var onScroll = function () { header.classList.toggle('is-solid', window.scrollY > 40); };
    window.addEventListener('scroll', onScroll, { passive: true });
    onScroll();
  }

  // Home hero: slides take turns. Videos play only while showing. With reduced motion, the first slide stays.
  var hero = $('[data-hero]');
  if (hero) {
    var slides = Array.prototype.slice.call(hero.querySelectorAll('[data-hero-slide]'));
    var still = window.matchMedia && window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    var play = function (slide, on) {
      var v = slide.querySelector('video');
      if (!v) return;
      if (on && !still) { var p = v.play(); if (p && p.catch) p.catch(function () {}); } else v.pause();
    };
    if (slides.length) play(slides[0], true);
    var seconds = parseFloat(hero.dataset.seconds) || 10;
    if (slides.length > 1 && !still) {
      var current = 0;
      setInterval(function () {
        if (document.hidden) return;
        var prev = slides[current];
        current = (current + 1) % slides.length;
        play(slides[current], true);
        slides[current].classList.add('is-on');
        prev.classList.remove('is-on');
        // Let the previous video finish fading before pausing it.
        setTimeout(function () { if (!prev.classList.contains('is-on')) play(prev, false); }, 4000);
      }, Math.max(2, seconds) * 1000);
    }
  }

  // Home quotes: three at a time; with more, the next group fades in every few seconds. Pauses while
  // hovered or focused, and stays put with reduced motion.
  var quoteBox = $('[data-quotes]');
  if (quoteBox) {
    var quotes = Array.prototype.slice.call(quoteBox.querySelectorAll('[data-quote]'));
    var calmQuotes = window.matchMedia && window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    var per = 3, start = 0, held = false;
    quoteBox.addEventListener('mouseenter', function () { held = true; });
    quoteBox.addEventListener('mouseleave', function () { held = false; });
    quoteBox.addEventListener('focusin', function () { held = true; });
    quoteBox.addEventListener('focusout', function () { held = false; });
    if (quotes.length > per && !calmQuotes) {
      setInterval(function () {
        if (held || document.hidden) return;
        quotes.forEach(function (q) { if (!q.hidden) q.classList.add('is-fading'); });
        setTimeout(function () {
          start = (start + per) % quotes.length;
          quotes.forEach(function (q, i) {
            var shown = (i - start + quotes.length) % quotes.length < per;
            q.hidden = !shown;
            if (shown) q.classList.add('is-fading');
          });
          // Let the browser draw them faded before easing back in.
          requestAnimationFrame(function () { requestAnimationFrame(function () {
            quotes.forEach(function (q) { q.classList.remove('is-fading'); });
          }); });
        }, 600);
      }, 9000);
    }
  }

  // How it's made: steps ease in as they scroll into view, and videos play only while on screen.
  var steps = $('.process-steps');
  if (steps && 'IntersectionObserver' in window) {
    var calm = window.matchMedia && window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    if (!calm) {
      steps.classList.add('is-revealing');
      var reveal = new IntersectionObserver(function (entries) {
        entries.forEach(function (e) {
          if (e.isIntersecting) { e.target.classList.add('is-seen'); reveal.unobserve(e.target); }
        });
      }, { rootMargin: '0px 0px -10% 0px' });
      steps.querySelectorAll('.process-step').forEach(function (s) { reveal.observe(s); });
    }
    var inView = new IntersectionObserver(function (entries) {
      entries.forEach(function (e) {
        if (e.isIntersecting) { var p = e.target.play(); if (p && p.catch) p.catch(function () {}); } else e.target.pause();
      });
    }, { threshold: 0.25 });
    // With reduced motion, videos wait for a tap on play instead.
    steps.querySelectorAll('video[data-play-in-view]').forEach(function (v) { if (calm) v.controls = true; else inView.observe(v); });
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
  // Once the slide-in after "Add to cart" ends, closing uses the normal slide-out.
  if (drawer) drawer.addEventListener('animationend', function () { drawer.classList.remove('drawer-enter'); });
  $$('[data-cart-open]').forEach(function (el) {
    el.addEventListener('click', function (e) { e.preventDefault(); openDrawer(); });
  });
  $$('[data-modal-close]').forEach(function (el) { el.addEventListener('click', closeAll); });
  // The drawer's insides get swapped after a quantity change, so its buttons are handled here.
  if (drawer) drawer.addEventListener('click', function (e) {
    if (e.target.closest('[data-cart-close]')) closeAll();
    if (e.target.closest('[data-checkout-popup]') && modal) {
      drawer.classList.remove('is-open');
      modal.classList.remove('hidden');
      scrim.classList.remove('hidden');
      var first = $('a, button', modal);
      if (first) first.focus();
    }
  });
  // Quantity and Remove in the drawer: save in the background and refresh the drawer in place,
  // instead of reloading the page (which made the drawer slide in again).
  if (drawer && window.fetch && window.DOMParser) drawer.addEventListener('submit', function (e) {
    var form = e.target;
    if (!/\/cart\/(update|remove)$/.test(form.getAttribute('action') || '')) return;
    e.preventDefault();
    var data = new URLSearchParams(new FormData(form));
    if (e.submitter && e.submitter.name) data.append(e.submitter.name, e.submitter.value);
    drawer.setAttribute('aria-busy', 'true');
    fetch(form.action, { method: 'POST', body: data, credentials: 'same-origin' })
      .then(function (r) { return r.text(); })
      .then(function (html) {
        var doc = new DOMParser().parseFromString(html, 'text/html');
        var fresh = $('[data-drawer]', doc);
        if (!fresh) { window.location.reload(); return; }
        drawer.innerHTML = fresh.innerHTML;
        var cartLink = $('[data-cart-open]');
        var freshLink = $('[data-cart-open]', doc);
        if (cartLink && freshLink) {
          cartLink.innerHTML = freshLink.innerHTML;
          cartLink.setAttribute('aria-label', freshLink.getAttribute('aria-label'));
        }
        var errors = $$('[data-flash] .alert-error li', doc).map(function (li) { return li.textContent; });
        if (errors.length) {
          var box = document.createElement('div');
          box.className = 'field-error';
          box.setAttribute('role', 'alert');
          box.style.margin = '12px 20px 0';
          box.textContent = errors.join(' ');
          var body = $('.drawer-body', drawer);
          if (body) body.insertBefore(box, body.firstChild);
        }
      })
      .catch(function () { window.location.reload(); })
      .then(function () { drawer.removeAttribute('aria-busy'); });
  });
  if (scrim) scrim.addEventListener('click', closeAll);
  document.addEventListener('keydown', function (e) { if (e.key === 'Escape') closeAll(); });

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
    var defaultPhoto = img ? img.getAttribute('src') : '';
    var previewText = $('[data-preview-text]', form);
    var lettersBox = $('[data-letters-box]', form);
    var lettersInput = lettersBox ? $('input', lettersBox) : null;
    var charCount = $('[data-char-count]', form);
    var fontSelect = $('[data-font]', form);
    var notesLabel = $('[data-notes-label]', form);
    var notes = $('#notes', form);
    var certToggle = $('[data-cert-toggle]', form);
    var certBox = $('[data-cert-box]', form);
    var qtyInput = $('[data-qty-input]', form);
    var fmt = function (n) { return '$' + (Math.round(n * 100) % 100 === 0 ? Math.round(n) : n.toFixed(2)); };
    var lastSwatch = null;

    var update = function (changed) {
      var price = base, half = false, tbd = false, custom = false, limit = 0, adds = { ingredients: [], allergens: [] };
      $$('[data-group]', form).forEach(function (g) {
        var names = [];
        $$('input', g).forEach(function (i) {
          if (!i.checked) return;
          names.push(i.getAttribute('data-name'));
          price += parseFloat(i.getAttribute('data-price')) || 0;
          if (i.getAttribute('data-halves') === 'true') half = true;
          if (i.getAttribute('data-tbd') === 'true') tbd = true;
          if (i.getAttribute('data-custom') === 'true') custom = true;
          ['ingredients', 'allergens'].forEach(function (k) {
            var v = i.getAttribute('data-' + k);
            if (v) adds[k].push(v);
          });
          var l = parseInt(i.getAttribute('data-text-limit'), 10);
          if (l > 0) limit = l;
        });
        var chosen = $('[data-chosen]', g);
        if (chosen) chosen.textContent = names.join(', ');
      });
      if (half) price = price / 2;
      // Ingredients and allergens: the product's base list plus whatever the picked flavor adds.
      $$('[data-label]', form).forEach(function (dd) {
        var parts = [dd.getAttribute('data-base')].concat(adds[dd.getAttribute('data-label')]).filter(Boolean);
        dd.textContent = parts.length ? parts.join(', ') : 'None listed';
      });
      if (certToggle && certToggle.checked) price += certPrice;
      var qty = qtyInput ? Math.max(1, parseInt(qtyInput.value, 10) || 1) : 1;
      var text = fmt(price * qty);
      $('[data-price-text]', form).textContent = fmt(price);
      $('[data-price-button]', form).textContent = text + (tbd ? ' + custom' : '');
      $('[data-tbd-note]', form).hidden = !tbd;

      // A "Custom" font is described in the notes box, so that box becomes required.
      var customFont = false;
      if (fontSelect) {
        fontSelect.required = limit > 0;
        var opt = fontSelect.options[fontSelect.selectedIndex];
        customFont = limit > 0 && !!opt && opt.getAttribute('data-custom') === 'true';
      }
      if (lettersBox) {
        lettersBox.hidden = limit === 0;
        if (lettersInput) {
          lettersInput.maxLength = limit || 40;
          lettersInput.required = limit > 0;
          if (charCount) charCount.textContent = (lettersInput.value.length) + ' / ' + (limit || 0);
        }
      }
      if (notesLabel) {
        notesLabel.textContent = custom ? 'Describe your idea (required)'
          : customFont ? 'Describe your font (required)' : 'Anything special? (optional)';
        if (notes) notes.required = custom || customFont;
      }
      if (certBox && certToggle) {
        certBox.hidden = !certToggle.checked;
        var certName = $('#certName', form);
        if (certName) certName.required = certToggle.checked;
      }
      // Swatch preview: once the customer picks a style, it swaps the large photo and its description.
      // Until then the product's own photo shows.
      if (changed && changed.closest && changed.closest('.swatches')) lastSwatch = changed;
      var sw = lastSwatch;
      if (sw && img) {
        // The swatch's own picture carries the photo's current address.
        var swatchImg = sw.parentNode.querySelector('.swatch img');
        img.src = (swatchImg && swatchImg.getAttribute('src')) || defaultPhoto;
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
        qtyInput.value = Math.max(1, Math.min(parseInt(qtyInput.max, 10) || 12, v));
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
    var busy = $('[data-busy]', co);
    var full = (co.getAttribute('data-full-dates') || '').split(',');
    var pretty = function (iso) {
      var d = new Date(iso + 'T12:00:00');
      return d.toLocaleDateString('en-US', { weekday: 'long', month: 'long', day: 'numeric' });
    };
    var sync = function () {
      var checked = $('[data-fulfillment]:checked', co);
      var way = checked ? checked.value : 'PICKUP';
      if (addr) addr.hidden = way === 'PICKUP';
      if (pickupNote) pickupNote.hidden = way !== 'PICKUP';
      $$('[data-for-way]', co).forEach(function (el) { el.hidden = el.getAttribute('data-for-way') !== way; });
      var min = prio && prio.checked ? minPri : minStd;
      if (date) {
        date.min = min;
        if (date.value && date.value < min) date.value = min;
      }
      if (help) help.textContent = 'Earliest: ' + pretty(min);
      if (busy) busy.hidden = !(date && date.value && full.indexOf(date.value) >= 0);
    };
    co.addEventListener('change', sync);
    if (date) date.addEventListener('input', sync);
    sync();
  }

  // Confirm before destructive actions.
  $$('form[data-confirm]').forEach(function (f) {
    f.addEventListener('submit', function (e) {
      if (!window.confirm(f.getAttribute('data-confirm'))) e.preventDefault();
    });
  });

  $$('button[data-confirm]').forEach(function (b) {
    b.addEventListener('click', function (e) {
      if (!window.confirm(b.getAttribute('data-confirm'))) e.preventDefault();
    });
  });

  $$('[data-print]').forEach(function (b) { b.addEventListener('click', function () { window.print(); }); });

  // After saving, land back where you were instead of at the top of the page.
  var SCROLL_KEY = 'mm-scroll';
  document.addEventListener('submit', function (e) {
    var form = e.target;
    if (!form || ((form.method || '').toLowerCase() !== 'post' && !form.hasAttribute('data-keep-scroll'))) return;
    var action = e.submitter && e.submitter.getAttribute('formaction') || form.getAttribute('action') || location.pathname;
    try {
      sessionStorage.setItem(SCROLL_KEY, JSON.stringify({
        from: location.pathname, to: new URL(action, location.href).pathname, y: window.scrollY, at: Date.now()
      }));
    } catch (err) { /* storage blocked: fall back to the browser's default */ }
  });
  // Links like the calendar's month arrows reload the same page, so they stay put too.
  $$('a[data-keep-scroll]').forEach(function (a) {
    a.addEventListener('click', function () {
      try {
        sessionStorage.setItem(SCROLL_KEY, JSON.stringify({
          from: location.pathname, to: new URL(a.href).pathname, y: window.scrollY, at: Date.now()
        }));
      } catch (err) { /* storage blocked */ }
    });
  });
  try {
    var saved = JSON.parse(sessionStorage.getItem(SCROLL_KEY) || 'null');
    sessionStorage.removeItem(SCROLL_KEY);
    var fieldError = $('[data-field-error]');
    var jumpTo = function (y) {
      // Jump straight there: no smooth scrolling, which can feel dizzying after a save.
      var root = document.documentElement;
      root.style.scrollBehavior = 'auto';
      window.scrollTo(0, y);
      requestAnimationFrame(function () { window.scrollTo(0, y); root.style.scrollBehavior = ''; });
    };
    if ('scrollRestoration' in history && saved) history.scrollRestoration = 'manual';
    if (fieldError) {
      // A problem with the form: show it beside the field it's about.
      jumpTo(Math.max(0, fieldError.getBoundingClientRect().top + window.scrollY - window.innerHeight / 3));
    } else if (saved && Date.now() - saved.at < 30000 && (saved.from === location.pathname || saved.to === location.pathname)) {
      jumpTo(saved.y);
    }
  } catch (err) { /* ignore */ }

  // Messages float at the top. Confirmations fade; problems stay until clicked.
  var flash = $('[data-flash]');
  if (flash) {
    $$('.alert-error', flash).forEach(function (el) {
      el.title = 'Click to dismiss';
      el.style.cursor = 'pointer';
      el.addEventListener('click', function () { el.remove(); });
    });
    $$('.alert-ok', flash).forEach(function (el) {
      setTimeout(function () { el.style.transition = 'opacity .4s'; el.style.opacity = '0'; setTimeout(function () { el.remove(); }, 450); }, 5000);
    });
  }
})();
