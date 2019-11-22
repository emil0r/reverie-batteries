# reverie-batteries

Batteries included for reverie/CMS.

Following included:

- objects
  - reverie/image (basic image object)
  - reverie/raw (will put out the raw output exactly as is, handle with care)
  - reverie/reset-password (reset password object, renderer enabled)
  - reverie/text (basic text object)
  - reverie/faq (faq object, takes FAQs from FAQ module)

- helpers
  - menu/get-menu-pages gets a tree structure of trees under the root page to be used for constructing a menu
  - breadcrumbs/get-breadcrumbs get a list of pages from the page given up until the root is hit

- modules
  - reverie.module/faq

## Usage

```clojure
[reverie-batteries "0.4.0"]
```

Assumes reverie 0.8 or higher.

## License

Copyright © 2014-2019 Emil Bengtsson

Distributed under the Eclipse Public License either version 1.0 or (at your option) any later version.


---

Coram Deo
