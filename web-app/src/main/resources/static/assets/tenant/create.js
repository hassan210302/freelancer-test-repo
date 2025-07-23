// Function to select company from search results
function selectCompanyFromSearch(element) {
    // Set flag to prevent form submission during selection
    isSelectingCompany = true;
    
    const name = element.getAttribute('data-name');
    const orgNumber = element.getAttribute('data-org-number');
    const countryCode = element.getAttribute('data-country-code');

    // Update form fields
    const nameInput = document.getElementById('name');
    const orgInput = document.getElementById('organizationNumber');
    const countrySelect = document.getElementById('countryCode');

    nameInput.value = name;
    if (orgNumber && orgNumber !== 'null') {
        orgInput.value = orgNumber;
    }
    countrySelect.value = countryCode;

    // Trigger change events to update component state
    nameInput.dispatchEvent(new Event('change', { bubbles: true }));
    orgInput.dispatchEvent(new Event('change', { bubbles: true }));
    countrySelect.dispatchEvent(new Event('change', { bubbles: true }));

    // Hide the dropdown
    document.getElementById('name-dropdown').innerHTML = '';
    
    // Force validation update and reset the selection flag
    setTimeout(() => {
        orgInput.setCustomValidity('');
        orgInput.reportValidity();
        // Reset flag to allow form submission
        isSelectingCompany = false;
    }, 100);
}

// Variable to track if company selection is in progress
let isSelectingCompany = false;

// Prevent form submission during company selection
document.getElementById('createCompanyForm').addEventListener('submit', function(e) {
    if (isSelectingCompany) {
        e.preventDefault();
        return false;
    }
});

// Reset submit button state after form response
document.getElementById('createCompanyForm').addEventListener('htmx:afterRequest', function(e) {
    // Only handle form responses, not search responses
    if (e.detail.elt.tagName === 'FORM') {
        setTimeout(() => {
            const submitButton = document.getElementById('submitButton');
            if (submitButton && submitButton.disabled) {
                submitButton.disabled = false;
                submitButton.innerHTML = '<wa-icon name="plus" style="margin-right: var(--wa-space-s);"></wa-icon>Create Company';
            }
        }, 100);
    }
});

// Reset selection flag when user types in name field
document.getElementById('name').addEventListener('input', function() {
    isSelectingCompany = false;
});

// Hide dropdown when clicking outside
document.addEventListener('click', function(e) {
    const nameInput = document.getElementById('name');
    const nameDropdown = document.getElementById('name-dropdown');

    // If clicking outside input and dropdown, hide dropdown
    if (nameInput && nameDropdown && 
        !nameInput.contains(e.target) && !nameDropdown.contains(e.target)) {
        nameDropdown.innerHTML = '';
        // Reset selection flag when hiding dropdown
        isSelectingCompany = false;
    }
});