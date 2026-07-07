document.addEventListener('DOMContentLoaded', function () {
    document.querySelectorAll('[data-modal-open]').forEach(function (btn) {
        btn.addEventListener('click', function () {
            const modal = document.getElementById(this.dataset.modalOpen);
            const input = modal.querySelector('[data-confirm-input]');
            const confirmBtn = modal.querySelector('[data-confirm-btn]');
            input.value = '';
            confirmBtn.disabled = true;
            modal.style.display = 'flex';
            input.focus();
        });
    });

    document.querySelectorAll('.modal-overlay').forEach(function (modal) {
        modal.addEventListener('click', function (e) {
            if (e.target === modal) {
                modal.style.display = 'none';
            }
        });

        const input = modal.querySelector('[data-confirm-input]');
        if (!input) return;
        const confirmBtn = modal.querySelector('[data-confirm-btn]');
        input.addEventListener('input', function () {
            confirmBtn.disabled = this.value !== input.dataset.confirmPhrase;
        });
    });

    document.querySelectorAll('[data-modal-close]').forEach(function (btn) {
        btn.addEventListener('click', function () {
            document.getElementById(this.dataset.modalClose).style.display = 'none';
        });
    });
});
