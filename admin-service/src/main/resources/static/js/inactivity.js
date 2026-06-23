(function () {
    const TIMEOUT = 30 * 60 * 1000;
    let timer;

    function triggerLogout() {
        document.getElementById('logout-form').submit();
    }

    function reset() {
        clearTimeout(timer);
        timer = setTimeout(triggerLogout, TIMEOUT);
    }

    ['mousemove', 'mousedown', 'keypress', 'scroll', 'touchstart'].forEach(function (e) {
        document.addEventListener(e, reset, true);
    });

    reset();
}());
