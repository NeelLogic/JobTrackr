(() => {
  const root = document.documentElement;
  let theme;
  try {
    const stored = window.localStorage.getItem('jobtrackr-theme');
    theme =
      stored === 'light' || stored === 'dark'
        ? stored
        : window.matchMedia('(prefers-color-scheme: dark)').matches
          ? 'dark'
          : 'light';
  } catch {
    theme = 'light';
  }
  root.dataset.theme = theme;
  root.style.colorScheme = theme;
})();
