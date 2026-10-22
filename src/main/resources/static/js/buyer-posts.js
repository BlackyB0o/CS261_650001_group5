(function () {
    const list = document.getElementById("buyer-posts-list");
    const count = document.getElementById("buyer-posts-count");
    const message = document.getElementById("buyer-posts-message");

    function formatDate(value) {
        const date = new Date(value + "T00:00:00");
        return Number.isNaN(date.getTime())
            ? value || "-"
            : new Intl.DateTimeFormat("th-TH", { dateStyle: "medium" }).format(date);
    }

    function showMessage(text, isError) {
        message.textContent = text;
        message.className = "form-message " + (isError ? "error-message" : "");
        message.hidden = false;
    }

    function renderPost(post) {
        const card = document.createElement("article");
        card.className = "buyer-post-card";
        const top = document.createElement("div");
        top.className = "buyer-post-card-top";
        const icon = document.createElement("div");
        icon.className = "buyer-post-category-icon";
        icon.textContent = "CH";
        const status = document.createElement("span");
        status.className = "buyer-post-status";
        status.textContent = post.status === "OPEN" ? "กำลังหาคนรับหิ้ว" : post.status;
        top.append(icon, status);

        const content = document.createElement("div");
        content.className = "buyer-post-card-content";
        const title = document.createElement("h3");
        title.textContent = post.postTitle || "โพสต์หาคนรับหิ้ว";
        const shop = document.createElement("p");
        shop.className = "buyer-post-shop";
        shop.textContent = post.shopName || "";
        const divider = document.createElement("div");
        divider.className = "buyer-post-divider";
        const info = document.createElement("div");
        info.className = "buyer-post-info";
        const buyer = document.createElement("div");
        const buyerLabel = document.createElement("span");
        buyerLabel.className = "buyer-post-info-label";
        buyerLabel.textContent = "ผู้ฝากซื้อ";
        const buyerName = document.createElement("span");
        buyerName.className = "buyer-post-info-value";
        buyerName.textContent = post.buyerName || "-";
        buyer.append(buyerLabel, buyerName);
        const date = document.createElement("div");
        const dateLabel = document.createElement("span");
        dateLabel.className = "buyer-post-info-label";
        dateLabel.textContent = "วันที่ต้องการ";
        const dateValue = document.createElement("span");
        dateValue.className = "buyer-post-info-value";
        dateValue.textContent = formatDate(post.desiredDate);
        date.append(dateLabel, dateValue);
        info.append(buyer, date);
        const description = document.createElement("p");
        description.className = "buyer-post-description";
        description.textContent = post.productDetail || "";
        content.append(title, shop, divider, info, description);

        const footer = document.createElement("div");
        footer.className = "buyer-post-card-footer";
        const link = document.createElement("a");
        link.className = "buyer-post-detail-btn";
        link.href = "buyer-post-detail.html?postId=" + encodeURIComponent(post.postId);
        link.textContent = "ดูรายละเอียด →";
        footer.appendChild(link);
        card.append(top, content, footer);
        return card;
    }

    fetch("/api/v1/posts")
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
                    : "โหลดโพสต์ไม่สำเร็จ (HTTP " + response.status + ")");
            }
            list.replaceChildren();
            count.textContent = result.data.length + " โพสต์";
            if (result.data.length === 0) {
                showMessage("ยังไม่มีโพสต์ที่เปิดรับคนช่วยซื้อ", false);
                return;
            }
            message.hidden = true;
            result.data.forEach(function (post) {
                list.appendChild(renderPost(post));
            });
        })
        .catch(function (error) {
            list.replaceChildren();
            count.textContent = "โหลดไม่สำเร็จ";
            showMessage(error.message || "ไม่สามารถเชื่อมต่อเซิร์ฟเวอร์ได้ กรุณาลองอีกครั้ง", true);
        });
})();
