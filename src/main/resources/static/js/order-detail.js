(function () {
    const main = document.querySelector(".order-detail-page");
    const orderId = new URLSearchParams(window.location.search).get("orderId");

    function element(tag, className, text) {
        const node = document.createElement(tag);
        if (className) {
            node.className = className;
        }
        if (text != null) {
            node.textContent = text;
        }
        return node;
    }

    function showMessage(text, isError) {
        main.replaceChildren();
        const message = element("p", "form-message " + (isError ? "error-message" : ""), text);
        message.setAttribute("role", "status");
        main.appendChild(message);
        main.appendChild(element("a", "", "กลับไปรายการของฉัน")).href = "/orders.html#round-orders-panel";
    }

    function formatDate(value) {
        const date = new Date(value);
        return Number.isNaN(date.getTime())
            ? value || "-"
            : new Intl.DateTimeFormat("th-TH", { dateStyle: "full", timeStyle: "short" }).format(date);
    }

    function addMeta(grid, label, value) {
        const item = element("div");
        item.append(element("span", "", label), element("strong", "", value == null || value === "" ? "-" : value));
        grid.appendChild(item);
    }

    function displayOrder(order) {
        document.title = "ออเดอร์ #" + order.orderId + " | CarryHub";
        main.replaceChildren();
        const breadcrumb = element("div", "order-breadcrumb");
        const back = element("a", "", "รายการของฉัน");
        back.href = "/orders.html#round-orders-panel";
        breadcrumb.append(back, element("span", "", "/"), element("span", "", "รายละเอียดออเดอร์"));

        const header = element("section", "order-header-card");
        const headerMain = element("div", "order-header-main");
        const titleBox = element("div");
        titleBox.append(element("p", "order-eyebrow", "ORDER DETAILS"));
        titleBox.append(element("h1", "", "รายละเอียดออเดอร์"));
        titleBox.append(element("p", "order-id", "หมายเลขออเดอร์ " + order.orderId));
        headerMain.append(titleBox, element("span", "order-status", order.status || "-"));
        header.append(headerMain, element("div", "order-header-divider"));
        const meta = element("div", "order-meta-grid");
        addMeta(meta, "วันที่สั่งซื้อ", formatDate(order.createdAt));
        addMeta(meta, "รอบรับหิ้ว", "#" + order.roundId);
        addMeta(meta, "ผู้ฝากซื้อ", order.buyerName);
        header.appendChild(meta);

        const section = element("section", "order-section-card");
        const heading = element("div", "order-section-heading");
        heading.append(element("h2", "", "รายการสินค้า"));
        heading.append(element("span", "order-item-count", (order.items || []).length + " รายการ"));
        section.appendChild(heading);
        (Array.isArray(order.items) ? order.items : []).forEach(function (item) {
            const row = element("article", "order-product");
            const info = element("div", "order-product-info");
            info.append(element("h3", "", item.productName || "สินค้า"));
            info.append(element("p", "", "จำนวน " + item.quantity));
            if (item.note) {
                info.append(element("p", "", "หมายเหตุ: " + item.note));
            }
            row.append(element("div", "product-placeholder", "CH"), info);
            section.appendChild(row);
        });

        const handoff = element("section", "order-section-card");
        handoff.append(element("h2", "", "การรับสินค้า"));
        handoff.append(element("p", "", "วิธีรับสินค้า: "
            + (order.handoffMethod === "MEETUP" ? "นัดรับ" : order.handoffMethod === "DELIVERY" ? "ส่งถึงที่" : "-")));
        handoff.append(element("p", "", "สถานที่: " + (order.handoffLocation || "-")));

        const actions = element("section", "order-bottom-actions");
        const ordersLink = element("a", "order-back-btn", "กลับไปรายการของฉัน");
        ordersLink.href = "/orders.html#round-orders-panel";
        const roundsLink = element("a", "order-proof-btn", "ดูรอบรับหิ้ว");
        roundsLink.href = "/round-detail.html?roundId=" + encodeURIComponent(order.roundId);
        actions.append(ordersLink, roundsLink);
        main.append(breadcrumb, header, section, handoff, actions);
    }

    async function loadOrder() {
        if (!orderId || !/^[1-9]\d*$/.test(orderId)) {
            showMessage("ไม่พบรหัสออเดอร์ กรุณาเลือกรายการจากหน้ารายการของฉัน", true);
            return;
        }
        let token;
        try {
            token = localStorage.getItem("token");
        } catch (error) {
            showMessage("ไม่สามารถอ่านสถานะเข้าสู่ระบบได้ กรุณาเข้าสู่ระบบใหม่", true);
            return;
        }
        if (!token) {
            window.location.replace("/login.html?returnTo="
                + encodeURIComponent(window.location.pathname + window.location.search));
            return;
        }
        try {
            const response = await fetch("/api/v1/orders/" + encodeURIComponent(orderId), {
                headers: {
                    "Authorization": (localStorage.getItem("tokenType") || "Bearer") + " " + token
                }
            });
            let result;
            try {
                result = await response.json();
            } catch (error) {
                throw new Error("เซิร์ฟเวอร์ตอบกลับในรูปแบบที่ไม่ถูกต้อง กรุณาลองอีกครั้ง");
            }
            if (response.status === 401) {
                localStorage.removeItem("token");
                localStorage.removeItem("tokenType");
                window.location.replace("/login.html?returnTo="
                    + encodeURIComponent(window.location.pathname + window.location.search));
                return;
            }
            if (!response.ok || !result || result.status !== "SUCCESS" || !result.data) {
                showMessage(result && typeof result.message === "string"
                    ? result.message
                    : "โหลดรายละเอียดออเดอร์ไม่สำเร็จ (HTTP " + response.status + ")", true);
                return;
            }
            displayOrder(result.data);
        } catch (error) {
            showMessage(error.message || "ไม่สามารถเชื่อมต่อเซิร์ฟเวอร์ได้ กรุณาลองอีกครั้ง", true);
        }
    }

    showMessage("กำลังโหลดรายละเอียดออเดอร์...", false);
    loadOrder();
})();
