const getInputValue = (input) => {
    if (!input) return '';
    return typeof input.value !== 'undefined' ? input.value : '';
};

const getNumericValue = (row, name) => {
    const input = row.querySelector(`wa-input[name="${name}"]`);
    return parseFloat(getInputValue(input)) || 0;
};

function roundHalfUp(value, decimals = 2) {
    const factor = 10 ** decimals;
    return Math.round((value * factor) + Number.EPSILON) / factor;
}

const updateAmount = (row) => {
    const qty = getNumericValue(row, 'quantity');
    const price = getNumericValue(row, 'unitPrice');
    const discount = getNumericValue(row, 'discount');
    const vatCode = getNumericValue(row, 'vatCode');
    const vatRate = vatCodes.find(v => Number(v.code) === vatCode)?.rate ?? 0;
    const subtotal = roundHalfUp(qty * price * (1 - discount / 100));
    const total = roundHalfUp(subtotal * (1 + vatRate / 100));
    row.querySelector('.amount').textContent = total.toFixed(2);
};

const updateTotal = () => {
    const total = Array.from(document.querySelectorAll('#invoiceBody .amount'))
        .reduce((sum, el) => sum + parseFloat(el.textContent || 0), 0);
    document.getElementById('invoiceTotal').textContent = total.toFixed(2);
};

const setupCombobox = (row) => {
    const combobox = row.querySelector('r-combobox');
    if (!combobox) return;
    combobox.items = vatCodes.map(v => ({
        value: v.code,
        title: v.code,
        subtitle: `${v.rate}% - ${v.description}`,
        displayText: `${v.code} (${v.rate}%) - ${v.description}`
    }));
    combobox.addEventListener('change', (e) => {
        const input = row.querySelector('wa-input[name="vatCode"]');
        input.value = e.detail.value;
        updateAmount(row);
        updateTotal();
    });
};

const cloneRow = (source) => {
    const clone = source.cloneNode(true);
    clone.querySelectorAll('wa-input').forEach(input => {
        const name = input.getAttribute('name');
        input.setAttribute('value',
            name === 'itemName' ? '' : (name === 'quantity' ? '1' : '0'));
    });
    clone.querySelector('r-combobox')?.setAttribute('value', '');
    clone.querySelector('.amount').textContent = '0.00';
    return clone;
};

const initInvoiceForm = async () => {
    vatCodes = await window.getVatCodes();
    const form = document.getElementById('invoiceForm');
    const tbody = document.getElementById('invoiceBody');
    const invoiceDate = document.getElementById('invoiceDate');
    const dueDate = document.getElementById('dueDate');
    const daysInput = document.getElementById('daysUntilDue');
    const customerInput = form.querySelector('wa-input[name="customerId"]');

    const getInvoiceDate = () => invoiceDate.value ? new Date(invoiceDate.value) : new Date();
    const getDaysUntilDue = () => parseInt(daysInput.value || daysInput.getAttribute('value') || '0', 10) || 0;

    const updateDueDate = () => {
        const date = new Date(getInvoiceDate());
        date.setDate(date.getDate() + getDaysUntilDue());
        dueDate.value = date.toISOString().slice(0, 10);
        daysInput.setAttribute('label', `days until due: ${date.toLocaleDateString()}`);
    };

    if (!invoiceDate.value) invoiceDate.value = new Date().toISOString().split('T')[0];

    updateDueDate();
    invoiceDate.addEventListener('change', updateDueDate);
    daysInput.addEventListener('input', updateDueDate);
    tbody.querySelectorAll('tr').forEach(row => {
        setupCombobox(row);
        updateAmount(row);
    });
    updateTotal();

    form.addEventListener('submit', (event) => {
        const customerId = getInputValue(customerInput);
        if (!customerId || customerId === '0') {
            form.querySelector('#customerSearch')?.scrollIntoView({behavior: 'smooth'});
            alert('Please select a customer.');
            return event.preventDefault();
        }

        for (const row of tbody.querySelectorAll('tr')) {
            const vat = getInputValue(row.querySelector('wa-input[name="vatCode"]'));
            if (!vat) {
                row.querySelector('r-combobox')?.scrollIntoView({behavior: 'smooth'});
                alert('Please select a VAT code for each line.');
                return event.preventDefault();
            }
        }

        event.preventDefault();
        const formData = new FormData();
        ['issueDate', 'dueDate', 'currencyCode', 'customerId', 'supplierId'].forEach(name => {
            const input = form.querySelector(`[name="${name}"]`);
            const val = getInputValue(input);
            if (val !== '') formData.append(name, val);
        });

        tbody.querySelectorAll('tr').forEach((row, i) => {
            ['itemName', 'quantity', 'unitPrice', 'discount', 'vatCode'].forEach(field => {
                const input = row.querySelector(`wa-input[name="${field}"]`);
                const val = getInputValue(input);
                if (val !== '') formData.append(`invoiceLines[${i}].${field}`, val);
            });
        });

        htmx.ajax('POST', '/htmx/invoice', {values: formData});
    });

    tbody.addEventListener('input', e => {
        const row = e.target.closest('tr');
        if (row && e.target.closest('wa-input')) {
            updateAmount(row);
            updateTotal();
        }
    });

    form.querySelector('[data-action="add-row"]')?.addEventListener('click', () => {
        const row = tbody.querySelector('tr');
        const newRow = cloneRow(row);
        tbody.appendChild(newRow);
        setupCombobox(newRow);
        updateAmount(newRow);
        updateTotal();
    });

    tbody.addEventListener('click', (e) => {
        const btn = e.target.closest('wa-button');
        if (!btn) return;

        const row = btn.closest('tr');
        const action = btn.getAttribute('data-action');

        if (action === 'delete') {
            if (tbody.rows.length > 1) row.remove();
            updateTotal();
        }

        if (action === 'duplicate') {
            const newRow = row.cloneNode(true);
            row.querySelectorAll('wa-input, r-combobox').forEach((el, i) => {
                const val = getInputValue(el);
                newRow.querySelectorAll('wa-input, r-combobox')[i]?.setAttribute('value', val);
            });
            row.parentNode.insertBefore(newRow, row.nextSibling);
            setupCombobox(newRow);
            updateAmount(newRow);
            updateTotal();
        }
    });
};

document.addEventListener('customerSelected', ({detail}) => {
    document.getElementById('customerId').value = detail.id;
});

document.addEventListener('DOMContentLoaded', initInvoiceForm);