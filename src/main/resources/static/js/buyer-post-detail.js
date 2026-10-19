(function () {
    const message = document.getElementById("post-detail-message");
    const content = document.getElementById("post-detail-content");
    const acceptButton = document.getElementById("accept-post-button");
    const actions = document.getElementById("post-detail-actions");
    const postId = new URLSearchParams(window.location.search).get("postId");

    function setText(id, value) {
        document.getElementById(id).textContent = value == null || value === "" ? "-" : value;
    }

    function formatDate(value) {
        const date = new Date(value + "T00:00:00");
        return Number.isNaN(date.getTime())
            ? value || "-"
            : new Intl.DateTimeFormat("th-TH", { dateStyle: "full" }).format(date);
    }

    function formatDateTime(value) {
        const date = new Date(value);
        return Number.isNaN(date.getTime())
            ? value || "-"
            : new Intl.DateTimeFormat("th-TH", { dateStyle: "medium", timeStyle: "short" }).format(date);
    }

    function statusLabel(status) {
        return status === "OPEN" ? "กำลังหาคนรับหิ้ว" : status === "ACCEPTED" ? "มีผู้รับงานแล้ว" : status || "-";
    }

    function showMessage(text, isError) {
        message.textContent = text;
        message.className = "form-message " + (isError ? "error-message" : "success-message");
        message.hidden = false;
    }

    function displayPost(post) {
        setText("post-detail-status", statusLabel(post.status));
        setText("post-detail-id", "โพสต์ #" + post.postId);
        setText("post-detail-title", post.postTitle);
        setText("post-detail-description", post.productDetail);
        setText("post-detail-shop", post.shopName);
        setText("post-detail-product", post.productDetail + " (จำนวน " + post.quantity + ")");
        setText("post-detail-buyer", post.buyerName);
        setText("post-detail-date", formatDate(post.desiredDate));
        setText("post-detail-location", post.location);
        setText("post-detail-fee", Number(post.carryFee).toLocaleString("th-TH") + " บาท");
        setText("post-detail-budget", post.budget == null
            ? "ไม่ระบุ"
            : Number(post.budget).toLocaleString("th-TH") + " บาท");
        setText("post-detail-accepted-by", post.acceptedByName || "ยังไม่มีผู้รับงาน");
        setText("post-detail-accepted-at", post.acceptedAt ? formatDateTime(post.acceptedAt) : "-");
        setText("post-detail-additional", post.additionalDetails || "ไม่มีรายละเอียดเพิ่มเติม");
        acceptButton.hidden = post.status !== "OPEN";
        actions.hidden = false;
        content.hidden = false;
    }

    async function readResponse(response) {
        try {
            return await response.json();
        } catch (error) {
            throw new Error("เซิร์ฟเวอร์ตอบกลับในรูปแบบที่ไม่ถูกต้อง กรุณาลองอีกครั้ง");
        }
    }

    async function loadPost() {
        if (!postId || !/^[1-9]\d*$/.test(postId)) {
            showMessage("ไม่พบรหัสโพสต์ กรุณาเลือกรายการจากหน้าโพสต์", true);
            return;
        }
        try {
            const response = await fetch("/api/v1/posts/" + encodeURIComponent(postId));
            const result = await readResponse(response);
            if (!response.ok || !result || result.status !== "SUCCESS" || !result.data) {
                showMessage(result && typeof result.message === "string"
                    ? result.message
                    : "โหลดรายละเอียดโพสต์ไม่สำเร็จ (HTTP " + response.status + ")", true);
                return;
            }
            document.title = (result.data.postTitle || "รายละเอียดโพสต์") + " | CarryHub";
            displayPost(result.data);
        } catch (error) {
            showMessage(error.message || "ไม่สามารถเชื่อมต่อเซิร์ฟเวอร์ได้ กรุณาลองอีกครั้ง", true);
        }
    }

    acceptButton.addEventListener("click", async function () {
        let token;
        try {
            token = localStorage.getItem("token");
        } catch (error) {
            showMessage("ไม่สามารถอ่านสถานะเข้าสู่ระบบได้ กรุณาเข้าสู่ระบบใหม่", true);
            return;
        }
        if (!token) {
            const returnTo = window.location.pathname + window.location.search;
            window.location.replace("/login.html?returnTo=" + encodeURIComponent(returnTo));
            return;
        }

        acceptButton.disabled = true;
        acceptButton.textContent = "กำลังรับงาน...";
        try {
            const response = await fetch("/api/v1/posts/" + encodeURIComponent(postId) + "/accept", {
                method: "PATCH",
                headers: {
                    "Authorization": (localStorage.getItem("tokenType") || "Bearer") + " " + token
                }
            });
            const result = await readResponse(response);
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
                    : "รับงานไม่สำเร็จ (HTTP " + response.status + ")", true);
                acceptButton.disabled = false;
                acceptButton.textContent = "รับงานนี้";
                return;
            }
            displayPost(result.data);
            showMessage("รับงานโพสต์เรียบร้อยแล้ว", false);
        } catch (error) {
            showMessage(error.message || "ไม่สามารถเชื่อมต่อเซิร์ฟเวอร์ได้ กรุณาลองอีกครั้ง", true);
            acceptButton.disabled = false;
            acceptButton.textContent = "รับงานนี้";
        }
    });

    loadPost();
})();
