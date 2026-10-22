(function () {
    const list = document.getElementById("my-posts-list");
    const message = document.getElementById("my-posts-message");
    const count = document.getElementById("posts-count");

    function showMessage(text, isError) {
        message.textContent = text;
        message.className = "form-message " + (isError ? "error-message" : "");
        message.hidden = false;
    }

    function renderPost(post) {
        const card = document.createElement("article");
        card.className = "order-card";
        const header = document.createElement("div");
        header.className = "order-card-header";
        const idContainer = document.createElement("div");
        const label = document.createElement("span");
        label.className = "order-label";
        label.textContent = "หมายเลขโพสต์";
        const id = document.createElement("h3");
        id.textContent = "BP" + post.postId;
        idContainer.append(label, id);
        const status = document.createElement("span");
        status.className = "order-status";
        status.textContent = post.status === "OPEN" ? "กำลังหาคนรับหิ้ว"
            : post.status === "ACCEPTED" ? "มีผู้รับงานแล้ว" : post.status;
        header.append(idContainer, status);

        const body = document.createElement("div");
        body.className = "order-card-body";
        const icon = document.createElement("div");
        icon.className = "order-shop-icon";
        icon.textContent = "CH";
        const details = document.createElement("div");
        details.className = "order-product-info";
        const title = document.createElement("h4");
        title.textContent = post.postTitle || "โพสต์หาคนรับหิ้ว";
        const shop = document.createElement("p");
        shop.textContent = post.shopName || "";
        const product = document.createElement("p");
        product.textContent = post.productDetail || "";
        const accepted = document.createElement("p");
        accepted.className = "order-carrier";
        accepted.textContent = post.acceptedByName
            ? "ผู้รับงาน: " + post.acceptedByName
            : "ยังไม่มีผู้รับงาน";
        details.append(title, shop, product, accepted);
        body.append(icon, details);

        const footer = document.createElement("div");
        footer.className = "order-card-footer";
        const info = document.createElement("div");
        info.className = "order-price";
        const fee = document.createElement("strong");
        fee.textContent = "ค่าหิ้ว " + Number(post.carryFee).toLocaleString("th-TH") + " บาท";
        info.appendChild(fee);
        const link = document.createElement("a");
        link.className = "order-detail-btn";
        link.href = "my-buyer-post-detail.html?postId=" + encodeURIComponent(post.postId);
        link.textContent = "ดูรายละเอียด →";
        footer.append(info, link);
        card.append(header, body, footer);
        return card;
    }

    async function loadPosts() {
        list.replaceChildren();
        let token;
        try {
            token = localStorage.getItem("token");
        } catch (error) {
            showMessage("ไม่สามารถอ่านสถานะเข้าสู่ระบบได้ กรุณาเข้าสู่ระบบใหม่", true);
            return;
        }
        if (!token) {
            window.location.replace("/login.html?returnTo=%2Forders.html%23posts-panel");
            return;
        }

        try {
            const response = await fetch("/api/v1/posts/my", {
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
                window.location.replace("/login.html?returnTo=%2Forders.html%23posts-panel");
                return;
            }
            if (!response.ok || !result || result.status !== "SUCCESS" || !Array.isArray(result.data)) {
                showMessage(result && typeof result.message === "string"
                    ? result.message
                    : "โหลดโพสต์ของคุณไม่สำเร็จ (HTTP " + response.status + ")", true);
                return;
            }

            count.textContent = result.data.length;
            if (result.data.length === 0) {
                showMessage("คุณยังไม่มีโพสต์หาคนรับหิ้ว", false);
                return;
            }
            message.hidden = true;
            result.data.forEach(function (post) {
                list.appendChild(renderPost(post));
            });
        } catch (error) {
            showMessage("ไม่สามารถเชื่อมต่อเซิร์ฟเวอร์ได้ กรุณาลองอีกครั้ง", true);
        }
    }

    loadPosts();
})();
