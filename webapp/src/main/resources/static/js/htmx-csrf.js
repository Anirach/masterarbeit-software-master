document.addEventListener("htmx:configRequest", function (evt) {
    evt.detail.headers["accept"] = "text/html-partial";

    if (evt.detail.verb !== "get") {
        const csrfHeaderName = document
            .querySelector("meta[name='_csrf_header']")
            .getAttribute("content");
        evt.detail.headers[csrfHeaderName] = document
            .querySelector("meta[name='_csrf']")
            .getAttribute("content");

        console.log(evt.detail)
    }
});