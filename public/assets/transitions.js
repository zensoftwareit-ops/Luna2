(() => {
  'use strict';

  const reducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)');
  const leaveDuration = 90;
  let navigating = false;

  const reset = () => {
    navigating = false;
    document.body.classList.remove('luna-page-leaving');
  };

  window.addEventListener('pageshow', reset);

  document.addEventListener('click', (event) => {
    if (
      navigating
      || reducedMotion.matches
      || event.defaultPrevented
      || event.button !== 0
      || event.metaKey
      || event.ctrlKey
      || event.shiftKey
      || event.altKey
    ) {
      return;
    }

    const anchor = event.target instanceof Element ? event.target.closest('a[href]') : null;
    if (
      !anchor
      || anchor.hasAttribute('download')
      || anchor.hasAttribute('data-no-page-transition')
      || (anchor.target && anchor.target.toLowerCase() !== '_self')
    ) {
      return;
    }

    const destination = new URL(anchor.href, window.location.href);
    if (
      destination.origin !== window.location.origin
      || !['http:', 'https:'].includes(destination.protocol)
      || (
        destination.pathname === window.location.pathname
        && destination.search === window.location.search
        && destination.hash !== window.location.hash
      )
    ) {
      return;
    }

    event.preventDefault();
    navigating = true;
    document.body.classList.add('luna-page-leaving');
    window.setTimeout(() => window.location.assign(destination.href), leaveDuration);
  });
})();
