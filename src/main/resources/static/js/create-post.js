(function () {
    const form = document.getElementById("create-post-form");
    const button = document.getElementById("create-post-button");
    const message = document.getElementById("create-post-message");
    const dateInput = document.getElementById("date");

    function showMessage(text) {
        message.textContent = text;
        message.className = "form-message error-message";
        message.hidden = false;
    }

    function localDateString(date) {
        const year = date.getFullYear();
        const month = String(date.getMonth() + 1).padStart(2, "0");
        const day = String(date.getDate()).padStart(2, "0");
        return year + "-" + month + "-" + day;
    }

    function getAuthHeaders() {
        const token = localStorage.getItem("token");
        if (!token) {
            return null;
        }
        return {
            "Content-Type": "application/json",
            "Authorization": (localStorage.getItem("tokenType") || "Bearer") + " " + token
        };
    }

    const today = localDateString(new Date());
    dateInput.min = today;

    form.addEventListener("submit", async function (event) {
        event.preventDefault();
        message.hidden = true;

        let headers;
        try {
            headers = getAuthHeaders();
        } catch (error) {
            showMessage("ไม่สามารถอ่านสถานะเข้าสู่ระบบได้ กรุณาเข้าสู่ระบบใหม่");
            return;
        }
        if (!headers) {
            window.location.replace("/login.html?returnTo=" + encodeURIComponent(window.location.pathname));
            return;
        }

        const budgetValue = document.getElementById("budget").value;
        const additionalDetails = document.getElementById("details").value.trim();
        const request = {
            postTitle: document.getElementById("post-title").value.trim(),
            shopName: document.getElementById("shop-name").value.trim(),
            productDetail: document.getElementById("product").value.trim(),
            quantity: Number(document.getElementById("quantity").value),
            carryFee: Number(document.getElementById("carry-fee").value),
            desiredDate: dateInput.value,
            location: document.getElementById("location").value.trim(),
            budget: budgetValue === "" ? null : Number(budgetValue),
            additionalDetails: additionalDetails || null
        };

        button.disabled = true;
        button.textContent = "กำลังสร้างโพสต์...";
        try {
            const response = await fetch("/api/v1/posts", {
                method: "POST",
                headers: headers,
                body: JSON.stringify(request)
            });
            let result;
            try {
                result = await response.json();
            } catch (error) {
                showMessage("เซิร์ฟเวอร์ตอบกลับในรูปแบบที่ไม่ถูกต้อง กรุณาลองอีกครั้ง");
                return;
            }

            if (response.status === 401) {
                localStorage.removeItem("token");
                localStorage.removeItem("tokenType");
                window.location.replace("/login.html?returnTo=" + encodeURIComponent(window.location.pathname));
                return;
            }
            if (!response.ok || !result || result.status !== "SUCCESS"
                || !result.data || !result.data.postId) {
                const fieldErrors = Array.isArray(result && result.errors)
                    ? result.errors.filter(function (item) {
                        return item && typeof item.field === "string" && typeof item.message === "string";
                    }).map(function (item) {
                        return item.field + ": " + item.message;
                    })
                    : [];
                showMessage([result && typeof result.message === "string"
                    ? result.message
                    : "สร้างโพสต์ไม่สำเร็จ (HTTP " + response.status + ")"].concat(fieldErrors).join("\n"));
                return;
            }
            window.location.assign("/my-buyer-post-detail.html?postId="
                + encodeURIComponent(result.data.postId));
        } catch (error) {
            showMessage("ไม่สามารถเชื่อมต่อเซิร์ฟเวอร์ได้ กรุณาลองอีกครั้ง");
        } finally {
            if (window.location.pathname.endsWith("create-buyer-post.html")) {
                button.disabled = false;
                button.textContent = "สร้างโพสต์";
            }
        }
    });
})();
