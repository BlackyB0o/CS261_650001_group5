document.addEventListener("DOMContentLoaded", function () {
    document.querySelectorAll("header.navbar").forEach(function (header) {
        const toggle = header.querySelector(".menu-toggle");
        const nav = header.querySelector("nav");

        if (!toggle || !nav) {
            return;
        }

        toggle.addEventListener("click", function () {
            const isOpen = nav.classList.toggle("open");

            toggle.setAttribute("aria-expanded", String(isOpen));
            toggle.setAttribute(
                "aria-label",
                isOpen ? "ปิดเมนู" : "เปิดเมนู"
            );
        });

        nav.addEventListener("click", function (event) {
            if (event.target.closest("a")) {
                nav.classList.remove("open");
                toggle.setAttribute("aria-expanded", "false");
                toggle.setAttribute("aria-label", "เปิดเมนู");
            }
        });
    });
});