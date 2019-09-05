# wikipedia-android-lite
Performance testing for mobile-html.

## Notes
None of the calls subtract out network load time so pages that haven't been loaded before will be slower as they're uncached.

`setTimeout` with 1ms inside of a `window.requestAnimationFrame` seems to be the best way to accurately wait for the page to render before receiving the callback.

## Buttons

### Load
Loads the page into the `mobile-html-shell` page and measures the time between calling `pagelib.c1.Page.load(` and the page rendering.

### First
Loads the first section of the page into the `mobile-html-shell` page and measures the time between calling `pagelib.c1.Page.loadProgressively(` and the page rendering the first section. This is still loading the full content but only appending the first section immediately. The rest of the content is added after a 100ms delay. For longer articles, the time to first paint is noticeably faster on device with this approach when compared to appending the whole article at once.

### Full
Loads the full `mobile-html` page using the split prototype from `apps.wmflabs.org` https://gerrit.wikimedia.org/r/c/mediawiki/services/mobileapps/+/534531. Measures the time between sending the network request and the `{action: 'setup'}` callback from pagelib.
