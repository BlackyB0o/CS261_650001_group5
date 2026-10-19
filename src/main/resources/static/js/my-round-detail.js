(function () {
    const roundId = new URLSearchParams(window.location.search).get("roundId");
    const message = document.getElementById("my-round-message");
    const content = document.getElementById("my-round-content");
    const statusForm = document.getElementById("round-status-form");
    const statusSelect = document.getElementById("round-status-select");
    const statusButton = document.getElementById("round-status-button");
    const ordersSection = document.getElementById("round-orders-section");
    const ordersList = document.getElementById("round-orders-list");
    const ordersMessage = document.getElementById("round-orders-message");
    let round;

    const transitions = {
        OPEN: ["CLOSED", "CANCELLED"],
        CLOSED: ["COMPLETED", "CANCELLED"],
        COMPLETED: [],
        CANCELLED: []
    };
    const statusLabels = {
        OPEN: "เปิดรับฝากซื้อ",
        CLOSED: "ปิดรับออเดอร์",
        COMPLETED: "เสร็จสิ้น",
        CANCELLED: "ยกเลิก"
    };

    function showMessage(text, isError) {
        message.textContent = text;
        message.className = "form-message " + (isError ? "error-message" : "success-message");
        message.hidden = false;
    }

    function displayRound() {
        document.title = (round.storeName || "จัดการรอบรับหิ้ว") + " | CarryHub";
        document.getElementById("my-round-status").textContent = statusLabels[round.status] || round.status;
        document.getElementById("my-round-id").textContent = "รอบรับหิ้ว #" + round.roundId;
        document.getElementById("my-round-store").textContent = round.storeName || "-";
        document.getElementById("my-round-runner").textContent = round.runnerName || "-";

        const date = new Date(round.scheduledAt);
        document.getElementById("my-round-scheduled").textContent = Number.isNaN(date.getTime())
            ? round.scheduledAt || "-"
            : new Intl.DateTimeFormat("th-TH", { dateStyle: "full", timeStyle: "short" }).format(date);
        document.getElementById("my-round-fee").textContent =
            Number(round.serviceFee).toLocaleString("th-TH") + " บาท / รายการ";
        document.getElementById("my-round-location").textContent = round.locationName || "-";

        const allowedStatuses = transitions[round.status] || [];
        statusSelect.replaceChildren();
        allowedStatuses.forEach(function (status) {
            const option = document.createElement("option");
            option.value = status;
            option.textContent = statusLabels[status];
            statusSelect.appendChild(option);
        });
        statusForm.hidden = allowedStatuses.length === 0;
        content.hidden = false;
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

    async function readResponse(response) {
        try {
            return await response.json();
        } catch (error) {
            throw new Error("เซิร์ฟเวอร์ตอบกลับในรูปแบบที่ไม่ถูกต้อง กรุณาลองอีกครั้ง");
        }
    }

    function showOrdersMessage(text, isError) {
        ordersMessage.textContent = text;
        ordersMessage.className = "form-message " + (isError ? "error-message" : "");
        ordersMessage.hidden = false;
    }

    function renderOrder(order) {
        const card = document.createElement("article");
        card.className = "order-card";
        const header = document.createElement("div");
        header.className = "order-card-header";
        const id = document.createElement("h3");
        id.textContent = "ออเดอร์ #" + order.orderId;
        const status = document.createElement("span");
        status.className = "order-status";
        status.textContent = order.status || "-";
        header.append(id, status);

        const body = document.createElement("div");
        body.className = "order-card-body";
        const details = document.createElement("div");
        details.className = "order-product-info";
        const buyer = document.createElement("p");
        buyer.textContent = "ผู้ฝากซื้อ: " + (order.buyerName || "-");
        const method = document.createElement("p");
        method.textContent = "วิธีรับสินค้า: "
            + (order.handoffMethod === "MEETUP" ? "นัดรับ" : order.handoffMethod === "DELIVERY" ? "ส่งถึงที่" : "-");
        const location = document.createElement("p");
        location.textContent = "สถานที่: " + (order.handoffLocation || "-");
        const products = document.createElement("ul");
        (Array.isArray(order.items) ? order.items : []).forEach(function (item) {
            const product = document.createElement("li");
            product.textContent = (item.productName || "สินค้า") + " × " + item.quantity
                + (item.note ? " — " + item.note : "");
            products.appendChild(product);
        });
        details.append(buyer, method, location, products);
        body.appendChild(details);

        const footer = document.createElement("div");
        footer.className = "order-card-footer";
        const link = document.createElement("a");
        link.className = "order-detail-btn";
        link.href = "order-detail.html?orderId=" + encodeURIComponent(order.orderId);
        link.textContent = "ดูรายละเอียด →";
        footer.appendChild(link);
        card.append(header, body, footer);
        return card;
    }

    async function loadRoundOrders() {
        ordersSection.hidden = false;
        ordersList.replaceChildren();
        try {
            const token = localStorage.getItem("token");
            if (!token) {
                window.location.replace("/login.html?returnTo="
                    + encodeURIComponent(window.location.pathname + window.location.search));
                return;
            }
            const response = await fetch("/api/v1/orders?roundId=" + encodeURIComponent(roundId), {
                headers: {
                    "Authorization": (localStorage.getItem("tokenType") || "Bearer") + " " + token
                }
            });
            const result = await readResponse(response);
            if (response.status === 401) {
                localStorage.removeItem("token");
                localStorage.removeItem("tokenType");
                window.location.replace("/login.html?returnTo="
                    + encodeURIComponent(window.location.pathname + window.location.search));
                return;
            }
            if (!response.ok || !result || result.status !== "SUCCESS" || !Array.isArray(result.data)) {
                showOrdersMessage(result && typeof result.message === "string"
                    ? result.message
                    : "โหลดรายการฝากซื้อในรอบไม่สำเร็จ (HTTP " + response.status + ")", true);
                return;
            }
            if (result.data.length === 0) {
                showOrdersMessage("ยังไม่มีรายการฝากซื้อในรอบนี้", false);
                return;
            }
            ordersMessage.hidden = true;
            result.data.forEach(function (order) {
                ordersList.appendChild(renderOrder(order));
            });
        } catch (error) {
            showOrdersMessage(error.message || "ไม่สามารถเชื่อมต่อเซิร์ฟเวอร์ได้ กรุณาลองอีกครั้ง", true);
        }
    }

    async function loadRound() {
        if (!roundId || !/^[1-9]\d*$/.test(roundId)) {
            showMessage("ไม่พบรหัสรอบรับหิ้ว กรุณาเลือกรอบจากรายการของฉัน", true);
            return;
        }
        try {
            const response = await fetch("/api/v1/delivery-rounds/" + encodeURIComponent(roundId));
            const result = await readResponse(response);
            if (!response.ok || !result || result.status !== "SUCCESS" || !result.data) {
                showMessage(result && typeof result.message === "string"
                    ? result.message
                    : "โหลดรายละเอียดรอบไม่สำเร็จ (HTTP " + response.status + ")", true);
                return;
            }
            round = result.data;
            displayRound();
            await loadRoundOrders();
        } catch (error) {
            showMessage(error.message || "ไม่สามารถเชื่อมต่อเซิร์ฟเวอร์ได้ กรุณาลองอีกครั้ง", true);
        }
    }

    statusForm.addEventListener("submit", async function (event) {
        event.preventDefault();
        let headers;
        try {
            headers = getAuthHeaders();
        } catch (error) {
            showMessage("ไม่สามารถอ่านสถานะเข้าสู่ระบบได้ กรุณาเข้าสู่ระบบใหม่", true);
            return;
        }
        if (!headers) {
            window.location.replace("/login.html?returnTo=" + encodeURIComponent(window.location.pathname + window.location.search));
            return;
        }

        statusButton.disabled = true;
        statusButton.textContent = "กำลังบันทึก...";
        try {
            const response = await fetch("/api/v1/delivery-rounds/" + encodeURIComponent(roundId) + "/status", {
                method: "PATCH",
                headers: headers,
                body: JSON.stringify({ status: statusSelect.value })
            });
            const result = await readResponse(response);
            if (response.status === 401) {
                localStorage.removeItem("token");
                localStorage.removeItem("tokenType");
                window.location.replace("/login.html?returnTo=" + encodeURIComponent(window.location.pathname + window.location.search));
                return;
            }
            if (!response.ok || !result || result.status !== "SUCCESS" || !result.data) {
                showMessage(result && typeof result.message === "string"
                    ? result.message
                    : "อัปเดตสถานะรอบไม่สำเร็จ (HTTP " + response.status + ")", true);
                return;
            }

            round.status = result.data.status;
            displayRound();
            showMessage("อัปเดตสถานะรอบเรียบร้อยแล้ว", false);
        } catch (error) {
            showMessage(error.message || "ไม่สามารถเชื่อมต่อเซิร์ฟเวอร์ได้ กรุณาลองอีกครั้ง", true);
        } finally {
            statusButton.disabled = false;
            statusButton.textContent = "บันทึกสถานะ";
        }
    });

    loadRound();
})();
