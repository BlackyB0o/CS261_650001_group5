(function () {
    const list = document.getElementById("my-orders-list");
    const message = document.getElementById("my-orders-message");
    const count = document.getElementById("round-orders-count");
    if (!list || !message || !count) {
        return;
    }

    function showMessage(text, isError) {
        message.textContent = text;
        message.className = "form-message " + (isError ? "error-message" : "");
        message.hidden = false;
    }

    function statusLabel(status) {
        return status === "PENDING" ? "รอดำเนินการ"
            : status === "PAID" ? "ชำระเงินแล้ว"
                : status === "COMPLETED" ? "เสร็จสิ้น"
                    : status || "-";
    }

    function formatDate(value) {
        const date = new Date(value);
        return Number.isNaN(date.getTime())
            ? value || "-"
            : new Intl.DateTimeFormat("th-TH", { dateStyle: "medium", timeStyle: "short" }).format(date);
    }

    function renderOrder(order) {
        const card = document.createElement("article");
        card.className = "order-card";
        const header = document.createElement("div");
        header.className = "order-card-header";
        const idBox = document.createElement("div");
        const label = document.createElement("span");
        label.className = "order-label";
        label.textContent = "หมายเลขออเดอร์";
        const id = document.createElement("h3");
        id.textContent = "RO" + order.orderId;
        idBox.append(label, id);
        const status = document.createElement("span");
        status.className = "order-status";
        status.textContent = statusLabel(order.status);
        header.append(idBox, status);

        const body = document.createElement("div");
        body.className = "order-card-body";
        const icon = document.createElement("div");
        icon.className = "order-shop-icon";
        icon.textContent = "CH";
        const details = document.createElement("div");
        details.className = "order-product-info";
        const products = document.createElement("h4");
        products.textContent = (Array.isArray(order.items) ? order.items : [])
            .map(function (item) { return (item.productName || "สินค้า") + " × " + item.quantity; })
            .join(", ") || "ไม่มีรายการสินค้า";
        const round = document.createElement("p");
        round.textContent = "รอบรับหิ้ว: #" + order.roundId;
        const handoff = document.createElement("p");
        handoff.className = "order-carrier";
        handoff.textContent = (order.handoffMethod === "MEETUP" ? "นัดรับ" : "ส่งถึงที่")
            + ": " + (order.handoffLocation || "-");
        const created = document.createElement("p");
        created.className = "order-carrier";
        created.textContent = "สร้างเมื่อ " + formatDate(order.createdAt);
        details.append(products, round, handoff, created);
        body.append(icon, details);

        const footer = document.createElement("div");
        footer.className = "order-card-footer";
        const buyer = document.createElement("div");
        buyer.className = "order-price";
        buyer.textContent = "สถานะ " + statusLabel(order.status);
        const link = document.createElement("a");
        link.className = "order-detail-btn";
        link.href = "order-detail.html?orderId=" + encodeURIComponent(order.orderId);
        link.textContent = "ดูรายละเอียด →";
        footer.append(buyer, link);
        card.append(header, body, footer);
        return card;
    }

    async function loadOrders() {
        list.replaceChildren();
        let token;
        try {
            token = localStorage.getItem("token");
        } catch (error) {
            showMessage("ไม่สามารถอ่านสถานะเข้าสู่ระบบได้ กรุณาเข้าสู่ระบบใหม่", true);
            return;
        }
        if (!token) {
            window.location.replace("/login.html?returnTo=%2Forders.html%23round-orders-panel");
            return;
        }
        try {
            const response = await fetch("/api/v1/orders/my", {
                headers: {
                    "Authorization": (localStorage.getItem("tokenType") || "Bearer") + " " + token
                }
            });
            let result;
            try {
                result = await response.json();
            } catch (error) {
                showMessage("เซิร์ฟเวอร์ตอบกลับในรูปแบบที่ไม่ถูกต้อง กรุณาลองอีกครั้ง", true);
                return;
            }
            if (response.status === 401) {
                localStorage.removeItem("token");
                localStorage.removeItem("tokenType");
                window.location.replace("/login.html?returnTo=%2Forders.html%23round-orders-panel");
                return;
            }
            if (!response.ok || !result || result.status !== "SUCCESS" || !Array.isArray(result.data)) {
                showMessage(result && typeof result.message === "string"
                    ? result.message
                    : "โหลดรายการฝากซื้อไม่สำเร็จ (HTTP " + response.status + ")", true);
                return;
            }
            count.textContent = result.data.length;
            const existingBuyerAndCarrierOrders =
                document.querySelectorAll("#buyer-panel .order-card, #carrier-panel .order-card").length;
            document.getElementById("total-count").textContent =
                existingBuyerAndCarrierOrders + result.data.length;
            if (result.data.length === 0) {
                showMessage("คุณยังไม่มีรายการฝากซื้อจากรอบรับหิ้ว", false);
                return;
            }
            message.hidden = true;
            result.data.forEach(function (order) {
                list.appendChild(renderOrder(order));
            });
        } catch (error) {
            showMessage("ไม่สามารถเชื่อมต่อเซิร์ฟเวอร์ได้ กรุณาลองอีกครั้ง", true);
        }
    }

    loadOrders();
})();
