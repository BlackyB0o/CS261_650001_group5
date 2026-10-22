(function () {
    const message = document.getElementById("round-detail-message");
    const detailContent = document.getElementById("round-detail-content");
    const createOrderLink = document.getElementById("create-order-link");
    const roundId = new URLSearchParams(window.location.search).get("roundId");
    createOrderLink.style.display = "none";

    function setText(id, value) {
        document.getElementById(id).textContent = value == null || value === "" ? "-" : value;
    }

    function formatDateTime(value) {
        const date = new Date(value);
        if (Number.isNaN(date.getTime())) {
            return value || "-";
        }
        return new Intl.DateTimeFormat("th-TH", {
            dateStyle: "full",
            timeStyle: "short"
        }).format(date);
    }

    function showError(text) {
        message.textContent = text;
        message.className = "form-message error-message";
        message.hidden = false;
        detailContent.hidden = true;
    }

    if (!roundId || !/^[1-9]\d*$/.test(roundId)) {
        showError("ไม่พบรหัสรอบรับหิ้ว กรุณาเลือกรอบจากหน้ารายการ");
        return;
    }

    fetch("/api/v1/delivery-rounds/" + encodeURIComponent(roundId))
        .then(async function (response) {
            let result;
            try {
                result = await response.json();
            } catch (error) {
                throw new Error("เซิร์ฟเวอร์ตอบกลับในรูปแบบที่ไม่ถูกต้อง กรุณาลองอีกครั้ง");
            }
            if (!response.ok || !result || result.status !== "SUCCESS" || !result.data) {
                throw new Error(result && typeof result.message === "string"
                    ? result.message
                    : "โหลดรายละเอียดรอบไม่สำเร็จ (HTTP " + response.status + ")");
            }

            const round = result.data;
            document.title = (round.storeName || "รายละเอียดรอบ") + " | CarryHub";
            setText("round-detail-status", round.status === "OPEN" ? "เปิดรับออเดอร์" : round.status);
            setText("round-detail-id", "รอบรับหิ้ว #" + round.roundId);
            setText("round-detail-store", round.storeName);
            setText("round-detail-runner", round.runnerName);
            setText("round-detail-scheduled", formatDateTime(round.scheduledAt));
            setText("round-detail-fee", Number(round.serviceFee).toLocaleString("th-TH") + " บาท / รายการ");
            setText("round-detail-location", round.locationName);
            setText("round-detail-current-status", round.status);
            if (round.status === "OPEN") {
                createOrderLink.href = "create-order.html?roundId=" + encodeURIComponent(round.roundId);
                createOrderLink.hidden = false;
                createOrderLink.style.display = "";
            }
            message.hidden = true;
            detailContent.hidden = false;
        })
        .catch(function (error) {
            showError(error.message || "ไม่สามารถเชื่อมต่อเซิร์ฟเวอร์ได้ กรุณาลองอีกครั้ง");
        });
})();
