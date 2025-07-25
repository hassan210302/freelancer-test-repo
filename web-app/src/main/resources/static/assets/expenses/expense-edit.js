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

// Upload files function for edit mode
function uploadFiles(files) {
    console.log("Uploading", files.length, "files");
    
    // Get the expense ID from the form
    const expenseIdInput = document.querySelector('input[name="expenseId"]');
    if (!expenseIdInput) {
        alert("Expense ID not found");
        return;
    }
    
    const expenseId = expenseIdInput.value;
    
    // Make direct HTMX request
    htmx.ajax('POST', '/htmx/expense/upload', {
        target: '#attachment-messages-edit',
        swap: 'outerHTML',
        values: {
            expenseId: expenseId
        }
    });
}

document.addEventListener('DOMContentLoaded', function() {
    // Handle file input change for manual file selection
    const fileInput = document.getElementById("file-input-edit");
    if (fileInput) {
        fileInput.addEventListener('change', function() {
            if (this.files.length > 0) {
                console.log("File input changed, uploading", this.files.length, "files");
                uploadFiles(this.files);
            }
        });
    } else {
        console.error("File input not found");
    }
});

// Reset form and update attachment count after successful upload
document.addEventListener("htmx:afterSwap", function(e) {
    console.log("HTMX after swap", e.detail.target.id, e.detail.xhr.status);
    if (e.detail.target.id === "attachment-messages-edit") {
        const fileInput = document.getElementById("file-input-edit");
        if (fileInput) {
            fileInput.value = "";
        }
        
        // Show success message and update attachment count
        if (e.detail.xhr.status === 200) {
            console.log("Upload successful, reloading page");
            // Update attachment count by reloading the page after a short delay to show the message
            setTimeout(() => {
                location.reload();
            }, 1000);
        }
    }
});

// Add error handling
document.addEventListener("htmx:responseError", function(e) {
    console.error("HTMX error", e.detail);
    alert("Upload failed: " + e.detail.xhr.status);
});

document.addEventListener("htmx:sendError", function(e) {
    console.error("HTMX send error", e.detail);
    alert("Network error during upload");
}); 