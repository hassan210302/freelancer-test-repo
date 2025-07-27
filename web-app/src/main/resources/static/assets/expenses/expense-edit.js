var lastTarget = null;

function isFile(evt) {
    var dt = evt.dataTransfer;
    for (var i = 0; i < dt.types.length; i++) {
        if (dt.types[i] === "Files") {
            return true;
        }
    }
    return false;
}

window.addEventListener("dragenter", function (e) {
    if (isFile(e)) {
        lastTarget = e.target;
        document.querySelector("#dropzone").classList.add("visible");
    }
});

window.addEventListener("dragleave", function (e) {
    e.preventDefault();
    if (e.target === lastTarget || e.target === document) {
        document.querySelector("#dropzone").classList.remove("visible");
    }
});

window.addEventListener("dragover", function (e) {
    e.preventDefault();
});

window.addEventListener("drop", function (e) {
    e.preventDefault();
    document.querySelector("#dropzone").classList.remove("visible");

    if (e.dataTransfer.files.length > 0) {
        const files = e.dataTransfer.files;
        uploadFiles(files);
    } else {
        alert("No files dropped.");
    }
});

function uploadFiles(files) {
    const expenseIdInput = document.querySelector('input[name="expenseId"]');
    if (!expenseIdInput) {
        alert("Expense ID not found");
        return;
    }
    
    const expenseId = expenseIdInput.value;
    
    htmx.ajax('POST', '/htmx/expense/upload', {
        target: '#attachment-messages-edit',
        swap: 'outerHTML',
        values: {
            expenseId: expenseId
        }
    });
}

document.addEventListener('DOMContentLoaded', function() {
    const fileInput = document.getElementById("file-input-edit");
    if (fileInput) {
        fileInput.addEventListener('change', function() {
            if (this.files.length > 0) {
                uploadFiles(this.files);
            }
        });
    }
});

document.addEventListener("htmx:afterSwap", function(e) {
    if (e.detail.target.id === "attachment-messages-edit") {
        const fileInput = document.getElementById("file-input-edit");
        if (fileInput) {
            fileInput.value = "";
        }
        
        if (e.detail.xhr.status === 200) {
            setTimeout(() => {
                location.reload();
            }, 1000);
        }
    }
});

document.addEventListener("htmx:responseError", function(e) {
    alert("Upload failed: " + e.detail.xhr.status);
});

document.addEventListener("htmx:sendError", function(e) {
    alert("Network error during upload");
}); 