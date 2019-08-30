# wikipedia-android-lite
Performance testing for mobile-html.

## Notes
None of the calls subtract out network load time so pages that haven't been loaded before will be slower as they're uncached.
 
`window.requestAnimationFrame` doesn't seem to wait until after DOM updates have been rendered to be called. This is different than the iOS behavior and makes it more difficult to measure the actual first paint. Any advice is appreciated on how to measure this in a better way.

## Buttons

### Load
Loads the page into the `mobile-html-shell` page and measures the time between calling `pagelib.c1.Page.load(` and the callback from a `window.requestAnimationFrame` after the load promise resolves.

### First
Loads the first section of the page into the `mobile-html-shell` page and measures the time between calling `pagelib.c1.Page.loadFirstSection(` and the callback from a `window.requestAnimationFrame` after the load promise resolves. This is still loading the full content but only appending the first section. The rest of the content could be added after a delay. For longer articles, the time to first paint is noticeably faster on device with this approach when compared to appending the whole article at once.

### Full
Loads the full `mobile-html` page, waits for it to finish loading, then calls `pagelib.c1.Page.setup(`. Measures the time from before the request is kicked off until after the setup completion is called.
