function openDownloadModal() {
    document.getElementById('confirmInput').value = '';
    document.getElementById('confirmBtn').disabled = true;
    document.getElementById('downloadModal').style.display = 'flex';
    document.getElementById('confirmInput').focus();
}

function closeDownloadModal() {
    document.getElementById('downloadModal').style.display = 'none';
}

function handleOverlayClick(e) {
    if (e.target === document.getElementById('downloadModal')) {
        closeDownloadModal();
    }
}

document.addEventListener('DOMContentLoaded', function () {
    const input = document.getElementById('confirmInput');
    if (input) {
        input.addEventListener('input', function () {
            document.getElementById('confirmBtn').disabled = this.value !== '확인합니다';
        });
    }
});
