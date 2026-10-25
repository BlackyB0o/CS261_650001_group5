(function () {
    const message = document.getElementById("my-post-message");
    const content = document.getElementById("my-post-content");
    const postId = new URLSearchParams(window.location.search).get("postId");

    function showMessage(text) {
        message.textContent = text;
        message.className = "form-message error-message";
        message.hidden = false;
    }

    function setText(id, value) {
        document.getElementById(id).textContent = value == null || value === "" ? "-" : value;
    }

    function formatDate(value, includeTime) {
        const date = new Date(includeTime ? value : value + "T00:00:00");
        return Number.isNaN(date.getTime())
            ? value || "-"
            : new Intl.DateTimeFormat("th-TH", includeTime
                ? { dateStyle: "medium", timeStyle: "short" }
                : { dateStyle: "full" }).format(date);
    }

    async function loadPost() {
        if (!postId || !/^[1-9]\d*$/.test(postId)) {
            showMessage("ไม่พบรหัสโพสต์ กรุณาเลือกโพสต์จากรายการของฉัน");
            return;
        }
        try {
            const response = await fetch("/api/v1/posts/" + encodeURIComponent(postId));
            let result;
            try {
                result = await response.json();
            } catch (error) {
                showMessage("เซิร์ฟเวอร์ตอบกลับในรูปแบบที่ไม่ถูกต้อง กรุณาลองอีกครั้ง");
                return;
            }
            if (!response.ok || !result || result.status !== "SUCCESS" || !result.data) {
                showMessage(result && typeof result.message === "string"
                    ? result.message
                    : "โหลดรายละเอียดโพสต์ไม่สำเร็จ (HTTP " + response.status + ")");
                return;
            }
            const post = result.data;
            document.title = (post.postTitle || "โพสต์ของฉัน") + " | CarryHub";
            setText("my-post-status", post.status === "OPEN" ? "กำลังหาคนรับหิ้ว"
                : post.status === "ACCEPTED" ? "มีผู้รับงานแล้ว" : post.status);
            setText("my-post-id", "โพสต์ #" + post.postId);
            setText("my-post-title", post.postTitle);
            setText("my-post-product", post.productDetail);
            setText("my-post-shop", post.shopName);
            setText("my-post-quantity", post.quantity);
            setText("my-post-date", formatDate(post.desiredDate, false));
            setText("my-post-location", post.location);
            setText("my-post-fee", Number(post.carryFee).toLocaleString("th-TH") + " บาท");
            setText("my-post-budget", post.budget == null
                ? "ไม่ระบุ"
                : Number(post.budget).toLocaleString("th-TH") + " บาท");
            setText("my-post-accepted-by", post.acceptedByName || "ยังไม่มีผู้รับงาน");
            setText("my-post-accepted-at", post.acceptedAt ? formatDate(post.acceptedAt, true) : "-");
            setText("my-post-additional", post.additionalDetails || "ไม่มีรายละเอียดเพิ่มเติม");
            content.hidden = false;
        } catch (error) {
            showMessage("ไม่สามารถเชื่อมต่อเซิร์ฟเวอร์ได้ กรุณาลองอีกครั้ง");
        }
    }

    loadPost();
})();
