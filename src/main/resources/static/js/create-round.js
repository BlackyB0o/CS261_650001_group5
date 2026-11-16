(function () {
    const form = document.getElementById("create-round-form");
    const button = document.getElementById("create-round-button");
    const message = document.getElementById("create-round-message");
    const scheduledAtInput = document.getElementById("scheduled-at");

    function toLocalInputValue(date) {
        const pad = function (value) {
            return String(value).padStart(2, "0");
        };
        return date.getFullYear() + "-" + pad(date.getMonth() + 1) + "-" + pad(date.getDate())
            + "T" + pad(date.getHours()) + ":" + pad(date.getMinutes());
    }

    function displayMessage(text, isSuccess) {
        message.textContent = text;
        message.className = "form-message " + (isSuccess ? "success-message" : "error-message");
        message.hidden = false;
    }

    function getToken() {
        try {
            return localStorage.getItem("token");
        } catch (error) {
            return null;
        }
    }

    const now = new Date();
    now.setMinutes(now.getMinutes() + 1);
    scheduledAtInput.min = toLocalInputValue(now);

    form.addEventListener("submit", async function (event) {
        event.preventDefault();
        message.hidden = true;

        const token = getToken();
        if (!token) {
            const returnTo = encodeURIComponent(window.location.pathname + window.location.search);
            window.location.replace("/login.html?returnTo=" + returnTo);
            return;
        }

        const request = {
            storeName: document.getElementById("shop").value.trim(),
            locationName: document.getElementById("location").value.trim(),
            scheduledAt: scheduledAtInput.value,
            serviceFee: Number(document.getElementById("fee").value)
        };

        button.disabled = true;
        button.textContent = "กำลังเปิดรอบ...";

        try {
            const response = await fetch("/api/v1/delivery-rounds", {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                    "Authorization": (localStorage.getItem("tokenType") || "Bearer") + " " + token
                },
                body: JSON.stringify(request)
            });

            let result;
            try {
                result = await response.json();
            } catch (error) {
                displayMessage("เซิร์ฟเวอร์ตอบกลับในรูปแบบที่ไม่ถูกต้อง กรุณาลองอีกครั้ง", false);
                return;
            }

            if (response.status === 401) {
                localStorage.removeItem("token");
                localStorage.removeItem("tokenType");
                const returnTo = encodeURIComponent(window.location.pathname + window.location.search);
                window.location.replace("/login.html?returnTo=" + returnTo);
                return;
            }

            if (!response.ok || !result || result.status !== "SUCCESS" || !result.data) {
                const details = Array.isArray(result && result.errors)
                    ? result.errors
                        .filter(function (item) {
                            return item && typeof item.field === "string" && typeof item.message === "string";
                        })
                        .map(function (item) {
                            return item.field + ": " + item.message;
                        })
                    : [];
                displayMessage([result && result.message
                    ? result.message
                    : "เปิดรอบรับหิ้วไม่สำเร็จ (HTTP " + response.status + ")"].concat(details).join("\n"), false);
                return;
            }

            window.location.assign("/my-round-detailod.html?roundId=" + encodeURIComponent(result.data.roundId));
        } catch (error) {
            displayMessage("ไม่สามารถเชื่อมต่อเซิร์ฟเวอร์ได้ กรุณาลองอีกครั้ง", false);
        } finally {
            if (window.location.pathname.endsWith("create-round.html")) {
                button.disabled = false;
                button.textContent = "เปิดรอบรับหิ้ว";
            }
        }
    });
})();
