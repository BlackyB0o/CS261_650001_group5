(function () {
    const main = document.querySelector(".order-detail-page");
    const orderId = new URLSearchParams(window.location.search).get("orderId");
    let order;
    let payment;

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

    function money(value) {
        const amount = Number(value);
        return Number.isFinite(amount) ? amount.toLocaleString("th-TH", { minimumFractionDigits: 2 }) + " บาท" : "-";
    }

    function addMeta(grid, label, value) {
        const item = element("div");
        item.append(element("span", "", label), element("strong", "", value == null || value === "" ? "-" : value));
        grid.appendChild(item);
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

    function authHeaders(json) {
        const token = localStorage.getItem("token");
        if (!token) {
            return null;
        }
        const headers = {
            "Authorization": (localStorage.getItem("tokenType") || "Bearer") + " " + token
        };
        if (json) {
            headers["Content-Type"] = "application/json";
        }
        return headers;
    }

    function sendToLogin() {
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

    function displayPaymentSection(isRunner) {
        const section = element("section", "order-section-card");
        section.id = "payment-section";
        section.appendChild(element("h2", "", isRunner ? "กำหนดราคาสินค้า" : "ชำระเงินผ่านระบบตัวกลาง"));
        const message = element("p", "form-message");
        message.id = "payment-message";
        message.hidden = true;
        section.appendChild(message);

        if (isRunner) {
            const form = element("form");
            form.id = "set-price-form";
            const label = element("label", "", "ราคารวมสินค้า (บาท)");
            label.htmlFor = "product-amount";
            const amount = element("input");
            amount.id = "product-amount";
            amount.type = "number";
            amount.min = "0.01";
            amount.step = "0.01";
            amount.required = true;
            if (payment && payment.productAmount != null) {
                amount.value = payment.productAmount;
            }
            const submit = element("button", "btn btn-primary", "บันทึกราคาสินค้า");
            submit.type = "submit";
            form.append(label, amount, submit);
            section.appendChild(form);
            form.addEventListener("submit", setPrice);
        }

        const summary = element("div");
        summary.id = "payment-summary";
        section.appendChild(summary);

        if (!isRunner) {
            const form = element("form");
            form.id = "payment-slip-form";
            const amountLabel = element("label", "", "ยอดที่ชำระ (บาท)");
            amountLabel.htmlFor = "paid-amount";
            const amount = element("input");
            amount.id = "paid-amount";
            amount.name = "paidAmount";
            amount.type = "number";
            amount.min = "0.01";
            amount.step = "0.01";
            amount.required = true;
            const fileLabel = element("label", "", "แนบสลิป (JPEG หรือ PNG, ไม่เกิน 5 MB)");
            fileLabel.htmlFor = "slip-image";
            const file = element("input");
            file.id = "slip-image";
            file.name = "slipImage";
            file.type = "file";
            file.accept = "image/jpeg,image/png";
            file.required = true;
            const submit = element("button", "btn btn-primary", "ยืนยันชำระเงิน");
            submit.type = "submit";
            form.append(amountLabel, amount, fileLabel, file, submit);
            section.appendChild(form);
            form.addEventListener("submit", submitSlip);
        }

        main.insertBefore(section, main.querySelector(".order-bottom-actions"));
        renderPayment();
    }

    function setPaymentMessage(text, isError) {
        const message = document.getElementById("payment-message");
        message.textContent = text;
        message.className = "form-message " + (isError ? "error-message" : "success-message");
        message.hidden = false;
    }

    function renderPayment() {
        const summary = document.getElementById("payment-summary");
        if (!summary) {
            return;
        }
        summary.replaceChildren();
        const priceForm = document.getElementById("set-price-form");
        const slipForm = document.getElementById("payment-slip-form");
        if (!payment) {
            if (priceForm) {
                priceForm.hidden = order.status !== "PENDING";
            }
            if (slipForm) {
                slipForm.hidden = true;
                summary.appendChild(element("p", "", "รอผู้รับหิ้วกำหนดราคาสินค้า"));
            }
            return;
        }

        const table = element("div", "order-price-summary");
        [
            ["ราคาสินค้า", money(payment.productAmount)],
            ["ค่าบริการรับหิ้ว", money(payment.serviceFee)],
            ["ยอดรวมที่ต้องชำระ", money(payment.totalAmount)],
            ["สถานะการชำระเงิน", payment.paymentStatus || "-"]
        ].forEach(function (row) {
            const line = element("div", "price-row");
            line.append(element("span", "", row[0]), element("strong", "", row[1]));
            table.appendChild(line);
        });
        summary.appendChild(table);
        if (payment.qrPayload) {
            summary.appendChild(element("h3", "", "ข้อมูล QR สำหรับชำระเงิน"));
            const payload = element("code", "payment-qr-payload", payment.qrPayload);
            payload.id = "payment-qr-payload";
            summary.appendChild(payload);
            summary.appendChild(element("p", "", "ชำระเข้าระบบตัวกลางตามยอดรวมข้างต้น ไม่โอนให้ผู้รับหิ้วโดยตรง"));
        }
        if (payment.paymentStatus === "HELD") {
            summary.appendChild(element("p", "payment-status", "ยืนยันการชำระแล้ว เงินพักไว้ในระบบตัวกลาง"));
            if (payment.transactionReference) {
                summary.appendChild(element("p", "", "เลขอ้างอิง: " + payment.transactionReference));
            }
            if (payment.paidAt) {
                summary.appendChild(element("p", "", "ชำระเมื่อ: " + formatDate(payment.paidAt)));
            }
            if (payment.slipImageUrl) {
                const slip = element("a", "", "ดูสลิปที่แนบ");
                slip.href = payment.slipImageUrl;
                slip.target = "_blank";
                slip.rel = "noopener noreferrer";
                summary.appendChild(slip);
            }
        }
        if (priceForm) {
            const input = document.getElementById("product-amount");
            input.value = payment.productAmount == null ? "" : payment.productAmount;
            priceForm.hidden = order.status !== "PENDING" || payment.paymentStatus !== "PENDING";
        }
        if (slipForm) {
            const paidAmount = document.getElementById("paid-amount");
            paidAmount.value = payment.totalAmount == null ? "" : payment.totalAmount;
            slipForm.hidden = order.status !== "PENDING" || payment.paymentStatus !== "PENDING";
        }
    }

    async function loadPayment(isRunner) {
        try {
            const headers = authHeaders(false);
            if (!headers) {
                sendToLogin();
                return;
            }
            const response = await fetch("/api/v1/orders/" + encodeURIComponent(orderId) + "/payment", {
                headers: headers
            });
            const result = await readResponse(response);
            if (response.status === 401) {
                localStorage.removeItem("token");
                localStorage.removeItem("tokenType");
                sendToLogin();
                return;
            }
            if (response.status === 404 && result && result.message === "Payment not found") {
                displayPaymentSection(isRunner);
                return;
            }
            if (!response.ok || !result || result.status !== "SUCCESS" || !result.data) {
                displayPaymentSection(isRunner);
                setPaymentMessage(result && typeof result.message === "string"
                    ? result.message
                    : "โหลดข้อมูลการชำระเงินไม่สำเร็จ (HTTP " + response.status + ")", true);
                return;
            }
            payment = result.data;
            displayPaymentSection(isRunner);
        } catch (error) {
            displayPaymentSection(isRunner);
            setPaymentMessage(error.message || "ไม่สามารถเชื่อมต่อเซิร์ฟเวอร์ได้ กรุณาลองอีกครั้ง", true);
        }
    }

    async function setPrice(event) {
        event.preventDefault();
        const form = event.currentTarget;
        const amount = document.getElementById("product-amount");
        const submit = form.querySelector("button[type='submit']");
        let headers;
        try {
            headers = authHeaders(true);
        } catch (error) {
            setPaymentMessage("ไม่สามารถอ่านสถานะเข้าสู่ระบบได้ กรุณาเข้าสู่ระบบใหม่", true);
            return;
        }
        if (!headers) {
            sendToLogin();
            return;
        }
        submit.disabled = true;
        try {
            const response = await fetch("/api/v1/orders/" + encodeURIComponent(orderId) + "/price", {
                method: "PUT",
                headers: headers,
                body: JSON.stringify({ productAmount: Number(amount.value) })
            });
            const result = await readResponse(response);
            if (response.status === 401) {
                localStorage.removeItem("token");
                localStorage.removeItem("tokenType");
                sendToLogin();
                return;
            }
            if (!response.ok || !result || result.status !== "SUCCESS" || !result.data) {
                const fieldErrors = Array.isArray(result && result.errors)
                    ? result.errors.map(function (item) {
                        return item && item.field && item.message ? item.field + ": " + item.message : "";
                    }).filter(Boolean)
                    : [];
                setPaymentMessage([result && result.message || "บันทึกราคาไม่สำเร็จ (HTTP " + response.status + ")"]
                    .concat(fieldErrors).join("\n"), true);
                return;
            }
            payment = result.data;
            renderPayment();
            setPaymentMessage("บันทึกราคาสินค้าและสร้างข้อมูลชำระเงินแล้ว", false);
        } catch (error) {
            setPaymentMessage(error.message || "ไม่สามารถเชื่อมต่อเซิร์ฟเวอร์ได้ กรุณาลองอีกครั้ง", true);
        } finally {
            submit.disabled = false;
        }
    }

    async function submitSlip(event) {
        event.preventDefault();
        const form = event.currentTarget;
        const fileInput = document.getElementById("slip-image");
        const paidAmount = document.getElementById("paid-amount");
        const submit = form.querySelector("button[type='submit']");
        const file = fileInput.files[0];
        if (!file) {
            setPaymentMessage("กรุณาแนบสลิปการชำระเงิน", true);
            return;
        }
        if (!["image/jpeg", "image/png"].includes(file.type)) {
            setPaymentMessage("แนบได้เฉพาะไฟล์ JPEG หรือ PNG", true);
            return;
        }
        if (file.size > 5 * 1024 * 1024) {
            setPaymentMessage("ขนาดไฟล์ต้องไม่เกิน 5 MB", true);
            return;
        }
        let headers;
        try {
            headers = authHeaders(false);
        } catch (error) {
            setPaymentMessage("ไม่สามารถอ่านสถานะเข้าสู่ระบบได้ กรุณาเข้าสู่ระบบใหม่", true);
            return;
        }
        if (!headers) {
            sendToLogin();
            return;
        }
        const body = new FormData();
        body.append("slipImage", file);
        body.append("paidAmount", paidAmount.value);
        submit.disabled = true;
        try {
            const response = await fetch("/api/v1/orders/" + encodeURIComponent(orderId) + "/payment/slip", {
                method: "POST",
                headers: headers,
                body: body
            });
            const result = await readResponse(response);
            if (response.status === 401) {
                localStorage.removeItem("token");
                localStorage.removeItem("tokenType");
                sendToLogin();
                return;
            }
            if (!response.ok || !result || result.status !== "SUCCESS" || !result.data) {
                const fieldErrors = Array.isArray(result && result.errors)
                    ? result.errors.map(function (item) {
                        return item && item.field && item.message ? item.field + ": " + item.message : "";
                    }).filter(Boolean)
                    : [];
                setPaymentMessage([result && result.message || "ยืนยันการชำระเงินไม่สำเร็จ (HTTP " + response.status + ")"]
                    .concat(fieldErrors).join("\n"), true);
                return;
            }
            payment = result.data;
            order.status = payment.orderStatus || "PAID";
            const status = main.querySelector(".order-header-main .order-status");
            if (status) {
                status.textContent = order.status;
            }
            renderPayment();
            setPaymentMessage("ยืนยันการชำระเงินแล้ว เงินถูกพักไว้ในระบบตัวกลาง", false);
        } catch (error) {
            setPaymentMessage(error.message || "ไม่สามารถเชื่อมต่อเซิร์ฟเวอร์ได้ กรุณาลองอีกครั้ง", true);
        } finally {
            submit.disabled = false;
        }
    }

    function displayOrder(data) {
        order = data;
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
        if (order.status === "PAID" || order.status === "COMPLETED") {
            const completionLink = element("a", "order-proof-btn",
                order.status === "COMPLETED" ? "ดูหลักฐานการรับสินค้า" : "ยืนยันรับสินค้า");
            completionLink.href = "/delivery-proof.html?orderId=" + encodeURIComponent(order.orderId);
            actions.appendChild(completionLink);
        }
        main.append(breadcrumb, header, section, handoff, actions);

        const userId = currentUserId();
        loadPayment(userId !== null && userId !== Number(order.buyerId));
    }

    async function loadOrder() {
        if (!orderId || !/^[1-9]\d*$/.test(orderId)) {
            showMessage("ไม่พบรหัสออเดอร์ กรุณาเลือกรายการจากหน้ารายการของฉัน", true);
            return;
        }
        let headers;
        try {
            headers = authHeaders(false);
        } catch (error) {
            showMessage("ไม่สามารถอ่านสถานะเข้าสู่ระบบได้ กรุณาเข้าสู่ระบบใหม่", true);
            return;
        }
        if (!headers) {
            sendToLogin();
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
                sendToLogin();
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
