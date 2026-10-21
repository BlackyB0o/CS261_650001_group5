(function () {
    const roundsList = document.getElementById("my-rounds-list");
    const message = document.getElementById("my-rounds-message");

    function showMessage(text, isError) {
        message.textContent = text;
        message.className = "form-message " + (isError ? "error-message" : "");
        message.hidden = false;
    }

    function formatDateTime(value) {
        const date = new Date(value);
        return Number.isNaN(date.getTime())
            ? value
            : new Intl.DateTimeFormat("th-TH", { dateStyle: "medium", timeStyle: "short" }).format(date);
    }

    function statusLabel(status) {
        return {
            OPEN: "เปิดรับฝากซื้อ",
            CLOSED: "ปิดรับออเดอร์",
            COMPLETED: "เสร็จสิ้น",
            CANCELLED: "ยกเลิก"
        }[status] || status;
    }

    function renderRound(round) {
        const card = document.createElement("article");
        card.className = "order-card";

        const header = document.createElement("div");
        header.className = "order-card-header";
        const idContainer = document.createElement("div");
        const label = document.createElement("span");
        label.className = "order-label";
        label.textContent = "หมายเลขรอบ";
        const id = document.createElement("h3");
        id.textContent = "RD" + round.roundId;
        idContainer.append(label, id);

        const status = document.createElement("span");
        status.className = "round-status status-" + String(round.status || "").toLowerCase();
        status.textContent = statusLabel(round.status);
        header.append(idContainer, status);

        const body = document.createElement("div");
        body.className = "order-card-body";
        const icon = document.createElement("div");
        icon.className = "order-shop-icon";
        icon.textContent = "CH";
        const product = document.createElement("div");
        product.className = "order-product-info";
        const storeName = document.createElement("h4");
        storeName.textContent = round.storeName || "รอบรับหิ้ว";
        const location = document.createElement("p");
        location.className = "order-carrier";
        location.textContent = round.locationName || "";
        product.append(storeName, location);
        body.append(icon, product);

        const footer = document.createElement("div");
        footer.className = "my-round-bottom";
        const details = document.createElement("div");
        details.className = "round-request-summary";
        const date = document.createElement("p");
        date.textContent = "วันและเวลารับสินค้า: " + formatDateTime(round.scheduledAt);
        const fee = document.createElement("p");
        fee.textContent = "ค่าบริการ " + Number(round.serviceFee).toLocaleString("th-TH") + " บาท / รายการ";
        details.append(date, fee);
        const actions = document.createElement("div");
        actions.className = "my-round-actions";
        const detailLink = document.createElement("a");
        detailLink.className = "btn btn-outline";
        detailLink.href = "my-round-detailod.html?roundId=" + encodeURIComponent(round.roundId);
        detailLink.textContent = "จัดการรอบ";
        actions.appendChild(detailLink);
        footer.append(details, actions);
        card.append(header, body, footer);
        return card;
    }

    async function loadRounds() {
        roundsList.replaceChildren();
        showMessage("กำลังโหลดรอบรับหิ้ว...", false);

        let token;
        try {
            token = localStorage.getItem("token");
        } catch (error) {
            showMessage("ไม่สามารถอ่านสถานะเข้าสู่ระบบได้ กรุณาเปิดใช้พื้นที่จัดเก็บข้อมูลแล้วลองใหม่", true);
            return;
        }
        if (!token) {
            window.location.replace("/login.html?returnTo=%2Forders.html");
            return;
        }

        try {
            const tokenType = localStorage.getItem("tokenType") || "Bearer";
            const response = await fetch("/api/v1/delivery-rounds/my", {
                headers: { "Authorization": tokenType + " " + token }
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
                window.location.replace("/login.html?returnTo=%2Forders.html");
                return;
            }
            if (!response.ok || !result || result.status !== "SUCCESS" || !Array.isArray(result.data)) {
                showMessage(result && typeof result.message === "string"
                    ? result.message
                    : "โหลดรอบรับหิ้วของคุณไม่สำเร็จ (HTTP " + response.status + ")", true);
                return;
            }

            message.hidden = true;
            if (result.data.length === 0) {
                showMessage("คุณยังไม่มีรอบรับหิ้วที่สร้างไว้", false);
                return;
            }
            result.data.forEach(function (round) {
                roundsList.appendChild(renderRound(round));
            });
        } catch (error) {
            showMessage("ไม่สามารถเชื่อมต่อเซิร์ฟเวอร์ได้ กรุณาลองอีกครั้ง", true);
        }
    }

    loadRounds();
})();
