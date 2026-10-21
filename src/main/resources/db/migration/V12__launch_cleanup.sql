-- Launch cleanup: drop the "[Placeholder] " prefix from the starting style descriptions.
update option_choice set description = substring(description from 15) where description like '[Placeholder] %';
