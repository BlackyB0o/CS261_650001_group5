(function () {
    "use strict";

    const orderId = new URLSearchParams(window.location.search).get("orderId");
    const $ = (id) => document.getElementById(id);
    let order = null;
    let payment = null;
    let isCarrier = false;

    const orderStatusText = {
        PENDING: "รอชำระเงิน",
        PAID: "ชำระเงินแล้ว",
        ACCEPTED: "ผู้รับหิ้วรับงานแล้ว",
        IN_PROGRESS: "กำลังดำเนินการ",
        PURCHASING: "กำลังซื้อสินค้า",
        READY_FOR_DELIVERY: "รอส่งมอบสินค้า",
        DELIVERING: "กำลังส่งมอบสินค้า",
        COMPLETED: "สำเร็จ",
        CANCELLED: "ยกเลิกแล้ว"
    };

    const paymentStatusText = {
        PENDING: "รอชำระเงิน",
        HELD: "ชำระแล้ว (เงินพักในระบบ)",
        PAID: "ชำระแล้ว",
        FAILED: "ชำระเงินไม่สำเร็จ",
        REFUNDED: "คืนเงินแล้ว"
    };

    function money(value) {
        const n = Number(value);
        return Number.isFinite(n)
            ? n.toLocaleString("th-TH", { minimumFractionDigits: 2, maximumFractionDigits: 2 }) + " บาท"
            : "-";
    }

    function dateText(value) {
        if (!value) return "-";
        const date = new Date(value);
        return Number.isNaN(date.getTime())
            ? String(value)
            : new Intl.DateTimeFormat("th-TH", { dateStyle: "medium", timeStyle: "short" }).format(date);
    }

    function authHeaders(json) {
        const token = localStorage.getItem("token");
        if (!token) return null;
        const headers = {
            "Authorization": (localStorage.getItem("tokenType") || "Bearer") + " " + token
        };
        if (json) headers["Content-Type"] = "application/json";
        return headers;
    }

    function redirectLogin() {
        window.location.replace("/login.html?returnTo=" +
            encodeURIComponent(window.location.pathname + window.location.search));
    }

    async function parseResponse(response) {
        let data;
        try {
            data = await response.json();
        } catch (_) {
            throw new Error("เซิร์ฟเวอร์ตอบกลับข้อมูลไม่ถูกต้อง");
        }
        return data;
    }

    function showPageMessage(message, isError) {
        const box = $("page-message");
        box.hidden = false;
        box.textContent = message;
        box.className = "page-message" + (isError ? " error-message" : "");
    }

    function showPaymentMessage(message, isError) {
        const box = $("payment-message");
        box.hidden = false;
        box.textContent = message;
        box.className = "form-message " + (isError ? "error-message" : "success-message");
    }

    function make(tag, className, text) {
        const node = document.createElement(tag);
        if (className) node.className = className;
        if (text !== undefined && text !== null) node.textContent = text;
        return node;
    }

    function statusLabel(status) {
        return orderStatusText[status] || status || "ไม่ระบุสถานะ";
    }

    function renderOrder(data) {
        order = data;
        $("order-number").textContent = data.orderId ?? orderId;
        $("order-date").textContent = dateText(data.createdAt);
        $("order-round").textContent = data.roundId ? "รอบ #" + data.roundId : "-";
        $("order-location").textContent = data.handoffLocation || "-";

        const status = $("order-status");
        status.textContent = statusLabel(data.status);
        status.className = "order-status status-" + String(data.status || "pending").toLowerCase();

        $("buyer-name").textContent = data.buyerName || "-";
        $("buyer-avatar").textContent = (data.buyerName || "B").trim().charAt(0).toUpperCase();
        $("carrier-name").textContent = data.carrierName || data.runnerName || "ยังไม่มีผู้รับหิ้ว";
        $("carrier-avatar").textContent = (data.carrierName || data.runnerName || "C").trim().charAt(0).toUpperCase();

        const itemsBox = $("order-items");
        itemsBox.replaceChildren();
        const items = Array.isArray(data.items) ? data.items : [];
        $("item-count").textContent = items.length + " รายการ";

        if (items.length === 0) {
            itemsBox.appendChild(make("p", "muted", "ไม่พบรายการสินค้า"));
        } else {
            items.forEach(function (item) {
                const row = make("article", "order-product");
                const icon = make("div", "product-placeholder", "CH");
                const info = make("div", "order-product-info");
                info.append(
                    make("h3", "", item.productName || "สินค้า"),
                    make("p", "", "จำนวน " + (item.quantity ?? "-") + (item.unit || " ชิ้น"))
                );
                if (item.note) info.appendChild(make("small", "item-note", "หมายเหตุ: " + item.note));
                row.append(icon, info);
                itemsBox.appendChild(row);
            });
        }

        const summary = $("order-price-summary");
        summary.replaceChildren();
        const productTotal = items.reduce((sum, item) => {
            const price = Number(item.subtotal ?? item.totalPrice ?? 0);
            return sum + (Number.isFinite(price) ? price : 0);
        }, 0);
        summary.append(
            priceRow("วิธีรับสินค้า", data.handoffMethod === "MEETUP" ? "นัดรับ" :
                data.handoffMethod === "DELIVERY" ? "จัดส่ง" : (data.handoffMethod || "-")),
            priceRow("สถานที่รับสินค้า", data.handoffLocation || "-")
        );

        $("round-link").href = "/round-detail.html?roundId=" + encodeURIComponent(data.roundId || "");
        $("report-button").href = "/report.html?orderId=" + encodeURIComponent(data.orderId || orderId);
        renderTimeline(data.status);
    }

    function priceRow(label, value) {
        const row = make("div", "price-row");
        row.append(make("span", "", label), make("strong", "", value));
        return row;
    }

    function renderTimeline(status) {
        const steps = [
            { title: "สร้างรายการสั่งซื้อ", detail: "ระบบได้รับรายการสั่งซื้อแล้ว", states: ["PENDING", "PAID", "ACCEPTED", "IN_PROGRESS", "PURCHASING", "READY_FOR_DELIVERY", "DELIVERING", "COMPLETED"] },
            { title: "ชำระเงิน", detail: "ตรวจสอบการชำระเงินผ่านระบบตัวกลาง", states: ["PAID", "ACCEPTED", "IN_PROGRESS", "PURCHASING", "READY_FOR_DELIVERY", "DELIVERING", "COMPLETED"] },
            { title: "ผู้รับหิ้วดำเนินการ", detail: "ผู้รับหิ้วกำลังซื้อสินค้าตามรายการ", states: ["ACCEPTED", "IN_PROGRESS", "PURCHASING", "READY_FOR_DELIVERY", "DELIVERING", "COMPLETED"] },
            { title: "ส่งมอบสินค้า", detail: "นัดรับหรือจัดส่งสินค้าตามที่ตกลง", states: ["READY_FOR_DELIVERY", "DELIVERING", "COMPLETED"] },
            { title: "เสร็จสิ้น", detail: "ผู้ฝากซื้อได้รับสินค้าเรียบร้อยแล้ว", states: ["COMPLETED"] }
        ];
        const box = $("order-timeline");
        box.replaceChildren();
        if (status === "CANCELLED") {
            box.appendChild(make("p", "timeline-cancelled", "ออเดอร์นี้ถูกยกเลิกแล้ว หากมีข้อสงสัยสามารถรายงานปัญหาให้ทีมงานตรวจสอบได้"));
            return;
        }
        const currentIndex = Math.max(0, steps.findIndex(step => !step.states.includes(status)));
        steps.forEach(function (step, index) {
            const state = step.states.includes(status) ? "completed" : (index === currentIndex ? "current" : "");
            const row = make("div", "timeline-item " + state);
            row.append(
                make("div", "timeline-marker", String(index + 1)),
                make("div", "timeline-content")
            );
            row.querySelector(".timeline-content").append(
                make("h3", "", step.title),
                make("p", "", step.detail)
            );
            if (state === "completed") row.appendChild(make("span", "timeline-tag", "เสร็จแล้ว"));
            if (state === "current") row.appendChild(make("span", "timeline-tag current-tag", "ขั้นตอนปัจจุบัน"));
            box.appendChild(row);
        });
    }

    async function loadPayment() {
        const response = await fetch("/api/v1/orders/" + encodeURIComponent(orderId) + "/payment", {
            headers: authHeaders(false)
        });
        const result = await parseResponse(response);

        if (response.status === 401) {
            localStorage.removeItem("token");
            localStorage.removeItem("tokenType");
            redirectLogin();
            return;
        }
        if (response.status === 404 && result && result.message === "Payment not found") {
            payment = null;
            renderPayment();
            return;
        }
        if (!response.ok || !result || result.status !== "SUCCESS" || !result.data) {
            throw new Error((result && result.message) || "โหลดข้อมูลการชำระเงินไม่สำเร็จ (HTTP " + response.status + ")");
        }
        payment = result.data;
        renderPayment();
    }

    function renderPayment() {
        const content = $("payment-content");
        content.replaceChildren();

        const pStatus = payment ? payment.paymentStatus : "PENDING";
        $("payment-status-badge").textContent = paymentStatusText[pStatus] || pStatus || "รอกำหนดราคา";
        $("total-payment-status").textContent = paymentStatusText[pStatus] || "รอผู้รับหิ้วกำหนดราคา";
        $("total-product").textContent = payment ? money(payment.productAmount) : "-";
        $("total-service").textContent = payment ? money(payment.serviceFee) : "-";
        $("total-amount").innerHTML = payment
            ? Number(payment.totalAmount || 0).toLocaleString("th-TH", { minimumFractionDigits: 2 }) + ' <span>บาท</span>'
            : 'รอกำหนดราคา <span></span>';

        if (!payment) {
            if (isCarrier && order.status === "PENDING") {
                content.appendChild(make("p", "payment-instruction", "กรุณากำหนดราคาสินค้ารวม เพื่อให้ผู้ฝากซื้อดำเนินการชำระเงิน"));
                content.appendChild(priceForm());
            } else {
                content.appendChild(make("p", "muted", "ยังไม่มีข้อมูลราคา รอผู้รับหิ้วกำหนดราคาสินค้า"));
            }
            return;
        }

        const summary = make("div", "payment-summary-list");
        summary.append(
            priceRow("ราคาสินค้า", money(payment.productAmount)),
            priceRow("ค่าบริการรับหิ้ว", money(payment.serviceFee)),
            priceRow("ยอดรวมที่ต้องชำระ", money(payment.totalAmount)),
            priceRow("สถานะการชำระเงิน", paymentStatusText[pStatus] || pStatus || "-")
        );
        content.appendChild(summary);

        if (payment.qrPayload) {
            const qrBox = make("div", "qr-payload-box");
            qrBox.append(make("h3", "", "ข้อมูลสำหรับชำระเงิน"), make("code", "", payment.qrPayload));
            qrBox.appendChild(make("p", "muted", "ชำระเข้าระบบตัวกลางตามยอดรวม ไม่โอนให้ผู้รับหิ้วโดยตรง"));
            content.appendChild(qrBox);
        }

        if (payment.slipImageUrl) {
            const slipLink = make("a", "slip-link", "เปิดดูสลิปที่แนบ");
            slipLink.href = payment.slipImageUrl;
            slipLink.target = "_blank";
            slipLink.rel = "noopener noreferrer";
            content.appendChild(slipLink);
        }

        if (payment.paymentStatus === "HELD") {
            content.appendChild(make("p", "payment-success", "ยืนยันการชำระเงินแล้ว เงินอยู่ระหว่างพักไว้ในระบบตัวกลาง"));
            if (payment.transactionReference) content.appendChild(make("p", "muted", "เลขอ้างอิง: " + payment.transactionReference));
            if (payment.paidAt) content.appendChild(make("p", "muted", "ชำระเมื่อ: " + dateText(payment.paidAt)));
        }

        if (isCarrier && order.status === "PENDING" && payment.paymentStatus === "PENDING") {
            content.appendChild(priceForm(payment.productAmount));
        } else if (!isCarrier && order.status === "PENDING" && payment.paymentStatus === "PENDING") {
            content.appendChild(slipForm());
        }
    }

    function priceForm(existingAmount) {
        const form = make("form", "payment-form");
        form.innerHTML = '<label for="product-amount">ราคารวมสินค้า (บาท)</label>' +
            '<input id="product-amount" name="productAmount" type="number" min="0.01" step="0.01" required>' +
            '<button class="btn btn-primary" type="submit">บันทึกราคาและแจ้งยอดชำระ</button>';
        if (existingAmount != null) form.querySelector("input").value = existingAmount;
        form.addEventListener("submit", submitPrice);
        return form;
    }

    function slipForm() {
        const form = make("form", "payment-form");
        form.innerHTML = '<label for="paid-amount">ยอดที่ชำระ (บาท)</label>' +
            '<input id="paid-amount" name="paidAmount" type="number" min="0.01" step="0.01" required>' +
            '<label for="slip-image">แนบสลิป (JPEG หรือ PNG ไม่เกิน 5 MB)</label>' +
            '<input id="slip-image" name="slipImage" type="file" accept="image/jpeg,image/png" required>' +
            '<button class="btn btn-primary" type="submit">ยืนยันชำระเงินและแนบสลิป</button>';
        form.querySelector("#paid-amount").value = payment.totalAmount ?? "";
        form.addEventListener("submit", submitSlip);
        return form;
    }

    async function submitPrice(event) {
        event.preventDefault();
        const form = event.currentTarget;
        const button = form.querySelector("button");
        const amount = Number(form.querySelector("input").value);
        if (!Number.isFinite(amount) || amount <= 0) {
            showPaymentMessage("กรุณากรอกราคามากกว่า 0 บาท", true);
            return;
        }
        button.disabled = true;
        try {
            const response = await fetch("/api/v1/orders/" + encodeURIComponent(orderId) + "/price", {
                method: "PUT",
                headers: authHeaders(true),
                body: JSON.stringify({ productAmount: amount })
            });
            const result = await parseResponse(response);
            if (response.status === 401) {
                localStorage.removeItem("token");
                localStorage.removeItem("tokenType");
                redirectLogin();
                return;
            }
            if (!response.ok || !result || result.status !== "SUCCESS" || !result.data) {
                throw new Error((result && result.message) || "บันทึกราคาไม่สำเร็จ");
            }
            payment = result.data;
            renderPayment();
            showPaymentMessage("บันทึกราคาสินค้าแล้ว ผู้ฝากซื้อสามารถชำระเงินได้", false);
        } catch (error) {
            showPaymentMessage(error.message || "เกิดข้อผิดพลาดในการบันทึกราคา", true);
        } finally {
            button.disabled = false;
        }
    }

    async function submitSlip(event) {
        event.preventDefault();
        const form = event.currentTarget;
        const file = form.querySelector("#slip-image").files[0];
        const amount = form.querySelector("#paid-amount").value;
        if (!file) return showPaymentMessage("กรุณาเลือกไฟล์สลิป", true);
        if (!["image/jpeg", "image/png"].includes(file.type)) return showPaymentMessage("รองรับเฉพาะไฟล์ JPEG และ PNG", true);
        if (file.size > 5 * 1024 * 1024) return showPaymentMessage("ไฟล์ต้องมีขนาดไม่เกิน 5 MB", true);

        const body = new FormData();
        body.append("slipImage", file);
        body.append("paidAmount", amount);
        const button = form.querySelector("button");
        button.disabled = true;
        try {
            const response = await fetch("/api/v1/orders/" + encodeURIComponent(orderId) + "/payment/slip", {
                method: "POST",
                headers: authHeaders(false),
                body
            });
            const result = await parseResponse(response);
            if (response.status === 401) {
                localStorage.removeItem("token");
                localStorage.removeItem("tokenType");
                redirectLogin();
                return;
            }
            if (!response.ok || !result || result.status !== "SUCCESS" || !result.data) {
                throw new Error((result && result.message) || "ส่งหลักฐานการชำระเงินไม่สำเร็จ");
            }
            payment = result.data;
            renderPayment();
            showPaymentMessage("ส่งหลักฐานการชำระเงินเรียบร้อยแล้ว", false);
        } catch (error) {
            showPaymentMessage(error.message || "เกิดข้อผิดพลาดในการส่งสลิป", true);
        } finally {
            button.disabled = false;
        }
    }

    async function loadOrder() {
        if (!orderId || !/^[1-9]\d*$/.test(orderId)) {
            showPageMessage("ไม่พบรหัสออเดอร์ กรุณาเปิดจากหน้ารายการของฉัน", true);
            return;
        }
        const headers = authHeaders(false);
        if (!headers) {
            redirectLogin();
            return;
        }

        try {
            const response = await fetch("/api/v1/orders/" + encodeURIComponent(orderId), { headers });
            const result = await parseResponse(response);
            if (response.status === 401) {
                localStorage.removeItem("token");
                localStorage.removeItem("tokenType");
                redirectLogin();
                return;
            }
            if (!response.ok || !result || result.status !== "SUCCESS" || !result.data) {
                throw new Error((result && result.message) || "โหลดรายละเอียดออเดอร์ไม่สำเร็จ (HTTP " + response.status + ")");
            }
            renderOrder(result.data);
            const token = localStorage.getItem("token");
            try {
                const payload = token.split(".")[1].replace(/-/g, "+").replace(/_/g, "/");
                const claims = JSON.parse(atob(payload + "=".repeat((4 - payload.length % 4) % 4)));
                isCarrier = Number(claims.sub) !== Number(order.buyerId);
            } catch (_) {
                isCarrier = false;
            }
            await loadPayment();
        } catch (error) {
            showPageMessage(error.message || "ไม่สามารถเชื่อมต่อเซิร์ฟเวอร์ได้", true);
        }
    }

    loadOrder();
})();
