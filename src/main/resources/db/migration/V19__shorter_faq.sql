-- The About page now answers delivery and payment itself, so drop those two entries from the editable
-- questions, and merge storing and reheating into one. Only the original wording is touched; anything
-- Madeline rewrote stays as she wrote it.
update setting set value = regexp_replace(value,
    'Do you deliver\?\r?\nYes, nearby\. Madeline quotes the delivery fee when she approves your order, and it''s paid up front as a deposit\.\s*', '')
where key = 'about.faq';
update setting set value = regexp_replace(value,
    'How do I pay\?\r?\nNothing is charged online\. You pay at pickup or delivery with cash, Venmo or Zelle\.\s*', '')
where key = 'about.faq';
update setting set value = regexp_replace(value,
    'How should I store it\?\r?\nTo enjoy it later, slice it and freeze the slices\.(\r?\n){2}Best way to reheat\?\r?\nToast it and butter it up!',
    'How should I store and reheat it?' || chr(10) || 'To enjoy it later, slice it and freeze the slices. To reheat, toast it and butter it up!')
where key = 'about.faq';
-- The decline note now sits under the first-person refund answer, so it speaks in first person too.
update setting set value = 'I can decline any order, for any reason. If I can''t take yours, I''ll let you know, and you won''t be charged anything.'
where key = 'notice.decline'
  and value = 'Madeline has the right to decline any order, for any reason. If she can''t take yours, she''ll let you know, and you won''t be charged anything.';
