(function () {
    const protectedPages = new Set([
        "accept-job-success.html",
        "admin-reports.html",
        "buyer-post-review.html",
        "buyer-post-success.html",
        "create-buyer-post.html",
        "create-round.html",
        "delivery-proof.html",
        "my-buyer-post-detail.html",
        "my-report-detail.html",
        "my-round-detail.html",
        "my-round-detailod.html",
        "order-detail.html",
        "orders.html",
        "profile.html",
        "report-detail.html",
        "report-success.html",
        "report.html",
        "round-order-success.html",
        "round-requests.html",
        "round-review.html",
        "round-success.html"
    ]);
    const protectedDestinations = protectedPages;
    const authPages = new Set([
        "forgot-password.html",
        "login.html",
        "register.html",
        "reset-password.html",
        "verify-otp.html"
    ]);

    function hasValidToken() {
        let token;
        try {
            token = localStorage.getItem("token");
        } catch (error) {
            return false;
        }
        if (!token) {
            return false;
        }

        try {
            const payload = token.split(".")[1];
            const base64 = payload.replace(/-/g, "+").replace(/_/g, "/");
            const claims = JSON.parse(atob(base64 + "=".repeat((4 - base64.length % 4) % 4)));
            if (typeof claims.exp !== "number" || claims.exp * 1000 <= Date.now()) {
                localStorage.removeItem("token");
                localStorage.removeItem("tokenType");
                return false;
            }
            return true;
        } catch (error) {
            localStorage.removeItem("token");
            localStorage.removeItem("tokenType");
            return false;
        }
    }

    function safeReturnPath(path) {
        if (!path || path.charAt(0) !== "/" || path.indexOf("//") === 0) {
            return "";
        }
        const destination = new URL(path, window.location.origin);
        return destination.origin === window.location.origin
            ? destination.pathname + destination.search + destination.hash
            : "";
    }

    function sendToLogin(returnPath) {
        const query = new URLSearchParams();
        const safePath = safeReturnPath(returnPath);
        if (safePath) {
            query.set("returnTo", safePath);
        }
        const queryString = query.toString();
        window.location.replace("/login.html" + (queryString ? "?" + queryString : ""));
    }

    function destinationPage(anchor) {
        try {
            return new URL(anchor.href, window.location.href).pathname.split("/").pop();
        } catch (error) {
            return "";
        }
    }

    const currentPage = window.location.pathname.split("/").pop() || "index.html";
    const authenticated = hasValidToken();

    if (!authenticated && protectedPages.has(currentPage)) {
        sendToLogin(window.location.pathname + window.location.search + window.location.hash);
        return;
    }

    if (authPages.has(currentPage)) {
        return;
    }

    document.addEventListener("click", function (event) {
        const target = event.target instanceof Element ? event.target : event.target.parentElement;
        const anchor = target && target.closest("a[href]");
        if (!anchor) {
            return;
        }

        if (anchor.dataset.logout === "true") {
            event.preventDefault();
            localStorage.removeItem("token");
            localStorage.removeItem("tokenType");
            window.location.replace("/login.html");
            return;
        }

        const page = destinationPage(anchor);
        if (!hasValidToken() && protectedDestinations.has(page)) {
            event.preventDefault();
            sendToLogin(anchor.pathname + anchor.search + anchor.hash);
        }
    }, true);

    document.addEventListener("submit", function (event) {
        if (hasValidToken()) {
            return;
        }

        const form = event.target;
        const action = new URL(form.getAttribute("action") || window.location.href, window.location.href);
        const targetPage = action.pathname.split("/").pop();
        if (protectedDestinations.has(targetPage) || protectedPages.has(currentPage)) {
            event.preventDefault();
            sendToLogin(window.location.pathname + window.location.search + window.location.hash);
        }
    }, true);

    document.querySelectorAll("nav").forEach(function (nav) {
        nav.querySelectorAll('a[href$="orders.html"], a[href$="profile.html"]').forEach(function (link) {
            if (!authenticated) {
                link.hidden = true;
            }
        });

        nav.querySelectorAll('a[href$="login.html"], a[href$="register.html"]').forEach(function (link) {
            if (authenticated) {
                link.hidden = true;
            }
        });

        if (authenticated) {
            if (nav.querySelector('[data-guest-access-logout="true"]')) {
                return;
            }
            const logoutLink = document.createElement("a");
            logoutLink.href = "#logout";
            logoutLink.dataset.logout = "true";
            logoutLink.dataset.guestAccessLogout = "true";
            logoutLink.textContent = "ออกจากระบบ";
            nav.appendChild(logoutLink);
        } else if (!nav.querySelector('a[href$="login.html"]')) {
            const loginLink = document.createElement("a");
            loginLink.href = "/login.html";
            loginLink.textContent = "เข้าสู่ระบบ";
            nav.appendChild(loginLink);
        }
    });
})();
