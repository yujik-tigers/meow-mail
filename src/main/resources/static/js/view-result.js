document.querySelectorAll('[data-close-tab]').forEach(button => {
    button.addEventListener('click', () => {
        window.close();

        window.setTimeout(() => {
            const fallback = document.querySelector('[data-close-fallback]');
            if (fallback) {
                fallback.classList.remove('hidden');
            }
        }, 150);
    });
});
