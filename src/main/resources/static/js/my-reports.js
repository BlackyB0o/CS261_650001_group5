(function () {
    const tableBody = document.getElementById("reportTableBody");
    const searchInput = document.getElementById("reportSearch");
    const message = document.getElementById("report-history-message");
    if (!tableBody || !searchInput || !message) {
        return;
    }

    const categoryLabels = {
        PRODUCT: "สินค้าไม่ตรงตามที่สั่ง",
        DELIVERY: "ปัญหาการส่งมอบสินค้า",
        PAYMENT: "ปัญหาการชำระเงิน",
        USER: "ปัญหาเกี่ยวกับผู้ใช้งาน",
        OTHER: "อื่น ๆ"
    };
    let reports = [];

    function showMessage(text, isError) {
        message.textContent = text;
        message.className = "form-message " + (isError ? "error-message" : "");
        message.hidden = false;
    }

    function formatDate(value) {
        const date = new Date(value);
        return Number.isNaN(date.getTime())
            ? value || "-"
            : new Intl.DateTimeFormat("th-TH", { dateStyle: "medium", timeStyle: "short" }).format(date);
    }

    function statusLabel(status) {
        return status === "PENDING" ? "กำลังตรวจสอบ"
            : status === "APPROVED" ? "อนุมัติแล้ว"
                : status === "REJECTED" ? "ปฏิเสธแล้ว"
                    : status || "-";
    }

    function renderReports() {
        const query = searchInput.value.trim().toLocaleLowerCase();
        const matchingReports = reports.filter(function (report) {
            return [
                "RP" + String(report.id).padStart(3, "0"),
                report.orderReference,
                categoryLabels[report.category] || report.category,
                report.description,
                statusLabel(report.status)
            ].some(function (value) {
                return String(value || "").toLocaleLowerCase().includes(query);
            });
        });

        tableBody.replaceChildren();
        if (matchingReports.length === 0) {
            const row = document.createElement("tr");
            const cell = document.createElement("td");
            cell.colSpan = 6;
            cell.className = "no-report";
            cell.textContent = reports.length === 0
                ? "ยังไม่มีประวัติการรายงานปัญหา"
                : "ไม่พบรายงานที่ตรงกับการค้นหา";
            row.appendChild(cell);
            tableBody.appendChild(row);
            return;
        }

        matchingReports.forEach(function (report) {
            const row = document.createElement("tr");
            const values = [
                "RP" + String(report.id).padStart(3, "0"),
                report.orderReference || "-",
                categoryLabels[report.category] || report.category || "-",
                formatDate(report.createdAt),
                statusLabel(report.status),
                report.description || "-"
            ];

            values.forEach(function (value, index) {
                const cell = document.createElement("td");
                if (index === 4) {
                    const badge = document.createElement("span");
                    badge.className = "report-status " + (report.status === "PENDING"
                        ? "pending"
                        : report.status === "APPROVED" ? "resolved" : "rejected");
                    badge.textContent = value;
                    cell.appendChild(badge);
                } else {
                    cell.textContent = value;
                }
                row.appendChild(cell);
            });
            tableBody.appendChild(row);
        });
    }

    async function loadReports() {
        let token;
        let tokenType;
        try {
            token = localStorage.getItem("token");
            tokenType = localStorage.getItem("tokenType") || "Bearer";
        } catch (error) {
            showMessage("ไม่สามารถอ่านสถานะเข้าสู่ระบบได้ กรุณาเข้าสู่ระบบใหม่", true);
            return;
        }
        if (!token) {
            window.location.replace("/login.html?returnTo=%2Forders.html%23report-history");
            return;
        }

        try {
            const response = await fetch("/api/v1/reports/my", {
                headers: {
                    "Authorization": tokenType + " " + token
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
                window.location.replace("/login.html?returnTo=%2Forders.html%23report-history");
                return;
            }
            if (!response.ok || !result || result.status !== "SUCCESS" || !Array.isArray(result.data)) {
                showMessage(result && typeof result.message === "string"
                    ? result.message
                    : "โหลดประวัติรายงานไม่สำเร็จ (HTTP " + response.status + ")", true);
                return;
            }
            reports = result.data;
            message.hidden = true;
            renderReports();
        } catch (error) {
            showMessage("ไม่สามารถเชื่อมต่อเซิร์ฟเวอร์ได้ กรุณาลองอีกครั้ง", true);
        }
    }

    searchInput.addEventListener("input", renderReports);
    loadReports();
})();
