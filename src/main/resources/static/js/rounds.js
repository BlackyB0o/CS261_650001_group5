(function () {
    const roundsList = document.getElementById("rounds-list");
    const roundsCount = document.getElementById("rounds-count");
    const roundsMessage = document.getElementById("rounds-message");
    const searchForm = document.getElementById("round-search-form");
    const keywordInput = document.getElementById("keyword");
    const dateInput = document.getElementById("date");
    let rounds = [];

    function formatDateTime(value) {
        const date = new Date(value);
        if (Number.isNaN(date.getTime())) {
            return value;
        }
        return new Intl.DateTimeFormat("th-TH", {
            dateStyle: "medium",
            timeStyle: "short"
        }).format(date);
    }

    function showMessage(message, isError) {
        roundsMessage.textContent = message;
        roundsMessage.className = "form-message " + (isError ? "error-message" : "");
        roundsMessage.hidden = false;
    }

    function createInfoRow(label, value, className) {
        const row = document.createElement("div");
        row.className = "round-info-row";
        const caption = document.createElement("span");
        caption.textContent = label;
        const content = document.createElement("strong");
        content.textContent = value;
        if (className) {
            content.className = className;
        }
        row.append(caption, content);
        return row;
    }

    function renderRounds() {
        const keyword = keywordInput.value.trim().toLocaleLowerCase();
        const selectedDate = dateInput.value;
        const filteredRounds = rounds.filter(function (round) {
            const searchable = [round.storeName, round.locationName, round.runnerName]
                .filter(Boolean)
                .join(" ")
                .toLocaleLowerCase();
            return (!keyword || searchable.includes(keyword))
                && (!selectedDate || (round.scheduledAt || "").startsWith(selectedDate));
        });

        roundsList.replaceChildren();
        roundsCount.textContent = filteredRounds.length + " รอบ";
        roundsMessage.hidden = true;

        if (filteredRounds.length === 0) {
            showMessage("ไม่พบรอบรับหิ้วที่ตรงกับการค้นหา", false);
            return;
        }

        filteredRounds.forEach(function (round, index) {
            const card = document.createElement("article");
            card.className = "round-card";

            const top = document.createElement("div");
            top.className = "round-card-top";
            const icon = document.createElement("div");
            icon.className = "round-category-icon";
            icon.textContent = "CH";
            const status = document.createElement("span");
            status.className = "round-status";
            status.textContent = round.status === "OPEN" ? "เปิดรับออเดอร์" : round.status;
            top.append(icon, status);

            const content = document.createElement("div");
            content.className = "round-card-content";
            const heading = document.createElement("h3");
            heading.textContent = round.storeName || "รอบรับหิ้ว";
            const location = document.createElement("p");
            location.className = "round-shop-name";
            location.textContent = round.locationName || "";
            const divider = document.createElement("div");
            divider.className = "round-divider";
            const info = document.createElement("div");
            info.className = "round-info-list";
            info.append(
                createInfoRow("ผู้รับหิ้ว", round.runnerName || "-"),
                createInfoRow("วันและเวลารับสินค้า", formatDateTime(round.scheduledAt || "")),
                createInfoRow("ค่าบริการ", Number(round.serviceFee).toLocaleString("th-TH") + " บาท / รายการ", "round-service-fee")
            );
            content.append(heading, location, divider, info);

            const footer = document.createElement("div");
            footer.className = "round-card-footer";
            const link = document.createElement("a");
            link.className = "round-detail-btn";
            link.href = "round-detail.html?roundId=" + encodeURIComponent(round.roundId);
            link.textContent = "ดูรายละเอียด →";
            footer.appendChild(link);

            card.append(top, content, footer);
            roundsList.appendChild(card);
        });
    }

    searchForm.addEventListener("submit", function (event) {
        event.preventDefault();
        renderRounds();
    });

    keywordInput.addEventListener("input", renderRounds);
    dateInput.addEventListener("change", renderRounds);

    fetch("/api/v1/delivery-rounds")
        .then(async function (response) {
            let result;
            try {
                result = await response.json();
            } catch (error) {
                throw new Error("เซิร์ฟเวอร์ตอบกลับในรูปแบบที่ไม่ถูกต้อง กรุณาลองอีกครั้ง");
            }
            if (!response.ok || !result || result.status !== "SUCCESS" || !Array.isArray(result.data)) {
                throw new Error(result && typeof result.message === "string"
                    ? result.message
                    : "โหลดรายการรอบรับหิ้วไม่สำเร็จ (HTTP " + response.status + ")");
            }
            rounds = result.data;
            renderRounds();
        })
        .catch(function (error) {
            roundsList.replaceChildren();
            roundsCount.textContent = "โหลดไม่สำเร็จ";
            showMessage(error.message || "ไม่สามารถเชื่อมต่อเซิร์ฟเวอร์ได้ กรุณาลองอีกครั้ง", true);
        });
})();
