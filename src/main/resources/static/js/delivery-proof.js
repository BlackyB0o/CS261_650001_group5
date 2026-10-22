(function () {
    const orderId = new URLSearchParams(window.location.search).get("orderId");
    const message = document.getElementById("completion-message");
    const form = document.getElementById("completion-form");
    const formMessage = document.getElementById("completion-form-message");
    const submitButton = document.getElementById("completion-submit");
    let order;

    function showMessage(target, text, isError) {
        target.textContent = text;
        target.className = "form-message " + (isError ? "error-message" : "success-message");
        target.hidden = false;
    }

    function currentUserId() {
        try {
            const token = localStorage.getItem("token");
            if (!token) {
                return null;
            }
            const encoded = token.split(".")[1];
            const base64 = encoded.replace(/-/g, "+").replace(/_/g, "/");
            const claims = JSON.parse(atob(base64 + "=".repeat((4 - base64.length % 4) % 4)));
            const userId = Number(claims.sub);
            return Number.isSafeInteger(userId) && userId > 0 ? userId : null;
        } catch (error) {
            return null;
        }
    }

    function authHeaders() {
        const token = localStorage.getItem("token");
        return token
            ? { "Authorization": (localStorage.getItem("tokenType") || "Bearer") + " " + token }
            : null;
    }

    function redirectToLogin() {
        window.location.replace("/login.html?returnTo="
            + encodeURIComponent(window.location.pathname + window.location.search));
    }

    async function readResponse(response) {
        try {
            return await response.json();
        } catch (error) {
            throw new Error("เซิร์ฟเวอร์ตอบกลับในรูปแบบที่ไม่ถูกต้อง กรุณาลองอีกครั้ง");
        }
    }

    function setOrderFields(data) {
        order = data;
        document.title = "หลักฐานออเดอร์ #" + order.orderId + " | CarryHub";
        document.getElementById("completion-order-link").href =
            "order-detail.html?orderId=" + encodeURIComponent(order.orderId);
        document.getElementById("completion-back-link").href =
            "order-detail.html?orderId=" + encodeURIComponent(order.orderId);
        document.getElementById("completion-order-label").textContent = "ORDER #" + order.orderId;
        document.getElementById("completion-order-status").textContent = order.status || "-";
        document.getElementById("completion-summary-id").textContent = "RO" + order.orderId;
        document.getElementById("completion-summary-status").textContent = order.status || "-";
        document.getElementById("completion-location").textContent = order.handoffLocation || "-";
        const rows = document.querySelectorAll(".proof-info-row .proof-info-value");
        rows[0].textContent = "#" + order.orderId;
        rows[1].textContent = "รอบ #" + order.roundId;
        rows[2].textContent = "-";
        rows[3].textContent = order.buyerName || "-";
        rows[4].textContent = order.handoffLocation || "-";
        form.hidden = !(currentUserId() === Number(order.buyerId) && order.status === "PAID");
        if (order.status !== "PAID" && order.status !== "COMPLETED") {
            showMessage(message, "ยืนยันรับสินค้าได้หลังยืนยันการชำระเงินแล้วเท่านั้น", false);
        }
    }

    function displayCompletion(completion) {
        order.status = completion.orderStatus || "COMPLETED";
        document.getElementById("completion-order-status").textContent = order.status;
        document.getElementById("completion-summary-status").textContent =
            "ยืนยันรับสินค้าแล้ว · เงิน " + (completion.paymentStatus || "RELEASED");
        document.getElementById("completion-evidence-status").textContent = "แนบหลักฐานแล้ว";
        form.hidden = true;

        const details = document.querySelector(".proof-info-list");
        const releaseRow = document.createElement("div");
        releaseRow.className = "proof-info-row";
        const label = document.createElement("span");
        label.className = "proof-info-label";
        label.textContent = "ยอดเงินที่ปล่อยให้ผู้รับหิ้ว";
        const amount = document.createElement("span");
        amount.className = "proof-info-value";
        amount.textContent = Number(completion.releasedAmount).toLocaleString("th-TH", {
            minimumFractionDigits: 2
        }) + " บาท";
        releaseRow.append(label, amount);
        details.appendChild(releaseRow);

        const proofCard = form.closest(".proof-card");
        const evidenceUrl = new URL(completion.evidenceImageUrl, window.location.origin);
        if (evidenceUrl.origin === window.location.origin) {
            const proofLink = document.createElement("a");
            proofLink.href = evidenceUrl.href;
            proofLink.target = "_blank";
            proofLink.rel = "noopener noreferrer";
            proofLink.textContent = "เปิดหลักฐานการรับสินค้า";
            proofLink.className = "proof-btn proof-btn-secondary";
            proofCard.appendChild(proofLink);
        }
        showMessage(message, "ออเดอร์เสร็จสมบูรณ์และเงินถูกปล่อยให้ผู้รับหิ้วแล้ว", false);
    }

    async function loadCompletion() {
        const headers = authHeaders();
        if (!headers) {
            redirectToLogin();
            return;
        }
        try {
            const response = await fetch("/api/v1/orders/" + encodeURIComponent(orderId) + "/completion", {
                headers: headers
            });
            const result = await readResponse(response);
            if (response.status === 401) {
                localStorage.removeItem("token");
                localStorage.removeItem("tokenType");
                redirectToLogin();
                return;
            }
            if (response.status === 404 && result && result.message === "Order completion not found") {
                document.getElementById("completion-evidence-status").textContent = "ยังไม่แนบรูปภาพ";
                if (order.status === "COMPLETED") {
                    showMessage(message, "ไม่พบข้อมูลการยืนยันรับสินค้าของออเดอร์นี้", true);
                } else if (order.status === "PAID" && currentUserId() === Number(order.buyerId)) {
                    showMessage(message, "อัปโหลดรูปหลักฐานเพื่อยืนยันรับสินค้าและปิดออเดอร์", false);
                } else if (order.status === "PAID") {
                    showMessage(message, "รอผู้ฝากซื้อยืนยันการรับสินค้า", false);
                }
                return;
            }
            if (!response.ok || !result || result.status !== "SUCCESS" || !result.data) {
                showMessage(message, result && typeof result.message === "string"
                    ? result.message
                    : "โหลดผลการยืนยันรับสินค้าไม่สำเร็จ (HTTP " + response.status + ")", true);
                return;
            }
            displayCompletion(result.data);
        } catch (error) {
            showMessage(message, error.message || "ไม่สามารถเชื่อมต่อเซิร์ฟเวอร์ได้ กรุณาลองอีกครั้ง", true);
        }
    }

    form.addEventListener("submit", async function (event) {
        event.preventDefault();
        const fileInput = document.getElementById("evidence-image");
        const file = fileInput.files[0];
        if (!file) {
            showMessage(formMessage, "กรุณาแนบรูปหลักฐานการรับสินค้า", true);
            return;
        }
        if (!["image/jpeg", "image/png"].includes(file.type)) {
            showMessage(formMessage, "แนบได้เฉพาะไฟล์ JPEG หรือ PNG", true);
            return;
        }
        if (file.size > 5 * 1024 * 1024) {
            showMessage(formMessage, "ขนาดไฟล์ต้องไม่เกิน 5 MB", true);
            return;
        }

        let headers;
        try {
            headers = authHeaders();
        } catch (error) {
            showMessage(formMessage, "ไม่สามารถอ่านสถานะเข้าสู่ระบบได้ กรุณาเข้าสู่ระบบใหม่", true);
            return;
        }
        if (!headers) {
            redirectToLogin();
            return;
        }
        const body = new FormData();
        body.append("evidenceImage", file);
        submitButton.disabled = true;
        try {
            const response = await fetch("/api/v1/orders/" + encodeURIComponent(orderId) + "/completion", {
                method: "POST",
                headers: headers,
                body: body
            });
            const result = await readResponse(response);
            if (response.status === 401) {
                localStorage.removeItem("token");
                localStorage.removeItem("tokenType");
                redirectToLogin();
                return;
            }
            if (!response.ok || !result || result.status !== "SUCCESS" || !result.data) {
                const errors = Array.isArray(result && result.errors)
                    ? result.errors.filter(function (item) {
                        return item && item.field && item.message;
                    }).map(function (item) {
                        return item.field + ": " + item.message;
                    })
                    : [];
                showMessage(formMessage, [result && result.message
                    ? result.message
                    : "ยืนยันรับสินค้าไม่สำเร็จ (HTTP " + response.status + ")"].concat(errors).join("\n"), true);
                return;
            }
            displayCompletion(result.data);
            showMessage(formMessage, "ยืนยันรับสินค้าเรียบร้อยแล้ว", false);
        } catch (error) {
            showMessage(formMessage, error.message || "ไม่สามารถเชื่อมต่อเซิร์ฟเวอร์ได้ กรุณาลองอีกครั้ง", true);
        } finally {
            submitButton.disabled = false;
        }
    });

    async function loadOrder() {
        if (!orderId || !/^[1-9]\d*$/.test(orderId)) {
            showMessage(message, "ไม่พบรหัสออเดอร์ กรุณาเปิดหน้านี้จากรายละเอียดออเดอร์", true);
            form.hidden = true;
            return;
        }
        let headers;
        try {
            headers = authHeaders();
        } catch (error) {
            showMessage(message, "ไม่สามารถอ่านสถานะเข้าสู่ระบบได้ กรุณาเข้าสู่ระบบใหม่", true);
            return;
        }
        if (!headers) {
            redirectToLogin();
            return;
        }
        try {
            const response = await fetch("/api/v1/orders/" + encodeURIComponent(orderId), {
                headers: headers
            });
            const result = await readResponse(response);
            if (response.status === 401) {
                localStorage.removeItem("token");
                localStorage.removeItem("tokenType");
                redirectToLogin();
                return;
            }
            if (!response.ok || !result || result.status !== "SUCCESS" || !result.data) {
                showMessage(message, result && typeof result.message === "string"
                    ? result.message
                    : "โหลดรายละเอียดออเดอร์ไม่สำเร็จ (HTTP " + response.status + ")", true);
                form.hidden = true;
                return;
            }
            setOrderFields(result.data);
            await loadCompletion();
        } catch (error) {
            showMessage(message, error.message || "ไม่สามารถเชื่อมต่อเซิร์ฟเวอร์ได้ กรุณาลองอีกครั้ง", true);
        }
    }

    form.hidden = true;
    loadOrder();
})();
