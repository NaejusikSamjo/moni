document.querySelectorAll('form[data-loading]').forEach(function (form) {
    form.addEventListener('submit', function () {
        const btn = form.querySelector('button[type="submit"]');
        if (!btn) return;
        btn.classList.add('loading');
        btn.disabled = true;
        btn.dataset.original = btn.textContent;
        btn.textContent = btn.dataset.loadingText || '처리 중...';
        const spinner = document.createElement('span');
        spinner.className = 'btn-spinner';
        btn.prepend(spinner);
    });
});