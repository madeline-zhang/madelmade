-- Crop now also stores the zoom and position, e.g. "square:1.40:0.200:-0.500".
alter table photo alter column crop type varchar(60);
