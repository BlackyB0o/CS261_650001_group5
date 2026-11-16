(function () {
    const form = document.getElementById("create-order-form");
    const items = document.getElementById("order-items");
    const message = document.getElementById("create-order-message");
    const button = document.getElementById("create-order-button");
    const roundId = new URLSearchParams(window.location.search).get("roundId");

    function showMessage(text) {
        message.textContent = text;
        message.className = "form-message error-message";
        message.hidden = false;
    }

    function addItem() {
        const row = document.createElement("div");
        row.className = "order-item-fields";
        row.innerHTML = '<div class="form-group"><label>ชื่อสินค้า</label><input name="productName" type="text" required></div>'
            + '<div class="form-group"><label>จำนวน</label><input name="quantity" type="number" min="1" step="1" value="1" required></div>'
            + '<div class="form-group"><label>หมายเหตุ (ไม่บังคับ)</label><input name="note" type="text"></div>'
            + '<button class="remove-order-item" type="button">ลบรายการ</button>';
        items.appendChild(row);
        updateRemoveButtons();
    }

    function updateRemoveButtons() {
        const rows = items.querySelectorAll(".order-item-fields");
        rows.forEach(function (row) {
            const removeButton = row.querySelector(".remove-order-item");
            removeButton.hidden = rows.length === 1;
            removeButton.style.display = rows.length === 1 ? "none" : "";
        });
    }

    async function readResponse(response) {
        try {
            return await response.json();
        } catch (error) {
            throw new Error("เซิร์ฟเวอร์ตอบกลับในรูปแบบที่ไม่ถูกต้อง กรุณาลองอีกครั้ง");
        }
    }

    if (!roundId || !/^[1-9]\d*$/.test(roundId)) {
        showMessage("ไม่พบรหัสรอบรับหิ้ว กรุณาเลือกรอบจากหน้ารายการ");
        form.querySelectorAll("input, select, button").forEach(function (element) {
            element.disabled = true;
        });
        return;
    }
    document.getElementById("order-round-back").href = "round-detail.html?roundId=" + encodeURIComponent(roundId);
    updateRemoveButtons();

    document.getElementById("add-order-item").addEventListener("click", addItem);
    items.addEventListener("click", function (event) {
        if (event.target.matches(".remove-order-item")) {
            event.target.closest(".order-item-fields").remove();
            updateRemoveButtons();
        }
    });

    form.addEventListener("submit", async function (event) {
        event.preventDefault();
        message.hidden = true;

        let token;
        try {
            token = localStorage.getItem("token");
        } catch (error) {
            showMessage("ไม่สามารถอ่านสถานะเข้าสู่ระบบได้ กรุณาเข้าสู่ระบบใหม่");
            return;
        }
        if (!token) {
            window.location.replace("/login.html?returnTo="
                + encodeURIComponent(window.location.pathname + window.location.search));
            return;
        }

        const request = {
            roundId: Number(roundId),
            handoffMethod: document.getElementById("handoff-method").value,
            handoffLocation: document.getElementById("handoff-location").value.trim(),
            items: Array.from(items.querySelectorAll(".order-item-fields")).map(function (row) {
                const note = row.querySelector('[name="note"]').value.trim();
                return {
                    productName: row.querySelector('[name="productName"]').value.trim(),
                    quantity: Number(row.querySelector('[name="quantity"]').value),
                    note: note || null
                };
            })
        };

        button.disabled = true;
        button.textContent = "กำลังส่งรายการ...";
        try {
            const response = await fetch("/api/v1/orders", {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                    "Authorization": (localStorage.getItem("tokenType") || "Bearer") + " " + token
                },
                body: JSON.stringify(request)
            });
            const result = await readResponse(response);
            if (response.status === 401) {
                localStorage.removeItem("token");
                localStorage.removeItem("tokenType");
                window.location.replace("/login.html?returnTo="
                    + encodeURIComponent(window.location.pathname + window.location.search));
                return;
            }
            if (!response.ok || !result || result.status !== "SUCCESS"
                || !result.data || !result.data.orderId) {
                const errors = Array.isArray(result && result.errors)
                    ? result.errors.filter(function (item) {
                        return item && typeof item.field === "string" && typeof item.message === "string";
                    }).map(function (item) {
                        return item.field + ": " + item.message;
                    })
                    : [];
                showMessage([result && typeof result.message === "string"
                    ? result.message
                    : "สร้างรายการฝากซื้อไม่สำเร็จ (HTTP " + response.status + ")"].concat(errors).join("\n"));
                return;
            }
            window.location.assign("/order-detail.html?orderId=" + encodeURIComponent(result.data.orderId));
        } catch (error) {
            showMessage(error.message || "ไม่สามารถเชื่อมต่อเซิร์ฟเวอร์ได้ กรุณาลองอีกครั้ง");
        } finally {
            if (window.location.pathname.endsWith("create-order.html")) {
                button.disabled = false;
                button.textContent = "ยืนยันฝากซื้อ";
            }
        }
    });
})();
